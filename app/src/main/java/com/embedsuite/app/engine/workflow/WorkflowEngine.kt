package com.embedsuite.app.engine.workflow

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.coroutineContext

/**
 * Execution states of the workflow state machine.
 *
 * Transitions:
 * ```
 * IDLE -> RUNNING -> COMPLETED | FAILED | CANCELLED -> IDLE
 * RUNNING -> PAUSED -> RUNNING (reserved for future interactive steps)
 * ```
 */
enum class WorkflowRunState {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

/** Events emitted while a workflow run progresses through the state machine. */
sealed class WorkflowRunEvent {
    abstract val workflowId: String

    data class Started(
        override val workflowId: String,
        val totalSteps: Int
    ) : WorkflowRunEvent()

    data class StepStarted(
        override val workflowId: String,
        val stepId: String,
        val stepIndex: Int
    ) : WorkflowRunEvent()

    data class StepCompleted(
        override val workflowId: String,
        val stepId: String,
        val stepIndex: Int
    ) : WorkflowRunEvent()

    data class ConditionEvaluated(
        override val workflowId: String,
        val stepId: String,
        val expression: String,
        val passed: Boolean
    ) : WorkflowRunEvent()

    data class Completed(
        override val workflowId: String,
        val result: WorkflowRunResult
    ) : WorkflowRunEvent()

    data class Failed(
        override val workflowId: String,
        val result: WorkflowRunResult
    ) : WorkflowRunEvent()

    data class Cancelled(
        override val workflowId: String,
        val completedSteps: Int
    ) : WorkflowRunEvent()
}

interface WorkflowEngine {
    fun serialize(workflow: Workflow): String
    fun deserialize(raw: String): Workflow?
    suspend fun run(workflow: Workflow): WorkflowRunResult

    /** Current state of the run state machine. */
    val runState: StateFlow<WorkflowRunState>
        get() = MutableStateFlow(WorkflowRunState.IDLE)

    /** Stream of progress events for UI / logging. */
    val runEvents: SharedFlow<WorkflowRunEvent>
        get() = MutableSharedFlow()

    /** Id of the workflow currently running, or null when IDLE. */
    val activeWorkflowId: StateFlow<String?>
        get() = MutableStateFlow(null)

