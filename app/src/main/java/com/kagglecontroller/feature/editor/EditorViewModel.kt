package com.kagglecontroller.feature.editor

import android.os.SystemClock
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kagglecontroller.AppContainer
import com.kagglecontroller.domain.model.Cell
import com.kagglecontroller.domain.model.CellType
import com.kagglecontroller.domain.model.LocalDraft
import com.kagglecontroller.domain.model.NotebookDoc
import com.kagglecontroller.domain.model.RunRecord
import com.kagglecontroller.domain.model.RunStatus
import com.kagglecontroller.domain.model.SyncState
import com.kagglecontroller.feature.runs.RunWatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class SaveLabel(val text: String) {
    UNSAVED("Unsaved changes"),
    SAVED_LOCALLY("Saved locally"),
    SYNCING("Syncing"),
    SYNCED("Synced"),
    SYNC_FAILED("Sync failed"),
}

data class EditorUi(
    val loading: Boolean = true,
    val missing: Boolean = false,
    val draft: LocalDraft? = null,
    val cells: List<Cell> = emptyList(),
    val activeIndex: Int = 0,
    val active: TextFieldValue = TextFieldValue(""),
    val label: SaveLabel = SaveLabel.SAVED_LOCALLY,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val pushing: Boolean = false,
)

sealed interface EditorEvent {
    data class Message(val text: String) : EditorEvent
    data class OpenRun(val ref: String) : EditorEvent
}

class EditorViewModel(private val c: AppContainer, private val draftId: String) : ViewModel() {
    private val _ui = MutableStateFlow(EditorUi())
    val ui: StateFlow<EditorUi> = _ui.asStateFlow()

    private val _events = MutableSharedFlow<EditorEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<EditorEvent> = _events.asSharedFlow()

    private val undoStack = ArrayDeque<TextFieldValue>()
    private val redoStack = ArrayDeque<TextFieldValue>()
    private var lastUndoPush = 0L
    private var saveJob: Job? = null

    init {
        viewModelScope.launch {
            val d = c.drafts.get(draftId)
            if (d == null) {
                _ui.value = EditorUi(loading = false, missing = true)
            } else {
                val cells = NotebookDoc.parse(d.text, d.kernelType)
                _ui.value = EditorUi(
                    loading = false, draft = d, cells = cells,
                    active = TextFieldValue(cells[0].source),
                    label = labelFor(d.sync),
                )
            }
        }
    }

    private fun labelFor(s: SyncState) = when (s) {
        SyncState.SYNCED -> SaveLabel.SYNCED
        SyncState.SYNCING -> SaveLabel.SYNCING
        SyncState.FAILED -> SaveLabel.SYNC_FAILED
        else -> SaveLabel.SAVED_LOCALLY
    }

    // ---------------- text editing ----------------

    fun onActiveChange(new: TextFieldValue) {
        val s = _ui.value
        val old = s.active
        var next = new
        if (new.text != old.text) {
            val py = s.draft?.language == "python" && s.cells.getOrNull(s.activeIndex)?.type == CellType.CODE
            next = autoIndent(old, new, c.settings.settings.value.tabSize, py)
            recordUndo(old)
            redoStack.clear()
        }
        commitActive(next)
    }

    private fun recordUndo(old: TextFieldValue) {
        val now = SystemClock.uptimeMillis()
        if (undoStack.isEmpty() || now - lastUndoPush > 600) {
            undoStack.addLast(old)
            if (undoStack.size > 100) undoStack.removeFirst()
        }
        lastUndoPush = now
    }

    private fun autoIndent(old: TextFieldValue, new: TextFieldValue, tab: Int, python: Boolean): TextFieldValue {
        if (new.text.length != old.text.length + 1 || !new.selection.collapsed) return new
        val pos = new.selection.start
        if (pos < 1 || new.text[pos - 1] != '\n') return new
        val prevLineStart = new.text.lastIndexOf('\n', pos - 2) + 1
        val prevLine = new.text.substring(prevLineStart, pos - 1)
        var indent = prevLine.takeWhile { it == ' ' || it == '\t' }
        if (python && prevLine.trimEnd().endsWith(":")) indent += " ".repeat(tab)
        if (indent.isEmpty()) return new
        val text = new.text.substring(0, pos) + indent + new.text.substring(pos)
        return TextFieldValue(text, TextRange(pos + indent.length))
    }

