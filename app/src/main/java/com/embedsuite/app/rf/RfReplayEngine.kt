package com.embedsuite.app.rf

import com.embedsuite.app.connection.DeviceConnectionManager
import com.embedsuite.app.connection.FirmwareProfile
import com.embedsuite.app.core.SoundFeedback
import com.embedsuite.app.data.CapturedSignalEntity
import com.embedsuite.app.data.TxHistoryRepository
import com.embedsuite.app.rf.RfFrequencyPresets.DEFAULT

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
        val decoded = decodeSignal(signal)
        val devicePath = signal.detail.takeIf { it.startsWith("device:") }
            ?.removePrefix("device:")?.trim()

        if (!devicePath.isNullOrBlank() && devicePath.endsWith(".sub", ignoreCase = true)) {
            return ReplayPreview(
                command = "tehlink:subghz_replay:$devicePath",
                protocol = decoded?.protocol ?: signal.protocol.ifBlank { "RAW" },
                frequency = signal.frequency.ifBlank { decoded?.frequency ?: DEFAULT },
                summary = "TEH-Link replay desde: $devicePath",
                canTransmit = connectionManager.detectedProfile.value == FirmwareProfile.XIBALBA,
                blockerMessage = if (connectionManager.detectedProfile.value == FirmwareProfile.XIBALBA) "" else TX_REQUIRES_XIBALBA,
                devicePath = devicePath
            )
        }

        if (decoded != null && decoded.protocol != "RAW" && decoded.hexKey.isNotBlank()) {
            return ReplayPreview(
                command = "tehlink:subghz_tx:${decoded.hexKey}",
                protocol = decoded.protocol,
                frequency = signal.frequency.ifBlank { decoded.frequency },
                summary = "TEH-Link subghz_tx (${decoded.protocol})",
                canTransmit = connectionManager.detectedProfile.value == FirmwareProfile.XIBALBA,
                blockerMessage = if (connectionManager.detectedProfile.value == FirmwareProfile.XIBALBA) "" else TX_REQUIRES_XIBALBA,
                rawHex = decoded.hexKey
            )
        }

        val rawHex = signal.rawData.replace(Regex("[^0-9A-Fa-f]"), "").takeIf { it.length >= 4 }
        return ReplayPreview(
            command = "tehlink:subghz_tx:${rawHex.orEmpty()}",
            protocol = "RAW",
            frequency = signal.frequency.ifBlank { DEFAULT },
            summary = "TEH-Link subghz_tx (RAW hex)",
            canTransmit = connectionManager.detectedProfile.value == FirmwareProfile.XIBALBA &&
                !rawHex.isNullOrBlank(),
            blockerMessage = when {
                connectionManager.detectedProfile.value != FirmwareProfile.XIBALBA -> TX_REQUIRES_XIBALBA
                rawHex.isNullOrBlank() -> "Sin payload RF para TX."
                else -> ""
            },
            rawHex = rawHex
        )
    }

    suspend fun replay(signal: CapturedSignalEntity): Result<String> {
        val preview = preview(signal)
        if (!preview.canTransmit) {
            SoundFeedback.playError()
            txHistoryRepository.record(signal, preview.command, false)
            return Result.failure(Exception(preview.blockerMessage.ifBlank { TX_REQUIRES_XIBALBA }))
        }
        val result = replayViaTehLink(preview, signal)
        val success = result.isSuccess
        if (success) SoundFeedback.playSuccess() else SoundFeedback.playError()
        txHistoryRepository.record(signal, preview.command, success)
        return result
    }

    suspend fun replayFromDeviceFile(relativePath: String): Result<String> {
        if (connectionManager.detectedProfile.value != FirmwareProfile.XIBALBA) {
            return Result.failure(Exception(TX_REQUIRES_XIBALBA))
        }
        val path = if (relativePath.startsWith("/")) relativePath else "/sdcard/$relativePath"
        return connectionManager.tehLinkRunSubGhzReplay(path).map { "TEH-Link replay OK: $path" }
    }

    private suspend fun replayViaTehLink(
        preview: ReplayPreview,
        signal: CapturedSignalEntity
    ): Result<String> {
        if (!connectionManager.hasXibalbaCapability("subghz_tx")) {
            return Result.failure(Exception("Sub-GHz TX no disponible en este dispositivo."))
        }

        val devicePath = preview.devicePath
        if (!devicePath.isNullOrBlank()) {
            val path = if (devicePath.startsWith("/")) devicePath else "/sdcard/$devicePath"
            return connectionManager.tehLinkRunSubGhzReplay(path).map {
                "TEH-Link replay OK: $path"
            }
        }

        val decoded = decodeSignal(signal)
        val rawHex = preview.rawHex ?: decoded?.hexKey?.ifBlank { null }
            ?: signal.rawData.replace(Regex("[^0-9A-Fa-f]"), "").takeIf { it.length >= 4 }
            ?: return Result.failure(Exception("Sin payload RF para TX."))

        val freq = signal.frequency.replace(Regex("[^0-9.]"), "").toDoubleOrNull()
        return connectionManager.tehLinkRunSubGhzTx(rawHex, freq).map {
            "TEH-Link TX OK: $rawHex"
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
        private const val TX_REQUIRES_XIBALBA = "TX RF requiere T-Embed Xibalba conectado (TEH-Link)."
    }
}
