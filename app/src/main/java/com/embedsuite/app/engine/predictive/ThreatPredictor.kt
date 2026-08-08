package com.embedsuite.app.engine.predictive

enum class ThreatKind {
    WIFI_AP,
    BLE_DEVICE,
    SUBGHZ_SIGNAL,
    NFC_TAG,
    UNKNOWN
}

data class ThreatGuess(
    val kind: ThreatKind,
    val confidence: Float,
    val rationale: String
)

/**
 * Placeholder decision-tree predictor — heuristic stub for v1.0.0 foundation.
 */
class ThreatPredictor {

    fun guess(
        signalType: String,
        rssi: Int? = null,
        label: String = ""
    ): ThreatGuess {
        val type = signalType.lowercase()
        val kind = when {
            type.contains("wifi") || type.contains("ap") -> ThreatKind.WIFI_AP
            type.contains("ble") || type.contains("bluetooth") -> ThreatKind.BLE_DEVICE
            type.contains("subghz") || type.contains("433") || type.contains("868") -> ThreatKind.SUBGHZ_SIGNAL
            type.contains("nfc") -> ThreatKind.NFC_TAG
            else -> ThreatKind.UNKNOWN
        }

        val baseConfidence = when (kind) {
            ThreatKind.WIFI_AP -> 0.55f
            ThreatKind.BLE_DEVICE -> 0.50f
            ThreatKind.SUBGHZ_SIGNAL -> 0.45f
            ThreatKind.NFC_TAG -> 0.40f
            ThreatKind.UNKNOWN -> 0.20f
        }

        val rssiBoost = rssi?.let {
            when {
                it >= -50 -> 0.25f
                it >= -70 -> 0.15f
                it >= -85 -> 0.05f
                else -> 0f
            }
        } ?: 0f

        val confidence = (baseConfidence + rssiBoost).coerceIn(0f, 1f)
        val rationale = buildString {
            append("Heuristic match for ")
            append(kind.name.lowercase())
            if (label.isNotBlank()) append(" ($label)")
            rssi?.let { append(" @ ${it}dBm") }
        }

        return ThreatGuess(kind = kind, confidence = confidence, rationale = rationale)
    }
}
