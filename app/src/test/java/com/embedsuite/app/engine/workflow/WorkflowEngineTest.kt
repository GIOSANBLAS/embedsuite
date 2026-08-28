package com.embedsuite.app.engine.workflow

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkflowEngineTest {

    private val runner = mockk<WorkflowActionRunner>()
    private val engine = SequentialWorkflowEngine(runner)

    @Test
    fun emptyWorkflowCompletes() = runTest {
        val wf = Workflow(id = "empty", name = "Empty")
        val result = engine.run(wf)
        assertTrue(result.success)
        assertEquals(0, result.completedSteps)
    }

    @Test
    fun actionStepsRunInOrder() = runTest {
        every { runner.isConnected() } returns true
        coEvery { runner.runAction(any(), any(), any()) } returns Result.success(Unit)

        val wf = Workflow(
            id = "seq",
            name = "Seq",
            steps = listOf(
                WorkflowStep.Action("s1", "A", "wifi_toolkit", "scan_start", mapOf("seconds" to "5")),
                WorkflowStep.Delay("s2", "Wait", 10L),
                WorkflowStep.Action("s3", "B", "ble_toolkit", "scan_start", emptyMap())
            )
        )
        val result = engine.run(wf)
        assertTrue(result.success)
        assertEquals(3, result.completedSteps)
        coVerify(exactly = 2) { runner.runAction(any(), any(), any()) }
    }

    @Test
    fun conditionBranchingFollowsOnTrue() = runTest {
        every { runner.isConnected() } returns true
        coEvery { runner.runAction(any(), any(), any()) } returns Result.success(Unit)

        val wf = Workflow(
            id = "cond",
            name = "Cond",
            steps = listOf(
                WorkflowStep.Condition("c1", "Check", "connected", onTrueStepId = "s2", onFalseStepId = "s3"),
                WorkflowStep.Action("s2", "True path", "wifi_toolkit", "status", emptyMap()),
                WorkflowStep.Action("s3", "False path", "ble_toolkit", "status", emptyMap())
            )
        )
        val result = engine.run(wf)
        assertTrue(result.success)
        coVerify { runner.runAction("wifi_toolkit", "status", any()) }
    }

    @Test
    fun loopDetectionFails() = runTest {
        every { runner.isConnected() } returns false

        val wf = Workflow(
            id = "loop",
            name = "Loop",
            steps = listOf(
                WorkflowStep.Condition("c1", "Loop", "always_false", onFalseStepId = "c1")
            )
        )
        val result = engine.run(wf)
        assertFalse(result.success)
        assertTrue(result.message.contains("Loop"))
    }

    @Test
    fun serializeDeserializeRoundTrip() {
        val wf = Workflow(
            id = "rt",
            name = "Roundtrip",
            trigger = WorkflowTrigger.ON_CONNECT,
            steps = listOf(
                WorkflowStep.Action("a1", "Ping", "diagnostic_tools", "status", mapOf("x" to "1"))
            )
        )
        val raw = engine.serialize(wf)
        val parsed = engine.deserialize(raw)
        assertEquals(wf.id, parsed?.id)
        assertEquals(wf.trigger, parsed?.trigger)
        assertEquals(1, parsed?.steps?.size)
    }

    @Test
    fun cancelStopsRun() = runTest {
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        coEvery { runner.runAction(any(), any(), any()) } coAnswers {
            gate.await()
            Result.success(Unit)
        }
        val wf = Workflow(
            id = "cancel",
            name = "Cancel",
            steps = listOf(
                WorkflowStep.Action("s0", "S0", "wifi_toolkit", "status", emptyMap())
            )
        )
        val job = launch { engine.run(wf) }
        delay(50)
        engine.cancel()
        gate.complete(Unit)
        job.join()
        assertEquals(WorkflowRunState.IDLE, engine.runState.value)
    }
}
