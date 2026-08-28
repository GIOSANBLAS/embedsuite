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
    val sub = state.capturedSignal?.flipperSub

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

        state.subParseError?.let { err ->
            Spacer(Modifier.height(8.dp))
            Text(err, color = NeonRed, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
        }

        Spacer(Modifier.height(12.dp))
        sub?.let { flipper ->
            Text("Editar .sub capturado", color = NeonCyan, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            Spacer(Modifier.height(6.dp))

            SubField("Protocolo", flipper.protocol) { viewModel.setSubProtocol(it) }
            SubField("Key", flipper.key.orEmpty()) { viewModel.setSubKey(it) }
            SubField("Bit", flipper.bit?.toString().orEmpty()) { viewModel.setSubBit(it) }
            SubField("TE (µs)", flipper.te?.toString().orEmpty()) { viewModel.setSubTe(it) }
            SubField("Preset", flipper.preset) { viewModel.setSubPreset(it) }

            if (flipper.rawTimings.isNotEmpty()) {
                var rawText by remember(flipper.rawTimings) {
                    mutableStateOf(flipper.rawTimings.joinToString(" "))
                }
                OutlinedTextField(
                    value = rawText,
                    onValueChange = {
                        rawText = it
                        viewModel.setSubRawData(it)
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    label = { Text("RAW_Data (µs)", fontSize = 10.sp) },
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp
                    ),
                    minLines = 2,
                    maxLines = 5
                )
            }

            Text("Recorte silencio (µs)", color = NeonCyan, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            Slider(
                value = state.trimThresholdUs.toFloat(),
                onValueChange = { viewModel.setTrimThresholdUs(it.toLong()) },
                valueRange = 500f..30_000f,
                colors = SliderDefaults.colors(thumbColor = MatrixGreen, activeTrackColor = MatrixGreen)
            )

            if (state.editedSubPreview.isNotBlank()) {
                Text("Vista previa .sub", color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                Text(
                    state.editedSubPreview.take(600) + if (state.editedSubPreview.length > 600) "…" else "",
                    color = TextGray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp
                )
            }

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

@Composable
private fun SubField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        label = { Text(label, fontSize = 10.sp) },
        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
        singleLine = true
    )
}
