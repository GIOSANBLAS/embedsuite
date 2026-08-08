package com.embedsuite.app.flash

import java.io.File
import java.nio.charset.StandardCharsets

/**
 * Detecta si un .bin es imagen merged (bootloader+partitions+app @ 0x0, estilo Xibalba/Bruce)
 * o solo aplicación (flash @ 0x10000 sobre partición ota_0 existente).
 */
object FirmwareImageAnalyzer {

    enum class ImageKind {
        /** merge-bin de esptool: tabla de particiones en offset 0x8000 del archivo. */
        MERGED_FULL,
        /** Solo firmware.bin de aplicación (offset 0x10000). */
        APP_ONLY
    }

    data class Analysis(
        val kind: ImageKind,
        val flashOffset: Int,
        val appVersion: String?,
        val projectName: String?,
        val sizeBytes: Long,
        val warning: String?
    )

    /** Offsets estándar ESP32-S3 / LilyGO T-Embed 16 MB (compatible Bruce + Xibalba). */
    const val OTA0_OFFSET = 0x10000
    const val MERGED_OFFSET = 0x0
    const val OTADATA_OFFSET = 0xE000
    const val OTADATA_SIZE = 0x2000
    /** Algunos layouts (Bruce/IDF) usan otadata @ 0xF000. */
    const val OTADATA_ALT_OFFSET = 0xF000
    const val OTADATA_ALT_SIZE = 0x1000

    fun analyze(file: File): Analysis {
        val size = file.length()
        val header = ByteArray(minOf(size.toInt(), 0x8020)).also { buf ->
            file.inputStream().use { stream ->
                var read = 0
                while (read < buf.size) {
                    val n = stream.read(buf, read, buf.size - read)
                    if (n <= 0) break
                    read += n
                }
            }
        }

        val merged = header.size >= 0x8002 &&
            header[0] == 0xE9.toByte() &&
            header[0x8000] == 0xAA.toByte() &&
            header[0x8001] == 0x50.toByte()

        val kind = if (merged) ImageKind.MERGED_FULL else ImageKind.APP_ONLY
        val meta = parseAppDescriptor(file)

        val warning = when {
            merged -> null
            meta?.version?.contains("0.19") == true || meta?.version?.contains("Xibalba", ignoreCase = true) == true ->
                "Binario APP (${meta.version}) — se flashea en 0x10000. " +
                    "El release oficial Xibalba-0.19 es merged @ 0x0 (estilo Bruce)."
            meta != null ->
                "Binario APP (${meta.version}) — se flashea en 0x10000. " +
                    "Para instalación limpia usa el merged .bin @ 0x0 de xibalba-bruce."
            !merged ->
                "Imagen APP (no merged). Se preserva bootloader; flash en 0x10000 + reset otadata."
            else -> null
        }

        return Analysis(
            kind = kind,
            flashOffset = if (merged) MERGED_OFFSET else OTA0_OFFSET,
            appVersion = meta?.version,
            projectName = meta?.project,
            sizeBytes = size,
            warning = warning
        )
    }

    private data class AppMeta(val project: String, val version: String)

    /** Busca bloque esp_app_desc en el binario (mismo criterio que esptool image-info). */
    private fun parseAppDescriptor(file: File): AppMeta? {
        val needle = "esp_app_desc".toByteArray(StandardCharsets.US_ASCII)
        val chunkSize = 256 * 1024
        val buffer = ByteArray(chunkSize)
        file.inputStream().use { input ->
            var base = 0L
            var carry = ByteArray(0)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                val combined = carry + buffer.copyOf(read)
                val idx = indexOf(combined, needle)
                if (idx >= 0) {
                    val descStart = idx + 32
                    if (descStart + 64 <= combined.size) {
                        val version = readCString(combined, descStart + 32, 32)
                        val project = readCString(combined, descStart, 32)
                        if (project.isNotBlank()) {
                            return AppMeta(project, version.ifBlank { "?" })
                        }
                    }
                }
                carry = combined.copyOfRange(maxOf(0, combined.size - needle.size), combined.size)
                base += read
                if (base > file.length()) break
            }
        }
        return null
    }

    private fun indexOf(haystack: ByteArray, needle: ByteArray): Int {
        if (needle.isEmpty()) return 0
        outer@ for (i in 0..haystack.size - needle.size) {
            for (j in needle.indices) {
                if (haystack[i + j] != needle[j]) continue@outer
            }
            return i
        }
        return -1
    }

    private fun readCString(data: ByteArray, offset: Int, maxLen: Int): String {
        val end = (offset until minOf(offset + maxLen, data.size))
            .firstOrNull { data[it] == 0.toByte() } ?: minOf(offset + maxLen, data.size)
        return String(data, offset, end - offset, StandardCharsets.US_ASCII).trim()
    }
}
