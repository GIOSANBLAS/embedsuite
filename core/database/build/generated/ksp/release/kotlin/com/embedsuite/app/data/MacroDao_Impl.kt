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
public class MacroDao_Impl(
  __db: RoomDatabase,
) : MacroDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfMacroEntity: EntityInsertAdapter<MacroEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfMacroEntity = object : EntityInsertAdapter<MacroEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR ABORT INTO `macros` (`id`,`name`,`commands`,`description`) VALUES (nullif(?, 0),?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: MacroEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindText(3, entity.commands)
        statement.bindText(4, entity.description)
      }
    }
  }

  public override suspend fun insert(macro: MacroEntity): Long = performSuspending(__db, false,
      true) { _connection ->
    val _result: Long = __insertAdapterOfMacroEntity.insertAndReturnId(_connection, macro)
    _result
  }

  public override fun observeAll(): Flow<List<MacroEntity>> {
    val _sql: String = "SELECT * FROM macros ORDER BY name"
    return createFlow(__db, false, arrayOf("macros")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfCommands: Int = getColumnIndexOrThrow(_stmt, "commands")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _result: MutableList<MacroEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: MacroEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpCommands: String
          _tmpCommands = _stmt.getText(_columnIndexOfCommands)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          _item = MacroEntity(_tmpId,_tmpName,_tmpCommands,_tmpDescription)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAll(): List<MacroEntity> {
    val _sql: String = "SELECT * FROM macros ORDER BY name"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfCommands: Int = getColumnIndexOrThrow(_stmt, "commands")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _result: MutableList<MacroEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: MacroEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpCommands: String
          _tmpCommands = _stmt.getText(_columnIndexOfCommands)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          _item = MacroEntity(_tmpId,_tmpName,_tmpCommands,_tmpDescription)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteById(id: Long) {
    val _sql: String = "DELETE FROM macros WHERE id = ?"
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
