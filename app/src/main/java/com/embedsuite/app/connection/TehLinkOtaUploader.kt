package com.embedsuite.app.connection

import android.util.Base64
import org.json.JSONObject
import java.io.File

/**
 * OTA firmware upload for T-Embed Xibalba via TEH-Link (USB CDC).
 * Protocol: ota_begin → ota_chunk (sequential) → ota_finish with SHA256 verification.
 *
 * After ota_finish the device writes SHA256 incrementally (mbedtls_sha256_update) and
 * validates. EmbedSuite MUST call ota_status afterwards to show the user whether the
 * flash integrity check actually passed before rebooting.
 */
class TehLinkOtaUploader(private val tehLinkClient: TehLinkClient) {

    companion object {
        /** Max raw bytes per chunk (device TEH_LINK_OTA_B64_MAX = 4096). */
        const val CHUNK_RAW_MAX = 3072
        private const val OTA_BEGIN_TIMEOUT_MS = 15_000L
        private const val OTA_CHUNK_TIMEOUT_MS = 30_000L
        private const val OTA_FINISH_TIMEOUT_MS = 60_000L
        private const val OTA_VERIFY_TIMEOUT_MS = 8_000L
    }

    data class OtaResult(
        val totalBytes: Long,
        val sha256Verified: Boolean,
        val otaState: String
    )

    suspend fun upload(
        transport: TEmbedTransport,
        binFile: File,
        expectedSha256: String,
        onProgress: (Int) -> Unit
    ): Result<OtaResult> = runCatching {
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
                val pct = (5 + (written * 82 / totalSize)).toInt().coerceIn(5, 87)
                onProgress(pct)
            }
        }

        onProgress(92)
        tehLinkClient.executeCommand(transport, "ota_finish", null, OTA_FINISH_TIMEOUT_MS).getOrThrow()
        onProgress(95)

        /* Paso final IMPRESCINDIBLE introducido en Xibalba 0.17.1:
         * confirmar que el lado del dispositivo marcó sha256_verified = true
         * antes de recomendar reboot al usuario. */
        kotlinx.coroutines.delay(600)
        val finalStatus = tehLinkClient.getOtaStatus(transport).getOrElse {
            TehLinkOtaStatus(state = "complete", bytesWritten = written, totalSize = totalSize, sha256Verified = false)
        }
        onProgress(100)

        OtaResult(
            totalBytes = totalSize,
            sha256Verified = finalStatus.sha256Verified,
            otaState = finalStatus.state
        )
    }
}

