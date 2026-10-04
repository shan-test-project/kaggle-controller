package com.kagglecontroller.data.kaggle

import com.kagglecontroller.core.network.KaggleException
import com.kagglecontroller.core.network.KaggleRpcClient
import com.kagglecontroller.core.security.SecureTokenStore
import com.kagglecontroller.domain.model.LocalDraft
import com.kagglecontroller.domain.model.Notebook
import com.kagglecontroller.domain.model.NotebookSource
import com.kagglecontroller.domain.model.OutputFile
import com.kagglecontroller.domain.model.Page
import com.kagglecontroller.domain.model.ResourceItem
import com.kagglecontroller.domain.model.RunStatus
import com.kagglecontroller.domain.model.SessionOutput
import com.kagglecontroller.domain.model.SessionStatus
import com.kagglecontroller.domain.model.UserAccount
import com.kagglecontroller.domain.repository.KaggleAuthRepository
import com.kagglecontroller.domain.repository.KaggleNotebookRepository
import com.kagglecontroller.domain.repository.KaggleResourceRepository
import com.kagglecontroller.domain.repository.ResourceKind
import com.kagglecontroller.domain.repository.SaveResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Service names and methods in ONE place. Source: docs/API_RESEARCH.md. */
internal object Rpc {
    const val KERNELS = "kernels.KernelsApiService"
    const val DATASETS = "datasets.DatasetApiService"
    const val MODELS = "models.ModelApiService"
    const val COMPETITIONS = "competitions.CompetitionApiService"
    const val OAUTH = "security.OAuthService"
}

class AuthRepositoryImpl(
    private val rpc: KaggleRpcClient,
    private val store: SecureTokenStore,
) : KaggleAuthRepository {
    private val _account = MutableStateFlow<UserAccount?>(null)
    override val account: StateFlow<UserAccount?> = _account.asStateFlow()

    override fun hasToken(): Boolean = store.load() != null

    override suspend fun signIn(token: String): UserAccount {
        val clean = token.trim()
        require(clean.isNotEmpty()) { "Paste your Kaggle API token first." }
        // Verify BEFORE persisting. Temporarily store so the shared client can authenticate,
        // and roll back on failure.
        val previous = store.load()
        store.save(clean)
        try {
            val user = introspect(clean)
            _account.value = user
            return user
        } catch (e: Exception) {
            if (previous != null) store.save(previous) else store.clear()
            throw e
        }
    }

    override suspend fun restore(): UserAccount? {
        val token = store.load() ?: return null
        return try {
            introspect(token).also { _account.value = it }
        } catch (e: KaggleException.Unauthorized) {
            store.clear(); _account.value = null; null
        }
    }

    override fun signOut() {
        store.clear()
        _account.value = null
    }

    private suspend fun introspect(token: String): UserAccount {
        val res = rpc.write(Rpc.OAUTH, "IntrospectToken", buildJsonObject { put("token", token) })
        val active = res.bool("active") ?: true
        val username = res.str("username", "userName")
            ?: throw KaggleException.Unauthorized()
        if (!active) throw KaggleException.Unauthorized()
        return UserAccount(username, res.long("userId", "id"), active)
    }
}

class NotebookRepositoryImpl(private val rpc: KaggleRpcClient) : KaggleNotebookRepository {

    override suspend fun list(search: String?, user: String?, language: String?, pageToken: String?, pageSize: Int, force: Boolean): Page<Notebook> {
        val body = buildJsonObject {
            if (!search.isNullOrBlank()) put("search", search)
            if (!user.isNullOrBlank()) put("user", user)
            if (!language.isNullOrBlank()) put("language", language)
            put("pageSize", pageSize)
            if (!pageToken.isNullOrBlank()) put("pageToken", pageToken)
        }
        val res = rpc.read(Rpc.KERNELS, "ListKernels", body, forceRefresh = force)
        val items = res.objectList("kernels", "items").mapNotNull { k ->
            val ref = normalizeRef(k.str("ref", "url", "slug"), "/code/") ?: return@mapNotNull null
            val owner = ref.substringBefore('/', missingDelimiterValue = k.str("author", "authorUserName", "ownerSlug").orEmpty())
            Notebook(
                ref = ref,
                title = k.str("title", "name") ?: ref.substringAfter('/'),
                owner = k.str("author", "authorUserName", "ownerSlug") ?: owner,
                slug = ref.substringAfter('/'),
                language = k.str("language"),
                kernelType = k.str("kernelType"),
                lastRunTime = k.str("lastRunTime", "lastRun"),
                votes = k.int("totalVotes", "votes"),
                isPrivate = k.bool("isPrivate"),
            )
        }
        return Page(items, res.str("nextPageToken"))
    }

    override suspend fun pull(ref: String, versionLabel: String?): NotebookSource {
        val (owner, slug) = split(ref)
        val body = buildJsonObject {
            put("userName", owner); put("kernelSlug", slug)
            if (!versionLabel.isNullOrBlank()) put("versionLabel", versionLabel)
        }
        val res = rpc.read("kernels.KernelsApiService", "GetKernel", body, cacheTtlMs = 0)
        // Documented request; response shape is not published, so look for the source defensively.
        val text = res.findString("source", "text", "sourceText")
            ?: throw KaggleException.Parse("Kaggle did not return the notebook source in a known field. Use Open in Kaggle.")
        return NotebookSource(
            text = text,
            title = res.findString("title"),
            language = res.findString("language"),
            kernelType = res.findString("kernelType"),
        )
    }

