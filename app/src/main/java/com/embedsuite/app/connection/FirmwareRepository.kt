package com.embedsuite.app.connection

import android.content.Context
import android.net.Uri
import com.embedsuite.app.security.SecureStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class FirmwareRepository(
    private val secureStore: SecureStore? = null
) {
    companion object {
        private const val MAX_FIRMWARE_BYTES = 16 * 1024 * 1024L

        fun validateDownloadUrl(url: String): Result<String> {
            val uri = Uri.parse(url)
            val host = uri.host.orEmpty()
            return if (uri.scheme == "https" && (host == "github.com" || host.endsWith(".githubusercontent.com"))) {
                Result.success(url)
            } else {
                Result.failure(IllegalArgumentException("URL de firmware no permitida"))
            }
        }
        fun extractVersion(text: String): String {
            val cleaned = text.trim()
            // Match version patterns like v0.18.0, 0.17.1, v1.0.0, etc.
            // Handles both with and without 'v' prefix
            Regex("""v?(\d+\.\d+(?:\.\d+)?(?:[-\w.]*)?)""", RegexOption.IGNORE_CASE)
                .find(cleaned)?.groupValues?.get(1)?.let { return it.lowercase() }
            return cleaned.lowercase().take(32)
        }

        fun normalizeVersionForCompare(version: String): String {
            // Normalize version strings for consistent comparison
            return version.lowercase()
                .replace(Regex("""^v+"""), "")
                .trim()
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

    private fun parseVersionParts(v: String): List<Int> {
        val normalized = normalizeVersionForCompare(v)
        return normalized.split('.')
            .mapNotNull { it.toIntOrNull() }
    }

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

    private fun githubAuthHeader(): String? {
        val token = secureStore?.getGithubToken().orEmpty().trim()
        return token.takeIf { it.isNotBlank() }?.let { "Bearer $it" }
    }

    private fun Request.Builder.withGithubAuth(): Request.Builder {
        githubAuthHeader()?.let { header("Authorization", it) }
        return header("Accept", "application/vnd.github+json")
            .header("User-Agent", "EMBED-SUITE-Android")
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    suspend fun fetchBruceReleases(): Result<List<FirmwareRelease>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.github.com/repos/pr3y/Bruce/releases?per_page=10")
                .withGithubAuth()
                .build()

            android.util.Log.d("FirmwareRepository", "Fetching releases from GitHub...")
            
            client.newCall(request).execute().use { response ->
                android.util.Log.d("FirmwareRepository", "GitHub API response code: ${response.code}")
                
                if (!response.isSuccessful) {
                    android.util.Log.w("FirmwareRepository", "GitHub API failed (${response.code}), using embedded catalog")
                    return@withContext Result.success(FirmwareCatalog.fallbackReleases())
                }

                val body = response.body?.string().orEmpty()
                android.util.Log.d("FirmwareRepository", "GitHub response length: ${body.length}")
                
                val releases = JSONArray(body)
                android.util.Log.d("FirmwareRepository", "Total releases found: ${releases.length()}")
                
                val results = mutableListOf<FirmwareRelease>()

                for (i in 0 until releases.length()) {
                    val release = releases.getJSONObject(i)
                    val assets = release.optJSONArray("assets") ?: continue
                    val tag = release.optString("tag_name", "unknown")
                    val name = release.optString("name", tag)
                    val prerelease = release.optBoolean("prerelease", false)
                    val bodyText = release.optString("body", "")
                    
                    android.util.Log.d("FirmwareRepository", "Processing release: $tag (prerelease=$prerelease)")

                    for (j in 0 until assets.length()) {
                        val asset = assets.getJSONObject(j)
                        val fileName = asset.optString("name", "")
                        android.util.Log.d("FirmwareRepository", "  Asset: $fileName")
                        
                        if (fileName.endsWith(".bin", ignoreCase = true) &&
                            (fileName.contains("bruce", ignoreCase = true) ||
                                fileName.contains("bruce", ignoreCase = true) ||
                                fileName.contains("lilygo-t-embed", ignoreCase = true))
                        ) {
                            // Buscar SHA256 con regex flexible
                            val sha256 = Regex("""SHA256:\s*([0-9a-fA-F]{64})""", RegexOption.IGNORE_CASE)
                                .find(bodyText)
                                ?.groupValues
                                ?.get(1)
                                ?.lowercase()
                            
                            // Si no encuentra con regex, buscar en formato código markdown
                            val sha256FromCode = if (sha256 == null) {
                                Regex("""```[a-fA-F0-9]{64}```""")
                                    .find(bodyText)
                                    ?.value
                                    ?.replace("```", "")
                                    ?.lowercase()
                            } else null
                            
                            val finalSha256 = sha256 ?: sha256FromCode
                            
                            results.add(
                                FirmwareRelease(
                                    tagName = tag,
                                    name = name,
                                    downloadUrl = asset.optString("browser_download_url"),
                                    fileName = fileName,
                                    isPrerelease = prerelease,
                                    source = FirmwareSource.OFFICIAL_BRUCE,
                                    sha256Hex = finalSha256
                                )
                            )
                            
                            android.util.Log.d("FirmwareRepository", "  ✓ Added: $tag, SHA256: $finalSha256")
                        }
                    }
                }

                android.util.Log.d("FirmwareRepository", "Total valid releases: ${results.size}")
                results.forEach { 
                    android.util.Log.d("FirmwareRepository", "  - ${it.tagName}: ${it.sha256Hex ?: "NO SHA256"}") 
                }
                
                if (results.isEmpty()) {
                    android.util.Log.w("FirmwareRepository", "No valid releases from GitHub, using embedded Bruce catalog")
                    Result.success(FirmwareCatalog.fallbackReleases())
                } else {
                    Result.success(FirmwareCatalog.mergeWithEmbedded(results))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FirmwareRepository", "Error fetching releases", e)
            Result.success(FirmwareCatalog.fallbackReleases())
        }
    }

    /** Releases oficiales Bruce para el catálogo principal. */
    suspend fun fetchDeviceFirmwares(): Result<List<FirmwareRelease>> = fetchBruceReleases()

    suspend fun fetchAllReleases(profile: FirmwareProfile = FirmwareProfile.BRUCE): Result<List<FirmwareRelease>> =
        fetchDeviceFirmwares()

    suspend fun importLocalBin(context: Context, uri: Uri, cacheDir: File): Result<FirmwareRelease> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (!cacheDir.exists()) cacheDir.mkdirs()
                val rawName = uri.lastPathSegment?.substringAfterLast('/') ?: "custom_firmware.bin"
                val safeName = sanitizeFirmwareFileName(rawName)
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

    suspend fun resolveFlashFile(
        context: Context,
        release: FirmwareRelease,
        cacheDir: File
    ): Result<File> = withContext(Dispatchers.IO) {
        release.localFilePath?.let { path ->
            val file = File(path)
            if (file.exists()) return@withContext Result.success(file)
            return@withContext Result.failure(Exception("Archivo local no encontrado: $path"))
        }

        if (!cacheDir.exists()) cacheDir.mkdirs()
        val safeName = sanitizeFirmwareFileName(release.fileName)
        val targetFile = File(cacheDir, "${release.tagName.replace(Regex("[^a-zA-Z0-9._-]"), "_")}_$safeName")
        ensureInsideDir(cacheDir, targetFile).getOrThrow()

        if (targetFile.exists() && targetFile.length() >= 1024) {
            release.sha256Hex?.let { expected ->
                verifyFileSha256(targetFile, expected).onSuccess {
                    return@withContext Result.success(targetFile)
                }
                targetFile.delete()
            } ?: return@withContext Result.success(targetFile)
        }

        release.bundledAssetPath?.let { assetPath ->
            copyBundledFirmware(context, assetPath, targetFile, release.sha256Hex).fold(
                onSuccess = { return@withContext Result.success(it) },
                onFailure = { android.util.Log.w("FirmwareRepository", "Bundled asset failed: ${it.message}") }
            )
        }

        downloadFirmware(release, targetFile).fold(
            onSuccess = { return@withContext Result.success(it) },
            onFailure = { downloadErr ->
                release.bundledAssetPath?.let { assetPath ->
                    copyBundledFirmware(context, assetPath, targetFile, release.sha256Hex)
                } ?: Result.failure(
                    Exception(
                        "${downloadErr.message}. Repo privado: importa .bin custom o configura token GitHub en Ajustes.",
                        downloadErr
                    )
                )
            }
        )
    }

    private fun copyBundledFirmware(
        context: Context,
        assetPath: String,
        targetFile: File,
        expectedSha256: String?
    ): Result<File> {
        return runCatching {
            context.assets.open(assetPath).use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            if (!targetFile.exists() || targetFile.length() < 1024) {
                targetFile.delete()
                throw IllegalStateException("Firmware embebido vacío o corrupto")
            }
            expectedSha256?.let { expected ->
                verifyFileSha256(targetFile, expected).getOrThrow()
            }
            targetFile
        }
    }

    suspend fun downloadFirmware(release: FirmwareRelease, targetFile: File): Result<File> =
        withContext(Dispatchers.IO) {
            try {
                if (release.source == FirmwareSource.OFFICIAL_BRUCE && release.sha256Hex.isNullOrBlank()) {
                    return@withContext Result.failure(
                        Exception("Release Bruce oficial sin SHA256 en notas — descarga rechazada")
                    )
                }
                val targetDir = targetFile.parentFile ?: return@withContext Result.failure(
                    Exception("Directorio destino inválido")
                )
                if (!targetDir.exists()) targetDir.mkdirs()
                ensureInsideDir(targetDir, targetFile).getOrThrow()

                if (release.downloadUrl.isBlank()) {
                    return@withContext Result.failure(Exception("URL de descarga no disponible"))
                }

                validateDownloadUrl(release.downloadUrl).getOrThrow()

                val request = Request.Builder()
                    .url(release.downloadUrl)
                    .withGithubAuth()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val hint = when (response.code) {
                            404 -> " (repo privado o release inexistente)"
                            401, 403 -> " (token GitHub inválido o sin permiso)"
                            else -> ""
                        }
                        return@withContext Result.failure(
                            Exception("Descarga fallida: HTTP ${response.code}$hint")
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
