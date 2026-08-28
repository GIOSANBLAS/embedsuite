package com.embedsuite.app.core.bruce

/** Parsea salidas CLI `storage list` / `storage read` del firmware Bruce. */
object BruceStorageParser {

    data class Entry(
        val name: String,
        val isDir: Boolean,
        val sizeLabel: String? = null
    ) {
        val displayName: String get() = if (isDir) "[D] $name" else name
    }

    fun parseListResponse(raw: String, basePath: String = "/"): List<Entry> {
        if (raw.isBlank()) return emptyList()
        val seen = linkedSetOf<String>()
        val entries = mutableListOf<Entry>()
        raw.lineSequence().forEach { line ->
            parseListLine(line)?.let { entry ->
                if (seen.add(entry.name.lowercase())) entries += entry
            }
        }
        if (entries.isNotEmpty()) return entries
        // Fallback: una sola línea separada por comas/tabs
        raw.split(',', '\t', ';').mapNotNull { parseListLine(it.trim()) }.forEach { entry ->
            if (seen.add(entry.name.lowercase())) entries += entry
        }
        return entries
    }

    private fun parseListLine(line: String): Entry? {
        val t = line.trim()
        if (t.isBlank()) return null
        if (t.startsWith("Error", ignoreCase = true) || t.contains("No SD", ignoreCase = true)) return null
        if (t.startsWith("Path:", ignoreCase = true) || t.startsWith("Listing", ignoreCase = true)) return null

        Regex("""^\[D(?:IR)?\]\s*(.+)""", RegexOption.IGNORE_CASE).find(t)?.let {
            return Entry(it.groupValues[1].trim(), isDir = true)
        }
        Regex("""^(?:DIR|Directory):\s*(.+)""", RegexOption.IGNORE_CASE).find(t)?.let {
            return Entry(it.groupValues[1].trim(), isDir = true)
        }
        Regex("""^(.+?)\s+(\d+)\s*bytes?$""", RegexOption.IGNORE_CASE).find(t)?.let {
            return Entry(it.groupValues[1].trim(), isDir = false, sizeLabel = "${it.groupValues[2]} B")
        }
        if (t.endsWith("/")) return Entry(t.trimEnd('/'), isDir = true)
        if (t.contains('.') || t.matches(Regex("""[\w\-]+"""))) {
            return Entry(t, isDir = false)
        }
        return null
    }

    fun childPath(base: String, name: String): String {
        val cleanName = name.removePrefix("[D] ").trim()
        val b = base.trim().ifBlank { "/" }.trimEnd('/')
        return if (b.isEmpty() || b == "/") "/$cleanName" else "$b/$cleanName"
    }

    fun parentPath(path: String): String {
        val p = path.trim().trimEnd('/')
        if (p.isBlank() || p == "/") return "/"
        val idx = p.lastIndexOf('/')
        return if (idx <= 0) "/" else p.substring(0, idx).ifBlank { "/" }
    }

    /** Extrae contenido de archivo desde respuesta `storage read`. */
    fun extractFileContent(raw: String): String {
        val lines = raw.lines()
        val start = lines.indexOfFirst { it.contains("Filetype:", ignoreCase = true) }
        if (start >= 0) return lines.drop(start).joinToString("\n")
        val readStart = lines.indexOfFirst { it.startsWith("---", ignoreCase = false) }
        if (readStart >= 0) return lines.drop(readStart + 1).joinToString("\n").trim()
        return raw.trim()
    }
}
