package com.embedsuite.app

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embedsuite.app.core.SessionStatsTracker
import com.embedsuite.app.scan.WirelessDevice
import com.embedsuite.app.ui.components.ScanPermissionsGate
import com.embedsuite.app.ui.theme.*
import com.embedsuite.app.ui.viewmodel.WirelessViewModel
import kotlinx.coroutines.launch

@Composable
fun WirelessScreen(
    viewModel: WirelessViewModel,
    sessionStats: SessionStatsTracker? = null,
    onSaveDevice: suspend (WirelessDevice) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val devices by viewModel.devices.collectAsState()
    val isBleScanning by viewModel.isBleScanning.collectAsState()
    val context = LocalContext.current

    val listaFiltrada = devices.filter {
        when (uiState.filtro) {
            "WI-FI" -> it.type == "WIFI"
            "BLE" -> it.type == "BLE"
            else -> true
        }
    }
    var showWarDriveLegal by remember { mutableStateOf(false) }

    if (showWarDriveLegal) {
        AlertDialog(
            onDismissRequest = { showWarDriveLegal = false },
            containerColor = DarkSurface,
            title = {
                Text(
                    stringResource(R.string.wireless_war_legal_title),
                    fontFamily = FontFamily.Monospace,
                    color = NeonOrange
                )
            },
            text = {
                Text(
                    stringResource(R.string.wireless_war_legal_body),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = TextGray
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showWarDriveLegal = false
                    viewModel.setWarDriving(true)
                }) {
                    Text(stringResource(R.string.wireless_war_legal_accept), color = MatrixGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWarDriveLegal = false }) {
                    Text(stringResource(R.string.action_cancel), color = TextGray)
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text(stringResource(R.string.wireless_title), fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = MatrixGreen, modifier = Modifier.padding(bottom = 8.dp))

        ScanPermissionsGate(onGranted = { viewModel.setFiltro(uiState.filtro) }) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    stringResource(R.string.wireless_filter_all) to "TODOS",
                    stringResource(R.string.wireless_filter_wifi) to "WI-FI",
                    stringResource(R.string.wireless_filter_ble) to "BLE"
                ).forEach { (label, filtro) ->
                    FilterChip(
                        selected = uiState.filtro == filtro,
                        onClick = { viewModel.setFiltro(filtro) },
                        label = { Text(label, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = if (uiState.filtro == filtro) BlackAMOLED else MatrixGreen) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MatrixGreen, containerColor = DarkSurface)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(uiState.scanStatus, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TextGray)
                Row {
                    FilterChip(
                        selected = uiState.warDriving,
                        onClick = {
                            if (uiState.warDriving) {
                                viewModel.setWarDriving(false)
                            } else {
                                showWarDriveLegal = true
                            }
                        },
                        label = { Text(stringResource(R.string.wireless_war_drive), fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = if (uiState.warDriving) BlackAMOLED else NeonOrange) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NeonOrange, containerColor = DarkSurface)
                    )
                    IconButton(onClick = {
                        viewModel.scanAll(onSaveDevice)
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.wireless_scan_cd), tint = MatrixGreen)
                    }
                }
            }
            if (isBleScanning) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), color = NeonCyan, trackColor = DarkSurface)
            }
            if (uiState.gattReadResult.isNotBlank()) {
                Text(uiState.gattReadResult, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = NeonCyan, modifier = Modifier.padding(vertical = 4.dp))
            }
            Text(stringResource(R.string.wireless_detected, listaFiltrada.size), fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NeonCyan, modifier = Modifier.padding(vertical = 6.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(listaFiltrada) { item ->
                    TarjetaDispositivo(
                        dispositivo = item,
                        gattExpanded = uiState.expandedGattAddress == item.mac,
                        gattConnecting = uiState.gattConnecting == item.mac,
                        gattServices = if (uiState.expandedGattAddress == item.mac) uiState.gattServices else emptyList(),
                        onToggleGatt = { viewModel.toggleGattExpand(item.mac) },
                        onConnectGatt = { viewModel.connectGatt(item.mac) },
                        onReadChar = { svc, ch -> viewModel.readCharacteristic(svc, ch) },
                        onWriteChar = { svc, ch -> viewModel.showWriteDialog(svc, ch) },
                        onSubscribeChar = { svc, ch -> viewModel.subscribeCharacteristic(svc, ch) },
                        subscribedChar = uiState.subscribedChar,
                        onSaveProfile = {
                            viewModel.saveBleProfile(item)
                            Toast.makeText(context, "Perfil BLE guardado", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    uiState.writeTarget?.let {
        AlertDialog(
            onDismissRequest = { viewModel.dismissWriteDialog() },
            containerColor = DarkSurface,
            title = { Text("Escribir GATT (HEX)", fontFamily = FontFamily.Monospace, color = NeonCyan) },
            text = {
                OutlinedTextField(
                    value = uiState.writeHexInput,
                    onValueChange = { viewModel.setWriteHex(it) },
                    label = { Text("Bytes hex") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmWrite() }) { Text("WRITE", color = MatrixGreen) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissWriteDialog() }) { Text("Cancelar", color = TextGray) }
            }
        )
    }
}

@Composable
fun TarjetaDispositivo(
    dispositivo: WirelessDevice,
    gattExpanded: Boolean = false,
    gattConnecting: Boolean = false,
    gattServices: List<com.embedsuite.app.scan.GattServiceInfo> = emptyList(),
    onToggleGatt: () -> Unit = {},
    onConnectGatt: () -> Unit = {},
    onReadChar: (String, String) -> Unit = { _, _ -> },
    onWriteChar: (String, String) -> Unit = { _, _ -> },
    onSubscribeChar: (String, String) -> Unit = { _, _ -> },
    subscribedChar: String? = null,
    onSaveProfile: () -> Unit = {}
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, MatrixGreen.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (dispositivo.type == "WIFI") Icons.Default.Wifi else Icons.Default.Bluetooth,
                        contentDescription = null,
                        tint = if (dispositivo.type == "WIFI") NeonCyan else NeonOrange,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(dispositivo.name, fontFamily = FontFamily.Monospace, color = MatrixGreen, fontSize = 14.sp)
                        Text("MAC: ${dispositivo.mac}", fontFamily = FontFamily.Monospace, color = TextGray, fontSize = 10.sp)
                        Text(dispositivo.detail, fontFamily = FontFamily.Monospace, color = NeonCyan.copy(alpha = 0.8f), fontSize = 10.sp)
                    }
                }
                Surface(color = when { dispositivo.rssi > -60 -> MatrixGreen.copy(alpha = 0.2f); dispositivo.rssi > -75 -> NeonOrange.copy(alpha = 0.2f); else -> DarkSurface }, shape = RoundedCornerShape(4.dp)) {
                    Text("${dispositivo.rssi} dBm", fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                        color = when { dispositivo.rssi > -60 -> MatrixGreen; dispositivo.rssi > -75 -> NeonOrange; else -> TextGray },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
            if (dispositivo.type == "BLE") {
                Row {
                    TextButton(onClick = onConnectGatt, enabled = !gattConnecting) {
                        Text(if (gattConnecting) "CONECTANDO..." else "CONECTAR GATT", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = NeonCyan)
                    }
                    TextButton(onClick = onToggleGatt) {
                        Text(if (gattExpanded) "OCULTAR" else "ADV DATA", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextGray)
                    }
                    TextButton(onClick = onSaveProfile) {
                        Text("GUARDAR", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = MatrixGreen)
                    }
                }
                if (gattExpanded && dispositivo.gattDetail.isNotBlank()) {
                    Text(dispositivo.gattDetail, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextGray,
                        modifier = Modifier.fillMaxWidth().background(BlackAMOLED, RoundedCornerShape(4.dp)).padding(8.dp))
                }
                gattServices.forEach { svc ->
                    Text("SVC ${svc.uuid.take(8)}...", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = NeonOrange)
                    svc.characteristics.filter { it.canRead || it.canWrite || it.canNotify }.take(6).forEach { ch ->
                        Row {
                            if (ch.canRead) {
                                TextButton(onClick = { onReadChar(svc.uuid, ch.uuid) }) {
                                    Text("R ${ch.uuid.take(8)}", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = MatrixGreen)
                                }
                            }
                            if (ch.canWrite) {
                                TextButton(onClick = { onWriteChar(svc.uuid, ch.uuid) }) {
                                    Text("W ${ch.uuid.take(8)}", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = NeonOrange)
                                }
                            }
                            if (ch.canNotify) {
                                TextButton(onClick = { onSubscribeChar(svc.uuid, ch.uuid) }) {
                                    Text(
                                        if (subscribedChar == ch.uuid) "● N ${ch.uuid.take(6)}" else "N ${ch.uuid.take(8)}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.sp,
                                        color = NeonCyan
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
