package com.kagglecontroller.feature.runs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kagglecontroller.core.ui.components.EmptyState
import com.kagglecontroller.core.ui.components.StatusPill
import com.kagglecontroller.core.ui.rememberContainer
import java.text.DateFormat
import java.util.Date

@Composable
fun RunsListScreen(onOpen: (String) -> Unit) {
    val c = rememberContainer()
    val runs by c.runs.runs.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { c.runs.refresh() }
    Column(Modifier.fillMaxSize()) {
        Text("Runs", Modifier.padding(16.dp), style = MaterialTheme.typography.headlineMedium)
        Text(
            "Runs you started from this app. Runs started on the Kaggle website aren't listed because the public API has no run-history call.",
            Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (runs.isEmpty()) EmptyState("No runs yet", "Open a notebook, tap RUN, and it will appear here.")
        else LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(runs, key = { it.id }) { r ->
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface)
                        .clickable { onOpen(r.ref) }.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(r.title, style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatusPill(r.runStatus)
                        Text(
                            buildString {
                                append(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(r.startedAt)))
                                r.durationMs?.let { append("  |  ${it / 1000 / 60}m ${it / 1000 % 60}s") }
                                r.versionNumber?.let { append("  |  v$it") }
                                append("  |  ${if (r.machineShape.isBlank()) "CPU" else r.machineShape}")
                            },
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
