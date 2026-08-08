package com.embedsuite.app.connection

sealed class OtaUpdateStatus {
    data object Unknown : OtaUpdateStatus()
    data object UpToDate : OtaUpdateStatus()
    data object Checking : OtaUpdateStatus()
    data class UpdateAvailable(
        val deviceVersion: String,
        val latestVersion: String,
        val release: FirmwareRelease,
        val sourceLabel: String = "Xibalba"
    ) : OtaUpdateStatus()
    data class Error(val message: String) : OtaUpdateStatus()
}

class OtaUpdateChecker(private val firmwareRepository: FirmwareRepository) {

    private var cachedStatus: OtaUpdateStatus = OtaUpdateStatus.Unknown
    private var lastCheckMs: Long = 0
    private val cacheTtlMs = 24 * 60 * 60 * 1000L

    suspend fun check(
        deviceFirmware: String,
        profile: FirmwareProfile = FirmwareProfile.XIBALBA,
        force: Boolean = false
    ): OtaUpdateStatus {
        if (!force && cachedStatus !is OtaUpdateStatus.Unknown && System.currentTimeMillis() - lastCheckMs < cacheTtlMs) {
            return cachedStatus
        }
        val deviceVer = FirmwareRepository.extractVersion(deviceFirmware)
        if (deviceVer.isBlank()) return OtaUpdateStatus.Unknown

        return firmwareRepository.fetchXibalbaReleases().fold(
            onSuccess = { releases ->
                val latest = FirmwareCatalog.pickRecommended(releases, profile)
                    ?: releases.firstOrNull { !it.isPrerelease }
                    ?: releases.firstOrNull()
                if (latest == null) {
                    cachedStatus = OtaUpdateStatus.UpToDate
                } else {
                    val latestVer = FirmwareRepository.extractVersion(latest.tagName)
                    cachedStatus = if (FirmwareRepository.isNewer(latestVer, deviceVer)) {
                        OtaUpdateStatus.UpdateAvailable(deviceVer, latestVer, latest, "Xibalba")
                    } else {
                        OtaUpdateStatus.UpToDate
                    }
                }
                lastCheckMs = System.currentTimeMillis()
                cachedStatus
            },
            onFailure = { e ->
                OtaUpdateStatus.Error(e.message ?: "Error OTA check")
            }
        )
    }
}
