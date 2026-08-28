package com.embedsuite.app.core.bruce

import com.embedsuite.app.connection.TehLinkIrUtils
import com.embedsuite.app.engine.sync.IrdbParser

/** Convierte payloads Flipper/IRDB a comandos CLI Bruce (`ir tx` / `ir tx_raw`). */
object BruceIrCommands {

    fun irTx(protocol: String, addressWord: String, commandWord: String): String =
        TehLinkIrUtils.irTx(protocol, addressWord, commandWord)

    fun fromFlipperButton(button: IrdbParser.IrButton): String {
        val proto = button.protocol.trim().ifBlank { "NEC" }
        val data = button.data.trim()
        if (data.isBlank()) return ""
        if (data.startsWith("ir ", ignoreCase = true)) return TehLinkIrUtils.normalizeIrCommand(data)
        return when (proto.uppercase()) {
            "RAW" -> {
                val freq = Regex("""(\d{4,6})""").find(data)?.value ?: "38000"
                val hex = data.replace(Regex("[^0-9A-Fa-f]"), "").take(512)
                if (hex.length >= 4) "ir tx_raw $freq $hex" else ""
            }
            "NEC", "NECext", "SIRC", "RC5", "RC6", "Samsung32" -> {
                val tokens = data.split(Regex("\\s+")).filter { it.isNotBlank() }
                when {
                    tokens.size >= 2 -> irTx(proto, tokens[0], tokens[1])
                    tokens.size == 1 && tokens[0].length >= 8 -> {
                        val word = tokens[0]
                        irTx(proto, word.take(8), word.drop(8).ifBlank { "00" })
                    }
                    else -> ""
                }
            }
            else -> {
                val tokens = data.split(Regex("\\s+")).filter { it.isNotBlank() }
                if (tokens.size >= 2) irTx(proto, tokens[0], tokens[1]) else ""
            }
        }
    }
}
