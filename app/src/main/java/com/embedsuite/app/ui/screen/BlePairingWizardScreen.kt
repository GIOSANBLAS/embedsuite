package com.embedsuite.app.ui.screen

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embedsuite.app.AppContainer
import com.embedsuite.app.connection.ConnectionState
import com.embedsuite.app.core.ble.BleScanEntry
import com.embedsuite.app.ui.theme.*
import kotlinx.coroutines.launch

/** Wizard BLE companion — activar BLE API en Bruce + emparejar + conectar GATT. */
@Composable
fun BlePairingWizardScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onConnected: () -> Unit = {}
) {
    val scanResults by container.bleScanner.results.collectAsState()
    val scanning by container.bleScanner.scanning.collectAsState()
    val connectionState by container.connectionManager.connectionState.collectAsState()
    val cliReady by container.connectionManager.bruceLinkReady.collectAsState()
    var step by remember { mutableIntStateOf(0) }
    var status by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(connectionState, cliReady) {
        if (connectionState is ConnectionState.Connected && cliReady && step >= 2) {
            status = "CLI OK — listo para control remoto"
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = MatrixGreen)
            }
            Text("Wizard BLE Bruce", color = EmbedGreen, style = MaterialTheme.typography.titleLarge)
        }

        val steps = listOf(
            "1. En el T-Embed: Config → BLE → activa BLE API",
            "2. Empareja el T-Embed en Ajustes Bluetooth del teléfono",
            "3. Escanea y conecta desde EmbedSuite"
        )
        steps.forEachIndexed { i, label ->
            Text(
                label,
                color = if (i <= step) MatrixGreen else TextMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        when (step) {
            0 -> {
                Text(
                    "El firmware Bruce expone CLI serial por GATT solo con BLE API activada.",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
                Button(
                    onClick = { step = 1 },
                    colors = ButtonDefaults.buttonColors(containerColor = EmbedGreen.copy(0.2f), contentColor = EmbedGreen),
                    modifier = Modifier.padding(top = 12.dp)
                ) { Text("BLE API activada en el T-Embed") }
            }
            1 -> {
                Text(
                    "Busca T-Embed o Bruce en Bluetooth del teléfono y empareja antes de continuar.",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                    OutlinedButton(onClick = { step = 0 }) { Text("Atrás") }
                    Button(
                        onClick = {
                            step = 2
                            container.bleScanner.startScan()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmbedGreen.copy(0.2f), contentColor = EmbedGreen)
                    ) { Text("Ya emparejé — Escanear") }
                }
            }
            else -> {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (scanning) "Escaneando…" else "${scanResults.size} dispositivos",
                        color = NeonCyan,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { container.bleScanner.stopScan() }) {
                            Text("Parar", color = TextMuted)
                        }
                        TextButton(onClick = { container.bleScanner.startScan() }) {
                            Text("Escanear", color = MatrixGreen)
                        }
                    }
                }
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(scanResults, key = { it.device.address }) { entry ->
                        BleDeviceRow(entry) { device ->
                            scope.launch {
                                status = "Conectando ${entry.name}…"
                                container.bleScanner.stopScan()
                                container.connectionManager.connectBleDevice(device).fold(
                                    onSuccess = {
                                        status = it
                                        onConnected()
                                    },
                                    onFailure = { status = it.message ?: "BLE falló" }
                                )
                            }
                        }
                    }
                }
                if (cliReady) {
                    Button(
                        onClick = onConnected,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmbedGreen.copy(0.25f), contentColor = EmbedGreen)
                    ) { Text("Continuar — CLI OK") }
                }
            }
        }

        if (status.isNotBlank()) {
            Text(status, color = NeonCyan, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun BleDeviceRow(entry: BleScanEntry, onConnect: (BluetoothDevice) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(entry.name, color = TextPrimary)
                Text("${entry.device.address} · ${entry.rssi} dBm", color = TextMuted, fontSize = 10.sp)
            }
            Button(
                onClick = { onConnect(entry.device) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (entry.isTEmbedCandidate) EmbedGreen.copy(0.25f) else DarkSurface,
                    contentColor = if (entry.isTEmbedCandidate) EmbedGreen else TextMuted
                )
            ) { Text("Conectar") }
        }
    }
}
