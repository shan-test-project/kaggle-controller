package com.kagglecontroller.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kagglecontroller.core.ui.components.StatusPill
import com.kagglecontroller.core.ui.rememberContainer
import com.kagglecontroller.core.ui.theme.KC
import com.kagglecontroller.domain.model.RunStatus

/**
 * Dashboard built only from data the app really has: the signed-in account, runs started here,
 * and local drafts. Statistics are computed from those runs, never invented (spec 3).
 */
@Composable
fun HomeScreen(
    username: String?, verified: Boolean,
    onNewNotebook: () -> Unit, onOpenRun: (String) -> Unit, onOpenDraft: (String) -> Unit,
    onAllRuns: () -> Unit, onSearch: () -> Unit, onMore: () -> Unit,
) {
    val c = rememberContainer()
    val runs by c.runs.runs.collectAsStateWithLifecycle()
    val drafts by c.drafts.drafts.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { c.runs.refresh(); c.drafts.refresh() }

    val weekAgo = System.currentTimeMillis() - 7L * 24 * 3600 * 1000
    val week = runs.filter { it.startedAt >= weekAgo }
    val active = runs.filter { !it.runStatus.terminal }
    val ok = week.count { it.runStatus == RunStatus.COMPLETE }
    val bad = week.count { it.runStatus == RunStatus.ERROR }
    val durations = week.mapNotNull { it.durationMs }
    val avg = if (durations.isEmpty()) null else durations.average().toLong() / 1000

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(username?.let { "Hi, $it" } ?: "Kaggle Controller", style = MaterialTheme.typography.headlineMedium)
                Text(if (verified) "Connected to Kaggle" else "Offline: showing local data", style = MaterialTheme.typography.bodySmall, color = if (verified) KC.Success else KC.Warning)
            }
            IconButton(onClick = onSearch) { Icon(Icons.Outlined.Search, "Search Kaggle") }
            IconButton(onClick = onMore) { Icon(Icons.Outlined.MoreHoriz, "More and settings") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("Running", active.size.toString(), Modifier.weight(1f))
            Stat("Done this week", ok.toString(), Modifier.weight(1f))
            Stat("Failed this week", bad.toString(), Modifier.weight(1f))
        }
        Text(
            avg?.let { "Average runtime this week: ${it / 60}m ${it % 60}s (runs started from this app)" } ?: "Run a notebook from here to start seeing runtime stats.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primary).clickable(onClick = onNewNotebook).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Add, null, tint = androidx.compose.ui.graphics.Color.White)
            Spacer(Modifier.width(10.dp))
            Text("New notebook", color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.titleMedium)
        }

        Section("Active runs", if (active.isEmpty()) null else "All runs", onAllRuns)
        if (active.isEmpty()) Muted("Nothing running right now.")
        active.take(4).forEach { r -> RunRow(r.title, r.runStatus, r.ref) { onOpenRun(r.ref) } }

        Section("Recent runs", null) {}
        val recent = runs.filter { it.runStatus.terminal }.take(4)
        if (recent.isEmpty()) Muted("No finished runs yet.")
        recent.forEach { r -> RunRow(r.title, r.runStatus, r.ref) { onOpenRun(r.ref) } }

        Section("Recent drafts", null) {}
        if (drafts.isEmpty()) Muted("No local drafts yet.")
        drafts.take(4).forEach { d ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface).clickable { onOpenDraft(d.id) }.padding(14.dp)) {
                Text(d.title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Text(d.kernelType, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable private fun Stat(label: String, value: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).padding(14.dp)) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.secondary)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun Section(title: String, action: String?, onAction: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        if (action != null) Text(action, Modifier.clickable(onClick = onAction), color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable private fun Muted(t: String) = Text(t, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable private fun RunRow(title: String, status: RunStatus, ref: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface).clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(ref, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        StatusPill(status)
    }
}
