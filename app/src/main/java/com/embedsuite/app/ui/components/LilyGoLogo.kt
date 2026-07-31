package com.embedsuite.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embedsuite.app.R
import com.embedsuite.app.ui.theme.*
import kotlin.math.sin

/**
 * Logo original EMBED SUITE: mascota + T-Embed LilyGO ([ic_launcher_image]).
 * Capa brutal: glow, brackets rojos, micro-glitch y wordmark LILYGO.
 */
@Composable
fun LilyGoLogo(
    modifier: Modifier = Modifier,
    logoSize: Dp = 140.dp,
    showWordmark: Boolean = true,
    showSubtitle: Boolean = true,
    brutal: Boolean = true
) {
    val infinite = rememberInfiniteTransition(label = "lilygo_logo")
    val pulse by infinite.animateFloat(
        initialValue = 0.88f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
        label = "pulse"
    )
    val glitch by infinite.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(110), RepeatMode.Reverse),
        label = "glitch"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (brutal) {
                Canvas(Modifier.size(logoSize + 36.dp)) {
                    val c = center
                    val r = size.minDimension / 2.15f
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(
                                MatrixGreen.copy(alpha = 0.45f * pulse),
                                NeonOrange.copy(alpha = 0.15f * pulse),
                                Color.Transparent
                            )
                        ),
                        radius = r,
                        center = c
                    )
                    drawCircle(
                        color = MatrixGreen.copy(alpha = 0.55f),
                        radius = r * 0.94f,
                        center = c,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
                    )
                    val b = r * 0.98f
                    val stroke = 3.5f
                    val red = NeonRed.copy(alpha = 0.95f)
                    drawLine(red, Offset(c.x - b, c.y - b), Offset(c.x - b + 20f, c.y - b), stroke)
                    drawLine(red, Offset(c.x - b, c.y - b), Offset(c.x - b, c.y - b + 20f), stroke)
                    drawLine(red, Offset(c.x + b, c.y - b), Offset(c.x + b - 20f, c.y - b), stroke)
                    drawLine(red, Offset(c.x + b, c.y - b), Offset(c.x + b, c.y - b + 20f), stroke)
                    drawLine(red, Offset(c.x - b, c.y + b), Offset(c.x - b + 20f, c.y + b), stroke)
                    drawLine(red, Offset(c.x - b, c.y + b), Offset(c.x - b, c.y + b - 20f), stroke)
                    drawLine(red, Offset(c.x + b, c.y + b), Offset(c.x + b - 20f, c.y + b), stroke)
                    drawLine(red, Offset(c.x + b, c.y + b), Offset(c.x + b, c.y + b - 20f), stroke)
                }
            }

            Image(
                painter = painterResource(R.drawable.ic_launcher_image),
                contentDescription = "LILYGO T-Embed EMBED SUITE",
                modifier = Modifier
                    .size(logoSize)
                    .graphicsLayer {
                        scaleX = 0.94f + pulse * 0.06f
                        scaleY = 0.94f + pulse * 0.06f
                        if (brutal) {
                            translationX = glitch * 2f
                            shadowElevation = 12f
                        }
                    }
            )
        }

        if (showWordmark) {
            Spacer(Modifier.height(10.dp))
            Text(
                "LILYGO",
                fontFamily = FontFamily.Monospace,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = NeonOrange,
                letterSpacing = 5.sp
            )
        }

        if (showSubtitle) {
            Text(
                "T-EMBED CC1101 PLUS",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MatrixGreen,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            if (brutal) {
                Text(
                    "// SUB-GHz BRUTAL MODE",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = NeonRed.copy(alpha = 0.8f + sin(pulse * 6f) * 0.2f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
