package com.embedsuite.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embedsuite.app.R
import com.embedsuite.app.ui.theme.*
import kotlin.math.sin

@Composable
fun GlassBackground(
    modifier: Modifier = Modifier,
    intensity: Float = 1f
) {
    val palette = EmbedTheme.palette
    val alphaScale = intensity.coerceIn(0.3f, 1f)
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing)),
        label = "phase"
    )

    Box(modifier = modifier.fillMaxSize().background(palette.background)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val rad = Math.toRadians(phase.toDouble())
            val blueAlpha = if (palette.isDark) 0.12f else 0.08f
            val greenAlpha = if (palette.isDark) 0.08f else 0.06f
            val cyanAlpha = if (palette.isDark) 0.06f else 0.05f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(KaliBlue.copy(alpha = blueAlpha * alphaScale), Color.Transparent),
                    center = Offset(w * 0.2f + sin(rad).toFloat() * 80f, h * 0.15f),
                    radius = w * 0.5f
                ),
                radius = w * 0.5f,
                center = Offset(w * 0.2f, h * 0.15f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(palette.accentGreen.copy(alpha = greenAlpha * alphaScale), Color.Transparent),
                    center = Offset(w * 0.85f, h * 0.75f),
                    radius = w * 0.45f
                ),
                radius = w * 0.45f,
                center = Offset(w * 0.85f, h * 0.75f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(palette.accentCyan.copy(alpha = cyanAlpha * alphaScale), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.5f),
                    radius = w * 0.35f
                ),
                radius = w * 0.35f,
                center = Offset(w * 0.5f, h * 0.5f)
            )
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        palette.background.copy(alpha = if (palette.isDark) 0.6f else 0.35f),
                        palette.background.copy(alpha = if (palette.isDark) 0.92f else 0.75f)
                    )
                )
            )
        )
    }
}

@Composable
fun ScanlineOverlay(modifier: Modifier = Modifier, enabled: Boolean = true) {
    if (!enabled) return
    val palette = EmbedTheme.palette
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "scanOffset"
    )

    Canvas(modifier = modifier) {
        val lineY = size.height * offset
        drawLine(
            color = palette.accentGreen.copy(alpha = if (palette.isDark) 0.04f else 0.03f),
            start = Offset(0f, lineY),
            end = Offset(size.width, lineY),
            strokeWidth = 2f
        )
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = palette.scanlineColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
            y += 4f
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    cornerRadius: Dp = 14.dp,
    elevation: Dp = 6.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = EmbedTheme.palette
    val accentColor = accent ?: palette.accentGreen
    val shape = RoundedCornerShape(cornerRadius)
    val clickMod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    val shadowColor = if (palette.isDark) accentColor.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.12f)

    Box(
        modifier = modifier
            .then(clickMod)
            .shadow(elevation, shape, ambientColor = shadowColor, spotColor = shadowColor)
            .drawBehind {
                drawRoundRect(
                    color = accentColor.copy(alpha = if (palette.isDark) 0.08f else 0.06f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx())
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.cardGradient, shape = shape)
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            accentColor.copy(alpha = if (palette.isDark) 0.45f else 0.35f),
                            palette.border.copy(alpha = 0.5f),
                            accentColor.copy(alpha = if (palette.isDark) 0.2f else 0.15f)
                        )
                    ),
                    shape = shape
                )
                .clip(shape)
                .padding(14.dp),
            content = content
        )
    }
}

@Composable
fun TopBarTelemetryStrip(
    batteryText: String,
    temperatureText: String,
    transportLabel: String,
    modifier: Modifier = Modifier
) {
    val palette = EmbedTheme.palette
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(
                        palette.surfaceElevated,
                        palette.surface,
                        palette.surfaceElevated
                    )
                )
            )
            .border(
                width = 0.5.dp,
                color = palette.accentGreen.copy(alpha = 0.2f)
            )
            .padding(horizontal = 14.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TelemetryChip(label = "BAT", value = batteryText, color = palette.accentGreen)
        TelemetryChip(label = "TMP", value = temperatureText, color = palette.accentCyan)
        TelemetryChip(label = "LNK", value = transportLabel, color = palette.accentOrange)
    }
}

