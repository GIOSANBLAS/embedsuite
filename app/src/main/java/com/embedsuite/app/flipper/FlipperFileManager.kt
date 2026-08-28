package com.embedsuite.app.flipper

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.embedsuite.app.data.CapturedSignalEntity
import com.embedsuite.app.data.IrButtonEntity
import com.embedsuite.app.data.NfcDumpEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FlipperFileManager {

    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    fun flipperDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), "flipper")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    // ── .sub (Sub-GHz) ──

    fun toSubContent(signal: CapturedSignalEntity): String {
        val freq = parseFrequencyHz(signal.frequency)
        val rawLine = extractRawData(signal.rawData)
        val protocol = signal.protocol.ifBlank { "RAW" }
        return buildString {
            appendLine("Filetype: Flipper SubGhz RAW File")
            appendLine("Version: 1")
            appendLine("Frequency: $freq")
            appendLine("Preset: FuriHalSubGhzPresetOok650Async")
            appendLine("Protocol: $protocol")
            if (rawLine.isNotBlank()) {
                appendLine("RAW_Data: $rawLine")
            } else {
                appendLine("RAW_Data: 1000 -1000 1000 -1000")
            }
        }
    }

    fun writeSubFile(context: Context, signal: CapturedSignalEntity): File {
        val name = "embed_${signal.id}_${dateFormat.format(Date())}.sub"
        val file = File(flipperDir(context), name)
        file.writeText(toSubContent(signal))
        return file
    }

    fun parseSubFile(content: String): CapturedSignalEntity? {
        if (!content.contains("Flipper SubGhz", ignoreCase = true)) return null
        return runCatching {
            val f = com.embedsuite.app.engine.decoder.SubFileParser.parseFlipperSub(content)
            CapturedSignalEntity(
                signalType = "RF",
                name = f.protocol,
                label = "Importado .sub",
                protocol = f.protocol,
                frequency = String.format(Locale.US, "%.2f", f.frequencyMhz()),
                rawData = f.rawTimings.joinToString(" ").ifBlank { f.key.orEmpty() },
                detail = f.toSubContent().take(4000)
            )
        }.getOrNull()
    }

    // ── .ir (Infrared) ──

    fun toIrContent(button: IrButtonEntity): String {
        return buildString {
            appendLine("Filetype: IR signals file")
            appendLine("Version: 1")
            appendLine("# ${button.buttonName}")
            appendLine("name: ${button.buttonName}")
            appendLine("type: ${button.protocol}")
            appendLine("protocol: ${button.protocol}")
            appendLine("address: 0x00FF")
            appendLine("command: ${button.hexCode.ifBlank { "0x00FF" }}")
        }
    }

    fun writeIrFile(context: Context, button: IrButtonEntity): File {
        val file = File(flipperDir(context), "${button.buttonName.replace(" ", "_")}.ir")
        file.writeText(toIrContent(button))
        return file
    }

    fun parseIrFile(content: String): IrButtonEntity? {
        if (!content.contains("IR signals", ignoreCase = true)) return null
        val name = Regex("""name:\s*(.+)""").find(content)?.groupValues?.get(1)?.trim() ?: "Imported"
        val protocol = Regex("""protocol:\s*(\S+)""").find(content)?.groupValues?.get(1) ?: "NEC"
        val command = Regex("""command:\s*(\S+)""").find(content)?.groupValues?.get(1) ?: "0x00FF"
        return IrButtonEntity(
            buttonName = name,
            protocol = protocol,
            hexCode = command,
            irPayload = "ir tx $protocol 0x00FF $command"
        )
    }

    // ── .nfc ──

    fun toNfcContent(dump: NfcDumpEntity): String {
        return buildString {
            appendLine("Filetype: Flipper NFC device")
            appendLine("Version: 4")
            appendLine("Device type: ${dump.tagType.ifBlank { "MIFARE Classic 1K" }}")
            appendLine("UID: ${dump.uid}")
            appendLine("# EMBED SUITE export")
            appendLine(dump.rawDump)
        }
    }

    fun writeNfcFile(context: Context, dump: NfcDumpEntity): File {
        val file = File(flipperDir(context), "nfc_${dump.uid.replace(":", "")}.nfc")
        file.writeText(toNfcContent(dump))
        return file
    }

    fun parseNfcFile(content: String): NfcDumpEntity? {
        if (!content.contains("Flipper NFC", ignoreCase = true) && !content.contains("UID:", ignoreCase = true)) return null
        val uid = Regex("""UID:\s*([0-9A-Fa-f:]+)""").find(content)?.groupValues?.get(1) ?: return null
        val type = Regex("""Device type:\s*(.+)""").find(content)?.groupValues?.get(1)?.trim() ?: "Unknown"
        return NfcDumpEntity(uid = uid, tagType = type, rawDump = content)
    }

    fun shareFile(context: Context, file: File): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = when (file.extension.lowercase()) {
                "sub" -> "text/plain"
                "ir" -> "text/plain"
                "nfc" -> "text/plain"
                else -> "*/*"
            }
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun parseFrequencyHz(freq: String): Long {
        val mhz = freq.replace("MHz", "").trim().toDoubleOrNull() ?: 433.92
        return (mhz * 1_000_000).toLong()
    }

    private fun extractRawData(raw: String): String {
        Regex("""RAW_Data:\s*(.+)""", RegexOption.IGNORE_CASE).find(raw)?.groupValues?.get(1)?.trim()?.let {
            return it
        }
        if (raw.matches(Regex("""[\d\s\-]+"""))) return raw.trim()
        val numbers = Regex("""-?\d+""").findAll(raw).map { it.value }.toList()
        if (numbers.size >= 4) return numbers.joinToString(" ")
        return ""
    }
}
