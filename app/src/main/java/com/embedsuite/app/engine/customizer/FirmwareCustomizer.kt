package com.embedsuite.app.engine.customizer

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class CustomBuildStatus {
    LOCAL_STAGED,
    QUEUED,
    BUILDING,
    FAILED
}

data class CustomBuildRequest(
    val modules: Set<String>,
    val theme: String = "maya_dark",
    val hardeningFlags: Set<String> = emptySet()
)

data class CustomBuildJob(
    val jobId: String,
    val status: CustomBuildStatus,
    val request: CustomBuildRequest,
    val message: String = ""
)

/**
 * Local-only firmware customizer stub (Phase 2–3).
 *
 * Cloud compilation and signed artifact delivery are planned for Phase 4.
 * This class packages module selection into a manifest JSON that can later
 * be submitted to an OTA/flash request once cloud builds exist.
 */
object FirmwareCustomizer {

    private val ramSavingsPerModuleKb = mapOf(
        "evil_portal" to 48,
        "beacon_spam" to 32,
        "badusb" to 24,
        "nrf24_toolkit" to 20,
        "wardriving" to 16,
        "ir_toolkit" to 12,
        "nfc_toolkit" to 12,
        "subghz_analyzer" to 8
    )

    fun estimateRamSavings(request: CustomBuildRequest): Int {
        return request.modules.sumOf { ramSavingsPerModuleKb[it] ?: 4 }
    }

    fun generateManifestJson(request: CustomBuildRequest): String {
        return JSONObject().apply {
            put("schema", "embedsuite-custom-build/v1")
            put("theme", request.theme)
            put("modules", JSONArray(request.modules.toList()))
            put("hardening", JSONArray(request.hardeningFlags.toList()))
            put("estimated_ram_savings_kb", estimateRamSavings(request))
            put("build_mode", "local_staged")
        }.toString(2)
    }

    fun queueLocalBuild(request: CustomBuildRequest): CustomBuildJob {
        val jobId = "local-${UUID.randomUUID()}"
        return CustomBuildJob(
            jobId = jobId,
            status = CustomBuildStatus.LOCAL_STAGED,
            request = request,
            message = "Build local preparado — manifest listo para flash futuro (sin cloud)."
        )
    }
}
