package com.embedsuite.app

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.embedsuite.app.connection.FirmwareProfile
import com.embedsuite.app.connection.WifiTransport
import com.embedsuite.app.connection.ConnectionState
import com.embedsuite.app.connection.DeviceConnectionManager
import com.embedsuite.app.connection.FirmwareRelease
import com.embedsuite.app.connection.OtaUpdateStatus
import com.embedsuite.app.connection.TransportType
import com.embedsuite.app.data.MacroEntity
import com.embedsuite.app.data.MacroRepository
import com.embedsuite.app.data.ProfileRepository
import com.embedsuite.app.flipper.FlipperFileManager
import com.embedsuite.app.flash.FirmwareFlashCoordinator
import com.embedsuite.app.macro.MacroEngine
import com.embedsuite.app.scan.LocationTracker
import com.embedsuite.app.ui.components.FirmwareFlashCard
import com.embedsuite.app.ui.components.LinkDebugPanel
import com.embedsuite.app.ui.components.HeatmapMapView
import com.embedsuite.app.ui.components.OfflineMapCard
import com.embedsuite.app.ui.components.RfAutomationCard
import com.embedsuite.app.ui.components.ScanPermissionsGate
import com.embedsuite.app.ui.components.HackerSectionHeader
import com.embedsuite.app.ui.components.WarDrivingMapView
import com.embedsuite.app.ui.theme.*
import com.embedsuite.app.ui.viewmodel.MapToolsViewModel
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun MapToolsScreen(
    viewModel: MapToolsViewModel,
    connectionManager: DeviceConnectionManager,
    locationTracker: LocationTracker,
    flashCoordinator: FirmwareFlashCoordinator,
    macroRepository: MacroRepository,
    macroEngine: MacroEngine,
    profileRepository: ProfileRepository,
    rfAutomationRepository: com.embedsuite.app.data.RfAutomationRepository,
    mapTileCacheManager: com.embedsuite.app.map.MapTileCacheManager,
    signalRepository: com.embedsuite.app.data.SignalRepository,
    irRepository: com.embedsuite.app.data.IrRepository,
    nfcDumpRepository: com.embedsuite.app.data.NfcDumpRepository
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val detectedProfile by connectionManager.detectedProfile.collectAsState()
    val systemInfo by viewModel.systemInfo.collectAsState()
    val location by viewModel.location.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var wifiHost by remember { mutableStateOf(WifiTransport.DEFAULT_HOST) }
    var selectedTransport by remember { mutableStateOf(TransportType.USB) }
    val otaProgress by flashCoordinator.otaProgress.collectAsState()
    val isFlashing by flashCoordinator.isFlashing.collectAsState()
    val flashStatus by flashCoordinator.flashStatus.collectAsState()
    var showHeatmapFullscreen by remember { mutableStateOf(false) }
    var mapLayer by remember { mutableStateOf("ALL") }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.parseImportFile(context, it) }
    }

    val customFirmwareLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importCustomFirmware(context, it) }
    }

    if (showHeatmapFullscreen) {
        Dialog(onDismissRequest = { showHeatmapFullscreen = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxSize().background(BlackAMOLED)) {
                Column(Modifier.fillMaxSize().padding(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.map_heatmap_title), fontFamily = FontFamily.Monospace, color = MatrixGreen)
                        IconButton(onClick = { showHeatmapFullscreen = false }) {
                            Icon(Icons.Default.Close, null, tint = NeonRed)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("ALL", "RF", "WIFI", "BLE").forEach { layer ->
                            FilterChip(
                                selected = mapLayer == layer,
                                onClick = { mapLayer = layer },
                                label = {
                                    Text(
                                        layer,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp,
                                        color = if (mapLayer == layer) BlackAMOLED else MatrixGreen
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MatrixGreen,
                                    containerColor = DarkSurfaceElevated
                                )
                            )
                        }
                    }
                    HeatmapMapView(
                        signals = uiState.mappedSignals,
                        currentLat = location?.latitude,
                        currentLng = location?.longitude,
                        layerFilter = mapLayer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    uiState.importPreview?.let { preview ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissImportPreview() },
            containerColor = DarkSurface,
            title = { Text(stringResource(R.string.map_import_title, preview.type), fontFamily = FontFamily.Monospace, color = NeonCyan) },
            text = {
                Column {
                    Text(preview.fileName, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MatrixGreen)
                    Text(preview.summary, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TextGray)
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.confirmImport() }) { Text(stringResource(R.string.action_import), color = MatrixGreen) } },
            dismissButton = { TextButton(onClick = { viewModel.dismissImportPreview() }) { Text(stringResource(R.string.action_cancel), color = TextGray) } }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)
    ) {
        HackerSectionHeader("MAP & TOOLS // T-EMBED CC1101", accent = MatrixGreen)

        when (val ota = uiState.otaStatus) {
            is OtaUpdateStatus.UpdateAvailable -> {
                Card(colors = CardDefaults.cardColors(containerColor = DarkSurface), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            stringResource(
                                R.string.map_ota_available,
                                ota.sourceLabel,
                                ota.latestVersion,
                                ota.deviceVersion
                            ),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = NeonOrange
                        )
                    }
                }
            }
            else -> {}
        }

        ConnectionCard(
            connectionState = connectionState,
            selectedTransport = selectedTransport,
            wifiHost = wifiHost,
            onTransportChange = { selectedTransport = it },
            onWifiHostChange = { wifiHost = it },
            onConnect = {
                scope.launch {
                    connectionManager.setWifiHost(wifiHost)
                    connectionManager.connect(selectedTransport)
                }
            },
            onDisconnect = { scope.launch { connectionManager.disconnect() } },
            onRefreshInfo = { scope.launch { connectionManager.refreshSystemInfo() } }
        )

        Spacer(modifier = Modifier.height(12.dp))
        SystemMonitorCard(systemInfo, connectionState)

        Spacer(modifier = Modifier.height(12.dp))

        ScanPermissionsGate(onGranted = { locationTracker.startTracking() }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, MatrixGreen.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.map_war_driving), fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = NeonCyan)
                        TextButton(onClick = { showHeatmapFullscreen = true }) {
                            Text(stringResource(R.string.map_heatmap_full), fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = NeonOrange)
                        }
                    }
                    location?.let {
                        Text(
                            stringResource(R.string.map_gps_coords, "%.5f".format(it.latitude), "%.5f".format(it.longitude)),
                            fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextGray
                        )
                    }
                    WarDrivingMapView(
                        signals = uiState.mappedSignals,
                        currentLat = location?.latitude,
                        currentLng = location?.longitude
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OfflineMapCard(
            mapTileCacheManager = mapTileCacheManager,
            currentLat = location?.latitude,
            currentLng = location?.longitude
        )

        Spacer(modifier = Modifier.height(12.dp))

        RfAutomationCard(
            repository = rfAutomationRepository,
            macroRepository = macroRepository
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
        ) {
            LinkDebugPanel(modifier = Modifier.padding(10.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        ExportCard(
            exportStatus = uiState.exportStatus.ifBlank {
                stringResource(R.string.map_export_signals_count, uiState.signalCount)
            },
            onExportJson = { scope.launch { viewModel.exportJson().fold(onSuccess = { f -> viewModel.setExportStatus(context.getString(R.string.map_export_status_json, f.name)); Toast.makeText(context, f.absolutePath, Toast.LENGTH_LONG).show() }, onFailure = { viewModel.setExportStatus(context.getString(R.string.map_export_error, it.message ?: "?")) }) } },
            onExportCsv = { scope.launch { viewModel.exportCsv().fold(onSuccess = { f -> viewModel.setExportStatus(context.getString(R.string.map_export_status_csv, f.name)); Toast.makeText(context, f.absolutePath, Toast.LENGTH_LONG).show() }, onFailure = { viewModel.setExportStatus(context.getString(R.string.map_export_error, it.message ?: "?")) }) } },
            onExportKml = { scope.launch { viewModel.exportKml().fold(onSuccess = { f -> viewModel.setExportStatus(context.getString(R.string.map_export_status_kml, f.name)); Toast.makeText(context, f.absolutePath, Toast.LENGTH_LONG).show() }, onFailure = { viewModel.setExportStatus(context.getString(R.string.map_export_error, it.message ?: "?")) }) } },
            onBackup = { scope.launch { viewModel.exportBackup().fold(onSuccess = { f -> viewModel.setExportStatus(context.getString(R.string.map_export_status_backup, f.name)); Toast.makeText(context, f.absolutePath, Toast.LENGTH_LONG).show() }, onFailure = { viewModel.setExportStatus(context.getString(R.string.map_export_backup_error, it.message ?: "?")) }) } },
            onExportSub = {
                scope.launch {
                    val file = viewModel.exportSub(context)
                    if (file != null) {
                        context.startActivity(Intent.createChooser(FlipperFileManager.shareFile(context, file), context.getString(R.string.map_share_sub)))
                        viewModel.setExportStatus(context.getString(R.string.map_export_status_sub, file.name))
                    } else viewModel.setExportStatus(context.getString(R.string.map_export_no_rf))
                }
            },
            onImport = { importLauncher.launch(arrayOf("text/*", "application/json", "*/*")) },
            onExportHtml = {
                scope.launch {
                    viewModel.exportSessionHtml().fold(
                        onSuccess = { f -> viewModel.setExportStatus(context.getString(R.string.map_export_status_html, f.name)); Toast.makeText(context, f.absolutePath, Toast.LENGTH_LONG).show() },
                        onFailure = { viewModel.setExportStatus(context.getString(R.string.map_export_error, it.message ?: "?")) }
                    )
                }
            },
            onExportPdf = {
                scope.launch {
                    viewModel.exportSessionPdf().fold(
                        onSuccess = { f -> viewModel.setExportStatus(context.getString(R.string.map_export_status_pdf, f.name)); Toast.makeText(context, f.absolutePath, Toast.LENGTH_LONG).show() },
                        onFailure = { viewModel.setExportStatus(context.getString(R.string.map_export_error, it.message ?: "?")) }
                    )
                }
            },
            onExportToDeviceSd = {
                scope.launch {
                    viewModel.exportJsonToDeviceSd().fold(
                        onSuccess = { path ->
                            viewModel.setExportStatus(context.getString(R.string.map_export_status_device_sd, path))
                            Toast.makeText(context, path, Toast.LENGTH_LONG).show()
                        },
                        onFailure = {
                            viewModel.setExportStatus(
                                context.getString(R.string.map_export_error, it.message ?: "?")
                            )
                        }
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        MacroPanel(
            macroRepository = macroRepository,
            macroEngine = macroEngine,
            isConnected = connectionState is ConnectionState.Connected
        )

        Spacer(modifier = Modifier.height(12.dp))

        ProfilesPanel(
            profileRepository = profileRepository,
            macroEngine = macroEngine,
            isConnected = connectionState is ConnectionState.Connected
        )

        Spacer(modifier = Modifier.height(12.dp))

        FirmwareFlashCard(
            otaProgress = otaProgress,
            flashStatus = flashStatus.ifBlank { stringResource(R.string.firmware_status_ready) },
            lastOta = systemInfo.lastOta,
            hardening = systemInfo.hardening,
            firmwareOptions = uiState.allFirmwareOptions,
            selectedRelease = uiState.selectedRelease,
            recommendedRelease = uiState.recommendedRelease,
            isLoadingReleases = uiState.isLoadingReleases,
            isFlashing = isFlashing,
            onLoadReleases = {
                viewModel.loadReleases()
                flashCoordinator.setStatusMessage(context.getString(R.string.firmware_status_fetching))
            },
            onSelectRelease = { viewModel.selectRelease(it) },
            onPickCustomBin = { customFirmwareLauncher.launch(arrayOf("application/octet-stream", "*/*")) },
            onClearCustom = { viewModel.clearCustomFirmware() },
            onFlashOta = { release ->
                val previous = uiState.recommendedRelease ?: uiState.releases.firstOrNull()
                scope.launch {
                    val result = flashCoordinator.flashWithRollback(
                        release = release,
                        previousRelease = if (release.identityKey() != previous?.identityKey()) previous else null
                    )
                    flashCoordinator.setStatusMessage(result.message)
                }
            },
            onFlashUsb = { release -> flashCoordinator.flashUsb(context, release) }
        )
    }
}

@Composable
private fun ExportCard(
    exportStatus: String,
    onExportJson: () -> Unit,
    onExportCsv: () -> Unit,
    onExportKml: () -> Unit,
    onBackup: () -> Unit = {},
    onExportSub: () -> Unit = {},
    onImport: () -> Unit = {},
    onExportHtml: () -> Unit = {},
    onExportPdf: () -> Unit = {},
    onExportToDeviceSd: () -> Unit = {}
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(stringResource(R.string.map_export_section), fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = NeonCyan)
            Text(exportStatus, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TextGray, modifier = Modifier.padding(vertical = 4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onExportJson, modifier = Modifier.weight(1f)) { Text("JSON", fontFamily = FontFamily.Monospace, color = MatrixGreen, fontSize = 10.sp) }
                OutlinedButton(onClick = onExportCsv, modifier = Modifier.weight(1f)) { Text("CSV", fontFamily = FontFamily.Monospace, color = MatrixGreen, fontSize = 10.sp) }
                OutlinedButton(onClick = onExportKml, modifier = Modifier.weight(1f)) { Text("KML", fontFamily = FontFamily.Monospace, color = MatrixGreen, fontSize = 10.sp) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                OutlinedButton(onClick = onExportSub, modifier = Modifier.weight(1f)) { Text(".sub", fontFamily = FontFamily.Monospace, color = NeonOrange, fontSize = 10.sp) }
                OutlinedButton(onClick = onBackup, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.map_export_backup_btn), fontFamily = FontFamily.Monospace, color = NeonCyan, fontSize = 10.sp) }
                OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.map_export_import_btn), fontFamily = FontFamily.Monospace, color = NeonOrange, fontSize = 10.sp) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                OutlinedButton(onClick = onExportHtml, modifier = Modifier.weight(1f)) { Text("HTML", fontFamily = FontFamily.Monospace, color = MatrixGreen, fontSize = 10.sp) }
                OutlinedButton(onClick = onExportPdf, modifier = Modifier.weight(1f)) { Text("PDF", fontFamily = FontFamily.Monospace, color = MatrixGreen, fontSize = 10.sp) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                OutlinedButton(onClick = onExportToDeviceSd, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.map_export_to_device_sd),
                        fontFamily = FontFamily.Monospace,
                        color = NeonCyan,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MacroPanel(
    macroRepository: MacroRepository,
    macroEngine: MacroEngine,
    isConnected: Boolean
) {
    val scope = rememberCoroutineScope()
    val macros by macroRepository.allMacros.collectAsState(initial = emptyList())
    var showCreate by remember { mutableStateOf(false) }
    var macroName by remember { mutableStateOf("") }
    var macroCommands by remember { mutableStateOf("") }
    val macroHint = stringResource(R.string.map_macro_hint)
    var macroStatus by remember { mutableStateOf(macroHint) }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, MatrixGreen.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.map_macros_title), fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = MatrixGreen)
                IconButton(onClick = { showCreate = true }) {
                    Icon(Icons.Default.Add, stringResource(R.string.map_macro_new_cd), tint = NeonCyan, modifier = Modifier.size(18.dp))
                }
            }
            Text(macroStatus, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextGray)

            macros.forEach { macro ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(macro.name, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = MatrixGreen)
                        Text(
                            macro.commands.lineSequence().firstOrNull()
                                ?: "(macro vacía)",
                            fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextGray
                        )
                    }
                    IconButton(
                        onClick = {
                            scope.launch {
                                macroEngine.execute(macro).fold(
                                    onSuccess = { macroStatus = "Ejecutado: ${macro.name} ($it cmds)" },
                                    onFailure = { macroStatus = "Error: ${it.message}" }
                                )
                            }
                        },
                        enabled = isConnected
                    ) {
                        Icon(Icons.Default.PlayArrow, "Run", tint = NeonOrange, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { scope.launch { macroRepository.delete(macro.id) } }) {
                        Icon(Icons.Default.Delete, "Del", tint = NeonRed, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            containerColor = DarkSurface,
            title = { Text(stringResource(R.string.map_macro_new_title), fontFamily = FontFamily.Monospace, color = MatrixGreen) },
            text = {
                Column {
                    OutlinedTextField(value = macroName, onValueChange = { macroName = it },
                        label = { Text(stringResource(R.string.map_macro_name)) }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = macroCommands, onValueChange = { macroCommands = it },
                        label = { Text(stringResource(R.string.map_macro_commands)) },
                        modifier = Modifier.fillMaxWidth().height(120.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        macroRepository.save(MacroEntity(name = macroName, commands = macroCommands))
                        showCreate = false
                        macroName = ""
                        macroCommands = ""
                    }
                }) { Text(stringResource(R.string.action_save), color = MatrixGreen) }
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false }) { Text(stringResource(R.string.action_cancel), color = TextGray) }
            }
        )
    }
}

@Composable
private fun ConnectionCard(
    connectionState: ConnectionState,
    selectedTransport: TransportType,
    wifiHost: String,
    onTransportChange: (TransportType) -> Unit,
    onWifiHostChange: (String) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onRefreshInfo: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, MatrixGreen.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(stringResource(R.string.map_connection_title), fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = NeonCyan)
            Spacer(modifier = Modifier.height(8.dp))
            val statusText = when (connectionState) {
                ConnectionState.Disconnected -> stringResource(R.string.map_conn_disconnected)
                ConnectionState.Connecting -> stringResource(R.string.map_conn_connecting)
                is ConnectionState.Connected -> stringResource(R.string.map_conn_connected, connectionState.detail)
                is ConnectionState.Error -> stringResource(R.string.map_conn_error, connectionState.message)
            }
            val statusColor = when (connectionState) {
                is ConnectionState.Connected -> MatrixGreen
                is ConnectionState.Error -> NeonRed
                ConnectionState.Connecting -> NeonOrange
                ConnectionState.Disconnected -> TextGray
            }
            Text(statusText, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = statusColor)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(TransportType.USB, TransportType.WIFI, TransportType.BLE).forEach { type ->
                    FilterChip(
                        selected = selectedTransport == type,
                        onClick = { onTransportChange(type) },
                        label = {
                            Text(type.name, fontFamily = FontFamily.Monospace, fontSize = 10.sp,
                                color = if (selectedTransport == type) BlackAMOLED else MatrixGreen)
                        },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MatrixGreen, containerColor = BlackAMOLED)
                    )
                }
            }
            if (selectedTransport == TransportType.BLE) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    stringResource(R.string.transport_ble_experimental),
                    fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = NeonOrange
                )
            }
            if (selectedTransport == TransportType.WIFI) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.transport_wifi_experimental),
                    fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = NeonOrange
                )
                OutlinedTextField(
                    value = wifiHost, onValueChange = onWifiHostChange,
                    label = { Text(stringResource(R.string.map_wifi_host), fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MatrixGreen, unfocusedBorderColor = TextGray, focusedTextColor = MatrixGreen, unfocusedTextColor = MatrixGreen)
                )
            }
            if (selectedTransport == TransportType.USB) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    stringResource(R.string.transport_usb_recommended),
                    fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = MatrixGreen
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onConnect, colors = ButtonDefaults.buttonColors(containerColor = MatrixGreen), modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.map_connect), fontFamily = FontFamily.Monospace, color = BlackAMOLED, fontSize = 11.sp)
                }
                OutlinedButton(onClick = onDisconnect, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.map_disconnect), fontFamily = FontFamily.Monospace, color = NeonRed, fontSize = 11.sp)
                }
            }
            TextButton(onClick = onRefreshInfo) {
                Icon(Icons.Default.Refresh, null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.map_refresh_esp32), fontFamily = FontFamily.Monospace, color = NeonCyan, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun SystemMonitorCard(
    systemInfo: com.embedsuite.app.connection.SystemInfo,
    connectionState: ConnectionState
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(stringResource(R.string.map_monitor_title), fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = NeonCyan)
            InfoLine("UPTIME", systemInfo.uptime.ifBlank { "—" })
            InfoLine("FREE HEAP", systemInfo.freeHeap.ifBlank { "—" })
            InfoLine(stringResource(R.string.map_monitor_battery), systemInfo.battery.ifBlank { "—" })
            InfoLine("FIRMWARE", systemInfo.firmware.ifBlank { "Xibalba" })
            InfoLine(
                stringResource(R.string.map_monitor_status),
                if (connectionState is ConnectionState.Connected) {
                    stringResource(R.string.map_monitor_online)
                } else {
                    stringResource(R.string.map_monitor_offline)
                }
            )
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Text("$label: $value", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TextGray, modifier = Modifier.padding(vertical = 1.dp))
}
