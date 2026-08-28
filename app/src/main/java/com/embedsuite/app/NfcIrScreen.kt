package com.embedsuite.app

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import com.embedsuite.app.connection.FirmwareProfile
import com.embedsuite.app.data.NfcDumpEntity
import com.embedsuite.app.flipper.FlipperFileManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embedsuite.app.connection.ConnectionState
import com.embedsuite.app.data.IrButtonEntity
import com.embedsuite.app.ui.components.MifareHexEditor
import com.embedsuite.app.ui.theme.*
import com.embedsuite.app.ui.viewmodel.NfcIrViewModel
import kotlinx.coroutines.launch

@Composable
fun NfcIrScreen(viewModel: NfcIrViewModel) {
    var showAddIr by remember { mutableStateOf(false) }
    var newIrName by remember { mutableStateOf("") }
    var newIrCommand by remember { mutableStateOf(com.embedsuite.app.connection.TehLinkIrUtils.irTx("NEC", "00FF", "00FF")) }

    val uiState by viewModel.uiState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val detectedProfile by viewModel.detectedProfile.collectAsState()
    val nfcDeviceEnabled by viewModel.nfcDeviceEnabled.collectAsState()
    val irDeviceEnabled by viewModel.irDeviceEnabled.collectAsState()
    val irButtons by viewModel.irButtons.collectAsState()
    val isConnected = connectionState is ConnectionState.Connected
    val isBruce = detectedProfile == FirmwareProfile.BRUCE
    val showNfcUnavailableBanner = isBruce && isConnected && !nfcDeviceEnabled
    val showIrUnavailableBanner = isBruce && isConnected && !irDeviceEnabled
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showNfcUnavailableBanner && uiState.modo == "NFC / RFID") {
            Text(
                stringResource(R.string.plus_compat_nfc_unavailable),
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = NeonOrange,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
        }
        if (showIrUnavailableBanner && uiState.modo == "INFRARED") {
            Text(
                stringResource(R.string.plus_compat_ir_unavailable),
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = NeonOrange,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            FilterChip(
                selected = uiState.modo == "NFC / RFID",
                onClick = { viewModel.setModo("NFC / RFID") },
                label = { Text("NFC / RFID 13.56MHz", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = if (uiState.modo == "NFC / RFID") BlackAMOLED else MatrixGreen) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MatrixGreen, containerColor = DarkSurface)
            )
            FilterChip(
                selected = uiState.modo == "INFRARED",
                onClick = { viewModel.setModo("INFRARED") },
                label = { Text("INFRARED (IR)", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = if (uiState.modo == "INFRARED") BlackAMOLED else NeonOrange) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NeonOrange, containerColor = DarkSurface)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.modo == "NFC / RFID") {
            if (uiState.savedDumps.isNotEmpty()) {
                Text("DUMPS GUARDADOS — EMULAR", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = NeonCyan, modifier = Modifier.fillMaxWidth())
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                    items(uiState.savedDumps, key = { it.id }) { dump ->
                        OutlinedButton(onClick = { viewModel.emulateFromDump(dump) }, enabled = isConnected && nfcDeviceEnabled) {
                            Text(dump.uid, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = MatrixGreen)
                        }
                    }
                }
            }
            NfcPanel(
                modifier = Modifier.weight(1f),
                estadoOperacion = uiState.estadoOperacion,
                nfcUid = uiState.nfcUid,
                nfcDump = uiState.nfcDump,
                parsedMifare = uiState.parsedMifare,
                mifareSectors = uiState.mifareSectors,
                isConnected = isConnected,
                controlsEnabled = isConnected && nfcDeviceEnabled,
                waitingHint = stringResource(R.string.nfc_waiting_teh_link),
                onRead = { viewModel.readNfc() },
                onEmulate = { viewModel.emulateUid() },
                onClearDump = { viewModel.clearDump() },
                onSaveDump = { viewModel.saveDump() },
                onBlockChanged = { sector, block, hex ->
                    viewModel.updateMifareBlock(sector, block, hex)
                },
                onExportNfc = {
                    if (uiState.nfcUid == "—" || uiState.nfcDump.isBlank()) {
                        Toast.makeText(context, "No hay dump para exportar", Toast.LENGTH_SHORT).show()
                    } else {
                        val dump = NfcDumpEntity(
                            uid = uiState.nfcUid,
                            tagType = "MIFARE Classic 1K",
                            rawDump = uiState.nfcDump,
                            parsedSectors = uiState.parsedMifare
                        )
                        val file = FlipperFileManager.writeNfcFile(context, dump)
                        context.startActivity(Intent.createChooser(FlipperFileManager.shareFile(context, file), "Exportar .nfc"))
                    }
                }
            )
        } else {
            IrPanel(
                modifier = Modifier.weight(1f),
                irButtons = irButtons,
                isConnected = isConnected,
                controlsEnabled = isConnected && (irDeviceEnabled || !isBruce),
                onSend = { viewModel.sendIr(it) },
                onCapture = { viewModel.captureIr() },
                onAdd = { showAddIr = true },
                onExportIr = { button ->
                    val file = FlipperFileManager.writeIrFile(context, button)
                    context.startActivity(Intent.createChooser(FlipperFileManager.shareFile(context, file), "Exportar .ir"))
                }
            )
        }
    }

    if (showAddIr) {
        AlertDialog(
            onDismissRequest = { showAddIr = false },
            containerColor = DarkSurface,
            title = { Text("Nuevo botón IR", fontFamily = FontFamily.Monospace, color = NeonOrange) },
            text = {
                Column {
                    OutlinedTextField(value = newIrName, onValueChange = { newIrName = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = newIrCommand,
                        onValueChange = { newIrCommand = it },
                        label = {
                            Text(
                                stringResource(R.string.nfc_ir_command_teh_link),
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.saveIrButton(newIrName, newIrCommand)
                    showAddIr = false
                    newIrName = ""
                }) { Text("Guardar", color = MatrixGreen) }
            },
            dismissButton = { TextButton(onClick = { showAddIr = false }) { Text("Cancelar", color = TextGray) } }
        )
    }
}

@Composable
private fun NfcPanel(
    modifier: Modifier = Modifier,
    estadoOperacion: String,
    nfcUid: String,
    nfcDump: String,
    parsedMifare: String,
    mifareSectors: List<com.embedsuite.app.nfc.MifareParser.SectorInfo>,
    isConnected: Boolean,
    controlsEnabled: Boolean = isConnected,
    waitingHint: String = "",
    onRead: () -> Unit,
    onEmulate: () -> Unit,
    onClearDump: () -> Unit,
    onSaveDump: () -> Unit,
    onBlockChanged: (sectorIndex: Int, blockIndex: Int, hex: String) -> Unit,
    onExportNfc: () -> Unit = {}
) {
    Box(
        modifier = Modifier.fillMaxWidth().height(140.dp).background(DarkSurface, RoundedCornerShape(8.dp))
            .border(1.dp, MatrixGreen.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Nfc, null, tint = MatrixGreen, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(estadoOperacion, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = NeonCyan)
            Text("UID: $nfcUid", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = MatrixGreen)
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Button(onClick = onRead, enabled = controlsEnabled, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MatrixGreen)) {
            Text("LEER TAG", fontFamily = FontFamily.Monospace, color = BlackAMOLED, fontSize = 11.sp)
        }
        OutlinedButton(onClick = onEmulate, enabled = controlsEnabled, modifier = Modifier.weight(1f)) {
            Text("EMULAR UID", fontFamily = FontFamily.Monospace, color = NeonCyan, fontSize = 11.sp)
        }
        OutlinedButton(onClick = onExportNfc, enabled = nfcDump.isNotBlank(), modifier = Modifier.weight(1f)) {
            Text("EXPORT .nfc", fontFamily = FontFamily.Monospace, color = MatrixGreen, fontSize = 10.sp)
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
    Card(colors = CardDefaults.cardColors(containerColor = DarkSurface), modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("MIFARE HEX EDITOR", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NeonCyan)
                Row {
                    TextButton(onClick = onSaveDump) { Text("GUARDAR", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = MatrixGreen) }
                    TextButton(onClick = onClearDump) { Text("LIMPIAR", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextGray) }
                }
            }
            if (mifareSectors.isNotEmpty()) {
                MifareHexEditor(
                    sectors = mifareSectors,
                    onBlockChanged = onBlockChanged,
                    enabled = true,
                    modifier = Modifier.heightIn(max = 320.dp)
                )
            } else if (parsedMifare.isNotBlank()) {
                Text(parsedMifare, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = NeonOrange, modifier = Modifier.padding(bottom = 4.dp))
            }
            if (nfcDump.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text("RAW DUMP", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = TextMuted)
                Text(
                    nfcDump,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                    color = MatrixGreen,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 80.dp).verticalScroll(rememberScrollState())
                )
            } else if (mifareSectors.isEmpty()) {
                Text(
                    waitingHint,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun IrPanel(
    modifier: Modifier = Modifier,
    irButtons: List<IrButtonEntity>,
    isConnected: Boolean,
    controlsEnabled: Boolean = isConnected,
    onSend: (String) -> Unit,
    onCapture: () -> Unit,
    onAdd: () -> Unit,
    onExportIr: (IrButtonEntity) -> Unit = {}
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("PANEL IR TÁCTIL", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = NeonOrange)
        Row {
            IconButton(onClick = onAdd) { Icon(Icons.Default.Add, "Añadir", tint = MatrixGreen) }
        }
    }
    LazyVerticalGrid(columns = GridCells.Fixed(3), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier.fillMaxWidth()) {
        items(irButtons, key = { it.id }) { button ->
            Column {
                Button(onClick = { onSend(button.irPayload) }, enabled = controlsEnabled, modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonOrange.copy(alpha = 0.85f)), shape = RoundedCornerShape(6.dp)) {
                    Text(button.buttonName, fontFamily = FontFamily.Monospace, color = BlackAMOLED, fontSize = 10.sp)
                }
                TextButton(onClick = { onExportIr(button) }, modifier = Modifier.fillMaxWidth()) {
                    Text(".ir", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = MatrixGreen)
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedButton(onClick = onCapture, enabled = controlsEnabled, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Sensors, null, tint = NeonOrange, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("CAPTURAR COMANDO IR", fontFamily = FontFamily.Monospace, color = NeonOrange, fontSize = 11.sp)
    }
}
