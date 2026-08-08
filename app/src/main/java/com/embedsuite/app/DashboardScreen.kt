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
import org.json.JSONObject
import com.embedsuite.app.connection.ConnectionState
import com.embedsuite.app.connection.OtaUpdateStatus
import com.embedsuite.app.core.AppPreferences
import com.embedsuite.app.data.CapturedSignalEntity
import com.embedsuite.app.data.SessionReportGenerator
import com.embedsuite.app.data.TxHistoryEntity
import com.embedsuite.app.field.FieldOperationManager
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
    onNavigateRf: () -> Unit = {},
    onNavigateTools: () -> Unit = {},
    onNavigateHardwareBringup: () -> Unit = {},
    onNavigateProbeSniffer: () -> Unit = {},
    onNavigateSpectrum: () -> Unit = {},
    onNavigateNfcClone: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val detectedProfile by connectionManager.detectedProfile.collectAsState()
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
        HackerSectionHeader(stringResource(R.string.dash_command_center), accent = MatrixGreen)

        if (detectedProfile == com.embedsuite.app.connection.FirmwareProfile.XIBALBA) {
            TextButton(onClick = onNavigateHardwareBringup, modifier = Modifier.padding(bottom = 4.dp)) {
                Text(
                    stringResource(R.string.dash_hardware_bringup),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = NeonCyan
                )
            }
        }

        when (val ota = uiState.otaStatus) {
            is OtaUpdateStatus.UpdateAvailable -> {
                GlassCard(accent = NeonOrange, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                    Text(stringResource(R.string.dash_ota_title_xibalba), fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NeonOrange)
                    Text(stringResource(R.string.dash_ota_body, ota.deviceVersion, ota.latestVersion), fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextGray)
                    TextButton(onClick = onNavigateTools) {
                        Text(stringResource(R.string.dash_ota_action), fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MatrixGreen)
                    }
                }
            }
            else -> {}
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
            StatRow(stringResource(R.string.dash_uptime), uiState.systemInfo.uptime.ifBlank { "—" })
            StatRow(stringResource(R.string.dash_heap), uiState.systemInfo.freeHeap.ifBlank { "—" })
            StatRow(stringResource(R.string.dash_battery), uiState.systemInfo.battery.ifBlank { "—" })
            StatRow(stringResource(R.string.dash_firmware), uiState.systemInfo.firmware.ifBlank { "—" })
            if (uiState.systemInfo.uiScreen.isNotBlank()) {
                StatRow("UI", uiState.systemInfo.uiScreen)
            }
            if (uiState.systemInfo.sdMounted.isNotBlank()) {
                StatRow("SD", uiState.systemInfo.sdMounted)
            }
            TextButton(onClick = { viewModel.refreshSystemInfo() }) {
                Text(stringResource(R.string.action_refresh), fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MatrixGreen)
            }
        }

        /* ====== XIBALBA HARDENING STATUS CARD ====== */
        if (
            uiState.systemInfo.profile == com.embedsuite.app.connection.FirmwareProfile.XIBALBA &&
            (uiState.systemInfo.hardening.twdtEnabled ||
                uiState.systemInfo.hardening.bodEnabled ||
                uiState.systemInfo.hardening.secureBoot ||
                uiState.systemInfo.hardening.stackCanaries ||
                uiState.connectionState is ConnectionState.Connected)
        ) {
            val h = uiState.systemInfo.hardening
            val anySecurityIssue = (!h.twdtEnabled || !h.bodEnabled || !h.secureBoot ||
                !h.nvsEncryption || !h.stackCanaries || uiState.systemInfo.coredumpPending)
            val accent = if (anySecurityIssue) NeonOrange else MatrixGreen

            GlassCard(accent = accent, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                Text(
                    "🛡 XIBALBA HARDENING 0.19.0+",
                    fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = accent, fontWeight = FontWeight.Bold
                )
                HardeningRow("Task Watchdog (TWDT ${h.twdtTimeoutSeconds}s)", h.twdtEnabled)
                HardeningRow("Brownout detector 3.0V", h.bodEnabled)
                HardeningRow("Secure Boot V2 (firmware signed)", h.secureBoot)
                HardeningRow("Flash Encryption XTS-256", h.flashEncryption)
                HardeningRow("NVS Encryption (WiFi/tokens)", h.nvsEncryption)
                HardeningRow("Stack Canaries + Heap Poisoning", h.stackCanaries || h.heapPoisoning)

                if (uiState.systemInfo.coredumpPending) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().background(NeonRed.copy(alpha = 0.12f)).padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "⚠️ COREDUMP EN FLASH: reinicio anormal pendiente (TWDT / BOD).",
                            fontFamily = FontFamily.Monospace, fontSize = 9.sp,
                            color = NeonRed, modifier = Modifier.weight(1f)
                        )
                    }
                    TextButton(
                        onClick = { scope.launch { runCatching { viewModel.clearCoredump() }.onFailure { Toast.makeText(context, "Clear: ${it.message}", Toast.LENGTH_LONG).show() } } }
                    ) {
                        Text("Borrar coredump", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = MatrixGreen)
                    }
                }
                uiState.systemInfo.wdtPanicReason?.let { reason ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Último reset: $reason",
                        fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = NeonRed
                    )
                }
                if (uiState.systemInfo.lastOta.sha256Verified) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "✅ Última OTA: SHA256 VERIFIED (${uiState.systemInfo.lastOta.totalSize} B)",
                        fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = MatrixGreen
                    )
                } else if (uiState.systemInfo.lastOta.totalSize > 0L && !uiState.systemInfo.lastOta.sha256Verified) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "❌ Última OTA: sha256 NOT VERIFIED (NO reinicies)",
                        fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = NeonRed
                    )
                }
            }
        }

        if (
            uiState.connectionState is ConnectionState.Connected &&
            uiState.systemInfo.profile == com.embedsuite.app.connection.FirmwareProfile.XIBALBA &&
            uiState.systemInfo.xibalbaPlugins.isNotEmpty()
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            HackerSectionHeader("XIBALBA PLUGINS", accent = MatrixGreen)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                uiState.systemInfo.xibalbaPlugins.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        row.forEach { plugin ->
                            NeonOutlinedButton(
                                text = plugin.name.ifBlank { plugin.id },
                                onClick = { viewModel.openXibalbaPlugin(plugin.id) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                NeonButton(
                    text = stringResource(R.string.dashboard_menu),
                    onClick = { viewModel.backToXibalbaMenu() },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HackerSectionHeader("ACTIONS", accent = NeonCyan)
            val isXibalba = detectedProfile == com.embedsuite.app.connection.FirmwareProfile.XIBALBA
            GlassCard(accent = NeonCyan, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    NeonButton(
                        text = stringResource(R.string.dashboard_action_capture_15s),
                        onClick = { viewModel.runSubGhzCapture(15) },
                        modifier = Modifier.weight(1f)
                    )
                    if (!isXibalba) {
                        NeonOutlinedButton(
                            text = stringResource(R.string.dashboard_action_demo_script),
                            onClick = { viewModel.runBadUsbDemoScript() },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    NeonButton(
                        text = stringResource(R.string.dashboard_action_wifi_scan),
                        onClick = { viewModel.runWifiScan(10) },
                        modifier = Modifier.weight(1f)
                    )
                    NeonButton(
                        text = stringResource(R.string.dashboard_action_ble_scan),
                        onClick = { viewModel.runBleScan(10) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val wardrivingActive = uiState.lastActionState?.let {
                        it.pluginId == "wardriving" && (it.running || it.wardriving?.running == true)
                    } == true
                    NeonButton(
                        text = stringResource(R.string.dashboard_action_wardriving_start),
                        onClick = { viewModel.runWardrivingStart() },
                        enabled = !wardrivingActive,
                        modifier = Modifier.weight(1f)
                    )
                    NeonOutlinedButton(
                        text = stringResource(R.string.dashboard_action_wardriving_stop),
                        onClick = { viewModel.runWardrivingStop() },
                        enabled = wardrivingActive,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (isXibalba) {
                    Text(
                        stringResource(R.string.plus_compat_wardriving_xibalba_hint),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    NeonButton(
                        text = stringResource(R.string.dashboard_action_hash_test),
                        onClick = { viewModel.runCryptoHashTest() },
                        modifier = Modifier.weight(1f)
                    )
                    NeonOutlinedButton(
                        text = stringResource(R.string.dashboard_action_gen_password),
                        onClick = { viewModel.runGenPassword() },
                        modifier = Modifier.weight(1f)
                    )
                }
                val actionState = uiState.lastActionState
                if (actionState != null) {
                    val label = actionState.pluginId.ifBlank { "action" }
                    val value = buildString {
                        if (actionState.state.isNotBlank()) append(actionState.state)
                        if (actionState.running || actionState.capturing) {
                            if (isNotEmpty()) append(" · ")
                            if (actionState.progress > 0) append("${actionState.progress}%")
                            if (actionState.secondsRemaining > 0) {
                                if (actionState.progress > 0) append(", ")
                                append(context.getString(R.string.dashboard_action_seconds_left, actionState.secondsRemaining))
                            }
                        }
                        if (actionState.packets > 0) {
                            if (isNotEmpty()) append(" · ")
                            append(context.getString(R.string.dashboard_action_pkts, actionState.packets))
                        }
                        actionState.wardriving?.let { wd ->
                            if (isNotEmpty()) append(" · ")
                            append(context.getString(R.string.dashboard_action_aps, wd.apCount))
                            if (wd.csvPath.isNotBlank()) append(" → ${wd.csvPath.substringAfterLast('/')}")
                            else if (wd.csvBasename.isNotBlank()) append(" → ${wd.csvBasename}")
                        }
                        if (actionState.aps.isNotEmpty()) {
                            if (isNotEmpty()) append(" · ")
                            append("${actionState.aps.size} APs")
                        }
                        if (actionState.devices.isNotEmpty()) {
                            if (isNotEmpty()) append(" · ")
                            append("${actionState.devices.size} BLE")
                        }
                        actionState.crypto?.let { crypto ->
                            val isSecret = actionState.action in setOf("gen_password", "gen_passphrase")
                            if (!isSecret) {
                                val cryptoOut = crypto.digest.ifBlank { crypto.result }
                                if (cryptoOut.isNotBlank()) {
                                    if (isNotEmpty()) append(" · ")
                                    if (crypto.algo.isNotBlank()) append("${crypto.algo}: ")
                                    append(cryptoOut.take(48))
                                    if (cryptoOut.length > 48) append("…")
                                }
                            } else if (crypto.result.isNotBlank() || crypto.digest.isNotBlank()) {
                                if (isNotEmpty()) append(" · ")
                                append(context.getString(R.string.dashboard_secret_hidden))
                            }
                        }
                        if (actionState.message.isNotBlank()) {
                            if (isNotEmpty()) append(" — ")
                            append(actionState.message)
                        }
                        if (isEmpty()) append("—")
                    }
                    StatRow(label, value)
                } else {
                    StatRow("action", "—")
                }
                TextButton(onClick = {
                    val pluginId = uiState.lastActionState?.pluginId?.takeIf { it.isNotBlank() }
                        ?: "subghz_analyzer"
                    viewModel.refreshActionState(pluginId)
                }) {
                    Text(stringResource(R.string.action_refresh_state), fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MatrixGreen)
                }
            }
        }

        if (
            uiState.connectionState is ConnectionState.Connected &&
            uiState.systemInfo.profile == com.embedsuite.app.connection.FirmwareProfile.XIBALBA
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            HackerSectionHeader("OFENSIVE TOOLS · AUDIT MODE", accent = NeonRed)

            GlassCard(accent = NeonPurple, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bluetooth, null, tint = NeonPurple)
                    Spacer(Modifier.width(8.dp))
                    Text("BLE AD SPAM (AppleJuice / SwiftPair / FindMy / HomeKit)",
                        fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                        color = NeonPurple, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()) {
                    NeonButton(text = "AppleJuice", onClick = {
                        scope.launch {
                            val r = connectionManager.tehLinkRunAction("ble_ad_spam", "start",
                                JSONObject().put("campaign", "applejuice").put("hz", 10))
                            Toast.makeText(context, r.fold({ "Start AppleJuice" }) { it.message ?: "fail" },
                                Toast.LENGTH_SHORT).show()
                        }
                    }, modifier = Modifier.weight(1f),
                        color = NeonPurple.copy(alpha = 0.9f))
                    NeonButton(text = "SwiftPair", onClick = {
                        scope.launch {
                            connectionManager.tehLinkRunAction("ble_ad_spam", "start",
                                JSONObject().put("campaign", "swiftpair").put("hz", 10))
                        }
                    }, modifier = Modifier.weight(1f),
                        color = NeonCyan.copy(alpha = 0.9f))
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()) {
                    NeonButton(text = "FindMy", onClick = {
                        scope.launch {
                            connectionManager.tehLinkRunAction("ble_ad_spam", "start",
                                JSONObject().put("campaign", "findmy").put("hz", 5))
                        }
                    }, modifier = Modifier.weight(1f),
                        color = MatrixGreen.copy(alpha = 0.9f))
                    NeonButton(text = "HomeKit", onClick = {
                        scope.launch {
                            connectionManager.tehLinkRunAction("ble_ad_spam", "start",
                                JSONObject().put("campaign", "homekit").put("hz", 5))
                        }
                    }, modifier = Modifier.weight(1f),
                        color = NeonOrange.copy(alpha = 0.9f))
                    NeonOutlinedButton(text = "STOP", onClick = {
                        scope.launch {
                            connectionManager.tehLinkRunAction("ble_ad_spam", "stop", JSONObject())
                        }
                    }, modifier = Modifier.weight(1f))
                }
            }

            GlassCard(accent = NeonRed, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WifiOff, null, tint = NeonRed)
                    Spacer(Modifier.width(8.dp))
                    Text("WiFi OFFENSIVE (Deauth Broadcast + Probe Sniffer)",
                        fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                        color = NeonRed, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()) {
                    NeonOutlinedButton(text = "Deauth BC 30s", onClick = {
                        scope.launch {
                            val r = connectionManager.tehLinkRunAction("wifi_offensive", "deauth_start",
                                JSONObject()
                                    .put("bssid", "FF:FF:FF:FF:FF:FF")
                                    .put("sta", "FF:FF:FF:FF:FF:FF")
                                    .put("channel", 1)
                                    .put("pps", 10))
                            Toast.makeText(context, r.fold({ "Deauth BC Ch1" }) { it.message ?: "fail" },
                                Toast.LENGTH_SHORT).show()
                        }
                    }, color = NeonRed,
                        modifier = Modifier.weight(1f))
                    NeonOutlinedButton(text = "Deauth STOP", onClick = {
                        scope.launch {
                            connectionManager.tehLinkRunAction("wifi_offensive", "deauth_stop", JSONObject())
                        }
                    }, color = NeonOrange, modifier = Modifier.weight(1f))
                    NeonButton(text = "Probe Sniffer", onClick = onNavigateProbeSniffer,
                        modifier = Modifier.weight(1.2f),
                        color = MatrixGreen.copy(alpha = 0.9f))
                }
            }

            GlassCard(accent = NeonOrange, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Mouse, null, tint = NeonOrange)
                    Spacer(Modifier.width(8.dp))
                    Text("MOUSEJACK NRF24 (Logitech / MS dongles 2.4G)",
                        fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                        color = NeonOrange, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()) {
                    NeonButton(text = "Scan 5s", onClick = {
                        scope.launch {
                            val r = connectionManager.tehLinkRunAction("mousejack", "scan",
                                JSONObject().put("ms", 5000))
                            Toast.makeText(context, r.fold({ data ->
                                val d = data.rawResponse?.getInt("dongles_found") ?: 0
                                "Scanned $d dongles"
                            }) { it.message ?: "fail" }, Toast.LENGTH_SHORT).show()
                        }
                    }, modifier = Modifier.weight(1f),
                        color = NeonOrange.copy(alpha = 0.95f))
                    NeonOutlinedButton(text = "Inject GUI+r", onClick = {
                        scope.launch {
                            connectionManager.tehLinkRunAction("mousejack", "play_ducky",
                                JSONObject().put("script", "GUI r\nDELAY 300\nSTRING notepad\nENTER\nDELAY 500\nSTRING Hello Mousejack\n"))
                        }
                    }, color = NeonRed,
                        modifier = Modifier.weight(1f))
                    NeonOutlinedButton(text = "Map Tools", onClick = onNavigateTools,
                        modifier = Modifier.weight(1f))
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                GlassCard(accent = KaliBlue, modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SettingsInputAntenna, null, tint = KaliBlue)
                        Spacer(Modifier.width(6.dp))
                        Text("SubGHz", fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp, color = KaliBlue, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Spectrum heatmap + AutoDecoder 433/868/915\n(Keeloq · Somfy · Nice · PT2262)",
                        fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextGray,
                        lineHeight = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    NeonButton(text = "Abrir Spectrum", onClick = onNavigateSpectrum,
                        modifier = Modifier.fillMaxWidth(),
                        color = KaliBlue.copy(alpha = 0.9f))
                }
                GlassCard(accent = MatrixGreen, modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Nfc, null, tint = MatrixGreen)
                        Spacer(Modifier.width(6.dp))
                        Text("NFC Clone", fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp, color = MatrixGreen, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Mifare 1K read/write + NTAG URL / WiFi WSC QR (WPA2/WPA3)",
                        fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextGray,
                        lineHeight = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    NeonButton(text = "Abrir NFC Clone", onClick = onNavigateNfcClone,
                        modifier = Modifier.fillMaxWidth(),
                        color = MatrixGreen.copy(alpha = 0.9f))
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
                    NeonOutlinedButton(text = stringResource(R.string.dash_view_rf), onClick = onNavigateRf, modifier = Modifier.weight(1f))
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

        NeonOutlinedButton(text = stringResource(R.string.dash_tools_export), onClick = onNavigateTools, modifier = Modifier.fillMaxWidth())
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
