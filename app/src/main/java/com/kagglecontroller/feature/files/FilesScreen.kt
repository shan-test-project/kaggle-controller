package com.kagglecontroller.feature.files

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kagglecontroller.core.ui.components.EmptyState
import com.kagglecontroller.core.ui.rememberContainer
import com.kagglecontroller.domain.model.LocalDraft
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun FilesScreen(onOpenEditor: (String) -> Unit) {
    val c = rememberContainer()
    val ctx = LocalContext.current
    val drafts by c.drafts.drafts.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf<LocalDraft?>(null) }
    val downloads = remember { File(ctx.getExternalFilesDir(null), "Download/kaggle") }
    var dlSize by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) { c.drafts.refresh(); dlSize = downloads.walkTopDown().filter { it.isFile }.sumOf { it.length() } }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Files", style = MaterialTheme.typography.headlineMedium)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Storage", style = MaterialTheme.typography.titleMedium)
                Text("Drafts: ${fmt(c.drafts.sizeBytes())}", style = MaterialTheme.typography.bodyMedium)
                Text("Downloaded outputs: ${fmt(dlSize)}", style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = {
                    downloads.deleteRecursively(); dlSize = 0
                }) { Text("Delete downloaded outputs") }
            }
        }
        Text("Local drafts", style = MaterialTheme.typography.titleMedium)
        if (drafts.isEmpty()) EmptyState("No drafts", "Notebooks you create or pull appear here and work offline.")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(drafts, key = { it.id }) { d ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(d.title, style = MaterialTheme.typography.titleMedium)
                            Text("${d.kernelType} | ${d.language} | ${fmt(d.text.length.toLong())}", style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = { onOpenEditor(d.id) }) { Text("Open") }
                        TextButton(onClick = { share(ctx, d) }) { Text("Share") }
                        TextButton(onClick = { confirm = d }) { Text("Delete") }
                    }
                }
            }
        }
    }
    confirm?.let { d ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("Delete this draft?") },
            text = { Text("\"${d.title}\" will be removed from this phone. This can't be undone.") },
            confirmButton = { Button(onClick = { confirm = null; scope.launch { c.drafts.delete(d.id) } }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Keep it") } },
        )
    }
}

private fun share(ctx: Context, d: LocalDraft) {
    val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_SUBJECT, d.title); putExtra(Intent.EXTRA_TEXT, d.text) }
    ctx.startActivity(Intent.createChooser(send, "Share draft"))
}

private fun fmt(b: Long) = when {
    b < 1024 -> "$b B"
    b < 1024 * 1024 -> "${b / 1024} KB"
    else -> "%.1f MB".format(b / 1048576.0)
}
