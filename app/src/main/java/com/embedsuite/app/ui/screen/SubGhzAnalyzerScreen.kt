package com.embedsuite.app.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embedsuite.app.ui.theme.*
import com.embedsuite.app.ui.viewmodel.SubGhzAnalyzerViewModel

@Composable
fun SubGhzAnalyzerScreen(viewModel: SubGhzAnalyzerViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()

    Column(
        Modifier.fillMaxSize().background(BlackAMOLED).padding(12.dp).verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = MatrixGreen) }
            Text("SUB-GHZ ANALYZER", color = MatrixGreen, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
        Text(state.transportHint, color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
        Spacer(Modifier.height(12.dp))

        Text("Frecuencia: ${"%.2f".format(state.freqMhz)} MHz", color = NeonCyan, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
        Slider(
            value = state.freqMhz,
            onValueChange = viewModel::setFreqMhz,
            valueRange = 300f..928f,
            colors = SliderDefaults.colors(thumbColor = MatrixGreen, activeTrackColor = MatrixGreen)
        )

        Text("Duración: ${state.durationSec}s", color = NeonCyan, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
        Slider(
            value = state.durationSec.toFloat(),
            onValueChange = { viewModel.setDuration(it.toInt()) },
            valueRange = 1f..120f,
            steps = 118,
            colors = SliderDefaults.colors(thumbColor = MatrixGreen, activeTrackColor = MatrixGreen)
        )

        Button(
            onClick = viewModel::capture,
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MatrixGreen.copy(alpha = 0.2f))
        ) {
            if (state.busy) {
                CircularProgressIndicator(Modifier.size(18.dp), color = MatrixGreen, strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text("CAPTURAR (subghz rx)", fontFamily = FontFamily.Monospace)
        }

        Spacer(Modifier.height(12.dp))
        state.capturedSignal?.let {
            Text("Recorte silencio (µs)", color = NeonCyan, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            Slider(
                value = state.trimThresholdUs.toFloat(),
                onValueChange = { viewModel.setTrimThresholdUs(it.toLong()) },
                valueRange = 500f..30_000f,
                colors = SliderDefaults.colors(thumbColor = MatrixGreen, activeTrackColor = MatrixGreen)
            )
            Button(
                onClick = viewModel::replayEdited,
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.2f))
            ) {
                Text("REPLAY (.sub editado)", fontFamily = FontFamily.Monospace)
            }
            Spacer(Modifier.height(8.dp))
        }
        state.waveform?.let { bmp ->
            Text("Forma de onda", color = NeonCyan, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Waveform",
                modifier = Modifier.fillMaxWidth().height(96.dp)
            )
            Spacer(Modifier.height(8.dp))
        }
        OrchestrationFeedback(state.lastResult)
    }
}
