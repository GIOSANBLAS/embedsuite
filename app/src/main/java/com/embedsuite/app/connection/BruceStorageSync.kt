package com.embedsuite.app.connection

import com.embedsuite.app.data.CapturedSignalEntity
import com.embedsuite.app.data.IrButtonEntity
import com.embedsuite.app.data.NfcDumpEntity
import com.embedsuite.app.flipper.FlipperFileManager

data class StorageFileEntry(
    val path: String,
    val name: String,
    val extension: String
)

class BruceStorageSync(
    private val connectionManager: DeviceConnectionManager
) {
    suspend fun listSignalFiles(): Result<List<StorageFileEntry>> {
        return connectionManager.sendCommandAndCollect(BruceCommands.storageList("/"), waitMs = 6000L).map { lines ->
            parseStorageListing(lines)
        }
    }

    suspend fun readFile(path: String): Result<String> {
        val normalized = BruceCommands.sanitizeDeviceRelativePath(path)
        return connectionManager.sendCommandAndCollect(BruceCommands.storageRead(normalized), waitMs = 8000L).map { lines ->
            lines.joinToString("\n").trim()
        }
    }

    suspend fun importSubFile(path: String): Result<CapturedSignalEntity> {
        return readFile(path).mapCatching { content ->
            val parsed = FlipperFileManager.parseSubFile(content)
                ?: throw IllegalArgumentException("No es un .sub válido")
            parsed.copy(detail = "device:$path")
        }
    }

    suspend fun importIrFile(path: String): Result<IrButtonEntity> {
        return readFile(path).mapCatching { content ->
            FlipperFileManager.parseIrFile(content)
                ?: throw IllegalArgumentException("No es un .ir válido")
        }
    }

    suspend fun importNfcFile(path: String): Result<NfcDumpEntity> {
        return readFile(path).mapCatching { content ->
            FlipperFileManager.parseNfcFile(content)
                ?: throw IllegalArgumentException("No es un .nfc válido")
        }
    }

    companion object {
        fun parseStorageListing(lines: List<String>): List<StorageFileEntry> {
            val found = linkedMapOf<String, StorageFileEntry>()
            lines.forEach { line ->
                Regex("""(\/?[\w./\-]+\.(sub|ir|nfc))""", RegexOption.IGNORE_CASE).findAll(line).forEach { match ->
                    val path = match.groupValues[1]
                    val ext = match.groupValues[2].lowercase()
                    val name = path.substringAfterLast('/')
                    found[path] = StorageFileEntry(path = path, name = name, extension = ext)
                }
            }
            return found.values.sortedBy { it.name.lowercase() }
        }
    }
}
