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
public class IrButtonDao_Impl(
  __db: RoomDatabase,
) : IrButtonDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfIrButtonEntity: EntityInsertAdapter<IrButtonEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfIrButtonEntity = object : EntityInsertAdapter<IrButtonEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR ABORT INTO `ir_buttons` (`id`,`panelName`,`buttonName`,`protocol`,`hexCode`,`ir_payload`) VALUES (nullif(?, 0),?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: IrButtonEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.panelName)
        statement.bindText(3, entity.buttonName)
        statement.bindText(4, entity.protocol)
        statement.bindText(5, entity.hexCode)
        statement.bindText(6, entity.irPayload)
      }
    }
  }

  public override suspend fun insert(button: IrButtonEntity): Long = performSuspending(__db, false,
      true) { _connection ->
    val _result: Long = __insertAdapterOfIrButtonEntity.insertAndReturnId(_connection, button)
    _result
  }

  public override fun observeAll(): Flow<List<IrButtonEntity>> {
    val _sql: String = "SELECT * FROM ir_buttons ORDER BY panelName, buttonName"
    return createFlow(__db, false, arrayOf("ir_buttons")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfPanelName: Int = getColumnIndexOrThrow(_stmt, "panelName")
        val _columnIndexOfButtonName: Int = getColumnIndexOrThrow(_stmt, "buttonName")
        val _columnIndexOfProtocol: Int = getColumnIndexOrThrow(_stmt, "protocol")
        val _columnIndexOfHexCode: Int = getColumnIndexOrThrow(_stmt, "hexCode")
        val _columnIndexOfIrPayload: Int = getColumnIndexOrThrow(_stmt, "ir_payload")
        val _result: MutableList<IrButtonEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: IrButtonEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpPanelName: String
          _tmpPanelName = _stmt.getText(_columnIndexOfPanelName)
          val _tmpButtonName: String
          _tmpButtonName = _stmt.getText(_columnIndexOfButtonName)
          val _tmpProtocol: String
          _tmpProtocol = _stmt.getText(_columnIndexOfProtocol)
          val _tmpHexCode: String
          _tmpHexCode = _stmt.getText(_columnIndexOfHexCode)
          val _tmpIrPayload: String
          _tmpIrPayload = _stmt.getText(_columnIndexOfIrPayload)
          _item =
              IrButtonEntity(_tmpId,_tmpPanelName,_tmpButtonName,_tmpProtocol,_tmpHexCode,_tmpIrPayload)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAll(): List<IrButtonEntity> {
    val _sql: String = "SELECT * FROM ir_buttons ORDER BY panelName, buttonName"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfPanelName: Int = getColumnIndexOrThrow(_stmt, "panelName")
        val _columnIndexOfButtonName: Int = getColumnIndexOrThrow(_stmt, "buttonName")
        val _columnIndexOfProtocol: Int = getColumnIndexOrThrow(_stmt, "protocol")
        val _columnIndexOfHexCode: Int = getColumnIndexOrThrow(_stmt, "hexCode")
        val _columnIndexOfIrPayload: Int = getColumnIndexOrThrow(_stmt, "ir_payload")
        val _result: MutableList<IrButtonEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: IrButtonEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpPanelName: String
          _tmpPanelName = _stmt.getText(_columnIndexOfPanelName)
          val _tmpButtonName: String
          _tmpButtonName = _stmt.getText(_columnIndexOfButtonName)
          val _tmpProtocol: String
          _tmpProtocol = _stmt.getText(_columnIndexOfProtocol)
          val _tmpHexCode: String
          _tmpHexCode = _stmt.getText(_columnIndexOfHexCode)
          val _tmpIrPayload: String
          _tmpIrPayload = _stmt.getText(_columnIndexOfIrPayload)
          _item =
              IrButtonEntity(_tmpId,_tmpPanelName,_tmpButtonName,_tmpProtocol,_tmpHexCode,_tmpIrPayload)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteById(id: Long) {
    val _sql: String = "DELETE FROM ir_buttons WHERE id = ?"
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
