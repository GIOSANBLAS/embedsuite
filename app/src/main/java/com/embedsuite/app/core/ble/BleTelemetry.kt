package com.embedsuite.app.core.ble

import com.embedsuite.app.connection.TehLinkDeviceInfo

/** Telemetría en tiempo real del T-Embed (Módulo A). */
data class BleTelemetrySnapshot(
    val batteryPercent: Int = 0,
    val batteryVoltage: Double = 0.0,
    val product: String = "",
    val firmware: String = "",
    val sdStatus: String = "",
    val transportLabel: String = "BLE",
    val updatedAtMs: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromDeviceInfo(info: TehLinkDeviceInfo, transport: String = "BLE"): BleTelemetrySnapshot =
            BleTelemetrySnapshot(
                batteryPercent = info.battery?.percentage ?: 0,
                batteryVoltage = info.battery?.voltage ?: 0.0,
                product = info.product.ifBlank { info.hardware },
                firmware = info.firmware.ifBlank { info.version },
                sdStatus = info.sdStatus,
                transportLabel = transport
            )
    }
}
