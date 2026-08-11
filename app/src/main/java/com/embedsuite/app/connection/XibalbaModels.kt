package com.embedsuite.app.connection

import org.json.JSONObject
import java.util.concurrent.atomic.AtomicInteger

/**
 * Modelos del protocolo TEH-Link v3 extendido (Xibalba 0.20+).
 * Superficie completa de comandos del ecosistema: RF, NFC, SD, audio, IR y sistema.
 */

data class XibalbaCommand(
    val id: Int = XibalbaCommandIds.next(),
    val cmd: String,
    val params: Map<String, Any?>? = null
)

object XibalbaCommandIds {
    private val counter = AtomicInteger(10_000)
    fun next(): Int = counter.incrementAndGet()
}

// ===== RF =====

data class RfScanParams(
    val freqStartMhz: Double = 433.0,
    val freqEndMhz: Double = 435.0,
    val stepMhz: Double = 0.1,
    val rssiThreshold: Int = -100,
    val dwellMs: Int = 10
) {
    fun isValid(): Boolean =
        freqStartMhz >= 300.0 && freqEndMhz <= 928.0 &&
            freqStartMhz < freqEndMhz && stepMhz > 0.0 && stepMhz <= (freqEndMhz - freqStartMhz)
}

data class RfScanStatus(
    val running: Boolean = false,
    val state: String = "idle",
    val freqStart: Double = 0.0,
    val freqEnd: Double = 0.0,
    val step: Double = 0.0,
    val rssiThreshold: Int = -100,
    val samples: Long = 0,
    val sweeps: Long = 0,
    val maxFreq: Double = 0.0,
    val maxRssi: Int = -127
) {
    companion object {
        fun fromJson(data: JSONObject): RfScanStatus = RfScanStatus(
            running = data.optBoolean("running"),
            state = data.optString("state", "idle"),
            freqStart = data.optDouble("freq_start"),
            freqEnd = data.optDouble("freq_end"),
            step = data.optDouble("step"),
            rssiThreshold = data.optInt("rssi_threshold", -100),
            samples = data.optLong("samples"),
            sweeps = data.optLong("sweeps"),
            maxFreq = data.optDouble("max_freq"),
            maxRssi = data.optInt("max_rssi", -127)
        )
    }
}

data class JammerParams(
    val freqMhz: Double = 433.92,
    val powerDbm: Int = 12,
    val mode: JammerMode = JammerMode.CONTINUOUS,
    val burstOnMs: Int = 50,
    val burstIntervalMs: Int = 100,
    val maxSeconds: Int = 30
) {
    fun isValid(): Boolean = freqMhz in 300.0..928.0 && powerDbm in -30..12 && maxSeconds in 1..300
}

enum class JammerMode(val wireName: String) {
    CONTINUOUS("continuous"),
    BURST("burst");

    companion object {
        fun fromWire(value: String?): JammerMode =
            entries.firstOrNull { it.wireName.equals(value, ignoreCase = true) } ?: CONTINUOUS
    }
}

data class JammerStatus(
    val running: Boolean = false,
    val state: String = "idle",
    val freqMhz: Double = 0.0,
    val powerDbm: Int = 0,
    val mode: JammerMode = JammerMode.CONTINUOUS,
    val pulses: Long = 0,
    val maxMs: Long = 0,
    val elapsedMs: Long = 0
) {
    companion object {
        fun fromJson(data: JSONObject): JammerStatus = JammerStatus(
            running = data.optBoolean("running"),
            state = data.optString("state", "idle"),
            freqMhz = data.optDouble("freq"),
            powerDbm = data.optInt("power_dbm"),
            mode = JammerMode.fromWire(data.optString("mode")),
            pulses = data.optLong("pulses"),
            maxMs = data.optLong("max_ms"),
            elapsedMs = data.optLong("elapsed_ms")
        )
    }
}

// ===== NFC =====

data class NfcCard(
    val uid: String,
    val type: String = "",
    val sak: String = "",
    val atqa: String = "",
    val timestampMs: Long = System.currentTimeMillis()
) {
    val uidHex: String get() = uid.replace(":", "").uppercase()

    companion object {
        fun fromJson(data: JSONObject): NfcCard = NfcCard(
            uid = data.optString("uid"),
            type = data.optString("type"),
            sak = data.optString("sak"),
            atqa = data.optString("atqa")
        )
    }
}

data class NfcReaderStatus(
    val running: Boolean = false,
    val ready: Boolean = false,
    val state: String = "idle",
    val cards: Long = 0,
    val lastUid: String = "",
    val message: String = ""
) {
    companion object {
        fun fromJson(data: JSONObject): NfcReaderStatus = NfcReaderStatus(
            running = data.optBoolean("running"),
            ready = data.optBoolean("ready"),
            state = data.optString("state", "idle"),
            cards = data.optLong("cards"),
            lastUid = data.optString("last_uid"),
            message = data.optString("message")
        )
    }
}

// ===== IR =====

data class IrSignal(
    val protocol: String,
    val address: String = "",
    val command: String = "",
    val raw: String = "",
    val frequencyHz: Int = 38000
)

// ===== SD =====

data class SdFileEntry(
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long
) {
    companion object {
        fun fromJson(obj: JSONObject): SdFileEntry = SdFileEntry(
            name = obj.optString("name"),
            isDirectory = obj.optBoolean("dir"),
            sizeBytes = obj.optLong("size")
        )
    }
}

data class SdStatus(
    val mounted: Boolean = false,
    val state: String = "missing",
    val totalBytes: Long = 0,
    val usedBytes: Long = 0
) {
    val freeBytes: Long get() = (totalBytes - usedBytes).coerceAtLeast(0)

    companion object {
        fun fromJson(data: JSONObject): SdStatus = SdStatus(
            mounted = data.optBoolean("mounted"),
            state = data.optString("state", "missing"),
            totalBytes = data.optLong("total_bytes"),
            usedBytes = data.optLong("used_bytes")
        )
    }
}

// ===== Sistema =====

enum class DeviceMode(val wireName: String) {
    ACTIVE("active"),
    STEALTH("stealth");

    companion object {
        fun fromWire(value: String?): DeviceMode =
            entries.firstOrNull { it.wireName.equals(value, ignoreCase = true) } ?: ACTIVE
    }
}

data class TimeSyncResult(
    val synced: Boolean,
    val offsetMs: Long,
    val deviceTimeMs: Long
)

data class PowerStatus(
    val powerMode: DeviceMode = DeviceMode.ACTIVE,
    val idleMs: Long = 0,
    val idleTimeoutMs: Long = 0,
    val timeSynced: Boolean = false,
    val lang: String = "es"
) {
    companion object {
        fun fromJson(data: JSONObject): PowerStatus = PowerStatus(
            powerMode = DeviceMode.fromWire(data.optString("power_mode")),
            idleMs = data.optLong("idle_ms"),
            idleTimeoutMs = data.optLong("idle_timeout_ms"),
            timeSynced = data.optBoolean("time_synced"),
            lang = data.optString("lang", "es")
        )
    }
}
