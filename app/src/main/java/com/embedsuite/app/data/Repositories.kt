package com.embedsuite.app.data

import com.embedsuite.app.connection.SignalEntry
import com.embedsuite.app.rf.DecodedRfSignal
import com.embedsuite.app.rf.RfProtocolDecoder
import com.embedsuite.app.scan.WirelessDevice
import kotlinx.coroutines.flow.Flow

class SignalRepository(private val dao: CapturedSignalDao) {

    val allSignals: Flow<List<CapturedSignalEntity>> = dao.observeAll()
    val mappedSignals: Flow<List<CapturedSignalEntity>> = dao.observeWithLocation()

    fun search(query: String): Flow<List<CapturedSignalEntity>> = dao.search(query)

    fun observeByType(type: String): Flow<List<CapturedSignalEntity>> = dao.observeByType(type)

    suspend fun getLatest(): CapturedSignalEntity? = dao.getLatest()

    suspend fun countToday(): Int = dao.countSince(startOfTodayMillis())

    suspend fun countTodayByType(type: String): Int = dao.countSinceType(type, startOfTodayMillis())

    suspend fun saveSubGhzSignal(
        entry: SignalEntry,
        latitude: Double?,
        longitude: Double?,
        decoded: DecodedRfSignal? = null
    ): Long {
        return dao.insert(
            CapturedSignalEntity(
                signalType = "RF",
                name = entry.protocol,
                label = decoded?.protocol ?: entry.protocol,
                tags = decoded?.protocol ?: "",
                frequency = entry.frequency,
                protocol = entry.protocol,
                deviceId = entry.deviceId,
                rssi = entry.power.filter { it.isDigit() || it == '-' }.toIntOrNull() ?: 0,
                latitude = latitude,
                longitude = longitude,
                rawData = entry.rawData,
                detail = entry.power,
                decodedFields = decoded?.let { RfProtocolDecoder.formatDecoded(it) } ?: ""
            )
        )
    }

    /** Muestra de barrido CC1101 (`rf.scan.sample`) geotagged con GPS Android. */
    suspend fun saveRfScanSample(
        freqMhz: Double,
        rssi: Int,
        latitude: Double?,
        longitude: Double?,
        timestampMs: Long = System.currentTimeMillis()
    ): Long {
        return dao.insert(
            CapturedSignalEntity(
                timestamp = timestampMs,
                signalType = "RF_SCAN",
                name = "scan",
                label = "RSSI sweep",
                tags = "rf.scan",
                frequency = "%.3f".format(freqMhz),
                protocol = "RSSI",
                rssi = rssi,
                latitude = latitude,
                longitude = longitude,
                detail = "$rssi dBm"
            )
        )
    }

    suspend fun saveFromDecodedLine(line: String, latitude: Double?, longitude: Double?) {
        val decoded = RfProtocolDecoder.decode(line) ?: return
        dao.insert(
            CapturedSignalEntity(
                signalType = "RF",
                name = decoded.protocol,
                label = decoded.protocol,
                tags = decoded.protocol,
                frequency = decoded.frequency,
                protocol = decoded.protocol,
                deviceId = decoded.hexKey.take(16),
                latitude = latitude,
                longitude = longitude,
                rawData = line,
                decodedFields = RfProtocolDecoder.formatDecoded(decoded)
            )
        )
    }

    suspend fun saveWirelessDevice(device: WirelessDevice, latitude: Double?, longitude: Double?) {
        dao.insert(
            CapturedSignalEntity(
                signalType = device.type,
                name = device.name,
                label = device.name,
                macAddress = device.mac,
                rssi = device.rssi,
                latitude = latitude,
                longitude = longitude,
                detail = device.detail
            )
        )
    }

    suspend fun updateLabel(id: Long, label: String, tags: String) {
        val signal = dao.getById(id) ?: return
        dao.update(signal.copy(label = label, tags = tags))
    }

