package com.embedsuite.app.engine.workflow

import org.json.JSONArray
import org.json.JSONObject

interface WorkflowEngine {
    fun serialize(workflow: Workflow): String
    fun deserialize(raw: String): Workflow?
    suspend fun run(workflow: Workflow): WorkflowRunResult
}

data class WorkflowRunResult(
    val workflowId: String,
    val completedSteps: Int,
    val totalSteps: Int,
    val success: Boolean,
    val message: String = ""
)

class SequentialWorkflowEngine : WorkflowEngine {

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
        // Stub: sequential no-op pass-through until TEH-Link action runner is wired.
        return WorkflowRunResult(
            workflowId = workflow.id,
            completedSteps = workflow.steps.size,
            totalSteps = workflow.steps.size,
            success = true,
            message = "Sequential stub completed"
        )
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
