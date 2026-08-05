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
    val freeHeapBytes: Long? = null,
    val freePsramBytes: Long? = null,
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
    val xibalbaCapabilities: Map<String, Boolean> = emptyMap(),
    /** Flags de hardening reportados por Xibalba 0.17.1+ (TWDT / BOD / Secure Boot / etc). */
    val hardening: TehLinkHardeningInfo = TehLinkHardeningInfo(),
    /** Coredump ELF pendiente en flash (solo útil cuando hubo pánico WDT). */
    val coredumpPending: Boolean = false,
    /** Última razón de pánico TWDT (null si el firmware no reinició por watchdog). */
    val wdtPanicReason: String? = null,
    /** Estado OTA más reciente: sha256_verified, progress, state. */
    val lastOta: TehLinkOtaStatus = TehLinkOtaStatus()
)

sealed class DeviceEvent {
    data class RawLine(val line: String) : DeviceEvent()
    data class SubGhzSignal(val entry: SignalEntry) : DeviceEvent()
    data class SubGhzSignalSaved(val entry: SignalEntry, val signalId: Long) : DeviceEvent()
    data class WaveformSample(val level: Float, val durationUs: Long) : DeviceEvent()
    data class SystemInfoUpdate(val info: SystemInfo) : DeviceEvent()
    /** Avisos TEH-Link (pairing, auth). */
    data class TehLinkNotice(val message: String) : DeviceEvent()
    /** OTA Xibalba completada: expone sha256_verified para UI feedback instantáneo. */
    data class OtaCompleted(val status: TehLinkOtaStatus) : DeviceEvent()
}
