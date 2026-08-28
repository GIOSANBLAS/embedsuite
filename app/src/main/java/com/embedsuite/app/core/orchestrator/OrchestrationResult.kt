package com.embedsuite.app.core.orchestrator

import java.io.File

data class OrchestrationResult(
    val success: Boolean,
    val message: String,
    val phase: IntentPhase = IntentPhase.DONE,
    val cliResponse: String = "",
    val localFile: File? = null,
    val remotePath: String? = null,
    val cliCommand: String? = null,
    val downloadedBytes: Int = 0,
    val artifact: Any? = null
)
