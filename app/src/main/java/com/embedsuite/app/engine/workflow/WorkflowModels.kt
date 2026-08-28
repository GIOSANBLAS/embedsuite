package com.embedsuite.app.engine.workflow

const val WORKFLOW_FILE_EXTENSION = ".ewf"

enum class WorkflowTrigger {
    MANUAL,
    ON_CONNECT,
    ON_SIGNAL,
    SCHEDULED
}

sealed class WorkflowStep {
    abstract val id: String
    abstract val label: String

    data class Action(
        override val id: String,
        override val label: String,
        val pluginId: String,
        val action: String,
        val params: Map<String, String> = emptyMap()
    ) : WorkflowStep()

    data class Condition(
        override val id: String,
        override val label: String,
        val expression: String,
        val onTrueStepId: String? = null,
        val onFalseStepId: String? = null
    ) : WorkflowStep()

    data class Delay(
        override val id: String,
        override val label: String,
        val delayMs: Long
    ) : WorkflowStep()
}

data class Workflow(
    val id: String,
    val name: String,
    val description: String = "",
    val trigger: WorkflowTrigger = WorkflowTrigger.MANUAL,
    val steps: List<WorkflowStep> = emptyList(),
    val version: Int = 1
)
