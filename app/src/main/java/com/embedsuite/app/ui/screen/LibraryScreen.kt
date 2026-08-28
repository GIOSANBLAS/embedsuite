package com.embedsuite.app.ui.screen

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.embedsuite.app.AppContainer
import com.embedsuite.app.connection.ConnectionState
import com.embedsuite.app.core.bruce.BruceIrCommands
import com.embedsuite.app.data.CapturedSignalEntity
import com.embedsuite.app.engine.sync.IrdbParser
import com.embedsuite.app.flipper.FlipperZipExporter
import com.embedsuite.app.ui.theme.*
import kotlinx.coroutines.launch

/** Biblioteca companion — análisis local + envío/captura vía CLI Bruce (no espejo del T-Embed). */
@Composable
fun LibraryScreen(
    container: AppContainer,
    onOpenRf: () -> Unit,
    onOpenNfc: () -> Unit,
    onOpenIr: () -> Unit
) {
    val signals by container.signalRepository.allSignals.collectAsState(initial = emptyList())
    val nfcDumps by container.nfcDumpRepository.allDumps.collectAsState(initial = emptyList())
    val irButtons by container.irRepository.allButtons.collectAsState(initial = emptyList())
    val connectionState by container.connectionManager.connectionState.collectAsState()
    val cliReady by container.connectionManager.bruceLinkReady.collectAsState()
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    var status by remember { mutableStateOf("") }
    var irdbStatus by remember { mutableStateOf("") }
    var irdbFiles by remember { mutableStateOf<List<com.embedsuite.app.engine.sync.IrdbSync.IrdbEntry>>(emptyList()) }
    val connected = connectionState is ConnectionState.Connected

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Biblioteca", style = MaterialTheme.typography.headlineSmall, color = EmbedGreen)
            Text(
                "Suite companion: decodifica y guarda en el móvil; envía al CC1101/IR/NFC solo vía CLI Bruce.",
                color = TextMuted,
                style = MaterialTheme.typography.bodySmall
            )
            if (status.isNotBlank()) {
                Text(status, color = NeonCyan, style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpenRf) {
                    Icon(Icons.Default.SettingsInputAntenna, null)
                    Spacer(Modifier.width(4.dp))
                    Text("RF Hub")
                }
                OutlinedButton(onClick = onOpenNfc) {
                    Icon(Icons.Default.Nfc, null)
                    Spacer(Modifier.width(4.dp))
                    Text("NFC")
                }
                OutlinedButton(onClick = onOpenIr) {
                    Icon(Icons.Default.WbTwilight, null)
                    Spacer(Modifier.width(4.dp))
                    Text("IR")
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        scope.launch {
                            irdbStatus = "Sync IRDB…"
                            container.irdbSync.listRemoteIrFiles()
                                .onSuccess { list ->
                                    irdbFiles = list.take(100)
                                    irdbStatus = "${list.size} archivos .ir"
                                }
                                .onFailure { irdbStatus = it.message ?: "Error IRDB" }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmbedCyan.copy(alpha = 0.2f),
                        contentColor = EmbedCyan
                    )
                ) { Text("Sync IRDB") }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            container.connectionManager.captureSubGhzCompanion(15).fold(
                                onSuccess = { status = it },
                                onFailure = { status = it.message ?: "Captura falló" }
                            )
                        }
                    },
                    enabled = connected && cliReady
                ) {
                    Icon(Icons.Default.Sensors, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Capturar 15s")
                }
                OutlinedButton(onClick = {
                    scope.launch {
                        val zip = FlipperZipExporter.export(
                            ctx,
                            FlipperZipExporter.ExportBundle(
                                signals = signals.take(50),
                                irButtons = irButtons.take(50),
                                nfcDumps = nfcDumps.take(20)
                            )
                        )
                        ctx.startActivity(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "application/zip"
                                    putExtra(
                                        Intent.EXTRA_STREAM,
                                        androidx.core.content.FileProvider.getUriForFile(
                                            ctx, "${ctx.packageName}.fileprovider", zip
                                        )
                                    )
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                },
                                "Export Flipper .zip"
                            )
                        )
                    }
                }) { Text("Export .zip") }
            }
            if (irdbStatus.isNotBlank()) {
                Text(irdbStatus, color = NeonCyan, style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            Text("Sub-GHz (${signals.size})", color = NeonCyan, style = MaterialTheme.typography.titleSmall)
        }
        items(signals.take(50), key = { it.id }) { sig ->
            SignalLibraryRow(
                signal = sig,
                canSend = connected && cliReady,
                onSend = {
                    scope.launch {
                        container.rfReplayEngine.replay(sig).fold(
                            onSuccess = { status = it },
                            onFailure = { status = it.message ?: "TX falló" }
                        )
                    }
                }
            )
        }
        if (irdbFiles.isNotEmpty()) {
            item {
                Text("IRDB remoto (${irdbFiles.size})", color = NeonCyan, style = MaterialTheme.typography.titleSmall)
            }
            items(irdbFiles.take(15), key = { it.path }) { entry ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                ) {
                    Row(
                        Modifier.padding(10.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(entry.name, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                            Text(entry.category, color = TextMuted, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = {
                                scope.launch {
                                    container.irdbSync.downloadIrFile(entry.path).onSuccess { content ->
                                        IrdbParser.parse(content).forEach { btn ->
                                            val cli = BruceIrCommands.fromFlipperButton(btn)
                                            container.irRepository.save(
                                                com.embedsuite.app.data.IrButtonEntity(
                                                    buttonName = "${entry.name}/${btn.name}",
                                                    protocol = btn.protocol,
                                                    hexCode = btn.data,
                                                    irPayload = cli.ifBlank { btn.data }
                                                )
                                            )
                                        }
                                        Toast.makeText(ctx, "Importado: ${entry.name}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }) { Text("Importar", color = MatrixGreen) }
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        container.irdbSync.downloadIrFile(entry.path).onSuccess { content ->
                                            val btn = IrdbParser.parse(content).firstOrNull()
                                            val cli = btn?.let { BruceIrCommands.fromFlipperButton(it) }.orEmpty()
                                            if (cli.isBlank()) {
                                                status = "Sin comando IR mapeable en ${entry.name}"
                                                return@onSuccess
                                            }
                                            container.connectionManager.sendBruceCliLine(cli).fold(
                                                onSuccess = { status = "IR enviado: ${btn?.name ?: entry.name}" },
                                                onFailure = { status = it.message ?: "IR falló" }
                                            )
                                        }
                                    }
                                },
                                enabled = connected && cliReady
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(14.dp))
                                Text("Enviar", color = NeonOrange)
                            }
                        }
                    }
                }
            }
        }
        if (irButtons.isNotEmpty()) {
            item {
                Text("Botones IR (${irButtons.size})", color = NeonCyan, style = MaterialTheme.typography.titleSmall)
            }
            items(irButtons.take(20), key = { it.id }) { btn ->
                Card(colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)) {
                    Row(
                        Modifier.padding(10.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(btn.buttonName, color = TextPrimary)
                            Text(btn.irPayload.take(48), color = TextMuted, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(
                            onClick = {
                                scope.launch {
                                    val cmd = btn.irPayload.ifBlank { BruceIrCommands.irTx(btn.protocol, btn.hexCode, btn.hexCode) }
                                    container.connectionManager.sendBruceCliLine(cmd).fold(
                                        onSuccess = { status = "IR OK: ${btn.buttonName}" },
                                        onFailure = { status = it.message ?: "IR falló" }
                                    )
                                }
                            },
                            enabled = connected && cliReady
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, "Enviar IR", tint = NeonOrange)
                        }
                    }
                }
            }
        }
        item {
            Text("NFC (${nfcDumps.size})", color = NeonCyan, style = MaterialTheme.typography.titleSmall)
        }
        items(nfcDumps.take(20), key = { it.id }) { dump ->
            Card(colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)) {
                Column(Modifier.padding(12.dp)) {
                    Text(dump.uid.ifBlank { "NFC #${dump.id}" }, color = TextPrimary)
                    Text(dump.tagType, color = TextMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun SignalLibraryRow(
    signal: CapturedSignalEntity,
    canSend: Boolean,
    onSend: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)) {
        Row(
            Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(signal.label.ifBlank { signal.name }.ifBlank { "Captura #${signal.id}" }, color = TextPrimary)
                Text(
                    "${signal.frequency} · ${signal.protocol}",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onSend, enabled = canSend && signal.signalType == "RF") {
                Icon(Icons.AutoMirrored.Filled.Send, "Enviar al T-Embed", tint = NeonOrange)
            }
        }
    }
}
