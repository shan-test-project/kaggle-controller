package com.kagglecontroller.feature.notebooks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kagglecontroller.AppContainer
import com.kagglecontroller.core.common.ApiState
import com.kagglecontroller.core.common.toApiState
import com.kagglecontroller.domain.model.LocalDraft
import com.kagglecontroller.domain.model.Notebook
import com.kagglecontroller.domain.model.NotebookDoc
import com.kagglecontroller.domain.model.RunRecord
import com.kagglecontroller.domain.model.RunStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import java.util.UUID

/** Filters only offer things the app can truthfully compute (spec 5): remote lists + local data. */
enum class NotebookFilter(val label: String) {
    MINE("Mine"), PUBLIC("Public"), FAVORITES("Favorites"), DRAFTS("Drafts"), RUNNING("Running"), FAILED("Failed")
}

data class NotebookPage(val items: List<Notebook>, val nextToken: String?, val loadingMore: Boolean = false)

class NotebooksViewModel(private val c: AppContainer) : ViewModel() {
    private val _filter = MutableStateFlow(NotebookFilter.MINE)
    val filter: StateFlow<NotebookFilter> = _filter.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _remote = MutableStateFlow<ApiState<NotebookPage>>(ApiState.Idle)
    val remote: StateFlow<ApiState<NotebookPage>> = _remote.asStateFlow()

    val drafts: StateFlow<List<LocalDraft>> = c.drafts.drafts
    val runs: StateFlow<List<RunRecord>> = c.runs.runs
    val favorites: StateFlow<Set<String>> = c.settings.favorites

    private var loadJob: Job? = null

    init {
        viewModelScope.launch { c.drafts.refresh(); c.runs.refresh() }
        reload()
    }

    fun setFilter(f: NotebookFilter) { _filter.value = f; reload() }

    fun setQuery(q: String) {
        _query.value = q
        loadJob?.cancel()
        loadJob = viewModelScope.launch { delay(400); load(reset = true, force = false) } // debounce
    }

    fun reload(force: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch { load(reset = true, force = force) }
    }

    fun loadMore() {
        val s = _remote.value as? ApiState.Success ?: return
        if (s.data.nextToken == null || s.data.loadingMore) return
        _remote.value = ApiState.Success(s.data.copy(loadingMore = true))
        viewModelScope.launch { load(reset = false, force = false) }
    }

    private suspend fun load(reset: Boolean, force: Boolean) {
        val f = _filter.value
        if (f != NotebookFilter.MINE && f != NotebookFilter.PUBLIC) { _remote.value = ApiState.Idle; return }
        val previous = (_remote.value as? ApiState.Success)?.data
        if (reset) _remote.value = ApiState.Loading
        val me = c.auth.account.value?.username ?: c.settings.lastUsername
        try {
            val page = c.notebooks.list(
                search = _query.value.ifBlank { null },
                user = if (f == NotebookFilter.MINE) me else null,
                language = null,
                pageToken = if (reset) null else previous?.nextToken,
                force = force,
            )
            _remote.value = ApiState.Success(NotebookPage(
                items = if (reset || previous == null) page.items else previous.items + page.items,
                nextToken = page.nextPageToken,
            ))
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            _remote.value = if (!reset && previous != null) ApiState.Success(previous.copy(loadingMore = false)) else e.toApiState()
        }
    }

    fun toggleFavorite(ref: String) = c.settings.toggleFavorite(ref)

    suspend fun createDraft(title: String, kernelType: String, language: String, text: String = ""): String {
        val id = UUID.randomUUID().toString()
        val body = if (text.isNotEmpty() || kernelType != "notebook") text else NotebookDoc.serialize(NotebookDoc.parse("", "notebook"), "notebook", language)
        c.drafts.save(LocalDraft(id = id, title = title, kernelType = kernelType, language = language, text = body))
        return id
    }

    suspend fun draftFromRemote(ref: String): Result<String> = try {
        val src = c.notebooks.pull(ref)
        val id = UUID.randomUUID().toString()
        val type = src.kernelType?.lowercase()?.takeIf { it == "notebook" || it == "script" }
            ?: if (src.text.trimStart().startsWith("{")) "notebook" else "script"
        c.drafts.save(LocalDraft(
            id = id, ref = ref, title = src.title ?: ref.substringAfter('/'),
            kernelType = type, language = src.language?.lowercase() ?: "python", text = src.text,
            sync = com.kagglecontroller.domain.model.SyncState.SYNCED,
        ))
        Result.success(id)
    } catch (e: Exception) { Result.failure(e) }

    suspend fun deleteDraft(id: String) = c.drafts.delete(id)

    suspend fun deleteRemote(ref: String): Result<Unit> = try { c.notebooks.delete(ref); reload(force = true); Result.success(Unit) }
    catch (e: Exception) { Result.failure(e) }

    fun runsWith(status: (RunStatus) -> Boolean): List<RunRecord> =
        c.runs.runs.value.filter { status(it.runStatus) }.distinctBy { it.ref }
}
