package com.embedsuite.app.`data`

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class EmbedDatabase_Impl : EmbedDatabase() {
  private val _capturedSignalDao: Lazy<CapturedSignalDao> = lazy {
    CapturedSignalDao_Impl(this)
  }

  private val _irButtonDao: Lazy<IrButtonDao> = lazy {
    IrButtonDao_Impl(this)
  }

  private val _macroDao: Lazy<MacroDao> = lazy {
    MacroDao_Impl(this)
  }

  private val _profileDao: Lazy<ProfileDao> = lazy {
    ProfileDao_Impl(this)
  }

  private val _txHistoryDao: Lazy<TxHistoryDao> = lazy {
    TxHistoryDao_Impl(this)
  }

  private val _nfcDumpDao: Lazy<NfcDumpDao> = lazy {
    NfcDumpDao_Impl(this)
  }

  private val _bleProfileDao: Lazy<BleProfileDao> = lazy {
    BleProfileDao_Impl(this)
  }

  private val _rfAutomationDao: Lazy<RfAutomationDao> = lazy {
    RfAutomationDao_Impl(this)
  }

  private val _scriptDao: Lazy<ScriptDao> = lazy {
    ScriptDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(10,
        "a5e119f3d1a5c7713418b0ed6979db10", "27d02c75901b6698b57cd0a06ad2abe3") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `captured_signals` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` INTEGER NOT NULL, `signalType` TEXT NOT NULL, `name` TEXT NOT NULL, `label` TEXT NOT NULL, `tags` TEXT NOT NULL, `frequency` TEXT NOT NULL, `protocol` TEXT NOT NULL, `deviceId` TEXT NOT NULL, `macAddress` TEXT NOT NULL, `rssi` INTEGER NOT NULL, `latitude` REAL, `longitude` REAL, `rawData` TEXT NOT NULL, `detail` TEXT NOT NULL, `decodedFields` TEXT NOT NULL, `favorite` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `ir_buttons` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `panelName` TEXT NOT NULL, `buttonName` TEXT NOT NULL, `protocol` TEXT NOT NULL, `hexCode` TEXT NOT NULL, `ir_payload` TEXT NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `macros` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `commands` TEXT NOT NULL, `description` TEXT NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `profiles` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `category` TEXT NOT NULL, `commands` TEXT NOT NULL, `description` TEXT NOT NULL, `icon` TEXT NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `tx_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `signalId` INTEGER NOT NULL, `label` TEXT NOT NULL, `protocol` TEXT NOT NULL, `command` TEXT NOT NULL, `success` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `nfc_dumps` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `uid` TEXT NOT NULL, `tagType` TEXT NOT NULL, `rawDump` TEXT NOT NULL, `parsedSectors` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `ble_profiles` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `address` TEXT NOT NULL, `services` TEXT NOT NULL, `manufacturerData` TEXT NOT NULL, `notes` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `rf_automation_rules` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `enabled` INTEGER NOT NULL, `matchProtocol` TEXT NOT NULL, `matchFrequency` TEXT NOT NULL, `actionType` TEXT NOT NULL, `actionPayload` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `scripts` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `category` TEXT NOT NULL, `description` TEXT NOT NULL, `author` TEXT NOT NULL, `version` TEXT NOT NULL, `dialect` TEXT NOT NULL, `sourceCode` TEXT NOT NULL, `isFavorite` INTEGER NOT NULL, `lastSyncAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'a5e119f3d1a5c7713418b0ed6979db10')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `captured_signals`")
        connection.execSQL("DROP TABLE IF EXISTS `ir_buttons`")
        connection.execSQL("DROP TABLE IF EXISTS `macros`")
        connection.execSQL("DROP TABLE IF EXISTS `profiles`")
        connection.execSQL("DROP TABLE IF EXISTS `tx_history`")
        connection.execSQL("DROP TABLE IF EXISTS `nfc_dumps`")
        connection.execSQL("DROP TABLE IF EXISTS `ble_profiles`")
        connection.execSQL("DROP TABLE IF EXISTS `rf_automation_rules`")
        connection.execSQL("DROP TABLE IF EXISTS `scripts`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection):
          RoomOpenDelegate.ValidationResult {
        val _columnsCapturedSignals: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsCapturedSignals.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCapturedSignals.put("timestamp", TableInfo.Column("timestamp", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCapturedSignals.put("signalType", TableInfo.Column("signalType", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCapturedSignals.put("name", TableInfo.Column("name", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCapturedSignals.put("label", TableInfo.Column("label", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCapturedSignals.put("tags", TableInfo.Column("tags", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCapturedSignals.put("frequency", TableInfo.Column("frequency", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCapturedSignals.put("protocol", TableInfo.Column("protocol", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCapturedSignals.put("deviceId", TableInfo.Column("deviceId", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCapturedSignals.put("macAddress", TableInfo.Column("macAddress", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCapturedSignals.put("rssi", TableInfo.Column("rssi", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCapturedSignals.put("latitude", TableInfo.Column("latitude", "REAL", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCapturedSignals.put("longitude", TableInfo.Column("longitude", "REAL", false, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCapturedSignals.put("rawData", TableInfo.Column("rawData", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCapturedSignals.put("detail", TableInfo.Column("detail", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCapturedSignals.put("decodedFields", TableInfo.Column("decodedFields", "TEXT", true,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCapturedSignals.put("favorite", TableInfo.Column("favorite", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysCapturedSignals: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesCapturedSignals: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoCapturedSignals: TableInfo = TableInfo("captured_signals", _columnsCapturedSignals,
            _foreignKeysCapturedSignals, _indicesCapturedSignals)
        val _existingCapturedSignals: TableInfo = read(connection, "captured_signals")
        if (!_infoCapturedSignals.equals(_existingCapturedSignals)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |captured_signals(com.embedsuite.app.data.CapturedSignalEntity).
              | Expected:
              |""".trimMargin() + _infoCapturedSignals + """
              |
              | Found:
              |""".trimMargin() + _existingCapturedSignals)
        }
        val _columnsIrButtons: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsIrButtons.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsIrButtons.put("panelName", TableInfo.Column("panelName", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsIrButtons.put("buttonName", TableInfo.Column("buttonName", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsIrButtons.put("protocol", TableInfo.Column("protocol", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsIrButtons.put("hexCode", TableInfo.Column("hexCode", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsIrButtons.put("ir_payload", TableInfo.Column("ir_payload", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysIrButtons: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesIrButtons: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoIrButtons: TableInfo = TableInfo("ir_buttons", _columnsIrButtons,
            _foreignKeysIrButtons, _indicesIrButtons)
        val _existingIrButtons: TableInfo = read(connection, "ir_buttons")
        if (!_infoIrButtons.equals(_existingIrButtons)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |ir_buttons(com.embedsuite.app.data.IrButtonEntity).
              | Expected:
              |""".trimMargin() + _infoIrButtons + """
              |
              | Found:
              |""".trimMargin() + _existingIrButtons)
        }
        val _columnsMacros: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsMacros.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMacros.put("name", TableInfo.Column("name", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMacros.put("commands", TableInfo.Column("commands", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMacros.put("description", TableInfo.Column("description", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysMacros: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesMacros: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoMacros: TableInfo = TableInfo("macros", _columnsMacros, _foreignKeysMacros,
            _indicesMacros)
        val _existingMacros: TableInfo = read(connection, "macros")
        if (!_infoMacros.equals(_existingMacros)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |macros(com.embedsuite.app.data.MacroEntity).
              | Expected:
              |""".trimMargin() + _infoMacros + """
              |
              | Found:
              |""".trimMargin() + _existingMacros)
        }
        val _columnsProfiles: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsProfiles.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsProfiles.put("name", TableInfo.Column("name", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsProfiles.put("category", TableInfo.Column("category", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsProfiles.put("commands", TableInfo.Column("commands", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsProfiles.put("description", TableInfo.Column("description", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsProfiles.put("icon", TableInfo.Column("icon", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysProfiles: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesProfiles: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoProfiles: TableInfo = TableInfo("profiles", _columnsProfiles, _foreignKeysProfiles,
            _indicesProfiles)
        val _existingProfiles: TableInfo = read(connection, "profiles")
        if (!_infoProfiles.equals(_existingProfiles)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |profiles(com.embedsuite.app.data.ProfileEntity).
              | Expected:
              |""".trimMargin() + _infoProfiles + """
              |
              | Found:
              |""".trimMargin() + _existingProfiles)
        }
        val _columnsTxHistory: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsTxHistory.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTxHistory.put("signalId", TableInfo.Column("signalId", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTxHistory.put("label", TableInfo.Column("label", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTxHistory.put("protocol", TableInfo.Column("protocol", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTxHistory.put("command", TableInfo.Column("command", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTxHistory.put("success", TableInfo.Column("success", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTxHistory.put("timestamp", TableInfo.Column("timestamp", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysTxHistory: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesTxHistory: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoTxHistory: TableInfo = TableInfo("tx_history", _columnsTxHistory,
            _foreignKeysTxHistory, _indicesTxHistory)
        val _existingTxHistory: TableInfo = read(connection, "tx_history")
        if (!_infoTxHistory.equals(_existingTxHistory)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |tx_history(com.embedsuite.app.data.TxHistoryEntity).
              | Expected:
              |""".trimMargin() + _infoTxHistory + """
              |
              | Found:
              |""".trimMargin() + _existingTxHistory)
        }
        val _columnsNfcDumps: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsNfcDumps.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsNfcDumps.put("uid", TableInfo.Column("uid", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsNfcDumps.put("tagType", TableInfo.Column("tagType", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsNfcDumps.put("rawDump", TableInfo.Column("rawDump", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsNfcDumps.put("parsedSectors", TableInfo.Column("parsedSectors", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsNfcDumps.put("timestamp", TableInfo.Column("timestamp", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysNfcDumps: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesNfcDumps: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoNfcDumps: TableInfo = TableInfo("nfc_dumps", _columnsNfcDumps,
            _foreignKeysNfcDumps, _indicesNfcDumps)
        val _existingNfcDumps: TableInfo = read(connection, "nfc_dumps")
        if (!_infoNfcDumps.equals(_existingNfcDumps)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |nfc_dumps(com.embedsuite.app.data.NfcDumpEntity).
              | Expected:
              |""".trimMargin() + _infoNfcDumps + """
              |
              | Found:
              |""".trimMargin() + _existingNfcDumps)
        }
        val _columnsBleProfiles: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsBleProfiles.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBleProfiles.put("name", TableInfo.Column("name", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBleProfiles.put("address", TableInfo.Column("address", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBleProfiles.put("services", TableInfo.Column("services", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBleProfiles.put("manufacturerData", TableInfo.Column("manufacturerData", "TEXT",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBleProfiles.put("notes", TableInfo.Column("notes", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBleProfiles.put("timestamp", TableInfo.Column("timestamp", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysBleProfiles: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesBleProfiles: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoBleProfiles: TableInfo = TableInfo("ble_profiles", _columnsBleProfiles,
            _foreignKeysBleProfiles, _indicesBleProfiles)
        val _existingBleProfiles: TableInfo = read(connection, "ble_profiles")
        if (!_infoBleProfiles.equals(_existingBleProfiles)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |ble_profiles(com.embedsuite.app.data.BleProfileEntity).
              | Expected:
              |""".trimMargin() + _infoBleProfiles + """
              |
              | Found:
              |""".trimMargin() + _existingBleProfiles)
        }
        val _columnsRfAutomationRules: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsRfAutomationRules.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsRfAutomationRules.put("name", TableInfo.Column("name", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsRfAutomationRules.put("enabled", TableInfo.Column("enabled", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRfAutomationRules.put("matchProtocol", TableInfo.Column("matchProtocol", "TEXT",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRfAutomationRules.put("matchFrequency", TableInfo.Column("matchFrequency", "TEXT",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRfAutomationRules.put("actionType", TableInfo.Column("actionType", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRfAutomationRules.put("actionPayload", TableInfo.Column("actionPayload", "TEXT",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRfAutomationRules.put("createdAt", TableInfo.Column("createdAt", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysRfAutomationRules: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesRfAutomationRules: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoRfAutomationRules: TableInfo = TableInfo("rf_automation_rules",
            _columnsRfAutomationRules, _foreignKeysRfAutomationRules, _indicesRfAutomationRules)
        val _existingRfAutomationRules: TableInfo = read(connection, "rf_automation_rules")
        if (!_infoRfAutomationRules.equals(_existingRfAutomationRules)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |rf_automation_rules(com.embedsuite.app.data.RfAutomationRuleEntity).
              | Expected:
              |""".trimMargin() + _infoRfAutomationRules + """
              |
              | Found:
              |""".trimMargin() + _existingRfAutomationRules)
        }
        val _columnsScripts: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsScripts.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsScripts.put("name", TableInfo.Column("name", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsScripts.put("category", TableInfo.Column("category", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsScripts.put("description", TableInfo.Column("description", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsScripts.put("author", TableInfo.Column("author", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsScripts.put("version", TableInfo.Column("version", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsScripts.put("dialect", TableInfo.Column("dialect", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsScripts.put("sourceCode", TableInfo.Column("sourceCode", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsScripts.put("isFavorite", TableInfo.Column("isFavorite", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsScripts.put("lastSyncAt", TableInfo.Column("lastSyncAt", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysScripts: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesScripts: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoScripts: TableInfo = TableInfo("scripts", _columnsScripts, _foreignKeysScripts,
            _indicesScripts)
        val _existingScripts: TableInfo = read(connection, "scripts")
        if (!_infoScripts.equals(_existingScripts)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |scripts(com.embedsuite.app.data.ScriptEntity).
              | Expected:
              |""".trimMargin() + _infoScripts + """
              |
              | Found:
              |""".trimMargin() + _existingScripts)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "captured_signals",
        "ir_buttons", "macros", "profiles", "tx_history", "nfc_dumps", "ble_profiles",
        "rf_automation_rules", "scripts")
  }

  public override fun clearAllTables() {
    super.performClear(false, "captured_signals", "ir_buttons", "macros", "profiles", "tx_history",
        "nfc_dumps", "ble_profiles", "rf_automation_rules", "scripts")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(CapturedSignalDao::class, CapturedSignalDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(IrButtonDao::class, IrButtonDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(MacroDao::class, MacroDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(ProfileDao::class, ProfileDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(TxHistoryDao::class, TxHistoryDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(NfcDumpDao::class, NfcDumpDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(BleProfileDao::class, BleProfileDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(RfAutomationDao::class, RfAutomationDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(ScriptDao::class, ScriptDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override
      fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>):
      List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun capturedSignalDao(): CapturedSignalDao = _capturedSignalDao.value

  public override fun irButtonDao(): IrButtonDao = _irButtonDao.value

  public override fun macroDao(): MacroDao = _macroDao.value

  public override fun profileDao(): ProfileDao = _profileDao.value

  public override fun txHistoryDao(): TxHistoryDao = _txHistoryDao.value

  public override fun nfcDumpDao(): NfcDumpDao = _nfcDumpDao.value

  public override fun bleProfileDao(): BleProfileDao = _bleProfileDao.value

  public override fun rfAutomationDao(): RfAutomationDao = _rfAutomationDao.value

  public override fun scriptDao(): ScriptDao = _scriptDao.value
}
