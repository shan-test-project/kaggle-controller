package com.kaggle.controller

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kaggle.controller.ui.home.HomeScreen
import com.kaggle.controller.ui.notebooks.NotebooksScreen
import com.kaggle.controller.ui.theme.ElectricCyan
import com.kaggle.controller.ui.theme.TextSecondary

@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    var selectedTab by mutableStateOf(0)

    val tabs = listOf(
        "Home" to R.drawable.ic_home,
        "Notebooks" to R.drawable.ic_notebook,
        "Schedules" to R.drawable.ic_schedule,
        "Explore" to R.drawable.ic_explore,
        "Files" to R.drawable.ic_folder,
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
            ) {
                tabs.forEachIndexed { index, (label, iconRes) ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = ImageVector.vectorResource(id = iconRes),
                                contentDescription = label,
                                tint = if (selectedTab == index) ElectricCyan else TextSecondary,
                            )
                        },
                        label = {
                            androidx.compose.material3.Text(
                                text = label,
                                color = if (selectedTab == index) ElectricCyan else TextSecondary,
                                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                            )
                        },
                    )
                }
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> HomeScreen(modifier = Modifier.padding(padding))
            1 -> NotebooksScreen(modifier = Modifier.padding(padding))
            2 -> SchedulesScreen(modifier = Modifier.padding(padding))
            3 -> ExploreScreen(modifier = Modifier.padding(padding))
            4 -> FilesScreen(modifier = Modifier.padding(padding))
        }
    }
}

@Composable
fun SchedulesScreen(modifier: Modifier = Modifier) {
    com.kaggle.controller.ui.schedules.SchedulesPlaceholder(modifier)
}

@Composable
fun ExploreScreen(modifier: Modifier = Modifier) {
    com.kaggle.controller.ui.explore.ExplorePlaceholder(modifier)
}

@Composable
fun FilesScreen(modifier: Modifier = Modifier) {
    com.kaggle.controller.ui.files.FilesPlaceholder(modifier)
}
