package com.embedsuite.app.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {

    /** Identity migration — preserves v4 data on upgrade to v5. */
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Schema unchanged; bump version for release-ready migration path.
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS rf_automation_rules (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    enabled INTEGER NOT NULL DEFAULT 1,
                    matchProtocol TEXT NOT NULL DEFAULT '',
                    matchFrequency TEXT NOT NULL DEFAULT '',
                    actionType TEXT NOT NULL DEFAULT 'NOTIFY',
                    actionPayload TEXT NOT NULL DEFAULT '',
                    createdAt INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )
        }
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE captured_signals ADD COLUMN favorite INTEGER NOT NULL DEFAULT 0"
            )
        }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            normalizeIrButtonsPayloadColumn(db)
        }
    }

    /** Fixes v8 installs that still carry legacy `bruceCommand` / `bruce_command` columns. */
    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            normalizeIrButtonsPayloadColumn(db)
        }
    }

    val ALL: Array<Migration> = arrayOf(
        MIGRATION_4_5,
        MIGRATION_5_6,
        MIGRATION_6_7,
        MIGRATION_7_8,
        MIGRATION_8_9
    )

    /**
     * Room historically created `bruceCommand` (field name) or `bruce_command` (snake).
     * Target schema uses [ir_payload] only. Idempotent — safe on fresh v9 installs.
     */
    private fun normalizeIrButtonsPayloadColumn(db: SupportSQLiteDatabase) {
        if (!tableExists(db, "ir_buttons")) return

        val hasPayload = columnExists(db, "ir_buttons", "ir_payload")
        val hasCamelLegacy = columnExists(db, "ir_buttons", "bruceCommand")
        val hasSnakeLegacy = columnExists(db, "ir_buttons", "bruce_command")

        when {
            !hasPayload && hasCamelLegacy -> {
                db.execSQL("ALTER TABLE ir_buttons RENAME COLUMN bruceCommand TO ir_payload")
                return
            }
            !hasPayload && hasSnakeLegacy -> {
                db.execSQL("ALTER TABLE ir_buttons RENAME COLUMN bruce_command TO ir_payload")
                return
            }
            !hasPayload -> {
                db.execSQL(
                    "ALTER TABLE ir_buttons ADD COLUMN ir_payload TEXT NOT NULL DEFAULT ''"
                )
                return
            }
        }

        if (hasCamelLegacy || hasSnakeLegacy) {
            rebuildIrButtonsWithoutLegacyColumns(db, hasCamelLegacy, hasSnakeLegacy)
        }
    }

    private fun rebuildIrButtonsWithoutLegacyColumns(
        db: SupportSQLiteDatabase,
        hasCamelLegacy: Boolean,
        hasSnakeLegacy: Boolean
    ) {
        val payloadExpr = when {
            hasCamelLegacy && hasSnakeLegacy ->
                "COALESCE(NULLIF(ir_payload, ''), NULLIF(bruceCommand, ''), NULLIF(bruce_command, ''), '')"
            hasCamelLegacy ->
                "COALESCE(NULLIF(ir_payload, ''), NULLIF(bruceCommand, ''), '')"
            else ->
                "COALESCE(NULLIF(ir_payload, ''), NULLIF(bruce_command, ''), '')"
        }
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS ir_buttons_mig (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                panelName TEXT NOT NULL,
                buttonName TEXT NOT NULL,
                protocol TEXT NOT NULL,
                hexCode TEXT NOT NULL,
                ir_payload TEXT NOT NULL DEFAULT ''
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO ir_buttons_mig (id, panelName, buttonName, protocol, hexCode, ir_payload)
            SELECT id, panelName, buttonName, protocol, hexCode, $payloadExpr
            FROM ir_buttons
            """.trimIndent()
        )
        db.execSQL("DROP TABLE ir_buttons")
        db.execSQL("ALTER TABLE ir_buttons_mig RENAME TO ir_buttons")
    }

    private fun tableExists(db: SupportSQLiteDatabase, table: String): Boolean {
        if (!table.matches(Regex("^[a-zA-Z0-9_]+$"))) return false
        db.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
            arrayOf(table)
        ).use { return it.moveToFirst() }
    }

    private fun columnExists(db: SupportSQLiteDatabase, table: String, column: String): Boolean {
        if (!table.matches(Regex("^[a-zA-Z0-9_]+$"))) return false
        db.query("PRAGMA table_info($table)").use { cursor ->
            val nameIdx = cursor.getColumnIndex("name")
            if (nameIdx < 0) return false
            while (cursor.moveToNext()) {
                if (column == cursor.getString(nameIdx)) return true
            }
        }
        return false
    }
}
