package com.embedsuite.app.core.bruce

import com.embedsuite.app.connection.SignalEntry
import com.embedsuite.app.rf.RfLineParser
import com.embedsuite.app.rf.RfProtocolDecoder

/** Parsea respuestas multilínea del CLI Bruce para la biblioteca companion (no espejo). */
object BruceCliCaptureParser {

    data class IrCapture(
        val protocol: String,
        val address: String,
        val command: String,
        val cliCommand: String,
        val raw: String
    )

    fun parseSubGhzResponse(response: String, defaultFreqMhz: Double = 433.92): SignalEntry? {
        if (response.isBlank()) return null
        response.lineSequence().forEach { line ->
            RfLineParser.parseSubGhzSignal(line)?.let { return it }
        }
        RfProtocolDecoder.decode(response)?.let { decoded ->
            return SignalEntry(
                timestamp = "",
                frequency = decoded.frequency.ifBlank { "%.2f".format(defaultFreqMhz) },
                deviceId = decoded.hexKey.take(16).ifBlank { "DECODED" },
                protocol = decoded.protocol,
                power = "-50 dBm",
                rawData = response.take(4000)
            )
        }
        if (response.contains("RAW", ignoreCase = true) ||
            response.contains("Filetype:", ignoreCase = true) ||
            response.contains("Key:", ignoreCase = true)
        ) {
            return SignalEntry(
                timestamp = "",
                frequency = "%.2f".format(defaultFreqMhz),
                deviceId = "CLI",
                protocol = "RAW",
                power = "-50 dBm",
                rawData = response.take(4000)
            )
        }
        return null
    }

    fun parseIrCapture(response: String): IrCapture? {
        if (response.isBlank()) return null
        Regex("""(?i)ir\s+tx\s+(\w+)\s+([0-9A-Fa-f]+)\s+([0-9A-Fa-f]+)""")
            .find(response)?.let { m ->
                val cli = "ir tx ${m.groupValues[1]} ${m.groupValues[2]} ${m.groupValues[3]}"
                return IrCapture(m.groupValues[1], m.groupValues[2], m.groupValues[3], cli, response.take(2000))
            }
        Regex("""(?i)(?:Protocol|Proto):\s*(\S+).*?(?:Address|Addr):\s*([0-9A-Fa-fx]+).*?(?:Command|Cmd|Data):\s*([0-9A-Fa-fx]+)""", RegexOption.DOT_MATCHES_ALL)
            .find(response)?.let { m ->
                val cli = BruceIrCommands.irTx(m.groupValues[1], m.groupValues[2], m.groupValues[3])
                return IrCapture(m.groupValues[1], m.groupValues[2], m.groupValues[3], cli, response.take(2000))
            }
        return null
    }
}