    /** Requests cancellation of the active run (cooperative, checked between steps). */
    fun cancel() {}
}

data class WorkflowRunResult(
    val workflowId: String,
    val completedSteps: Int,
    val totalSteps: Int,
    val success: Boolean,
    val message: String = ""
)

class SequentialWorkflowEngine(
    private val actionRunner: WorkflowActionRunner
) : WorkflowEngine {

    private val runMutex = Mutex()

    private val _runState = MutableStateFlow(WorkflowRunState.IDLE)
    override val runState: StateFlow<WorkflowRunState> = _runState.asStateFlow()

    private val _runEvents = MutableSharedFlow<WorkflowRunEvent>(extraBufferCapacity = 64)
    override val runEvents: SharedFlow<WorkflowRunEvent> = _runEvents.asSharedFlow()

    private val _activeWorkflowId = MutableStateFlow<String?>(null)
    override val activeWorkflowId: StateFlow<String?> = _activeWorkflowId.asStateFlow()

    @Volatile
    private var cancelRequested = false

    override fun cancel() {
        cancelRequested = true
    }

    override fun serialize(workflow: Workflow): String {
        val root = JSONObject().apply {
            put("id", workflow.id)
            put("name", workflow.name)
            put("description", workflow.description)
            put("trigger", workflow.trigger.name)
            put("version", workflow.version)
            put("steps", JSONArray().apply {
                workflow.steps.forEach { step ->
                    put(stepToJson(step))
                }
            })
        }
        return root.toString(2)
    }

    override fun deserialize(raw: String): Workflow? = runCatching {
        val root = JSONObject(raw)
        val stepsArr = root.optJSONArray("steps") ?: JSONArray()
        val steps = buildList {
            for (i in 0 until stepsArr.length()) {
                parseStep(stepsArr.getJSONObject(i))?.let { add(it) }
            }
        }
        Workflow(
            id = root.getString("id"),
            name = root.optString("name", root.getString("id")),
            description = root.optString("description"),
            trigger = WorkflowTrigger.valueOf(
                root.optString("trigger", WorkflowTrigger.MANUAL.name)
            ),
            steps = steps,
            version = root.optInt("version", 1)
        )
    }.getOrNull()

    override suspend fun run(workflow: Workflow): WorkflowRunResult {
        // Single-run policy: a second run request while RUNNING fails fast.
        if (!runMutex.tryLock()) {
            return WorkflowRunResult(
                workflowId = workflow.id,
                completedSteps = 0,
                totalSteps = workflow.steps.size,
                success = false,
                message = "Another workflow is already running."
            )
        }
        try {
            return executeStateMachine(workflow)
        } finally {
            runMutex.unlock()
        }
    }

    private suspend fun executeStateMachine(workflow: Workflow): WorkflowRunResult {
        cancelRequested = false
        _activeWorkflowId.value = workflow.id
        transition(WorkflowRunState.RUNNING)
        _runEvents.tryEmit(WorkflowRunEvent.Started(workflow.id, workflow.steps.size))

        if (workflow.steps.isEmpty()) {
            val result = WorkflowRunResult(
                workflowId = workflow.id,
                completedSteps = 0,
                totalSteps = 0,
                success = true,
                message = "Empty workflow."
            )
            finish(result, WorkflowRunState.COMPLETED)
            return result
        }

        var index = 0
        var completed = 0
        val visited = mutableSetOf<Int>()

        try {
            while (index in workflow.steps.indices) {
                coroutineContext.ensureActive()
                if (cancelRequested) {
                    transition(WorkflowRunState.CANCELLED)
                    _runEvents.tryEmit(WorkflowRunEvent.Cancelled(workflow.id, completed))
                    val result = WorkflowRunResult(
                        workflowId = workflow.id,
                        completedSteps = completed,
                        totalSteps = workflow.steps.size,
                        success = false,
                        message = "Cancelled by user."
                    )
                    _runEvents.tryEmit(WorkflowRunEvent.Failed(workflow.id, result))
                    return result
                }

                if (!visited.add(index)) {
                    val result = failResult(
                        workflow,
                        completed,
                        "Loop detected at step ${workflow.steps[index].id}."
                    )
                    finish(result, WorkflowRunState.FAILED)
                    return result
                }

                val step = workflow.steps[index]
                _runEvents.tryEmit(WorkflowRunEvent.StepStarted(workflow.id, step.id, index))

                when (step) {
                    is WorkflowStep.Action -> {
                        val result = actionRunner.runAction(step.pluginId, step.action, step.params)
                        completed++
                        _runEvents.tryEmit(WorkflowRunEvent.StepCompleted(workflow.id, step.id, index))
                        if (result.isFailure) {
                            val failure = failResult(
                                workflow,
                                completed,
                                "Action ${step.pluginId}/${step.action} failed: " +
                                    (result.exceptionOrNull()?.message ?: "?")
                            )
                            finish(failure, WorkflowRunState.FAILED)
                            return failure
                        }
                        index++
                    }

                    is WorkflowStep.Delay -> {
                        delay(step.delayMs.coerceAtLeast(0L))
                        completed++
                        _runEvents.tryEmit(WorkflowRunEvent.StepCompleted(workflow.id, step.id, index))
                        index++
                    }

                    is WorkflowStep.Condition -> {
                        val passed = evaluateCondition(step.expression)
                        completed++
                        _runEvents.tryEmit(
                            WorkflowRunEvent.ConditionEvaluated(
                                workflow.id, step.id, step.expression, passed
                            )
                        )
                        _runEvents.tryEmit(WorkflowRunEvent.StepCompleted(workflow.id, step.id, index))
                        val targetId = if (passed) step.onTrueStepId else step.onFalseStepId
                        index = if (targetId != null) {
                            workflow.steps.indexOfFirst { it.id == targetId }
                                .takeIf { it >= 0 } ?: (index + 1)
                        } else {
                            index + 1
                        }
                    }
                }
            }
        } catch (e: CancellationException) {
            transition(WorkflowRunState.CANCELLED)
            _runEvents.tryEmit(WorkflowRunEvent.Cancelled(workflow.id, completed))
            throw e
        } catch (e: Exception) {
            val result = failResult(workflow, completed, "Unexpected error: ${e.message}")
            finish(result, WorkflowRunState.FAILED)
            return result
        }

        val result = WorkflowRunResult(
            workflowId = workflow.id,
            completedSteps = completed,
            totalSteps = workflow.steps.size,
            success = true,
            message = "Workflow completed."
        )
        finish(result, WorkflowRunState.COMPLETED)
        return result
    }

    private fun failResult(workflow: Workflow, completed: Int, message: String) =
        WorkflowRunResult(
            workflowId = workflow.id,
            completedSteps = completed,
            totalSteps = workflow.steps.size,
            success = false,
            message = message
        )

    private suspend fun finish(result: WorkflowRunResult, terminal: WorkflowRunState) {
        transition(terminal)
        if (result.success) {
            _runEvents.tryEmit(WorkflowRunEvent.Completed(result.workflowId, result))
        } else {
            _runEvents.tryEmit(WorkflowRunEvent.Failed(result.workflowId, result))
        }
        // Return to IDLE so observers can chain runs deterministically.
        transition(WorkflowRunState.IDLE)
        _activeWorkflowId.value = null
    }

    private fun transition(next: WorkflowRunState) {
        _runState.value = next
    }

    private suspend fun evaluateCondition(expression: String): Boolean {
        val expr = expression.trim().lowercase()
        return when {
            expr == "always_true" -> true
            expr == "always_false" -> false
            expr == "connected" -> actionRunner.isConnected()
            expr == "profile_xibalba" -> actionRunner.isXibalbaProfile()
            expr.startsWith("ble_count_gte:") -> {
                val threshold = expr.removePrefix("ble_count_gte:").toIntOrNull() ?: return false
                val count = actionRunner.bleDeviceCount() ?: return false
                count >= threshold
            }
            else -> false
        }
    }

    private fun stepToJson(step: WorkflowStep): JSONObject = JSONObject().apply {
        put("id", step.id)
        put("label", step.label)
        when (step) {
            is WorkflowStep.Action -> {
                put("type", "action")
                put("pluginId", step.pluginId)
                put("action", step.action)
                put("params", JSONObject(step.params))
            }
            is WorkflowStep.Condition -> {
                put("type", "condition")
                put("expression", step.expression)
                put("onTrueStepId", step.onTrueStepId)
                put("onFalseStepId", step.onFalseStepId)
            }
            is WorkflowStep.Delay -> {
                put("type", "delay")
                put("delayMs", step.delayMs)
            }
        }
    }

    private fun parseStep(obj: JSONObject): WorkflowStep? = runCatching {
        val id = obj.getString("id")
        val label = obj.optString("label", id)
        when (obj.optString("type")) {
            "action" -> WorkflowStep.Action(
                id = id,
                label = label,
                pluginId = obj.getString("pluginId"),
                action = obj.getString("action"),
                params = jsonToMap(obj.optJSONObject("params"))
            )
            "condition" -> WorkflowStep.Condition(
                id = id,
                label = label,
                expression = obj.getString("expression"),
                onTrueStepId = obj.optString("onTrueStepId").ifBlank { null },
                onFalseStepId = obj.optString("onFalseStepId").ifBlank { null }
            )
            "delay" -> WorkflowStep.Delay(
                id = id,
                label = label,
                delayMs = obj.optLong("delayMs", 0L)
            )
            else -> null
        }
    }.getOrNull()

    private fun jsonToMap(obj: JSONObject?): Map<String, String> {
        if (obj == null) return emptyMap()
        return buildMap {
            obj.keys().forEach { key ->
                put(key, obj.optString(key))
            }
        }
    }
}
