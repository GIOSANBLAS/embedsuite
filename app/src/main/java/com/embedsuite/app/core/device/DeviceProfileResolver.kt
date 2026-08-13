package com.embedsuite.app.core.device

import com.embedsuite.app.connection.TehLinkDeviceInfo
import java.util.Locale
import java.util.UUID

object DeviceProfileResolver {

    private val capabilityKeyMap = mapOf(
        "wifi" to DeviceCapability.WIFI,
        "ble" to DeviceCapability.BLE,
        "bluetooth" to DeviceCapability.BLE,
        "subghz" to DeviceCapability.SUBGHZ_CC1101,
        "sub_ghz" to DeviceCapability.SUBGHZ_CC1101,
        "cc1101" to DeviceCapability.SUBGHZ_CC1101,
        "nrf24" to DeviceCapability.NRF24,
        "nrf24l01" to DeviceCapability.NRF24,
        "nfc" to DeviceCapability.NFC,
        "ir" to DeviceCapability.IR,
        "badusb" to DeviceCapability.BADUSB,
        "rfid" to DeviceCapability.RFID,
        "gps" to DeviceCapability.GPS,
        "sd" to DeviceCapability.SD,
        "sdcard" to DeviceCapability.SD
    )

    fun resolve(
        deviceInfo: TehLinkDeviceInfo,
        capabilities: Map<String, Boolean> = emptyMap(),
        sdMounted: Boolean? = null
    ): DeviceProfile {
        val hardwareKind = detectHardwareKind(deviceInfo)
        val caps = resolveCapabilities(deviceInfo, capabilities, sdMounted, hardwareKind)
        val stableId = buildStableId(deviceInfo, hardwareKind)
        return DeviceProfile(
            id = stableId,
            name = deviceInfo.product.ifBlank { deviceInfo.codename }.ifBlank { "TEH-Link device" },
            hardwareKind = hardwareKind,
            capabilities = caps,
            firmwareVersion = deviceInfo.version,
            productName = deviceInfo.product,
            lastSeenMs = System.currentTimeMillis(),
            notes = "proto=${deviceInfo.proto} v${deviceInfo.protoVer} channel=${deviceInfo.channel}"
        )
    }

    fun detectHardwareKind(deviceInfo: TehLinkDeviceInfo): DeviceHardwareKind {
        val blob = listOf(
            deviceInfo.hardware,
            deviceInfo.firmware,
            deviceInfo.product,
            deviceInfo.codename,
            deviceInfo.channel
        ).joinToString(" ").lowercase(Locale.US)

        return when {
            blob.contains("t-embed") || blob.contains("t_embed") || blob.contains("cc1101") ||
                blob.contains("c1101") || blob.contains("xibalba") || blob.contains("lilygo") ->
                DeviceHardwareKind.T_EMBED_CC1101
            blob.contains("esp32-s3") || blob.contains("esp32s3") -> DeviceHardwareKind.ESP32_S3_GENERIC
            blob.contains("custom") -> DeviceHardwareKind.CUSTOM
            else -> DeviceHardwareKind.UNKNOWN
        }
    }

    fun resolveCapabilities(
        deviceInfo: TehLinkDeviceInfo,
        capabilities: Map<String, Boolean>,
        sdMounted: Boolean?,
        hardwareKind: DeviceHardwareKind = detectHardwareKind(deviceInfo)
    ): Set<DeviceCapability> {
        val resolved = linkedSetOf<DeviceCapability>()

        capabilities.forEach { (key, enabled) ->
            if (!enabled) return@forEach
            capabilityKeyMap[key.lowercase(Locale.US)]?.let { resolved += it }
        }

        deviceInfo.plugins.forEach { plugin ->
            val pid = plugin.id.lowercase(Locale.US)
            when {
                pid.contains("wifi") || pid.contains("wardriv") -> resolved += DeviceCapability.WIFI
                pid.contains("ble") -> resolved += DeviceCapability.BLE
                pid.contains("subghz") || pid.contains("cc1101") -> resolved += DeviceCapability.SUBGHZ_CC1101
                pid.contains("nrf24") || pid.contains("mousejack") -> resolved += DeviceCapability.NRF24
                pid.contains("nfc") -> resolved += DeviceCapability.NFC
                pid.contains("ir") -> resolved += DeviceCapability.IR
                pid.contains("badusb") || pid.contains("ducky") -> resolved += DeviceCapability.BADUSB
                pid.contains("rfid") -> resolved += DeviceCapability.RFID
                pid.contains("gps") -> resolved += DeviceCapability.GPS
            }
        }

        if (sdMounted == true) resolved += DeviceCapability.SD

        if (resolved.isEmpty() && hardwareKind == DeviceHardwareKind.T_EMBED_CC1101) {
            resolved += setOf(
                DeviceCapability.WIFI,
                DeviceCapability.BLE,
                DeviceCapability.SUBGHZ_CC1101,
                DeviceCapability.NRF24,
                DeviceCapability.NFC,
                DeviceCapability.IR,
                DeviceCapability.BADUSB,
                DeviceCapability.RFID,
                DeviceCapability.GPS,
                DeviceCapability.SD
            )
        }

        return resolved
    }

    private fun buildStableId(deviceInfo: TehLinkDeviceInfo, kind: DeviceHardwareKind): String {
        val seed = listOf(kind.name, deviceInfo.product, deviceInfo.codename, deviceInfo.channel)
            .joinToString("|")
            .ifBlank { UUID.randomUUID().toString() }
        return seed.hashCode().toUInt().toString(16)
    }
}
