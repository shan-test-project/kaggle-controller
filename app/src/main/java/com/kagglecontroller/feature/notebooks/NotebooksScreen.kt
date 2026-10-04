package com.kagglecontroller.feature.notebooks

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kagglecontroller.core.common.ApiState
import com.kagglecontroller.core.ui.components.EmptyState
import com.kagglecontroller.core.ui.components.OpenInKaggleButton
import com.kagglecontroller.core.ui.components.StateHost
import com.kagglecontroller.core.ui.components.StatusPill
import com.kagglecontroller.core.ui.containerViewModel
import com.kagglecontroller.core.ui.theme.KC
import com.kagglecontroller.domain.model.LocalDraft
import com.kagglecontroller.domain.model.Notebook
import com.kagglecontroller.domain.model.RunRecord
import com.kagglecontroller.domain.model.SyncState
import kotlinx.coroutines.launch

private const val MAX_IMPORT_BYTES = 5 * 1024 * 1024

/** What a row can open and which quick actions it offers. */
private data class NbRow(
    val key: String,
    val title: String,
    val subtitle: String,
    val ref: String? = null,
    val draftId: String? = null,
    val webUrl: String? = null,
    val run: RunRecord? = null,
    val syncLabel: String? = null,
    val isMine: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotebooksScreen(
    onOpenEditor: (String) -> Unit,
    onOpenNotebook: (String) -> Unit,
    onReauth: () -> Unit,
    username: String?,
) {
    val vm = containerViewModel { NotebooksViewModel(it) }
    val filter by vm.filter.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val remote by vm.remote.collectAsStateWithLifecycle()
    val drafts by vm.drafts.collectAsStateWithLifecycle()
    val runs by vm.runs.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    var showNew by rememberSaveable { mutableStateOf(false) }
    var sheet by remember { mutableStateOf<NbRow?>(null) }
    var confirmDelete by remember { mutableStateOf<NbRow?>(null) }
    var busyPull by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val imported = readImport(ctx, uri)
            if (imported == null) { snackbar.showSnackbar("Couldn't import that file. Use .ipynb, .py, .r or .rmd under 5 MB."); return@launch }
            val id = vm.createDraft(imported.title, imported.kernelType, imported.language, imported.text)
            onOpenEditor(id)
        }
    }

    fun pullToEditor(ref: String) {
        scope.launch {
            busyPull = true
            vm.draftFromRemote(ref).fold(
                onSuccess = { onOpenEditor(it) },
                onFailure = { snackbar.showSnackbar(it.message ?: "Couldn't download the notebook.") },
            )
            busyPull = false
        }
    }

    val rows: List<NbRow> = when (filter) {
        NotebookFilter.MINE, NotebookFilter.PUBLIC -> ((remote as? ApiState.Success)?.data?.items.orEmpty()).map {
            NbRow(it.ref, it.title, subtitleOf(it), ref = it.ref, webUrl = it.webUrl, isMine = it.owner.equals(username, true))
        }
        NotebookFilter.FAVORITES -> favorites.sorted().map {
            NbRow(it, it.substringAfter('/'), it.substringBefore('/'), ref = it, webUrl = "https://www.kaggle.com/code/$it") }
        NotebookFilter.DRAFTS -> drafts.map {
            NbRow(it.id, it.title, draftSubtitle(it), draftId = it.id, syncLabel = syncText(it.sync)) }
        NotebookFilter.RUNNING -> runs.filter { !it.runStatus.terminal }.distinctBy { it.ref }.map(::runRow)
        NotebookFilter.FAILED -> runs.filter { it.runStatus.name == "ERROR" }.distinctBy { it.ref }.map(::runRow)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { showNew = true }, icon = { Icon(Icons.Outlined.Add, null) }, text = { Text("New notebook") })
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Notebooks", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = { importLauncher.launch(arrayOf("*/*")) }) { Icon(Icons.Outlined.FileUpload, contentDescription = "Import a file") }
            }
            if (filter == NotebookFilter.MINE || filter == NotebookFilter.PUBLIC) {
                OutlinedTextField(
                    query, vm::setQuery, singleLine = true, label = { Text("Search notebooks") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(NotebookFilter.entries) { f -> FilterChip(selected = f == filter, onClick = { vm.setFilter(f) }, label = { Text(f.label) }) }
            }
            Spacer(Modifier.height(8.dp))

            val listUi: @Composable () -> Unit = {
                if (rows.isEmpty()) {
                    EmptyState(emptyTitle(filter), emptyBody(filter)) {
                        if (filter == NotebookFilter.DRAFTS) Button(onClick = { showNew = true }) { Text("Create a notebook") }
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(rows, key = { it.key }) { row ->
                            NotebookCard(
                                row = row, favorite = row.ref?.let { it in favorites } ?: false,
                                onToggleFavorite = { row.ref?.let(vm::toggleFavorite) },
                                onClick = {
                                    when {
                                        row.draftId != null -> onOpenEditor(row.draftId)
                                        row.ref != null -> onOpenNotebook(row.ref)
                                    }
                                },
                                onLongClick = { sheet = row },
                            )
                            if (row === rows.lastOrNull() && filter.let { it == NotebookFilter.MINE || it == NotebookFilter.PUBLIC }) {
                                LaunchedEffect(row.key) { vm.loadMore() }
                            }
                        }
                        val more = (remote as? ApiState.Success)?.data?.loadingMore == true
                        if (more) item { Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp) } }
                    }
                }
            }

            if (filter == NotebookFilter.MINE || filter == NotebookFilter.PUBLIC) {
                PullToRefreshBox(isRefreshing = remote is ApiState.Loading, onRefresh = { vm.reload(force = true) }, modifier = Modifier.fillMaxSize()) {
                    StateHost(remote, onRetry = { vm.reload(force = true) }, onReauth = onReauth) { listUi() }
                }
            } else listUi()
        }
    }

    if (busyPull) AlertDialog(onDismissRequest = {}, confirmButton = {}, title = { Text("Downloading notebook") }, text = { CircularProgressIndicator() })

    if (showNew) NewNotebookDialog(onDismiss = { showNew = false }) { title, type, lang ->
        showNew = false
        scope.launch { onOpenEditor(vm.createDraft(title, type, lang)) }
    }

    sheet?.let { row ->
        ModalBottomSheet(onDismissRequest = { sheet = null }) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(row.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                if (row.draftId != null) {
                    SheetAction("Open editor") { sheet = null; onOpenEditor(row.draftId) }
                    SheetAction("Delete local draft", danger = true) { sheet = null; confirmDelete = row }
                }
                if (row.ref != null) {
                    SheetAction("Open run view, log and outputs") { sheet = null; onOpenNotebook(row.ref) }
                    SheetAction("Download to editor (pull)") { sheet = null; pullToEditor(row.ref) }
                    SheetAction(if (row.ref in favorites) "Remove from favorites" else "Add to favorites") { sheet = null; vm.toggleFavorite(row.ref) }
                    if (row.isMine) SheetAction("Delete on Kaggle", danger = true) { sheet = null; confirmDelete = row }
                }
                row.webUrl?.let { OpenInKaggleButton(it, Modifier.fillMaxWidth()) }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    confirmDelete?.let { row ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text(if (row.draftId != null) "Delete this draft?" else "Delete on Kaggle?") },
            text = { Text(if (row.draftId != null) "\"${row.title}\" will be removed from this phone. This can't be undone."
                          else "\"${row.title}\" and all of its versions will be permanently deleted from your Kaggle account.") },
            confirmButton = {
                Button(onClick = {
                    confirmDelete = null
                    scope.launch {
                        if (row.draftId != null) vm.deleteDraft(row.draftId)
                        else row.ref?.let { r -> vm.deleteRemote(r).onFailure { snackbar.showSnackbar(it.message ?: "Delete failed.") } }
                    }
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Keep it") } },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NotebookCard(row: NbRow, favorite: Boolean, onToggleFavorite: () -> Unit, onClick: () -> Unit, onLongClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(row.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(row.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (row.run != null || row.syncLabel != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    row.run?.let { StatusPill(it.runStatus) }
                    row.syncLabel?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = KC.Cyan) }
                }
            }
        }
        if (row.ref != null) IconButton(onClick = onToggleFavorite) {
            Icon(if (favorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                contentDescription = if (favorite) "Remove from favorites" else "Add to favorites",
                tint = if (favorite) KC.Warning else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SheetAction(label: String, danger: Boolean = false, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.fillMaxWidth(), color = if (danger) KC.Danger else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun NewNotebookDialog(onDismiss: () -> Unit, onCreate: (title: String, kernelType: String, language: String) -> Unit) {
    var title by remember { mutableStateOf("Untitled notebook") }
    var choice by remember { mutableStateOf(0) }
    val options = listOf(
        Triple("Python notebook", "notebook", "python"),
        Triple("Python script", "script", "python"),
        Triple("R notebook", "notebook", "r"),
        Triple("R script", "script", "r"),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New notebook") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true)
                options.forEachIndexed { i, o ->
                    FilterChip(selected = choice == i, onClick = { choice = i }, label = { Text(o.first) })
                }
                Text("It's created on this phone first. Nothing is sent to Kaggle until you save or run it.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { Button(onClick = { onCreate(title.ifBlank { "Untitled notebook" }, options[choice].second, options[choice].third) }) { Text("Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun subtitleOf(n: Notebook) = listOfNotNull(n.owner.takeIf { it.isNotBlank() }, n.language, n.kernelType, n.lastRunTime?.let { "last run $it" })
    .joinToString("  |  ")

private fun draftSubtitle(d: LocalDraft) = listOfNotNull(d.ref ?: "not on Kaggle yet", d.language, d.kernelType).joinToString("  |  ")

private fun syncText(s: SyncState) = when (s) {
    SyncState.LOCAL_ONLY -> "Saved locally"
    SyncState.UNSAVED -> "Local changes not on Kaggle"
    SyncState.SYNCING -> "Syncing"
    SyncState.SYNCED -> "Synced"
    SyncState.FAILED -> "Sync failed"
}

private fun runRow(r: RunRecord) = NbRow(r.id, r.title, r.ref, ref = r.ref, webUrl = "https://www.kaggle.com/code/${r.ref}", run = r)

private fun emptyTitle(f: NotebookFilter) = when (f) {
    NotebookFilter.MINE -> "No notebooks found"
    NotebookFilter.PUBLIC -> "No public notebooks match"
    NotebookFilter.FAVORITES -> "No favorites yet"
    NotebookFilter.DRAFTS -> "No local drafts"
    NotebookFilter.RUNNING -> "Nothing running"
    NotebookFilter.FAILED -> "No failed runs"
}

private fun emptyBody(f: NotebookFilter) = when (f) {
    NotebookFilter.MINE -> "Your Kaggle notebooks will show up here. Create one to get started."
    NotebookFilter.PUBLIC -> "Try a different search."
    NotebookFilter.FAVORITES -> "Tap the star on a notebook to keep it close."
    NotebookFilter.DRAFTS -> "Drafts live on this phone and work offline."
    NotebookFilter.RUNNING -> "Runs you start from this app appear here while they're queued or running."
    NotebookFilter.FAILED -> "Failed runs started from this app appear here."
}

private data class Imported(val title: String, val kernelType: String, val language: String, val text: String)

private suspend fun readImport(ctx: Context, uri: Uri): Imported? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    try {
        val name = ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        } ?: return@withContext null
        val ext = name.substringAfterLast('.', "").lowercase()
        val (type, lang) = when (ext) {
            "ipynb" -> "notebook" to "python"
            "py" -> "script" to "python"
            "r" -> "script" to "r"
            "rmd" -> "script" to "rmarkdown"
            else -> return@withContext null
        }
        val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readNBytes(MAX_IMPORT_BYTES + 1) } ?: return@withContext null
        if (bytes.size > MAX_IMPORT_BYTES) return@withContext null
        val text = String(bytes, Charsets.UTF_8)
        // An .ipynb for R declares its language in the kernelspec.
        val finalLang = if (type == "notebook" && text.contains("\"language\": \"R\"")) "r" else lang
        Imported(name.substringBeforeLast('.'), type, finalLang, text)
    } catch (e: Exception) { null }
}
