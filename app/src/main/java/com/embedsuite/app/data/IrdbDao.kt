package com.embedsuite.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface IrdbDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<IrdbEntryEntity>)

    @Query("SELECT COUNT(*) FROM irdb_index")
    suspend fun count(): Int

    @Query(
        """
        SELECT irdb_index.* FROM irdb_index
        JOIN irdb_index_fts ON irdb_index.rowid = irdb_index_fts.rowid
        WHERE irdb_index_fts MATCH :query
        ORDER BY irdb_index.brand, irdb_index.device
        LIMIT :limit
        """
    )
    suspend fun search(query: String, limit: Int = 40): List<IrdbEntryEntity>

    @Query("SELECT * FROM irdb_index WHERE path = :path LIMIT 1")
    suspend fun getByPath(path: String): IrdbEntryEntity?

    @Query("DELETE FROM irdb_index")
    suspend fun clearAll()
}
