package com.kagglecontroller.domain.repository

import com.kagglecontroller.domain.model.LocalDraft
import com.kagglecontroller.domain.model.NotebookSource
import com.kagglecontroller.domain.model.Notebook
import com.kagglecontroller.domain.model.Page
import com.kagglecontroller.domain.model.ResourceItem
import com.kagglecontroller.domain.model.SessionOutput
import com.kagglecontroller.domain.model.SessionStatus
import com.kagglecontroller.domain.model.UserAccount
import kotlinx.coroutines.flow.StateFlow

interface KaggleAuthRepository {
    val account: StateFlow<UserAccount?>
    fun hasToken(): Boolean
    /** Verifies the token with Kaggle first and stores it only if Kaggle accepts it. */
    suspend fun signIn(token: String): UserAccount
    suspend fun restore(): UserAccount?
    fun signOut()
}

data class SaveResult(val versionNumber: Long?, val url: String?, val ref: String?)

interface KaggleNotebookRepository {
    suspend fun list(search: String?, user: String?, language: String?, pageToken: String?, pageSize: Int = 20, force: Boolean = false): Page<Notebook>
    suspend fun pull(ref: String, versionLabel: String? = null): NotebookSource
    /** run = true -> save AND run; run = false -> quick save only. */
    suspend fun push(draft: LocalDraft, run: Boolean): SaveResult
    suspend fun status(ref: String): SessionStatus
    suspend fun output(ref: String, pageToken: String? = null, versionLabel: String? = null): SessionOutput
    suspend fun delete(ref: String)
}

enum class ResourceKind(val title: String, val webPath: String) {
    DATASETS("Datasets", "/datasets"),
    MODELS("Models", "/models"),
    COMPETITIONS("Competitions", "/competitions"),
}

/** Datasets, models and competitions share the same read-only browse shape for now. */
interface KaggleResourceRepository {
    suspend fun list(kind: ResourceKind, search: String?, pageToken: String?, pageSize: Int = 20, force: Boolean = false): Page<ResourceItem>
}
