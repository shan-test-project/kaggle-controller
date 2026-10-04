package com.kagglecontroller.domain.model

import kotlinx.serialization.Serializable

data class UserAccount(val username: String, val userId: Long?, val active: Boolean)

data class Notebook(
    val ref: String,            // "owner/slug"
    val title: String,
    val owner: String,
    val slug: String,
    val language: String?,
    val kernelType: String?,    // "notebook" | "script"
    val lastRunTime: String?,
    val votes: Int?,
    val isPrivate: Boolean?,
) {
    val webUrl: String get() = "https://www.kaggle.com/code/$ref"
}

data class Page<T>(val items: List<T>, val nextPageToken: String?)

/** Mirrors Kaggle's KernelWorkerStatus enum (wire form is the SCREAMING_SNAKE_CASE name). */
enum class RunStatus(val label: String, val terminal: Boolean) {
    NEW_SCRIPT("New", false),
    QUEUED("Queued", false),
    RUNNING("Running", false),
    COMPLETE("Completed", true),
    ERROR("Failed", true),
    CANCEL_REQUESTED("Cancelling", false),
    CANCEL_ACKNOWLEDGED("Cancelled", true),
    UNKNOWN("Unknown", false);

    companion object {
        fun parse(raw: String?): RunStatus {
            val name = raw?.substringAfterLast('.')?.trim()?.uppercase().orEmpty()
            return entries.firstOrNull { it.name == name } ?: UNKNOWN
        }
    }
}

data class SessionStatus(val status: RunStatus, val failureMessage: String?)
data class OutputFile(val name: String, val url: String)
data class SessionOutput(val files: List<OutputFile>, val log: String, val nextPageToken: String?)

/** Generic row for datasets / models / competitions (we only show fields Kaggle actually returned). */
data class ResourceItem(
    val ref: String,
    val title: String,
    val subtitle: String?,
    val detail: String?,
    val webUrl: String,
)

data class NotebookSource(val text: String, val title: String?, val language: String?, val kernelType: String?)

// ---------- Local, persisted models ----------

@Serializable
enum class SyncState { LOCAL_ONLY, UNSAVED, SYNCING, SYNCED, FAILED }

@Serializable
data class LocalDraft(
    val id: String,
    val title: String,
    /** "owner/slug" once the notebook is tied to a Kaggle notebook; the slug part is what Kaggle uses. */
    val ref: String? = null,
    val kernelType: String = "notebook",   // "notebook" | "script"
    val language: String = "python",       // python | r | rmarkdown
    val text: String = "",
    val isPrivate: Boolean = true,
    val enableInternet: Boolean = false,
    /** "" = CPU. Otherwise a machine shape such as NvidiaTeslaT4 (see Kaggle CLI docs). */
    val machineShape: String = "",
    val datasetSources: List<String> = emptyList(),
    val competitionSources: List<String> = emptyList(),
    val modelSources: List<String> = emptyList(),
    val sessionTimeoutSeconds: Long = 0,
    val updatedAt: Long = System.currentTimeMillis(),
    val sync: SyncState = SyncState.LOCAL_ONLY,
    val lastPushedVersion: Long? = null,
)

@Serializable
data class RunRecord(
    val id: String,
    val ref: String,
    val title: String,
    val versionNumber: Long?,
    val startedAt: Long,
    val endedAt: Long? = null,
    val status: String = RunStatus.QUEUED.name,
    val machineShape: String = "",
    val failureMessage: String? = null,
) {
    val runStatus: RunStatus get() = RunStatus.parse(status)
    val durationMs: Long? get() = endedAt?.let { it - startedAt }
}
