package com.embedsuite.app.connection

import org.json.JSONObject

data class TehLinkPluginInfo(
    val id: String,
    val name: String,
    val version: String,
    val author: String
)

data class TehLinkBatteryInfo(
    val voltage: Double = 0.0,
    val percentage: Int = 0
)

data class TehLinkDeviceInfo(
    val product: String,
    val version: String,
    val codename: String,
    val channel: String,
    val proto: String,
    val protoVer: Int,
    val plugins: List<TehLinkPluginInfo>,
    val hardening: TehLinkHardeningInfo = TehLinkHardeningInfo(),
    val hardware: String = "",
    val firmware: String = "",
    val battery: TehLinkBatteryInfo? = null,
    val sdStatus: String = "",
    val capabilityList: List<String> = emptyList()
)

/** Flags de seguridad / hardening reportados por Bruce v0.17+. */
data class TehLinkHardeningInfo(
    val twdtEnabled: Boolean = false,
    val twdtTimeoutSeconds: Int = 0,
    val bodEnabled: Boolean = false,
    val bodVoltage: Float? = null,
    val secureBoot: Boolean = false,
    val flashEncryption: Boolean = false,
    val nvsEncryption: Boolean = false,
    val stackCanaries: Boolean = false,
    val heapPoisoning: Boolean = false
)

data class TehLinkScreenInfo(
    val uiScreen: String,
    val activePlugin: String,
    val openedPluginId: String = ""
)

data class TehLinkDeviceStatus(
    val sdMounted: Boolean,
    val flashMounted: Boolean,
    val uiScreen: String,
    val uptimeMs: Long,
    val sim: Map<String, Boolean>,
    val capabilities: Map<String, Boolean> = emptyMap(),
    val batteryPct: Int? = null,
    val chargeStatus: String? = null,
    val charging: Boolean? = null,
    val vbusPresent: Boolean? = null,
    val heapFreeBytes: Long? = null,
    val psramFreeBytes: Long? = null,
    val sdFreeBytes: Long? = null,
    val coredumpPresent: Boolean = false,
    val wdtPanicReason: String? = null,
    val temperatureC: Float? = null
)

data class TehLinkActionInfo(
    val pluginId: String,
    val action: String,
    val params: List<String> = emptyList()
)

data class TehLinkWifiAp(
    val ssid: String,
    val bssid: String,
    val channel: Int,
    val rssi: Int,
    val security: String = ""
)

data class TehLinkBleDevice(
    val name: String,
    val address: String,
    val rssi: Int,
    val isTracker: Boolean = false
)

data class TehLinkWardrivingStatus(
    val running: Boolean = false,
    val apCount: Int = 0,
    val csvPath: String = "",
    val csvBasename: String = ""
)

data class TehLinkCryptoResult(
    val digest: String = "",
    val result: String = "",
    val algo: String = ""
)

data class TehLinkNfcResult(
    val uid: String = "",
    val sak: Int = 0,
    val ready: Boolean = false
)

data class TehLinkIrResult(
    val ready: Boolean = false,
    val message: String = "",
    val raw: String = "",
    val protocol: String = ""
)

/** Estado de OTA expandido: incluye sha256_verified introducido en Bruce 0.17.1+. */
data class TehLinkOtaStatus(
    val state: String = "idle",
    val bytesWritten: Long = 0,
    val totalSize: Long = 0,
    val sha256Verified: Boolean = false
) {
    val progressPct: Int
        get() {
            if (totalSize <= 0) return 0
            return ((bytesWritten * 100L + totalSize / 2) / totalSize).coerceIn(0L, 100L).toInt()
        }
    val isComplete: Boolean get() = state.equals("complete", true) || state.equals("verified", true)
    val hasError: Boolean get() = state.equals("error", true) || state.equals("mismatch", true)
}

/** Estado Evil Portal (Bruce v0.19+). */
data class TehLinkEvilPortalStatus(
    val running: Boolean = false,
    val ssid: String = "",
    val templateId: String = "",
    val channel: Int = 1,
    val credentialCount: Int = 0,
    val clientCount: Int = 0
) {
    companion object {
        fun fromJson(data: JSONObject): TehLinkEvilPortalStatus {
            return TehLinkEvilPortalStatus(
                running = data.optBoolean("running"),
                ssid = data.optString("ssid"),
                templateId = data.optString("template_id").ifBlank {
                    data.optString("template")
                },
                channel = data.optInt("channel", 1),
                credentialCount = data.optInt("credential_count", data.optInt("count")),
                clientCount = data.optInt("client_count", 0)
            )
        }
    }
}

