package com.embedsuite.app.flash

import android.hardware.usb.UsbDevice

/**
 * IDs USB Espressif / LilyGO T-Embed CC1101 Plus (ESP32-S3).
 * Bootloader ROM, USB-Serial/JTAG y TinyUSB CDC usan distintos PID bajo VID 0x303A.
 */
object Esp32UsbIds {
    const val VENDOR_ESPRESSIF = 0x303A

    /** USB-Serial/JTAG (bootloader + runtime nativo S3). */
    const val PID_USB_JTAG = 0x1001

    /** USB-OTG CDC (algunos firmwares). */
    const val PID_USB_OTG = 0x0002

    /** Bootloader serie legacy. */
    const val PID_BOOTLOADER = 0x0009

    /** PID custom Bruce / LilyGO (CDC TEH-Link). */
    const val PID_BRUCE_CDC = 0x303A

    val KNOWN_PIDS = setOf(PID_USB_JTAG, PID_USB_OTG, PID_BOOTLOADER, PID_BRUCE_CDC)

    fun isEspressifDevice(device: UsbDevice): Boolean =
        device.vendorId == VENDOR_ESPRESSIF

    fun isLikelyTEmbed(device: UsbDevice): Boolean =
        isEspressifDevice(device) && (device.productId in KNOWN_PIDS || true)

    fun deviceScore(device: UsbDevice): Int = when {
        device.vendorId == VENDOR_ESPRESSIF && device.productId == PID_BRUCE_CDC -> 100
        device.vendorId == VENDOR_ESPRESSIF && device.productId == PID_USB_JTAG -> 90
        device.vendorId == VENDOR_ESPRESSIF && device.productId == PID_USB_OTG -> 80
        device.vendorId == VENDOR_ESPRESSIF && device.productId == PID_BOOTLOADER -> 85
        device.vendorId == VENDOR_ESPRESSIF -> 70
        else -> 0
    }

    fun pickBestDevice(devices: List<UsbDevice>): UsbDevice? =
        devices.maxByOrNull { deviceScore(it) }

    /** Para esptool ROM: priorizar USB-JTAG / bootloader sobre CDC TEH-Link en runtime. */
    fun flashDeviceScore(device: UsbDevice): Int = when {
        device.vendorId == VENDOR_ESPRESSIF && device.productId == PID_USB_JTAG -> 100
        device.vendorId == VENDOR_ESPRESSIF && device.productId == PID_BOOTLOADER -> 95
        device.vendorId == VENDOR_ESPRESSIF && device.productId == PID_USB_OTG -> 85
        device.vendorId == VENDOR_ESPRESSIF && device.productId == PID_BRUCE_CDC -> 40
        device.vendorId == VENDOR_ESPRESSIF -> 60
        else -> 0
    }

    fun pickFlashDevice(devices: List<UsbDevice>): UsbDevice? =
        devices.filter { isEspressifDevice(it) }.maxByOrNull { flashDeviceScore(it) }
            ?: devices.firstOrNull()
}
