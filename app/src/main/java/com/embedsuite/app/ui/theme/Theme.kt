package com.embedsuite.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val HackerColorScheme = darkColorScheme(
    primary = MatrixGreen,
    onPrimary = BlackAMOLED,
    primaryContainer = GlassGreen,
    onPrimaryContainer = MatrixGreen,
    secondary = NeonCyan,
    onSecondary = BlackAMOLED,
    secondaryContainer = GlassCyan,
    onSecondaryContainer = NeonCyan,
    tertiary = KaliBlue,
    onTertiary = BlackAMOLED,
    tertiaryContainer = GlassBlue,
    onTertiaryContainer = KaliBlue,
    background = BlackAMOLED,
    onBackground = MatrixGreen,
    surface = DarkSurface,
    onSurface = MatrixGreen,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextGray,
    error = NeonRed,
    onError = BlackAMOLED,
    outline = GlassWhiteBorder,
    outlineVariant = TextMuted
)

/**
 * Tema fijo AMOLED/cyberpunk (no sigue isSystemInDarkTheme).
 * Intencional: la identidad visual de EMBED SUITE es dark-only.
 */
@Composable
fun EMBEDSUITETheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = BlackAMOLED.toArgb()
            window.navigationBarColor = BlackAMOLED.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = HackerColorScheme,
        typography = Typography,
        content = content
    )
}
