package com.embedsuite.app.connection

/** Perfil de firmware del T-Embed conectado (Bruce BLE C2). */
enum class FirmwareProfile(val label: String) {
    BRUCE("T-Embed Bruce"),
    UNKNOWN("Desconocido");

    companion object {
        fun fromPref(name: String?): FirmwareProfile = when (name) {
            "BRUCE", "BRUCE_TEHLINK" -> BRUCE
            null, "" -> BRUCE
            else -> entries.find { it.name == name } ?: BRUCE
        }

        val settingsDisplayOrder: List<FirmwareProfile> = listOf(BRUCE)

        fun supportsBruce(profile: FirmwareProfile): Boolean = profile == BRUCE
    }
}

fun FirmwareProfile.supportsBruce(): Boolean = FirmwareProfile.supportsBruce(this)
