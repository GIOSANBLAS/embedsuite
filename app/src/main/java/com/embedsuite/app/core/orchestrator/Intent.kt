package com.embedsuite.app.core.orchestrator

import android.content.Context
import com.embedsuite.app.core.connection.TransportTask
import java.io.File

enum class IntentPhase {
    PREPARE,
    UPLOAD,
    TRIGGER,
    DOWNLOAD,
    POST_PROCESS,
    DONE
}

data class PrepareResult(
    val file: File? = null,
    val metadata: Map<String, String> = emptyMap()
)

/** Intención de alto nivel — el usuario nunca escribe CLI manualmente. */
sealed interface Intent {
    val label: String
    val uploadTask: TransportTask
    val triggerTask: TransportTask
}

/** Pipeline estándar: preparar → WiFi upload → BLE disparo. */
sealed interface FileRunIntent : Intent {
    suspend fun prepare(context: Context): Result<PrepareResult>
    val remotePath: String
    fun triggerCommand(remotePath: String): String
}

/** Captura asíncrona: BLE trigger → espera → WiFi download → analizar. */
sealed interface AsyncCaptureIntent : Intent {
    val triggerCommand: String
    val waitAfterTriggerMs: Long
    val preferredDownloadPath: String?
    fun resolveDownloadPath(cliResponse: String): String?
}

/** Comando simple sin archivo (info, nav, etc.). */
data class DirectCliIntent(
    override val label: String,
    val command: String,
    override val triggerTask: TransportTask = TransportTask.CLI_TRIGGER
) : Intent {
    override val uploadTask: TransportTask = TransportTask.CLI_TRIGGER
}

/** Export local — sin ejecución remota Bruce. */
data class LocalExportIntent(
    override val label: String,
    val exportFileName: String,
    val buildPayload: suspend (Context) -> Result<String>
) : Intent {
    override val uploadTask: TransportTask = TransportTask.DISCOVERY
    override val triggerTask: TransportTask = TransportTask.DISCOVERY
}
