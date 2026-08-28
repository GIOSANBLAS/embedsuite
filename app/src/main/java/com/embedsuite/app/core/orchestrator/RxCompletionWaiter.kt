package com.embedsuite.app.core.orchestrator

import com.embedsuite.app.connection.DeviceConnectionManager
import com.embedsuite.app.connection.DeviceEvent
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/** Espera fin de captura RX vía eventos BLE/USB (no solo delay fijo). */
object RxCompletionWaiter {

    suspend fun waitSubGhzRx(
        connectionManager: DeviceConnectionManager,
        timeoutMs: Long
    ): Boolean = withTimeoutOrNull(timeoutMs) {
        connectionManager.events.filter { event ->
            when (event) {
                is DeviceEvent.SubGhzSignalSaved -> true
                is DeviceEvent.SubGhzSignal -> true
                is DeviceEvent.RawLine -> {
                    val l = event.line.lowercase()
                    l.contains("rx") && (l.contains("done") || l.contains("finish") || l.contains("complete") || l.contains("saved"))
                }
                else -> false
            }
        }.first()
        true
    } ?: false

    suspend fun waitIrRx(
        connectionManager: DeviceConnectionManager,
        timeoutMs: Long
    ): Boolean = withTimeoutOrNull(timeoutMs) {
        connectionManager.events.filter { event ->
            when (event) {
                is DeviceEvent.RawLine -> {
                    val l = event.line.lowercase()
                    l.contains("protocol:") || l.contains("ir tx") ||
                        (l.contains("rx") && (l.contains("done") || l.contains("finish")))
                }
                else -> false
            }
        }.first()
        true
    } ?: false
}
