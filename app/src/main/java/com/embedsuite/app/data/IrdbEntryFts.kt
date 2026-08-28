package com.embedsuite.app.data

import androidx.room.Entity
import androidx.room.Fts4

/** FTS4 shadow table for fast token search over [IrdbEntryEntity]. */
@Fts4(contentEntity = IrdbEntryEntity::class)
@Entity(tableName = "irdb_index_fts")
data class IrdbEntryFts(
    val searchBlob: String,
    val brand: String,
    val device: String,
    val function: String
)
