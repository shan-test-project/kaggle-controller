package com.kagglecontroller.feature.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight

/** Lightweight regex highlighter for Python and R. Skipped on huge files to keep typing smooth. */
class SyntaxPalette(
    val keyword: Color, val string: Color, val comment: Color, val number: Color, val builtin: Color, val plain: Color,
)

object Highlighter {
    const val MAX_CHARS = 40_000

    private val pyKeywords = setOf(
        "False", "None", "True", "and", "as", "assert", "async", "await", "break", "class", "continue", "def", "del",
        "elif", "else", "except", "finally", "for", "from", "global", "if", "import", "in", "is", "lambda",
        "nonlocal", "not", "or", "pass", "raise", "return", "try", "while", "with", "yield",
    )
    private val pyBuiltins = setOf(
        "print", "len", "range", "int", "str", "float", "list", "dict", "set", "tuple", "open", "enumerate", "zip",
        "map", "filter", "sum", "min", "max", "abs", "sorted", "isinstance", "super", "self", "type", "bool",
    )
    private val rKeywords = setOf(
        "function", "if", "else", "for", "while", "repeat", "break", "next", "return", "TRUE", "FALSE", "NULL",
        "NA", "in", "library", "require",
    )

    private val pattern = Regex(
        "(#[^\\n]*)" +                                                       // 1 comment
            "|(\"\"\"[\\s\\S]*?\"\"\"|'''[\\s\\S]*?'''" +                    // 2 strings: triple
            "|\"(?:\\\\.|[^\"\\\\\\n])*\"|'(?:\\\\.|[^'\\\\\\n])*')" +         //            single line
            "|(\\b\\d+(?:\\.\\d+)?\\b)" +                                    // 3 number
            "|([A-Za-z_][A-Za-z0-9_.]*)"                                     // 4 identifier
    )

    fun highlight(text: String, language: String, palette: SyntaxPalette): AnnotatedString {
        if (text.length > MAX_CHARS) return AnnotatedString(text)
        val keywords = if (language.equals("r", true) || language.equals("rmarkdown", true)) rKeywords else pyKeywords
        return buildAnnotatedString {
            append(text)
            for (m in pattern.findAll(text)) {
                val style: SpanStyle? = when {
                    m.groups[1] != null -> SpanStyle(color = palette.comment)
                    m.groups[2] != null -> SpanStyle(color = palette.string)
                    m.groups[3] != null -> SpanStyle(color = palette.number)
                    else -> {
                        val word = m.value
                        when {
                            word in keywords -> SpanStyle(color = palette.keyword, fontWeight = FontWeight.SemiBold)
                            word in pyBuiltins -> SpanStyle(color = palette.builtin)
                            else -> null
                        }
                    }
                }
                if (style != null) addStyle(style, m.range.first, m.range.last + 1)
            }
        }
    }
}
