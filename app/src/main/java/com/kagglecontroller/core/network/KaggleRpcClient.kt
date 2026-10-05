package com.kagglecontroller.core.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.LinkedHashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Minimal client for Kaggle's Connect-style JSON-RPC API.
 *
 *   POST https://api.kaggle.com/v1/{service}/{method}
 *   Authorization: Bearer KGAT_...
 *   Content-Type: application/json
 *
 * Built-in protections (spec 39): request de-duplication, short TTL cache, exponential backoff
 * on 429/5xx for idempotent reads only, Retry-After support, and no automatic retries for writes.
 * The token only ever goes into the Authorization header: it is never logged or put in exceptions.
 */
class KaggleRpcClient(
    private val tokenProvider: () -> String?,
    private val http: OkHttpClient = defaultHttp(),
    private val baseUrl: String = "https://api.kaggle.com",
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val inFlight = ConcurrentHashMap<String, Deferred<JsonObject>>()
    private val cache = BoundedLruCache<String, CacheEntry>(48) // bounded cache for low-end phones

    private class CacheEntry(val at: Long, val value: JsonObject)

    /** Read call: de-duplicated, cached for [cacheTtlMs], retried with backoff. */
    suspend fun read(
        service: String,
        method: String,
        body: JsonObject = JsonObject(emptyMap()),
        cacheTtlMs: Long = 20_000,
        forceRefresh: Boolean = false,
    ): JsonObject {
        val key = "$service/$method:$body"
        if (!forceRefresh && cacheTtlMs > 0) {
            val hit = cache.get(key)
            if (hit != null && System.currentTimeMillis() - hit.at < cacheTtlMs) return hit.value
        }
        val deferred = inFlight.getOrPut(key) {
            scope.async {
                try {
                    val result = execWithRetry(service, method, body, maxAttempts = 3)
                    cache.put(key, CacheEntry(System.currentTimeMillis(), result))
                    result
                } finally {
                    inFlight.remove(key)
                }
            }
        }
        return deferred.await()
    }

    /** Write call: single attempt, never retried automatically, never cached. */
    suspend fun write(service: String, method: String, body: JsonObject): JsonObject {
        cache.clear()
        return execWithRetry(service, method, body, maxAttempts = 1)
    }

    /** Open a pre-signed URL. No Authorization header is sent (matches the official SDK). */
    suspend fun openSigned(url: String): Response {
        val request = Request.Builder().url(url).get().build()
        val response = try {
            http.newCall(request).await()
        } catch (e: IOException) {
            throw KaggleException.Offline(e)
        }
        if (!response.isSuccessful) {
            val code = response.code
            response.close()
            throw KaggleException.Http(code, "Download failed (HTTP $code).")
        }
        return response
    }

    private suspend fun execWithRetry(service: String, method: String, body: JsonObject, maxAttempts: Int): JsonObject {
        var attempt = 0
        var wait = 1_000L
        while (true) {
            attempt++
            try {
                return exec(service, method, body)
            } catch (e: KaggleException.RateLimited) {
                if (attempt >= maxAttempts) throw e
                delay(((e.retryAfterSeconds ?: 0) * 1000L).coerceAtLeast(wait))
            } catch (e: KaggleException.Http) {
                if (attempt >= maxAttempts || e.code < 500) throw e
                delay(wait)
            } catch (e: KaggleException.Offline) {
                if (attempt >= maxAttempts) throw e
                delay(wait)
            }
            wait = (wait * 2).coerceAtMost(8_000L)
        }
    }

    private suspend fun exec(service: String, method: String, body: JsonObject): JsonObject {
        val token = tokenProvider() ?: throw KaggleException.Unauthorized()
        val request = Request.Builder()
            .url("$baseUrl/v1/$service/$method")
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .post(body.toString().toRequestBody(JSON))
            .build()
        val response = try {
            http.newCall(request).await()
        } catch (e: IOException) {
            throw KaggleException.Offline(e)
        }
        response.use { r ->
            val text = r.body?.string().orEmpty()
            if (r.isSuccessful) {
                if (text.isBlank()) return JsonObject(emptyMap())
                return try {
                    val element = json.parseToJsonElement(text)
                    element as? JsonObject ?: JsonObject(mapOf("value" to element))
                } catch (e: Exception) {
                    throw KaggleException.Parse("Could not read Kaggle's response.")
                }
            }
            val message = extractMessage(text)
            throw when (r.code) {
                401 -> KaggleException.Unauthorized()
                403 -> KaggleException.Forbidden(message)
                404 -> KaggleException.NotFound(message)
                429 -> KaggleException.RateLimited(r.header("Retry-After")?.toIntOrNull())
                else -> KaggleException.Http(r.code, message)
            }
        }
    }

    private fun extractMessage(text: String): String? = try {
        val obj = json.parseToJsonElement(text) as? JsonObject
        (obj?.get("message") as? JsonPrimitive)?.contentOrNull
    } catch (e: Exception) {
        null
    }

    companion object {
        private val JSON = "application/json".toMediaType()

        fun defaultHttp(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }
}

private suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
    cont.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (!cont.isCancelled) cont.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            cont.resume(response)
        }
    })
}

/** Small synchronized LRU cache that also works in local JVM unit tests. */
private class BoundedLruCache<K : Any, V : Any>(private val maxSize: Int) {
    private val entries = LinkedHashMap<K, V>(maxSize, 0.75f, true)

    init {
        require(maxSize > 0)
    }

    @Synchronized
    fun get(key: K): V? = entries[key]

    @Synchronized
    fun put(key: K, value: V) {
        entries[key] = value
        if (entries.size > maxSize) {
            entries.remove(entries.entries.iterator().next().key)
        }
    }

    @Synchronized
    fun clear() {
        entries.clear()
    }
}
