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
    val sim: Map<String, Boolean>
)

/** Chips rápidos TEH-Link para consola Xibalba. */
object TehLinkConsoleChips {
    data class Chip(val label: String, val json: String)

    val chips: List<Chip> = listOf(
        Chip("ping", """{"cmd":"ping","id":1}"""),
        Chip("get_info", """{"cmd":"get_info","id":2}"""),
        Chip("get_status", """{"cmd":"get_status","id":3}"""),
        Chip("back_to_menu", """{"cmd":"back_to_menu","id":4}""")
    )
}
