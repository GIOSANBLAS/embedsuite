package com.embedsuite.app.data.export

import com.embedsuite.app.data.IrButtonEntity
import com.embedsuite.app.flipper.FlipperFileManager

/** Export IR captures to Flipper `.ir` format (NEC / RAW). */
object IrExporter {
    fun toFlipperIr(button: IrButtonEntity): String = FlipperFileManager.toIrContent(button)

    fun exportRaw(protocol: String, name: String, rawPayload: String): String {
        return buildString {
            appendLine("Filetype: IR signals file")
            appendLine("Version: 1")
            appendLine("# $name")
            appendLine("name: $name")
            appendLine("type: raw")
            appendLine("protocol: $protocol")
            appendLine("data: $rawPayload")
        }
    }
}
