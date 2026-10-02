package com.kaggle.controller.ui.files

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kaggle.controller.ui.theme.TextPrimary
import com.kaggle.controller.ui.theme.TextSecondary

@Composable
fun FilesPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Files", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Dataset uploads, model files, and outputs.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
    }
}
