package com.embedsuite.app.ui.screen

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.embedsuite.app.connection.DeviceConnectionManager
import com.embedsuite.app.ui.theme.*
import com.embedsuite.app.ui.viewmodel.SpectrumViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpectrumScreen(
    connectionManager: DeviceConnectionManager,
    factory: androidx.lifecycle.ViewModelProvider.Factory,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val vm: SpectrumViewModel = viewModel(factory = factory)
    val samples by vm.samples.collectAsStateWithLifecycle()
    val frames by vm.frames.collectAsStateWithLifecycle()
    val specRun by vm.specRunning.collectAsStateWithLifecycle()
    val decRun by vm.decRunning.collectAsStateWithLifecycle()

    var freqStart by remember { mutableStateOf("380.0") }
    var freqEnd by remember { mutableStateOf("450.0") }
    var freqStep by remember { mutableStateOf("0.025") }
    var pps by remember { mutableStateOf("100") }
    var decFreq by remember { mutableStateOf("433.92") }
    var decMod by remember { mutableStateOf("OOK") }
    var modExpanded by remember { mutableStateOf(false) }

    val saveCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
        if (uri != null) vm.exportCsv(uri)
    }

    LaunchedEffect(Unit) {
        vm.toast.collect { t -> Toast.makeText(ctx, t, Toast.LENGTH_SHORT).show() }
    }

    Scaffold(
        containerColor = BlackAMOLED,
        topBar = {
            TopAppBar(
                title = { Text("SubGHz Spectrum + Decoder", fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MatrixGreen) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
                },
                actions = {
                    IconButton(onClick = { saveCsv.launch("spectrum_${System.currentTimeMillis()}.csv") }) {
                        Icon(Icons.Default.FileDownload, null, tint = NeonCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(12.dp)) {
            Card(colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp)) {
                    Text("SPECTRUM ANALYZER", fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                        color = MatrixGreen, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                        MiniField("Start MHz", freqStart, { freqStart = it }, Modifier.weight(1f))
                        Spacer(Modifier.width(6.dp))
                        MiniField("End MHz", freqEnd, { freqEnd = it }, Modifier.weight(1f))
                        Spacer(Modifier.width(6.dp))
                        MiniField("Step", freqStep, { freqStep = it }, Modifier.weight(1f))
                        Spacer(Modifier.width(6.dp))
                        MiniField("PPS", pps, { pps = it }, Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = {
                            vm.startSpec(freqStart.toDouble(), freqEnd.toDouble(),
                                freqStep.toDouble(), pps.toInt())
                        }, enabled = !specRun,
                            colors = ButtonDefaults.buttonColors(containerColor = MatrixGreen,
                                contentColor = BlackAMOLED)) {
                            Icon(Icons.Default.PlayArrow, null)
                            Text("START", fontFamily = FontFamily.Monospace, fontSize = 10.sp,
                                fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { vm.stopSpec() }, enabled = specRun,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonRed,
                                contentColor = Color.White)) {
                            Icon(Icons.Default.Stop, null)
                            Text("STOP", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                        }
                        Spacer(Modifier.weight(1f))
                        Text(if (specRun) "RUN" else "IDLE",
                            fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                            color = if (specRun) MatrixGreen else TextGray,
                            fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Card(colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(220.dp)) {
                Box(Modifier.fillMaxSize().padding(10.dp)) {
                    if (samples.isEmpty()) {
                        Text("No hay datos. Arranca el barrido para activar la vista.",
                            fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TextGray)
                    } else {
                        SpectrumHeatmap(samples)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Card(colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp)) {
                    Text("AUTO-DECODER (KeeLoq · Somfy · PT2262 · Generic)",
                        fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                        color = NeonPurple, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MiniField("Freq MHz", decFreq, { decFreq = it }, Modifier.weight(1f))
                        Spacer(Modifier.width(6.dp))
                        ExposedDropdownMenuBox(
                            expanded = modExpanded, onExpandedChange = { modExpanded = !modExpanded }) {
                            OutlinedTextField(
                                value = decMod, onValueChange = { decMod = it },
                                readOnly = true,
                                label = { Text("Mod", fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp) },
                                modifier = Modifier.menuAnchor().weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonPurple,
                                    focusedTextColor = NeonPurple, unfocusedTextColor = TextGray
                                ),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modExpanded) }
                            )
                            ExposedDropdownMenu(expanded = modExpanded, onDismissRequest = { modExpanded = false }) {
                                listOf("OOK", "2FSK", "GFSK").forEach { m ->
                                    DropdownMenuItem(text = { Text(m, fontFamily = FontFamily.Monospace) },
                                        onClick = { decMod = m; modExpanded = false })
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row {
                        Button(onClick = { vm.startDecode(decFreq.toDouble(), decMod) }, enabled = !decRun,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple,
                                contentColor = Color.White)) {
                            Icon(Icons.Default.SatelliteAlt, null)
                            Text("DECODE", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { vm.stopDecode() }, enabled = decRun,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonRed,
                                contentColor = Color.White)) {
                            Icon(Icons.Default.Stop, null)
                        }
                        Spacer(Modifier.weight(1f))
                        OutlinedButton(onClick = { vm.clearFrames() }) {
                            Icon(Icons.Default.Delete, null, tint = NeonOrange)
                            Text("Clear", fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Frames: ${frames.size}", fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp, color = TextGray)
                }
            }

            Spacer(Modifier.height(8.dp))

            Text("Frames decodificados:", fontFamily = FontFamily.Monospace,
                fontSize = 10.sp, color = TextGray)
            Spacer(Modifier.height(4.dp))
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                items(frames.reversed(), key = { it.tMs.toString() + it.decoded }) { f ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(8.dp)) {
                        Column(Modifier.padding(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Badge(containerColor = NeonPurple)
                                Spacer(Modifier.width(6.dp))
                                Text(f.proto, fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                                    color = NeonPurple, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(8.dp))
                                Text("@${f.freqMhz} · ${f.rssi} dBm",
                                    fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextGray)
                                Spacer(Modifier.weight(1f))
                                Text("×${f.seen}", fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp, color = TextGray)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(f.decoded, fontFamily = FontFamily.Monospace, fontSize = 10.sp,
                                color = Color.White, lineHeight = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniField(label: String, value: String, onChange: (String) -> Unit, mod: Modifier = Modifier) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        label = { Text(label, fontFamily = FontFamily.Monospace, fontSize = 9.sp) },
        singleLine = true, modifier = mod,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MatrixGreen, focusedTextColor = MatrixGreen,
            unfocusedTextColor = TextGray
        )
    )
}

@Composable
private fun SpectrumHeatmap(samples: List<com.embedsuite.app.ui.viewmodel.SpectrumSample>) {
    val sorted = samples.sortedBy { it.freqMhz }
    if (sorted.size < 8) return
    val minF = sorted.first().freqMhz
    val maxF = sorted.last().freqMhz
    val minR = -110f
    val maxR = -20f

    Canvas(Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        val grid = Path()
        for (row in 0..10) {
            val y = h * row / 10f
            grid.moveTo(0f, y); grid.lineTo(w, y)
        }
        for (col in 0..10) {
            val x = w * col / 10f
            grid.moveTo(x, 0f); grid.lineTo(x, h)
        }
        drawPath(grid, MatrixGreen.copy(alpha = 0.08f), style = Stroke(width = 1.dp.toPx()))

        val barWidth = (w / (sorted.size.coerceAtLeast(1))).coerceAtLeast(1.2.dp.toPx())
        sorted.forEachIndexed { _, s ->
            val x = if (maxF > minF) ((s.freqMhz - minF) / (maxF - minF) * w).toFloat() else 0f
            val normRssi = ((s.rssi - minR) / (maxR - minR)).coerceIn(0f, 1f)
            val barH = normRssi * h
            val color = when {
                normRssi < 0.25f -> Color(0xFF0a4f1f)
                normRssi < 0.5f -> Color(0xFF00ff80)
                normRssi < 0.75f -> Color(0xFFFFD400)
                else -> Color(0xFFff2b5d)
            }
            drawRect(
                brush = Brush.verticalGradient(listOf(color, color.copy(alpha = 0.05f))),
                topLeft = Offset((x - barWidth / 2).coerceIn(0f, w - barWidth), h - barH),
                size = androidx.compose.ui.geometry.Size(barWidth, barH)
            )
        }

        val path = Path()
        sorted.forEachIndexed { i, s ->
            val x = if (maxF > minF) ((s.freqMhz - minF) / (maxF - minF) * w).toFloat() else 0f
            val normRssi = ((s.rssi - minR) / (maxR - minR)).coerceIn(0f, 1f)
            val y = h * (1f - normRssi)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, NeonCyan, style = Stroke(width = 1.3.dp.toPx()))
    }
}
