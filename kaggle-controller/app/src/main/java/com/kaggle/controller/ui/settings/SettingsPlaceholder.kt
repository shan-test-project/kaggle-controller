package com.kaggle.controller.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kaggle.controller.ui.theme.*

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val settingsItems = listOf(
        "Account" to "Kaggle authentication and profile",
        "Notifications" to "Run alerts and sync status",
        "Appearance" to "Theme, colors, and typography",
        "Offline Mode" to "Local drafts and cached data",
        "Auto Sync" to "Background synchronization",
        "Storage" to "Cache, downloads, and drafts",
        "About" to "Kaggle Controller v1.0.0001",
    )

    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
        Text("Customize your Kaggle control center.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Spacer(modifier = Modifier.height(16.dp))
        settingsItems.forEach { (title, desc) ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text(desc, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        }
    }
}