    override suspend fun push(draft: LocalDraft, run: Boolean): SaveResult {
        val ref = draft.ref ?: throw IllegalArgumentException("Choose an owner/slug for this notebook before pushing.")
        val body = buildJsonObject {
            put("slug", ref)
            put("newTitle", draft.title)
            put("text", draft.text)
            put("language", draft.language)
            put("kernelType", draft.kernelType)
            put("isPrivate", draft.isPrivate)
            put("enableInternet", draft.enableInternet)
            if (draft.machineShape.isNotBlank()) put("machineShape", draft.machineShape)
            if (draft.sessionTimeoutSeconds > 0) put("sessionTimeoutSeconds", draft.sessionTimeoutSeconds)
            put("kernelExecutionType", if (run) "SAVE_AND_RUN_ALL" else "QUICK_SAVE")
            put("datasetDataSources", JsonArray(draft.datasetSources.map { JsonPrimitive(it) }))
            put("competitionDataSources", JsonArray(draft.competitionSources.map { JsonPrimitive(it) }))
            put("modelDataSources", JsonArray(draft.modelSources.map { JsonPrimitive(it) }))
        }
        val res = rpc.write(Rpc.KERNELS, "SaveKernel", body)
        // A 200 response can still be a rejection (see Kaggle SDK notes): check error + invalid sources.
        res.str("error")?.let { throw KaggleException.Http(400, it) }
        val invalid = listOf("invalidTags", "invalidDatasetSources", "invalidCompetitionSources", "invalidKernelSources", "invalidModelSources")
            .flatMap { key -> ((res[key] as? JsonArray).orEmpty()).mapNotNull { (it as? JsonPrimitive)?.content } }
        if (invalid.isNotEmpty()) throw KaggleException.Http(400, "Kaggle rejected these sources: ${invalid.joinToString()}")
        return SaveResult(res.long("versionNumber"), res.str("url"), res.str("ref"))
    }

    override suspend fun status(ref: String): SessionStatus {
        val (owner, slug) = split(ref)
        val res = rpc.read(Rpc.KERNELS, "GetKernelSessionStatus",
            buildJsonObject { put("userName", owner); put("kernelSlug", slug) }, cacheTtlMs = 0)
        return SessionStatus(RunStatus.parse(res.str("status")), res.str("failureMessage"))
    }

    override suspend fun output(ref: String, pageToken: String?, versionLabel: String?): SessionOutput {
        val (owner, slug) = split(ref)
        val res = rpc.read(Rpc.KERNELS, "ListKernelSessionOutput", buildJsonObject {
            put("userName", owner); put("kernelSlug", slug); put("pageSize", 50)
            if (!pageToken.isNullOrBlank()) put("pageToken", pageToken)
            if (!versionLabel.isNullOrBlank()) put("versionLabel", versionLabel)
        }, cacheTtlMs = 0)
        val files = ((res["files"] as? JsonArray).orEmpty()).filterIsInstance<JsonObject>().mapNotNull {
            val url = it.str("url") ?: return@mapNotNull null
            OutputFile(it.str("fileName", "name") ?: "file", url)
        }
        return SessionOutput(files, res.str("log").orEmpty(), res.str("nextPageToken"))
    }

    override suspend fun delete(ref: String) {
        val (owner, slug) = split(ref)
        rpc.write(Rpc.KERNELS, "DeleteKernel", buildJsonObject { put("userName", owner); put("kernelSlug", slug) })
    }

    private fun split(ref: String): Pair<String, String> {
        val parts = ref.trim('/').split('/')
        require(parts.size >= 2) { "Notebook reference must look like owner/slug." }
        return parts[0] to parts[1]
    }
}

class ResourceRepositoryImpl(private val rpc: KaggleRpcClient) : KaggleResourceRepository {
    override suspend fun list(kind: ResourceKind, search: String?, pageToken: String?, pageSize: Int, force: Boolean): Page<ResourceItem> {
        val (service, method, marker, listKey) = when (kind) {
            ResourceKind.DATASETS -> Quad(Rpc.DATASETS, "ListDatasets", "/datasets/", "datasets")
            ResourceKind.MODELS -> Quad(Rpc.MODELS, "ListModels", "/models/", "models")
            ResourceKind.COMPETITIONS -> Quad(Rpc.COMPETITIONS, "ListCompetitions", "/competitions/", "competitions")
        }
        val body = buildJsonObject {
            if (!search.isNullOrBlank()) put("search", search)
            put("pageSize", pageSize)
            if (!pageToken.isNullOrBlank()) put("pageToken", pageToken)
        }
        val res = rpc.read(service, method, body, forceRefresh = force)
        val items = res.objectList(listKey, "items").mapNotNull { o ->
            val rawRef = o.str("ref", "url", "slug", "id")
            val ref = normalizeRef(rawRef, marker) ?: return@mapNotNull null
            ResourceItem(
                ref = ref,
                title = o.str("title", "name", "displayName") ?: ref,
                subtitle = o.str("subtitle", "ownerName", "creatorName", "organizationName", "category"),
                detail = o.str("description", "briefDescription", "reward", "lastUpdated", "deadline"),
                webUrl = o.str("url")?.takeIf { it.startsWith("http") } ?: ("https://www.kaggle.com$marker$ref"),
            )
        }
        return Page(items, res.str("nextPageToken"))
    }

    private data class Quad(val service: String, val method: String, val marker: String, val listKey: String)
}
