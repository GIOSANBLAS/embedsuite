package com.embedsuite.app.data.export

import com.embedsuite.app.data.CapturedSignalEntity
import com.embedsuite.app.flipper.FlipperFileManager

/** Export Sub-GHz captures to Flipper `.sub` format. */
object SubGhzExporter {
    fun toFlipperSub(signal: CapturedSignalEntity): String = FlipperFileManager.toSubContent(signal)

    fun exportRawTimings(rawTimings: List<Int>, frequencyHz: Long, protocol: String = "RAW"): String {
        val rawLine = rawTimings.joinToString(" ") { it.toString() }
        return buildString {
            appendLine("Filetype: Flipper SubGhz RAW File")
            appendLine("Version: 1")
            appendLine("Frequency: $frequencyHz")
            appendLine("Preset: FuriHalSubGhzPresetOok650Async")
            appendLine("Protocol: $protocol")
            appendLine("RAW_Data: $rawLine")
        }
    }
}
