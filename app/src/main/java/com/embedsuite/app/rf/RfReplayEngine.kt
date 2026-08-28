package com.embedsuite.app.rf

import com.embedsuite.app.connection.ConnectionState
import com.embedsuite.app.connection.DeviceConnectionManager
import com.embedsuite.app.core.SoundFeedback
import com.embedsuite.app.core.bruce.BruceCli
import com.embedsuite.app.data.CapturedSignalEntity
import com.embedsuite.app.data.TxHistoryRepository
import com.embedsuite.app.rf.RfFrequencyPresets.DEFAULT

/** Retransmisión Sub-GHz vía CLI Bruce — companion, no espejo del menú T-Embed. */
class RfReplayEngine(
    private val connectionManager: DeviceConnectionManager,
    private val txHistoryRepository: TxHistoryRepository
) {

    data class ReplayPreview(
        val command: String,
        val protocol: String,
        val frequency: String,
        val summary: String,
        val canTransmit: Boolean,
        val blockerMessage: String = "",
        val rawHex: String? = null,
        val devicePath: String? = null
    )

    fun preview(signal: CapturedSignalEntity): ReplayPreview {
        val linked = connectionManager.connectionState.value is ConnectionState.Connected
        val cliReady = connectionManager.bruceLinkReady.value
        val decoded = decodeSignal(signal)
        val devicePath = signal.detail.takeIf { it.startsWith("device:") }
            ?.removePrefix("device:")?.trim()

        fun gate(okPayload: Boolean, emptyPayloadMsg: String): Pair<Boolean, String> {
            return when {
                !linked -> false to TX_REQUIRES_LINK
                !cliReady -> false to TX_REQUIRES_CLI
                !okPayload -> false to emptyPayloadMsg
                else -> true to ""
            }
        }

        if (!devicePath.isNullOrBlank() && devicePath.endsWith(".sub", ignoreCase = true)) {
            val path = normalizeDevicePath(devicePath)
            val (can, blocker) = gate(true, "")
            return ReplayPreview(
                command = "subghz tx_from_file $path",
                protocol = decoded?.protocol ?: signal.protocol.ifBlank { "RAW" },
                frequency = signal.frequency.ifBlank { decoded?.frequency ?: DEFAULT },
                summary = "Enviar al CC1101 desde SD: $path",
                canTransmit = can,
                blockerMessage = blocker,
                devicePath = path
            )
        }

        if (decoded != null && decoded.protocol != "RAW" && decoded.hexKey.isNotBlank()) {
            val freqHz = freqMhzToHz(signal.frequency.ifBlank { decoded.frequency })
            val (can, blocker) = gate(true, "")
            return ReplayPreview(
                command = "subghz tx ${decoded.hexKey} $freqHz ${decoded.te} 10",
                protocol = decoded.protocol,
                frequency = signal.frequency.ifBlank { decoded.frequency },
                summary = "CLI Bruce · subghz tx (${decoded.protocol})",
                canTransmit = can,
                blockerMessage = blocker,
                rawHex = decoded.hexKey
            )
        }

        val rawHex = signal.rawData.replace(Regex("[^0-9A-Fa-f]"), "").takeIf { it.length >= 4 }
        val (can, blocker) = gate(!rawHex.isNullOrBlank(), "Sin payload RF para TX.")
        val freqHz = freqMhzToHz(signal.frequency)
        return ReplayPreview(
            command = if (!rawHex.isNullOrBlank()) "subghz tx $rawHex $freqHz 174 10" else "",
            protocol = "RAW",
            frequency = signal.frequency.ifBlank { DEFAULT },
            summary = "CLI Bruce · subghz tx (RAW)",
            canTransmit = can,
            blockerMessage = blocker,
            rawHex = rawHex
        )
    }

    suspend fun replay(signal: CapturedSignalEntity): Result<String> {
        return try {
            val preview = preview(signal)
            if (!preview.canTransmit) {
                SoundFeedback.playError()
                runCatching { txHistoryRepository.record(signal, preview.command, false) }
                return Result.failure(Exception(preview.blockerMessage.ifBlank { TX_REQUIRES_CLI }))
            }
            val result = transmitViaBruceCli(preview, signal)
            val success = result.isSuccess
            if (success) SoundFeedback.playSuccess() else SoundFeedback.playError()
            runCatching { txHistoryRepository.record(signal, preview.command, success) }
            result
        } catch (e: Exception) {
            SoundFeedback.playError()
            Result.failure(Exception("TX falló: ${e.message ?: e.javaClass.simpleName}"))
        }
    }

    suspend fun replayFromDeviceFile(relativePath: String): Result<String> {
        if (connectionManager.connectionState.value !is ConnectionState.Connected) {
            return Result.failure(Exception(TX_REQUIRES_LINK))
        }
        if (!connectionManager.bruceLinkReady.value) {
            return Result.failure(Exception(TX_REQUIRES_CLI))
        }
        val path = normalizeDevicePath(relativePath)
        return connectionManager.tehLinkRunSubGhzReplay(path).map { "Enviado: $path" }
    }

    private suspend fun transmitViaBruceCli(
        preview: ReplayPreview,
        signal: CapturedSignalEntity
    ): Result<String> {
        preview.devicePath?.let { path ->
            return connectionManager.tehLinkRunSubGhzReplay(path).map { "Enviado: $path" }
        }

        val decoded = decodeSignal(signal)
        val rawHex = preview.rawHex ?: decoded?.hexKey?.ifBlank { null }
            ?: signal.rawData.replace(Regex("[^0-9A-Fa-f]"), "").takeIf { it.length >= 4 }
            ?: return Result.failure(Exception("Sin payload RF para TX."))

        val freq = signal.frequency.replace(Regex("[^0-9.]"), "").toDoubleOrNull()
        return connectionManager.tehLinkRunSubGhzTx(rawHex, freq).map {
            "TX OK · ${preview.protocol} @ ${preview.frequency} MHz"
        }
    }

    private fun decodeSignal(signal: CapturedSignalEntity): DecodedRfSignal? {
        RfProtocolDecoder.decode(signal.rawData)?.let { return it }
        RfProtocolDecoder.decode(signal.decodedFields)?.let { return it }
        if (signal.protocol.isNotBlank() && signal.deviceId.isNotBlank() &&
            signal.protocol != "RAW"
        ) {
            return DecodedRfSignal(
                protocol = signal.protocol,
                frequency = signal.frequency.ifBlank { DEFAULT },
                hexKey = signal.deviceId,
                bitCount = 24,
                te = 174
            )
        }
        return null
    }

    private fun freqMhzToHz(freq: String): Long {
        val mhz = freq.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 433.92
        return BruceCli.mhzToHz(mhz)
    }

    private fun normalizeDevicePath(path: String): String {
        val trimmed = path.trim()
        if (trimmed.startsWith("/")) return trimmed
        return "/$trimmed"
    }

    fun compareSignals(a: CapturedSignalEntity, b: CapturedSignalEntity): String {
        val score = similarityScore(a, b)
        val verdict = when {
            score >= 0.9f -> "MISMA señal (replay viable)"
            score >= 0.6f -> "SIMILAR — posible mismo dispositivo"
            score >= 0.3f -> "PARCIALMENTE similar"
            else -> "DIFERENTE"
        }
        return buildString {
            appendLine("Comparación RF: $verdict (${"%.0f".format(score * 100)}%)")
            appendLine("A: ${a.protocol} ${a.frequency} ${a.deviceId}")
            appendLine("B: ${b.protocol} ${b.frequency} ${b.deviceId}")
            if (a.protocol == b.protocol && a.deviceId == b.deviceId) appendLine("Protocolo y device ID coinciden.")
        }
    }

    private fun similarityScore(a: CapturedSignalEntity, b: CapturedSignalEntity): Float {
        var score = 0f
        if (a.protocol.equals(b.protocol, ignoreCase = true) && a.protocol.isNotBlank()) score += 0.4f
        if (a.frequency == b.frequency && a.frequency.isNotBlank()) score += 0.2f
        if (a.deviceId == b.deviceId && a.deviceId.isNotBlank()) score += 0.3f
        if (a.rawData.isNotBlank() && a.rawData == b.rawData) score += 0.1f
        return score.coerceIn(0f, 1f)
    }

    companion object {
        private const val TX_REQUIRES_CLI = "Espera CLI OK (comando info) con USB/BLE/WiFi."
        private const val TX_REQUIRES_LINK = "Sin conexión. Conecta el T-Embed (USB OTG, BLE o WiFi WebUI)."
    }
}
