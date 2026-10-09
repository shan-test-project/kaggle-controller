package com.kagglecontroller.feature.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kagglecontroller.core.common.ApiSupport
import com.kagglecontroller.core.common.Capabilities
import com.kagglecontroller.core.ui.components.OpenInKaggleButton
import com.kagglecontroller.core.ui.rememberContainer
import com.kagglecontroller.data.local.AppMode
import com.kagglecontroller.data.local.ThemeMode

/** Settings + account + the live feature-support map, so users always know what the API can do. */
@Composable
fun MoreScreen(username: String?, verified: Boolean, onSignOut: () -> Unit) {
    val c = rememberContainer()
    val s by c.settings.settings.collectAsStateWithLifecycle()
    var confirmOut by remember { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("More", style = MaterialTheme.typography.headlineMedium) }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Account", style = MaterialTheme.typography.titleMedium)
                    Text(username ?: "Unknown", style = MaterialTheme.typography.bodyLarge)
                    Text(if (verified) "Connected to Kaggle" else "Offline: token not re-checked yet", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = { confirmOut = true }) { Text("Sign out and remove token") }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Workspace mode", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = s.appMode == AppMode.API_ONLY,
                            onClick = { c.settings.update { it.copy(appMode = AppMode.API_ONLY) } },
                            label = { Text("API only") },
                        )
                        FilterChip(
                            selected = s.appMode == AppMode.FULL,
                            onClick = { c.settings.update { it.copy(appMode = AppMode.FULL) } },
                            label = { Text("Full mode") },
                        )
                    }
                    Text(
                        if (s.appMode == AppMode.FULL) {
                            "Website-only tools open in a secure browser tab over the app. Kaggle and Google sign-in use your browser session, separate from your API token. Use the browser menu to request the desktop site on phones."
                        } else {
                            "API-only mode keeps website actions in your browser. The in-app notebook list, editor, logs, and API controls stay available."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (s.appMode == AppMode.FULL) {
                        OpenInKaggleButton("https://www.kaggle.com/code", label = "Open Kaggle workspace")
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Appearance", style = MaterialTheme.typography.titleMedium)
                    Row(Modifier.fillMaxWidth().then(Modifier), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { m ->
                            FilterChip(selected = s.themeMode == m, onClick = { c.settings.update { it.copy(themeMode = m) } },
                                label = { Text(m.name.lowercase().replaceFirstChar { it.uppercase() }) })
                        }
                    }
                    SwitchRow("Dynamic colors (Android 12+)", s.dynamicColor) { v -> c.settings.update { it.copy(dynamicColor = v) } }
                    Text("Editor", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(12, 14, 16, 18).forEach { size ->
                            FilterChip(selected = s.editorFontSize == size, onClick = { c.settings.update { it.copy(editorFontSize = size) } }, label = { Text("${size}sp") })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(2, 4).forEach { t -> FilterChip(selected = s.tabSize == t, onClick = { c.settings.update { it.copy(tabSize = t) } }, label = { Text("Tab $t") }) }
                    }
                    SwitchRow("Word wrap", s.wordWrap) { v -> c.settings.update { it.copy(wordWrap = v) } }
                    Text("Notifications", style = MaterialTheme.typography.titleMedium)
                    SwitchRow("Notify when a run finishes", s.notifyRuns) { v -> c.settings.update { it.copy(notifyRuns = v) } }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Privacy", style = MaterialTheme.typography.titleMedium)
                    Text("No telemetry. No Kaggle Controller server. Your token is stored encrypted on this phone and only sent to api.kaggle.com. Notebook content is never sent anywhere except Kaggle when you save or run.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { Text("What works through Kaggle's API", style = MaterialTheme.typography.titleMedium) }
        items(Capabilities.all, key = { it.id }) { cap ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(cap.title, style = MaterialTheme.typography.titleSmall)
                    Text(supportLabel(cap.support) + if (cap.support != ApiSupport.UNSUPPORTED && !cap.implementedInApp) " (not built in this version)" else "",
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                    Text(cap.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (cap.support == ApiSupport.SUPPORTED_BY_WEBSITE_ONLY || !cap.implementedInApp) {
                        if (cap.webPath.isNotBlank()) OpenInKaggleButton(Capabilities.webUrl(cap.webPath))
                    }
                }
            }
        }
    }
    if (confirmOut) AlertDialog(
        onDismissRequest = { confirmOut = false },
        title = { Text("Sign out?") },
        text = { Text("The token is removed from this phone. Local drafts are kept. The token itself stays valid on Kaggle until you revoke it in your Kaggle settings.") },
        confirmButton = { Button(onClick = { confirmOut = false; onSignOut() }) { Text("Sign out") } },
        dismissButton = { TextButton(onClick = { confirmOut = false }) { Text("Cancel") } },
    )
}

private fun supportLabel(s: ApiSupport) = when (s) {
    ApiSupport.SUPPORTED_BY_API -> "Supported by the Kaggle API"
    ApiSupport.PARTIALLY_SUPPORTED -> "Partially supported"
    ApiSupport.SUPPORTED_BY_WEBSITE_ONLY -> "Website only: use Open in Kaggle"
    ApiSupport.UNSUPPORTED -> "Local feature / not available"
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f)); Switch(checked, onChange)
    }
}
