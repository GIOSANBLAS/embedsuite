package com.embedsuite.app.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.embedsuite.app.AppContainer
import com.embedsuite.app.connection.ConnectionState
import com.embedsuite.app.core.bruce.BruceStorageParser
import com.embedsuite.app.ui.theme.*
import kotlinx.coroutines.launch

/** Explorador SD companion — listar, leer e importar/ejecutar vía CLI Bruce. */
@Composable
fun BruceFilesScreen(
    container: AppContainer,
    onBack: () -> Unit
) {
    val connectionState by container.connectionManager.connectionState.collectAsState()
    val cliReady by container.connectionManager.bruceLinkReady.collectAsState()
    val connected = connectionState is ConnectionState.Connected
    var currentPath by remember { mutableStateOf("/bruce") }
    var entries by remember { mutableStateOf<List<BruceStorageParser.Entry>>(emptyList()) }
    var status by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun refresh() {
        scope.launch {
            status = "Listando $currentPath…"
            container.connectionManager.listStorageEntries(currentPath).fold(
                onSuccess = {
                    entries = it
                    status = "${it.size} entradas"
                },
                onFailure = { status = it.message ?: "Error listando SD" }
            )
        }
    }

    LaunchedEffect(currentPath, connected, cliReady) {
        if (connected && cliReady) refresh()
    }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = MatrixGreen)
            }
            Column(Modifier.weight(1f)) {
                Text("Archivos Bruce", color = EmbedGreen, style = MaterialTheme.typography.titleMedium)
                Text(currentPath, color = TextMuted, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = { refresh() }, enabled = connected && cliReady) {
                Icon(Icons.Default.Refresh, "Actualizar", tint = MatrixGreen)
            }
        }

        if (!connected || !cliReady) {
            Text("Conecta USB/BLE/WiFi y espera CLI OK.", color = NeonOrange, modifier = Modifier.padding(8.dp))
        }
        if (status.isNotBlank()) {
            Text(status, color = NeonCyan, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
            OutlinedButton(
                onClick = {
                    currentPath = BruceStorageParser.parentPath(currentPath)
                    preview = null
                },
                enabled = currentPath != "/"
            ) { Text("↑ Carpeta") }
            OutlinedButton(onClick = {
                currentPath = "/bruce"
                preview = null
            }) { Text("/bruce") }
            OutlinedButton(onClick = {
                currentPath = "/badusb"
                preview = null
            }) { Text("/badusb") }
        }

        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(entries, key = { "${currentPath}/${it.name}" }) { entry ->
                Card(colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)) {
                    Row(
                        Modifier.fillMaxWidth().padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                if (entry.isDir) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                                null,
                                tint = if (entry.isDir) NeonCyan else TextMuted
                            )
                            Column {
                                Text(entry.name, color = TextPrimary)
                                entry.sizeLabel?.let {
                                    Text(it, color = TextMuted, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (entry.isDir) {
                                TextButton(onClick = {
                                    currentPath = BruceStorageParser.childPath(currentPath, entry.name)
                                    preview = null
                                }) { Text("Abrir", color = MatrixGreen) }
                            } else {
                                TextButton(onClick = {
                                    val full = BruceStorageParser.childPath(currentPath, entry.name)
                                    scope.launch {
                                        container.connectionManager.readStorageFile(full).fold(
                                            onSuccess = { preview = it.take(2000) },
                                            onFailure = { status = it.message ?: "Lectura falló" }
                                        )
                                    }
                                }) { Text("Ver", color = NeonCyan) }
                                if (entry.name.endsWith(".ir", ignoreCase = true)) {
                                    TextButton(onClick = {
                                        val full = BruceStorageParser.childPath(currentPath, entry.name)
                                        scope.launch {
                                            container.connectionManager.readStorageFile(full).fold(
                                                onSuccess = { content ->
                                                    val btn = com.embedsuite.app.flipper.FlipperFileManager.parseIrFile(content)
                                                    if (btn != null) {
                                                        container.irRepository.save(
                                                            btn.copy(buttonName = entry.name.removeSuffix(".ir"))
                                                        )
                                                        status = "Importado IR: ${entry.name}"
                                                    } else {
                                                        status = "No se pudo parsear .ir"
                                                    }
                                                },
                                                onFailure = { status = it.message ?: "Import IR falló" }
                                            )
                                        }
                                    }) { Text("Import IR", color = MatrixGreen) }
                                }
                                if (entry.name.endsWith(".sub", ignoreCase = true)) {
                                    TextButton(onClick = {
                                        val full = BruceStorageParser.childPath(currentPath, entry.name)
                                        scope.launch {
                                            container.connectionManager.readStorageFile(full).fold(
                                                onSuccess = { content ->
                                                    val sig = com.embedsuite.app.flipper.FlipperFileManager.parseSubFile(content)
                                                    if (sig != null) {
                                                        container.signalRepository.saveImported(
                                                            sig.copy(label = entry.name, detail = "device:$full")
                                                        )
                                                        status = "Importado: ${entry.name}"
                                                    } else {
                                                        status = "No se pudo parsear .sub"
                                                    }
                                                },
                                                onFailure = { status = it.message ?: "Import falló" }
                                            )
                                        }
                                    }) { Text("Import", color = MatrixGreen) }
                                    TextButton(
                                        onClick = {
                                            val full = BruceStorageParser.childPath(currentPath, entry.name)
                                            scope.launch {
                                                container.rfReplayEngine.replayFromDeviceFile(full).fold(
                                                    onSuccess = { status = it },
                                                    onFailure = { status = it.message ?: "TX falló" }
                                                )
                                            }
                                        },
                                        enabled = cliReady
                                    ) { Text("TX RF", color = NeonOrange) }
                                }
                                if (entry.name.endsWith(".txt", ignoreCase = true)) {
                                    TextButton(
                                        onClick = {
                                            val full = BruceStorageParser.childPath(currentPath, entry.name)
                                            scope.launch {
                                                container.connectionManager.runBadUsbFromFile(full).fold(
                                                    onSuccess = { status = "BadUSB: $full" },
                                                    onFailure = { status = it.message ?: "BadUSB falló" }
                                                )
                                            }
                                        },
                                        enabled = cliReady
                                    ) { Text("BadUSB", color = NeonOrange) }
                                }
                            }
                        }
                    }
                }
            }
        }

        preview?.let { text ->
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Column(Modifier.padding(10.dp)) {
                    Text("Vista previa", color = NeonCyan, style = MaterialTheme.typography.labelSmall)
                    Text(text, color = TextPrimary, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