    private fun commitActive(next: TextFieldValue) {
        _ui.update { s ->
            val cells = s.cells.toMutableList()
            if (s.activeIndex in cells.indices && cells[s.activeIndex].source != next.text) {
                cells[s.activeIndex] = cells[s.activeIndex].copy(source = next.text)
            }
            s.copy(
                cells = cells, active = next, label = SaveLabel.UNSAVED,
                canUndo = undoStack.isNotEmpty(), canRedo = redoStack.isNotEmpty(),
            )
        }
        scheduleSave()
    }

    fun insert(text: String) {
        val v = _ui.value.active
        val sel = v.selection
        val newText = v.text.replaceRange(sel.min, sel.max, text)
        onActiveChange(TextFieldValue(newText, TextRange(sel.min + text.length)))
    }

    fun insertTab() = insert(" ".repeat(c.settings.settings.value.tabSize))

    fun indentSelection() {
        val v = _ui.value.active
        val tab = " ".repeat(c.settings.settings.value.tabSize)
        val start = v.text.lastIndexOf('\n', v.selection.min - 1) + 1
        val end = v.selection.max
        val block = v.text.substring(start, end)
        val indented = block.split("\n").joinToString("\n") { tab + it }
        val newText = v.text.substring(0, start) + indented + v.text.substring(end)
        val delta = indented.length - block.length
        onActiveChange(TextFieldValue(newText, TextRange(v.selection.min + tab.length, v.selection.max + delta)))
    }

    fun undo() {
        val prev = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(_ui.value.active)
        commitActive(prev)
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(_ui.value.active)
        commitActive(next)
    }

    fun findNext(query: String) {
        if (query.isBlank()) return
        val v = _ui.value.active
        var idx = v.text.indexOf(query, v.selection.max, ignoreCase = true)
        if (idx < 0) idx = v.text.indexOf(query, 0, ignoreCase = true)
        if (idx >= 0) commitActive(v.copy(selection = TextRange(idx, idx + query.length)))
        else _events.tryEmit(EditorEvent.Message("No match in this cell"))
    }

    fun replaceAll(query: String, replacement: String) {
        if (query.isBlank()) return
        val v = _ui.value.active
        val count = Regex(Regex.escape(query), RegexOption.IGNORE_CASE).findAll(v.text).count()
        if (count == 0) { _events.tryEmit(EditorEvent.Message("No match in this cell")); return }
        onActiveChange(TextFieldValue(v.text.replace(query, replacement, ignoreCase = true)))
        _events.tryEmit(EditorEvent.Message("Replaced $count in this cell"))
    }

    // ---------------- cells ----------------

    fun selectCell(index: Int) {
        val s = _ui.value
        if (index !in s.cells.indices || index == s.activeIndex) return
        undoStack.clear(); redoStack.clear()
        _ui.update {
            val src = it.cells[index].source
            it.copy(activeIndex = index, active = TextFieldValue(src, TextRange(src.length)), canUndo = false, canRedo = false)
        }
    }

    private fun editCells(newActive: Int, change: (MutableList<Cell>) -> Unit) {
        undoStack.clear(); redoStack.clear()
        _ui.update { s ->
            val list = s.cells.toMutableList()
            change(list)
            if (list.isEmpty()) list.add(Cell(type = CellType.CODE, source = ""))
            val idx = newActive.coerceIn(0, list.lastIndex)
            val src = list[idx].source
            s.copy(cells = list, activeIndex = idx, active = TextFieldValue(src, TextRange(src.length)),
                label = SaveLabel.UNSAVED, canUndo = false, canRedo = false)
        }
        scheduleSave()
    }

    fun addCellBelow(type: CellType = CellType.CODE, source: String = "") {
        val i = _ui.value.activeIndex
        editCells(i + 1) { it.add(i + 1, Cell(type = type, source = source)) }
    }

    fun deleteCell(index: Int) = editCells(index) { if (index in it.indices) it.removeAt(index) }

