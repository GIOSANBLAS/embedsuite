package com.embedsuite.app.data

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.embedsuite.app.security.SecureStore
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import java.io.File

object EmbedDatabaseFactory {

    private const val DB_NAME = "embed_suite.db"
    private const val TAG = "EmbedDatabaseFactory"

    fun create(context: Context, secureStore: SecureStore): EmbedDatabase {
        if (!secureStore.isAvailable) {
            throw IllegalStateException(
                "SecureStore no disponible; la app requiere almacenamiento seguro (EncryptedSharedPreferences)."
            )
        }

        val appContext = context.applicationContext
        SQLiteDatabase.loadLibs(appContext)
        val passphrase = secureStore.getOrCreateDatabasePassphrase()
        val builder = Room.databaseBuilder(appContext, EmbedDatabase::class.java, DB_NAME)
            .addMigrations(*DatabaseMigrations.ALL)
        SqlCipherMigration.migratePlaintextIfNeeded(appContext, DB_NAME, passphrase)
        builder.openHelperFactory(SupportFactory(passphrase))
        return builder.build()
    }
}

private object SqlCipherMigration {
    private const val MIGRATION_FLAG = "embed_sqlcipher_v1"
    private const val TAG = "SqlCipherMigration"

    fun migratePlaintextIfNeeded(context: Context, dbName: String, passphrase: ByteArray) {
        val dbFile = context.getDatabasePath(dbName)
        if (!dbFile.exists()) return

        val flagPrefs = context.getSharedPreferences("embed_db_flags", Context.MODE_PRIVATE)
        if (flagPrefs.getBoolean(MIGRATION_FLAG, false)) return

        if (canOpenEncrypted(dbFile, passphrase)) {
            flagPrefs.edit().putBoolean(MIGRATION_FLAG, true).apply()
            return
        }

        val backup = File(dbFile.parent, "$dbName.bak")
        val encrypted = File(dbFile.parent, "$dbName.new")
        encrypted.delete()

        try {
            val db = SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                "",
                null,
                SQLiteDatabase.OPEN_READWRITE
            )
            val keyLiteral = passphrase.decodeToString().replace("'", "''")
            db.rawExecSQL(
                "ATTACH DATABASE '${encrypted.absolutePath.replace("'", "''")}' AS encrypted KEY '$keyLiteral'"
            )
            db.rawExecSQL("SELECT sqlcipher_export('encrypted')")
            db.rawExecSQL("DETACH DATABASE encrypted")
            db.close()

            if (!dbFile.renameTo(backup)) {
                throw IllegalStateException("No se pudo respaldar la base de datos")
            }
            if (!encrypted.renameTo(dbFile)) {
                backup.renameTo(dbFile)
                throw IllegalStateException("No se pudo activar la base cifrada")
            }
            backup.delete()
            flagPrefs.edit().putBoolean(MIGRATION_FLAG, true).apply()
            Log.i(TAG, "Migración SQLCipher completada")
        } catch (e: Exception) {
            Log.e(TAG, "Migración SQLCipher falló; se mantiene BD sin cifrar", e)
            encrypted.delete()
            if (backup.exists() && !dbFile.exists()) {
                backup.renameTo(dbFile)
            }
        }
    }

    private fun canOpenEncrypted(dbFile: File, passphrase: ByteArray): Boolean {
        return try {
            val db = SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                passphrase.decodeToString(),
                null,
                SQLiteDatabase.OPEN_READONLY
            )
            db.close()
            true
        } catch (_: Exception) {
            false
        }
    }
}
