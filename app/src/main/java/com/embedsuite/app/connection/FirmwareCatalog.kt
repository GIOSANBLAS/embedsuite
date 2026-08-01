package com.embedsuite.app.connection

enum class FirmwareSource {
    /** BruceDevices/firmware — T-Embed CC1101 Plus */
    OFFICIAL_BRUCE,
    /** Binario local elegido por el usuario */
    CUSTOM_LOCAL
}

enum class FirmwareRiskLevel {
    /** Bruce oficial para T-Embed */
    OFFICIAL,
    /** Custom — responsabilidad del usuario */
    CUSTOM
}

data class FirmwareRelease(
    val tagName: String,
    val name: String,
    val downloadUrl: String,
    val fileName: String,
    val isPrerelease: Boolean,
    val source: FirmwareSource = FirmwareSource.OFFICIAL_BRUCE,
    val localFilePath: String? = null,
    val isRecommended: Boolean = false,
    val description: String = "",
    /** SHA256 hex opcional para verificación post-descarga (64 chars). */
    val sha256Hex: String? = null
) {
    val riskLevel: FirmwareRiskLevel = when (source) {
        FirmwareSource.OFFICIAL_BRUCE -> FirmwareRiskLevel.OFFICIAL
        FirmwareSource.CUSTOM_LOCAL -> FirmwareRiskLevel.CUSTOM
    }

    val requiresDisclaimer: Boolean = riskLevel == FirmwareRiskLevel.CUSTOM

    val displayLabel: String = when (source) {
        FirmwareSource.OFFICIAL_BRUCE -> tagName
        FirmwareSource.CUSTOM_LOCAL -> name.ifBlank { fileName }
    }

    val isLocal: Boolean get() = localFilePath != null

    fun identityKey(): String = localFilePath ?: downloadUrl
}

object FirmwareCatalog {

    /** Nota mostrada junto al firmware recomendado. */
    const val RECOMMENDATION_REASON_KEY = "firmware_recommend_reason"

    fun markRecommended(releases: List<FirmwareRelease>): List<FirmwareRelease> {
        val recommended = pickRecommended(releases) ?: return releases
        return releases.map { release ->
            release.copy(
                isRecommended = release.identityKey() == recommended.identityKey()
            )
        }
    }

    /** Último Bruce estable (no prerelease) para T-Embed CC1101. */
    fun pickRecommended(releases: List<FirmwareRelease>): FirmwareRelease? {
        val stable = releases.filter { it.source == FirmwareSource.OFFICIAL_BRUCE && !it.isPrerelease }
        val pool = stable.ifEmpty { releases.filter { it.source == FirmwareSource.OFFICIAL_BRUCE } }
        if (pool.isEmpty()) return null
        return pool.reduce { best, current ->
            val bestVer = FirmwareRepository.extractVersion(best.tagName)
            val curVer = FirmwareRepository.extractVersion(current.tagName)
            if (FirmwareRepository.isNewer(curVer, bestVer)) current else best
        }
    }

    fun customFromFile(file: java.io.File, displayName: String? = null): FirmwareRelease {
        return FirmwareRelease(
            tagName = "CUSTOM",
            name = displayName ?: file.nameWithoutExtension,
            downloadUrl = "",
            fileName = file.name,
            isPrerelease = false,
            source = FirmwareSource.CUSTOM_LOCAL,
            localFilePath = file.absolutePath,
            isRecommended = false,
            description = ""
        )
    }
}
