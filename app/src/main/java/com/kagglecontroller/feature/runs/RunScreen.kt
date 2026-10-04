package com.kagglecontroller.feature.runs

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.kagglecontroller.core.common.ApiState
import com.kagglecontroller.core.ui.components.OpenInKaggleButton
import com.kagglecontroller.core.ui.components.StatusPill
import com.kagglecontroller.core.ui.containerViewModel
import com.kagglecontroller.core.ui.theme.CodeFont
import com.kagglecontroller.core.ui.theme.KC
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunScreen(ref: String, onBack: () -> Unit, onEdit: (() -> Unit)?) {
    val vm = containerViewModel(key = "run-$ref") { RunViewModel(it, ref) }
    val ui by vm.ui.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var autoScroll by remember { mutableStateOf(true) }
    var filter by remember { mutableStateOf("") }

    // Poll only while the screen is visible. Leaving the app pauses it; the loop ends when the run ends.
    LaunchedEffect(ref) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { vm.pollUntilDone() }
    }

    val lines = remember(ui.log, filter) {
        val all = ui.log.lines()
        if (filter.isBlank()) all else all.filter { it.contains(filter, ignoreCase = true) }
    }
    LaunchedEffect(lines.size, autoScroll) {
        if (autoScroll && lines.isNotEmpty()) listState.scrollToItem(lines.lastIndex)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(ref, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } },
                actions = { IconButton(onClick = { scope.launch { vm.refresh() } }) { Icon(Icons.Outlined.Refresh, "Refresh now") } },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatusPill(ui.status)
                Text(
                    ui.lastChecked?.let { "Checked ${(System.currentTimeMillis() - it) / 1000}s ago" } ?: "Checking...",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ui.failureMessage?.let { Text(it, color = KC.Danger, style = MaterialTheme.typography.bodyMedium) }
            when (val e = ui.error) {
                is ApiState.Unauthorized -> Text("Kaggle rejected your token. Sign in again from Settings.", color = KC.Danger)
                is ApiState.RateLimited -> Text("Kaggle asked us to slow down. Polling is backing off.", color = KC.Warning)
                is ApiState.Offline -> Text("You're offline. Showing the last log we received.", color = KC.Warning)
                is ApiState.Error -> Text(e.message, color = KC.Danger)
                else -> Unit
            }
            Text(
                "Kaggle exposes the run log as a snapshot, so it updates every few seconds rather than streaming line by line.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                FilterChip(selected = autoScroll, onClick = { autoScroll = !autoScroll }, label = { Text(if (autoScroll) "Auto-scroll on" else "Auto-scroll paused") })
                TextButton(onClick = {
                    val i = vm.errorLineIndex()
                    if (i >= 0) { autoScroll = false; scope.launch { listState.scrollToItem(i.coerceIn(0, lines.lastIndex.coerceAtLeast(0))) } }
                    else scope.launch { snackbar.showSnackbar("No error found in the log") }
                }) { Text("Jump to error") }
                TextButton(onClick = { autoScroll = true; scope.launch { if (lines.isNotEmpty()) listState.scrollToItem(lines.lastIndex) } }) { Text("Latest") }
                IconButton(onClick = { clipboard.setText(AnnotatedString(ui.log)) }) { Icon(Icons.Outlined.ContentCopy, "Copy log") }
                IconButton(onClick = { shareText(ctx, "$ref log", ui.log) }) { Icon(Icons.Outlined.Share, "Share log") }
            }
            androidx.compose.material3.OutlinedTextField(
                filter, { filter = it }, singleLine = true, label = { Text("Search log") }, modifier = Modifier.fillMaxWidth(),
            )

            LogView(lines, listState, Modifier.weight(1f).fillMaxWidth())

            Text("Output files (${ui.files.size})", style = MaterialTheme.typography.titleMedium)
            if (ui.files.isEmpty()) {
                Text("No output files yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(Modifier.height(140.dp)) {
                    itemsIndexed(ui.files) { _, f ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text(f.name, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = CodeFont, style = MaterialTheme.typography.bodySmall)
                            IconButton(onClick = {
                                val ok = enqueueDownload(ctx, f.url, f.name)
                                scope.launch { snackbar.showSnackbar(if (ok) "Downloading ${f.name}" else "Couldn't start the download") }
                            }) { Icon(Icons.Outlined.Download, "Download ${f.name}") }
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                onEdit?.let { OutlinedButton(onClick = it) { Text("Open draft") } }
                OpenInKaggleButton("https://www.kaggle.com/code/$ref")
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun LogView(lines: List<String>, state: LazyListState, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xFF050810)).padding(10.dp)) {
        if (lines.isEmpty() || (lines.size == 1 && lines[0].isEmpty())) {
            Text("Waiting for log output...", color = KC.TextDim, fontFamily = CodeFont, style = MaterialTheme.typography.bodySmall)
        } else {
            LazyColumn(state = state) {
                itemsIndexed(lines) { _, line ->
                    val isErr = line.contains("Traceback") || line.contains("Error")
                    Text(line, fontFamily = CodeFont, style = MaterialTheme.typography.bodySmall, color = if (isErr) KC.Danger else KC.Text)
                }
            }
        }
    }
}

private fun shareText(ctx: Context, subject: String, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"; putExtra(Intent.EXTRA_SUBJECT, subject); putExtra(Intent.EXTRA_TEXT, text.takeLast(200_000))
    }
    ctx.startActivity(Intent.createChooser(send, "Share log"))
}

/** Uses Android's DownloadManager: background, resumable, notifies on completion, streams to disk. */
fun enqueueDownload(ctx: Context, url: String, name: String): Boolean = try {
    val req = DownloadManager.Request(Uri.parse(url))
        .setTitle(name)
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalFilesDir(ctx, Environment.DIRECTORY_DOWNLOADS, "kaggle/" + name.replace("..", "_"))
    (ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(req)
    true
} catch (e: Exception) { false }
