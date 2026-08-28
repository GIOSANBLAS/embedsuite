package com.embedsuite.app.core.connection

import com.embedsuite.app.connection.TransportType

/** Estado de cada canal de comunicación (skill §5). */
data class TransportAvailability(
    val usbAvailable: Boolean = false,
    val bleAvailable: Boolean = false,
    val wifiAvailable: Boolean = false,
    val active: TransportType? = null
) {
    fun isActive(type: TransportType): Boolean = active == type
}
