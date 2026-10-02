package com.kaggle.controller.domain.model

data class UserAccount(
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val tier: String = "Standard",
)

data class Notebook(
    val id: String,
    val title: String,
    val owner: String,
    val language: String,
    val lastUpdated: String,
    val lastRun: String? = null,
    val runStatus: String = "None",
    val computeType: String = "None",
    val isFavorite: Boolean = false,
    val hasLocalChanges: Boolean = false,
    val schedule: String? = null,
)

data class NotebookRun(
    val id: String,
    val notebookId: String,
    val versionId: String? = null,
    val status: String,
    val startTime: String,
    val endTime: String? = null,
    val durationMs: Long? = null,
    val computeType: String,
    val accelerator: String? = null,
)

data class Schedule(
    val id: String,
    val notebookId: String,
    val trigger: String,
    val nextRun: String,
    val lastRun: String? = null,
    val status: String,
)
