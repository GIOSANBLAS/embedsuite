package com.embedsuite.app.connection

enum class TransportType {
    USB,
    WIFI,
    BLE
}

sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data object Connecting : ConnectionState()
    data class Connected(val type: TransportType, val detail: String) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

data class SignalEntry(
    val timestamp: String,
    val frequency: String,
    val deviceId: String,
    val protocol: String,
    val power: String,
    val rawData: String = ""
)

data class SystemInfo(
    val uptime: String = "",
    val freeHeap: String = "",
    val battery: String = "",
    val firmware: String = "",
    val codename: String = "",
    val channel: String = "",
    val uiScreen: String = "",
    val sdMounted: String = "",
    val profile: FirmwareProfile = FirmwareProfile.UNKNOWN,
    val xibalbaPlugins: List<TehLinkPluginInfo> = emptyList(),
    /** Subsistemas en modo simulación (TEH-Link get_status.sim). Vacío = desconocido. */
    val simFlags: Map<String, Boolean> = emptyMap(),
    /** Capabilities reportadas por TEH-Link get_status.capabilities. */
    val xibalbaCapabilities: Map<String, Boolean> = emptyMap()
)

sealed class DeviceEvent {
    data class RawLine(val line: String) : DeviceEvent()
    data class SubGhzSignal(val entry: SignalEntry) : DeviceEvent()
    data class SubGhzSignalSaved(val entry: SignalEntry, val signalId: Long) : DeviceEvent()
    data class WaveformSample(val level: Float, val durationUs: Long) : DeviceEvent()
    data class SystemInfoUpdate(val info: SystemInfo) : DeviceEvent()
    /** Avisos TEH-Link (pairing, auth). */
    data class TehLinkNotice(val message: String) : DeviceEvent()
}
