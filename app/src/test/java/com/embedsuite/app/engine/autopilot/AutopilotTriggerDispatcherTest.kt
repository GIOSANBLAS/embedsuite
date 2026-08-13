package com.embedsuite.app.engine.autopilot

import com.embedsuite.app.connection.ConnectionState
import com.embedsuite.app.connection.DeviceEvent
import com.embedsuite.app.connection.TransportType
import com.embedsuite.app.engine.workflow.SequentialWorkflowEngine
import com.embedsuite.app.engine.workflow.Workflow
import com.embedsuite.app.engine.workflow.WorkflowActionRunner
import com.embedsuite.app.engine.workflow.WorkflowEngine
import com.embedsuite.app.engine.workflow.WorkflowRunEvent
import com.embedsuite.app.engine.workflow.WorkflowRunResult
import com.embedsuite.app.engine.workflow.WorkflowRunState
import com.embedsuite.app.engine.workflow.WorkflowStep
import com.embedsuite.app.engine.workflow.WorkflowTrigger
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AutopilotTriggerDispatcherTest {

    private val runner = mockk<WorkflowActionRunner>(relaxed = true)

    @Test
    fun onConnectFiresMatchingWorkflow() = runTest {
        coEvery { runner.runAction(any(), any(), any()) } returns Result.success(Unit)
        val engine = SequentialWorkflowEngine(runner)
        val conn = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
        val events = MutableSharedFlow<DeviceEvent>(extraBufferCapacity = 8)
        val wf = Workflow(
            id = "auto-connect",
            name = "Auto",
            trigger = WorkflowTrigger.ON_CONNECT,
            steps = listOf(
                WorkflowStep.Action("a1", "Ping", "wifi_toolkit", "status", emptyMap())
            )
        )

        val dispatcher = AutopilotTriggerDispatcher(
            scope = backgroundScope,
            connectionState = conn,
            deviceEvents = events,
            workflowProvider = { listOf(wf) },
            engine = engine
        )
        dispatcher.start()
        conn.value = ConnectionState.Connected(TransportType.USB, "usb")
        advanceUntilIdle()
        dispatcher.stop()
        assertEquals(WorkflowRunState.IDLE, engine.runState.value)
    }

    @Test
    fun skipsWhenEngineBusy() = runTest {
        val busyEngine = BusyWorkflowEngine()
        val conn = MutableStateFlow<ConnectionState>(
            ConnectionState.Connected(TransportType.USB, "usb")
        )
        val events = MutableSharedFlow<DeviceEvent>(extraBufferCapacity = 8)

        val dispatcher = AutopilotTriggerDispatcher(
            scope = backgroundScope,
            connectionState = conn,
            deviceEvents = events,
            workflowProvider = {
                listOf(Workflow(id = "x", name = "X", trigger = WorkflowTrigger.SCHEDULED))
            },
            engine = busyEngine,
            schedulerTickMs = 50L,
            scheduledMinIntervalMs = 0L
        )
        var skipped = false
        val collectJob = launch {
            dispatcher.events.collect { ev ->
                if (ev is AutopilotTriggerEvent.Skipped) skipped = true
            }
        }
        dispatcher.start()
        advanceTimeBy(200)
        advanceUntilIdle()
        dispatcher.stop()
        advanceUntilIdle()
        collectJob.cancel()
        assertTrue(skipped)
    }

    /** Minimal engine stub that always reports RUNNING. */
    private class BusyWorkflowEngine : WorkflowEngine {
        override val runState: StateFlow<WorkflowRunState> =
            MutableStateFlow(WorkflowRunState.RUNNING)
        override val runEvents: SharedFlow<WorkflowRunEvent> =
            MutableSharedFlow(extraBufferCapacity = 8)
        override val activeWorkflowId: StateFlow<String?> = MutableStateFlow("busy")

        override fun cancel() = Unit
        override fun serialize(workflow: Workflow): String = workflow.id
        override fun deserialize(raw: String): Workflow? = null
        override suspend fun run(workflow: Workflow): WorkflowRunResult =
            WorkflowRunResult(workflow.id, 0, 0, false, "busy")
    }
}
