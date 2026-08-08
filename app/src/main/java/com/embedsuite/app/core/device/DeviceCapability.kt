package com.embedsuite.app.core.device

/**
 * Hardware / firmware capabilities reported by TEH-Link or inferred from device profile.
 */
enum class DeviceCapability {
    WIFI,
    BLE,
    SUBGHZ_CC1101,
    NRF24,
    NFC,
    IR,
    BADUSB,
    RFID,
    GPS,
    SD
}
