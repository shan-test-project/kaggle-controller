package com.kagglecontroller.domain.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import java.util.UUID

enum class CellType(val label: String) { CODE("Code"), MARKDOWN("Markdown") }

data class Cell(val id: String = UUID.randomUUID().toString(), val type: CellType, val source: String)

/**
 * In-memory document model shared by the notebook editor and script editor.
 * A script is simply a document with exactly one code cell and is serialized as raw text.
 */
object NotebookDoc {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; prettyPrint = true }

    fun parse(text: String, kernelType: String): List<Cell> {
        if (kernelType != "notebook") return listOf(Cell(type = CellType.CODE, source = text))
        if (text.isBlank()) return listOf(Cell(type = CellType.CODE, source = ""))
        return try {
            val root = json.parseToJsonElement(text) as JsonObject
            val cells = (root["cells"] as? JsonArray).orEmpty().mapNotNull { el ->
                val obj = el as? JsonObject ?: return@mapNotNull null
                val type = when ((obj["cell_type"] as? JsonPrimitive)?.contentOrNull) {
                    "markdown" -> CellType.MARKDOWN
                    "code" -> CellType.CODE
                    else -> return@mapNotNull null // raw cells are left untouched by the editor
                }
                Cell(type = type, source = sourceToString(obj["source"]))
            }
            cells.ifEmpty { listOf(Cell(type = CellType.CODE, source = "")) }
        } catch (e: Exception) {
            // Not valid ipynb JSON: keep the text rather than lose it.
            listOf(Cell(type = CellType.CODE, source = text))
        }
    }

    fun serialize(cells: List<Cell>, kernelType: String, language: String): String {
        if (kernelType != "notebook") return cells.firstOrNull()?.source.orEmpty()
        val isR = language.equals("r", ignoreCase = true)
        val root = buildJsonObject {
            put("nbformat", 4)
            put("nbformat_minor", 4)
            put("metadata", buildJsonObject {
                put("kernelspec", buildJsonObject {
                    put("display_name", if (isR) "R" else "Python 3")
                    put("language", if (isR) "R" else "python")
                    put("name", if (isR) "ir" else "python3")
                })
            })
            put("cells", buildJsonArray {
                cells.forEach { cell ->
                    add(buildJsonObject {
                        put("cell_type", if (cell.type == CellType.CODE) "code" else "markdown")
                        put("metadata", buildJsonObject { })
                        if (cell.type == CellType.CODE) {
                            put("execution_count", JsonNull)
                            put("outputs", buildJsonArray { })
                        }
                        put("source", linesOf(cell.source))
                    })
                }
            })
        }
        return json.encodeToString(JsonElement.serializer(), root)
    }

    private fun sourceToString(el: JsonElement?): String = when (el) {
        is JsonArray -> el.joinToString("") { (it as? JsonPrimitive)?.contentOrNull.orEmpty() }
        is JsonPrimitive -> el.contentOrNull.orEmpty()
        else -> ""
    }

    /** ipynb stores source as a list of lines that keep their trailing newline. */
    private fun linesOf(text: String): JsonArray = buildJsonArray {
        if (text.isEmpty()) return@buildJsonArray
        val parts = text.split("\n")
        parts.forEachIndexed { i, line ->
            val last = i == parts.lastIndex
            if (!(last && line.isEmpty())) add(JsonPrimitive(if (last) line else line + "\n"))
        }
    }
}