    suspend fun setFavorite(id: Long, favorite: Boolean) = dao.setFavorite(id, favorite)

    fun observeFavorites(): Flow<List<CapturedSignalEntity>> = dao.observeFavorites()

    suspend fun getFavoriteRf(limit: Int = 8): List<CapturedSignalEntity> = dao.getFavoriteRf(limit)

    suspend fun getSince(since: Long): List<CapturedSignalEntity> = dao.getSince(since)

    suspend fun update(signal: CapturedSignalEntity) = dao.update(signal)

    suspend fun delete(id: Long) = dao.deleteById(id)

    suspend fun getRecent(limit: Int = 500): List<CapturedSignalEntity> = dao.getRecent(limit)

    suspend fun getById(id: Long): CapturedSignalEntity? = dao.getById(id)

    suspend fun count(): Int = dao.count()

    suspend fun clearAll() = dao.clearAll()

    suspend fun saveImported(signal: CapturedSignalEntity) = dao.insert(signal)

    private fun startOfTodayMillis(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}

class IrRepository(private val dao: IrButtonDao) {
    val allButtons: Flow<List<IrButtonEntity>> = dao.observeAll()
    suspend fun save(button: IrButtonEntity) = dao.insert(button)
    suspend fun delete(id: Long) = dao.deleteById(id)
}

class MacroRepository(private val dao: MacroDao) {
    val allMacros: Flow<List<MacroEntity>> = dao.observeAll()
    suspend fun save(macro: MacroEntity) = dao.insert(macro)
    suspend fun delete(id: Long) = dao.deleteById(id)
    suspend fun getAll(): List<MacroEntity> = dao.getAll()
}

class ProfileRepository(private val dao: ProfileDao) {
    val allProfiles: Flow<List<ProfileEntity>> = dao.observeAll()
    fun byCategory(category: String): Flow<List<ProfileEntity>> = dao.observeByCategory(category)
    suspend fun save(profile: ProfileEntity) = dao.insert(profile)
    suspend fun delete(id: Long) = dao.deleteById(id)
    suspend fun count(): Int = dao.count()

    suspend fun seedDefaultsIfEmpty() {
        if (dao.count() > 0) return
        DEFAULT_PROFILES.forEach { dao.insert(it) }
        SCENARIO_PROFILES.forEach { dao.insert(it) }
    }

