package com.embedsuite.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embedsuite.app.ui.theme.BlackAMOLED
import com.embedsuite.app.ui.theme.MatrixGreen
import com.embedsuite.app.ui.theme.MatrixGreenDim
import com.embedsuite.app.ui.theme.NeonCyan
import com.embedsuite.app.ui.theme.NeonOrange
import com.embedsuite.app.ui.theme.NeonPurple
import com.embedsuite.app.ui.theme.TextMuted
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

enum class RadarBlipKind {
    WIFI,
    BLE,
    SUBGHZ,
    NFC
}

data class RadarBlip(
    val id: String,
    val label: String,
    val angleDeg: Float,
    val range01: Float,
    val kind: RadarBlipKind,
    val riskScore: Int = 0
)

@Composable
fun SpectrumRadar(
    blips: List<RadarBlip>,
    modifier: Modifier = Modifier,
    accent: Color = MatrixGreen
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = min(size.width, size.height) * 0.42f
            val center = Offset(size.width / 2f, size.height / 2f)

            drawCircle(
                color = BlackAMOLED,
                radius = radius + 8f,
                center = center
            )

            for (ring in 1..4) {
                val r = radius * ring / 4f
                drawCircle(
                    color = accent.copy(alpha = 0.12f + ring * 0.03f),
                    radius = r,
                    center = center,
                    style = Stroke(width = 1f)
                )
            }

            drawLine(
                color = accent.copy(alpha = 0.25f),
                start = Offset(center.x - radius, center.y),
                end = Offset(center.x + radius, center.y),
                strokeWidth = 1f
            )
            drawLine(
                color = accent.copy(alpha = 0.25f),
                start = Offset(center.x, center.y - radius),
                end = Offset(center.x, center.y + radius),
                strokeWidth = 1f
            )

            val sweepRad = Math.toRadians(sweepAngle.toDouble())
            val sweepEnd = Offset(
                center.x + cos(sweepRad).toFloat() * radius,
                center.y + sin(sweepRad).toFloat() * radius
            )
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(accent.copy(alpha = 0.05f), accent.copy(alpha = 0.85f)),
                    start = center,
                    end = sweepEnd
                ),
                start = center,
                end = sweepEnd,
                strokeWidth = 2.5f
            )

            blips.forEach { blip ->
                val angleRad = Math.toRadians(blip.angleDeg.toDouble())
                val dist = radius * blip.range01.coerceIn(0.05f, 0.98f)
                val pos = Offset(
                    center.x + cos(angleRad).toFloat() * dist,
                    center.y + sin(angleRad).toFloat() * dist
                )
                val blipColor = blipColorFor(blip)
                val pulse = 4f + (blip.riskScore / 100f) * 6f
                drawCircle(
                    color = blipColor.copy(alpha = 0.35f),
                    radius = pulse + 4f,
                    center = pos
                )
                drawCircle(
                    color = blipColor,
                    radius = pulse,
                    center = pos
                )
            }
        }

        if (blips.isEmpty()) {
            Text(
                text = "SCAN — awaiting signals",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = TextMuted
            )
        }
    }
}

private fun blipColorFor(blip: RadarBlip): Color {
    if (blip.riskScore >= 70) return NeonOrange
    return when (blip.kind) {
        RadarBlipKind.WIFI -> NeonCyan
        RadarBlipKind.BLE -> NeonPurple
        RadarBlipKind.SUBGHZ -> MatrixGreen
        RadarBlipKind.NFC -> MatrixGreenDim
    }
}
