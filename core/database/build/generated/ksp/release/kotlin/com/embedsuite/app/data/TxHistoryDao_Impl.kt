package com.embedsuite.app.`data`

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
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class TxHistoryDao_Impl(
  __db: RoomDatabase,
) : TxHistoryDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfTxHistoryEntity: EntityInsertAdapter<TxHistoryEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfTxHistoryEntity = object : EntityInsertAdapter<TxHistoryEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR ABORT INTO `tx_history` (`id`,`signalId`,`label`,`protocol`,`command`,`success`,`timestamp`) VALUES (nullif(?, 0),?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: TxHistoryEntity) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.signalId)
        statement.bindText(3, entity.label)
        statement.bindText(4, entity.protocol)
        statement.bindText(5, entity.command)
        val _tmp: Int = if (entity.success) 1 else 0
        statement.bindLong(6, _tmp.toLong())
        statement.bindLong(7, entity.timestamp)
      }
    }
  }

  public override suspend fun insert(entry: TxHistoryEntity): Long = performSuspending(__db, false,
      true) { _connection ->
    val _result: Long = __insertAdapterOfTxHistoryEntity.insertAndReturnId(_connection, entry)
    _result
  }

  public override fun observeRecent(limit: Int): Flow<List<TxHistoryEntity>> {
    val _sql: String = "SELECT * FROM tx_history ORDER BY timestamp DESC LIMIT ?"
    return createFlow(__db, false, arrayOf("tx_history")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, limit.toLong())
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSignalId: Int = getColumnIndexOrThrow(_stmt, "signalId")
        val _columnIndexOfLabel: Int = getColumnIndexOrThrow(_stmt, "label")
        val _columnIndexOfProtocol: Int = getColumnIndexOrThrow(_stmt, "protocol")
        val _columnIndexOfCommand: Int = getColumnIndexOrThrow(_stmt, "command")
        val _columnIndexOfSuccess: Int = getColumnIndexOrThrow(_stmt, "success")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _result: MutableList<TxHistoryEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: TxHistoryEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpSignalId: Long
          _tmpSignalId = _stmt.getLong(_columnIndexOfSignalId)
          val _tmpLabel: String
          _tmpLabel = _stmt.getText(_columnIndexOfLabel)
          val _tmpProtocol: String
          _tmpProtocol = _stmt.getText(_columnIndexOfProtocol)
          val _tmpCommand: String
          _tmpCommand = _stmt.getText(_columnIndexOfCommand)
          val _tmpSuccess: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfSuccess).toInt()
          _tmpSuccess = _tmp != 0
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          _item =
              TxHistoryEntity(_tmpId,_tmpSignalId,_tmpLabel,_tmpProtocol,_tmpCommand,_tmpSuccess,_tmpTimestamp)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getRecent(limit: Int): List<TxHistoryEntity> {
    val _sql: String = "SELECT * FROM tx_history ORDER BY timestamp DESC LIMIT ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, limit.toLong())
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSignalId: Int = getColumnIndexOrThrow(_stmt, "signalId")
        val _columnIndexOfLabel: Int = getColumnIndexOrThrow(_stmt, "label")
        val _columnIndexOfProtocol: Int = getColumnIndexOrThrow(_stmt, "protocol")
        val _columnIndexOfCommand: Int = getColumnIndexOrThrow(_stmt, "command")
        val _columnIndexOfSuccess: Int = getColumnIndexOrThrow(_stmt, "success")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _result: MutableList<TxHistoryEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: TxHistoryEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpSignalId: Long
          _tmpSignalId = _stmt.getLong(_columnIndexOfSignalId)
          val _tmpLabel: String
          _tmpLabel = _stmt.getText(_columnIndexOfLabel)
          val _tmpProtocol: String
          _tmpProtocol = _stmt.getText(_columnIndexOfProtocol)
          val _tmpCommand: String
          _tmpCommand = _stmt.getText(_columnIndexOfCommand)
          val _tmpSuccess: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfSuccess).toInt()
          _tmpSuccess = _tmp != 0
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          _item =
              TxHistoryEntity(_tmpId,_tmpSignalId,_tmpLabel,_tmpProtocol,_tmpCommand,_tmpSuccess,_tmpTimestamp)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getSince(since: Long): List<TxHistoryEntity> {
    val _sql: String = "SELECT * FROM tx_history WHERE timestamp >= ? ORDER BY timestamp DESC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, since)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSignalId: Int = getColumnIndexOrThrow(_stmt, "signalId")
        val _columnIndexOfLabel: Int = getColumnIndexOrThrow(_stmt, "label")
        val _columnIndexOfProtocol: Int = getColumnIndexOrThrow(_stmt, "protocol")
        val _columnIndexOfCommand: Int = getColumnIndexOrThrow(_stmt, "command")
        val _columnIndexOfSuccess: Int = getColumnIndexOrThrow(_stmt, "success")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _result: MutableList<TxHistoryEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: TxHistoryEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpSignalId: Long
          _tmpSignalId = _stmt.getLong(_columnIndexOfSignalId)
          val _tmpLabel: String
          _tmpLabel = _stmt.getText(_columnIndexOfLabel)
          val _tmpProtocol: String
          _tmpProtocol = _stmt.getText(_columnIndexOfProtocol)
          val _tmpCommand: String
          _tmpCommand = _stmt.getText(_columnIndexOfCommand)
          val _tmpSuccess: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfSuccess).toInt()
          _tmpSuccess = _tmp != 0
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          _item =
              TxHistoryEntity(_tmpId,_tmpSignalId,_tmpLabel,_tmpProtocol,_tmpCommand,_tmpSuccess,_tmpTimestamp)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAll(): List<TxHistoryEntity> {
    val _sql: String = "SELECT * FROM tx_history ORDER BY timestamp DESC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSignalId: Int = getColumnIndexOrThrow(_stmt, "signalId")
        val _columnIndexOfLabel: Int = getColumnIndexOrThrow(_stmt, "label")
        val _columnIndexOfProtocol: Int = getColumnIndexOrThrow(_stmt, "protocol")
        val _columnIndexOfCommand: Int = getColumnIndexOrThrow(_stmt, "command")
        val _columnIndexOfSuccess: Int = getColumnIndexOrThrow(_stmt, "success")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _result: MutableList<TxHistoryEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: TxHistoryEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpSignalId: Long
          _tmpSignalId = _stmt.getLong(_columnIndexOfSignalId)
          val _tmpLabel: String
          _tmpLabel = _stmt.getText(_columnIndexOfLabel)
          val _tmpProtocol: String
          _tmpProtocol = _stmt.getText(_columnIndexOfProtocol)
          val _tmpCommand: String
          _tmpCommand = _stmt.getText(_columnIndexOfCommand)
          val _tmpSuccess: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfSuccess).toInt()
          _tmpSuccess = _tmp != 0
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          _item =
              TxHistoryEntity(_tmpId,_tmpSignalId,_tmpLabel,_tmpProtocol,_tmpCommand,_tmpSuccess,_tmpTimestamp)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun trimOld() {
    val _sql: String =
        "DELETE FROM tx_history WHERE id NOT IN (SELECT id FROM tx_history ORDER BY timestamp DESC LIMIT 50)"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
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