    companion object {
        val DEFAULT_PROFILES = listOf(
            ProfileEntity(
                name = "Captura Sub-GHz 15s",
                category = "RF",
                commands = """{"cmd":"run_action","plugin_id":"subghz_analyzer","action":"capture_start","params":{"seconds":15,"freq_mhz":433.92}}""",
                description = "Captura TEH-Link 15s @ 433.92 (usar Dashboard/RF — run_action no va por consola)"
            ),
            ProfileEntity(
                name = "Captura Sub-GHz 30s",
                category = "RF",
                commands = """{"cmd":"run_action","plugin_id":"subghz_analyzer","action":"capture_start","params":{"seconds":30,"freq_mhz":433.92}}""",
                description = "Captura TEH-Link extendida"
            ),
            ProfileEntity(
                name = "TV Power NEC",
                category = "IR",
                commands = """{"cmd":"run_action","plugin_id":"ir_toolkit","action":"send","params":{"protocol":"NEC","address":"00FF","command":"00FF"}}""",
                description = "Power NEC vía TEH-Link (pantalla NFC/IR)"
            ),
            ProfileEntity(
                name = "IR Sniff 10s",
                category = "IR",
                commands = """{"cmd":"run_action","plugin_id":"ir_toolkit","action":"rx_start","params":{"seconds":10}}""",
                description = "Captura IR vía TEH-Link"
            ),
            ProfileEntity(
                name = "System Recon",
                category = "RECON",
                commands = """{"cmd":"get_info","id":1}
{"cmd":"get_status","id":2}""",
                description = "get_info + get_status TEH-Link"
            ),
            ProfileEntity(
                name = "WiFi Scan T-Embed",
                category = "RECON",
                commands = """{"cmd":"run_action","plugin_id":"wifi_toolkit","action":"scan_start","params":{"seconds":30}}""",
                description = "Escaneo WiFi del T-Embed (Dashboard)"
            ),
            ProfileEntity(
                name = "List Actions",
                category = "RECON",
                commands = """{"cmd":"list_actions","id":1}""",
                description = "Descubre acciones TEH-Link del firmware"
            ),
            ProfileEntity(
                name = "Sub-GHz State",
                category = "RECON",
                commands = """{"cmd":"get_action_state","id":1,"plugin_id":"subghz_analyzer"}""",
                description = "Telemetría captura CC1101"
            )
        )

        val SCENARIO_PROFILES = listOf(
            ProfileEntity(
                name = "Garage Audit",
                category = "SCENARIO",
                commands = """{"cmd":"run_action","plugin_id":"subghz_analyzer","action":"capture_start","params":{"seconds":20,"freq_mhz":433.92}}
{"cmd":"get_status","id":2}""",
                description = "Captura RF 20s + status"
            ),
            ProfileEntity(
                name = "Hotel IR Scan",
                category = "SCENARIO",
                commands = """{"cmd":"run_action","plugin_id":"ir_toolkit","action":"rx_start","params":{"seconds":15}}""",
                description = "Captura IR habitación"
            ),
            ProfileEntity(
                name = "IoT Recon",
                category = "SCENARIO",
                commands = """{"cmd":"get_info","id":1}
{"cmd":"get_status","id":2}
{"cmd":"list_actions","id":3}""",
                description = "Recon TEH-Link completo"
            )
        )
    }
}

class TxHistoryRepository(private val dao: TxHistoryDao) {
    fun observeRecent(limit: Int = 5): Flow<List<TxHistoryEntity>> = dao.observeRecent(limit)

    suspend fun getRecent(limit: Int = 50): List<TxHistoryEntity> = dao.getRecent(limit)

    suspend fun getSince(since: Long): List<TxHistoryEntity> = dao.getSince(since)

    suspend fun record(signal: CapturedSignalEntity, command: String, success: Boolean) {
        dao.insert(
            TxHistoryEntity(
                signalId = signal.id,
                label = signal.label.ifBlank { signal.protocol },
                protocol = signal.protocol,
                command = command,
                success = success
            )
        )
        dao.trimOld()
    }
}

class NfcDumpRepository(private val dao: NfcDumpDao) {
    val allDumps: Flow<List<NfcDumpEntity>> = dao.observeAll()
    fun observeAll(): Flow<List<NfcDumpEntity>> = dao.observeAll()
    suspend fun save(dump: NfcDumpEntity) = dao.insert(dump)
    suspend fun getById(id: Long) = dao.getById(id)
    suspend fun delete(id: Long) = dao.deleteById(id)
}

class BleProfileRepository(private val dao: BleProfileDao) {
    val allProfiles: Flow<List<BleProfileEntity>> = dao.observeAll()
    suspend fun save(profile: BleProfileEntity) = dao.insert(profile)
    suspend fun delete(id: Long) = dao.deleteById(id)
}

class RfAutomationRepository(private val dao: RfAutomationDao) {
    val allRules: Flow<List<RfAutomationRuleEntity>> = dao.observeAll()
    suspend fun save(rule: RfAutomationRuleEntity) = dao.insert(rule)
    suspend fun update(rule: RfAutomationRuleEntity) = dao.update(rule)
    suspend fun delete(id: Long) = dao.deleteById(id)
    suspend fun getEnabled(): List<RfAutomationRuleEntity> = dao.getEnabled()
    suspend fun getAll(): List<RfAutomationRuleEntity> = dao.getAll()
}
