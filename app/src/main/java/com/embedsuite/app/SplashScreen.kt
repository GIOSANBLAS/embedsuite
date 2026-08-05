package com.embedsuite.app

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embedsuite.app.ui.components.GlassBackground
import com.embedsuite.app.ui.components.LilyGoLogo
import com.embedsuite.app.ui.components.ScanlineOverlay
import com.embedsuite.app.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var progress by remember { mutableFloatStateOf(0f) }
    var status by remember { mutableStateOf("BOOTING XIBALBA LINK...") }

    LaunchedEffect(Unit) {
        val steps = listOf(
            0.12f to "INIT TEH-LINK PROTOCOL...",
            0.28f to "ARM CC1101 @ 300-928 MHz...",
            0.44f to "MOUNT USB OTG / WiFi STACK...",
            0.60f to "PAIRING READY — GPIO6 LONG-PRESS",
            0.78f to "SYNC XIBALBA SYMBIOSIS CORE...",
            0.92f to "TEH-LINK ONLINE — RF/NFC/IR",
            1.0f to "EMBED SUITE v${com.embedsuite.app.core.AppVersion.NAME} ONLINE."
        )
        for ((pct, msg) in steps) {
            delay(320)
            progress = pct
            status = msg
        }
        delay(380)
        onFinished()
    }

    Box(Modifier.fillMaxSize().background(BlackAMOLED)) {
        GlassBackground()
        ScanlineOverlay(Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LilyGoLogo(logoSize = 132.dp, brutal = true)

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                "EMBED SUITE",
                fontFamily = FontFamily.Monospace,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MatrixGreen,
                letterSpacing = 3.sp
            )
            Text(
                "XIBALBA SYMBIOSIS // T-EMBED CC1101 PLUS",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = KaliBlue
            )

            Spacer(modifier = Modifier.height(28.dp))

            Box(
                Modifier
                    .fillMaxWidth(0.75f)
                    .height(5.dp)
                    .background(DarkSurfaceElevated, androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(listOf(NeonOrange, MatrixGreen, NeonCyan)),
                            androidx.compose.foundation.shape.RoundedCornerShape(2.dp)
                        )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(status, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = NeonCyan)
            Text(
                "[ BRUTAL RF MODE // USER ASSUMES RISK ]",
                fontFamily = FontFamily.Monospace,
                fontSize = 8.sp,
                color = NeonRed.copy(alpha = 0.75f),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
