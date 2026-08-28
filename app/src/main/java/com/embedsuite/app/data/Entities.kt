package com.embedsuite.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "captured_signals")
data class CapturedSignalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val signalType: String,
    val name: String = "",
    val label: String = "",
    val tags: String = "",
    val frequency: String = "",
    val protocol: String = "",
    val deviceId: String = "",
    val macAddress: String = "",
    val rssi: Int = 0,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val rawData: String = "",
    val detail: String = "",
    val decodedFields: String = "",
    val favorite: Boolean = false
)

@Entity(tableName = "ir_buttons")
data class IrButtonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val panelName: String = "Default",
    val buttonName: String,
    val protocol: String = "NEC",
    val hexCode: String = "",
    @ColumnInfo(name = "ir_payload")
    val irPayload: String = ""
)

@Entity(tableName = "macros")
data class MacroEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val commands: String,
    val description: String = ""
)

/** Comando CLI Bruce guardado por el usuario (patrón companion oficial, persistido local). */
@Entity(tableName = "bruce_custom_commands")
data class BruceCustomCommandEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val command: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val commands: String,
    val description: String = "",
    val icon: String = ""
)

@Entity(tableName = "tx_history")
data class TxHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val signalId: Long = 0,
    val label: String = "",
    val protocol: String = "",
    val command: String = "",
    val success: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "nfc_dumps")
data class NfcDumpEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uid: String = "",
    val tagType: String = "",
    val rawDump: String = "",
    val parsedSectors: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "ble_profiles")
data class BleProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val address: String = "",
    val services: String = "",
    val manufacturerData: String = "",
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "rf_automation_rules")
data class RfAutomationRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val enabled: Boolean = true,
    val matchProtocol: String = "",
    val matchFrequency: String = "",
    val actionType: String = "NOTIFY",
    val actionPayload: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