    fun duplicateCell(index: Int) = editCells(index + 1) {
        it.getOrNull(index)?.let { cell -> it.add(index + 1, cell.copy(id = UUID.randomUUID().toString())) }
    }

    fun moveCell(index: Int, delta: Int) {
        val target = index + delta
        if (target !in _ui.value.cells.indices) return
        editCells(target) { val cell = it.removeAt(index); it.add(target, cell) }
    }

    fun toggleCellType(index: Int) = editCells(index) {
        it.getOrNull(index)?.let { cell ->
            it[index] = cell.copy(type = if (cell.type == CellType.CODE) CellType.MARKDOWN else CellType.CODE)
        }
    }

    // ---------------- metadata / persistence ----------------

    fun updateDraft(transform: (LocalDraft) -> LocalDraft) {
        _ui.update { s -> s.draft?.let { s.copy(draft = transform(it), label = SaveLabel.UNSAVED) } ?: s }
        scheduleSave()
    }

    private fun currentDraft(): LocalDraft? {
        val s = _ui.value
        val d = s.draft ?: return null
        val text = NotebookDoc.serialize(s.cells, d.kernelType, d.language)
        val sync = if (d.sync == SyncState.SYNCED || d.sync == SyncState.FAILED || d.sync == SyncState.SYNCING) SyncState.UNSAVED else d.sync
        return d.copy(text = text, updatedAt = System.currentTimeMillis(), sync = sync)
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(800)
            persistNow()
        }
    }

    private suspend fun persistNow() {
        val d = currentDraft() ?: return
        c.drafts.save(d)
        _ui.update { it.copy(draft = d, label = if (it.pushing) SaveLabel.SYNCING else SaveLabel.SAVED_LOCALLY) }
    }

    /** Survives ViewModel clearing: called when leaving the screen so no keystroke is lost. */
    fun flush() {
        saveJob?.cancel()
        val d = currentDraft() ?: return
        c.appScope.launch { c.drafts.save(d) }
    }

    // ---------------- push / run ----------------

    fun push(run: Boolean) {
        val s = _ui.value
        if (s.pushing) return
        val base = currentDraft() ?: return
        val owner = c.auth.account.value?.username ?: c.settings.lastUsername
        val ref = base.ref ?: owner?.let { "$it/${slugify(base.title)}" }
        if (ref == null) { _events.tryEmit(EditorEvent.Message("Connect to Kaggle first so we know your username.")); return }
        val draft = base.copy(ref = ref, sync = SyncState.SYNCING)
        _ui.update { it.copy(draft = draft, pushing = true, label = SaveLabel.SYNCING) }
        viewModelScope.launch {
            c.drafts.save(draft)
            try {
                val res = c.notebooks.push(draft, run)
                val done = draft.copy(sync = SyncState.SYNCED, lastPushedVersion = res.versionNumber ?: draft.lastPushedVersion)
                c.drafts.save(done)
                _ui.update { it.copy(draft = done, pushing = false, label = SaveLabel.SYNCED) }
                if (run) {
                    c.runs.upsert(RunRecord(
                        id = UUID.randomUUID().toString(), ref = ref, title = draft.title,
                        versionNumber = res.versionNumber, startedAt = System.currentTimeMillis(),
                        status = RunStatus.QUEUED.name, machineShape = draft.machineShape,
                    ))
                    RunWatcher.schedule(c.appContext, ref)
                    _events.emit(EditorEvent.OpenRun(ref))
                } else {
                    _events.emit(EditorEvent.Message("Saved to Kaggle as a new version (not run)."))
                }
            } catch (e: Exception) {
                val failed = draft.copy(sync = SyncState.FAILED)
                c.drafts.save(failed)
                _ui.update { it.copy(draft = failed, pushing = false, label = SaveLabel.SYNC_FAILED) }
                _events.emit(EditorEvent.Message(e.message ?: "Push failed. Your draft is safe on this phone."))
            }
        }
    }

    companion object {
        /** Kaggle slugs: lowercase letters, digits and hyphens. */
        fun slugify(title: String): String =
            title.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifBlank { "untitled-notebook" }.take(50)
    }
}
