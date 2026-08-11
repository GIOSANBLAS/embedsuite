package com.embedsuite.app.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.embedsuite.app.R
import com.embedsuite.app.connection.ConnectionState
import com.embedsuite.app.connection.FirmwareProfile
import com.embedsuite.app.map.DarkMapTileSource
import com.embedsuite.app.map.OsmdroidConfig
import com.embedsuite.app.scan.HybridLocationProvider
import com.embedsuite.app.ui.components.GlassCard
import com.embedsuite.app.ui.components.GlassChip
import com.embedsuite.app.ui.theme.*
import com.embedsuite.app.ui.viewmodel.RfScannerViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import kotlin.math.roundToInt

/**
 * RfScannerScreen — escáner de espectro CC1101 (TEH-Link rf_scanner) con
 * gráfica RSSI en vivo y mapa de calor GPS (ubicación del teléfono).
 */
@Composable
fun RfScannerScreen(
    viewModel: RfScannerViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val samples by viewModel.samples.collectAsState()
    val geoSamples by viewModel.geoSamples.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val detectedProfile by viewModel.detectedProfile.collectAsState()
    val isReady = connectionState is ConnectionState.Connected &&
        detectedProfile == FirmwareProfile.XIBALBA

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BlackAMOLED)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back), tint = MatrixGreen)
            }
            Column {
                Text(
                    stringResource(R.string.scanner_title),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MatrixGreen
                )
                Text(
                    stringResource(R.string.scanner_subtitle),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = TextGray
                )
            }
        }
        HorizontalDivider(color = MatrixGreen.copy(alpha = 0.35f))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            // Selector de vista
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                GlassChip(
                    label = stringResource(R.string.scanner_view_chart),
                    selected = uiState.viewMode == RfScannerViewModel.ViewMode.CHART,
                    onClick = { viewModel.setViewMode(RfScannerViewModel.ViewMode.CHART) },
                    accent = MatrixGreen
                )
                GlassChip(
                    label = stringResource(R.string.scanner_view_map),
                    selected = uiState.viewMode == RfScannerViewModel.ViewMode.MAP,
                    onClick = { viewModel.setViewMode(RfScannerViewModel.ViewMode.MAP) },
                    accent = NeonCyan
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            when (uiState.viewMode) {
                RfScannerViewModel.ViewMode.CHART -> RfChartView(
                    samples = samples,
                    freqStart = uiState.freqStartMhz,
                    freqEnd = uiState.freqEndMhz,
                    threshold = uiState.rssiThreshold
                )
                RfScannerViewModel.ViewMode.MAP -> RfHeatMapView(geoSamples = geoSamples)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Estado en vivo
            GlassCard(accent = NeonCyan, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(R.string.scanner_samples, uiState.sampleCount),
                        fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TextGray
                    )
                    if (uiState.maxRssi > -127) {
                        Text(
                            stringResource(
                                R.string.scanner_max,
                                uiState.maxRssi,
                                String.format("%.3f", uiState.maxFreqMhz)
                            ),
                            fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = NeonOrange
                        )
                    }
                }
                if (uiState.timeSynced) {
                    Text(
                        stringResource(R.string.scanner_time_synced),
                        fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = MatrixGreen
                    )
                }
                if (uiState.statusMessage.isNotBlank()) {
                    Text(
                        uiState.statusMessage,
                        fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = NeonOrange
                    )
                }
            }

            // Parámetros del barrido
            GlassCard(accent = MatrixGreen, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                Text(
                    stringResource(
                        R.string.scanner_range,
                        String.format("%.2f", uiState.freqStartMhz),
                        String.format("%.2f", uiState.freqEndMhz)
                    ),
                    fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = MatrixGreen
                )
                RangeSlider(
                    value = uiState.freqStartMhz.toFloat()..uiState.freqEndMhz.toFloat(),
                    onValueChange = { range ->
                        viewModel.updateParams(
                            freqStart = (range.start * 100).roundToInt() / 100.0,
                            freqEnd = (range.endInclusive * 100).roundToInt() / 100.0
                        )
                    },
                    valueRange = 300f..928f,
                    enabled = !uiState.scanning,
                    colors = SliderDefaults.colors(thumbColor = MatrixGreen, activeTrackColor = MatrixGreen)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlassChip("433–435M", false, {
                        viewModel.updateParams(freqStart = 433.0, freqEnd = 435.0, step = 0.05)
                    }, NeonCyan)
                    GlassChip("868–870M", false, {
                        viewModel.updateParams(freqStart = 868.0, freqEnd = 870.0, step = 0.05)
                    }, NeonCyan)
                    GlassChip("300–928M", false, {
                        viewModel.updateParams(freqStart = 300.0, freqEnd = 928.0, step = 1.0)
                    }, NeonOrange)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.scanner_threshold, uiState.rssiThreshold),
                    fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TextGray
                )
                Slider(
                    value = uiState.rssiThreshold.toFloat(),
                    onValueChange = { viewModel.updateParams(threshold = it.roundToInt()) },
                    valueRange = -120f..-30f,
                    enabled = !uiState.scanning,
                    colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                )
            }

            // Controles
            Button(
                onClick = { if (uiState.scanning) viewModel.stopScanning() else viewModel.startScanning() },
                enabled = isReady,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.scanning) NeonRed else MatrixGreen
                )
            ) {
                Text(
                    if (uiState.scanning) stringResource(R.string.scanner_stop)
                    else stringResource(R.string.scanner_start),
                    fontFamily = FontFamily.Monospace,
                    color = BlackAMOLED,
                    fontWeight = FontWeight.Bold
                )
            }
            if (!isReady) {
                Text(
                    stringResource(R.string.scanner_requires_xibalba),
                    fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = NeonOrange,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

/** Gráfica RSSI (frecuencia vs dBm) con umbral dibujado. */
@Composable
private fun RfChartView(
    samples: List<Pair<Double, Int>>,
    freqStart: Double,
    freqEnd: Double,
    threshold: Int
) {
    GlassCard(accent = MatrixGreen, modifier = Modifier.fillMaxWidth()) {
        Text(
            "RSSI LIVE",
            fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MatrixGreen,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            val w = size.width
            val h = size.height
            val rssiMin = -120f
            val rssiMax = -20f
            val span = (freqEnd - freqStart).takeIf { it > 0 } ?: 1.0

            // Rejilla
            for (i in 1..3) {
                val y = h * i / 4f
                drawLine(Color(0xFF1A2A1A), Offset(0f, y), Offset(w, y), strokeWidth = 1f)
            }

            // Línea de umbral
            val thY = h - ((threshold - rssiMin) / (rssiMax - rssiMin)).coerceIn(0f, 1f) * h
            drawLine(NeonCyan.copy(alpha = 0.5f), Offset(0f, thY), Offset(w, thY), strokeWidth = 2f)

            // Muestras
            samples.forEach { (freq, rssi) ->
                val x = (((freq - freqStart) / span).coerceIn(0.0, 1.0)).toFloat() * w
                val y = h - ((rssi - rssiMin) / (rssiMax - rssiMin)).coerceIn(0f, 1f) * h
                val color = when {
                    rssi >= -50 -> NeonRed
                    rssi >= -70 -> NeonOrange
                    else -> MatrixGreen
                }
                drawCircle(color, radius = 3f, center = Offset(x, y))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("${freqStart}M", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = TextMuted)
            Text("-120 dBm → -20 dBm", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = TextMuted)
            Text("${freqEnd}M", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = TextMuted)
        }
    }
}

/** Mapa de calor RF: círculos coloreados por RSSI sobre osmdroid oscuro. */
@Composable
private fun RfHeatMapView(geoSamples: List<HybridLocationProvider.GeoRfSample>) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var mapView by remember { mutableStateOf<MapView?>(null) }

    DisposableEffect(Unit) {
        OsmdroidConfig.init(context)
        Configuration.getInstance().userAgentValue = context.packageName
        onDispose { mapView?.onDetach() }
    }

    GlassCard(accent = NeonCyan, modifier = Modifier.fillMaxWidth()) {
        Text(
            "RF HEATMAP (GPS)",
            fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = NeonCyan,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setTileSource(DarkMapTileSource)
                    setMultiTouchControls(true)
                    mapView = this
                }
            },
            update = { map ->
                map.overlays.clear()
                val geo = geoSamples.filter { it.latitude != null && it.longitude != null }
                geo.forEach { sample ->
                    val circle = Polygon(map).apply {
                        points = Polygon.pointsAsCircle(
                            GeoPoint(sample.latitude!!, sample.longitude!!), 12.0
                        )
                        fillPaint.color = when {
                            sample.rssi >= -50 -> 0x66FF0033
                            sample.rssi >= -70 -> 0x66FF6600
                            else -> 0x6600FF41
                        }
                        outlinePaint.color = 0x00000000
                    }
                    map.overlays.add(circle)
                }
                geo.lastOrNull()?.let { last ->
                    map.overlays.add(
                        Marker(map).apply {
                            position = GeoPoint(last.latitude!!, last.longitude!!)
                            title = "${last.freqMhz} MHz · ${last.rssi} dBm"
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        }
                    )
                    map.controller.setCenter(GeoPoint(last.latitude!!, last.longitude!!))
                    map.controller.setZoom(16.0)
                }
                map.invalidate()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        )
    }
}
