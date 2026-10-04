package com.kagglecontroller.data.kaggle

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

/**
 * Kaggle does not publish a response schema for every RPC, so list/detail responses are read
 * defensively: we try a few key names and never fabricate a value that isn't there.
 */
internal fun JsonObject.str(vararg keys: String): String? {
    for (k in keys) {
        val v = (this[k] as? JsonPrimitive)?.contentOrNull
        if (!v.isNullOrBlank()) return v
    }
    return null
}

internal fun JsonObject.int(vararg keys: String): Int? {
    for (k in keys) {
        val p = this[k] as? JsonPrimitive ?: continue
        p.intOrNull?.let { return it }
        p.contentOrNull?.toIntOrNull()?.let { return it }
    }
    return null
}

internal fun JsonObject.long(vararg keys: String): Long? {
    for (k in keys) {
        val p = this[k] as? JsonPrimitive ?: continue
        p.longOrNull?.let { return it }
        p.contentOrNull?.toLongOrNull()?.let { return it }
    }
    return null
}

internal fun JsonObject.bool(vararg keys: String): Boolean? {
    for (k in keys) {
        (this[k] as? JsonPrimitive)?.booleanOrNull?.let { return it }
    }
    return null
}

/** First array of objects found under any of [preferredKeys], else under any key at all. */
internal fun JsonObject.objectList(vararg preferredKeys: String): List<JsonObject> {
    for (k in preferredKeys) {
        val arr = this[k] as? JsonArray
        if (arr != null) return arr.filterIsInstance<JsonObject>()
    }
    for ((_, v) in this) {
        if (v is JsonArray && v.isNotEmpty() && v.first() is JsonObject) return v.filterIsInstance<JsonObject>()
    }
    return emptyList()
}

/** Depth-limited search for the first string value stored under one of [keys]. */
internal fun JsonElement.findString(vararg keys: String, depth: Int = 4): String? {
    if (depth < 0) return null
    when (this) {
        is JsonObject -> {
            for (k in keys) {
                val v = (this[k] as? JsonPrimitive)?.contentOrNull
                if (v != null) return v
            }
            for ((_, child) in this) child.findString(*keys, depth = depth - 1)?.let { return it }
        }
        is JsonArray -> for (child in this) child.findString(*keys, depth = depth - 1)?.let { return it }
        else -> Unit
    }
    return null
}

internal fun normalizeRef(raw: String?, marker: String): String? {
    if (raw.isNullOrBlank()) return null
    val idx = raw.indexOf(marker)
    return (if (idx >= 0) raw.substring(idx + marker.length) else raw).trim('/').ifBlank { null }
}
