package com.embedsuite.app.ui.screen

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.embedsuite.app.connection.ConnectionState
import com.embedsuite.app.connection.FirmwareProfile
import com.embedsuite.app.connection.SystemInfo
import com.embedsuite.app.core.device.DeviceCapability
import com.embedsuite.app.engine.predictive.ThreatKind
import com.embedsuite.app.engine.risk.DeviceRiskInput
import com.embedsuite.app.engine.risk.RiskScorer
import com.embedsuite.app.ui.components.GlassCard
import com.embedsuite.app.ui.components.HardeningPanel
import com.embedsuite.app.ui.components.HackerSectionHeader
import com.embedsuite.app.ui.components.RadarBlip
import com.embedsuite.app.ui.components.RadarBlipKind
import com.embedsuite.app.ui.components.SpectrumRadar
import com.embedsuite.app.ui.components.toUiState
import com.embedsuite.app.ui.theme.MatrixGreen
import com.embedsuite.app.ui.theme.NeonCyan
import com.embedsuite.app.ui.theme.NeonOrange
import kotlin.math.abs

@Composable
fun RadarHardeningSection(
    systemInfo: SystemInfo,
    connectionState: ConnectionState,
    modifier: Modifier = Modifier
) {
    if (connectionState !is ConnectionState.Connected) return
    if (systemInfo.profile != FirmwareProfile.XIBALBA) return

    val blips = remember(systemInfo.xibalbaCapabilities, systemInfo.hardening) {
        buildRadarBlips(systemInfo)
    }
    val hardeningState = systemInfo.hardening.toUiState()
    val anyIssue = !hardeningState.twdt || !hardeningState.bod || !hardeningState.secureBoot ||
        !hardeningState.nvsIntegrity || !hardeningState.stackCanaries
    val accent = if (anyIssue) NeonOrange else MatrixGreen

    Spacer(modifier = Modifier.height(10.dp))
    HackerSectionHeader("SPECTRUM RADAR", accent = NeonCyan)
    GlassCard(accent = NeonCyan, modifier = modifier.fillMaxWidth().padding(bottom = 10.dp)) {
        SpectrumRadar(blips = blips, accent = MatrixGreen)
    }

    Spacer(modifier = Modifier.height(4.dp))
    HackerSectionHeader("SECURITY POSTURE", accent = accent)
    GlassCard(accent = accent, modifier = modifier.fillMaxWidth().padding(bottom = 10.dp)) {
        HardeningPanel(state = hardeningState, title = "TEH-LINK HARDENING")
    }
}

private fun buildRadarBlips(systemInfo: SystemInfo): List<RadarBlip> {
    val caps = systemInfo.xibalbaCapabilities
    val entries = mutableListOf<RadarBlip>()
    var angle = 0f

    fun addBlip(id: String, label: String, kind: RadarBlipKind, threatKind: ThreatKind, range: Float) {
        val risk = RiskScorer.score(
            DeviceRiskInput(
                id = id,
                label = label,
                kind = threatKind,
                rssi = -60
            )
        )
        entries += RadarBlip(
            id = id,
            label = label,
            angleDeg = angle,
            range01 = range,
            kind = kind,
            riskScore = risk
        )
        angle = (angle + 47f) % 360f
    }

    if (caps["wifi"] == true) {
        addBlip("wifi", "WiFi", RadarBlipKind.WIFI, ThreatKind.WIFI_AP, 0.55f)
    }
    if (caps["ble"] == true || caps["bluetooth"] == true) {
        addBlip("ble", "BLE", RadarBlipKind.BLE, ThreatKind.BLE_DEVICE, 0.42f)
    }
    if (caps["subghz"] == true || caps["cc1101"] == true) {
        addBlip("subghz", "Sub-GHz", RadarBlipKind.SUBGHZ, ThreatKind.SUBGHZ_SIGNAL, 0.68f)
    }
    if (caps["nfc"] == true) {
        addBlip("nfc", "NFC", RadarBlipKind.NFC, ThreatKind.NFC_TAG, 0.35f)
    }

    if (entries.isEmpty()) {
        DeviceCapability.entries.forEachIndexed { index, cap ->
            val kind = when (cap) {
                DeviceCapability.WIFI -> RadarBlipKind.WIFI to ThreatKind.WIFI_AP
                DeviceCapability.BLE -> RadarBlipKind.BLE to ThreatKind.BLE_DEVICE
                DeviceCapability.SUBGHZ_CC1101 -> RadarBlipKind.SUBGHZ to ThreatKind.SUBGHZ_SIGNAL
                DeviceCapability.NFC -> RadarBlipKind.NFC to ThreatKind.NFC_TAG
                else -> null
            } ?: return@forEachIndexed
            addBlip(
                cap.name.lowercase(),
                cap.name,
                kind.first,
                kind.second,
                0.3f + abs(index % 5) * 0.1f
            )
        }
    }

    return entries.take(8)
}
