package com.kagglecontroller.feature.editor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kagglecontroller.core.ui.theme.CodeFont

@Composable
fun rememberPalette(): SyntaxPalette {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return remember(dark) {
        if (dark) SyntaxPalette(Color(0xFF82AAFF), Color(0xFFA5E3B0), Color(0xFF66749A), Color(0xFFFFB86B), Color(0xFF22D3EE), Color(0xFFE8EDF8))
        else SyntaxPalette(Color(0xFF1F55E0), Color(0xFF1B7F4A), Color(0xFF7B879E), Color(0xFFB45309), Color(0xFF007C91), Color(0xFF121826))
    }
}

/**
 * Editable code view: monospace, optional line-number gutter, regex syntax highlighting.
 * With word wrap on, the gutter is hidden because wrapped lines would no longer line up.
 */
@Composable
fun CodeField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    language: String,
    fontSizeSp: Int,
    wrap: Boolean,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    showLineNumbers: Boolean = true,
) {
    val palette = rememberPalette()
    val lineHeight = (fontSizeSp * 1.45f).sp
    val style = TextStyle(fontFamily = CodeFont, fontSize = fontSizeSp.sp, lineHeight = lineHeight, color = palette.plain)
    val transformation = remember(language, palette) {
        VisualTransformation { text ->
            TransformedText(Highlighter.highlight(text.text, language, palette), OffsetMapping.Identity)
        }
    }
    val lineCount = remember(value.text) { value.text.count { it == '\n' } + 1 }
    val gutter = remember(lineCount) { (1..lineCount).joinToString("\n") }
    val hScroll = rememberScrollState()

    Row(modifier, verticalAlignment = Alignment.Top) {
        if (showLineNumbers && !wrap) {
            Text(
                gutter,
                style = style.copy(color = palette.comment, textAlign = TextAlign.End),
                modifier = Modifier.padding(end = 10.dp),
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = style,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.secondary),
            visualTransformation = transformation,
            modifier = Modifier
                .weight(1f)
                .let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
                .let { if (!wrap) it.horizontalScroll(hScroll) else it },
        )
    }
}
