package com.embedsuite.app.core.orchestrator

import android.content.Context
import com.embedsuite.app.connection.DeviceConnectionManager
import com.embedsuite.app.core.connection.TransportOrchestrator
import com.embedsuite.app.core.connection.TransportTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Orquestador de intenciones — pipeline garantizado:
 * 1) Preparación (Android)  2) Inyección (WiFi)  3) Disparo (BLE)
 * + flujo async Capturar → Transferir → Analizar para RF/IR pesado.
 */
class IntentOrchestrator(
    private val context: Context,
    private val transportOrchestrator: TransportOrchestrator,
    private val connectionManager: DeviceConnectionManager
) {

    suspend fun execute(intent: Intent): OrchestrationResult = withContext(Dispatchers.IO) {
        when (intent) {
            is FileRunIntent -> executeFileRun(intent)
            is AsyncCaptureIntent -> executeAsyncCapture(intent)
            is DirectCliIntent -> executeDirectCli(intent.triggerTask, intent.command)
            is LocalExportIntent -> executeLocalExport(intent)
            is BruceIntentLegacy -> executeLegacy(intent)
        }
    }

    suspend fun executeDirectCli(task: TransportTask, cliLine: String): OrchestrationResult {
        val result = transportOrchestrator.executeBruceCliForTask(task, cliLine.trim())
        return result.fold(
            onSuccess = { response ->
                OrchestrationResult(
                    success = true,
                    message = "OK",
                    phase = IntentPhase.DONE,
                    cliResponse = response,
                    cliCommand = cliLine.trim()
                )
            },
            onFailure = { err ->
                OrchestrationResult(
                    success = false,
                    message = err.message ?: "CLI falló",
                    phase = IntentPhase.TRIGGER,
                    cliCommand = cliLine.trim()
                )
            }
        )
    }

    private suspend fun executeFileRun(intent: FileRunIntent): OrchestrationResult {
        var localFile: File? = null
        val remote = intent.remotePath

        val prepared = intent.prepare(context)
        if (prepared.isFailure) {
            return OrchestrationResult(
                success = false,
                message = "Preparación falló: ${prepared.exceptionOrNull()?.message}",
                phase = IntentPhase.PREPARE
            )
        }
        localFile = prepared.getOrThrow().file
        if (localFile == null || !localFile.exists()) {
            return OrchestrationResult(
                success = false,
                message = "Archivo local no generado",
                phase = IntentPhase.PREPARE
            )
        }

        val uploaded = transportOrchestrator.uploadHeavyFile(localFile, remote)
        if (uploaded.isFailure) {
            rollbackLocal(localFile)
            return OrchestrationResult(
                success = false,
                message = "Upload WiFi falló: ${uploaded.exceptionOrNull()?.message}",
                phase = IntentPhase.UPLOAD,
                localFile = localFile,
                remotePath = remote
            )
        }

        val cli = intent.triggerCommand(remote)
        val fired = transportOrchestrator.executeBruceCliForTask(intent.triggerTask, cli)
        rollbackLocal(localFile)

        return fired.fold(
            onSuccess = { response ->
                OrchestrationResult(
                    success = true,
                    message = "${intent.label}: listo",
                    phase = IntentPhase.DONE,
                    cliResponse = response,
                    remotePath = remote,
                    cliCommand = cli
                )
            },
            onFailure = { err ->
                OrchestrationResult(
                    success = false,
                    message = "Disparo falló: ${err.message}",
                    phase = IntentPhase.TRIGGER,
                    remotePath = remote,
                    cliCommand = cli
                )
            }
        )
    }

    private suspend fun executeAsyncCapture(intent: AsyncCaptureIntent): OrchestrationResult {
        val triggered = transportOrchestrator.executeBruceCliForTask(
            intent.triggerTask,
            intent.triggerCommand
        )
        if (triggered.isFailure) {
            return OrchestrationResult(
                success = false,
                message = "Captura falló: ${triggered.exceptionOrNull()?.message}",
                phase = IntentPhase.TRIGGER,
                cliCommand = intent.triggerCommand
            )
        }
        val cliResponse = triggered.getOrThrow()
        val isSubGhz = intent is SubGhzIntent.Capture
        val isIr = intent is IrIntent.Capture
        val waited = when {
            isSubGhz -> RxCompletionWaiter.waitSubGhzRx(connectionManager, intent.waitAfterTriggerMs)
            isIr -> RxCompletionWaiter.waitIrRx(connectionManager, intent.waitAfterTriggerMs)
            else -> {
                delay(intent.waitAfterTriggerMs)
                true
            }
        }
        if (!waited) delay(2_000L)

        val downloadPath = intent.resolveDownloadPath(cliResponse) ?: intent.preferredDownloadPath
        if (downloadPath == null) {
            return OrchestrationResult(
                success = true,
                message = "${intent.label}: respuesta CLI (sin archivo SD)",
                phase = IntentPhase.DONE,
                cliResponse = cliResponse,
                cliCommand = intent.triggerCommand
            )
        }

        val localFile = File(context.cacheDir, "embed_capture_${System.currentTimeMillis()}.bin")
        val downloaded = transportOrchestrator.downloadHeavyFile(downloadPath, localFile)
        if (downloaded.isFailure) {
            val viaCli = connectionManager.readStorageFile(downloadPath)
            if (viaCli.isSuccess) {
                localFile.writeText(viaCli.getOrThrow())
            } else {
                return OrchestrationResult(
                    success = true,
                    message = "Captura OK — download falló, usando respuesta CLI",
                    phase = IntentPhase.DONE,
                    cliResponse = cliResponse,
                    cliCommand = intent.triggerCommand
                )
            }
        }

        val content = localFile.readText()
        val artifact = (intent as? SubGhzIntent.Capture)?.let {
            CapturedSignal.fromSubContent(content, it.freqMhz)
        }

        return OrchestrationResult(
            success = true,
            message = "${intent.label}: ${localFile.length()} bytes",
            phase = IntentPhase.DONE,
            cliResponse = cliResponse,
            cliCommand = intent.triggerCommand,
            localFile = localFile,
            remotePath = downloadPath,
            downloadedBytes = localFile.length().toInt(),
            artifact = artifact
        )
    }

    private suspend fun executeLocalExport(intent: LocalExportIntent): OrchestrationResult {
        val built = intent.buildPayload(context)
        if (built.isFailure) {
            return OrchestrationResult(
                success = false,
                message = built.exceptionOrNull()?.message ?: "Export falló",
                phase = IntentPhase.PREPARE
            )
        }
        val dir = File(context.cacheDir, "intent_exports").apply { mkdirs() }
        val out = File(dir, intent.exportFileName)
        out.writeText(built.getOrThrow())
        return OrchestrationResult(
            success = true,
            message = "Exportado localmente",
            phase = IntentPhase.DONE,
            localFile = out
        )
    }

    /** Compatibilidad con API anterior (BruceIntent). */
    private suspend fun executeLegacy(intent: BruceIntentLegacy): OrchestrationResult = when (intent) {
        is BruceIntentLegacy.DirectCli -> executeDirectCli(intent.transportTask, intent.command)
        is BruceIntentLegacy.FilePipeline -> executeFileRun(
            LegacyFileRunAdapter(intent)
        )
    }

    private data class LegacyFileRunAdapter(
        private val legacy: BruceIntentLegacy.FilePipeline
    ) : FileRunIntent {
        override val label = legacy.label
        override val uploadTask = legacy.transportTask
        override val triggerTask = TransportTask.CLI_TRIGGER
        override val remotePath = legacy.remotePath
        override suspend fun prepare(ctx: Context) =
            legacy.prepareFile(ctx).map { PrepareResult(file = it) }
        override fun triggerCommand(remotePath: String) = legacy.cliFromRemotePath(remotePath)
    }

    private fun rollbackLocal(file: File?) {
        runCatching { file?.delete() }
    }
}

/** Tipos legacy — migrar a módulos concretos. */
sealed class BruceIntentLegacy {
    abstract val transportTask: TransportTask

    data class FilePipeline(
        val label: String,
        val remotePath: String,
        val prepareFile: suspend (Context) -> Result<File>,
        val cliFromRemotePath: (String) -> String,
        override val transportTask: TransportTask = TransportTask.FILE_UPLOAD
    ) : BruceIntentLegacy()

    data class DirectCli(
        val command: String,
        override val transportTask: TransportTask = TransportTask.CLI_TRIGGER
    ) : BruceIntentLegacy()
}
