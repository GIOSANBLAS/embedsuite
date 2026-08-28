package com.embedsuite.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ── Accent spectrum (shared) ──
val MatrixGreen = Color(0xFF00FF88)
val MatrixGreenDim = Color(0xFF00CC6A)
val NeonCyan = Color(0xFF00D4FF)
val NeonOrange = Color(0xFFFF6600)
val NeonRed = Color(0xFFFF0033)
val NeonPurple = Color(0xFFBB86FC)
val KaliBlue = Color(0xFF367BF0)
val KaliBlueDark = Color(0xFF1A3A7A)

// ── Legacy aliases (dark defaults) ──
val EmbedBg = Color(0xFF0D0D0D)
val EmbedGreen = MatrixGreen
val EmbedCyan = NeonCyan
val BlackAMOLED = Color(0xFF0D0D0D)
val DarkSurface = Color(0xFF121212)
val DarkSurfaceElevated = Color(0xFF1A1A1A)
val TextGray = Color(0xFF889988)
val TextMuted = Color(0xFF556655)
val TextPrimary = Color(0xFFE6E6E6)

// ── Liquid Glass surfaces (dark) ──
val GlassWhite = Color(0x18FFFFFF)
val GlassWhiteBorder = Color(0x28FFFFFF)
val GlassGreen = Color(0x1200FF41)
val GlassCyan = Color(0x1200FFFF)
val GlassBlue = Color(0x14367BF0)
val GlassSurfaceTop = Color(0x1AFFFFFF)
val GlassSurfaceBottom = Color(0x08000000)
val GlassHighlight = Color(0x30FFFFFF)

// ── RF / Flipper spectrum ──
val FlipperBackground = Color(0xFF0A0A0A)
val FlipperCardBg = Color(0xFF121212)
val FlipperGrid = Color(0xFF1E2820)
val FlipperSignalNeon = Color(0xFF00FF66)
val FlipperTextPrimary = Color(0xFFE0E0E0)
val FlipperTextSecondary = Color(0xFF757575)
val FlipperAccentCyan = Color(0xFF00E5FF)
val FlipperAlertRed = Color(0xFFFF3366)
val FlipperWarningYellow = Color(0xFFFFCC00)

// ── Semantic glow ──
val GlowGreen = Color(0x4000FF41)
val GlowCyan = Color(0x4000FFFF)
val GlowBlue = Color(0x40367BF0)
val GlowRed = Color(0x40FF0033)

// ── Diurnal (light) palette ──
private val DiurnalBg = Color(0xFFF4F7F5)
private val DiurnalSurface = Color(0xFFFFFFFF)
private val DiurnalSurfaceElevated = Color(0xFFE8F0EC)
private val DiurnalTextPrimary = Color(0xFF1A2E24)
private val DiurnalTextSecondary = Color(0xFF4A6358)
private val DiurnalTextMuted = Color(0xFF7A9488)
private val DiurnalBorder = Color(0x3300AA66)
private val DiurnalGlassTop = Color(0x40FFFFFF)
private val DiurnalGlassBottom = Color(0x0800AA66)
private val DiurnalGlassHighlight = Color(0x5000FF88)

@Immutable
data class EmbedPalette(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accentGreen: Color,
    val accentCyan: Color,
    val accentOrange: Color,
    val accentRed: Color,
    val border: Color,
    val glassTop: Color,
    val glassBottom: Color,
    val glassHighlight: Color,
    val glowGreen: Color,
    val glowCyan: Color,
    val onAccent: Color,
    val scanlineColor: Color,
    val cardGradient: Brush
)

val ObscuroPalette = EmbedPalette(
    isDark = true,
    background = BlackAMOLED,
    surface = DarkSurface,
    surfaceElevated = DarkSurfaceElevated,
    textPrimary = TextPrimary,
    textSecondary = TextGray,
    textMuted = TextMuted,
    accentGreen = MatrixGreen,
    accentCyan = NeonCyan,
    accentOrange = NeonOrange,
    accentRed = NeonRed,
    border = GlassWhiteBorder,
    glassTop = GlassSurfaceTop,
    glassBottom = GlassSurfaceBottom,
    glassHighlight = GlassHighlight,
    glowGreen = GlowGreen,
    glowCyan = GlowCyan,
    onAccent = BlackAMOLED,
    scanlineColor = Color.White.copy(alpha = 0.015f),
    cardGradient = Brush.verticalGradient(
        listOf(
            DarkSurfaceElevated.copy(alpha = 0.95f),
            DarkSurface.copy(alpha = 0.88f),
            Color(0xFF0F0F0F)
        )
    )
)

val DiurnalPalette = EmbedPalette(
    isDark = false,
    background = DiurnalBg,
    surface = DiurnalSurface,
    surfaceElevated = DiurnalSurfaceElevated,
    textPrimary = DiurnalTextPrimary,
    textSecondary = DiurnalTextSecondary,
    textMuted = DiurnalTextMuted,
    accentGreen = Color(0xFF007A4D),
    accentCyan = Color(0xFF0077AA),
    accentOrange = Color(0xFFCC5500),
    accentRed = Color(0xFFCC0033),
    border = DiurnalBorder,
    glassTop = DiurnalGlassTop,
    glassBottom = DiurnalGlassBottom,
    glassHighlight = DiurnalGlassHighlight,
    glowGreen = Color(0x2000FF88),
    glowCyan = Color(0x2000D4FF),
    onAccent = Color.White,
    scanlineColor = Color.Black.copy(alpha = 0.025f),
    cardGradient = Brush.verticalGradient(
        listOf(
            DiurnalSurface,
            DiurnalSurfaceElevated.copy(alpha = 0.92f),
            Color(0xFFDCE8E0)
        )
    )
)

val LocalEmbedPalette = staticCompositionLocalOf { ObscuroPalette }

/** Current palette — use inside @Composable for theme-aware colors. */
object EmbedTheme {
    val palette: EmbedPalette
        @androidx.compose.runtime.Composable
        get() = LocalEmbedPalette.current
}
