package com.embedsuite.app

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.embedsuite.app.connection.ConnectionState
import com.embedsuite.app.connection.OtaUpdateStatus
import com.embedsuite.app.connection.TransportType
import com.embedsuite.app.core.AppPreferences
import com.embedsuite.app.data.CapturedSignalEntity
import com.embedsuite.app.data.SessionReportGenerator
import com.embedsuite.app.data.TxHistoryEntity
import com.embedsuite.app.field.FieldOperationManager
import com.embedsuite.app.rf.RfFrequencyPresets
import com.embedsuite.app.rf.RfReplayEngine
import com.embedsuite.app.ui.components.*
import com.embedsuite.app.ui.theme.*
import com.embedsuite.app.ui.viewmodel.DashboardViewModel
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    connectionManager: com.embedsuite.app.connection.DeviceConnectionManager,
    rfReplayEngine: RfReplayEngine,
    sessionReportGenerator: SessionReportGenerator,
    appPreferences: AppPreferences,
    developerMode: Boolean = false,
    onNavigateRf: () -> Unit = {},
    onNavigateTools: () -> Unit = {},
    onNavigateFirmwareFlash: () -> Unit = {},
    onNavigateCompanion: (String) -> Unit = {},
    onNavigateBruceFiles: () -> Unit = {},
    onNavigateBleWizard: () -> Unit = {},
    onNavigateWifiWizard: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val detectedProfile by connectionManager.detectedProfile.collectAsState()
    val transportAvailability by connectionManager.transportAvailability.collectAsState()
    val activeTransport by connectionManager.activeTransportType.collectAsState()
    val bruceLinkReady by connectionManager.bruceLinkReady.collectAsState()
    val subGhzFreq by connectionManager.subGhzFrequencyMhz.collectAsState()
    val fieldActive by FieldOperationManager.isActiveFlow.collectAsState()
    var replayTarget by remember { mutableStateOf<CapturedSignalEntity?>(null) }
    var keepScreenOn by remember { mutableStateOf(appPreferences.fieldKeepScreenOn) }
    var fieldSessionName by remember { mutableStateOf("Campo") }
    var lastFieldReportPath by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { viewModel.refreshStats() }
    LaunchedEffect(fieldActive) {
        if (!fieldActive) {
            repeat(12) {
                kotlinx.coroutines.delay(250)
                FieldOperationManager.lastReportFile?.absolutePath?.let { path ->
                    lastFieldReportPath = path
                    viewModel.refreshStats()
                    return@LaunchedEffect
                }
            }
            viewModel.refreshStats()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
    ) {
        HackerSectionHeader("COMPANION", accent = MatrixGreen)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            NeonButton(text = "Capturar RF", onClick = { onNavigateCompanion("subghz_analyzer") }, modifier = Modifier.weight(1f))
            NeonOutlinedButton(text = "Buscar IR", onClick = { onNavigateCompanion("ir_search") }, modifier = Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            NeonOutlinedButton(text = "BadUSB", onClick = { onNavigateCompanion("badusb_forge") }, modifier = Modifier.weight(1f))
            NeonOutlinedButton(text = "Wizard WiFi", onClick = onNavigateWifiWizard, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(10.dp))

        HackerSectionHeader(stringResource(R.string.dash_command_center), accent = MatrixGreen)

        TransportIndicatorRow(
            availability = transportAvailability,
            onSelect = { type ->
                scope.launch { connectionManager.connect(type) }
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
        )

        when (val ota = uiState.otaStatus) {
            is OtaUpdateStatus.UpdateAvailable -> {
                GlassCard(accent = NeonOrange, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                    Text(stringResource(R.string.dash_ota_title_bruce), fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NeonOrange)
                    Text(stringResource(R.string.dash_ota_body, ota.deviceVersion, ota.latestVersion), fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextGray)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onNavigateFirmwareFlash) {
                            Text(stringResource(R.string.dash_firmware_flash), fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = NeonOrange)
                        }
                        TextButton(onClick = onNavigateTools) {
                            Text(stringResource(R.string.dash_ota_action), fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MatrixGreen)
                        }
                    }
                }
            }
            else -> {}
        }

        GlassCard(accent = NeonOrange, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
            Text(stringResource(R.string.firmware_flash_title), fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NeonOrange)
            Text(
                stringResource(R.string.dash_firmware_flash_sub),
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = TextGray,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            NeonButton(
                text = stringResource(R.string.dash_firmware_flash),
                onClick = onNavigateFirmwareFlash,
                color = NeonOrange,
                modifier = Modifier.fillMaxWidth()
            )
        }

        val simActive = uiState.systemInfo.simFlags.any { it.value }
        if (simActive && uiState.connectionState is ConnectionState.Connected) {
            val simList = uiState.systemInfo.simFlags.filter { it.value }.keys.sorted().joinToString(", ")
            GlassCard(accent = NeonOrange, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                Text(
                    stringResource(R.string.plus_compat_sim_warning, simList),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = NeonOrange,
                    lineHeight = 12.sp
                )
            }
        }

        GlassCard(accent = connectionColor(uiState.connectionState), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
            Text(stringResource(R.string.dash_link), fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NeonCyan)
            Text(connectionLabel(uiState.connectionState), fontFamily = FontFamily.Monospace, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = connectionColor(uiState.connectionState))
            if (uiState.connectionState is ConnectionState.Connected) {
                val c = uiState.connectionState as ConnectionState.Connected
                Text(stringResource(R.string.conn_transport, c.type.name), fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TextGray)
                Text(c.detail, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextMuted)
            }
            if (uiState.connectionState is ConnectionState.Error) {
                Text(stringResource(R.string.conn_check_usb), fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = NeonOrange)
            }
            uiState.tehLinkNotice?.let { notice ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    notice,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = NeonOrange,
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(onClick = { viewModel.clearTehLinkNotice() }) {
                    Text(stringResource(R.string.action_ok), fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = MatrixGreen)
                }
            }
        }

        GlassCard(accent = KaliBlue, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
            Text(stringResource(R.string.dash_system), fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = KaliBlue)
            if (uiState.connectionState is ConnectionState.Connected) {
                val na = stringResource(R.string.dash_teh_link_na)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DeviceTelemetryChip(
                        label = stringResource(R.string.dash_battery),
                        value = uiState.systemInfo.battery.ifBlank { na },
                        color = when {
                            activeTransport == TransportType.BLE && bruceLinkReady -> MatrixGreen
                            bruceLinkReady -> NeonCyan
                            else -> TextMuted
                        }
                    )
                    DeviceTelemetryChip(
                        label = stringResource(R.string.dash_sd_free),
                        value = uiState.systemInfo.sdFreeSpace.ifBlank { na },
                        color = if (bruceLinkReady && uiState.systemInfo.sdFreeSpace.isNotBlank()) NeonCyan else TextMuted
                    )
                    DeviceTelemetryChip(
                        label = stringResource(R.string.dash_temperature),
                        value = uiState.systemInfo.temperatureC.ifBlank { na },
                        color = if (bruceLinkReady) NeonCyan else TextMuted
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DeviceTelemetryChip(
                        label = stringResource(R.string.dash_transport),
                        value = activeTransport.name,
                        color = NeonOrange
                    )
                }
                if (activeTransport == TransportType.BLE && bruceLinkReady) {
                    Text(
                        "Telemetría BLE activa (GATT Battery + CLI)",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = MatrixGreen,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                if (!bruceLinkReady) {
                    Text(
                        stringResource(R.string.dash_teh_link_hint),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = NeonOrange,
                        lineHeight = 12.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                HorizontalDivider(
                    color = KaliBlue.copy(alpha = 0.25f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            StatRow(stringResource(R.string.dash_uptime), uiState.systemInfo.uptime.ifBlank { if (bruceLinkReady) "—" else stringResource(R.string.dash_teh_link_na) })
            StatRow(stringResource(R.string.dash_heap), uiState.systemInfo.freeHeap.ifBlank { if (bruceLinkReady) "—" else stringResource(R.string.dash_teh_link_na) })
            if (uiState.connectionState !is ConnectionState.Connected) {
                StatRow(stringResource(R.string.dash_battery), uiState.systemInfo.battery.ifBlank { "—" })
            }
            StatRow(
                stringResource(R.string.dash_firmware),
                uiState.systemInfo.firmware.ifBlank {
                    if (bruceLinkReady) "—" else stringResource(R.string.dash_firmware_no_tehlink)
                }
            )
            if (uiState.systemInfo.uiScreen.isNotBlank()) {
                StatRow("UI", uiState.systemInfo.uiScreen)
            }
            if (uiState.systemInfo.sdMounted.isNotBlank()) {
                val sdValue = buildString {
                    append(uiState.systemInfo.sdMounted)
                    if (uiState.systemInfo.sdFreeSpace.isNotBlank()) {
                        append(" · ")
                        append(uiState.systemInfo.sdFreeSpace)
                    }
                }
                StatRow("SD", sdValue)
            }
            TextButton(onClick = { viewModel.refreshSystemInfo() }) {
                Text(stringResource(R.string.action_refresh), fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MatrixGreen)
            }
        }

        if (uiState.connectionState is ConnectionState.Connected && bruceLinkReady) {
            Spacer(modifier = Modifier.height(10.dp))
            HackerSectionHeader("BRUCE CLI", accent = NeonCyan)
            GlassCard(accent = NeonCyan, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                Text(
                    "Control remoto vía CLI serial (BLE / WiFi WebUI / USB). Evil Portal, BLE Spam y Wardriving solo en el menú del T-Embed.",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = TextMuted,
                    lineHeight = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    NeonButton(
                        text = stringResource(R.string.dashboard_action_capture_15s),
                        onClick = { viewModel.runSubGhzCapture(15) },
                        modifier = Modifier.weight(1f)
                    )
                    NeonOutlinedButton(
                        text = stringResource(R.string.dashboard_menu),
                        onClick = { viewModel.backToBruceMenu() },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Presets CC1101", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextMuted)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RfFrequencyPresets.PRESETS.forEach { mhz ->
                        val selected = subGhzFreq == mhz
                        NeonOutlinedButton(
                            text = mhz.take(5),
                            onClick = { scope.launch { connectionManager.setSubGhzFrequency(mhz) } },
                            modifier = Modifier.weight(1f),
                            color = if (selected) MatrixGreen else TextMuted
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BruceNavPad(
                        enabled = true,
                        compact = true,
                        onNav = { cmd -> scope.launch { connectionManager.sendNavCommand(cmd) } }
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        if (developerMode) {
                            TextButton(onClick = onNavigateBruceFiles) {
                                Text("Archivos SD", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = NeonCyan)
                            }
                        }
                        TextButton(onClick = onNavigateBleWizard) {
                            Text("Wizard BLE", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = EmbedGreen)
                        }
                        TextButton(onClick = onNavigateWifiWizard) {
                            Text("Wizard WiFi", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = NeonCyan)
                        }
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatBadge(stringResource(R.string.dash_signals_today), uiState.signalsToday.toString(), MatrixGreen, Modifier.weight(1f))
            StatBadge(stringResource(R.string.dash_aps_today), uiState.apsToday.toString(), NeonCyan, Modifier.weight(1f))
            StatBadge(stringResource(R.string.dash_macros_today), uiState.macrosToday.toString(), NeonOrange, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(10.dp))

        HackerSectionHeader(stringResource(R.string.dash_quick_actions), accent = NeonCyan)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            NeonButton(
                text = stringResource(R.string.dash_rx_15s),
                onClick = {
                    scope.launch {
                        connectionManager.startSubGhzRawCapture(15)
                        viewModel.refreshStats()
                    }
                },
                enabled = uiState.connectionState is ConnectionState.Connected,
                modifier = Modifier.weight(1f)
            )
            NeonOutlinedButton(
                text = stringResource(R.string.dash_tx_last),
                onClick = {
                    uiState.lastSignal?.let { replayTarget = it }
                },
                enabled = uiState.connectionState is ConnectionState.Connected && uiState.lastSignal != null,
                modifier = Modifier.weight(1f)
            )
            NeonOutlinedButton(
                text = stringResource(R.string.dash_info),
                onClick = { viewModel.refreshSystemInfo() },
                enabled = uiState.connectionState is ConnectionState.Connected,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        GlassCard(accent = NeonOrange, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
            Text(stringResource(R.string.dash_last_signal), fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NeonOrange)
            val s = uiState.lastSignal
            if (s != null) {
                Text(s.label.ifBlank { s.protocol }, fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = MatrixGreen, fontWeight = FontWeight.Bold)
                Text("${s.frequency} // ${s.protocol} // ${s.deviceId}", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextGray)
                if (s.decodedFields.isNotBlank()) {
                    Text(
                        s.decodedFields.lineSequence().firstOrNull() ?: s.decodedFields.take(40),
                        fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = NeonCyan
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    NeonButton(
                        text = stringResource(R.string.dash_retransmit),
                        onClick = { replayTarget = s },
                        enabled = uiState.connectionState is ConnectionState.Connected,
                        modifier = Modifier.weight(1f)
                    )
                    NeonOutlinedButton(text = "Analizar RF", onClick = onNavigateRf, modifier = Modifier.weight(1f))
                }
            } else {
                Text(stringResource(R.string.dash_no_signals), fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TextGray)
            }
        }

        if (uiState.txHistory.isNotEmpty()) {
            GlassCard(accent = NeonCyan, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                Text(stringResource(R.string.dash_last_tx), fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NeonCyan)
                uiState.txHistory.forEach { tx -> TxHistoryRow(tx) }
            }
        }

        if (uiState.favoriteRf.isNotEmpty()) {
            GlassCard(accent = NeonOrange, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                Text(stringResource(R.string.dash_favorites), fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NeonOrange)
                uiState.favoriteRf.take(5).forEach { fav ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "★ ${fav.label.ifBlank { fav.protocol }}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = MatrixGreen,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { replayTarget = fav },
                            enabled = uiState.connectionState is ConnectionState.Connected
                        ) {
                            Text("TX", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = NeonOrange)
                        }
                    }
                }
            }
        }

        GlassCard(accent = if (fieldActive) NeonRed else MatrixGreen, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
            Text(stringResource(R.string.dash_field_mode), fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = if (fieldActive) NeonRed else MatrixGreen)
            Text(
                if (fieldActive) stringResource(R.string.dash_field_active, appPreferences.fieldFrequencyMhz)
                else stringResource(R.string.dash_field_idle),
                fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextGray
            )
            if (!fieldActive) {
                OutlinedTextField(
                    value = fieldSessionName,
                    onValueChange = { fieldSessionName = it.take(40) },
                    label = { Text(stringResource(R.string.dash_field_session_name), fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MatrixGreen,
                        focusedTextColor = MatrixGreen,
                        unfocusedTextColor = MatrixGreen
                    )
                )
            } else {
                Text(
                    stringResource(R.string.dash_field_session_running, FieldOperationManager.sessionName),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = NeonCyan
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.dash_keep_screen), fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextMuted)
                Switch(checked = keepScreenOn, onCheckedChange = { keepScreenOn = it; appPreferences.fieldKeepScreenOn = it })
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (fieldActive) {
                NeonButton(text = stringResource(R.string.dash_stop_field), onClick = {
                    FieldOperationManager.stop(context)
                    viewModel.refreshStats()
                }, color = NeonRed, modifier = Modifier.fillMaxWidth())
            } else {
                NeonButton(text = stringResource(R.string.dash_start_field), onClick = {
                    FieldOperationManager.start(context, keepScreenOn, fieldSessionName)
                }, enabled = uiState.connectionState is ConnectionState.Connected, modifier = Modifier.fillMaxWidth())
            }
            lastFieldReportPath?.let { path ->
                Spacer(modifier = Modifier.height(6.dp))
                NeonOutlinedButton(
                    text = stringResource(R.string.dash_share_field_report),
                    onClick = {
                        val file = java.io.File(path)
                        if (!file.exists()) {
                            Toast.makeText(context, R.string.dash_field_report_missing, Toast.LENGTH_SHORT).show()
                            return@NeonOutlinedButton
                        }
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        context.startActivity(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "text/html"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                },
                                context.getString(R.string.dash_share_report)
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        replayTarget?.let { signal ->
            val preview = remember(signal) { rfReplayEngine.preview(signal) }
            AlertDialog(
                onDismissRequest = { replayTarget = null },
                containerColor = DarkSurface,
                title = { Text(stringResource(R.string.dash_preview_tx), fontFamily = FontFamily.Monospace, color = NeonOrange) },
                text = {
                    Column {
                        Text("${preview.protocol} @ ${preview.frequency}", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = MatrixGreen)
                        Text(preview.command, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = NeonCyan)
                        Text(preview.summary.take(220), fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextGray)
                        if (!preview.canTransmit) {
                            Text(
                                preview.blockerMessage,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = NeonOrange,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = preview.canTransmit,
                        onClick = {
                            scope.launch {
                                rfReplayEngine.replay(signal).fold(
                                    onSuccess = {
                                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                                        viewModel.refreshStats()
                                    },
                                    onFailure = {
                                        Toast.makeText(
                                            context,
                                            it.message ?: context.getString(R.string.widget_tx_fail),
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                )
                                replayTarget = null
                            }
                        }
                    ) {
                        Text(
                            if (preview.canTransmit) stringResource(R.string.action_transmit)
                            else stringResource(R.string.action_unavailable),
                            color = if (preview.canTransmit) MatrixGreen else TextGray
                        )
                    }
                },
                dismissButton = { TextButton(onClick = { replayTarget = null }) { Text(stringResource(R.string.action_cancel), color = TextGray) } }
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
            NeonOutlinedButton(text = stringResource(R.string.dash_report_html), onClick = {
                scope.launch {
                    sessionReportGenerator.generateHtmlReport().fold(
                        onSuccess = { f ->
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f)
                            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                type = "text/html"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }, context.getString(R.string.dash_share_report)))
                        },
                        onFailure = { Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show() }
                    )
                }
            }, modifier = Modifier.weight(1f))
            NeonOutlinedButton(text = stringResource(R.string.dash_report_pdf), onClick = {
                scope.launch {
                    sessionReportGenerator.generatePdfReport().fold(
                        onSuccess = { f ->
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f)
                            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }, context.getString(R.string.dash_share_pdf)))
                        },
                        onFailure = { Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show() }
                    )
                }
            }, modifier = Modifier.weight(1f))
        }

        if (developerMode) {
            NeonOutlinedButton(text = stringResource(R.string.dash_tools_export), onClick = onNavigateTools, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun DeviceTelemetryChip(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = TextMuted)
        Text(value, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TextMuted)
        Text(value, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MatrixGreen)
    }
}

@Composable
private fun HardeningRow(label: String, enabled: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontFamily = FontFamily.Monospace, fontSize = 9.sp,
            color = if (enabled) TextGray else NeonOrange
        )
        Text(
            if (enabled) "✅ OK" else "⚠️ OFF",
            fontFamily = FontFamily.Monospace, fontSize = 9.sp,
            color = if (enabled) MatrixGreen else NeonOrange,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StatBadge(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    GlassCard(accent = color, modifier = modifier, cornerRadius = 8.dp) {
        Text(label, fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = TextMuted)
        Text(value, fontFamily = FontFamily.Monospace, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun TxHistoryRow(tx: TxHistoryEntity) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("${if (tx.success) "✓" else "✗"} ${tx.label}", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = if (tx.success) MatrixGreen else NeonRed)
        Text(tx.protocol, fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = TextMuted)
    }
}

@Composable
private fun connectionLabel(state: ConnectionState): String = when (state) {
    ConnectionState.Disconnected -> stringResource(R.string.conn_disconnected)
    ConnectionState.Connecting -> stringResource(R.string.conn_connecting)
    is ConnectionState.Connected -> stringResource(R.string.conn_connected)
    is ConnectionState.Error -> stringResource(R.string.conn_error)
}

private fun connectionColor(state: ConnectionState) = when (state) {
    is ConnectionState.Connected -> MatrixGreen
    ConnectionState.Connecting -> NeonOrange
    is ConnectionState.Error -> NeonRed
    ConnectionState.Disconnected -> TextGray
}