@Composable
private fun TelemetryChip(label: String, value: String, color: Color) {
    val palette = EmbedTheme.palette
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            fontFamily = FontFamily.Monospace,
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            color = palette.textMuted,
            letterSpacing = 0.5.sp
        )
        Spacer(Modifier.width(4.dp))
        Text(
            value,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun EmbedTopBar(
    title: String,
    subtitle: String? = null,
    statusText: String,
    statusColor: Color,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    batteryText: String? = null,
    temperatureText: String? = null,
    transportLabel: String? = null
) {
    val palette = EmbedTheme.palette
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(palette.surfaceElevated, palette.surface)
                )
            )
    ) {
        if (batteryText != null || temperatureText != null || transportLabel != null) {
            TopBarTelemetryStrip(
                batteryText = batteryText ?: "—",
                temperatureText = temperatureText ?: "—",
                transportLabel = transportLabel ?: "OFFLINE"
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.accentGreen
                )
                subtitle?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textSecondary
                    )
                }
            }
            ConnectionPulse(color = statusColor)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                statusText,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = statusColor,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.settings_title),
                    tint = palette.accentGreen,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        HorizontalDivider(
            color = palette.accentGreen.copy(alpha = if (palette.isDark) 0.35f else 0.25f),
            thickness = 1.dp
        )
    }
}

/** @deprecated Use [EmbedTopBar] — kept for compatibility */
@Composable
fun GlassTopBar(
    title: String,
    subtitle: String? = null,
    statusText: String,
    statusColor: Color,
    onSettingsClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    EmbedTopBar(
        title = title,
        subtitle = subtitle,
        statusText = statusText,
        statusColor = statusColor,
        onSettingsClick = onSettingsClick ?: {},
        modifier = modifier
    )
}

@Composable
fun ConnectionPulse(color: Color, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "pulseScale"
    )
    Box(modifier = modifier.size(10.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size((10 * scale).dp)
                .background(color.copy(alpha = 0.2f), CircleShape)
        )
        Box(
            Modifier
                .size(5.dp)
                .background(color, CircleShape)
        )
    }
}

@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color? = null,
    enabled: Boolean = true,
    contentDescription: String = text
) {
    val palette = EmbedTheme.palette
    val btnColor = color ?: palette.accentGreen
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .shadow(if (enabled) 4.dp else 0.dp, RoundedCornerShape(8.dp))
            .semantics { this.contentDescription = contentDescription },
        colors = ButtonDefaults.buttonColors(
            containerColor = btnColor.copy(alpha = if (palette.isDark) 0.85f else 0.92f),
            contentColor = palette.onAccent,
            disabledContainerColor = palette.textMuted.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(8.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 6.dp)
    ) {
        Text(text, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun NeonOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color? = null,
    enabled: Boolean = true,
    contentDescription: String = text
) {
    val palette = EmbedTheme.palette
    val btnColor = color ?: palette.accentCyan
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .border(1.dp, btnColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .semantics { this.contentDescription = contentDescription },
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = btnColor)
    ) {
        Text(text, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
    }
}

@Composable
fun HackerSectionHeader(
    title: String,
    accent: Color? = null,
    modifier: Modifier = Modifier
) {
    val palette = EmbedTheme.palette
    val accentColor = accent ?: palette.accentGreen
    Column(modifier = modifier.padding(bottom = 8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = accentColor
        )
        Box(
            Modifier
                .fillMaxWidth(0.4f)
                .height(2.dp)
                .background(Brush.horizontalGradient(listOf(accentColor, Color.Transparent)))
        )
    }
}

@Composable
fun GlassChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    accent: Color? = null
) {
    val palette = EmbedTheme.palette
    val accentColor = accent ?: palette.accentGreen
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                label,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = if (selected) palette.onAccent else accentColor
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = accentColor,
            containerColor = palette.surfaceElevated
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = accentColor.copy(alpha = 0.5f),
            selectedBorderColor = accentColor
        )
    )
}

@Composable
fun StatBadge(label: String, value: String, color: Color? = null) {
    val palette = EmbedTheme.palette
    val accent = color ?: palette.accentCyan
    GlassCard(accent = accent, cornerRadius = 8.dp, elevation = 3.dp) {
        Text(label, fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = palette.textMuted)
        Text(
            value,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = accent
        )
    }
}

@Composable
fun HackerDivider(color: Color? = null) {
    val palette = EmbedTheme.palette
    HorizontalDivider(color = color ?: palette.border, thickness = 0.5.dp)
}
