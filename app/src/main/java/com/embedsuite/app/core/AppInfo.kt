package com.embedsuite.app.core

data class ChangelogEntry(
    val version: String,
    val date: String,
    val highlights: List<String>
)

data class TeamMember(
    val name: String,
    val role: String,
    val detail: String
)

data class OpenSourceLicense(
    val name: String,
    val license: String,
    val url: String
)

object AppInfo {

    val tagline = "Professional platform for Xibalba / Bruce TEH-Link companion control"

    val team = listOf(
        TeamMember(
            name = "GIOSÁNBLAS",
            role = "Desarrollador principal · Product Owner",
            detail = "Visión del producto, pruebas en campo y hardware LilyGO T-Embed CC1101 Plus"
        )
    )

    val changelog = listOf(
        ChangelogEntry(
            version = "1.0.0",
            date = "Ago 2026",
            highlights = listOf(
                "Foundation platform reset — EmbedSuite v1.0.0 architecture baseline",
                "Core device layer: profiles, capabilities, hardware kind resolver, profile store",
                "Core TEH-Link protocol surface with migration bridge to connection layer",
                "Engine stubs: workflows (.ewf), autopilot profiles, threat predictor, risk scorer",
                "UI: Spectrum radar + hardening traffic-light panel wired to dashboard",
                "Firmware catalog trimmed to Xibalba-0.19.0 Maya + custom local only"
            )
        )
    )

    val openSourceLicenses = listOf(
        OpenSourceLicense("Android Jetpack (Compose, Room, Navigation)", "Apache 2.0", "https://developer.android.com/jetpack"),
        OpenSourceLicense("Kotlin", "Apache 2.0", "https://kotlinlang.org/"),
        OpenSourceLicense("usb-serial-for-android (mik3y)", "Apache 2.0", "https://github.com/mik3y/usb-serial-for-android"),
        OpenSourceLicense("OkHttp (Square)", "Apache 2.0", "https://square.github.io/okhttp/"),
        OpenSourceLicense("osmdroid", "Apache 2.0", "https://github.com/osmdroid/osmdroid"),
        OpenSourceLicense("Play Services Location", "Google Terms", "https://developers.google.com/android/guides/overview"),
        OpenSourceLicense("AndroidX Security Crypto", "Apache 2.0", "https://developer.android.com/jetpack/androidx/releases/security")
    )

    val privacyPolicyText = """
POLÍTICA DE PRIVACIDAD — EMBED SUITE
Última actualización: julio 2026

1. RESPONSABLE
EMBED SUITE es una aplicación companion de uso personal desarrollada por GIOSÁNBLAS. No opera como servicio comercial ni recopila datos para publicidad.

2. DATOS QUE PROCESA LA APP
• Datos locales: señales RF, dispositivos WiFi/BLE, dumps NFC, macros, historial TX e IR — almacenados en el dispositivo Android (Room/SQLite).
• Ubicación (GPS): opcional, para war-driving, heatmap y georreferenciar capturas. Solo se usa si otorgas el permiso.
• Bluetooth / WiFi: escaneo de entorno inalámbrico en el teléfono; no se envían a servidores externos.
• USB: comunicación directa con el T-Embed CC1101 Plus vía OTG (TEH-Link / protocolo Xibalba).
• API key Gemini (opcional): si la configuras, se guarda cifrada en el dispositivo (EncryptedSharedPreferences). Solo se envía a Google Gemini cuando usas el modo AI cloud.
• Ollama (opcional): si configuras host/modelo en modo OLLAMA, las consultas se envían a tu servidor Ollama en la red local (LAN). No hay servidor propio de EMBED SUITE.

3. DATOS QUE NO RECOPILAMOS
• No hay analytics de terceros (Firebase Analytics, etc.).
• No vendemos ni compartimos datos personales.
• No hay cuenta de usuario ni login en servidores propios.

4. PERMISOS ANDROID
La app solicita permisos según la función: INTERNET (WiFi TEH-Link/OTA/Gemini/Ollama), ubicación, Bluetooth, notificaciones, USB host, micrófono (comandos por voz opcionales). Puedes denegarlos; algunas funciones quedarán limitadas.

5. BACKUP
Android Auto Backup / transferencia de dispositivo EXCLUYE: SecureStore (API Gemini), preferencias de app/AI, estado del widget y la base Room `embed_suite.db` (señales RF, dumps, GPS). Los exports que tú generes (JSON/HTML/.sub) sí pueden salir del teléfono si los compartes.

6. SEGURIDAD
Claves Gemini en EncryptedSharedPreferences (SecureStore). Comandos peligrosos bloqueados en CLI. Logs/consola redactan RAW/hex largos. Widget TX/RX protegido con token. La app se distribuye firmada para uso personal. Eres responsable de proteger tu keystore y exports.

7. CAMBIOS
Esta política puede actualizarse en futuras versiones. La versión vigente aparece en Acerca de.

8. CONTACTO
Desarrollador: GIOSÁNBLAS — proyecto EMBED SUITE (companion T-Embed CC1101 Plus / firmware Xibalba · TEH-Link).

9. WARDRIVING / RF / ESP
El escaneo WiFi/BLE, captura Sub-GHz con GPS y retransmisión RF pueden estar regulados en tu país. Solo usa la app en entornos autorizados (tu red, laboratorio, pentest con contrato). Las señales capturadas (p. ej. aperturas de garaje) son información sensible: protégelas y no las compartas.
""".trimIndent()
}
