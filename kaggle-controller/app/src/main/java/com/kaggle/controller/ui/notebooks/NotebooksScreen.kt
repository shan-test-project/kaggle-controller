package com.kaggle.controller.ui.notebooks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kaggle.controller.domain.model.Notebook
import com.kaggle.controller.ui.theme.*

@Composable
fun NotebooksScreen(modifier: Modifier = Modifier) {
    val notebooks = listOf(
        Notebook(id = "1", title = "boston-housing-analysis", owner = "shanahmed", language = "Python", lastUpdated = "2h ago", runStatus = "Running", computeType = "GPU"),
        Notebook(id = "2", title = "titanic-prediction", owner = "shanahmed", language = "Python", lastUpdated = "8h ago", runStatus = "Completed", computeType = "CPU"),
        Notebook(id = "3", title = "image-classification", owner = "shanahmed", language = "Python", lastUpdated = "1d ago", runStatus = "Failed", computeType = "GPU"),
    )

    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item {
            Text("Notebooks", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            Text("Manage your Kaggle notebooks and drafts", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(16.dp))
        }
        items(notebooks) { nb ->
            NotebookItemCard(nb)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun NotebookItemCard(nb: Notebook) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(nb.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Surface(
                    color = when (nb.runStatus) {
                        "Running" -> Success
                        "Failed" -> ErrorRed
                        else -> Warning
                    },
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Text(
                        text = nb.runStatus,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (nb.runStatus == "Failed") TextPrimary else Color.Black,
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("${nb.language} · ${nb.computeType} · Updated ${nb.lastUpdated}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}
