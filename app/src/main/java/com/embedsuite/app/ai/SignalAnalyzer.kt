package com.embedsuite.app.ai

import com.embedsuite.app.connection.SignalEntry
import com.embedsuite.app.data.CapturedSignalEntity

object SignalAnalyzer {

    fun analyze(entry: SignalEntry): SignalAnalysis {
        val protocol = entry.protocol.uppercase()
        val freq = entry.frequency.ifBlank { "433.92" }

        val (threat, recs) = when {
            protocol.contains("KEELOQ", ignoreCase = true) -> "MEDIO" to listOf(
                "Keeloq usa rolling code — replay directo limitado.",
                "Captura múltiples tramas para análisis de secuencia.",
                "Frecuencia típica: 433.92 MHz AM."
            )
            protocol.contains("PT2262", ignoreCase = true) || protocol.contains("EV1527", ignoreCase = true) -> "ALTO" to listOf(
                "Código fijo — vulnerable a replay.",
                "Guarda RAW y considera retransmisión controlada.",
                "Común en mandos de garaje y alarmas cheap."
            )
            protocol.contains("RAW", ignoreCase = true) -> "INFO" to listOf(
                "Señal RAW sin decodificar — analiza el waveform.",
                "Compara duración de pulsos para identificar modulación ASK/OOK.",
                "Reintenta con mayor tiempo de captura."
            )
            protocol.contains("NFC", ignoreCase = true) || protocol.contains("MIFARE", ignoreCase = true) -> "MEDIO" to listOf(
                "Tarjeta de proximidad 13.56 MHz.",
                "Verifica tipo de chip (Classic, DESFire, NTAG).",
                "No clones sin autorización."
            )
            else -> "BAJO" to listOf(
                "Señal capturada correctamente.",
                "Guardada en biblioteca local con GPS.",
                "Exporta JSON/CSV desde Map/Tools."
            )
        }

        return SignalAnalysis(
            protocol = protocol.ifBlank { "DESCONOCIDO" },
            frequency = "$freq MHz",
            summary = "Señal ${entry.deviceId} @ $freq MHz — protocolo $protocol. Potencia: ${entry.power}.",
            threatLevel = threat,
            recommendations = recs
        )
    }

    fun analyzeEntity(entity: CapturedSignalEntity): SignalAnalysis {
        if (entity.signalType == "WIFI" || entity.signalType == "BLE") {
            return SignalAnalysis(
                protocol = entity.signalType,
                frequency = entity.frequency.ifBlank { "2.4 GHz" },
                summary = "${entity.name} (${entity.macAddress}) — ${entity.rssi} dBm. ${entity.detail}",
                threatLevel = if (entity.detail.contains("OPEN", ignoreCase = true)) "ALTO" else "INFO",
                recommendations = listOf(
                    "Dispositivo inalámbrico geolocalizado.",
                    "Evalúa seguridad: ${entity.detail}",
                    "Exporta desde Map/Tools → JSON/CSV."
                )
            )
        }
        return analyze(
            SignalEntry(
                timestamp = "",
                frequency = entity.frequency,
                deviceId = entity.deviceId.ifBlank { entity.macAddress },
                protocol = entity.protocol.ifBlank { entity.signalType },
                power = "${entity.rssi} dBm",
                rawData = entity.rawData
            )
        )
    }

    fun summarizeSession(signals: List<CapturedSignalEntity>): String {
        if (signals.isEmpty()) return "Sin señales en la sesión. Conecta el T-Embed y captura RF/WiFi/BLE."

        val rf = signals.count { it.signalType == "RF" }
        val wifi = signals.count { it.signalType == "WIFI" }
        val ble = signals.count { it.signalType == "BLE" }
        val withGps = signals.count { it.latitude != null }

        return buildString {
            appendLine("AUDITORÍA DE SESIÓN — EMBED AI")
            appendLine("Total capturas: ${signals.size}")
            appendLine("RF: $rf | WiFi: $wifi | BLE: $ble")
            appendLine("Con geolocalización: $withGps")
            appendLine()
            signals.take(5).forEach { s ->
                appendLine("• [${s.signalType}] ${s.name.ifBlank { s.protocol }} @ ${s.rssi}dBm")
            }
            if (signals.size > 5) appendLine("... y ${signals.size - 5} más.")
        }
    }
}
