package com.embedsuite.app.core.bruce

import java.util.UUID

/**
 * GATT Bruce oficial (ble_api / BLESerialService + BatteryService).
 * Referencia: BruceDevices/firmware src/modules/ble_api/
 */
object BruceGatt {
    /** Servicio serial — comandos texto (CLI Bruce), sin espejo de pantalla. */
    val SERIAL_SERVICE: UUID = UUID.fromString("4371ec0b-3d43-49f9-b731-7c72a4a7bb91")
    val SERIAL_CHAR: UUID = UUID.fromString("d555ed97-bf2a-4f46-b3eb-d1fcdd7325e9")

    /** Battery Service estándar BLE. */
    val BATTERY_SERVICE: UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
    val BATTERY_LEVEL: UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")

    val CCCD: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    /** Comandos BruceApp (solo control C2 — NO display start/render). */
    object Commands {
        const val REBOOT = "reboot"
        const val POWER_OFF = "poweroff"
    }
}
