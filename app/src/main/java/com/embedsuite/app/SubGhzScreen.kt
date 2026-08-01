package com.embedsuite.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.embedsuite.app.connection.ConnectionState
import com.embedsuite.app.connection.DeviceConnectionManager
import com.embedsuite.app.connection.FirmwareProfile
import com.embedsuite.app.connection.SignalEntry
import com.embedsuite.app.rf.RfFrequencyPresets
import com.embedsuite.app.ui.components.RfFrequencyPicker
import com.embedsuite.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SubGhzScreen(
    connectionManager: DeviceConnectionManager,
    onSignalSelected: (SignalEntry) -> Unit = {}
) {
    val connectionState by connectionManager.connectionState.collectAsState()
    val detectedProfile by connectionManager.detectedProfile.collectAsState()
    val signalLog by connectionManager.signalLog.collectAsState()
    val selectedMhz by connectionManager.subGhzFrequencyMhz.collectAsState()
    val rfLive by connectionManager.rfLive.collectAsState()

    var currentFrequency by remember { mutableStateOf(RfFrequencyPresets.label(selectedMhz)) }
    var isCapturingRaw by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }

    val isConnected = connectionState is ConnectionState.Connected
    val scope = rememberCoroutineScope()

    LaunchedEffect(selectedMhz) {
        currentFrequency = RfFrequencyPresets.label(selectedMhz)
    }

    LaunchedEffect(connectionManager) {
        connectionManager.events.collect { event ->
            if (event is com.embedsuite.app.connection.BruceEvent.SubGhzSignal) {
                if (event.entry.frequency.isNotBlank()) {
                    currentFrequency = "${event.entry.frequency} MHz"
                }
            }
        }
    }

    val spectrumPoints = remember(rfLive.spectrumBins) {
        rfLive.spectrumBins.mapIndexed { index, bin ->
            Offset(
                x = index.toFloat() / rfLive.spectrumBins.size.coerceAtLeast(1),
                y = bin.coerceIn(0f, 1f)
            )
        }
    }

    val isXibalba = detectedProfile == FirmwareProfile.XIBALBA
    val liveRfEnabled = isConnected && !isXibalba

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FlipperBackground)
            .padding(10.dp)
    ) {
        if (isXibalba) {
            Text(
                stringResource(R.string.plus_compat_subghz_xibalba_hint),
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = NeonOrange,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        HeaderSection(
            headerTitle = if (isXibalba) {
                stringResource(R.string.plus_compat_subghz_header_xibalba)
            } else {
                stringResource(R.string.plus_compat_subghz_header_bruce)
            },
            frequency = currentFrequency,
            isConnected = isConnected,
            lastRssi = if (liveRfEnabled) rfLive.lastRssiDbm else null
        )

        RfFrequencyPicker(
            selectedMhz = selectedMhz,
            onSelected = { mhz ->
                scope.launch {
                    connectionManager.setSubGhzFrequency(mhz)
                    currentFrequency = RfFrequencyPresets.label(mhz)
                }
            },
            modifier = Modifier.padding(vertical = 6.dp)
        )
        Text(
            com.embedsuite.app.connection.BruceCommands.FREQ_LOCAL_HINT,
            fontFamily = FontFamily.Monospace,
            fontSize = 8.sp,
            color = FlipperTextSecondary,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        SectionHeader(
            title = "RF SPECTRUM ANALYZER",
            badge = rfLive.lastRssiDbm?.let { "${it.toInt()} dBm" } ?: "${spectrumPoints.size} PTS",
            isLive = liveRfEnabled && (isCapturingRaw || isScanning)
        )
        SpectrumAnalyzerView(
            points = spectrumPoints,
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .background(FlipperCardBg, RoundedCornerShape(6.dp))
                .border(1.dp, FlipperGrid, RoundedCornerShape(6.dp))
        )

        Spacer(modifier = Modifier.height(10.dp))

        SectionHeader(
            title = "WATERFALL DISPLAY",
            badge = if (liveRfEnabled && (isCapturingRaw || isScanning)) "LIVE" else "PAUSED",
            isLive = liveRfEnabled && (isCapturingRaw || isScanning)
        )
        WaterfallDisplayView(
            history = rfLive.waterfall,
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(FlipperCardBg, RoundedCornerShape(6.dp))
                .border(1.dp, FlipperGrid, RoundedCornerShape(6.dp))
        )

        Spacer(modifier = Modifier.height(10.dp))

        SectionHeader(
            title = "WAVEFORM VIEWER (µs)",
            badge = "${rfLive.waveform.size} pulses · ${rfLive.totalPulseUs}µs"
        )
        WaveformViewer(
            samples = rfLive.waveform,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .background(FlipperCardBg, RoundedCornerShape(6.dp))
                .border(1.dp, FlipperGrid, RoundedCornerShape(6.dp))
        )

        Spacer(modifier = Modifier.height(10.dp))

        SectionHeader(title = "SIGNAL LOG", badge = "${signalLog.size} CAPTURAS")
        SignalLogTableView(
            signals = signalLog,
            onSignalClick = onSignalSelected,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(FlipperCardBg, RoundedCornerShape(6.dp))
                .border(1.dp, FlipperGrid, RoundedCornerShape(6.dp))
        )

        Spacer(modifier = Modifier.height(8.dp))

        QuickActionButtons(
            isCapturing = isCapturingRaw,
            isScanning = isScanning,
            isConnected = isConnected,
            isXibalba = isXibalba,
            onToggleCapture = {
                scope.launch {
                    if (isCapturingRaw) {
                        connectionManager.stopSubGhzCapture()
                        isCapturingRaw = false
                    } else {
                        isScanning = false
                        connectionManager.startSubGhzRawCapture(15)
                        isCapturingRaw = true
                        if (isXibalba) {
                            kotlinx.coroutines.delay(15_000L)
                            isCapturingRaw = false
                        }
                    }
                }
            },
            onToggleScan = {
                if (isXibalba) return@QuickActionButtons
                scope.launch {
                    if (isScanning) {
                        connectionManager.stopSubGhzCapture()
                        isScanning = false
                    } else {
                        isCapturingRaw = false
                        connectionManager.startSubGhzSpectrumScan()
                        isScanning = true
                    }
                }
            }
        )
    }
}

@Composable
private fun WaveformViewer(
    samples: List<Pair<Float, Long>>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.padding(4.dp)) {
        if (samples.isEmpty()) return@Canvas
        var x = 0f
        val scaleUs = size.width / samples.sumOf { it.second.coerceAtLeast(1L) }.coerceAtLeast(1L).toFloat()
        samples.forEach { (level, durationUs) ->
            val w = (durationUs * scaleUs).coerceAtLeast(1f)
            val y = if (level > 0.5f) size.height * 0.2f else size.height * 0.8f
            drawLine(
                FlipperSignalNeon,
                Offset(x, y),
                Offset(x + w, y),
                strokeWidth = 2f
            )
            x += w
        }
    }
}

@Composable
private fun HeaderSection(
    headerTitle: String,
    frequency: String,
    isConnected: Boolean,
    lastRssi: Float?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = headerTitle,
                color = FlipperTextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Text(
                text = "SUB-GHZ // $frequency",
                color = FlipperAccentCyan,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (isConnected) FlipperSignalNeon else FlipperAlertRed,
                            shape = RoundedCornerShape(50)
                        )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isConnected) "T-EMBED OK" else "SIN LINK",
                    color = if (isConnected) FlipperSignalNeon else FlipperAlertRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            lastRssi?.let {
                Text(
                    "${it.toInt()} dBm",
                    color = FlipperWarningYellow,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, badge: String, isLive: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "▼ $title",
            color = FlipperAccentCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.5.sp
        )
        Text(
            text = badge,
            color = if (isLive) FlipperSignalNeon else FlipperTextSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun SpectrumAnalyzerView(points: List<Offset>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.padding(4.dp)) {
        val rows = 4
        for (i in 1 until rows) {
            val y = size.height * (i.toFloat() / rows)
            drawLine(FlipperGrid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }

        if (points.size > 1) {
            val path = Path().apply {
                moveTo(points.first().x * size.width, (1f - points.first().y) * size.height)
                for (i in 1 until points.size) {
                    lineTo(points[i].x * size.width, (1f - points[i].y) * size.height)
                }
            }
            drawPath(path = path, color = FlipperSignalNeon, style = Stroke(width = 1.5.dp.toPx()))
        }
    }
}

@Composable
private fun WaterfallDisplayView(history: List<List<Float>>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        if (history.isEmpty()) return@Canvas
        val rowHeight = size.height / history.size.coerceAtLeast(1)

        history.forEachIndexed { rowIndex, rssiList ->
            if (rssiList.isEmpty()) return@forEachIndexed
            val colWidth = size.width / rssiList.size.coerceAtLeast(1)

            rssiList.forEachIndexed { colIndex, rssi ->
                val intensity = ((rssi + 110f) / 90f).coerceIn(0f, 1f)
                val color = Color(red = 0f, green = intensity, blue = 0.3f * intensity, alpha = 1f)
                drawRect(
                    color = color,
                    topLeft = Offset(colIndex * colWidth, rowIndex * rowHeight),
                    size = Size(colWidth + 1f, rowHeight + 1f)
                )
            }
        }
    }
}

@Composable
private fun SignalLogTableView(
    signals: List<SignalEntry>,
    onSignalClick: (SignalEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(FlipperGrid)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text("FREQ", color = FlipperTextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
            Text("ID HEX", color = FlipperTextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1.2f))
            Text("PROTO", color = FlipperTextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1.2f))
            Text("POWER", color = FlipperTextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(0.8f), textAlign = TextAlign.End)
        }

        if (signals.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Presiona CAPTURAR RAW o SCAN para escuchar señales...",
                    color = FlipperTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(signals) { signal ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSignalClick(signal) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(signal.frequency, color = FlipperSignalNeon, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                        Text(signal.deviceId, color = FlipperAccentCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1.2f))
                        Text(signal.protocol, color = FlipperTextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1.2f))
                        Text(signal.power, color = FlipperWarningYellow, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(0.8f), textAlign = TextAlign.End)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionButtons(
    isCapturing: Boolean,
    isScanning: Boolean,
    isConnected: Boolean,
    isXibalba: Boolean,
    onToggleCapture: () -> Unit,
    onToggleScan: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            if (isXibalba) {
                "Captura remota vía TEH-Link. Resultados en pantalla del T-Embed."
            } else {
                "TX: Tools → Sync SD → TX en .sub del T-Embed (o biblioteca si está decodificada)"
            },
            fontFamily = FontFamily.Monospace,
            fontSize = 8.sp,
            color = FlipperTextSecondary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (!isXibalba) {
                Button(
                    onClick = onToggleScan,
                    enabled = isConnected,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isScanning) NeonCyan.copy(alpha = 0.3f) else FlipperGrid
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        if (isScanning) "STOP UI" else "LISTEN 20s",
                        color = if (isScanning) BlackAMOLED else NeonCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Button(
                onClick = onToggleCapture,
                enabled = isConnected,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCapturing) FlipperAlertRed else FlipperGrid
                ),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = when {
                        isCapturing -> "STOP UI"
                        isXibalba -> stringResource(R.string.plus_compat_capture_tehlink)
                        else -> "RAW RX 15s"
                    },
                    color = if (isCapturing) Color.White else FlipperSignalNeon,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
