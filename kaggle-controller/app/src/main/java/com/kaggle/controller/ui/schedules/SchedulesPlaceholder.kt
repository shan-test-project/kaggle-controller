package com.kaggle.controller.ui.schedules

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
fun SchedulesPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Schedules", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Automation and scheduling coming in Phase 4.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
    }
}