/** Estado Beacon Spam (Bruce v0.19+). */
data class TehLinkBeaconSpamStatus(
    val running: Boolean = false,
    val spec: String = "",
    val hz: Int = 10,
    val channel: Int = 0,
    val sentCount: Int = 0
) {
    companion object {
        fun fromJson(data: JSONObject): TehLinkBeaconSpamStatus {
            return TehLinkBeaconSpamStatus(
                running = data.optBoolean("running"),
                spec = data.optString("spec"),
                hz = data.optInt("hz", 10),
                channel = data.optInt("channel", 0),
                sentCount = data.optInt("sent_count", data.optInt("sent"))
            )
        }
    }
}

data class TehLinkActionState(
    val pluginId: String,
    val action: String = "",
    val state: String = "",
    val progress: Int = 0,
    val message: String = "",
    val loadedPath: String = "",
    val running: Boolean = false,
    val capturing: Boolean = false,
    val packets: Int = 0,
    val secondsRemaining: Int = 0,
    val aps: List<TehLinkWifiAp> = emptyList(),
    val devices: List<TehLinkBleDevice> = emptyList(),
    val wardriving: TehLinkWardrivingStatus? = null,
    val crypto: TehLinkCryptoResult? = null,
    val nfc: TehLinkNfcResult? = null,
    val ir: TehLinkIrResult? = null,
    val ota: TehLinkOtaStatus? = null,
    val soak: TehLinkSoakResult? = null,
    /** Estado Evil Portal (Bruce v0.19+). */
    val evilPortal: TehLinkEvilPortalStatus? = null,
    /** Estado Beacon Spam (Bruce v0.19+). */
    val beaconSpam: TehLinkBeaconSpamStatus? = null
)

data class TehLinkActionResult(
    val pluginId: String,
    val action: String,
    val state: TehLinkActionState,
    val rawResponse: JSONObject? = null
) {
    /** Helper para acceder rápido al estado Evil Portal (Bruce v0.19+). */
    val evilPortalStatus: TehLinkEvilPortalStatus?
        get() = state.evilPortal
    
    /** Helper para acceder rápido al estado Beacon Spam (Bruce v0.19+). */
    val beaconSpamStatus: TehLinkBeaconSpamStatus?
        get() = state.beaconSpam
}

/** Resultado de un soak test stress: detección de memory leaks / cuelgues. */
data class TehLinkSoakResult(
    val iterations: Int = 0,
    val failures: Int = 0,
    val heapFreeBefore: Long = 0,
    val heapFreeAfter: Long = 0,
    val leakBytes: Long = 0,
    val completed: Boolean = false
) {
    val isHealthy: Boolean
        get() = completed && failures == 0 && leakBytes < 4_096L
}

/** Chips rápidos TEH-Link para consola Bruce. */
object TehLinkConsoleChips {
    data class Chip(val label: String, val json: String)

    val chips: List<Chip> = listOf(
        Chip("ping", """{"cmd":"ping","id":1}"""),
        Chip("get_info", """{"cmd":"get_info","id":2}"""),
        Chip("get_status", """{"cmd":"get_status","id":3}"""),
        Chip("get_screen", """{"cmd":"get_screen","id":4}"""),
        Chip("list_actions", """{"cmd":"list_actions","id":5}"""),
        Chip("ota_status", """{"cmd":"ota_status","id":6}"""),
        Chip("get_action_state", """{"cmd":"get_action_state","id":7,"plugin_id":"subghz_analyzer"}"""),
        Chip("back_to_menu", """{"cmd":"back_to_menu","id":8}"""),
        Chip("evil_portal_status", """{"cmd":"get_action_state","id":9,"plugin_id":"evil_portal"}"""),
        Chip("beacon_spam_status", """{"cmd":"get_action_state","id":10,"plugin_id":"beacon_spam"}""")
    )
}