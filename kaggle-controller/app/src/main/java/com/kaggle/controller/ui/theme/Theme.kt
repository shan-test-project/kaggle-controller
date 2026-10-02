package com.kaggle.controller.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun KaggleColorScheme(darkTheme: Boolean = true): ColorScheme {
    return ColorScheme(
        primary = ElectricCyan,
        onPrimary = Color.Black,
        primaryContainer = Color(0xFF003F5A),
        onPrimaryContainer = ElectricCyan,
        secondary = CyanHighlight,
        onSecondary = Color.Black,
        secondaryContainer = Color(0xFF003A3A),
        onSecondaryContainer = CyanHighlight,
        tertiary = AccentBlue,
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFF002A50),
        onTertiaryContainer = AccentBlue,
        error = ErrorRed,
        onError = Color.White,
        errorContainer = Color(0xFF3A0A0A),
        onErrorContainer = ErrorRed,
        background = DeepNavy,
        onBackground = TextPrimary,
        surface = SurfaceDark,
        onSurface = TextPrimary,
        surfaceVariant = SurfaceElevated,
        onSurfaceVariant = TextSecondary,
        outline = BorderMedium,
        outlineVariant = BorderSubtle,
        inversePrimary = AccentBlue,
        inverseSurface = TextPrimary,
        inverseOnSurface = DeepNavy,
        surfaceTint = ElectricCyan,
        scrim = Color.Black.copy(alpha = 0.4f),
    )
}
