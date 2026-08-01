package com.embedsuite.app.connection

import android.util.Base64
import org.json.JSONObject
import java.io.File

/**
 * OTA firmware upload for T-Embed Xibalba via TEH-Link (USB CDC).
 * Protocol: ota_begin → ota_chunk (sequential) → ota_finish with SHA256 verification.
 */
class TehLinkOtaUploader(private val tehLinkClient: TehLinkClient) {

    companion object {
        /** Max raw bytes per chunk (device TEH_LINK_OTA_B64_MAX = 4096). */
        const val CHUNK_RAW_MAX = 3072
        private const val OTA_BEGIN_TIMEOUT_MS = 15_000L
        private const val OTA_CHUNK_TIMEOUT_MS = 30_000L
        private const val OTA_FINISH_TIMEOUT_MS = 60_000L
    }

    suspend fun upload(
        transport: TEmbedTransport,
        binFile: File,
        expectedSha256: String,
        onProgress: (Int) -> Unit
    ): Result<String> = runCatching {
        val sha256 = expectedSha256.trim().lowercase()
        FirmwareRepository.verifyFileSha256(binFile, sha256).getOrThrow()
        val totalSize = binFile.length()
        if (totalSize <= 0) {
            throw IllegalArgumentException("Archivo firmware vacío")
        }

        onProgress(5)
        tehLinkClient.executeCommand(
            transport,
            "ota_begin",
            JSONObject().put("size", totalSize).put("sha256", sha256),
            OTA_BEGIN_TIMEOUT_MS
        ).getOrThrow()

        var written = 0L
        var seq = 0
        binFile.inputStream().use { input ->
            val buffer = ByteArray(CHUNK_RAW_MAX)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                val chunk = if (read == buffer.size) buffer else buffer.copyOf(read)
                tehLinkClient.executeCommand(
                    transport,
                    "ota_chunk",
                    JSONObject()
                        .put("seq", seq)
                        .put("data", Base64.encodeToString(chunk, Base64.NO_WRAP)),
                    OTA_CHUNK_TIMEOUT_MS
                ).getOrThrow()
                written += read
                seq++
                val pct = (5 + (written * 85 / totalSize)).toInt().coerceIn(5, 90)
                onProgress(pct)
            }
        }

        onProgress(95)
        tehLinkClient.executeCommand(transport, "ota_finish", null, OTA_FINISH_TIMEOUT_MS).getOrThrow()
        onProgress(100)
        "OTA TEH-Link OK (${totalSize} bytes, SHA256 verificado)"
    }
}
