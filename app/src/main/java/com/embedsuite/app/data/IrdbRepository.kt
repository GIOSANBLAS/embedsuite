package com.embedsuite.app.data

import com.embedsuite.app.engine.sync.IrdbSync

class IrdbRepository(
    private val dao: IrdbDao,
    private val irdbSync: IrdbSync
) {
    suspend fun indexedCount(): Int = dao.count()

    suspend fun search(query: String): List<IrdbEntryEntity> {
        val ftsQuery = buildFtsQuery(query) ?: return emptyList()
        return dao.search(ftsQuery)
    }

    suspend fun getByPath(path: String): IrdbEntryEntity? = dao.getByPath(path)

    suspend fun downloadContent(relativePath: String): Result<String> =
        irdbSync.downloadIrFile(relativePath)

    /** Indexa metadatos IRDB remoto en Room (sin descargar todos los .ir). */
    suspend fun syncIndex(maxEntries: Int = 4_000): Result<Int> = runCatching {
        val remote = irdbSync.listRemoteIrFiles().getOrThrow()
        dao.clearAll()
        val batch = remote.take(maxEntries).map { entry ->
            val parts = entry.path.split('/')
            val brand = parts.dropLast(1).lastOrNull()?.replace('_', ' ') ?: entry.category
            val device = entry.name.replace('_', ' ')
            val function = parts.firstOrNull()?.replace('_', ' ') ?: "General"
            val blob = listOf(entry.path, brand, device, function, entry.name, entry.category)
                .joinToString(" ")
                .lowercase()
            IrdbEntryEntity(
                path = entry.path,
                brand = brand,
                device = device,
                function = function,
                fileName = entry.name,
                searchBlob = blob
            )
        }
        dao.insertAll(batch)
        batch.size
    }

    companion object {
        /** Builds an FTS4 MATCH query with prefix tokens (min 2 chars each). */
        internal fun buildFtsQuery(raw: String): String? {
            val tokens = raw.trim().lowercase()
                .split(Regex("\\s+"))
                .map { it.replace("\"", "\"\"") }
                .filter { it.length >= 2 }
            if (tokens.isEmpty()) return null
            return tokens.joinToString(" ") { token ->
                if (token.any { !it.isLetterOrDigit() && it != '_' }) "\"$token\"*" else "$token*"
            }
        }
    }
}
