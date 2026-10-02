package com.kaggle.controller.ui.explore

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
fun ExplorePlaceholder(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Explore", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Global Kaggle search, datasets, models, and competitions.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
    }
}
