package com.embedsuite.app.`data`

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class RfAutomationDao_Impl(
  __db: RoomDatabase,
) : RfAutomationDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfRfAutomationRuleEntity: EntityInsertAdapter<RfAutomationRuleEntity>

  private val __updateAdapterOfRfAutomationRuleEntity:
      EntityDeleteOrUpdateAdapter<RfAutomationRuleEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfRfAutomationRuleEntity = object :
        EntityInsertAdapter<RfAutomationRuleEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR ABORT INTO `rf_automation_rules` (`id`,`name`,`enabled`,`matchProtocol`,`matchFrequency`,`actionType`,`actionPayload`,`createdAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: RfAutomationRuleEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.name)
        val _tmp: Int = if (entity.enabled) 1 else 0
        statement.bindLong(3, _tmp.toLong())
        statement.bindText(4, entity.matchProtocol)
        statement.bindText(5, entity.matchFrequency)
        statement.bindText(6, entity.actionType)
        statement.bindText(7, entity.actionPayload)
        statement.bindLong(8, entity.createdAt)
      }
    }
    this.__updateAdapterOfRfAutomationRuleEntity = object :
        EntityDeleteOrUpdateAdapter<RfAutomationRuleEntity>() {
      protected override fun createQuery(): String =
          "UPDATE OR ABORT `rf_automation_rules` SET `id` = ?,`name` = ?,`enabled` = ?,`matchProtocol` = ?,`matchFrequency` = ?,`actionType` = ?,`actionPayload` = ?,`createdAt` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: RfAutomationRuleEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.name)
        val _tmp: Int = if (entity.enabled) 1 else 0
        statement.bindLong(3, _tmp.toLong())
        statement.bindText(4, entity.matchProtocol)
        statement.bindText(5, entity.matchFrequency)
        statement.bindText(6, entity.actionType)
        statement.bindText(7, entity.actionPayload)
        statement.bindLong(8, entity.createdAt)
        statement.bindLong(9, entity.id)
      }
    }
  }

  public override suspend fun insert(rule: RfAutomationRuleEntity): Long = performSuspending(__db,
      false, true) { _connection ->
    val _result: Long = __insertAdapterOfRfAutomationRuleEntity.insertAndReturnId(_connection, rule)
    _result
  }

  public override suspend fun update(rule: RfAutomationRuleEntity): Unit = performSuspending(__db,
      false, true) { _connection ->
    __updateAdapterOfRfAutomationRuleEntity.handle(_connection, rule)
  }

  public override fun observeAll(): Flow<List<RfAutomationRuleEntity>> {
    val _sql: String = "SELECT * FROM rf_automation_rules ORDER BY createdAt DESC"
    return createFlow(__db, false, arrayOf("rf_automation_rules")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfEnabled: Int = getColumnIndexOrThrow(_stmt, "enabled")
        val _columnIndexOfMatchProtocol: Int = getColumnIndexOrThrow(_stmt, "matchProtocol")
        val _columnIndexOfMatchFrequency: Int = getColumnIndexOrThrow(_stmt, "matchFrequency")
        val _columnIndexOfActionType: Int = getColumnIndexOrThrow(_stmt, "actionType")
        val _columnIndexOfActionPayload: Int = getColumnIndexOrThrow(_stmt, "actionPayload")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _result: MutableList<RfAutomationRuleEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: RfAutomationRuleEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpEnabled: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfEnabled).toInt()
          _tmpEnabled = _tmp != 0
          val _tmpMatchProtocol: String
          _tmpMatchProtocol = _stmt.getText(_columnIndexOfMatchProtocol)
          val _tmpMatchFrequency: String
          _tmpMatchFrequency = _stmt.getText(_columnIndexOfMatchFrequency)
          val _tmpActionType: String
          _tmpActionType = _stmt.getText(_columnIndexOfActionType)
          val _tmpActionPayload: String
          _tmpActionPayload = _stmt.getText(_columnIndexOfActionPayload)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _item =
              RfAutomationRuleEntity(_tmpId,_tmpName,_tmpEnabled,_tmpMatchProtocol,_tmpMatchFrequency,_tmpActionType,_tmpActionPayload,_tmpCreatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getEnabled(): List<RfAutomationRuleEntity> {
    val _sql: String = "SELECT * FROM rf_automation_rules WHERE enabled = 1 ORDER BY createdAt DESC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfEnabled: Int = getColumnIndexOrThrow(_stmt, "enabled")
        val _columnIndexOfMatchProtocol: Int = getColumnIndexOrThrow(_stmt, "matchProtocol")
        val _columnIndexOfMatchFrequency: Int = getColumnIndexOrThrow(_stmt, "matchFrequency")
        val _columnIndexOfActionType: Int = getColumnIndexOrThrow(_stmt, "actionType")
        val _columnIndexOfActionPayload: Int = getColumnIndexOrThrow(_stmt, "actionPayload")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _result: MutableList<RfAutomationRuleEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: RfAutomationRuleEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpEnabled: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfEnabled).toInt()
          _tmpEnabled = _tmp != 0
          val _tmpMatchProtocol: String
          _tmpMatchProtocol = _stmt.getText(_columnIndexOfMatchProtocol)
          val _tmpMatchFrequency: String
          _tmpMatchFrequency = _stmt.getText(_columnIndexOfMatchFrequency)
          val _tmpActionType: String
          _tmpActionType = _stmt.getText(_columnIndexOfActionType)
          val _tmpActionPayload: String
          _tmpActionPayload = _stmt.getText(_columnIndexOfActionPayload)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _item =
              RfAutomationRuleEntity(_tmpId,_tmpName,_tmpEnabled,_tmpMatchProtocol,_tmpMatchFrequency,_tmpActionType,_tmpActionPayload,_tmpCreatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAll(): List<RfAutomationRuleEntity> {
    val _sql: String = "SELECT * FROM rf_automation_rules ORDER BY createdAt DESC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfEnabled: Int = getColumnIndexOrThrow(_stmt, "enabled")
        val _columnIndexOfMatchProtocol: Int = getColumnIndexOrThrow(_stmt, "matchProtocol")
        val _columnIndexOfMatchFrequency: Int = getColumnIndexOrThrow(_stmt, "matchFrequency")
        val _columnIndexOfActionType: Int = getColumnIndexOrThrow(_stmt, "actionType")
        val _columnIndexOfActionPayload: Int = getColumnIndexOrThrow(_stmt, "actionPayload")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _result: MutableList<RfAutomationRuleEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: RfAutomationRuleEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpEnabled: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfEnabled).toInt()
          _tmpEnabled = _tmp != 0
          val _tmpMatchProtocol: String
          _tmpMatchProtocol = _stmt.getText(_columnIndexOfMatchProtocol)
          val _tmpMatchFrequency: String
          _tmpMatchFrequency = _stmt.getText(_columnIndexOfMatchFrequency)
          val _tmpActionType: String
          _tmpActionType = _stmt.getText(_columnIndexOfActionType)
          val _tmpActionPayload: String
          _tmpActionPayload = _stmt.getText(_columnIndexOfActionPayload)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _item =
              RfAutomationRuleEntity(_tmpId,_tmpName,_tmpEnabled,_tmpMatchProtocol,_tmpMatchFrequency,_tmpActionType,_tmpActionPayload,_tmpCreatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteById(id: Long) {
    val _sql: String = "DELETE FROM rf_automation_rules WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
