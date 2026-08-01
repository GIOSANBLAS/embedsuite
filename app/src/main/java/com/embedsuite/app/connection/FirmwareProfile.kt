package com.embedsuite.app.connection

/** Perfil de firmware del T-Embed conectado o preferido en ajustes. */
enum class FirmwareProfile(val label: String) {
    XIBALBA("T-Embed Xibalba"),
    UNKNOWN("Desconocido");

    companion object {
        fun fromPref(name: String?): FirmwareProfile =
            entries.find { it.name == name } ?: XIBALBA

        val settingsDisplayOrder: List<FirmwareProfile> = listOf(XIBALBA)
    }
}
