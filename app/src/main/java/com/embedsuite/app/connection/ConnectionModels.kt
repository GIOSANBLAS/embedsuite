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
    data class SubGhzSample(val freqMhz: Double, val rssi: Int) : DeviceEvent()
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

    /* ===== Eventos streaming TEH-Link v3 (Xibalba 0.20+) =====
     * Líneas NDJSON no solicitadas {"type":"event","event":...,"ts":...,"data":{...}} */

    /** Muestra RSSI del barrido rf_scanner (una por frecuencia ≥ umbral). */
    data class RfScanSample(
        val freqMhz: Double,
        val rssi: Int,
        val timestampMs: Long = 0
    ) : DeviceEvent()

    /** Cambio de estado del escáner RF (started/stopped/error). */
    data class RfScanStateChanged(val running: Boolean, val detail: String = "") : DeviceEvent()

    /** Cambio de estado del jammer RF (started/stopped + cutoff de seguridad). */
    data class RfJammerStateChanged(
        val running: Boolean,
        val freqMhz: Double = 0.0,
        val detail: String = ""
    ) : DeviceEvent()

    /** Tarjeta NFC detectada por el lector continuo PN532. */
    data class NfcCardDetected(
        val uid: String,
        val type: String = "",
        val sak: String = "",
        val atqa: String = "",
        val timestampMs: Long = 0
    ) : DeviceEvent()

    /** Lector NFC arrancado/detenido en el dispositivo. */
    data class NfcReaderStateChanged(val running: Boolean) : DeviceEvent()
}
