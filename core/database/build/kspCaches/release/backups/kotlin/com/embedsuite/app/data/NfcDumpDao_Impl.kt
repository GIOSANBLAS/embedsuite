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
public class NfcDumpDao_Impl(
  __db: RoomDatabase,
) : NfcDumpDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfNfcDumpEntity: EntityInsertAdapter<NfcDumpEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfNfcDumpEntity = object : EntityInsertAdapter<NfcDumpEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR ABORT INTO `nfc_dumps` (`id`,`uid`,`tagType`,`rawDump`,`parsedSectors`,`timestamp`) VALUES (nullif(?, 0),?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: NfcDumpEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.uid)
        statement.bindText(3, entity.tagType)
        statement.bindText(4, entity.rawDump)
        statement.bindText(5, entity.parsedSectors)
        statement.bindLong(6, entity.timestamp)
      }
    }
  }

  public override suspend fun insert(dump: NfcDumpEntity): Long = performSuspending(__db, false,
      true) { _connection ->
    val _result: Long = __insertAdapterOfNfcDumpEntity.insertAndReturnId(_connection, dump)
    _result
  }

  public override fun observeAll(): Flow<List<NfcDumpEntity>> {
    val _sql: String = "SELECT * FROM nfc_dumps ORDER BY timestamp DESC"
    return createFlow(__db, false, arrayOf("nfc_dumps")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfUid: Int = getColumnIndexOrThrow(_stmt, "uid")
        val _columnIndexOfTagType: Int = getColumnIndexOrThrow(_stmt, "tagType")
        val _columnIndexOfRawDump: Int = getColumnIndexOrThrow(_stmt, "rawDump")
        val _columnIndexOfParsedSectors: Int = getColumnIndexOrThrow(_stmt, "parsedSectors")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _result: MutableList<NfcDumpEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: NfcDumpEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpUid: String
          _tmpUid = _stmt.getText(_columnIndexOfUid)
          val _tmpTagType: String
          _tmpTagType = _stmt.getText(_columnIndexOfTagType)
          val _tmpRawDump: String
          _tmpRawDump = _stmt.getText(_columnIndexOfRawDump)
          val _tmpParsedSectors: String
          _tmpParsedSectors = _stmt.getText(_columnIndexOfParsedSectors)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          _item =
              NfcDumpEntity(_tmpId,_tmpUid,_tmpTagType,_tmpRawDump,_tmpParsedSectors,_tmpTimestamp)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAll(): List<NfcDumpEntity> {
    val _sql: String = "SELECT * FROM nfc_dumps ORDER BY timestamp DESC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfUid: Int = getColumnIndexOrThrow(_stmt, "uid")
        val _columnIndexOfTagType: Int = getColumnIndexOrThrow(_stmt, "tagType")
        val _columnIndexOfRawDump: Int = getColumnIndexOrThrow(_stmt, "rawDump")
        val _columnIndexOfParsedSectors: Int = getColumnIndexOrThrow(_stmt, "parsedSectors")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _result: MutableList<NfcDumpEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: NfcDumpEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpUid: String
          _tmpUid = _stmt.getText(_columnIndexOfUid)
          val _tmpTagType: String
          _tmpTagType = _stmt.getText(_columnIndexOfTagType)
          val _tmpRawDump: String
          _tmpRawDump = _stmt.getText(_columnIndexOfRawDump)
          val _tmpParsedSectors: String
          _tmpParsedSectors = _stmt.getText(_columnIndexOfParsedSectors)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          _item =
              NfcDumpEntity(_tmpId,_tmpUid,_tmpTagType,_tmpRawDump,_tmpParsedSectors,_tmpTimestamp)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getById(id: Long): NfcDumpEntity? {
    val _sql: String = "SELECT * FROM nfc_dumps WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfUid: Int = getColumnIndexOrThrow(_stmt, "uid")
        val _columnIndexOfTagType: Int = getColumnIndexOrThrow(_stmt, "tagType")
        val _columnIndexOfRawDump: Int = getColumnIndexOrThrow(_stmt, "rawDump")
        val _columnIndexOfParsedSectors: Int = getColumnIndexOrThrow(_stmt, "parsedSectors")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _result: NfcDumpEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpUid: String
          _tmpUid = _stmt.getText(_columnIndexOfUid)
          val _tmpTagType: String
          _tmpTagType = _stmt.getText(_columnIndexOfTagType)
          val _tmpRawDump: String
          _tmpRawDump = _stmt.getText(_columnIndexOfRawDump)
          val _tmpParsedSectors: String
          _tmpParsedSectors = _stmt.getText(_columnIndexOfParsedSectors)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          _result =
              NfcDumpEntity(_tmpId,_tmpUid,_tmpTagType,_tmpRawDump,_tmpParsedSectors,_tmpTimestamp)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteById(id: Long) {
    val _sql: String = "DELETE FROM nfc_dumps WHERE id = ?"
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
