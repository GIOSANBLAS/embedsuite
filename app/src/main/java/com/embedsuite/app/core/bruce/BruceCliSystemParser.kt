package com.embedsuite.app.core.bruce

/** Parses Bruce stock CLI responses for `info`, `free`, and `storage free sd`. */
object BruceCliSystemParser {

    data class InfoParsed(
        val version: String = "",
        val deviceName: String = "",
        val sdMentioned: Boolean = false,
        val wifiConnected: Boolean? = null
    )

    data class FreeParsed(
        val heapFreeBytes: Long? = null,
        val heapTotalBytes: Long? = null,
        val psramFreeBytes: Long? = null,
        val psramTotalBytes: Long? = null
    )

    data class SdStorageParsed(
        val mounted: Boolean = false,
        val freeBytes: Long? = null,
        val usedBytes: Long? = null,
        val totalBytes: Long? = null
    )

    fun parseInfo(response: String): InfoParsed {
        var version = ""
        var deviceName = ""
        var sdMentioned = false
        var wifiConnected: Boolean? = null
        response.lineSequence().forEach { line ->
            val t = line.trim()
            if (t.isBlank()) return@forEach
            val bruceIdx = t.indexOf("Bruce v", ignoreCase = true)
            if (bruceIdx >= 0 && version.isBlank()) {
                version = t.substring(bruceIdx + "Bruce v".length).trim().substringBefore(' ')
            }
            if (t.startsWith("Device:", ignoreCase = true)) {
                deviceName = t.substringAfter(":").trim()
            }
            if (t.contains("SD", ignoreCase = true)) {
                sdMentioned = !t.contains("No SD", ignoreCase = true)
            }
            if (t.startsWith("Wifi:", ignoreCase = true)) {
                val state = t.substringAfter(":").trim()
                wifiConnected = state.contains("connected", ignoreCase = true) &&
                    !state.contains("not", ignoreCase = true)
            }
        }
        return InfoParsed(version, deviceName, sdMentioned, wifiConnected)
    }

    fun parseFree(response: String): FreeParsed {
        var heapFree: Long? = null
        var heapTotal: Long? = null
        var psramFree: Long? = null
        var psramTotal: Long? = null
        response.lineSequence().forEach { line ->
            parseLongField(line, "Free heap")?.let { heapFree = it }
            parseLongField(line, "Total heap")?.let { heapTotal = it }
            parseLongField(line, "Free PSRAM")?.let { psramFree = it }
            parseLongField(line, "Total PSRAM")?.let { psramTotal = it }
        }
        return FreeParsed(heapFree, heapTotal, psramFree, psramTotal)
    }

    fun parseSdFree(response: String): SdStorageParsed {
        if (response.contains("No SD", ignoreCase = true)) {
            return SdStorageParsed(mounted = false)
        }
        var free: Long? = null
        var used: Long? = null
        var total: Long? = null
        response.lineSequence().forEach { line ->
            parseLongField(line, "SD Free space")?.let { free = it }
            parseLongField(line, "SD Used space")?.let { used = it }
            parseLongField(line, "SD Total space")?.let { total = it }
        }
        val mounted = free != null || used != null || total != null
        return SdStorageParsed(mounted, free, used, total)
    }

    fun formatBytes(bytes: Long): String = when {
        bytes >= 1_073_741_824L -> "%.1f GB".format(bytes / 1_073_741_824.0)
        bytes >= 1_048_576L -> "%.0f MB".format(bytes / 1_048_576.0)
        bytes >= 1024L -> "%.0f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }

    fun formatBattery(pct: Int?, voltage: Double? = null): String? {
        if (pct == null && voltage == null) return null
        val pctLabel = pct?.let { "$it%" }
        val voltLabel = voltage?.let { "%.2fV".format(it) }
        return when {
            pctLabel != null && voltLabel != null -> "$pctLabel · $voltLabel"
            pctLabel != null -> pctLabel
            else -> voltLabel
        }
    }

    private fun parseLongField(line: String, label: String): Long? {
        val pattern = Regex("""${Regex.escape(label)}:\s*(\d+)""", RegexOption.IGNORE_CASE)
        return pattern.find(line.trim())?.groupValues?.get(1)?.toLongOrNull()
    }
}
