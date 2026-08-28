package com.embedsuite.app.connection

import com.embedsuite.app.core.bruce.BruceLimits

/**
 * Política de acciones remotas — solo lo que Bruce stock expone por CLI serial.
 */
object TehLinkActionPolicy {

    /** Plugins sin CLI en Bruce stock — bloqueados desde la app. */
    private val NO_CLI_PLUGINS = setOf(
        "evil_portal",
        "beacon_spam",
        "ble_ad_spam",
        "wardriving",
        "wifi_toolkit",
        "ble_toolkit",
        "crypto_toolkit",
        "nrf24_toolkit",
        "diagnostic_tools"
    )

    fun validate(pluginId: String, action: String): Result<Unit> {
        if (pluginId in NO_CLI_PLUGINS) {
            return Result.failure(IllegalArgumentException(BruceLimits.NO_CLI))
        }
        return Result.success(Unit)
    }
}
