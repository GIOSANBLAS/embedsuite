package com.embedsuite.app.ui.components

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embedsuite.app.connection.BruceStorageSync
import com.embedsuite.app.connection.ConnectionState
import com.embedsuite.app.connection.DeviceConnectionManager
import com.embedsuite.app.connection.StorageFileEntry
import com.embedsuite.app.data.IrRepository
import com.embedsuite.app.data.NfcDumpRepository
import com.embedsuite.app.data.SignalRepository
import com.embedsuite.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun BruceSdSyncCard(
    storageSync: BruceStorageSync,
    connectionManager: DeviceConnectionManager,
    signalRepository: SignalRepository,
    irRepository: IrRepository,
    nfcDumpRepository: NfcDumpRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val connectionState by connectionManager.connectionState.collectAsState()

    var files by remember { mutableStateOf<List<StorageFileEntry>>(emptyList()) }
    var status by remember { mutableStateOf("Lista señales en SD del T-Embed (Bruce).") }
    var loading by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, KaliBlue.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "SYNC SD T-EMBED",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = KaliBlue
                )
                IconButton(
                    onClick = {
                        if (connectionState !is ConnectionState.Connected) {
                            status = "Conecta T-Embed primero."
                            return@IconButton
                        }
                        loading = true
                        status = "Listando storage..."
                        scope.launch {
                            storageSync.listSignalFiles().fold(
                                onSuccess = { list ->
                                    files = list
                                    status = if (list.isEmpty()) {
                                        "Sin .sub/.ir/.nfc en SD (prueba capturar en Bruce)."
                                    } else {
                                        "${list.size} archivos encontrados."
                                    }
                                },
                                onFailure = { status = "Error: ${it.message}" }
                            )
                            loading = false
                        }
                    },
                    enabled = !loading
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refrescar", tint = MatrixGreen)
                }
            }
            Text(status, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextGray)
            if (loading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    color = MatrixGreen
                )
            }
            if (files.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                LazyColumn(
                    modifier = Modifier.heightIn(max = 160.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(files, key = { it.path }) { file ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    file.name,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = MatrixGreen
                                )
                                Text(
                                    ".${file.extension}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 8.sp,
                                    color = TextGray
                                )
                            }
                            Row {
                                if (file.extension.equals("sub", ignoreCase = true)) {
                                    TextButton(
                                        onClick = {
                                            scope.launch {
                                                loading = true
                                                connectionManager.sendCommand(
                                                    com.embedsuite.app.connection.BruceCommands.subGhzTxFromFile(file.path)
                                                ).fold(
                                                    onSuccess = {
                                                        status = "TX OK: ${file.name}"
                                                        Toast.makeText(context, status, Toast.LENGTH_SHORT).show()
                                                    },
                                                    onFailure = { status = "TX falló: ${it.message}" }
                                                )
                                                loading = false
                                            }
                                        },
                                        enabled = !loading && connectionState is ConnectionState.Connected
                                    ) {
                                        Text("TX", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = NeonOrange)
                                    }
                                }
                                TextButton(
                                    onClick = {
                                        scope.launch {
                                            loading = true
                                            val result = when (file.extension.lowercase()) {
                                                "sub" -> storageSync.importSubFile(file.path).map {
                                                    signalRepository.saveImported(it)
                                                    "RF importado (replay OK): ${it.protocol}"
                                                }
                                                "ir" -> storageSync.importIrFile(file.path).map {
                                                    irRepository.save(it)
                                                    "IR importado: ${it.buttonName}"
                                                }
                                                "nfc" -> storageSync.importNfcFile(file.path).map {
                                                    nfcDumpRepository.save(it)
                                                    "NFC importado: ${it.uid}"
                                                }
                                                else -> Result.failure(Exception("Tipo no soportado"))
                                            }
                                            result.fold(
                                                onSuccess = { msg ->
                                                    status = msg
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                },
                                                onFailure = { status = "Import falló: ${it.message}" }
                                            )
                                            loading = false
                                        }
                                    },
                                    enabled = !loading
                                ) {
                                    Text("IMPORT", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = NeonCyan)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
