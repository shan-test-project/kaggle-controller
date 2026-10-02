package com.kaggle.controller.ui.navigation

sealed class Screen(val route: String, val label: String) {
    object Home : Screen("home", "Home")
    object Notebooks : Screen("notebooks", "Notebooks")
    object Schedules : Screen("schedules", "Schedules")
    object Explore : Screen("explore", "Explore")
    object Files : Screen("files", "Files")
    object More : Screen("more", "More")
    object Settings : Screen("settings", "Settings")
    object NotebookDetail : Screen("notebook/{id}", "Notebook")
    object NotebookEditor : Screen("notebook/edit/{id}", "Editor")
    object RunMonitor : Screen("run/{runId}", "Run Monitor")
}
