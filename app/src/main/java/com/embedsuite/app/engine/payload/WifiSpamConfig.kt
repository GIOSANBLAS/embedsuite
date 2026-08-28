package com.embedsuite.app.engine.payload

import com.embedsuite.app.core.bruce.BruceLimits

data class WifiSpamConfig(
    val ssid: String = "Free_WiFi",
    val channel: Int = 6,
    val beaconIntervalMs: Int = 100
)

data class BleSpamConfig(
    val spec: String = "apple_juice",
    val intervalMs: Int = 200
)

/** Plantillas — solo CLI Bruce documentado; resto bloqueado. */
object PayloadTemplates {

    fun badUsbFilePath(scriptName: String = "payload.txt"): String {
        val name = scriptName.trim().let { if (it.endsWith(".txt", true)) it else "$it.txt" }
        return "/badusb/$name"
    }

    @Deprecated("Evil Portal no tiene CLI en Bruce stock")
    fun evilPortalTehLink(ssid: String, templateId: String = "default"): String {
        throw UnsupportedOperationException(BruceLimits.NO_CLI)
    }

    @Deprecated("BLE Spam no tiene CLI en Bruce stock")
    fun bleSpamTehLink(config: BleSpamConfig): String {
        throw UnsupportedOperationException(BruceLimits.NO_CLI)
    }

    @Deprecated("TEH-Link eliminado")
    fun writeRfidTehLink(uid: String, type: String = "EM4100", blocks: List<String> = emptyList()): String {
        throw UnsupportedOperationException("Usa rfid read / rfid write vía terminal Bruce.")
    }
}
