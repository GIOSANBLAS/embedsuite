package com.embedsuite.app.ui.screen

import androidx.compose.foundation.layout.*
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
import com.embedsuite.app.connection.TransportType
import com.embedsuite.app.core.wifi.WifiApManager
import com.embedsuite.app.ui.theme.*
import kotlinx.coroutines.launch

/** Wizard WiFi companion — AP Bruce + WebUI POST /cm. */
@Composable
fun WifiPairingWizardScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onConnected: () -> Unit = {}
) {
    val connectionState by container.connectionManager.connectionState.collectAsState()
    val cliReady by container.connectionManager.bruceLinkReady.collectAsState()
    val activeTransport by container.connectionManager.activeTransportType.collectAsState()
    var step by remember { mutableIntStateOf(0) }
    var status by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val discovery by container.autoDiscoveryManager.discoveryState.collectAsState()

    LaunchedEffect(Unit) {
        val d = container.autoDiscoveryManager.discover()
        status = d.message
    }

    LaunchedEffect(connectionState, cliReady, activeTransport) {
        if (connectionState is ConnectionState.Connected &&
            activeTransport == TransportType.WIFI &&
            cliReady
        ) {
            status = "WiFi CLI OK — WebUI responde"
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = MatrixGreen)
            }
            Text("Wizard WiFi Bruce", color = EmbedGreen, style = MaterialTheme.typography.titleLarge)
        }

        val steps = listOf(
            "1. En el T-Embed: menú WiFi → activa AP / WebUI",
            "2. Android detecta SSID Bruce_* o mDNS automáticamente",
            "3. EmbedSuite conecta WiFi y prueba CLI Bruce"
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
        if (discovery.message.isNotBlank()) {
            Text("Auto-discovery: ${discovery.message}", color = NeonCyan, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            discovery.host?.let {
                Text("Host: $it", color = MatrixGreen, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            }
        }
        if (status.isNotBlank() && status != discovery.message) {
            Text(status, color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
        }

        when (step) {
            0 -> {
                Text(
                    "Bruce expone CLI por HTTP POST http://${WifiApManager.DEFAULT_HOST}/cm con body cmnd=…",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Ideal para subir .txt/.sub pesados a la SD antes de badusb run_from_file.",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 6.dp)
                )
                Button(
                    onClick = { step = 1 },
                    colors = ButtonDefaults.buttonColors(containerColor = EmbedGreen.copy(0.2f), contentColor = EmbedGreen),
                    modifier = Modifier.padding(top = 12.dp)
                ) { Text("WebUI activa en el T-Embed") }
            }
            1 -> {
                Text(
                    "SSID típico: Bruce / T-Embed. IP gateway: ${WifiApManager.DEFAULT_HOST}",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                    OutlinedButton(onClick = { step = 0 }) { Text("Atrás") }
                    Button(
                        onClick = { step = 2 },
                        colors = ButtonDefaults.buttonColors(containerColor = EmbedGreen.copy(0.2f), contentColor = EmbedGreen)
                    ) { Text("Ya estoy en el WiFi del T-Embed") }
                }
            }
            else -> {
                Text(
                    "Conectando vía WiFi WebUI…",
                    color = NeonCyan,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 12.dp)) {
                    Button(
                        onClick = {
                            scope.launch {
                                status = "Conectando WiFi…"
                                container.connectionManager.connect(TransportType.WIFI).fold(
                                    onSuccess = {
                                        status = it
                                        container.connectionManager.refreshSystemInfo()
                                    },
                                    onFailure = { status = it.message ?: "WiFi falló" }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(0.2f), contentColor = NeonCyan)
                    ) { Text("Conectar WiFi") }
                    OutlinedButton(onClick = {
                        scope.launch {
                            container.connectionManager.sendBruceCliLine("info").fold(
                                onSuccess = { status = "info OK:\n${it.take(200)}" },
                                onFailure = { status = it.message ?: "info falló" }
                            )
                        }
                    }) { Text("Probar info") }
                }
                if (cliReady && activeTransport == TransportType.WIFI) {
                    Button(
                        onClick = onConnected,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = EmbedGreen.copy(0.25f), contentColor = EmbedGreen)
                    ) { Text("Continuar — WiFi CLI OK") }
                }
            }
        }

        if (status.isNotBlank()) {
            Text(status, color = NeonCyan, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        }
    }
}
