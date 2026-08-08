package com.embedsuite.app.engine.risk

import com.embedsuite.app.engine.predictive.ThreatKind

data class DeviceRiskInput(
    val id: String,
    val label: String = "",
    val rssi: Int? = null,
    val kind: ThreatKind = ThreatKind.UNKNOWN,
    val isOpenNetwork: Boolean = false,
    val isTracker: Boolean = false
)

object RiskScorer {

    fun score(input: DeviceRiskInput): Int {
        var score = when (input.kind) {
            ThreatKind.WIFI_AP -> 35
            ThreatKind.BLE_DEVICE -> 30
            ThreatKind.SUBGHZ_SIGNAL -> 40
            ThreatKind.NFC_TAG -> 25
            ThreatKind.UNKNOWN -> 15
        }

        input.rssi?.let { rssi ->
            score += when {
                rssi >= -40 -> 25
                rssi >= -55 -> 18
                rssi >= -70 -> 10
                rssi >= -85 -> 5
                else -> 0
            }
        }

        if (input.isOpenNetwork) score += 20
        if (input.isTracker) score += 15

        return score.coerceIn(0, 100)
    }
}
