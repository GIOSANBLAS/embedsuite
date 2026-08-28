package com.embedsuite.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embedsuite.app.ai.EmbedAiEngine
import com.embedsuite.app.data.CapturedSignalEntity
import com.embedsuite.app.data.SignalRepository
import com.embedsuite.app.rf.RfProtocolDecoder
import com.embedsuite.app.rf.RfReplayEngine
import com.embedsuite.app.ui.components.*
import com.embedsuite.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun RfAnalysisScreen(
    signalRepository: SignalRepository,
    rfReplayEngine: RfReplayEngine,
    aiEngine: EmbedAiEngine
) {
    val scope = rememberCoroutineScope()
    val rfSignals by signalRepository.observeByType("RF").collectAsState(initial = emptyList())
    var selectedA by remember { mutableStateOf<CapturedSignalEntity?>(null) }
    var selectedB by remember { mutableStateOf<CapturedSignalEntity?>(null) }
    var compareResult by remember { mutableStateOf("") }
    var aiHint by remember { mutableStateOf("") }
    var zoom by remember { mutableFloatStateOf(1f) }

    Column(Modifier.fillMaxSize().padding(10.dp)) {
        HackerSectionHeader(stringResource(R.string.rf_analysis_title), accent = NeonCyan)

        // RAW visualizer
        GlassCard(accent = MatrixGreen, modifier = Modifier.fillMaxWidth().height(120.dp).padding(bottom = 8.dp)) {
            Text(stringResource(R.string.rf_raw_visualizer), fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MatrixGreen)
            val raw = selectedA?.rawData ?: rfSignals.firstOrNull()?.rawData ?: ""
            val pulses = Regex("""-?\d+""").findAll(raw).map { it.value.toFloatOrNull() ?: 0f }.toList()
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoomChange, _ ->
                            zoom = (zoom * zoomChange).coerceIn(0.5f, 5f)
                        }
                    }
            ) {
                if (pulses.isEmpty()) return@Canvas
                val maxVal = pulses.maxOf { kotlin.math.abs(it) }.coerceAtLeast(1f)
                val step = (size.width / pulses.size.coerceAtMost(200)) * zoom
                pulses.take(200).forEachIndexed { i, v ->
                    val h = (kotlin.math.abs(v) / maxVal) * size.height * 0.8f
                    val color = if (i % 2 == 0) Color(0xFF00FF41) else Color(0xFF00FFFF)
                    drawLine(
                        color = color,
                        start = Offset(i * step, size.height),
                        end = Offset(i * step, size.height - h),
                        strokeWidth = 2f.coerceAtLeast(step * 0.5f)
                    )
                }
            }
            Text(stringResource(R.string.rf_pinch_zoom, detectDominantFreq(selectedA)), fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = TextMuted)
        }

        // Compare
        Text(
            "A: ${selectedA?.label?.ifBlank { selectedA?.protocol } ?: "—"}  |  B: ${selectedB?.label?.ifBlank { selectedB?.protocol } ?: "—"}",
            fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextGray,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GlassChip(stringResource(R.string.rf_clear_ab), false, onClick = { selectedA = null; selectedB = null; compareResult = "" })
            NeonButton(text = stringResource(R.string.rf_compare), onClick = {
                val a = selectedA
                val b = selectedB
                if (a != null && b != null) {
                    compareResult = rfReplayEngine.compareSignals(a, b)
                }
            }, modifier = Modifier.weight(1f), enabled = selectedA != null && selectedB != null)
        }

        if (compareResult.isNotBlank()) {
            Text(compareResult, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = NeonCyan, modifier = Modifier.padding(vertical = 4.dp))
        }

        NeonOutlinedButton(
            text = stringResource(R.string.rf_ai_hint),
            onClick = {
                scope.launch {
                    val s = selectedA ?: rfSignals.firstOrNull()
                    if (s != null) {
                        aiHint = aiEngine.analyzeCapturedSignal(s)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        )
        if (aiHint.isNotBlank()) {
            Text(aiHint, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = MatrixGreen)
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
            items(rfSignals.take(20), key = { it.id }) { signal ->
                val role = when (signal.id) {
                    selectedA?.id -> "A"
                    selectedB?.id -> "B"
                    else -> ""
                }
                GlassCard(accent = if (role.isNotBlank()) NeonCyan else MatrixGreen, cornerRadius = 8.dp, onClick = {
                    when {
                        selectedA?.id == signal.id -> selectedA = null
                        selectedB?.id == signal.id -> selectedB = null
                        selectedA == null -> selectedA = signal
                        selectedB == null -> selectedB = signal
                        else -> { selectedB = signal }
                    }
                }) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(signal.label.ifBlank { signal.protocol }, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MatrixGreen)
                        if (role.isNotBlank()) Text("[$role]", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = NeonCyan)
                    }
                    Text("${signal.frequency} // ${signal.protocol}", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = TextGray)
                }
            }
        }
    }
}

private fun detectDominantFreq(signal: CapturedSignalEntity?): String {
    if (signal == null) return "433.92"
    return signal.frequency.ifBlank {
        RfProtocolDecoder.decode(signal.rawData)?.frequency ?: "433.92"
    }
}
