package com.embedsuite.app.core.wifi

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

/** Transferencia de archivos pesados (.sub, .ir, scripts) vía HTTP al T-Embed. */
class WifiFileTransfer(
    private val host: String = WifiApManager.DEFAULT_HOST,
    private val port: Int = 80
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    suspend fun uploadFile(localFile: File, remotePath: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "http://$host:$port/upload"
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("path", remotePath)
                .addFormDataPart(
                    "file",
                    localFile.name,
                    localFile.asRequestBody("application/octet-stream".toMediaType())
                )
                .build()
            val request = Request.Builder().url(url).post(body).build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
                resp.body?.string() ?: "ok"
            }
        }
    }

    suspend fun downloadUrl(url: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching {
            client.newCall(Request.Builder().url(url).get().build()).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
                resp.body?.bytes() ?: error("empty body")
            }
        }
    }

    /** Descarga archivo de la SD del T-Embed vía WebUI Bruce. */
    suspend fun downloadFile(remotePath: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        val normalized = remotePath.trim().let { if (it.startsWith("/")) it else "/$it" }
        val candidates = listOf(
            "http://$host:$port/download?path=$normalized",
            "http://$host:$port/file?path=$normalized",
            "http://$host:$port/sd$normalized"
        )
        var lastErr: Throwable? = null
        for (url in candidates) {
            downloadUrl(url).fold(
                onSuccess = { return@withContext Result.success(it) },
                onFailure = { lastErr = it }
            )
        }
        Result.failure(lastErr ?: IllegalStateException("Download falló para $normalized"))
    }
}
