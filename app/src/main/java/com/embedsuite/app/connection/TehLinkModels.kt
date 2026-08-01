package com.embedsuite.app.connection

data class TehLinkPluginInfo(
    val id: String,
    val name: String,
    val version: String,
    val author: String
)

data class TehLinkDeviceInfo(
    val product: String,
    val version: String,
    val codename: String,
    val channel: String,
    val proto: String,
    val protoVer: Int,
    val plugins: List<TehLinkPluginInfo>
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
    val batteryPct: Int? = null
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
    val ir: TehLinkIrResult? = null
)

data class TehLinkActionResult(
    val pluginId: String,
    val action: String,
    val state: TehLinkActionState
)

/** Chips rápidos TEH-Link para consola Xibalba. */
object TehLinkConsoleChips {
    data class Chip(val label: String, val json: String)

    val chips: List<Chip> = listOf(
        Chip("ping", """{"cmd":"ping","id":1}"""),
        Chip("get_info", """{"cmd":"get_info","id":2}"""),
        Chip("get_status", """{"cmd":"get_status","id":3}"""),
        Chip("back_to_menu", """{"cmd":"back_to_menu","id":4}"""),
        Chip("list_actions", """{"cmd":"list_actions","id":5}"""),
        Chip("subghz_tx", """{"cmd":"run_action","id":6,"plugin_id":"subghz_analyzer","action":"subghz_tx","params":{"raw":"AABBCC","confirm":true}}"""),
        Chip("ir_rx", """{"cmd":"run_action","id":7,"plugin_id":"ir_toolkit","action":"rx_start","params":{"seconds":10}}"""),
        Chip("nfc_read", """{"cmd":"run_action","id":8,"plugin_id":"nfc_toolkit","action":"read"}"""),
        Chip("nrf24", """{"cmd":"run_action","id":9,"plugin_id":"nrf24_toolkit","action":"status"}""")
    )
}
