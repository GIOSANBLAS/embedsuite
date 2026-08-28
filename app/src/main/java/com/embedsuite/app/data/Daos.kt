package com.embedsuite.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CapturedSignalDao {
    @Insert
    suspend fun insert(signal: CapturedSignalEntity): Long

    @Update
    suspend fun update(signal: CapturedSignalEntity)

    @Query("SELECT * FROM captured_signals ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<CapturedSignalEntity>>

    @Query("SELECT * FROM captured_signals WHERE latitude IS NOT NULL AND longitude IS NOT NULL ORDER BY timestamp DESC")
    fun observeWithLocation(): Flow<List<CapturedSignalEntity>>

    @Query("""
        SELECT * FROM captured_signals 
        WHERE name LIKE '%' || :query || '%' 
           OR label LIKE '%' || :query || '%'
           OR protocol LIKE '%' || :query || '%'
           OR tags LIKE '%' || :query || '%'
           OR deviceId LIKE '%' || :query || '%'
        ORDER BY timestamp DESC
    """)
    fun search(query: String): Flow<List<CapturedSignalEntity>>

    @Query("SELECT * FROM captured_signals WHERE signalType = :type ORDER BY timestamp DESC")
    fun observeByType(type: String): Flow<List<CapturedSignalEntity>>

    @Query("SELECT * FROM captured_signals ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 500): List<CapturedSignalEntity>

    @Query("SELECT COUNT(*) FROM captured_signals")
    suspend fun count(): Int

    @Query("DELETE FROM captured_signals WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM captured_signals WHERE signalType = :type AND timestamp >= :since")
    suspend fun countSinceType(type: String, since: Long): Int

    @Query("SELECT COUNT(*) FROM captured_signals WHERE timestamp >= :since")
    suspend fun countSince(since: Long): Int

    @Query("SELECT * FROM captured_signals ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(): CapturedSignalEntity?

    @Query("SELECT * FROM captured_signals WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CapturedSignalEntity?

    @Query("UPDATE captured_signals SET favorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("SELECT * FROM captured_signals WHERE favorite = 1 ORDER BY timestamp DESC")
    fun observeFavorites(): Flow<List<CapturedSignalEntity>>

    @Query("SELECT * FROM captured_signals WHERE favorite = 1 AND signalType = 'RF' ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getFavoriteRf(limit: Int = 10): List<CapturedSignalEntity>

    @Query("SELECT * FROM captured_signals WHERE timestamp >= :since ORDER BY timestamp DESC")
    suspend fun getSince(since: Long): List<CapturedSignalEntity>

    @Query("DELETE FROM captured_signals")
    suspend fun clearAll()
}

@Dao
interface IrButtonDao {
    @Insert
    suspend fun insert(button: IrButtonEntity): Long

    @Query("SELECT * FROM ir_buttons ORDER BY panelName, buttonName")
    fun observeAll(): Flow<List<IrButtonEntity>>

    @Query("SELECT * FROM ir_buttons ORDER BY panelName, buttonName")
    suspend fun getAll(): List<IrButtonEntity>

    @Query("DELETE FROM ir_buttons WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface MacroDao {
    @Insert
    suspend fun insert(macro: MacroEntity): Long

    @Query("SELECT * FROM macros ORDER BY name")
    fun observeAll(): Flow<List<MacroEntity>>

    @Query("SELECT * FROM macros ORDER BY name")
    suspend fun getAll(): List<MacroEntity>

    @Query("DELETE FROM macros WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface BruceCustomCommandDao {
    @Insert
    suspend fun insert(command: BruceCustomCommandEntity): Long

    @Query("SELECT * FROM bruce_custom_commands ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<BruceCustomCommandEntity>>

    @Query("DELETE FROM bruce_custom_commands WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface ProfileDao {
    @Insert
    suspend fun insert(profile: ProfileEntity): Long

    @Query("SELECT * FROM profiles ORDER BY category, name")
    fun observeAll(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE category = :category ORDER BY name")
    fun observeByCategory(category: String): Flow<List<ProfileEntity>>

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM profiles")
    suspend fun count(): Int

    @Query("SELECT * FROM profiles ORDER BY category, name")
    suspend fun getAll(): List<ProfileEntity>
}

@Dao
interface TxHistoryDao {
    @Insert
    suspend fun insert(entry: TxHistoryEntity): Long

    @Query("SELECT * FROM tx_history ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int = 5): Flow<List<TxHistoryEntity>>

    @Query("SELECT * FROM tx_history ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 50): List<TxHistoryEntity>

    @Query("SELECT * FROM tx_history WHERE timestamp >= :since ORDER BY timestamp DESC")
    suspend fun getSince(since: Long): List<TxHistoryEntity>

    @Query("SELECT * FROM tx_history ORDER BY timestamp DESC")
    suspend fun getAll(): List<TxHistoryEntity>

    @Query("DELETE FROM tx_history WHERE id NOT IN (SELECT id FROM tx_history ORDER BY timestamp DESC LIMIT 50)")
    suspend fun trimOld()
}

@Dao
interface NfcDumpDao {
    @Insert
    suspend fun insert(dump: NfcDumpEntity): Long

    @Query("SELECT * FROM nfc_dumps ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<NfcDumpEntity>>

    @Query("SELECT * FROM nfc_dumps ORDER BY timestamp DESC")
    suspend fun getAll(): List<NfcDumpEntity>

    @Query("SELECT * FROM nfc_dumps WHERE id = :id")
    suspend fun getById(id: Long): NfcDumpEntity?

    @Query("DELETE FROM nfc_dumps WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface BleProfileDao {
    @Insert
    suspend fun insert(profile: BleProfileEntity): Long

    @Query("SELECT * FROM ble_profiles ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<BleProfileEntity>>

    @Query("SELECT * FROM ble_profiles ORDER BY timestamp DESC")
    suspend fun getAll(): List<BleProfileEntity>

    @Query("DELETE FROM ble_profiles WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface RfAutomationDao {
    @Insert
    suspend fun insert(rule: RfAutomationRuleEntity): Long

    @Update
    suspend fun update(rule: RfAutomationRuleEntity)

    @Query("SELECT * FROM rf_automation_rules ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<RfAutomationRuleEntity>>

    @Query("SELECT * FROM rf_automation_rules WHERE enabled = 1 ORDER BY createdAt DESC")
    suspend fun getEnabled(): List<RfAutomationRuleEntity>

    @Query("DELETE FROM rf_automation_rules WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM rf_automation_rules ORDER BY createdAt DESC")
    suspend fun getAll(): List<RfAutomationRuleEntity>
}
