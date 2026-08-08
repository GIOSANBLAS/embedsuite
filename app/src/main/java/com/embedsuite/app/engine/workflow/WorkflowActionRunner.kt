package com.embedsuite.app.engine.workflow

import com.embedsuite.app.connection.ConnectionState
import com.embedsuite.app.connection.DeviceConnectionManager
import com.embedsuite.app.connection.FirmwareProfile
import org.json.JSONObject

/**
 * Narrow port for workflow execution — avoids pulling the full connection manager into tests
 * and keeps the workflow package free of circular imports.
 */
interface WorkflowActionRunner {
    fun isConnected(): Boolean
    fun isXibalbaProfile(): Boolean
    suspend fun runAction(
        pluginId: String,
        action: String,
        params: Map<String, String>
    ): Result<Unit>
    suspend fun bleDeviceCount(): Int?
}

class DeviceConnectionWorkflowRunner(
    private val connectionManager: DeviceConnectionManager
) : WorkflowActionRunner {

    override fun isConnected(): Boolean =
        connectionManager.connectionState.value is ConnectionState.Connected

    override fun isXibalbaProfile(): Boolean =
        connectionManager.detectedProfile.value == FirmwareProfile.XIBALBA

    override suspend fun runAction(
        pluginId: String,
        action: String,
        params: Map<String, String>
    ): Result<Unit> {
        val jsonParams = JSONObject()
        params.forEach { (k, v) -> jsonParams.put(k, v) }
        return connectionManager.tehLinkRunAction(pluginId, action, jsonParams)
            .map { }
    }

    override suspend fun bleDeviceCount(): Int? {
        val fromStatus = connectionManager.tehLinkGetActionState("ble_toolkit")
            .getOrNull()
            ?.devices
            ?.size
        if (fromStatus != null) return fromStatus
        return null
    }
}
