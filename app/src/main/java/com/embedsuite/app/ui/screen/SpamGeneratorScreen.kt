package com.embedsuite.app.ui.screen

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embedsuite.app.core.orchestrator.SpamIntent
import com.embedsuite.app.ui.theme.*
import com.embedsuite.app.ui.viewmodel.SpamGeneratorViewModel

@Composable
fun SpamGeneratorScreen(viewModel: SpamGeneratorViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()

    Column(
        Modifier.fillMaxSize().background(BlackAMOLED).padding(12.dp).verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = MatrixGreen) }
            Text("EN DISPOSITIVO", color = MatrixGreen, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
        Text(state.disclaimer, color = NeonOrange, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
        Spacer(Modifier.height(12.dp))

        Text("Plantilla local (export)", color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
        OutlinedTextField(value = state.ssid, onValueChange = viewModel::setSsid, label = { Text("SSID plantilla") }, modifier = Modifier.fillMaxWidth())
        Text("Paquetes (referencia local): ${state.packetCount}", color = NeonCyan, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
        Slider(
            value = state.packetCount.toFloat(),
            onValueChange = { viewModel.setPacketCount(it.toInt()) },
            valueRange = 1f..200f,
            colors = SliderDefaults.colors(thumbColor = MatrixGreen, activeTrackColor = MatrixGreen)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            SpamIntent.BleSpamProfile.entries.forEach { p ->
                FilterChip(selected = state.bleProfile == p, onClick = { viewModel.setBleProfile(p) }, label = { Text(p.name, fontSize = 8.sp) })
            }
        }

        Button(onClick = viewModel::exportLocalConfig, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MatrixGreen.copy(0.2f))) {
            Text("EXPORTAR JSON", fontFamily = FontFamily.Monospace)
        }

        Spacer(Modifier.height(16.dp))
        Text("Abrir en T-Embed (loader Bruce)", color = MatrixGreen, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Button(onClick = viewModel::openBleSpamOnDevice, modifier = Modifier.fillMaxWidth()) {
            Text("loader open BLE", fontFamily = FontFamily.Monospace)
        }
        Button(onClick = viewModel::openWifiMenuOnDevice, modifier = Modifier.fillMaxWidth()) {
            Text("loader open WiFi", fontFamily = FontFamily.Monospace)
        }

        if (state.status.isNotBlank()) {
            Text(state.status, color = NeonCyan, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
        }
        state.exportedPath?.let { Text("Guardado: $it", color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 8.sp) }
    }
}
