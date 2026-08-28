package com.embedsuite.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.embedsuite.app.core.ThemeMode

private fun obscuroScheme() = darkColorScheme(
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
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextGray,
    error = NeonRed,
    onError = BlackAMOLED,
    outline = GlassWhiteBorder,
    outlineVariant = TextMuted
)

private fun diurnalScheme() = lightColorScheme(
    primary = DiurnalPalette.accentGreen,
    onPrimary = DiurnalPalette.onAccent,
    primaryContainer = Color(0xFFD0F5E4),
    onPrimaryContainer = Color(0xFF003822),
    secondary = DiurnalPalette.accentCyan,
    onSecondary = DiurnalPalette.onAccent,
    secondaryContainer = Color(0xFFD0EEF8),
    onSecondaryContainer = Color(0xFF003344),
    tertiary = KaliBlue,
    onTertiary = DiurnalPalette.onAccent,
    tertiaryContainer = Color(0xFFD6E4FF),
    onTertiaryContainer = Color(0xFF1A3A7A),
    background = DiurnalPalette.background,
    onBackground = DiurnalPalette.textPrimary,
    surface = DiurnalPalette.surface,
    onSurface = DiurnalPalette.textPrimary,
    surfaceVariant = DiurnalPalette.surfaceElevated,
    onSurfaceVariant = DiurnalPalette.textSecondary,
    error = DiurnalPalette.accentRed,
    onError = DiurnalPalette.onAccent,
    outline = DiurnalPalette.border,
    outlineVariant = DiurnalPalette.textMuted
)

@Composable
fun EMBEDSUITETheme(
    themeMode: ThemeMode = ThemeMode.OBSCURO,
    content: @Composable () -> Unit
) {
    val palette = if (themeMode.isDark) ObscuroPalette else DiurnalPalette
    val colorScheme = if (themeMode.isDark) obscuroScheme() else diurnalScheme()
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = palette.background.toArgb()
            window.navigationBarColor = palette.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !themeMode.isDark
                isAppearanceLightNavigationBars = !themeMode.isDark
            }
        }
    }

    CompositionLocalProvider(LocalEmbedPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

/** Convenience for previews — follows system when mode not fixed. */
@Composable
fun EMBEDSUITEThemeSystem(
    content: @Composable () -> Unit
) {
    EMBEDSUITETheme(
        themeMode = if (isSystemInDarkTheme()) ThemeMode.OBSCURO else ThemeMode.DIURNAL,
        content = content
    )
}
