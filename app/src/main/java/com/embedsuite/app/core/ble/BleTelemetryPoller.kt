package com.embedsuite.app.core.ble

import com.embedsuite.app.connection.ConnectionState
import com.embedsuite.app.connection.DeviceConnectionManager
import com.embedsuite.app.connection.TransportType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Polling periódico de telemetría (batería, temp, SD) mientras hay enlace activo.
 * Prioriza BLE pero funciona en cualquier transporte conectado.
 */
class BleTelemetryPoller(
    private val connectionManager: DeviceConnectionManager,
    private val scope: CoroutineScope,
    private val intervalMs: Long = 15_000L
) {
    fun start() {
        scope.launch {
            connectionManager.connectionState
                .collect { state ->
                    if (state !is ConnectionState.Connected) return@collect
                    while (isActive && connectionManager.connectionState.value is ConnectionState.Connected) {
                        runCatching { connectionManager.refreshSystemInfo() }
                        delay(intervalMs)
                    }
                }
        }
    }

    fun snapshotFromSystemInfo(): BleTelemetrySnapshot {
        val info = connectionManager.systemInfo.value
        val transport = connectionManager.activeTransportType.value.name
        val pct = info.battery.filter { it.isDigit() }.take(3).toIntOrNull() ?: 0
        return BleTelemetrySnapshot(
            batteryPercent = pct,
            batteryVoltage = 0.0,
            product = info.codename.ifBlank { info.channel },
            firmware = info.firmware,
            sdStatus = info.sdMounted,
            transportLabel = transport,
            updatedAtMs = System.currentTimeMillis()
        )
    }

    fun isBleActive(): Boolean =
        connectionManager.connectionState.value is ConnectionState.Connected &&
            connectionManager.activeTransportType.value == TransportType.BLE
}
