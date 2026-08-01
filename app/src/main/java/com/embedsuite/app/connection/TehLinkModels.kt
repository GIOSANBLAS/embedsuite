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

data class TehLinkDeviceStatus(
    val sdMounted: Boolean,
    val flashMounted: Boolean,
    val uiScreen: String,
    val uptimeMs: Long,
    val sim: Map<String, Boolean>
)
