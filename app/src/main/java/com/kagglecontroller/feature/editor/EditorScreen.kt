package com.kagglecontroller.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kagglecontroller.core.ui.components.EmptyState
import com.kagglecontroller.core.ui.components.OpenInKaggleButton
import com.kagglecontroller.core.ui.containerViewModel
import com.kagglecontroller.core.ui.rememberContainer
import com.kagglecontroller.core.ui.theme.CodeFont
import com.kagglecontroller.core.ui.theme.KC
import com.kagglecontroller.domain.model.CellType
import com.kagglecontroller.domain.model.LocalDraft

private val machineOptions = listOf(
    "" to "CPU only",
    "NvidiaTeslaT4" to "GPU T4 x2",
    "NvidiaL4" to "GPU L4",
    "TpuV5E8" to "TPU v5e-8",
)

private enum class Snippet(val title: String, val code: String) {
    ENV("Check environment", "import sys, platform\nprint(sys.version)\nprint(platform.platform())\n!pip list --format=freeze | head -n 40"),
    GPU("Check GPU", "!nvidia-smi\nimport torch\nprint(torch.cuda.is_available())"),
    DISK("Check disk and memory", "!df -h /kaggle/working\n!free -h"),
    PIP("Install a package (edit first)", "!pip install -q package-name"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(draftId: String, onBack: () -> Unit, onOpenRun: (String) -> Unit) {
    val vm = containerViewModel(key = "editor-$draftId") { EditorViewModel(it, draftId) }
    val ui by vm.ui.collectAsStateWithLifecycle()
    val settings by rememberContainer().settings.settings.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val clipboard = LocalClipboardManager.current
    val desktop = LocalConfiguration.current.screenWidthDp >= 760

    var menu by remember { mutableStateOf(false) }
    var showFind by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var confirmRun by rememberSaveable { mutableStateOf(false) }
    var confirmPush by rememberSaveable { mutableStateOf(false) }
    var findText by rememberSaveable { mutableStateOf("") }
    var replaceText by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        vm.events.collect { e ->
            when (e) {
                is EditorEvent.Message -> snackbar.showSnackbar(e.text)
                is EditorEvent.OpenRun -> onOpenRun(e.ref)
            }
        }
    }
    DisposableEffect(Unit) { onDispose { vm.flush() } }

    val draft = ui.draft
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(draft?.title ?: "Editor", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                        // Text label as well as colour: never colour alone.
                        Text(ui.label.text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { vm.flush(); onBack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showFind = !showFind }) { Icon(Icons.Outlined.Search, contentDescription = "Find and replace") }
                    if (desktop && draft != null) {
                        IconButton(onClick = { confirmRun = true }) { Icon(Icons.Outlined.PlayArrow, contentDescription = "Run notebook") }
                    }
                    IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, contentDescription = "More actions") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Notebook settings") }, onClick = { menu = false; showSettings = true })
                        DropdownMenuItem(text = { Text("Save to Kaggle (don't run)") }, enabled = !ui.pushing, onClick = { menu = false; confirmPush = true })
                        HorizontalDivider()
                        Snippet.entries.forEach { sn ->
                            DropdownMenuItem(text = { Text("Insert: ${sn.title}") }, onClick = {
                                menu = false
                                if (draft?.kernelType == "notebook") vm.addCellBelow(CellType.CODE, sn.code) else vm.insert("\n" + sn.code + "\n")
                            })
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (draft != null && !desktop) Column(Modifier.navigationBarsPadding().imePadding()) {
                KeyToolbar(
                    canUndo = ui.canUndo, canRedo = ui.canRedo,
                    onKey = vm::insert, onTab = vm::insertTab, onIndent = vm::indentSelection,
                    onCopy = {
                        val sel = ui.active.selection
                        val t = if (sel.collapsed) ui.active.text else ui.active.text.substring(sel.min, sel.max)
                        clipboard.setText(AnnotatedString(t))
                    },
                    onPaste = { clipboard.getText()?.text?.let(vm::insert) },
                    onUndo = vm::undo, onRedo = vm::redo, onRun = { confirmRun = true },
                )
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                ui.loading -> Unit
                ui.missing || draft == null -> EmptyState("Draft not found", "This draft was deleted or never saved.")
                else -> Column(Modifier.fillMaxSize()) {
                    if (showFind) FindBar(
                        find = findText, replace = replaceText,
                        onFind = { findText = it }, onReplace = { replaceText = it },
                        onNext = { vm.findNext(findText) }, onReplaceAll = { vm.replaceAll(findText, replaceText) },
                    )
                    if (draft.kernelType == "notebook") {
                        if (desktop) {
                            Row(Modifier.fillMaxSize().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                NotebookCells(
                                    vm, ui, draft, settings.editorFontSize, settings.wordWrap,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    largeCells = true,
                                )
                                EditorDetails(
                                    draft = draft,
                                    modifier = Modifier.widthIn(min = 280.dp, max = 360.dp).fillMaxHeight(),
                                    onSettings = { showSettings = true },
                                    onRun = { confirmRun = true },
                                )
                            }
                        } else {
                            NotebookCells(vm, ui, draft, settings.editorFontSize, settings.wordWrap)
                        }
                    } else {
                        if (desktop) {
                            Row(Modifier.fillMaxSize().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Column(Modifier.weight(1f).fillMaxHeight().padding(12.dp)) {
                                    CodeField(
                                        value = ui.active, onValueChange = vm::onActiveChange,
                                        language = draft.language, fontSizeSp = settings.editorFontSize, wrap = settings.wordWrap,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                                EditorDetails(
                                    draft = draft,
                                    modifier = Modifier.widthIn(min = 280.dp, max = 360.dp).fillMaxHeight(),
                                    onSettings = { showSettings = true },
                                    onRun = { confirmRun = true },
                                )
                            }
                        } else {
                            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
                                CodeField(
                                    value = ui.active, onValueChange = vm::onActiveChange,
                                    language = draft.language, fontSizeSp = settings.editorFontSize, wrap = settings.wordWrap,
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 400.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSettings && draft != null) NotebookSettingsDialog(draft, { showSettings = false }) { new ->
        vm.updateDraft { new }; showSettings = false
    }

    if (confirmRun && draft != null) AlertDialog(
        onDismissRequest = { confirmRun = false },
        title = { Text("Run on Kaggle?") },
        text = {
            Text(
                "This saves a new version of \"${draft.title}\" and starts a run on Kaggle's servers" +
                    (if (draft.machineShape.isNotBlank()) " using ${machineOptions.firstOrNull { it.first == draft.machineShape }?.second ?: draft.machineShape}. GPU and TPU time counts against your weekly quota." else ".") +
                    " You can leave the app while it runs."
            )
        },
        confirmButton = { Button(onClick = { confirmRun = false; vm.push(run = true) }, enabled = !ui.pushing) { Text("Save and run") } },
        dismissButton = { TextButton(onClick = { confirmRun = false }) { Text("Cancel") } },
    )

    if (confirmPush && draft != null) AlertDialog(
        onDismissRequest = { confirmPush = false },
        title = { Text("Save to Kaggle?") },
        text = { Text("This creates a new saved version of the notebook on Kaggle without running it. Your local draft is kept.") },
        confirmButton = { Button(onClick = { confirmPush = false; vm.push(run = false) }) { Text("Save version") } },
        dismissButton = { TextButton(onClick = { confirmPush = false }) { Text("Cancel") } },
    )
}

@Composable
private fun NotebookCells(
    vm: EditorViewModel,
    ui: EditorUi,
    draft: LocalDraft,
    fontSize: Int,
    wrap: Boolean,
    modifier: Modifier = Modifier,
    largeCells: Boolean = false,
) {
    val focus = remember { FocusRequester() }
    LazyColumn(modifier, contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        itemsIndexed(ui.cells, key = { _, c -> c.id }) { index, cell ->
            val active = index == ui.activeIndex
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(if (active) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
                    .clickable(enabled = !active) { vm.selectCell(index) }
                    .padding(12.dp)
                    .semantics { contentDescription = "${cell.type.label} cell ${index + 1}" }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${cell.type.label} ${index + 1}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.weight(1f))
                    if (active) CellMenu(vm, index, cell.type, ui.cells.size)
                }
                if (active) {
                    CodeField(
                        value = ui.active, onValueChange = vm::onActiveChange,
                        language = if (cell.type == CellType.MARKDOWN) "markdown" else draft.language,
                        fontSizeSp = fontSize, wrap = wrap, focusRequester = focus,
                        modifier = Modifier.fillMaxWidth().heightIn(min = if (largeCells) 180.dp else 56.dp),
                    )
                } else {
                    // Inactive cells are cheap read-only previews: keeps long notebooks fast on low-end phones.
                    val preview = remember(cell.source) { cell.source.lines().take(8).joinToString("\n").ifBlank { "(empty)" } }
                    Text(preview, fontFamily = CodeFont, style = MaterialTheme.typography.bodySmall, maxLines = 8, overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        item { TextButton(onClick = { vm.addCellBelow() }) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(6.dp)); Text("Add code cell") } }
    }
}

@Composable
private fun EditorDetails(
    draft: LocalDraft,
    modifier: Modifier = Modifier,
    onSettings: () -> Unit,
    onRun: () -> Unit,
) {
    Column(
        modifier.background(MaterialTheme.colorScheme.surface).verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("NOTEBOOK", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
        Text(draft.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
        HorizontalDivider()
        Text("Runtime", style = MaterialTheme.typography.labelLarge)
        Text(machineOptions.firstOrNull { it.first == draft.machineShape }?.second ?: "CPU only",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Language", style = MaterialTheme.typography.labelLarge)
        Text(draft.language.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyMedium)
        Text("Datasets", style = MaterialTheme.typography.labelLarge)
        Text(draft.datasetSources.takeIf { it.isNotEmpty() }?.joinToString("\n") ?: "None attached",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(if (draft.enableInternet) "Internet enabled" else "Internet disabled",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth()) { Text("Runtime settings") }
        Button(onClick = onRun, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Run notebook")
        }
        draft.ref?.let { ref ->
            OpenInKaggleButton("https://www.kaggle.com/code/$ref", Modifier.fillMaxWidth(), "Open Kaggle editor")
        }
    }
}

@Composable
private fun CellMenu(vm: EditorViewModel, index: Int, type: CellType, count: Int) {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }, modifier = Modifier.size(40.dp)) { Icon(Icons.Outlined.MoreVert, contentDescription = "Cell actions") }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        DropdownMenuItem(text = { Text("Add code cell below") }, onClick = { open = false; vm.addCellBelow(CellType.CODE) })
        DropdownMenuItem(text = { Text("Add text cell below") }, onClick = { open = false; vm.addCellBelow(CellType.MARKDOWN) })
        DropdownMenuItem(text = { Text("Duplicate") }, onClick = { open = false; vm.duplicateCell(index) })
        DropdownMenuItem(text = { Text(if (type == CellType.CODE) "Convert to text" else "Convert to code") }, onClick = { open = false; vm.toggleCellType(index) })
        DropdownMenuItem(text = { Text("Move up") }, enabled = index > 0, leadingIcon = { Icon(Icons.Outlined.ExpandLess, null) }, onClick = { open = false; vm.moveCell(index, -1) })
        DropdownMenuItem(text = { Text("Move down") }, enabled = index < count - 1, leadingIcon = { Icon(Icons.Outlined.ExpandMore, null) }, onClick = { open = false; vm.moveCell(index, 1) })
        HorizontalDivider()
        DropdownMenuItem(text = { Text("Delete cell", color = KC.Danger) }, onClick = { open = false; vm.deleteCell(index) })
    }
}

@Composable
private fun FindBar(find: String, replace: String, onFind: (String) -> Unit, onReplace: (String) -> Unit, onNext: () -> Unit, onReplaceAll: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
        Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(find, onFind, label = { Text("Find in this cell") }, singleLine = true, modifier = Modifier.weight(1f))
                Button(onClick = onNext) { Text("Next") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(replace, onReplace, label = { Text("Replace with") }, singleLine = true, modifier = Modifier.weight(1f))
                TextButton(onClick = onReplaceAll) { Text("Replace all") }
            }
        }
    }
}

/** Phone keyboard toolbar (spec 6). Horizontally scrollable so every key stays reachable. */
@Composable
private fun KeyToolbar(
    canUndo: Boolean, canRedo: Boolean,
    onKey: (String) -> Unit, onTab: () -> Unit, onIndent: () -> Unit,
    onCopy: () -> Unit, onPaste: () -> Unit, onUndo: () -> Unit, onRedo: () -> Unit, onRun: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            KeyButton("TAB", "Insert tab", onTab)
            KeyButton("INDENT", "Indent lines", onIndent)
            listOf("(", ")", "[", "]", "{", "}", ":", "_", "/").forEach { k -> KeyButton(k, "Insert $k") { onKey(k) } }
            Box(Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outline))
            IconKey(Icons.Outlined.ContentCopy, "Copy", true, onCopy)
            IconKey(Icons.Outlined.ContentPaste, "Paste", true, onPaste)
            IconKey(Icons.AutoMirrored.Outlined.Undo, "Undo", canUndo, onUndo)
            IconKey(Icons.AutoMirrored.Outlined.Redo, "Redo", canRedo, onRedo)
            Button(onClick = onRun, modifier = Modifier.heightIn(min = 40.dp)) {
                Icon(Icons.Outlined.PlayArrow, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("RUN")
            }
        }
    }
}

@Composable
private fun KeyButton(label: String, description: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.heightIn(min = 44.dp).defaultMinSize(minWidth = 44.dp).semantics { contentDescription = description },
    ) { Text(label, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelLarge) }
}

@Composable
private fun IconKey(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(44.dp)) { Icon(icon, contentDescription = description) }
}

@Composable
private fun NotebookSettingsDialog(draft: LocalDraft, onDismiss: () -> Unit, onSave: (LocalDraft) -> Unit) {
    var title by remember { mutableStateOf(draft.title) }
    var machine by remember { mutableStateOf(draft.machineShape) }
    var internet by remember { mutableStateOf(draft.enableInternet) }
    var isPrivate by remember { mutableStateOf(draft.isPrivate) }
    var datasets by remember { mutableStateOf(draft.datasetSources.joinToString(", ")) }
    var models by remember { mutableStateOf(draft.modelSources.joinToString(", ")) }
    var competitions by remember { mutableStateOf(draft.competitionSources.joinToString(", ")) }
    val ctx = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Notebook settings") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text(
                    "Kaggle address: ${draft.ref ?: "chosen from the title when you first save"}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("Accelerator", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    machineOptions.forEach { (id, label) -> FilterChip(selected = machine == id, onClick = { machine = id }, label = { Text(label) }) }
                }
                Text(
                    "Which accelerators you may use depends on your Kaggle account and quota. Kaggle decides at run time.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SettingSwitch("Internet access", internet) { internet = it }
                SettingSwitch("Private notebook", isPrivate) { isPrivate = it }
                OutlinedTextField(datasets, { datasets = it }, label = { Text("Datasets (owner/slug, comma separated)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(models, { models = it }, label = { Text("Models (owner/model/framework/variation)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(competitions, { competitions = it }, label = { Text("Competitions (slug)") }, modifier = Modifier.fillMaxWidth())
                OpenInKaggleButton("https://www.kaggle.com/code", label = "Secrets and add-ons: open Kaggle")
            }
        },
        confirmButton = {
            Button(onClick = {
                fun parse(s: String) = s.split(',', '\n').map { it.trim() }.filter { it.isNotEmpty() }
                onSave(draft.copy(
                    title = title.ifBlank { draft.title }, machineShape = machine, enableInternet = internet, isPrivate = isPrivate,
                    datasetSources = parse(datasets), modelSources = parse(models), competitionSources = parse(competitions),
                ))
            }) { Text("Save settings") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
