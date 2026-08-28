package com.embedsuite.app.core.orchestrator

import android.content.Context
import com.embedsuite.app.core.bruce.BruceLimits
import org.json.JSONObject

/**
 * MÓDULO D — Perfiles spam/beacon.
 * Bruce stock NO expone Evil Portal / BLE spam por CLI → solo export local + aviso.
 */
object SpamIntent {

    enum class BleSpamProfile {
        APPLE, SAMSUNG, GOOGLE, MICROSOFT, GENERIC
    }

    enum class WifiSpamProfile {
        BEACON, PROBE, DEAUTH, CUSTOM
    }

    data class Config(
        val bleProfile: BleSpamProfile = BleSpamProfile.GENERIC,
        val wifiProfile: WifiSpamProfile = WifiSpamProfile.BEACON,
        val ssid: String = "Free_WiFi",
        val channel: Int = 6,
        val note: String = ""
    )

    /** Genera payload JSON local — NO ejecuta CLI remota. */
    fun buildLocalExport(config: Config): LocalExportIntent = LocalExportIntent(
        label = "Beacon Template",
        exportFileName = "beacon_${System.currentTimeMillis()}.json",
        buildPayload = { _ ->
            runCatching {
                JSONObject()
                    .put("ble_profile", config.bleProfile.name)
                    .put("wifi_profile", config.wifiProfile.name)
                    .put("ssid", config.ssid)
                    .put("channel", config.channel.coerceIn(1, 13))
                    .put("note", config.note)
                    .put("remote_execution", false)
                    .put("hint", BruceLimits.NO_CLI)
                    .put("menu_hint", "Configura Evil Portal / BLE Spam en el menú del T-Embed")
                    .toString(2)
            }
        }
    )

    /**
     * Bruce stock no documenta CLI segura para spam remoto.
     * Devuelve null — la UI debe mostrar [BruceLimits.NO_CLI].
     */
    fun buildRemoteCommand(config: Config): Result<String> = Result.failure(
        UnsupportedOperationException(
            "${BruceLimits.NO_CLI} Perfil=${config.bleProfile}/${config.wifiProfile}"
        )
    )
}
