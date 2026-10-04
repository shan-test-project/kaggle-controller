package com.kagglecontroller.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.kagglecontroller.data.local.ThemeMode

/** Palette: near-black navy, electric blue, cyan highlight (spec 63). */
object KC {
    val Ink = Color(0xFF070B16)
    val Surface = Color(0xFF0E1526)
    val SurfaceHigh = Color(0xFF152038)
    val Outline = Color(0xFF24304D)
    val Blue = Color(0xFF2F6BFF)
    val Cyan = Color(0xFF22D3EE)
    val Text = Color(0xFFE8EDF8)
    val TextDim = Color(0xFF8A97B5)
    val Success = Color(0xFF34D399)
    val Warning = Color(0xFFFBBF24)
    val Danger = Color(0xFFFF5C7A)
    val Black = Color(0xFF000000)
    val AmoledSurface = Color(0xFF080B12)
    val AmoledHigh = Color(0xFF10151F)
}

private fun darkScheme(amoled: Boolean) = darkColorScheme(
    primary = KC.Blue, onPrimary = Color.White,
    secondary = KC.Cyan, onSecondary = KC.Ink,
    tertiary = KC.Cyan,
    background = if (amoled) KC.Black else KC.Ink, onBackground = KC.Text,
    surface = if (amoled) KC.AmoledSurface else KC.Surface, onSurface = KC.Text,
    surfaceVariant = if (amoled) KC.AmoledHigh else KC.SurfaceHigh, onSurfaceVariant = KC.TextDim,
    surfaceContainer = if (amoled) KC.AmoledSurface else KC.Surface,
    surfaceContainerHigh = if (amoled) KC.AmoledHigh else KC.SurfaceHigh,
    outline = KC.Outline, outlineVariant = KC.Outline,
    error = KC.Danger,
)

private val lightScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF1F55E0), onPrimary = Color.White,
    secondary = Color(0xFF007C91),
    background = Color(0xFFF5F7FC), surface = Color.White,
    surfaceVariant = Color(0xFFE6ECF8), onSurfaceVariant = Color(0xFF475069),
    outline = Color(0xFFC5CEE3),
)

/** Sans for UI, monospace for anything that is code, a log or a number that should line up. */
private val typography = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.4).sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp),
)

val CodeFont = FontFamily.Monospace

private val shapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
)

@Composable
fun KaggleControllerTheme(mode: ThemeMode, dynamicColor: Boolean, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        else -> true
    }
    val ctx = LocalContext.current
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> darkScheme(amoled = mode == ThemeMode.AMOLED)
        else -> lightScheme
    }
    MaterialTheme(colorScheme = scheme, typography = typography, shapes = shapes, content = content)
}
