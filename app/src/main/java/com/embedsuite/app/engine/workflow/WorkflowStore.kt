package com.embedsuite.app.engine.workflow

import android.content.Context
import java.io.File

class WorkflowStore(
    private val context: Context,
    private val engine: WorkflowEngine
) {
    private val dir: File
        get() = File(context.filesDir, "workflows").also { it.mkdirs() }

    fun list(): List<WorkflowFileEntry> =
        dir.listFiles { file -> file.name.endsWith(WORKFLOW_FILE_EXTENSION) }
            ?.sortedBy { it.name.lowercase() }
            ?.map { file ->
                WorkflowFileEntry(
                    id = file.nameWithoutExtension,
                    fileName = file.name,
                    sizeBytes = file.length(),
                    lastModifiedMs = file.lastModified()
                )
            }
            .orEmpty()

    fun listStored(): List<Workflow> =
        list().mapNotNull { entry -> load(entry.id)?.let { engine.deserialize(it) } }

    fun load(id: String): String? {
        val file = File(dir, "$id$WORKFLOW_FILE_EXTENSION")
        return file.takeIf { it.isFile }?.readText()
    }

    fun save(workflow: Workflow) {
        File(dir, "${workflow.id}$WORKFLOW_FILE_EXTENSION")
            .writeText(engine.serialize(workflow))
    }

    fun saveRaw(id: String, raw: String) {
        File(dir, "$id$WORKFLOW_FILE_EXTENSION").writeText(raw)
    }

    fun delete(id: String) {
        File(dir, "$id$WORKFLOW_FILE_EXTENSION").delete()
    }

    fun importRaw(raw: String): Result<Workflow> = runCatching {
        val signed = WorkflowMarketplace.validateSignature(raw)
        val payload = when (signed) {
            is WorkflowMarketplace.SignatureValidation.ValidLocal,
            is WorkflowMarketplace.SignatureValidation.Unsigned -> raw
            is WorkflowMarketplace.SignatureValidation.Unknown -> raw
            is WorkflowMarketplace.SignatureValidation.Invalid -> error(signed.reason)
        }
        val clean = WorkflowMarketplace.stripSignature(payload)
        val workflow = engine.deserialize(clean) ?: error("JSON de workflow inválido")
        save(workflow)
        workflow
    }

    fun exportRaw(workflow: Workflow): String =
        WorkflowMarketplace.signForSharing(engine.serialize(workflow))

    data class WorkflowFileEntry(
        val id: String,
        val fileName: String,
        val sizeBytes: Long,
        val lastModifiedMs: Long
    )
}
