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
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class ScriptDao_Impl(
  __db: RoomDatabase,
) : ScriptDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfScriptEntity: EntityInsertAdapter<ScriptEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfScriptEntity = object : EntityInsertAdapter<ScriptEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `scripts` (`id`,`name`,`category`,`description`,`author`,`version`,`dialect`,`sourceCode`,`isFavorite`,`lastSyncAt`) VALUES (?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: ScriptEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindText(3, entity.category)
        statement.bindText(4, entity.description)
        statement.bindText(5, entity.author)
        statement.bindText(6, entity.version)
        statement.bindText(7, entity.dialect)
        statement.bindText(8, entity.sourceCode)
        val _tmp: Int = if (entity.isFavorite) 1 else 0
        statement.bindLong(9, _tmp.toLong())
        statement.bindLong(10, entity.lastSyncAt)
      }
    }
  }

  public override suspend fun insert(script: ScriptEntity): Unit = performSuspending(__db, false,
      true) { _connection ->
    __insertAdapterOfScriptEntity.insert(_connection, script)
  }

  public override fun observeAll(): Flow<List<ScriptEntity>> {
    val _sql: String = "SELECT * FROM scripts ORDER BY category, name"
    return createFlow(__db, false, arrayOf("scripts")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfAuthor: Int = getColumnIndexOrThrow(_stmt, "author")
        val _columnIndexOfVersion: Int = getColumnIndexOrThrow(_stmt, "version")
        val _columnIndexOfDialect: Int = getColumnIndexOrThrow(_stmt, "dialect")
        val _columnIndexOfSourceCode: Int = getColumnIndexOrThrow(_stmt, "sourceCode")
        val _columnIndexOfIsFavorite: Int = getColumnIndexOrThrow(_stmt, "isFavorite")
        val _columnIndexOfLastSyncAt: Int = getColumnIndexOrThrow(_stmt, "lastSyncAt")
        val _result: MutableList<ScriptEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ScriptEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpAuthor: String
          _tmpAuthor = _stmt.getText(_columnIndexOfAuthor)
          val _tmpVersion: String
          _tmpVersion = _stmt.getText(_columnIndexOfVersion)
          val _tmpDialect: String
          _tmpDialect = _stmt.getText(_columnIndexOfDialect)
          val _tmpSourceCode: String
          _tmpSourceCode = _stmt.getText(_columnIndexOfSourceCode)
          val _tmpIsFavorite: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsFavorite).toInt()
          _tmpIsFavorite = _tmp != 0
          val _tmpLastSyncAt: Long
          _tmpLastSyncAt = _stmt.getLong(_columnIndexOfLastSyncAt)
          _item =
              ScriptEntity(_tmpId,_tmpName,_tmpCategory,_tmpDescription,_tmpAuthor,_tmpVersion,_tmpDialect,_tmpSourceCode,_tmpIsFavorite,_tmpLastSyncAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeByCategory(category: String): Flow<List<ScriptEntity>> {
    val _sql: String = "SELECT * FROM scripts WHERE category = ? ORDER BY name"
    return createFlow(__db, false, arrayOf("scripts")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, category)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfAuthor: Int = getColumnIndexOrThrow(_stmt, "author")
        val _columnIndexOfVersion: Int = getColumnIndexOrThrow(_stmt, "version")
        val _columnIndexOfDialect: Int = getColumnIndexOrThrow(_stmt, "dialect")
        val _columnIndexOfSourceCode: Int = getColumnIndexOrThrow(_stmt, "sourceCode")
        val _columnIndexOfIsFavorite: Int = getColumnIndexOrThrow(_stmt, "isFavorite")
        val _columnIndexOfLastSyncAt: Int = getColumnIndexOrThrow(_stmt, "lastSyncAt")
        val _result: MutableList<ScriptEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ScriptEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpAuthor: String
          _tmpAuthor = _stmt.getText(_columnIndexOfAuthor)
          val _tmpVersion: String
          _tmpVersion = _stmt.getText(_columnIndexOfVersion)
          val _tmpDialect: String
          _tmpDialect = _stmt.getText(_columnIndexOfDialect)
          val _tmpSourceCode: String
          _tmpSourceCode = _stmt.getText(_columnIndexOfSourceCode)
          val _tmpIsFavorite: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsFavorite).toInt()
          _tmpIsFavorite = _tmp != 0
          val _tmpLastSyncAt: Long
          _tmpLastSyncAt = _stmt.getLong(_columnIndexOfLastSyncAt)
          _item =
              ScriptEntity(_tmpId,_tmpName,_tmpCategory,_tmpDescription,_tmpAuthor,_tmpVersion,_tmpDialect,_tmpSourceCode,_tmpIsFavorite,_tmpLastSyncAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getById(id: String): ScriptEntity? {
    val _sql: String = "SELECT * FROM scripts WHERE id = ? LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfAuthor: Int = getColumnIndexOrThrow(_stmt, "author")
        val _columnIndexOfVersion: Int = getColumnIndexOrThrow(_stmt, "version")
        val _columnIndexOfDialect: Int = getColumnIndexOrThrow(_stmt, "dialect")
        val _columnIndexOfSourceCode: Int = getColumnIndexOrThrow(_stmt, "sourceCode")
        val _columnIndexOfIsFavorite: Int = getColumnIndexOrThrow(_stmt, "isFavorite")
        val _columnIndexOfLastSyncAt: Int = getColumnIndexOrThrow(_stmt, "lastSyncAt")
        val _result: ScriptEntity?
        if (_stmt.step()) {
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpAuthor: String
          _tmpAuthor = _stmt.getText(_columnIndexOfAuthor)
          val _tmpVersion: String
          _tmpVersion = _stmt.getText(_columnIndexOfVersion)
          val _tmpDialect: String
          _tmpDialect = _stmt.getText(_columnIndexOfDialect)
          val _tmpSourceCode: String
          _tmpSourceCode = _stmt.getText(_columnIndexOfSourceCode)
          val _tmpIsFavorite: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsFavorite).toInt()
          _tmpIsFavorite = _tmp != 0
          val _tmpLastSyncAt: Long
          _tmpLastSyncAt = _stmt.getLong(_columnIndexOfLastSyncAt)
          _result =
              ScriptEntity(_tmpId,_tmpName,_tmpCategory,_tmpDescription,_tmpAuthor,_tmpVersion,_tmpDialect,_tmpSourceCode,_tmpIsFavorite,_tmpLastSyncAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAll(): List<ScriptEntity> {
    val _sql: String = "SELECT * FROM scripts ORDER BY category, name"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfAuthor: Int = getColumnIndexOrThrow(_stmt, "author")
        val _columnIndexOfVersion: Int = getColumnIndexOrThrow(_stmt, "version")
        val _columnIndexOfDialect: Int = getColumnIndexOrThrow(_stmt, "dialect")
        val _columnIndexOfSourceCode: Int = getColumnIndexOrThrow(_stmt, "sourceCode")
        val _columnIndexOfIsFavorite: Int = getColumnIndexOrThrow(_stmt, "isFavorite")
        val _columnIndexOfLastSyncAt: Int = getColumnIndexOrThrow(_stmt, "lastSyncAt")
        val _result: MutableList<ScriptEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ScriptEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpAuthor: String
          _tmpAuthor = _stmt.getText(_columnIndexOfAuthor)
          val _tmpVersion: String
          _tmpVersion = _stmt.getText(_columnIndexOfVersion)
          val _tmpDialect: String
          _tmpDialect = _stmt.getText(_columnIndexOfDialect)
          val _tmpSourceCode: String
          _tmpSourceCode = _stmt.getText(_columnIndexOfSourceCode)
          val _tmpIsFavorite: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsFavorite).toInt()
          _tmpIsFavorite = _tmp != 0
          val _tmpLastSyncAt: Long
          _tmpLastSyncAt = _stmt.getLong(_columnIndexOfLastSyncAt)
          _item =
              ScriptEntity(_tmpId,_tmpName,_tmpCategory,_tmpDescription,_tmpAuthor,_tmpVersion,_tmpDialect,_tmpSourceCode,_tmpIsFavorite,_tmpLastSyncAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeFavorites(): Flow<List<ScriptEntity>> {
    val _sql: String = "SELECT * FROM scripts WHERE isFavorite = 1 ORDER BY category, name"
    return createFlow(__db, false, arrayOf("scripts")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfAuthor: Int = getColumnIndexOrThrow(_stmt, "author")
        val _columnIndexOfVersion: Int = getColumnIndexOrThrow(_stmt, "version")
        val _columnIndexOfDialect: Int = getColumnIndexOrThrow(_stmt, "dialect")
        val _columnIndexOfSourceCode: Int = getColumnIndexOrThrow(_stmt, "sourceCode")
        val _columnIndexOfIsFavorite: Int = getColumnIndexOrThrow(_stmt, "isFavorite")
        val _columnIndexOfLastSyncAt: Int = getColumnIndexOrThrow(_stmt, "lastSyncAt")
        val _result: MutableList<ScriptEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ScriptEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpAuthor: String
          _tmpAuthor = _stmt.getText(_columnIndexOfAuthor)
          val _tmpVersion: String
          _tmpVersion = _stmt.getText(_columnIndexOfVersion)
          val _tmpDialect: String
          _tmpDialect = _stmt.getText(_columnIndexOfDialect)
          val _tmpSourceCode: String
          _tmpSourceCode = _stmt.getText(_columnIndexOfSourceCode)
          val _tmpIsFavorite: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsFavorite).toInt()
          _tmpIsFavorite = _tmp != 0
          val _tmpLastSyncAt: Long
          _tmpLastSyncAt = _stmt.getLong(_columnIndexOfLastSyncAt)
          _item =
              ScriptEntity(_tmpId,_tmpName,_tmpCategory,_tmpDescription,_tmpAuthor,_tmpVersion,_tmpDialect,_tmpSourceCode,_tmpIsFavorite,_tmpLastSyncAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteById(id: String) {
    val _sql: String = "DELETE FROM scripts WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun setFavorite(id: String, favorite: Boolean) {
    val _sql: String = "UPDATE scripts SET isFavorite = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        val _tmp: Int = if (favorite) 1 else 0
        _stmt.bindLong(_argIndex, _tmp.toLong())
        _argIndex = 2
        _stmt.bindText(_argIndex, id)
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
