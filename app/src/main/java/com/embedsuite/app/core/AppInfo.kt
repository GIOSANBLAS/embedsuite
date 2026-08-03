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

    val tagline = "Xibalba Native · T-Embed CC1101 Plus · TEH-Link"

    val team = listOf(
        TeamMember(
            name = "GIOSÁNBLAS",
            role = "Desarrollador principal · Product Owner",
            detail = "Visión del producto, pruebas en campo y hardware LilyGO T-Embed CC1101 Plus"
        ),
        TeamMember(
            name = "Cursor",
            role = "IDE con IA · Socio de ingeniería",
            detail = "cursor.com — Agente AI que co-desarrolló EMBED SUITE: arquitectura Kotlin/Compose, integración TEH-Link/Xibalba, RF/USB/BLE y releases"
        )
    )

    val changelog = listOf(
        ChangelogEntry(
            version = "4.0.7",
            date = "Ago 2026",
            highlights = listOf(
                "Simbiosis Xibalba: telemetría CC1101 en vivo (paquetes/tiempo) vía get_action_state",
                "Captura con freq_mhz + capture_stop real; macros ejecutan run_action TEH-Link",
                "Perfiles/chips alineados al protocolo (sin CLI Bruce legacy)",
                "Mock: subghz_tx/replay; USB sigue siendo el camino diario"
            )
        ),
        ChangelogEntry(
            version = "4.0.6",
            date = "Ago 2026",
            highlights = listOf(
                "Pulido USB diario: auto-reconnect y arranque priorizan USB OTG",
                "WiFi/BLE marcados experimental en Tools y Ajustes",
                "Espectro/waterfall live oculto (stub TEH-Link listo para activar)",
                "Metadata unificada 4.0.6; manual docs Xibalba; OTA/replay/widget más robustos"
            )
        ),
        ChangelogEntry(
            version = "4.0.0",
            date = "Ago 2026",
            highlights = listOf(
                "Xibalba Native: app orientada 100% a T-Embed CC1101 Plus + firmware Xibalba",
                "Eliminación completa de código y UI legacy; solo TEH-Link",
                "Catálogo firmware solo releases Xibalba oficiales; custom .bin local",
                "OTA USB (TEH-Link); LinkDebugPanel en MapTools"
            )
        ),
        ChangelogEntry(
            version = "3.9.0",
            date = "Jul 2026",
            highlights = listOf(
                "Xibalba Symbiosis: perfil Xibalba por defecto en instalaciones nuevas",
                "TEH-Link primario en RF/NFC/IR/wardriving/OTA",
                "Splash, ajustes y copy orientados a T-Embed CC1101 Plus + te-embed-xibalba",
                "Consola TEH-Link JSON",
                "OTA Xibalba v0.16.5 catalog preservado; versión 3.9.0 build 19"
            )
        ),
        ChangelogEntry(
            version = "3.8.1",
            date = "Jul 2026",
            highlights = listOf(
                "Seguridad: widget TX/RX con token anti-broadcast; CLI redacta RAW/hex",
                "Validador TEH-Link; AI auto-ejecutar OFF por defecto",
                "Batería: BLE BALANCED, GPS 5s, stop BLE/GPS al ir a segundo plano",
                "Legal: checkbox onboarding + aviso WAR-DRIVE; backup excluye Room RF/GPS",
                "Fixes: wake lock campo, USB reconnect leak, sin runBlocking en onDestroy"
            )
        ),
        ChangelogEntry(
            version = "3.8.0",
            date = "Jul 2026",
            highlights = listOf(
                "TX RAW vía TEH-Link subghz_tx / replay",
                "Widget: botón TX ★ del favorito RF #1 con toasts de error/offline",
                "Dashboard: preview TX bloquea RAW sin USB y muestra motivo",
                "Hardening push: límite 48KB, sin línea EOF embebida, mkdir/write validados"
            )
        ),
        ChangelogEntry(
            version = "3.7.0",
            date = "Jul 2026",
            highlights = listOf(
                "Favoritos RF: estrella en biblioteca, filtro FAV, TX rápido en dashboard",
                "Field Session: nombre de misión + reporte HTML al detener (señales de la sesión + GPS)",
                "Compartir último reporte de campo desde Inicio",
                "Room v7: columna favorite; export/backup incluyen favoritos"
            )
        ),
        ChangelogEntry(
            version = "3.6.0",
            date = "Jul 2026",
            highlights = listOf(
                "Alineación hardware: acciones TEH-Link documentadas",
                "TX RF: subghz_tx / subghz_replay vía TEH-Link",
                "IR/NFC vía plugins ir_toolkit / nfc_toolkit",
                "USB preferido; WiFi/BLE transporte TEH-Link experimental"
            )
        ),
        ChangelogEntry(
            version = "3.5.1",
            date = "Jul 2026",
            highlights = listOf(
                "Seguridad: validación TEH-Link, redacción RAW en debug, HTTPS estricto en release",
                "MockTransport DEBUG, macros validados, escaneos se detienen al cambiar tab",
                "i18n RF/mapa/wireless, accesibilidad en botones neon, SecureStore warning"
            )
        ),
        ChangelogEntry(
            version = "3.5.0",
            date = "Jul 2026",
            highlights = listOf(
                "RF Live: spectrum/waterfall desde stream TEH-Link (cuando disponible)",
                "Inspector de ondas con timings µs + botón SCAN",
                "Deep link RF: scroll + highlight en biblioteca",
                "Hardening: flash persistente, paths seguros, singleTop, bootloader USB"
            )
        ),
        ChangelogEntry(
            version = "3.4.0",
            date = "Jul 2026",
            highlights = listOf(
                "Multilenguaje: Español, English, Português + idioma del sistema",
                "Manual interactivo in-app (Acerca de) + docs/manual HTML",
                "Flash: catálogo Xibalba + custom .bin con disclaimer de responsabilidad",
                "Panel Link Debug (200 líneas, filtros, copiar)",
                "Import señales .sub/.ir/.nfc desde SD del T-Embed",
                "Widget: última freq + RX 15s + estado LINK",
                "RF rules → ejecutar macro + tap notificación abre biblioteca"
            )
        ),
        ChangelogEntry(
            version = "3.3.0",
            date = "Jul 2026",
            highlights = listOf(
                "Modo AI Ollama local (LAN) — tercer motor junto a LOCAL y Gemini",
                "Mapas offline osmdroid: precachear zona GPS + caché persistente",
                "Automatizaciones RF: reglas protocolo/frecuencia → notify, tag o alert",
                "Room v6 con tabla rf_automation_rules + backup incluido"
            )
        ),
        ChangelogEntry(
            version = "3.2.0",
            date = "Jul 2026",
            highlights = listOf(
                "Selector MHz RF (315/433/868/915) + modo campo configurable",
                "BLE GATT notify/subscribe en dispositivos BLE",
                "Export Flipper .ir y .nfc desde pantalla NFC/IR",
                "Acciones rápidas Dashboard (RX 15s, TX última, INFO)",
                "Comparador RF A vs B mejorado en Análisis",
                "Reconexión reactiva a cambios de transporte en Ajustes"
            )
        ),
        ChangelogEntry(
            version = "3.1.4",
            date = "Jul 2026",
            highlights = listOf(
                "Fix crash en Acerca de al mostrar el icono (adaptive icon → PNG)"
            )
        ),
        ChangelogEntry(
            version = "3.1.3",
            date = "Jul 2026",
            highlights = listOf(
                "Pantalla Acerca de con historial de versiones",
                "Política de privacidad y licencias open source in-app",
                "Fixes de estabilidad: USB, timeouts CLI, reconexión inteligente"
            )
        ),
        ChangelogEntry(
            version = "3.1.2",
            date = "Jul 2026",
            highlights = listOf(
                "Icono launcher personalizado (Android hacker + T-Embed CC1101 Plus)",
                "APK release firmado"
            )
        ),
        ChangelogEntry(
            version = "3.1.1",
            date = "Jul 2026",
            highlights = listOf(
                "Release-ready: migraciones Room v4→v5, R8/ProGuard",
                "Backup completo (7 tablas), CrashLogger, signing config",
                "Tests unitarios: Flipper, Firmware, RfProtocolDecoder"
            )
        ),
        ChangelogEntry(
            version = "3.1.0",
            date = "Jul 2026",
            highlights = listOf(
                "ViewModels en pantallas principales",
                "Import macros TEH-Link JSON, export Flipper, perfiles SCENARIO",
                "GATT write UI, replay preview, SecureStore sin fallback plaintext"
            )
        ),
        ChangelogEntry(
            version = "3.0.0",
            date = "Jul 2026",
            highlights = listOf(
                "Dashboard, RF hub (spectrum/waterfall/biblioteca/análisis)",
                "WiFi/BLE scanner, war-driving, heatmap osmdroid",
                "NFC/IR, CLI TEH-Link, AI (LOCAL + Gemini), macros, flash esptool",
                "Modo campo FGS, widget, Quick Settings tile, OTA check",
                "Reportes HTML/PDF, onboarding, permisos guiados"
            )
        ),
        ChangelogEntry(
            version = "2.0.0",
            date = "2026",
            highlights = listOf(
                "Jetpack Compose UI cyberpunk/matrix",
                "Conexión USB serial + parser TEH-Link JSON",
                "Captura Sub-GHz básica y log de señales"
            )
        ),
        ChangelogEntry(
            version = "1.0.0",
            date = "2026",
            highlights = listOf(
                "Concepto inicial EMBED SUITE",
                "Companion Android para T-Embed CC1101 Plus",
                "Terminal TEH-Link USB OTG hacia firmware Xibalba"
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
