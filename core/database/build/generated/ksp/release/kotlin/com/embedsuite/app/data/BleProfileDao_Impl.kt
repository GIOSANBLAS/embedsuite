package com.embedsuite.app.`data`

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class BleProfileDao_Impl(
  __db: RoomDatabase,
) : BleProfileDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfBleProfileEntity: EntityInsertAdapter<BleProfileEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfBleProfileEntity = object : EntityInsertAdapter<BleProfileEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR ABORT INTO `ble_profiles` (`id`,`name`,`address`,`services`,`manufacturerData`,`notes`,`timestamp`) VALUES (nullif(?, 0),?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: BleProfileEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindText(3, entity.address)
        statement.bindText(4, entity.services)
        statement.bindText(5, entity.manufacturerData)
        statement.bindText(6, entity.notes)
        statement.bindLong(7, entity.timestamp)
      }
    }
  }

  public override suspend fun insert(profile: BleProfileEntity): Long = performSuspending(__db,
      false, true) { _connection ->
    val _result: Long = __insertAdapterOfBleProfileEntity.insertAndReturnId(_connection, profile)
    _result
  }

  public override fun observeAll(): Flow<List<BleProfileEntity>> {
    val _sql: String = "SELECT * FROM ble_profiles ORDER BY timestamp DESC"
    return createFlow(__db, false, arrayOf("ble_profiles")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfAddress: Int = getColumnIndexOrThrow(_stmt, "address")
        val _columnIndexOfServices: Int = getColumnIndexOrThrow(_stmt, "services")
        val _columnIndexOfManufacturerData: Int = getColumnIndexOrThrow(_stmt, "manufacturerData")
        val _columnIndexOfNotes: Int = getColumnIndexOrThrow(_stmt, "notes")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _result: MutableList<BleProfileEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: BleProfileEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpAddress: String
          _tmpAddress = _stmt.getText(_columnIndexOfAddress)
          val _tmpServices: String
          _tmpServices = _stmt.getText(_columnIndexOfServices)
          val _tmpManufacturerData: String
          _tmpManufacturerData = _stmt.getText(_columnIndexOfManufacturerData)
          val _tmpNotes: String
          _tmpNotes = _stmt.getText(_columnIndexOfNotes)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          _item =
              BleProfileEntity(_tmpId,_tmpName,_tmpAddress,_tmpServices,_tmpManufacturerData,_tmpNotes,_tmpTimestamp)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAll(): List<BleProfileEntity> {
    val _sql: String = "SELECT * FROM ble_profiles ORDER BY timestamp DESC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfAddress: Int = getColumnIndexOrThrow(_stmt, "address")
        val _columnIndexOfServices: Int = getColumnIndexOrThrow(_stmt, "services")
        val _columnIndexOfManufacturerData: Int = getColumnIndexOrThrow(_stmt, "manufacturerData")
        val _columnIndexOfNotes: Int = getColumnIndexOrThrow(_stmt, "notes")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _result: MutableList<BleProfileEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: BleProfileEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpAddress: String
          _tmpAddress = _stmt.getText(_columnIndexOfAddress)
          val _tmpServices: String
          _tmpServices = _stmt.getText(_columnIndexOfServices)
          val _tmpManufacturerData: String
          _tmpManufacturerData = _stmt.getText(_columnIndexOfManufacturerData)
          val _tmpNotes: String
          _tmpNotes = _stmt.getText(_columnIndexOfNotes)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          _item =
              BleProfileEntity(_tmpId,_tmpName,_tmpAddress,_tmpServices,_tmpManufacturerData,_tmpNotes,_tmpTimestamp)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteById(id: Long) {
    val _sql: String = "DELETE FROM ble_profiles WHERE id = ?"
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
