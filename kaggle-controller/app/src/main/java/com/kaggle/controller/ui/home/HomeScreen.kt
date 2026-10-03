package com.kaggle.controller.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kaggle.controller.ui.theme.*

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with app branding
        item {
            Column(modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Cloud,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = ElectricCyan,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Kaggle Controller",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary,
                        )
                        Text(
                            text = "Your Kaggle workspace, redesigned for Android.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }
            }
        }

        // Connection status
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = Success,
                        modifier = Modifier.size(12.dp),
                    ) {}
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Connected to Kaggle", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text("Official API · No backend", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text("v1.0.0001", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                }
            }
        }

        // Quick actions
        item {
            Text("Quick Actions", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                QuickActionButton("New Notebook", "Add new Python or R notebook")
                QuickActionButton("Run Notebook", "Execute selected notebook")
                QuickActionButton("Import", "Import .ipynb from device")
            }
        }

        // Running/Queued/Failed stats cards
        item {
            Text("Current Runs", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("Running", "2", Success)
                StatCard("Queued", "1", Warning)
                StatCard("Failed", "0", ErrorRed)
            }
        }

        // Recent notebooks
        item {
            Text("Recent Notebooks", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "boston-housing-analysis" to "Python · Updated 2h ago · Running",
                    "titanic-prediction" to "Python · Updated 8h ago · Completed",
                    "image-classification" to "Python · Updated 1d ago · Scheduled",
                ).forEach { (title, meta) ->
                    NotebookCard(title = title, meta = meta)
                }
            }
        }
    }
}

@Composable
fun RowScope.QuickActionButton(label: String, desc: String) {
    ElevatedButton(
        onClick = {},
        colors = ButtonDefaults.elevatedButtonColors(containerColor = SurfaceHigh, contentColor = TextPrimary),
        modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(desc, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}

@Composable
fun RowScope.StatCard(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Card(
        modifier = Modifier.weight(1f),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = color, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}

@Composable
fun NotebookCard(title: String, meta: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(meta, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}
