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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embedsuite.app.R
import com.embedsuite.app.ui.theme.*
import kotlin.math.sin

@Composable
fun GlassBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing)),
        label = "phase"
    )

    Box(modifier = modifier.fillMaxSize().background(BlackAMOLED)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val rad = Math.toRadians(phase.toDouble())

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(KaliBlue.copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(w * 0.2f + sin(rad).toFloat() * 80f, h * 0.15f),
                    radius = w * 0.5f
                ),
                radius = w * 0.5f,
                center = Offset(w * 0.2f, h * 0.15f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(MatrixGreen.copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(w * 0.85f, h * 0.75f),
                    radius = w * 0.45f
                ),
                radius = w * 0.45f,
                center = Offset(w * 0.85f, h * 0.75f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(NeonCyan.copy(alpha = 0.06f), Color.Transparent),
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
                    listOf(Color.Transparent, BlackAMOLED.copy(alpha = 0.6f), BlackAMOLED.copy(alpha = 0.9f))
                )
            )
        )
    }
}

@Composable
fun ScanlineOverlay(modifier: Modifier = Modifier, enabled: Boolean = true) {
    if (!enabled) return
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
            color = MatrixGreen.copy(alpha = 0.04f),
            start = Offset(0f, lineY),
            end = Offset(size.width, lineY),
            strokeWidth = 2f
        )
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = Color.White.copy(alpha = 0.015f),
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
    accent: Color = MatrixGreen,
    cornerRadius: Dp = 14.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val clickMod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    Box(
        modifier = modifier
            .then(clickMod)
            .drawBehind {
                drawRoundRect(
                    color = accent.copy(alpha = 0.06f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx())
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurfaceElevated, shape = shape)
                .border(
                    width = 1.dp,
                    color = accent.copy(alpha = 0.35f),
                    shape = shape
                )
                .clip(shape)
                .padding(14.dp),
            content = content
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkSurface)
    ) {
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
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MatrixGreen
                )
                subtitle?.let {
                    Text(it, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TextGray)
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
                    tint = MatrixGreen,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        HorizontalDivider(color = MatrixGreen.copy(alpha = 0.35f), thickness = 1.dp)
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
    color: Color = MatrixGreen,
    enabled: Boolean = true,
    contentDescription: String = text
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.semantics { this.contentDescription = contentDescription },
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.85f),
            disabledContainerColor = TextMuted.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(8.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Text(text, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BlackAMOLED)
    }
}

@Composable
fun NeonOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = NeonCyan,
    enabled: Boolean = true,
    contentDescription: String = text
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .semantics { this.contentDescription = contentDescription },
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color)
    ) {
        Text(text, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = color)
    }
}

@Composable
fun HackerSectionHeader(
    title: String,
    accent: Color = MatrixGreen,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(bottom = 8.dp)) {
        Text(
            title,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = accent
        )
        Box(
            Modifier
                .fillMaxWidth(0.4f)
                .height(2.dp)
                .background(Brush.horizontalGradient(listOf(accent, Color.Transparent)))
        )
    }
}

@Composable
fun GlassChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    accent: Color = MatrixGreen
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                label,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = if (selected) BlackAMOLED else accent
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = accent,
            containerColor = DarkSurfaceElevated
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = accent.copy(alpha = 0.5f),
            selectedBorderColor = accent
        )
    )
}

@Composable
fun StatBadge(label: String, value: String, color: Color = NeonCyan) {
    GlassCard(accent = color, cornerRadius = 8.dp) {
        Text(label, fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = TextMuted)
        Text(value, fontFamily = FontFamily.Monospace, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun HackerDivider(color: Color = GlassWhiteBorder) {
    HorizontalDivider(color = color, thickness = 0.5.dp)
}
