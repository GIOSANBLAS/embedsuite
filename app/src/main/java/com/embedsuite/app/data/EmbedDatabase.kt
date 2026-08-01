package com.embedsuite.app.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        CapturedSignalEntity::class,
        IrButtonEntity::class,
        MacroEntity::class,
        ProfileEntity::class,
        TxHistoryEntity::class,
        NfcDumpEntity::class,
        BleProfileEntity::class,
        RfAutomationRuleEntity::class
    ],
    version = 7,
    exportSchema = false
)
/** Room local; cifrado at-rest vía SQLCipher cuando [SecureStore] está disponible. */
abstract class EmbedDatabase : RoomDatabase() {
    abstract fun capturedSignalDao(): CapturedSignalDao
    abstract fun irButtonDao(): IrButtonDao
    abstract fun macroDao(): MacroDao
    abstract fun profileDao(): ProfileDao
    abstract fun txHistoryDao(): TxHistoryDao
    abstract fun nfcDumpDao(): NfcDumpDao
    abstract fun bleProfileDao(): BleProfileDao
    abstract fun rfAutomationDao(): RfAutomationDao
}
