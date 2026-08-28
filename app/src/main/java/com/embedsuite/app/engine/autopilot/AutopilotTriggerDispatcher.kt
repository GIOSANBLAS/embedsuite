package com.embedsuite.app.engine.autopilot

import android.util.Log
import com.embedsuite.app.connection.ConnectionState
import com.embedsuite.app.connection.DeviceEvent
import com.embedsuite.app.engine.workflow.Workflow
import com.embedsuite.app.engine.workflow.WorkflowEngine
import com.embedsuite.app.engine.workflow.WorkflowRunResult
import com.embedsuite.app.engine.workflow.WorkflowRunState
import com.embedsuite.app.engine.workflow.WorkflowTrigger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Events emitted by the trigger dispatcher so the UI can show why a workflow fired. */
sealed class AutopilotTriggerEvent {
    data class Triggered(val workflowId: String, val trigger: WorkflowTrigger) : AutopilotTriggerEvent()
    data class Skipped(val workflowId: String, val reason: String) : AutopilotTriggerEvent()
    data class RunFinished(val result: WorkflowRunResult) : AutopilotTriggerEvent()
}

/**
 * Automatic trigger engine: watches connection state, device events and a periodic
 * scheduler tick, and runs the stored workflows whose [WorkflowTrigger] matches.
 *
 * - ON_CONNECT  — fires when the link transitions to [ConnectionState.Connected].
 * - ON_SIGNAL   — fires on RF/BLE signal events (per-workflow cooldown applies).
 * - SCHEDULED   — fires on every scheduler tick once the per-workflow minimum
 *                 interval has elapsed.
 *
 * A workflow is skipped (with an event) while another run is in progress.
 */
class AutopilotTriggerDispatcher(
    private val scope: CoroutineScope,
    private val connectionState: StateFlow<ConnectionState>,
    private val deviceEvents: SharedFlow<DeviceEvent>,
    private val workflowProvider: () -> List<Workflow>,
    private val engine: WorkflowEngine,
    private val schedulerTickMs: Long = DEFAULT_TICK_MS,
    private val scheduledMinIntervalMs: Long = DEFAULT_SCHEDULED_MIN_INTERVAL_MS,
    private val signalCooldownMs: Long = DEFAULT_SIGNAL_COOLDOWN_MS,
    private val clock: () -> Long = System::currentTimeMillis
) {

    private val _events = MutableSharedFlow<AutopilotTriggerEvent>(extraBufferCapacity = 32)
    val events: SharedFlow<AutopilotTriggerEvent> = _events.asSharedFlow()

    @Volatile
    private var enabled = false

    private var watchJob: Job? = null
    private var schedulerJob: Job? = null

    private val lastSignalRunMs = mutableMapOf<String, Long>()
    private val lastScheduledRunMs = mutableMapOf<String, Long>()

    fun start() {
        if (watchJob?.isActive == true) return
        enabled = true
        watchJob = scope.launch {
            launch { watchConnection() }
            launch { watchSignals() }
        }
        schedulerJob = scope.launch { runScheduler() }
        Log.i(TAG, "Trigger dispatcher started")
    }

    fun stop() {
        enabled = false
        watchJob?.cancel()
        schedulerJob?.cancel()
        watchJob = null
        schedulerJob = null
        Log.i(TAG, "Trigger dispatcher stopped")
    }

    fun isRunning(): Boolean = watchJob?.isActive == true

    private suspend fun watchConnection() {
        var wasConnected = connectionState.value is ConnectionState.Connected
        connectionState.collect { state ->
            val isConnected = state is ConnectionState.Connected
            if (isConnected && !wasConnected) {
                fireTriggers(WorkflowTrigger.ON_CONNECT)
            }
            wasConnected = isConnected
        }
    }

    private suspend fun watchSignals() {
        deviceEvents.collect { event ->
            if (!enabled) return@collect
            val isSignal = when (event) {
                is DeviceEvent.SubGhzSignal,
                is DeviceEvent.SubGhzSignalSaved,
                is DeviceEvent.SubGhzSample,
                is DeviceEvent.SubGhzDecodedFrame -> true
                else -> false
            }
            if (isSignal) {
                fireTriggers(WorkflowTrigger.ON_SIGNAL, cooldownMs = signalCooldownMs, cooldownStore = lastSignalRunMs)
            }
        }
    }

    private suspend fun runScheduler() {
        while (scope.isActive && enabled) {
            delay(schedulerTickMs)
            if (!enabled) break
            fireTriggers(
                WorkflowTrigger.SCHEDULED,
                cooldownMs = scheduledMinIntervalMs,
                cooldownStore = lastScheduledRunMs
            )
        }
    }

    private fun fireTriggers(
        trigger: WorkflowTrigger,
        cooldownMs: Long = 0L,
        cooldownStore: MutableMap<String, Long>? = null
    ) {
        if (!enabled) return
        val candidates = runCatching { workflowProvider() }
            .onFailure { Log.w(TAG, "workflowProvider failed: ${it.message}") }
            .getOrDefault(emptyList())
            .filter { it.trigger == trigger }

        for (workflow in candidates) {
            if (cooldownStore != null) {
                val last = cooldownStore[workflow.id] ?: 0L
                if (clock() - last < cooldownMs) {
                    _events.tryEmit(AutopilotTriggerEvent.Skipped(workflow.id, "cooldown"))
                    continue
                }
            }
            if (engine.runState.value == WorkflowRunState.RUNNING) {
                _events.tryEmit(AutopilotTriggerEvent.Skipped(workflow.id, "engine_busy"))
                continue
            }
            cooldownStore?.put(workflow.id, clock())
            _events.tryEmit(AutopilotTriggerEvent.Triggered(workflow.id, trigger))
            scope.launch {
                val result = runCatching { engine.run(workflow) }
                    .getOrElse { err ->
                        WorkflowRunResult(
                            workflowId = workflow.id,
                            completedSteps = 0,
                            totalSteps = workflow.steps.size,
                            success = false,
                            message = err.message ?: "trigger run failed"
                        )
                    }
                _events.tryEmit(AutopilotTriggerEvent.RunFinished(result))
            }
        }
    }

    companion object {
        private const val TAG = "AutopilotTriggers"
        const val DEFAULT_TICK_MS = 60_000L
        const val DEFAULT_SCHEDULED_MIN_INTERVAL_MS = 15 * 60_000L
        const val DEFAULT_SIGNAL_COOLDOWN_MS = 30_000L
    }
}
