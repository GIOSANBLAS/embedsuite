package com.embedsuite.app

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embedsuite.app.connection.ConnectionState
import com.embedsuite.app.connection.FirmwareProfile
import com.embedsuite.app.connection.JammerMode
import com.embedsuite.app.ui.components.GlassCard
import com.embedsuite.app.ui.components.GlassChip
import com.embedsuite.app.ui.theme.*
import com.embedsuite.app.ui.viewmodel.JammerViewModel
import kotlin.math.roundToInt

/**
 * JammerScreen — control del jammer RF CC1101 (TEH-Link rf_jammer, Xibalba 0.20+).
 * Solo para uso en laboratorio/auditoría autorizada (ver aviso en pantalla).
 */
@Composable
fun JammerScreen(
    viewModel: JammerViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val detectedProfile by viewModel.detectedProfile.collectAsState()
    val isXibalba = detectedProfile == FirmwareProfile.XIBALBA
    val isConnected = connectionState is ConnectionState.Connected && isXibalba

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
                    stringResource(R.string.jammer_title),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonRed
                )
                Text(
                    stringResource(R.string.jammer_subtitle),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = TextGray
                )
            }
        }
        HorizontalDivider(color = NeonRed.copy(alpha = 0.35f))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            // Frecuencia
            GlassCard(accent = NeonRed, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                Text(
                    stringResource(R.string.jammer_freq, String.format("%.2f", uiState.freqMhz)),
                    fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NeonCyan
                )
                Slider(
                    value = uiState.freqMhz.toFloat(),
                    onValueChange = { viewModel.setFreq((it * 100).roundToInt() / 100.0) },
                    valueRange = 300f..928f,
                    enabled = !uiState.running,
                    colors = SliderDefaults.colors(thumbColor = NeonRed, activeTrackColor = NeonRed)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(315.0, 433.92, 868.35, 915.0).forEach { preset ->
                        GlassChip(
                            label = "${preset}M",
                            selected = uiState.freqMhz == preset,
                            onClick = { viewModel.setFreq(preset) },
                            accent = NeonCyan
                        )
                    }
                }
            }

            // Potencia
            GlassCard(accent = NeonOrange, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                Text(
                    stringResource(R.string.jammer_power, uiState.powerDbm),
                    fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NeonOrange
                )
                Slider(
                    value = uiState.powerDbm.toFloat(),
                    onValueChange = { viewModel.setPower(it.roundToInt()) },
                    valueRange = -30f..12f,
                    enabled = !uiState.running,
                    colors = SliderDefaults.colors(thumbColor = NeonOrange, activeTrackColor = NeonOrange)
                )
            }

            // Modo + duración
            GlassCard(accent = MatrixGreen, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                Text(
                    stringResource(R.string.jammer_mode),
                    fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = MatrixGreen
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlassChip(
                        label = stringResource(R.string.jammer_mode_continuous),
                        selected = uiState.mode == JammerMode.CONTINUOUS,
                        onClick = { viewModel.setMode(JammerMode.CONTINUOUS) },
                        accent = NeonRed
                    )
                    GlassChip(
                        label = stringResource(R.string.jammer_mode_burst),
                        selected = uiState.mode == JammerMode.BURST,
                        onClick = { viewModel.setMode(JammerMode.BURST) },
                        accent = NeonOrange
                    )
                }
                if (uiState.mode == JammerMode.BURST) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.jammer_burst_interval, uiState.burstIntervalMs),
                        fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TextGray
                    )
                    Slider(
                        value = uiState.burstIntervalMs.toFloat(),
                        onValueChange = { viewModel.setBurstInterval(it.roundToInt()) },
                        valueRange = 10f..1000f,
                        enabled = !uiState.running,
                        colors = SliderDefaults.colors(thumbColor = NeonOrange, activeTrackColor = NeonOrange)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.jammer_max_seconds, uiState.maxSeconds),
                    fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TextGray
                )
                Slider(
                    value = uiState.maxSeconds.toFloat(),
                    onValueChange = { viewModel.setMaxSeconds(it.roundToInt()) },
                    valueRange = 5f..300f,
                    enabled = !uiState.running,
                    colors = SliderDefaults.colors(thumbColor = MatrixGreen, activeTrackColor = MatrixGreen)
                )
            }

            // Activación
            Button(
                onClick = { if (uiState.running) viewModel.stopJammer() else viewModel.startJammer() },
                enabled = isConnected,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.running) NeonRed else MatrixGreen
                )
            ) {
                Text(
                    if (uiState.running) {
                        stringResource(R.string.jammer_stop, uiState.elapsedSeconds)
                    } else {
                        stringResource(R.string.jammer_start)
                    },
                    fontFamily = FontFamily.Monospace,
                    color = BlackAMOLED,
                    fontWeight = FontWeight.Bold
                )
            }

            if (!isConnected) {
                Text(
                    stringResource(R.string.jammer_requires_xibalba),
                    fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = NeonOrange,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (uiState.statusMessage.isNotBlank()) {
                Text(
                    uiState.statusMessage,
                    fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = NeonOrange,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Advertencia legal / de seguridad
            if (uiState.running) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = NeonRed.copy(alpha = 0.15f))
                ) {
                    Text(
                        stringResource(R.string.jammer_active_warning, String.format("%.2f", uiState.freqMhz)),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = NeonRed,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            Text(
                stringResource(R.string.jammer_legal_notice),
                fontFamily = FontFamily.Monospace,
                fontSize = 8.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}
