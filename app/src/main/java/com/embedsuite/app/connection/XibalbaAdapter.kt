package com.embedsuite.app.connection

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import org.json.JSONObject

/**
 * XibalbaAdapter — superficie completa de comandos TEH-Link v3 (Xibalba 0.20+).
 *
 * Fachada tipada sobre [DeviceConnectionManager]: un punto de entrada para todos
 * los comandos RF / NFC / IR / SD / audio / sistema del ecosistema, con parsing
 * de respuestas extendidas (rawResponse) y flujos de eventos en tiempo real.
 */
class XibalbaAdapter(
    private val connectionManager: DeviceConnectionManager
) {

    // ===== Sistema =====

    suspend fun ping(): Boolean = connectionManager.detectedProfile.value == FirmwareProfile.XIBALBA

    /** Sincroniza el reloj del T-Embed con el de Android (ms epoch). */
    suspend fun syncTime(androidTimestampMs: Long = System.currentTimeMillis()): Result<TimeSyncResult> {
        val cmd = XibalbaCommand(
            cmd = "time_sync",
            params = mapOf("timestamp_ms" to androidTimestampMs)
        )
        return executeRaw(cmd).mapCatching { data ->
            TimeSyncResult(
                synced = data.optBoolean("synced"),
                offsetMs = data.optLong("offset_ms"),
                deviceTimeMs = data.optLong("device_time_ms")
            )
        }
    }

    suspend fun setLanguage(lang: String): Result<String> {
        return runAction("device", "set_language", JSONObject().put("lang", lang))
            .mapCatching { it.optString("lang", lang) }
    }

    suspend fun setMode(mode: DeviceMode): Result<DeviceMode> {
        return runAction("device", "set_mode", JSONObject().put("mode", mode.wireName))
            .mapCatching { DeviceMode.fromWire(it.optString("mode")) }
    }

    suspend fun getPowerStatus(): Result<PowerStatus> {
        return runAction("device", "power_status").mapCatching { PowerStatus.fromJson(it) }
    }

    // ===== RF Scanner =====

    suspend fun startScan(params: RfScanParams): Result<Unit> {
        if (!params.isValid()) return Result.failure(IllegalArgumentException("freq_out_of_range"))
        return runAction(
            "rf_scanner", "start",
            JSONObject()
                .put("freq_start", params.freqStartMhz)
                .put("freq_end", params.freqEndMhz)
                .put("step", params.stepMhz)
                .put("rssi_threshold", params.rssiThreshold)
                .put("dwell_ms", params.dwellMs)
        ).map { }
    }

    suspend fun stopScan(): Result<RfScanStatus> {
        return runAction("rf_scanner", "stop").mapCatching { RfScanStatus.fromJson(it) }
    }

    suspend fun getScanStatus(): Result<RfScanStatus> {
        return runAction("rf_scanner", "status").mapCatching { RfScanStatus.fromJson(it) }
    }

    // ===== RF Jammer =====

    suspend fun startJammer(params: JammerParams): Result<Unit> {
        if (!params.isValid()) return Result.failure(IllegalArgumentException("invalid_jammer_params"))
        return runAction(
            "rf_jammer", "start",
            JSONObject()
                .put("freq", params.freqMhz)
                .put("power", params.powerDbm)
                .put("mode", params.mode.wireName)
                .put("burst_on_ms", params.burstOnMs)
                .put("burst_interval", params.burstIntervalMs)
                .put("max_seconds", params.maxSeconds)
        ).map { }
    }

    suspend fun stopJammer(): Result<JammerStatus> {
        return runAction("rf_jammer", "stop").mapCatching { JammerStatus.fromJson(it) }
    }

    suspend fun getJammerStatus(): Result<JammerStatus> {
        return runAction("rf_jammer", "status").mapCatching { JammerStatus.fromJson(it) }
    }

    // ===== NFC =====

    suspend fun nfcRead(timeoutSec: Int = 10): Result<NfcCard> {
        return runAction("nfc_toolkit", "read", JSONObject().put("timeout", timeoutSec))
            .mapCatching { data ->
                if (data.optString("state") != "detected") {
                    throw Exception(data.optString("message").ifBlank { "nfc_timeout" })
                }
                NfcCard.fromJson(data)
            }
    }

    suspend fun startNfcReader(timeoutSec: Int = 60): Result<Unit> {
        return runAction("nfc_toolkit", "reader_start", JSONObject().put("timeout", timeoutSec))
            .map { }
    }

    suspend fun stopNfcReader(): Result<NfcReaderStatus> {
        return runAction("nfc_toolkit", "reader_stop").mapCatching { NfcReaderStatus.fromJson(it) }
    }

    suspend fun getNfcStatus(): Result<NfcReaderStatus> {
        return runAction("nfc_toolkit", "status").mapCatching { NfcReaderStatus.fromJson(it) }
    }

    /** Escribe un registro NDEF texto en una tarjeta NTAG/Ultralight presente. */
    suspend fun writeNfcTag(text: String, timeoutSec: Int = 15): Result<Unit> {
        if (text.isBlank() || text.length > 130) {
            return Result.failure(IllegalArgumentException("invalid_data"))
        }
        return runAction(
            "nfc_toolkit", "write",
            JSONObject().put("text", text).put("timeout", timeoutSec)
        ).map { }
    }

    // ===== IR =====

    suspend fun irTransmit(signal: IrSignal): Result<Unit> {
        val params = JSONObject()
        if (signal.raw.isNotBlank()) {
            params.put("raw", signal.raw)
        } else {
            params
                .put("protocol", signal.protocol)
                .put("address", signal.address)
                .put("command", signal.command)
        }
        return runAction("ir_toolkit", "send", params).map { }
    }

    suspend fun irCapture(seconds: Int = 10): Result<IrSignal> {
        return runAction("ir_toolkit", "rx_start", JSONObject().put("seconds", seconds.coerceIn(1, 60)))
            .mapCatching { data ->
                IrSignal(
                    protocol = data.optString("protocol", "RAW"),
                    raw = data.optString("raw")
                )
            }
    }

    // ===== SD =====

    suspend fun sdMount(): Result<SdStatus> {
        return runAction("sd_storage", "mount").mapCatching { SdStatus.fromJson(it) }
    }

    suspend fun sdStatus(): Result<SdStatus> {
        return runAction("sd_storage", "status").mapCatching { SdStatus.fromJson(it) }
    }

    suspend fun sdListFiles(path: String = "/"): Result<List<SdFileEntry>> {
        return runAction("sd_storage", "list", JSONObject().put("path", path))
            .mapCatching { data ->
                val arr = data.optJSONArray("files") ?: return@mapCatching emptyList<SdFileEntry>()
                (0 until arr.length()).map { SdFileEntry.fromJson(arr.getJSONObject(it)) }
            }
    }

    /** Guarda bytes en /embedsuite/ de la SD del T-Embed (sesiones, dumps). */
    suspend fun sdSaveFile(filename: String, data: ByteArray): Result<String> {
        val safeName = filename.substringAfterLast('/').take(64)
        if (safeName.isBlank() || safeName.contains("..")) {
            return Result.failure(IllegalArgumentException("invalid_filename"))
        }
        val b64 = android.util.Base64.encodeToString(data, android.util.Base64.NO_WRAP)
        return runAction(
            "sd_storage", "save",
            JSONObject().put("filename", safeName).put("data", b64)
        ).mapCatching { it.optString("path", "/embedsuite/$safeName") }
    }

    // ===== Audio =====

    suspend fun audioBeep(freqHz: Int = 1000, durationMs: Int = 100): Result<Unit> {
        return runAction(
            "audio", "beep",
            JSONObject()
                .put("freq", freqHz.coerceIn(50, 8000))
                .put("duration", durationMs.coerceIn(0, 5000))
        ).map { }
    }

    // ===== Flujos de eventos en tiempo real =====

    fun observeEvents(): Flow<DeviceEvent> = connectionManager.events

    fun observeRfSamples(): Flow<DeviceEvent.RfScanSample> =
        connectionManager.events.filterIsInstance<DeviceEvent.RfScanSample>()

    fun observeNfcCards(): Flow<DeviceEvent.NfcCardDetected> =
        connectionManager.events.filterIsInstance<DeviceEvent.NfcCardDetected>()

    fun observeJammerState(): Flow<DeviceEvent.RfJammerStateChanged> =
        connectionManager.events.filterIsInstance<DeviceEvent.RfJammerStateChanged>()

    fun observeScanState(): Flow<DeviceEvent.RfScanStateChanged> =
        connectionManager.events.filterIsInstance<DeviceEvent.RfScanStateChanged>()

    // ===== Internos =====

    private suspend fun runAction(
        pluginId: String,
        action: String,
        params: JSONObject = JSONObject()
    ): Result<JSONObject> {
        return connectionManager.tehLinkRunAction(pluginId, action, params).mapCatching { result ->
            result.rawResponse ?: JSONObject()
        }
    }

    private suspend fun executeRaw(command: XibalbaCommand): Result<JSONObject> {
        val json = JSONObject().put("cmd", command.cmd).put("id", command.id)
        command.params?.forEach { (k, v) -> if (v != null) json.put(k, v) }
        return connectionManager.executeTehLinkJson(json.toString()).mapCatching { line ->
            val root = JSONObject(line)
            if (!root.optBoolean("ok")) {
                throw Exception(root.optString("error", "teh_link_error"))
            }
            root.optJSONObject("data") ?: JSONObject()
        }
    }
}
