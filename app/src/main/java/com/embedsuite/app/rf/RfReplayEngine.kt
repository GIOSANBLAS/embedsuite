package com.embedsuite.app.rf

import android.content.Context
import com.embedsuite.app.connection.BruceCommands
import com.embedsuite.app.connection.DeviceConnectionManager
import com.embedsuite.app.core.SoundFeedback
import com.embedsuite.app.data.CapturedSignalEntity
import com.embedsuite.app.data.TxHistoryRepository
import com.embedsuite.app.flipper.FlipperFileManager
import java.io.File

class RfReplayEngine(
    private val context: Context,
    private val connectionManager: DeviceConnectionManager,
    private val txHistoryRepository: TxHistoryRepository
) {

    data class ReplayPreview(
        val command: String,
        val protocol: String,
        val frequency: String,
        val summary: String,
        val subFile: File?,
        val canTransmit: Boolean,
        val blockerMessage: String = "",
        /** Si true, replay hará push USB del .sub antes de tx_from_file */
        val needsPush: Boolean = false,
        val devicePath: String? = null
    )

    fun preview(signal: CapturedSignalEntity): ReplayPreview {
        val decoded = decodeSignal(signal)
        val devicePath = signal.detail.takeIf { it.startsWith("device:") }
            ?.removePrefix("device:")?.trim()

        if (!devicePath.isNullOrBlank() && devicePath.endsWith(".sub", ignoreCase = true)) {
            val cmd = BruceCommands.subGhzTxFromFile(devicePath)
            return ReplayPreview(
                command = cmd,
                protocol = decoded?.protocol ?: signal.protocol.ifBlank { "RAW" },
                frequency = signal.frequency.ifBlank { decoded?.frequency ?: RfFrequencyPresets.DEFAULT },
                summary = "TX desde SD/LittleFS: $devicePath",
                subFile = null,
                canTransmit = true,
                devicePath = devicePath
            )
        }

        if (decoded != null && decoded.protocol != "RAW" && decoded.hexKey.isNotBlank()) {
            val te = decoded.te.takeIf { it > 0 } ?: 174
            val cmd = BruceCommands.subGhzTx(
                hexKey = decoded.hexKey,
                frequencyMhz = signal.frequency.ifBlank { decoded.frequency }.ifBlank { RfFrequencyPresets.DEFAULT },
                te = te,
                count = 10
            )
            val xibalba = connectionManager.detectedProfile.value ==
                com.embedsuite.app.connection.FirmwareProfile.XIBALBA
            return ReplayPreview(
                command = cmd,
                protocol = decoded.protocol,
                frequency = signal.frequency.ifBlank { decoded.frequency },
                summary = if (xibalba) {
                    "TEH-Link subghz_tx (${decoded.protocol})"
                } else {
                    RfProtocolDecoder.formatDecoded(decoded)
                },
                subFile = null,
                canTransmit = true
            )
        }

        val pushPath = BruceCommands.embedPushSubPath(signal.id)
        val localSub = FlipperFileManager.writeSubFile(context, signal)
        val usbReady = connectionManager.isUsbActive()

        return ReplayPreview(
            command = BruceCommands.subGhzTxFromFile(pushPath),
            protocol = "RAW",
            frequency = signal.frequency.ifBlank { RfFrequencyPresets.DEFAULT },
            summary = if (usbReady) BruceCommands.TX_PUSH_USB_HINT else BruceCommands.TX_REQUIRES_DEVICE_FILE,
            subFile = localSub,
            canTransmit = usbReady,
            blockerMessage = if (usbReady) "" else BruceCommands.TX_REQUIRES_DEVICE_FILE,
            needsPush = true,
            devicePath = pushPath
        )
    }

    suspend fun replay(signal: CapturedSignalEntity): Result<String> {
        val preview = preview(signal)
        if (!preview.canTransmit) {
            SoundFeedback.playError()
            txHistoryRepository.record(signal, preview.command, false)
            return Result.failure(Exception(preview.blockerMessage.ifBlank { BruceCommands.TX_REQUIRES_DEVICE_FILE }))
        }

        if (preview.needsPush) {
            val content = BruceCommands.preparePushContent(FlipperFileManager.toSubContent(signal))
                .getOrElse {
                    SoundFeedback.playError()
                    txHistoryRepository.record(signal, preview.command, false)
                    return Result.failure(it)
                }
            val path = preview.devicePath ?: BruceCommands.embedPushSubPath(signal.id)
            val pushed = connectionManager.writeTextFileToDevice(path, content)
            if (pushed.isFailure) {
                SoundFeedback.playError()
                txHistoryRepository.record(signal, "push $path", false)
                return Result.failure(pushed.exceptionOrNull() ?: Exception("Push falló"))
            }
        }

        val result = if (connectionManager.detectedProfile.value == com.embedsuite.app.connection.FirmwareProfile.XIBALBA) {
            replayViaTehLink(preview, signal)
        } else {
            connectionManager.sendCommand(preview.command)
        }
        val success = result.isSuccess
        if (success) SoundFeedback.playSuccess() else SoundFeedback.playError()
        txHistoryRepository.record(signal, preview.command, success)
        return result.map { "TX OK: ${preview.command}" }
    }

    suspend fun replayFromDeviceFile(relativePath: String): Result<String> {
        val cmd = BruceCommands.subGhzTxFromFile(relativePath)
        return connectionManager.sendCommand(cmd).map { "TX OK: $cmd" }
    }

    private suspend fun replayViaTehLink(
        preview: ReplayPreview,
        signal: CapturedSignalEntity
    ): Result<String> {
        if (!connectionManager.hasXibalbaCapability("subghz_tx")) {
            return Result.failure(Exception("Sub-GHz TX no disponible en este dispositivo."))
        }

        val devicePath = preview.devicePath
        if (!devicePath.isNullOrBlank() && devicePath.startsWith("/sdcard/")) {
            return connectionManager.tehLinkRunSubGhzReplay(devicePath).map {
                "TEH-Link replay OK: $devicePath"
            }
        }

        val decoded = decodeSignal(signal)
        val rawHex = decoded?.hexKey?.ifBlank { null }
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
                frequency = signal.frequency.ifBlank { RfFrequencyPresets.DEFAULT },
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
}
