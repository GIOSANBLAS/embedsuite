package com.embedsuite.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Índice local Flipper IRDB para búsqueda semántica (Módulo C). */
@Entity(tableName = "irdb_index")
data class IrdbEntryEntity(
    @PrimaryKey val path: String,
    val brand: String,
    val device: String,
    val function: String,
    val fileName: String,
    val searchBlob: String
)
