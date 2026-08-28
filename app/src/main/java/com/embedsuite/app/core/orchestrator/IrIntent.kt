package com.embedsuite.app.core.orchestrator

import android.content.Context
import com.embedsuite.app.core.bruce.BruceIrCommands
import com.embedsuite.app.core.connection.TransportTask
import com.embedsuite.app.data.IrButtonEntity
import com.embedsuite.app.data.IrRepository
import kotlinx.coroutines.flow.first
import java.io.File

/** MÓDULO C — IR: búsqueda local, TX desde Room/SD, captura async */
sealed class IrIntent : Intent {

    data class TransmitSaved(
        val button: IrButtonEntity,
        override val remotePath: String = "/bruce/ir/embed_${button.id}.ir"
    ) : IrIntent(), FileRunIntent {
        override val label = "IR Transmit"
        override val uploadTask = TransportTask.FILE_UPLOAD
        override val triggerTask = TransportTask.TRANSMIT_IR

        override suspend fun prepare(context: Context): Result<PrepareResult> = runCatching {
            val content = buildIrFile(button)
            val f = File.createTempFile("embed_ir_", ".ir", context.cacheDir)
            f.writeText(content)
            PrepareResult(file = f)
        }

        override fun triggerCommand(remotePath: String): String =
            "ir tx_from_file $remotePath"
    }

    data class TransmitLocal(
        val irContent: String,
        override val remotePath: String = "/bruce/ir/embed_local.ir"
    ) : IrIntent(), FileRunIntent {
        override val label = "IR Transmit Local"
        override val uploadTask = TransportTask.FILE_UPLOAD
        override val triggerTask = TransportTask.TRANSMIT_IR

        override suspend fun prepare(context: Context): Result<PrepareResult> = runCatching {
            val f = File.createTempFile("embed_ir_", ".ir", context.cacheDir)
            f.writeText(irContent)
            PrepareResult(file = f)
        }

        override fun triggerCommand(remotePath: String): String =
            "ir tx_from_file $remotePath"
    }

    data class Capture(
        val seconds: Int = 10,
        val downloadPath: String = "/bruce/ir/embed_capture.ir"
    ) : IrIntent(), AsyncCaptureIntent {
        override val label = "IR Capture"
        override val uploadTask = TransportTask.CAPTURE_IR
        override val triggerTask = TransportTask.CAPTURE_IR
        override val triggerCommand: String = "ir rx ${seconds.coerceIn(1, 60)}"
        override val waitAfterTriggerMs: Long = (seconds.coerceIn(1, 60) + 2) * 1000L
        override val preferredDownloadPath: String? = downloadPath

        override fun resolveDownloadPath(cliResponse: String): String? {
            Regex("""(?i)(/[\w./-]+\.ir)""").find(cliResponse)?.groupValues?.get(1)?.let { return it }
            if (cliResponse.contains("Protocol:", ignoreCase = true)) return null
            return downloadPath
        }
    }

    companion object {
        /** Búsqueda semántica en Room — sin CLI remota. */
        suspend fun search(query: String, irRepository: IrRepository): List<IrButtonEntity> {
            val q = query.trim().lowercase()
            if (q.isBlank()) return emptyList()
            val all = irRepository.allButtons.first()
            return all.filter {
                it.buttonName.lowercase().contains(q) ||
                    it.protocol.lowercase().contains(q) ||
                    it.irPayload.lowercase().contains(q)
            }
        }

        fun buildIrFile(button: IrButtonEntity): String {
            if (button.irPayload.startsWith("ir ", ignoreCase = true)) {
                return "Filetype: IR signals file\nVersion: 1\n# ${button.buttonName}\n${button.irPayload}\n"
            }
            return """
                Filetype: IR signals file
                Version: 1
                # ${button.buttonName}
                name: ${button.buttonName}
                type: ${button.protocol}
                protocol: ${button.protocol}
                address: ${button.hexCode.substringBefore(':').ifBlank { "00" }}
                command: ${button.hexCode.substringAfter(':', button.hexCode)}
            """.trimIndent()
        }

        fun cliFromButton(button: IrButtonEntity): String {
            val payload = button.irPayload.trim()
            if (payload.startsWith("ir ", ignoreCase = true)) return payload
            val parts = button.hexCode.split(':')
            return BruceIrCommands.irTx(
                button.protocol,
                parts.getOrElse(0) { "00" },
                parts.getOrElse(1) { parts[0] }
            )
        }
    }
}

/** @deprecated Usar [IrIntent.Capture] */
object IrCaptureIntent {
    fun listen(seconds: Int = 10): IrIntent.Capture = IrIntent.Capture(seconds)
}
