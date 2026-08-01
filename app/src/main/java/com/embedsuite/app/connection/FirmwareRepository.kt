package com.embedsuite.app.connection

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class FirmwareRepository {

    companion object {
        private const val MAX_FIRMWARE_BYTES = 16 * 1024 * 1024L

        fun validateDownloadUrl(url: String): Result<String> {
            val uri = android.net.Uri.parse(url)
            val host = uri.host.orEmpty()
            return if (uri.scheme == "https" && (host == "github.com" || host.endsWith(".githubusercontent.com"))) {
                Result.success(url)
            } else {
                Result.failure(IllegalArgumentException("URL de firmware no permitida"))
            }
        }
        fun extractVersion(text: String): String {
            val cleaned = text.trim()
            Regex("""v?(\d+\.\d+(?:\.\d+)?(?:[-\w.]*)?)""", RegexOption.IGNORE_CASE)
                .find(cleaned)?.groupValues?.get(1)?.let { return it.lowercase() }
            return cleaned.lowercase().take(32)
        }

        fun isNewer(latest: String, current: String): Boolean {
            val l = parseVersionParts(latest)
            val c = parseVersionParts(current)
            for (i in 0 until maxOf(l.size, c.size)) {
                val lv = l.getOrElse(i) { 0 }
                val cv = c.getOrElse(i) { 0 }
                if (lv != cv) return lv > cv
            }
            return false
        }

        fun sanitizeFirmwareFileName(raw: String): String {
            val base = File(raw).name
                .replace(Regex("""[^\w.\-]"""), "_")
                .trim('.', ' ')
                .ifBlank { "firmware.bin" }
            return if (base.endsWith(".bin", ignoreCase = true)) base else "$base.bin"
        }

        fun ensureInsideDir(baseDir: File, target: File): Result<File> {
            val basePath = baseDir.canonicalFile
            val resolved = target.canonicalFile
            return if (resolved.path.startsWith(basePath.path + File.separator) || resolved == basePath) {
                Result.success(resolved)
            } else {
                Result.failure(IllegalArgumentException("Ruta fuera del directorio permitido"))
            }
        }

        private fun parseVersionParts(v: String): List<Int> =
            v.replace(Regex("""[^\d.]"""), "")
                .split('.')
                .mapNotNull { it.toIntOrNull() }

        fun computeFileSha256Hex(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var read = input.read(buffer)
                while (read > 0) {
                    digest.update(buffer, 0, read)
                    read = input.read(buffer)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }

        fun verifyFileSha256(file: File, expectedHex: String): Result<Unit> {
            val normalized = expectedHex.trim().lowercase()
            if (!Regex("""^[0-9a-f]{64}$""").matches(normalized)) {
                return Result.failure(IllegalArgumentException("SHA256 esperado inválido"))
            }
            val actual = computeFileSha256Hex(file)
            return if (actual.equals(normalized, ignoreCase = true)) {
                Result.success(Unit)
            } else {
                Result.failure(
                    IllegalStateException("SHA256 no coincide (esperado=$normalized actual=$actual)")
                )
            }
        }
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    suspend fun fetchTEmbedReleases(): Result<List<FirmwareRelease>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.github.com/repos/BruceDevices/firmware/releases?per_page=10")
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "EMBED-SUITE-Android")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("GitHub API error: HTTP ${response.code}")
                    )
                }

                val body = response.body?.string().orEmpty()
                val releases = JSONArray(body)
                val results = mutableListOf<FirmwareRelease>()

                for (i in 0 until releases.length()) {
                    val release = releases.getJSONObject(i)
                    val assets = release.optJSONArray("assets") ?: continue
                    val tag = release.optString("tag_name", "unknown")
                    val name = release.optString("name", tag)
                    val prerelease = release.optBoolean("prerelease", false)

                    for (j in 0 until assets.length()) {
                        val asset = assets.getJSONObject(j)
                        val fileName = asset.optString("name", "")
                        if (fileName.contains("T-Embed", ignoreCase = true) &&
                            fileName.contains("CC1101", ignoreCase = true) &&
                            fileName.endsWith(".bin", ignoreCase = true)
                        ) {
                            results.add(
                                FirmwareRelease(
                                    tagName = tag,
                                    name = name,
                                    downloadUrl = asset.optString("browser_download_url"),
                                    fileName = fileName,
                                    isPrerelease = prerelease,
                                    source = FirmwareSource.OFFICIAL_BRUCE
                                )
                            )
                        }
                    }
                }

                Result.success(FirmwareCatalog.markRecommended(results))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchXibalbaReleases(): Result<List<FirmwareRelease>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.github.com/repos/GIOSANBLAS/te-embed-xibalba/releases?per_page=10")
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "EMBED-SUITE-Android")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.success(listOf(FirmwareCatalog.XIBALBA_FALLBACK_V0161))
                }

                val body = response.body?.string().orEmpty()
                val releases = JSONArray(body)
                val results = mutableListOf<FirmwareRelease>()

                for (i in 0 until releases.length()) {
                    val release = releases.getJSONObject(i)
                    val assets = release.optJSONArray("assets") ?: continue
                    val tag = release.optString("tag_name", "unknown")
                    val name = release.optString("name", tag)
                    val prerelease = release.optBoolean("prerelease", false)

                    for (j in 0 until assets.length()) {
                        val asset = assets.getJSONObject(j)
                        val fileName = asset.optString("name", "")
                        if (fileName.endsWith(".bin", ignoreCase = true) &&
                            (fileName.contains("xibalba", ignoreCase = true) ||
                                fileName.contains("te-embed", ignoreCase = true))
                        ) {
                            val sha256 = release.optString("body", "")
                                .let { bodyText ->
                                    Regex("""SHA256:\s*([0-9a-fA-F]{64})""").find(bodyText)?.groupValues?.get(1)
                                }
                            results.add(
                                FirmwareRelease(
                                    tagName = tag,
                                    name = name,
                                    downloadUrl = asset.optString("browser_download_url"),
                                    fileName = fileName,
                                    isPrerelease = prerelease,
                                    source = FirmwareSource.OFFICIAL_XIBALBA,
                                    sha256Hex = sha256?.lowercase()
                                )
                            )
                        }
                    }
                }

                if (results.isEmpty()) {
                    Result.success(listOf(FirmwareCatalog.XIBALBA_FALLBACK_V0161))
                } else {
                    Result.success(results)
                }
            }
        } catch (_: Exception) {
            Result.success(listOf(FirmwareCatalog.XIBALBA_FALLBACK_V0161))
        }
    }

    suspend fun fetchAllReleases(profile: FirmwareProfile = FirmwareProfile.AUTO): Result<List<FirmwareRelease>> =
        withContext(Dispatchers.IO) {
            val bruce = fetchTEmbedReleases().getOrElse { emptyList() }
            val xibalba = fetchXibalbaReleases().getOrElse { emptyList() }
            val merged = bruce + xibalba
            if (merged.isEmpty()) {
                Result.failure(Exception("No se encontraron releases de firmware"))
            } else {
                Result.success(FirmwareCatalog.markRecommended(merged, profile))
            }
        }

    suspend fun importLocalBin(context: Context, uri: Uri, cacheDir: File): Result<FirmwareRelease> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (!cacheDir.exists()) cacheDir.mkdirs()
                val rawName = uri.lastPathSegment?.substringAfterLast('/') ?: "custom_firmware.bin"
                val safeName = Companion.sanitizeFirmwareFileName(rawName)
                val target = File(cacheDir, "custom_${System.currentTimeMillis()}_$safeName")
                ensureInsideDir(cacheDir, target).getOrThrow()
                context.contentResolver.openInputStream(uri)?.use { input ->
                    target.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var total = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            total += read
                            if (total > MAX_FIRMWARE_BYTES) {
                                target.delete()
                                throw IllegalArgumentException("Firmware demasiado grande (máx 16 MB)")
                            }
                            output.write(buffer, 0, read)
                        }
                    }
                } ?: throw IllegalStateException("No se pudo leer el archivo .bin")

                if (target.length() < 1024) {
                    target.delete()
                    throw IllegalArgumentException("El archivo parece demasiado pequeño para un firmware válido.")
                }

                val release = FirmwareCatalog.customFromFile(target, safeName.removeSuffix(".bin"))
                release.sha256Hex?.let { expected ->
                    verifyFileSha256(target, expected).getOrThrow()
                }
                release
            }
        }

    suspend fun resolveFlashFile(release: FirmwareRelease, cacheDir: File): Result<File> =
        withContext(Dispatchers.IO) {
            release.localFilePath?.let { path ->
                val file = File(path)
                if (file.exists()) return@withContext Result.success(file)
                return@withContext Result.failure(Exception("Archivo local no encontrado: $path"))
            }
            downloadFirmware(release, cacheDir)
        }

    suspend fun downloadFirmware(release: FirmwareRelease, targetDir: File): Result<File> =
        withContext(Dispatchers.IO) {
            try {
                if (release.source == FirmwareSource.OFFICIAL_XIBALBA && release.sha256Hex.isNullOrBlank()) {
                    return@withContext Result.failure(
                        Exception("Release Xibalba oficial sin SHA256 en notas — descarga rechazada")
                    )
                }
                if (!targetDir.exists()) targetDir.mkdirs()
                val safeName = Companion.sanitizeFirmwareFileName(release.fileName)
                val targetFile = File(targetDir, safeName)
                ensureInsideDir(targetDir, targetFile).getOrThrow()

                validateDownloadUrl(release.downloadUrl).getOrThrow()

                val request = Request.Builder()
                    .url(release.downloadUrl)
                    .header("User-Agent", "EMBED-SUITE-Android")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            Exception("Descarga fallida: HTTP ${response.code}")
                        )
                    }
                    val body = response.body
                        ?: return@withContext Result.failure(Exception("Respuesta vacía del servidor"))
                    body.byteStream().use { input ->
                        targetFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (!targetFile.exists() || targetFile.length() < 1024) {
                        targetFile.delete()
                        return@withContext Result.failure(
                            Exception("Archivo descargado vacío o demasiado pequeño")
                        )
                    }
                    val computedSha = computeFileSha256Hex(targetFile)
                    release.sha256Hex?.let { expected ->
                        verifyFileSha256(targetFile, expected).getOrElse { err ->
                            targetFile.delete()
                            return@withContext Result.failure(err)
                        }
                    }
                    Result.success(targetFile)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
