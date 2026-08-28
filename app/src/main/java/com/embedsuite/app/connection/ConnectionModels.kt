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
    val sdFreeSpace: String = "",
    val sdFreeBytes: Long? = null,
    val profile: FirmwareProfile = FirmwareProfile.UNKNOWN,
    val brucePlugins: List<TehLinkPluginInfo> = emptyList(),
    /** Subsistemas en modo simulación (TEH-Link get_status.sim). Vacío = desconocido. */
    val simFlags: Map<String, Boolean> = emptyMap(),
    /** Capabilities reportadas por TEH-Link get_status.capabilities. */
    val bruceCapabilities: Map<String, Boolean> = emptyMap(),
    /** Flags de hardening reportados por Bruce 0.17.1+ (TWDT / BOD / Secure Boot / etc). */
    val hardening: TehLinkHardeningInfo = TehLinkHardeningInfo(),
    /** Coredump ELF pendiente en flash (solo útil cuando hubo pánico WDT). */
    val coredumpPending: Boolean = false,
    /** Última razón de pánico TWDT (null si el firmware no reinició por watchdog). */
    val wdtPanicReason: String? = null,
    /** Estado OTA más reciente: sha256_verified, progress, state. */
    val lastOta: TehLinkOtaStatus = TehLinkOtaStatus(),
    /** Temperatura reportada por firmware (°C). */
    val temperatureC: String = ""
)

sealed class DeviceEvent {
    data class RawLine(val line: String) : DeviceEvent()
    data class SubGhzSignal(val entry: SignalEntry) : DeviceEvent()
    data class SubGhzSignalSaved(val entry: SignalEntry, val signalId: Long) : DeviceEvent()
    data class WaveformSample(val level: Float, val durationUs: Long) : DeviceEvent()
    data class SystemInfoUpdate(val info: SystemInfo) : DeviceEvent()
    /** Avisos TEH-Link (pairing, auth). */
    data class TehLinkNotice(val message: String) : DeviceEvent()
    /** OTA Bruce completada: expone sha256_verified para UI feedback instantáneo. */
    data class OtaCompleted(val status: TehLinkOtaStatus) : DeviceEvent()

    /* ===== 5 plugins ofensivos ===== */
    data class BleAdSpamProgress(val packets: Long, val campaign: String) : DeviceEvent()
    data class WifiProbe(
        val ssid: String,
        val mac: String,
        val rssi: Int,
        val channel: Int,
        val vendor: String = ""
    ) : DeviceEvent()
    data class MousejackDongle(
        val addr: String,
        val bestChannel: Int,
        val bestRssi: Int,
        val frames: Long
    ) : DeviceEvent()
    data class SubGhzSample(
        val freqMhz: Double,
        val rssi: Int,
        val latitude: Double? = null,
        val longitude: Double? = null,
        val timestampMs: Long = System.currentTimeMillis()
    ) : DeviceEvent()
    data class SubGhzDecodedFrame(
        val proto: String,
        val decoded: String,
        val rssi: Int,
        val freqMhz: Double
    ) : DeviceEvent()
    data class NfcCloneProgress(
        val step: String,
        val sectors: Int = 0,
        val uid: String = "",
        val dumpHex: String = ""
    ) : DeviceEvent()

    /** Evento TEH-Link asíncrono (`{"event":"…","data":{…}}`). */
    data class TehLinkAsyncEvent(val type: String, val dataJson: String) : DeviceEvent()

    data class RfJammerStopped(val reason: String, val elapsedMs: Long = 0) : DeviceEvent()

    data class RfScanStopped(val reason: String, val sweeps: Long = 0, val samples: Long = 0) : DeviceEvent()
}
