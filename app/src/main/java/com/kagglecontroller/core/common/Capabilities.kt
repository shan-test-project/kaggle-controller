package com.kagglecontroller.core.common

/**
 * Single source of truth for "can the app do this through Kaggle's public API?" (spec 35, 36, 66).
 *
 * Rule: Kaggle API first -> official website fallback second -> local enhancement third.
 * Every UI action asks this registry. If the status is SUPPORTED_BY_WEBSITE_ONLY the UI shows
 * "Open in Kaggle" instead of calling anything. See docs/API_RESEARCH.md for the evidence.
 */
enum class ApiSupport { SUPPORTED_BY_API, PARTIALLY_SUPPORTED, SUPPORTED_BY_WEBSITE_ONLY, UNSUPPORTED }

data class Capability(
    val id: String,
    val title: String,
    val support: ApiSupport,
    /** Is this wired up in the current app build (vs. documented API we have not built yet)? */
    val implementedInApp: Boolean,
    val note: String,
    /** Path appended to https://www.kaggle.com for the fallback. */
    val webPath: String = "",
)

object Capabilities {
    const val WEB_BASE = "https://www.kaggle.com"

    val all: List<Capability> = listOf(
        Capability("notebooks.list", "List and search notebooks", ApiSupport.SUPPORTED_BY_API, true,
            "kernels.KernelsApiService/ListKernels", "/code"),
        Capability("notebooks.pull", "Download notebook source", ApiSupport.SUPPORTED_BY_API, true,
            "GetKernel. The response shape is parsed defensively; falls back to Open in Kaggle.", "/code"),
        Capability("notebooks.push", "Save / push notebook", ApiSupport.SUPPORTED_BY_API, true,
            "SaveKernel with QUICK_SAVE (save without running)."),
        Capability("notebooks.run", "Run notebook", ApiSupport.SUPPORTED_BY_API, true,
            "SaveKernel with SAVE_AND_RUN_ALL. There is no separate run call; running creates a new version."),
        Capability("notebooks.status", "Run status", ApiSupport.SUPPORTED_BY_API, true,
            "GetKernelSessionStatus (QUEUED, RUNNING, COMPLETE, ERROR, CANCEL_*)."),
        Capability("notebooks.logs", "Run log", ApiSupport.PARTIALLY_SUPPORTED, true,
            "ListKernelSessionOutput returns the captured log. It is a snapshot, not a documented live stream, so the monitor polls with backoff."),
        Capability("notebooks.outputs", "Run output files", ApiSupport.SUPPORTED_BY_API, true,
            "ListKernelSessionOutput returns signed download URLs."),
        Capability("notebooks.delete", "Delete notebook", ApiSupport.SUPPORTED_BY_API, true, "DeleteKernel."),
        Capability("notebooks.versions", "Open a specific version's source or output", ApiSupport.PARTIALLY_SUPPORTED, false,
            "versionLabel (for example \"v2\") is accepted by GetKernel and ListKernelSessionOutput. No documented version-list call was found: use Open in Kaggle.", "/code"),
        Capability("notebooks.cancel", "Cancel a running notebook", ApiSupport.SUPPORTED_BY_WEBSITE_ONLY, true,
            "Status values CANCEL_REQUESTED and CANCEL_ACKNOWLEDGED exist, but no public cancel call is documented.", "/code"),
        Capability("schedules.server", "Kaggle server-side schedules", ApiSupport.SUPPORTED_BY_WEBSITE_ONLY, true,
            "Not present in the public CLI or SDK. Manage it in the notebook's Schedule panel on the website.", "/code"),
        Capability("schedules.local", "Android local schedule", ApiSupport.UNSUPPORTED, false,
            "Planned: WorkManager triggers SaveKernel from the phone. Best effort only (Doze, network, battery)."),
        Capability("secrets", "Notebook secrets and add-ons", ApiSupport.SUPPORTED_BY_WEBSITE_ONLY, true,
            "Kaggle documents that secrets have no CLI support.", "/code"),
        Capability("datasets.browse", "Search and browse datasets", ApiSupport.SUPPORTED_BY_API, true,
            "datasets.DatasetApiService/ListDatasets", "/datasets"),
        Capability("datasets.create", "Create dataset, new version, upload", ApiSupport.PARTIALLY_SUPPORTED, false,
            "API exists (CreateDataset, CreateDatasetVersion plus signed-URL upload). Not built yet.", "/datasets"),
        Capability("models.browse", "Search and browse models", ApiSupport.SUPPORTED_BY_API, true,
            "models.ModelApiService/ListModels", "/models"),
        Capability("competitions.browse", "Browse competitions", ApiSupport.SUPPORTED_BY_API, true,
            "competitions.CompetitionApiService/ListCompetitions", "/competitions"),
        Capability("competitions.submit", "Submit predictions", ApiSupport.PARTIALLY_SUPPORTED, false,
            "The CLI supports it. The app will require an explicit confirmation dialog before sending.", "/competitions"),
        Capability("forums", "Discussions", ApiSupport.PARTIALLY_SUPPORTED, false,
            "The CLI documents read-only forum browsing. Posting and replying is not documented.", "/discussions"),
        Capability("benchmarks", "Benchmarks", ApiSupport.PARTIALLY_SUPPORTED, false,
            "The CLI documents benchmark tasks. Not built yet.", "/benchmarks"),
        Capability("ai", "AI assistant", ApiSupport.UNSUPPORTED, false,
            "Local feature. Never sends code anywhere until the user configures a provider."),
    )

    fun byId(id: String): Capability = all.first { it.id == id }

    fun webUrl(path: String): String = WEB_BASE + path
}
