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
import kotlin.Double
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
public class CapturedSignalDao_Impl(
  __db: RoomDatabase,
) : CapturedSignalDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfCapturedSignalEntity: EntityInsertAdapter<CapturedSignalEntity>

  private val __updateAdapterOfCapturedSignalEntity:
      EntityDeleteOrUpdateAdapter<CapturedSignalEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfCapturedSignalEntity = object :
        EntityInsertAdapter<CapturedSignalEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR ABORT INTO `captured_signals` (`id`,`timestamp`,`signalType`,`name`,`label`,`tags`,`frequency`,`protocol`,`deviceId`,`macAddress`,`rssi`,`latitude`,`longitude`,`rawData`,`detail`,`decodedFields`,`favorite`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: CapturedSignalEntity) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.timestamp)
        statement.bindText(3, entity.signalType)
        statement.bindText(4, entity.name)
        statement.bindText(5, entity.label)
        statement.bindText(6, entity.tags)
        statement.bindText(7, entity.frequency)
        statement.bindText(8, entity.protocol)
        statement.bindText(9, entity.deviceId)
        statement.bindText(10, entity.macAddress)
        statement.bindLong(11, entity.rssi.toLong())
        val _tmpLatitude: Double? = entity.latitude
        if (_tmpLatitude == null) {
          statement.bindNull(12)
        } else {
          statement.bindDouble(12, _tmpLatitude)
        }
        val _tmpLongitude: Double? = entity.longitude
        if (_tmpLongitude == null) {
          statement.bindNull(13)
        } else {
          statement.bindDouble(13, _tmpLongitude)
        }
        statement.bindText(14, entity.rawData)
        statement.bindText(15, entity.detail)
        statement.bindText(16, entity.decodedFields)
        val _tmp: Int = if (entity.favorite) 1 else 0
        statement.bindLong(17, _tmp.toLong())
      }
    }
    this.__updateAdapterOfCapturedSignalEntity = object :
        EntityDeleteOrUpdateAdapter<CapturedSignalEntity>() {
      protected override fun createQuery(): String =
          "UPDATE OR ABORT `captured_signals` SET `id` = ?,`timestamp` = ?,`signalType` = ?,`name` = ?,`label` = ?,`tags` = ?,`frequency` = ?,`protocol` = ?,`deviceId` = ?,`macAddress` = ?,`rssi` = ?,`latitude` = ?,`longitude` = ?,`rawData` = ?,`detail` = ?,`decodedFields` = ?,`favorite` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: CapturedSignalEntity) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.timestamp)
        statement.bindText(3, entity.signalType)
        statement.bindText(4, entity.name)
        statement.bindText(5, entity.label)
        statement.bindText(6, entity.tags)
        statement.bindText(7, entity.frequency)
        statement.bindText(8, entity.protocol)
        statement.bindText(9, entity.deviceId)
        statement.bindText(10, entity.macAddress)
        statement.bindLong(11, entity.rssi.toLong())
        val _tmpLatitude: Double? = entity.latitude
        if (_tmpLatitude == null) {
          statement.bindNull(12)
        } else {
          statement.bindDouble(12, _tmpLatitude)
        }
        val _tmpLongitude: Double? = entity.longitude
        if (_tmpLongitude == null) {
          statement.bindNull(13)
        } else {
          statement.bindDouble(13, _tmpLongitude)
        }
        statement.bindText(14, entity.rawData)
        statement.bindText(15, entity.detail)
        statement.bindText(16, entity.decodedFields)
        val _tmp: Int = if (entity.favorite) 1 else 0
        statement.bindLong(17, _tmp.toLong())
        statement.bindLong(18, entity.id)
      }
    }
  }

  public override suspend fun insert(signal: CapturedSignalEntity): Long = performSuspending(__db,
      false, true) { _connection ->
    val _result: Long = __insertAdapterOfCapturedSignalEntity.insertAndReturnId(_connection, signal)
    _result
  }

  public override suspend fun update(signal: CapturedSignalEntity): Unit = performSuspending(__db,
      false, true) { _connection ->
    __updateAdapterOfCapturedSignalEntity.handle(_connection, signal)
  }

  public override fun observeAll(): Flow<List<CapturedSignalEntity>> {
    val _sql: String = "SELECT * FROM captured_signals ORDER BY timestamp DESC"
    return createFlow(__db, false, arrayOf("captured_signals")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfSignalType: Int = getColumnIndexOrThrow(_stmt, "signalType")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfLabel: Int = getColumnIndexOrThrow(_stmt, "label")
        val _columnIndexOfTags: Int = getColumnIndexOrThrow(_stmt, "tags")
        val _columnIndexOfFrequency: Int = getColumnIndexOrThrow(_stmt, "frequency")
        val _columnIndexOfProtocol: Int = getColumnIndexOrThrow(_stmt, "protocol")
        val _columnIndexOfDeviceId: Int = getColumnIndexOrThrow(_stmt, "deviceId")
        val _columnIndexOfMacAddress: Int = getColumnIndexOrThrow(_stmt, "macAddress")
        val _columnIndexOfRssi: Int = getColumnIndexOrThrow(_stmt, "rssi")
        val _columnIndexOfLatitude: Int = getColumnIndexOrThrow(_stmt, "latitude")
        val _columnIndexOfLongitude: Int = getColumnIndexOrThrow(_stmt, "longitude")
        val _columnIndexOfRawData: Int = getColumnIndexOrThrow(_stmt, "rawData")
        val _columnIndexOfDetail: Int = getColumnIndexOrThrow(_stmt, "detail")
        val _columnIndexOfDecodedFields: Int = getColumnIndexOrThrow(_stmt, "decodedFields")
        val _columnIndexOfFavorite: Int = getColumnIndexOrThrow(_stmt, "favorite")
        val _result: MutableList<CapturedSignalEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: CapturedSignalEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          val _tmpSignalType: String
          _tmpSignalType = _stmt.getText(_columnIndexOfSignalType)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpLabel: String
          _tmpLabel = _stmt.getText(_columnIndexOfLabel)
          val _tmpTags: String
          _tmpTags = _stmt.getText(_columnIndexOfTags)
          val _tmpFrequency: String
          _tmpFrequency = _stmt.getText(_columnIndexOfFrequency)
          val _tmpProtocol: String
          _tmpProtocol = _stmt.getText(_columnIndexOfProtocol)
          val _tmpDeviceId: String
          _tmpDeviceId = _stmt.getText(_columnIndexOfDeviceId)
          val _tmpMacAddress: String
          _tmpMacAddress = _stmt.getText(_columnIndexOfMacAddress)
          val _tmpRssi: Int
          _tmpRssi = _stmt.getLong(_columnIndexOfRssi).toInt()
          val _tmpLatitude: Double?
          if (_stmt.isNull(_columnIndexOfLatitude)) {
            _tmpLatitude = null
          } else {
            _tmpLatitude = _stmt.getDouble(_columnIndexOfLatitude)
          }
          val _tmpLongitude: Double?
          if (_stmt.isNull(_columnIndexOfLongitude)) {
            _tmpLongitude = null
          } else {
            _tmpLongitude = _stmt.getDouble(_columnIndexOfLongitude)
          }
          val _tmpRawData: String
          _tmpRawData = _stmt.getText(_columnIndexOfRawData)
          val _tmpDetail: String
          _tmpDetail = _stmt.getText(_columnIndexOfDetail)
          val _tmpDecodedFields: String
          _tmpDecodedFields = _stmt.getText(_columnIndexOfDecodedFields)
          val _tmpFavorite: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfFavorite).toInt()
          _tmpFavorite = _tmp != 0
          _item =
              CapturedSignalEntity(_tmpId,_tmpTimestamp,_tmpSignalType,_tmpName,_tmpLabel,_tmpTags,_tmpFrequency,_tmpProtocol,_tmpDeviceId,_tmpMacAddress,_tmpRssi,_tmpLatitude,_tmpLongitude,_tmpRawData,_tmpDetail,_tmpDecodedFields,_tmpFavorite)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeWithLocation(): Flow<List<CapturedSignalEntity>> {
    val _sql: String =
        "SELECT * FROM captured_signals WHERE latitude IS NOT NULL AND longitude IS NOT NULL ORDER BY timestamp DESC"
    return createFlow(__db, false, arrayOf("captured_signals")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfSignalType: Int = getColumnIndexOrThrow(_stmt, "signalType")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfLabel: Int = getColumnIndexOrThrow(_stmt, "label")
        val _columnIndexOfTags: Int = getColumnIndexOrThrow(_stmt, "tags")
        val _columnIndexOfFrequency: Int = getColumnIndexOrThrow(_stmt, "frequency")
        val _columnIndexOfProtocol: Int = getColumnIndexOrThrow(_stmt, "protocol")
        val _columnIndexOfDeviceId: Int = getColumnIndexOrThrow(_stmt, "deviceId")
        val _columnIndexOfMacAddress: Int = getColumnIndexOrThrow(_stmt, "macAddress")
        val _columnIndexOfRssi: Int = getColumnIndexOrThrow(_stmt, "rssi")
        val _columnIndexOfLatitude: Int = getColumnIndexOrThrow(_stmt, "latitude")
        val _columnIndexOfLongitude: Int = getColumnIndexOrThrow(_stmt, "longitude")
        val _columnIndexOfRawData: Int = getColumnIndexOrThrow(_stmt, "rawData")
        val _columnIndexOfDetail: Int = getColumnIndexOrThrow(_stmt, "detail")
        val _columnIndexOfDecodedFields: Int = getColumnIndexOrThrow(_stmt, "decodedFields")
        val _columnIndexOfFavorite: Int = getColumnIndexOrThrow(_stmt, "favorite")
        val _result: MutableList<CapturedSignalEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: CapturedSignalEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          val _tmpSignalType: String
          _tmpSignalType = _stmt.getText(_columnIndexOfSignalType)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpLabel: String
          _tmpLabel = _stmt.getText(_columnIndexOfLabel)
          val _tmpTags: String
          _tmpTags = _stmt.getText(_columnIndexOfTags)
          val _tmpFrequency: String
          _tmpFrequency = _stmt.getText(_columnIndexOfFrequency)
          val _tmpProtocol: String
          _tmpProtocol = _stmt.getText(_columnIndexOfProtocol)
          val _tmpDeviceId: String
          _tmpDeviceId = _stmt.getText(_columnIndexOfDeviceId)
          val _tmpMacAddress: String
          _tmpMacAddress = _stmt.getText(_columnIndexOfMacAddress)
          val _tmpRssi: Int
          _tmpRssi = _stmt.getLong(_columnIndexOfRssi).toInt()
          val _tmpLatitude: Double?
          if (_stmt.isNull(_columnIndexOfLatitude)) {
            _tmpLatitude = null
          } else {
            _tmpLatitude = _stmt.getDouble(_columnIndexOfLatitude)
          }
          val _tmpLongitude: Double?
          if (_stmt.isNull(_columnIndexOfLongitude)) {
            _tmpLongitude = null
          } else {
            _tmpLongitude = _stmt.getDouble(_columnIndexOfLongitude)
          }
          val _tmpRawData: String
          _tmpRawData = _stmt.getText(_columnIndexOfRawData)
          val _tmpDetail: String
          _tmpDetail = _stmt.getText(_columnIndexOfDetail)
          val _tmpDecodedFields: String
          _tmpDecodedFields = _stmt.getText(_columnIndexOfDecodedFields)
          val _tmpFavorite: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfFavorite).toInt()
          _tmpFavorite = _tmp != 0
          _item =
              CapturedSignalEntity(_tmpId,_tmpTimestamp,_tmpSignalType,_tmpName,_tmpLabel,_tmpTags,_tmpFrequency,_tmpProtocol,_tmpDeviceId,_tmpMacAddress,_tmpRssi,_tmpLatitude,_tmpLongitude,_tmpRawData,_tmpDetail,_tmpDecodedFields,_tmpFavorite)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun search(query: String): Flow<List<CapturedSignalEntity>> {
    val _sql: String = """
        |
        |        SELECT * FROM captured_signals 
        |        WHERE name LIKE '%' || ? || '%' 
        |           OR label LIKE '%' || ? || '%'
        |           OR protocol LIKE '%' || ? || '%'
        |           OR tags LIKE '%' || ? || '%'
        |           OR deviceId LIKE '%' || ? || '%'
        |        ORDER BY timestamp DESC
        |    
        """.trimMargin()
    return createFlow(__db, false, arrayOf("captured_signals")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, query)
        _argIndex = 2
        _stmt.bindText(_argIndex, query)
        _argIndex = 3
        _stmt.bindText(_argIndex, query)
        _argIndex = 4
        _stmt.bindText(_argIndex, query)
        _argIndex = 5
        _stmt.bindText(_argIndex, query)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfSignalType: Int = getColumnIndexOrThrow(_stmt, "signalType")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfLabel: Int = getColumnIndexOrThrow(_stmt, "label")
        val _columnIndexOfTags: Int = getColumnIndexOrThrow(_stmt, "tags")
        val _columnIndexOfFrequency: Int = getColumnIndexOrThrow(_stmt, "frequency")
        val _columnIndexOfProtocol: Int = getColumnIndexOrThrow(_stmt, "protocol")
        val _columnIndexOfDeviceId: Int = getColumnIndexOrThrow(_stmt, "deviceId")
        val _columnIndexOfMacAddress: Int = getColumnIndexOrThrow(_stmt, "macAddress")
        val _columnIndexOfRssi: Int = getColumnIndexOrThrow(_stmt, "rssi")
        val _columnIndexOfLatitude: Int = getColumnIndexOrThrow(_stmt, "latitude")
        val _columnIndexOfLongitude: Int = getColumnIndexOrThrow(_stmt, "longitude")
        val _columnIndexOfRawData: Int = getColumnIndexOrThrow(_stmt, "rawData")
        val _columnIndexOfDetail: Int = getColumnIndexOrThrow(_stmt, "detail")
        val _columnIndexOfDecodedFields: Int = getColumnIndexOrThrow(_stmt, "decodedFields")
        val _columnIndexOfFavorite: Int = getColumnIndexOrThrow(_stmt, "favorite")
        val _result: MutableList<CapturedSignalEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: CapturedSignalEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          val _tmpSignalType: String
          _tmpSignalType = _stmt.getText(_columnIndexOfSignalType)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpLabel: String
          _tmpLabel = _stmt.getText(_columnIndexOfLabel)
          val _tmpTags: String
          _tmpTags = _stmt.getText(_columnIndexOfTags)
          val _tmpFrequency: String
          _tmpFrequency = _stmt.getText(_columnIndexOfFrequency)
          val _tmpProtocol: String
          _tmpProtocol = _stmt.getText(_columnIndexOfProtocol)
          val _tmpDeviceId: String
          _tmpDeviceId = _stmt.getText(_columnIndexOfDeviceId)
          val _tmpMacAddress: String
          _tmpMacAddress = _stmt.getText(_columnIndexOfMacAddress)
          val _tmpRssi: Int
          _tmpRssi = _stmt.getLong(_columnIndexOfRssi).toInt()
          val _tmpLatitude: Double?
          if (_stmt.isNull(_columnIndexOfLatitude)) {
            _tmpLatitude = null
          } else {
            _tmpLatitude = _stmt.getDouble(_columnIndexOfLatitude)
          }
          val _tmpLongitude: Double?
          if (_stmt.isNull(_columnIndexOfLongitude)) {
            _tmpLongitude = null
          } else {
            _tmpLongitude = _stmt.getDouble(_columnIndexOfLongitude)
          }
          val _tmpRawData: String
          _tmpRawData = _stmt.getText(_columnIndexOfRawData)
          val _tmpDetail: String
          _tmpDetail = _stmt.getText(_columnIndexOfDetail)
          val _tmpDecodedFields: String
          _tmpDecodedFields = _stmt.getText(_columnIndexOfDecodedFields)
          val _tmpFavorite: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfFavorite).toInt()
          _tmpFavorite = _tmp != 0
          _item =
              CapturedSignalEntity(_tmpId,_tmpTimestamp,_tmpSignalType,_tmpName,_tmpLabel,_tmpTags,_tmpFrequency,_tmpProtocol,_tmpDeviceId,_tmpMacAddress,_tmpRssi,_tmpLatitude,_tmpLongitude,_tmpRawData,_tmpDetail,_tmpDecodedFields,_tmpFavorite)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeByType(type: String): Flow<List<CapturedSignalEntity>> {
    val _sql: String = "SELECT * FROM captured_signals WHERE signalType = ? ORDER BY timestamp DESC"
    return createFlow(__db, false, arrayOf("captured_signals")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, type)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfSignalType: Int = getColumnIndexOrThrow(_stmt, "signalType")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfLabel: Int = getColumnIndexOrThrow(_stmt, "label")
        val _columnIndexOfTags: Int = getColumnIndexOrThrow(_stmt, "tags")
        val _columnIndexOfFrequency: Int = getColumnIndexOrThrow(_stmt, "frequency")
        val _columnIndexOfProtocol: Int = getColumnIndexOrThrow(_stmt, "protocol")
        val _columnIndexOfDeviceId: Int = getColumnIndexOrThrow(_stmt, "deviceId")
        val _columnIndexOfMacAddress: Int = getColumnIndexOrThrow(_stmt, "macAddress")
        val _columnIndexOfRssi: Int = getColumnIndexOrThrow(_stmt, "rssi")
        val _columnIndexOfLatitude: Int = getColumnIndexOrThrow(_stmt, "latitude")
        val _columnIndexOfLongitude: Int = getColumnIndexOrThrow(_stmt, "longitude")
        val _columnIndexOfRawData: Int = getColumnIndexOrThrow(_stmt, "rawData")
        val _columnIndexOfDetail: Int = getColumnIndexOrThrow(_stmt, "detail")
        val _columnIndexOfDecodedFields: Int = getColumnIndexOrThrow(_stmt, "decodedFields")
        val _columnIndexOfFavorite: Int = getColumnIndexOrThrow(_stmt, "favorite")
        val _result: MutableList<CapturedSignalEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: CapturedSignalEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          val _tmpSignalType: String
          _tmpSignalType = _stmt.getText(_columnIndexOfSignalType)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpLabel: String
          _tmpLabel = _stmt.getText(_columnIndexOfLabel)
          val _tmpTags: String
          _tmpTags = _stmt.getText(_columnIndexOfTags)
          val _tmpFrequency: String
          _tmpFrequency = _stmt.getText(_columnIndexOfFrequency)
          val _tmpProtocol: String
          _tmpProtocol = _stmt.getText(_columnIndexOfProtocol)
          val _tmpDeviceId: String
          _tmpDeviceId = _stmt.getText(_columnIndexOfDeviceId)
          val _tmpMacAddress: String
          _tmpMacAddress = _stmt.getText(_columnIndexOfMacAddress)
          val _tmpRssi: Int
          _tmpRssi = _stmt.getLong(_columnIndexOfRssi).toInt()
          val _tmpLatitude: Double?
          if (_stmt.isNull(_columnIndexOfLatitude)) {
            _tmpLatitude = null
          } else {
            _tmpLatitude = _stmt.getDouble(_columnIndexOfLatitude)
          }
          val _tmpLongitude: Double?
          if (_stmt.isNull(_columnIndexOfLongitude)) {
            _tmpLongitude = null
          } else {
            _tmpLongitude = _stmt.getDouble(_columnIndexOfLongitude)
          }
          val _tmpRawData: String
          _tmpRawData = _stmt.getText(_columnIndexOfRawData)
          val _tmpDetail: String
          _tmpDetail = _stmt.getText(_columnIndexOfDetail)
          val _tmpDecodedFields: String
          _tmpDecodedFields = _stmt.getText(_columnIndexOfDecodedFields)
          val _tmpFavorite: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfFavorite).toInt()
          _tmpFavorite = _tmp != 0
          _item =
              CapturedSignalEntity(_tmpId,_tmpTimestamp,_tmpSignalType,_tmpName,_tmpLabel,_tmpTags,_tmpFrequency,_tmpProtocol,_tmpDeviceId,_tmpMacAddress,_tmpRssi,_tmpLatitude,_tmpLongitude,_tmpRawData,_tmpDetail,_tmpDecodedFields,_tmpFavorite)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getRecent(limit: Int): List<CapturedSignalEntity> {
    val _sql: String = "SELECT * FROM captured_signals ORDER BY timestamp DESC LIMIT ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, limit.toLong())
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfSignalType: Int = getColumnIndexOrThrow(_stmt, "signalType")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfLabel: Int = getColumnIndexOrThrow(_stmt, "label")
        val _columnIndexOfTags: Int = getColumnIndexOrThrow(_stmt, "tags")
        val _columnIndexOfFrequency: Int = getColumnIndexOrThrow(_stmt, "frequency")
        val _columnIndexOfProtocol: Int = getColumnIndexOrThrow(_stmt, "protocol")
        val _columnIndexOfDeviceId: Int = getColumnIndexOrThrow(_stmt, "deviceId")
        val _columnIndexOfMacAddress: Int = getColumnIndexOrThrow(_stmt, "macAddress")
        val _columnIndexOfRssi: Int = getColumnIndexOrThrow(_stmt, "rssi")
        val _columnIndexOfLatitude: Int = getColumnIndexOrThrow(_stmt, "latitude")
        val _columnIndexOfLongitude: Int = getColumnIndexOrThrow(_stmt, "longitude")
        val _columnIndexOfRawData: Int = getColumnIndexOrThrow(_stmt, "rawData")
        val _columnIndexOfDetail: Int = getColumnIndexOrThrow(_stmt, "detail")
        val _columnIndexOfDecodedFields: Int = getColumnIndexOrThrow(_stmt, "decodedFields")
        val _columnIndexOfFavorite: Int = getColumnIndexOrThrow(_stmt, "favorite")
        val _result: MutableList<CapturedSignalEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: CapturedSignalEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          val _tmpSignalType: String
          _tmpSignalType = _stmt.getText(_columnIndexOfSignalType)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpLabel: String
          _tmpLabel = _stmt.getText(_columnIndexOfLabel)
          val _tmpTags: String
          _tmpTags = _stmt.getText(_columnIndexOfTags)
          val _tmpFrequency: String
          _tmpFrequency = _stmt.getText(_columnIndexOfFrequency)
          val _tmpProtocol: String
          _tmpProtocol = _stmt.getText(_columnIndexOfProtocol)
          val _tmpDeviceId: String
          _tmpDeviceId = _stmt.getText(_columnIndexOfDeviceId)
          val _tmpMacAddress: String
          _tmpMacAddress = _stmt.getText(_columnIndexOfMacAddress)
          val _tmpRssi: Int
          _tmpRssi = _stmt.getLong(_columnIndexOfRssi).toInt()
          val _tmpLatitude: Double?
          if (_stmt.isNull(_columnIndexOfLatitude)) {
            _tmpLatitude = null
          } else {
            _tmpLatitude = _stmt.getDouble(_columnIndexOfLatitude)
          }
          val _tmpLongitude: Double?
          if (_stmt.isNull(_columnIndexOfLongitude)) {
            _tmpLongitude = null
          } else {
            _tmpLongitude = _stmt.getDouble(_columnIndexOfLongitude)
          }
          val _tmpRawData: String
          _tmpRawData = _stmt.getText(_columnIndexOfRawData)
          val _tmpDetail: String
          _tmpDetail = _stmt.getText(_columnIndexOfDetail)
          val _tmpDecodedFields: String
          _tmpDecodedFields = _stmt.getText(_columnIndexOfDecodedFields)
          val _tmpFavorite: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfFavorite).toInt()
          _tmpFavorite = _tmp != 0
          _item =
              CapturedSignalEntity(_tmpId,_tmpTimestamp,_tmpSignalType,_tmpName,_tmpLabel,_tmpTags,_tmpFrequency,_tmpProtocol,_tmpDeviceId,_tmpMacAddress,_tmpRssi,_tmpLatitude,_tmpLongitude,_tmpRawData,_tmpDetail,_tmpDecodedFields,_tmpFavorite)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun count(): Int {
    val _sql: String = "SELECT COUNT(*) FROM captured_signals"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Int
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp
        } else {
          _result = 0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun countSinceType(type: String, since: Long): Int {
    val _sql: String =
        "SELECT COUNT(*) FROM captured_signals WHERE signalType = ? AND timestamp >= ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, type)
        _argIndex = 2
        _stmt.bindLong(_argIndex, since)
        val _result: Int
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp
        } else {
          _result = 0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun countSince(since: Long): Int {
    val _sql: String = "SELECT COUNT(*) FROM captured_signals WHERE timestamp >= ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, since)
        val _result: Int
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp
        } else {
          _result = 0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getLatest(): CapturedSignalEntity? {
    val _sql: String = "SELECT * FROM captured_signals ORDER BY timestamp DESC LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfSignalType: Int = getColumnIndexOrThrow(_stmt, "signalType")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfLabel: Int = getColumnIndexOrThrow(_stmt, "label")
        val _columnIndexOfTags: Int = getColumnIndexOrThrow(_stmt, "tags")
        val _columnIndexOfFrequency: Int = getColumnIndexOrThrow(_stmt, "frequency")
        val _columnIndexOfProtocol: Int = getColumnIndexOrThrow(_stmt, "protocol")
        val _columnIndexOfDeviceId: Int = getColumnIndexOrThrow(_stmt, "deviceId")
        val _columnIndexOfMacAddress: Int = getColumnIndexOrThrow(_stmt, "macAddress")
        val _columnIndexOfRssi: Int = getColumnIndexOrThrow(_stmt, "rssi")
        val _columnIndexOfLatitude: Int = getColumnIndexOrThrow(_stmt, "latitude")
        val _columnIndexOfLongitude: Int = getColumnIndexOrThrow(_stmt, "longitude")
        val _columnIndexOfRawData: Int = getColumnIndexOrThrow(_stmt, "rawData")
        val _columnIndexOfDetail: Int = getColumnIndexOrThrow(_stmt, "detail")
        val _columnIndexOfDecodedFields: Int = getColumnIndexOrThrow(_stmt, "decodedFields")
        val _columnIndexOfFavorite: Int = getColumnIndexOrThrow(_stmt, "favorite")
        val _result: CapturedSignalEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          val _tmpSignalType: String
          _tmpSignalType = _stmt.getText(_columnIndexOfSignalType)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpLabel: String
          _tmpLabel = _stmt.getText(_columnIndexOfLabel)
          val _tmpTags: String
          _tmpTags = _stmt.getText(_columnIndexOfTags)
          val _tmpFrequency: String
          _tmpFrequency = _stmt.getText(_columnIndexOfFrequency)
          val _tmpProtocol: String
          _tmpProtocol = _stmt.getText(_columnIndexOfProtocol)
          val _tmpDeviceId: String
          _tmpDeviceId = _stmt.getText(_columnIndexOfDeviceId)
          val _tmpMacAddress: String
          _tmpMacAddress = _stmt.getText(_columnIndexOfMacAddress)
          val _tmpRssi: Int
          _tmpRssi = _stmt.getLong(_columnIndexOfRssi).toInt()
          val _tmpLatitude: Double?
          if (_stmt.isNull(_columnIndexOfLatitude)) {
            _tmpLatitude = null
          } else {
            _tmpLatitude = _stmt.getDouble(_columnIndexOfLatitude)
          }
          val _tmpLongitude: Double?
          if (_stmt.isNull(_columnIndexOfLongitude)) {
            _tmpLongitude = null
          } else {
            _tmpLongitude = _stmt.getDouble(_columnIndexOfLongitude)
          }
          val _tmpRawData: String
          _tmpRawData = _stmt.getText(_columnIndexOfRawData)
          val _tmpDetail: String
          _tmpDetail = _stmt.getText(_columnIndexOfDetail)
          val _tmpDecodedFields: String
          _tmpDecodedFields = _stmt.getText(_columnIndexOfDecodedFields)
          val _tmpFavorite: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfFavorite).toInt()
          _tmpFavorite = _tmp != 0
          _result =
              CapturedSignalEntity(_tmpId,_tmpTimestamp,_tmpSignalType,_tmpName,_tmpLabel,_tmpTags,_tmpFrequency,_tmpProtocol,_tmpDeviceId,_tmpMacAddress,_tmpRssi,_tmpLatitude,_tmpLongitude,_tmpRawData,_tmpDetail,_tmpDecodedFields,_tmpFavorite)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getById(id: Long): CapturedSignalEntity? {
    val _sql: String = "SELECT * FROM captured_signals WHERE id = ? LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfSignalType: Int = getColumnIndexOrThrow(_stmt, "signalType")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfLabel: Int = getColumnIndexOrThrow(_stmt, "label")
        val _columnIndexOfTags: Int = getColumnIndexOrThrow(_stmt, "tags")
        val _columnIndexOfFrequency: Int = getColumnIndexOrThrow(_stmt, "frequency")
        val _columnIndexOfProtocol: Int = getColumnIndexOrThrow(_stmt, "protocol")
        val _columnIndexOfDeviceId: Int = getColumnIndexOrThrow(_stmt, "deviceId")
        val _columnIndexOfMacAddress: Int = getColumnIndexOrThrow(_stmt, "macAddress")
        val _columnIndexOfRssi: Int = getColumnIndexOrThrow(_stmt, "rssi")
        val _columnIndexOfLatitude: Int = getColumnIndexOrThrow(_stmt, "latitude")
        val _columnIndexOfLongitude: Int = getColumnIndexOrThrow(_stmt, "longitude")
        val _columnIndexOfRawData: Int = getColumnIndexOrThrow(_stmt, "rawData")
        val _columnIndexOfDetail: Int = getColumnIndexOrThrow(_stmt, "detail")
        val _columnIndexOfDecodedFields: Int = getColumnIndexOrThrow(_stmt, "decodedFields")
        val _columnIndexOfFavorite: Int = getColumnIndexOrThrow(_stmt, "favorite")
        val _result: CapturedSignalEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          val _tmpSignalType: String
          _tmpSignalType = _stmt.getText(_columnIndexOfSignalType)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpLabel: String
          _tmpLabel = _stmt.getText(_columnIndexOfLabel)
          val _tmpTags: String
          _tmpTags = _stmt.getText(_columnIndexOfTags)
          val _tmpFrequency: String
          _tmpFrequency = _stmt.getText(_columnIndexOfFrequency)
          val _tmpProtocol: String
          _tmpProtocol = _stmt.getText(_columnIndexOfProtocol)
          val _tmpDeviceId: String
          _tmpDeviceId = _stmt.getText(_columnIndexOfDeviceId)
          val _tmpMacAddress: String
          _tmpMacAddress = _stmt.getText(_columnIndexOfMacAddress)
          val _tmpRssi: Int
          _tmpRssi = _stmt.getLong(_columnIndexOfRssi).toInt()
          val _tmpLatitude: Double?
          if (_stmt.isNull(_columnIndexOfLatitude)) {
            _tmpLatitude = null
          } else {
            _tmpLatitude = _stmt.getDouble(_columnIndexOfLatitude)
          }
          val _tmpLongitude: Double?
          if (_stmt.isNull(_columnIndexOfLongitude)) {
            _tmpLongitude = null
          } else {
            _tmpLongitude = _stmt.getDouble(_columnIndexOfLongitude)
          }
          val _tmpRawData: String
          _tmpRawData = _stmt.getText(_columnIndexOfRawData)
          val _tmpDetail: String
          _tmpDetail = _stmt.getText(_columnIndexOfDetail)
          val _tmpDecodedFields: String
          _tmpDecodedFields = _stmt.getText(_columnIndexOfDecodedFields)
          val _tmpFavorite: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfFavorite).toInt()
          _tmpFavorite = _tmp != 0
          _result =
              CapturedSignalEntity(_tmpId,_tmpTimestamp,_tmpSignalType,_tmpName,_tmpLabel,_tmpTags,_tmpFrequency,_tmpProtocol,_tmpDeviceId,_tmpMacAddress,_tmpRssi,_tmpLatitude,_tmpLongitude,_tmpRawData,_tmpDetail,_tmpDecodedFields,_tmpFavorite)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeFavorites(): Flow<List<CapturedSignalEntity>> {
    val _sql: String = "SELECT * FROM captured_signals WHERE favorite = 1 ORDER BY timestamp DESC"
    return createFlow(__db, false, arrayOf("captured_signals")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfSignalType: Int = getColumnIndexOrThrow(_stmt, "signalType")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfLabel: Int = getColumnIndexOrThrow(_stmt, "label")
        val _columnIndexOfTags: Int = getColumnIndexOrThrow(_stmt, "tags")
        val _columnIndexOfFrequency: Int = getColumnIndexOrThrow(_stmt, "frequency")
        val _columnIndexOfProtocol: Int = getColumnIndexOrThrow(_stmt, "protocol")
        val _columnIndexOfDeviceId: Int = getColumnIndexOrThrow(_stmt, "deviceId")
        val _columnIndexOfMacAddress: Int = getColumnIndexOrThrow(_stmt, "macAddress")
        val _columnIndexOfRssi: Int = getColumnIndexOrThrow(_stmt, "rssi")
        val _columnIndexOfLatitude: Int = getColumnIndexOrThrow(_stmt, "latitude")
        val _columnIndexOfLongitude: Int = getColumnIndexOrThrow(_stmt, "longitude")
        val _columnIndexOfRawData: Int = getColumnIndexOrThrow(_stmt, "rawData")
        val _columnIndexOfDetail: Int = getColumnIndexOrThrow(_stmt, "detail")
        val _columnIndexOfDecodedFields: Int = getColumnIndexOrThrow(_stmt, "decodedFields")
        val _columnIndexOfFavorite: Int = getColumnIndexOrThrow(_stmt, "favorite")
        val _result: MutableList<CapturedSignalEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: CapturedSignalEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          val _tmpSignalType: String
          _tmpSignalType = _stmt.getText(_columnIndexOfSignalType)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpLabel: String
          _tmpLabel = _stmt.getText(_columnIndexOfLabel)
          val _tmpTags: String
          _tmpTags = _stmt.getText(_columnIndexOfTags)
          val _tmpFrequency: String
          _tmpFrequency = _stmt.getText(_columnIndexOfFrequency)
          val _tmpProtocol: String
          _tmpProtocol = _stmt.getText(_columnIndexOfProtocol)
          val _tmpDeviceId: String
          _tmpDeviceId = _stmt.getText(_columnIndexOfDeviceId)
          val _tmpMacAddress: String
          _tmpMacAddress = _stmt.getText(_columnIndexOfMacAddress)
          val _tmpRssi: Int
          _tmpRssi = _stmt.getLong(_columnIndexOfRssi).toInt()
          val _tmpLatitude: Double?
          if (_stmt.isNull(_columnIndexOfLatitude)) {
            _tmpLatitude = null
          } else {
            _tmpLatitude = _stmt.getDouble(_columnIndexOfLatitude)
          }
          val _tmpLongitude: Double?
          if (_stmt.isNull(_columnIndexOfLongitude)) {
            _tmpLongitude = null
          } else {
            _tmpLongitude = _stmt.getDouble(_columnIndexOfLongitude)
          }
          val _tmpRawData: String
          _tmpRawData = _stmt.getText(_columnIndexOfRawData)
          val _tmpDetail: String
          _tmpDetail = _stmt.getText(_columnIndexOfDetail)
          val _tmpDecodedFields: String
          _tmpDecodedFields = _stmt.getText(_columnIndexOfDecodedFields)
          val _tmpFavorite: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfFavorite).toInt()
          _tmpFavorite = _tmp != 0
          _item =
              CapturedSignalEntity(_tmpId,_tmpTimestamp,_tmpSignalType,_tmpName,_tmpLabel,_tmpTags,_tmpFrequency,_tmpProtocol,_tmpDeviceId,_tmpMacAddress,_tmpRssi,_tmpLatitude,_tmpLongitude,_tmpRawData,_tmpDetail,_tmpDecodedFields,_tmpFavorite)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getFavoriteRf(limit: Int): List<CapturedSignalEntity> {
    val _sql: String =
        "SELECT * FROM captured_signals WHERE favorite = 1 AND signalType = 'RF' ORDER BY timestamp DESC LIMIT ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, limit.toLong())
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfSignalType: Int = getColumnIndexOrThrow(_stmt, "signalType")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfLabel: Int = getColumnIndexOrThrow(_stmt, "label")
        val _columnIndexOfTags: Int = getColumnIndexOrThrow(_stmt, "tags")
        val _columnIndexOfFrequency: Int = getColumnIndexOrThrow(_stmt, "frequency")
        val _columnIndexOfProtocol: Int = getColumnIndexOrThrow(_stmt, "protocol")
        val _columnIndexOfDeviceId: Int = getColumnIndexOrThrow(_stmt, "deviceId")
        val _columnIndexOfMacAddress: Int = getColumnIndexOrThrow(_stmt, "macAddress")
        val _columnIndexOfRssi: Int = getColumnIndexOrThrow(_stmt, "rssi")
        val _columnIndexOfLatitude: Int = getColumnIndexOrThrow(_stmt, "latitude")
        val _columnIndexOfLongitude: Int = getColumnIndexOrThrow(_stmt, "longitude")
        val _columnIndexOfRawData: Int = getColumnIndexOrThrow(_stmt, "rawData")
        val _columnIndexOfDetail: Int = getColumnIndexOrThrow(_stmt, "detail")
        val _columnIndexOfDecodedFields: Int = getColumnIndexOrThrow(_stmt, "decodedFields")
        val _columnIndexOfFavorite: Int = getColumnIndexOrThrow(_stmt, "favorite")
        val _result: MutableList<CapturedSignalEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: CapturedSignalEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          val _tmpSignalType: String
          _tmpSignalType = _stmt.getText(_columnIndexOfSignalType)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpLabel: String
          _tmpLabel = _stmt.getText(_columnIndexOfLabel)
          val _tmpTags: String
          _tmpTags = _stmt.getText(_columnIndexOfTags)
          val _tmpFrequency: String
          _tmpFrequency = _stmt.getText(_columnIndexOfFrequency)
          val _tmpProtocol: String
          _tmpProtocol = _stmt.getText(_columnIndexOfProtocol)
          val _tmpDeviceId: String
          _tmpDeviceId = _stmt.getText(_columnIndexOfDeviceId)
          val _tmpMacAddress: String
          _tmpMacAddress = _stmt.getText(_columnIndexOfMacAddress)
          val _tmpRssi: Int
          _tmpRssi = _stmt.getLong(_columnIndexOfRssi).toInt()
          val _tmpLatitude: Double?
          if (_stmt.isNull(_columnIndexOfLatitude)) {
            _tmpLatitude = null
          } else {
            _tmpLatitude = _stmt.getDouble(_columnIndexOfLatitude)
          }
          val _tmpLongitude: Double?
          if (_stmt.isNull(_columnIndexOfLongitude)) {
            _tmpLongitude = null
          } else {
            _tmpLongitude = _stmt.getDouble(_columnIndexOfLongitude)
          }
          val _tmpRawData: String
          _tmpRawData = _stmt.getText(_columnIndexOfRawData)
          val _tmpDetail: String
          _tmpDetail = _stmt.getText(_columnIndexOfDetail)
          val _tmpDecodedFields: String
          _tmpDecodedFields = _stmt.getText(_columnIndexOfDecodedFields)
          val _tmpFavorite: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfFavorite).toInt()
          _tmpFavorite = _tmp != 0
          _item =
              CapturedSignalEntity(_tmpId,_tmpTimestamp,_tmpSignalType,_tmpName,_tmpLabel,_tmpTags,_tmpFrequency,_tmpProtocol,_tmpDeviceId,_tmpMacAddress,_tmpRssi,_tmpLatitude,_tmpLongitude,_tmpRawData,_tmpDetail,_tmpDecodedFields,_tmpFavorite)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getSince(since: Long): List<CapturedSignalEntity> {
    val _sql: String = "SELECT * FROM captured_signals WHERE timestamp >= ? ORDER BY timestamp DESC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, since)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfSignalType: Int = getColumnIndexOrThrow(_stmt, "signalType")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfLabel: Int = getColumnIndexOrThrow(_stmt, "label")
        val _columnIndexOfTags: Int = getColumnIndexOrThrow(_stmt, "tags")
        val _columnIndexOfFrequency: Int = getColumnIndexOrThrow(_stmt, "frequency")
        val _columnIndexOfProtocol: Int = getColumnIndexOrThrow(_stmt, "protocol")
        val _columnIndexOfDeviceId: Int = getColumnIndexOrThrow(_stmt, "deviceId")
        val _columnIndexOfMacAddress: Int = getColumnIndexOrThrow(_stmt, "macAddress")
        val _columnIndexOfRssi: Int = getColumnIndexOrThrow(_stmt, "rssi")
        val _columnIndexOfLatitude: Int = getColumnIndexOrThrow(_stmt, "latitude")
        val _columnIndexOfLongitude: Int = getColumnIndexOrThrow(_stmt, "longitude")
        val _columnIndexOfRawData: Int = getColumnIndexOrThrow(_stmt, "rawData")
        val _columnIndexOfDetail: Int = getColumnIndexOrThrow(_stmt, "detail")
        val _columnIndexOfDecodedFields: Int = getColumnIndexOrThrow(_stmt, "decodedFields")
        val _columnIndexOfFavorite: Int = getColumnIndexOrThrow(_stmt, "favorite")
        val _result: MutableList<CapturedSignalEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: CapturedSignalEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          val _tmpSignalType: String
          _tmpSignalType = _stmt.getText(_columnIndexOfSignalType)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpLabel: String
          _tmpLabel = _stmt.getText(_columnIndexOfLabel)
          val _tmpTags: String
          _tmpTags = _stmt.getText(_columnIndexOfTags)
          val _tmpFrequency: String
          _tmpFrequency = _stmt.getText(_columnIndexOfFrequency)
          val _tmpProtocol: String
          _tmpProtocol = _stmt.getText(_columnIndexOfProtocol)
          val _tmpDeviceId: String
          _tmpDeviceId = _stmt.getText(_columnIndexOfDeviceId)
          val _tmpMacAddress: String
          _tmpMacAddress = _stmt.getText(_columnIndexOfMacAddress)
          val _tmpRssi: Int
          _tmpRssi = _stmt.getLong(_columnIndexOfRssi).toInt()
          val _tmpLatitude: Double?
          if (_stmt.isNull(_columnIndexOfLatitude)) {
            _tmpLatitude = null
          } else {
            _tmpLatitude = _stmt.getDouble(_columnIndexOfLatitude)
          }
          val _tmpLongitude: Double?
          if (_stmt.isNull(_columnIndexOfLongitude)) {
            _tmpLongitude = null
          } else {
            _tmpLongitude = _stmt.getDouble(_columnIndexOfLongitude)
          }
          val _tmpRawData: String
          _tmpRawData = _stmt.getText(_columnIndexOfRawData)
          val _tmpDetail: String
          _tmpDetail = _stmt.getText(_columnIndexOfDetail)
          val _tmpDecodedFields: String
          _tmpDecodedFields = _stmt.getText(_columnIndexOfDecodedFields)
          val _tmpFavorite: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfFavorite).toInt()
          _tmpFavorite = _tmp != 0
          _item =
              CapturedSignalEntity(_tmpId,_tmpTimestamp,_tmpSignalType,_tmpName,_tmpLabel,_tmpTags,_tmpFrequency,_tmpProtocol,_tmpDeviceId,_tmpMacAddress,_tmpRssi,_tmpLatitude,_tmpLongitude,_tmpRawData,_tmpDetail,_tmpDecodedFields,_tmpFavorite)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteById(id: Long) {
    val _sql: String = "DELETE FROM captured_signals WHERE id = ?"
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

  public override suspend fun setFavorite(id: Long, favorite: Boolean) {
    val _sql: String = "UPDATE captured_signals SET favorite = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        val _tmp: Int = if (favorite) 1 else 0
        _stmt.bindLong(_argIndex, _tmp.toLong())
        _argIndex = 2
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun clearAll() {
    val _sql: String = "DELETE FROM captured_signals"
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
