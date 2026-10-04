package com.kagglecontroller.feature.schedules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kagglecontroller.core.common.Capabilities
import com.kagglecontroller.core.ui.components.OpenInKaggleButton

/**
 * Honest schedules screen. Kaggle server-side schedules are not in the public CLI/SDK, so we do not
 * pretend to list or edit them. Android-local schedules are planned and clearly labelled as different.
 */
@Composable
fun SchedulesScreen() {
    val server = Capabilities.byId("schedules.server")
    val local = Capabilities.byId("schedules.local")
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Schedules", style = MaterialTheme.typography.headlineMedium)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Kaggle server schedule", style = MaterialTheme.typography.titleMedium)
                Text("Runs on Kaggle's servers even when your phone is off.", style = MaterialTheme.typography.bodyMedium)
                Text(server.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OpenInKaggleButton(Capabilities.webUrl("/code"), label = "Manage on Kaggle")
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Android local schedule (not built yet)", style = MaterialTheme.typography.titleMedium)
                Text(local.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("These are not the same thing: a local schedule depends on this phone being awake, online and not restricted by battery saver.",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
