package com.kagglecontroller.data.local

import android.content.Context
import com.kagglecontroller.domain.model.LocalDraft
import com.kagglecontroller.domain.model.RunRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

private val storeJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

/** Atomic write: write to a temp file, then rename, so a crash never leaves a half-written draft. */
private fun File.writeAtomic(text: String) {
    parentFile?.mkdirs()
    val tmp = File(parentFile, "$name.tmp")
    tmp.writeText(text)
    if (!tmp.renameTo(this)) {
        writeText(text)
        tmp.delete()
    }
}

/** One JSON file per draft under files/drafts. Works fully offline (spec 9, 40). */
class DraftStore(context: Context) {
    private val dir = File(context.applicationContext.filesDir, "drafts").apply { mkdirs() }
    private val _drafts = MutableStateFlow<List<LocalDraft>>(emptyList())
    val drafts: StateFlow<List<LocalDraft>> = _drafts.asStateFlow()

    suspend fun refresh() = withContext(Dispatchers.IO) {
        _drafts.value = dir.listFiles { f -> f.extension == "json" }.orEmpty().mapNotNull { f ->
            try { storeJson.decodeFromString(LocalDraft.serializer(), f.readText()) } catch (e: Exception) { null }
        }.sortedByDescending { it.updatedAt }
    }

    suspend fun get(id: String): LocalDraft? = withContext(Dispatchers.IO) {
        val f = File(dir, "$id.json")
        if (!f.exists()) null else try { storeJson.decodeFromString(LocalDraft.serializer(), f.readText()) } catch (e: Exception) { null }
    }

    suspend fun save(draft: LocalDraft) = withContext(Dispatchers.IO) {
        File(dir, "${draft.id}.json").writeAtomic(storeJson.encodeToString(LocalDraft.serializer(), draft))
        _drafts.value = (listOf(draft) + _drafts.value.filterNot { it.id == draft.id }).sortedByDescending { it.updatedAt }
    }

    /** Callers must confirm with the user first: drafts are never deleted silently. */
    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        File(dir, "$id.json").delete()
        _drafts.value = _drafts.value.filterNot { it.id == id }
    }

    fun sizeBytes(): Long = dir.listFiles().orEmpty().sumOf { it.length() }
}

/** Local run history. Only records runs started from this app (we do not invent history). Capped at 200. */
class RunStore(context: Context) {
    private val file = File(context.applicationContext.filesDir, "runs.json")
    private val _runs = MutableStateFlow<List<RunRecord>>(emptyList())
    val runs: StateFlow<List<RunRecord>> = _runs.asStateFlow()
    private val serializer = ListSerializer(RunRecord.serializer())

    suspend fun refresh() = withContext(Dispatchers.IO) {
        _runs.value = if (!file.exists()) emptyList() else try {
            storeJson.decodeFromString(serializer, file.readText())
        } catch (e: Exception) { emptyList() }
    }

    suspend fun upsert(record: RunRecord) = withContext(Dispatchers.IO) {
        val next = (listOf(record) + _runs.value.filterNot { it.id == record.id })
            .sortedByDescending { it.startedAt }.take(200)
        file.writeAtomic(storeJson.encodeToString(serializer, next))
        _runs.value = next
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        file.delete(); _runs.value = emptyList()
    }
}

enum class ThemeMode { SYSTEM, LIGHT, DARK, AMOLED }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val dynamicColor: Boolean = false,
    val editorFontSize: Int = 14,
    val tabSize: Int = 4,
    val wordWrap: Boolean = false,
    val notifyRuns: Boolean = true,
    val reduceMotion: Boolean = false,
)

class SettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("kc_settings", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _favorites = MutableStateFlow(prefs.getStringSet("favorites", emptySet()).orEmpty())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    private fun read() = AppSettings(
        themeMode = runCatching { ThemeMode.valueOf(prefs.getString("theme", "DARK")!!) }.getOrDefault(ThemeMode.DARK),
        dynamicColor = prefs.getBoolean("dynamic", false),
        editorFontSize = prefs.getInt("fontSize", 14),
        tabSize = prefs.getInt("tabSize", 4),
        wordWrap = prefs.getBoolean("wrap", false),
        notifyRuns = prefs.getBoolean("notifyRuns", true),
        reduceMotion = prefs.getBoolean("reduceMotion", false),
    )

    fun update(block: (AppSettings) -> AppSettings) {
        val s = block(_settings.value)
        prefs.edit()
            .putString("theme", s.themeMode.name).putBoolean("dynamic", s.dynamicColor)
            .putInt("fontSize", s.editorFontSize).putInt("tabSize", s.tabSize)
            .putBoolean("wrap", s.wordWrap).putBoolean("notifyRuns", s.notifyRuns)
            .putBoolean("reduceMotion", s.reduceMotion).apply()
        _settings.value = s
    }

    /** Not a secret: only used to label the account when the app starts offline. */
    var lastUsername: String?
        get() = prefs.getString("lastUsername", null)
        set(value) { prefs.edit().putString("lastUsername", value).apply() }

    fun toggleFavorite(ref: String) {
        val next = _favorites.value.toMutableSet().apply { if (!add(ref)) remove(ref) }
        prefs.edit().putStringSet("favorites", next).apply()
        _favorites.value = next
    }
}
