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
    val xibalbaPlugins: List<TehLinkPluginInfo> = emptyList()
)

sealed class BruceEvent {
    data class RawLine(val line: String) : BruceEvent()
    data class SubGhzSignal(val entry: SignalEntry) : BruceEvent()
    data class SubGhzSignalSaved(val entry: SignalEntry, val signalId: Long) : BruceEvent()
    data class WaveformSample(val level: Float, val durationUs: Long) : BruceEvent()
    data class SystemInfoUpdate(val info: SystemInfo) : BruceEvent()
}
