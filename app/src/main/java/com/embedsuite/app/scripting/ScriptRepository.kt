package com.embedsuite.app.scripting

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface ScriptRepository {
    val categories: List<ScriptCategory> get() = ScriptCategory.values().toList()
    fun scripts(): List<Script>
    fun byId(id: String): Script? = scripts().firstOrNull { it.id == id }
    fun byCategory(cat: ScriptCategory): List<Script> = scripts().filter { it.category == cat }
}

class BuiltInScriptRepository : ScriptRepository {

    private val _reload = MutableStateFlow(0)
    val reloadTick: StateFlow<Int> = _reload

    fun invalidate() { _reload.value = _reload.value + 1 }

    override fun scripts(): List<Script> = BUILTINS

    companion object {
        private val BUILTINS: List<Script> = listOf(
            Script(
                id = "TEHLINK_JS_EVIL_PORTAL_START_GOOGLE",
                title = "Evil Portal · Plantilla Google",
                summary = "Arranca SoftAP 192.168.4.1: Google Sign-in. Las credenciales se recolectan en el ring buffer del firmware y se emiten por events NDJSON.",
                category = ScriptCategory.EVIL_PORTAL,
                dialect = ScriptDialect.TEHLINK_JS,
                pluginId = "evil_portal",
                action = "start",
                requiresAuditUnlock = true,
                icon = "wifi_tethering",
                defaultParams = mapOf("template" to "google", "ssid" to "Google Free WiFi", "channel" to 6),
                parameters = listOf(
                    ScriptParameter("ssid", "SSID falso", "string", "Google Free WiFi", true),
                    ScriptParameter("template", "Plantilla", "enum", "google", true, listOf("google", "facebook", "instagram", "microsoft", "netflix", "generic")),
                    ScriptParameter("channel", "Canal WiFi", "int", "6", true, listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11"))
                )
            ),
            Script(
                id = "TEHLINK_JS_EVIL_PORTAL_START_FACEBOOK",
                title = "Evil Portal · Plantilla Facebook",
                summary = "Arranca SoftAP con plantilla Facebook login.",
                category = ScriptCategory.EVIL_PORTAL,
                dialect = ScriptDialect.TEHLINK_JS,
                pluginId = "evil_portal",
                action = "start",
                requiresAuditUnlock = true,
                icon = "wifi_tethering",
                defaultParams = mapOf("template" to "facebook", "ssid" to "Facebook Guest WiFi", "channel" to 1)
            ),
            Script(
                id = "TEHLINK_JS_EVIL_PORTAL_STOP",
                title = "Evil Portal · STOP",
                summary = "Detener SoftAP + HTTP server y borrar listeners.",
                category = ScriptCategory.EVIL_PORTAL,
                dialect = ScriptDialect.TEHLINK_JS,
                pluginId = "evil_portal",
                action = "stop",
                requiresAuditUnlock = true
            ),
            Script(
                id = "TEHLINK_JS_EVIL_PORTAL_CREDS",
                title = "Evil Portal · Leer credenciales capturadas",
                summary = "Dump ring buffer credenciales (últimas 100).",
                category = ScriptCategory.EVIL_PORTAL,
                dialect = ScriptDialect.TEHLINK_JS,
                pluginId = "evil_portal",
                action = "creds",
                requiresAuditUnlock = true
            ),
            Script(
                id = "TEHLINK_JS_EVIL_PORTAL_CLEAR_CREDS",
                title = "Evil Portal · Limpiar credenciales",
                summary = "Vaciar buffer de credenciales capturadas.",
                category = ScriptCategory.EVIL_PORTAL,
                dialect = ScriptDialect.TEHLINK_JS,
                pluginId = "evil_portal",
                action = "clear_creds",
                requiresAuditUnlock = true
            ),
            Script(
                id = "TEHLINK_JS_EVIL_PORTAL_STATUS",
                title = "Evil Portal · Estado",
                summary = "Leer estado running/stop, plantilla, SSID, uptime, cred count.",
                category = ScriptCategory.EVIL_PORTAL,
                dialect = ScriptDialect.TEHLINK_JS,
                pluginId = "evil_portal",
                action = "status"
            ),
            Script(
                id = "TEHLINK_JS_BEACON_SPAM_RANDOM",
                title = "Beacon Spam · Random N SSIDs",
                summary = "Generar N tramas Beacon 802.11 aleatorias (MAC random 02:FE:xx), hopping canales 1/6/11.",
                category = ScriptCategory.BEACON_SPAM,
                dialect = ScriptDialect.TEHLINK_JS,
                pluginId = "beacon_spam",
                action = "start",
                requiresAuditUnlock = true,
                icon = "blur_on",
                defaultParams = mapOf("mode" to "random", "count" to 100, "channels" to listOf(1,6,11), "tx_power" to 20),
                parameters = listOf(
                    ScriptParameter("count", "Cuenta SSIDs", "int", "100", true),
                    ScriptParameter("channels", "Hopping", "string", "1,6,11", true)
                )
            ),
            Script(
                id = "TEHLINK_JS_BEACON_SPAM_CSV",
                title = "Beacon Spam · Lista CSV fija",
                summary = "Lista de SSIDs predefinida (separadas por coma) en orden secuencial.",
                category = ScriptCategory.BEACON_SPAM,
                dialect = ScriptDialect.TEHLINK_JS,
                pluginId = "beacon_spam",
                action = "start",
                requiresAuditUnlock = true,
                defaultParams = mapOf(
                    "mode" to "csv",
                    "ssids" to listOf("Free Public WiFi","Starbucks WiFi","McDonalds Free WiFi","Hotel Guest","Airport Free WiFi","AT&T Free WiFi","Xfinity WiFi","Library Public WiFi")
                ),
                parameters = listOf(
                    ScriptParameter("ssids", "Lista SSIDs (CSV)", "text", "Free Public WiFi,Starbucks WiFi,McDonalds Free WiFi,Hotel Guest", true)
                )
            ),
            Script(
                id = "TEHLINK_JS_BEACON_SPAM_STOP",
                title = "Beacon Spam · STOP",
                summary = "Matar tarea beacon_task y desactivar modo promiscuo.",
                category = ScriptCategory.BEACON_SPAM,
                dialect = ScriptDialect.TEHLINK_JS,
                pluginId = "beacon_spam",
                action = "stop",
                requiresAuditUnlock = true
            ),
            Script(
                id = "TEHLINK_JS_BEACON_SPAM_STATUS",
                title = "Beacon Spam · Estado",
                summary = "running/stop, paquetes enviados, canal actual, MAC utilizada.",
                category = ScriptCategory.BEACON_SPAM,
                dialect = ScriptDialect.TEHLINK_JS,
                pluginId = "beacon_spam",
                action = "status"
            ),
            Script(
                id = "SYSTEM_RECON",
                title = "Recon · get_info + get_status + list_actions",
                summary = "Trío de comandos básicos para identificar perfil del T-Embed.",
                category = ScriptCategory.RECON,
                dialect = ScriptDialect.MACRO_SEQUENCE,
                icon = "info"
            ),
            Script(
                id = "WIFI_SCAN_30S",
                title = "WiFi Scan 30s",
                summary = "Arrancar escaneo WiFi del dispositivo 30 segundos y volcar APs en list_actions data.",
                category = ScriptCategory.WIFI,
                pluginId = "wifi_toolkit",
                action = "scan_start",
                defaultParams = mapOf("seconds" to 30)
            ),
            Script(
                id = "RF_CAPTURE_15S_433",
                title = "RF Captura 15s · 433.92 MHz",
                summary = "SubGHz analyzer captura en 433.92 MHz por 15 segundos.",
                category = ScriptCategory.RF,
                pluginId = "subghz_analyzer",
                action = "capture_start",
                defaultParams = mapOf("seconds" to 15, "freq_mhz" to "433.92")
            ),
            Script(
                id = "IR_SNIFF_10S",
                title = "IR Sniff 10s",
                summary = "Escuchar receptor IR 10 segundos.",
                category = ScriptCategory.IR,
                pluginId = "ir_toolkit",
                action = "rx_start",
                defaultParams = mapOf("seconds" to 10)
            ),
            Script(
                id = "NFC_READ",
                title = "NFC Read (NTAG/MIFARE)",
                summary = "Lectura NFC inmediata si hay tag en campo.",
                category = ScriptCategory.NFC,
                pluginId = "nfc_toolkit",
                action = "read"
            ),
            Script(
                id = "CRYPTO_SHA256",
                title = "Crypto · SHA-256 (input texto)",
                summary = "Calcular SHA-256 sobre input string.",
                category = ScriptCategory.CRYPTO,
                pluginId = "crypto_toolkit",
                action = "sha256",
                defaultParams = mapOf("input" to "hola mundo"),
                parameters = listOf(ScriptParameter("input", "Texto", "text", "hola mundo", true))
            ),
            Script(
                id = "CRYPTO_AES_ENCRYPT",
                title = "Crypto · AES-256-GCM encrypt",
                summary = "Cifrado AES-GCM 256 bits con clave por defecto del secure store.",
                category = ScriptCategory.CRYPTO,
                pluginId = "crypto_toolkit",
                action = "aes_encrypt",
                defaultParams = mapOf("input" to "payload test", "key_tag" to "default")
            ),
            Script(
                id = "BADUSB_DEMO_NOTEPAD",
                title = "BadUSB · Payload demo (Notepad)",
                summary = "HID payload para abrir notepad y escribir demo en hosts Windows.",
                category = ScriptCategory.BADUSB,
                pluginId = "badusb",
                action = "run_script",
                defaultParams = mapOf(
                    "script" to "GUI r\nDELAY 500\nSTRING notepad\nENTER\nDELAY 800\nSTRING Hola desde Embed Suite / Xibalba BadUSB demo\n"
                )
            ),

            /* ========== BLE AD SPAM ========== */
            Script(
                id = "BLEAD_APPLEJUICE_START",
                title = "BLE Spam · AppleJuice",
                summary = "Advertising spoof Apple AirPods pairing pop-up (AppleJuice) a 30 Hz.",
                category = ScriptCategory.BLE_SPAM,
                pluginId = "ble_ad_spam",
                action = "start",
                requiresAuditUnlock = true,
                icon = "bluetooth",
                defaultParams = mapOf("campaign" to 0, "hz" to 30)
            ),
            Script(
                id = "BLEAD_SWIFTPAIR_START",
                title = "BLE Spam · SwiftPair",
                summary = "Microsoft SwiftPair discoverable advertisements.",
                category = ScriptCategory.BLE_SPAM,
                pluginId = "ble_ad_spam",
                action = "start",
                requiresAuditUnlock = true,
                icon = "laptop_windows",
                defaultParams = mapOf("campaign" to 1, "hz" to 20)
            ),
            Script(
                id = "BLEAD_FINDMY_START",
                title = "BLE Spam · FindMy",
                summary = "Fake Apple FindMy accessory beacons.",
                category = ScriptCategory.BLE_SPAM,
                pluginId = "ble_ad_spam",
                action = "start",
                requiresAuditUnlock = true,
                icon = "location_searching",
                defaultParams = mapOf("campaign" to 2, "hz" to 15)
            ),
            Script(
                id = "BLEAD_HOMEKIT_START",
                title = "BLE Spam · HomeKit",
                summary = "HomeKit accessory setup advertising frames.",
                category = ScriptCategory.BLE_SPAM,
                pluginId = "ble_ad_spam",
                action = "start",
                requiresAuditUnlock = true,
                icon = "home",
                defaultParams = mapOf("campaign" to 3, "hz" to 15)
            ),
            Script(
                id = "BLEAD_CUSTOM_HEX_START",
                title = "BLE Spam · ADV data custom HEX",
                summary = "Payload 31 bytes custom en hex.",
                category = ScriptCategory.BLE_SPAM,
                pluginId = "ble_ad_spam",
                action = "start",
                requiresAuditUnlock = true,
                defaultParams = mapOf("campaign" to 4, "hz" to 10,
                    "custom_hex" to "0201060303AAFE1616AAFE10EE01424243"),
                parameters = listOf(
                    ScriptParameter("custom_hex", "ADV data HEX (0..62 chars)", "text",
                        "0201060303AAFE1616AAFE10EE01424243", true)
                )
            ),
            Script(
                id = "BLEAD_STOP",
                title = "BLE Spam · STOP",
                summary = "Detener campaña BLE ADV spam actual.",
                category = ScriptCategory.BLE_SPAM,
                pluginId = "ble_ad_spam",
                action = "stop",
                requiresAuditUnlock = true
            ),
            Script(
                id = "BLEAD_STATUS",
                title = "BLE Spam · Estado",
                summary = "running / packets / campaign actual.",
                category = ScriptCategory.BLE_SPAM,
                pluginId = "ble_ad_spam",
                action = "status"
            ),

            /* ========== WIFI OFFENSIVE ========== */
            Script(
                id = "WOFF_PROBE_1611_START",
                title = "WiFi Probe Sniffer · Canales 1,6,11",
                summary = "Captura Probe Request 2.4 GHz hopping 1/6/11. Auto-off 5 min.",
                category = ScriptCategory.WIFI_OFFENSIVE,
                pluginId = "wifi_offensive",
                action = "probe_start",
                requiresAuditUnlock = true,
                icon = "wifi_find",
                defaultParams = mapOf("channels" to "1,6,11")
            ),
            Script(
                id = "WOFF_PROBE_ALL_START",
                title = "WiFi Probe Sniffer · 1..13",
                summary = "Hopping todos canales 2.4 GHz.",
                category = ScriptCategory.WIFI_OFFENSIVE,
                pluginId = "wifi_offensive",
                action = "probe_start",
                requiresAuditUnlock = true,
                icon = "wifi_find",
                defaultParams = mapOf("channels" to "1,2,3,4,5,6,7,8,9,10,11,12,13")
            ),
            Script(
                id = "WOFF_PROBE_STOP",
                title = "WiFi Probe · STOP",
                summary = "Detener sniffer modo promiscuo.",
                category = ScriptCategory.WIFI_OFFENSIVE,
                pluginId = "wifi_offensive",
                action = "probe_stop",
                requiresAuditUnlock = true
            ),
            Script(
                id = "WOFF_PROBE_FLUSH",
                title = "WiFi Probe · Flush buffer en JSON",
                summary = "Devuelve probes dedupe acumulados con SSID/MAC/RSSI/vendor OUI.",
                category = ScriptCategory.WIFI_OFFENSIVE,
                pluginId = "wifi_offensive",
                action = "probe_flush"
            ),
            Script(
                id = "WOFF_DEAUTH_BC_START",
                title = "WiFi Deauth · Broadcast (prueba)",
                summary = "Ráfaga Deauth+Disassoc a FF:FF:FF:FF:FF:FF en canal 6 a 20 pps.",
                category = ScriptCategory.WIFI_OFFENSIVE,
                pluginId = "wifi_offensive",
                action = "deauth_start",
                requiresAuditUnlock = true,
                icon = "wifi_off",
                defaultParams = mapOf(
                    "bssid" to "AA:BB:CC:DD:EE:FF",
                    "sta" to "FF:FF:FF:FF:FF:FF",
                    "channel" to 6,
                    "pps" to 20
                ),
                parameters = listOf(
                    ScriptParameter("bssid", "BSSID objetivo", "string", "AA:BB:CC:DD:EE:FF", true),
                    ScriptParameter("channel", "Canal", "int", "6", true,
                        (1..13).map { it.toString() }),
                    ScriptParameter("pps", "Paquetes/seg", "int", "20", true)
                )
            ),
            Script(
                id = "WOFF_DEAUTH_STOP",
                title = "WiFi Deauth · STOP",
                summary = "Matar ráfaga Deauth.",
                category = ScriptCategory.WIFI_OFFENSIVE,
                pluginId = "wifi_offensive",
                action = "deauth_stop",
                requiresAuditUnlock = true
            ),
            Script(
                id = "WOFF_STATUS",
                title = "WiFi Ofensivo · Estado",
                summary = "probe/deauth running + counters.",
                category = ScriptCategory.WIFI_OFFENSIVE,
                pluginId = "wifi_offensive",
                action = "status"
            ),

            /* ========== MOUSEJACK NRF24 ========== */
            Script(
                id = "MJ_SCAN_5S",
                title = "Mousejack · Scan 5s (Logitech)",
                summary = "Hopping canales NRF24L01 Logitech detectando dongles sin encrypt.",
                category = ScriptCategory.MOUSEJACK,
                pluginId = "mousejack",
                action = "scan",
                requiresAuditUnlock = true,
                icon = "mouse",
                defaultParams = mapOf("ms" to 5000)
            ),
            Script(
                id = "MJ_INJECT_GUI_R",
                title = "Mousejack · Inject GUI+r (dongle 0)",
                summary = "Injecta HID report modifier=GUI key=r sobre primer dongle descubierto.",
                category = ScriptCategory.MOUSEJACK,
                pluginId = "mousejack",
                action = "inject_report",
                requiresAuditUnlock = true,
                icon = "keyboard_command_key",
                defaultParams = mapOf(
                    "addr" to "00:00:00:00:00",
                    "modifier" to 0x08,
                    "keys" to "150000000000"
                ),
                parameters = listOf(
                    ScriptParameter("addr", "Dongle addr (5 bytes HH:HH..)", "string", "00:00:00:00:00", true)
                )
            ),
            Script(
                id = "MJ_DUCKY_NOTEPAD",
                title = "Mousejack · Ducky abrir notepad",
                summary = "STRING notepad + ENTER sobre dongle addr.",
                category = ScriptCategory.MOUSEJACK,
                pluginId = "mousejack",
                action = "play_ducky",
                requiresAuditUnlock = true,
                defaultParams = mapOf(
                    "addr" to "00:00:00:00:00",
                    "script" to "GUI r\nDELAY 600\nSTRING notepad\nENTER\nDELAY 800\nSTRING Mousejack desde EmbedSuite + Xibalba"
                )
            ),
            Script(
                id = "MJ_CLEAR_DONGLES",
                title = "Mousejack · Limpiar lista",
                summary = "Limpia el buffer interno de dongles descubiertos.",
                category = ScriptCategory.MOUSEJACK,
                pluginId = "mousejack",
                action = "clear_dongles"
            ),

            /* ========== SUBGHZ TOOLS ========== */
            Script(
                id = "SG_SPEC_433_START",
                title = "Spectrum Analyzer · 380..450 MHz",
                summary = "Barrido RSSI SubGHz 433 MHz ISM con 25 kHz step. Muestra heatmap en SpectrumScreen.",
                category = ScriptCategory.SUBGHZ_TOOLS,
                pluginId = "subghz_tools",
                action = "spectrum_start",
                icon = "sensors",
                defaultParams = mapOf("f_start" to 380.0, "f_end" to 450.0, "step" to 0.025, "pps" to 100)
            ),
            Script(
                id = "SG_SPEC_868_START",
                title = "Spectrum Analyzer · 863..870 MHz",
                summary = "Banda SRD 868 EU.",
                category = ScriptCategory.SUBGHZ_TOOLS,
                pluginId = "subghz_tools",
                action = "spectrum_start",
                icon = "sensors",
                defaultParams = mapOf("f_start" to 863.0, "f_end" to 870.0, "step" to 0.025, "pps" to 100)
            ),
            Script(
                id = "SG_SPEC_STOP",
                title = "Spectrum · STOP",
                summary = "Detener barrido spectrum y volver CC1101 idle.",
                category = ScriptCategory.SUBGHZ_TOOLS,
                pluginId = "subghz_tools",
                action = "spectrum_stop"
            ),
            Script(
                id = "SG_DEC_433_OOK",
                title = "Auto-Decoder · 433.92 OOK",
                summary = "CC1101 RX OOK feeds decoder_registry (Keeloq/PT2262/OOK generic).",
                category = ScriptCategory.SUBGHZ_TOOLS,
                pluginId = "subghz_tools",
                action = "rx_decode",
                icon = "satellite_alt",
                defaultParams = mapOf("freq" to 433.92, "mod" to "OOK", "bitrate_khz" to 3.9)
            ),
            Script(
                id = "SG_DEC_STOP",
                title = "Auto-Decoder · STOP",
                summary = "Detener RX decoder.",
                category = ScriptCategory.SUBGHZ_TOOLS,
                pluginId = "subghz_tools",
                action = "rx_decode_stop"
            ),
            Script(
                id = "SG_REPLAY_DEMO",
                title = "Replay · Demo HEX 3 veces",
                summary = "Reproduce un raw HEX predefinido 3 veces.",
                category = ScriptCategory.SUBGHZ_TOOLS,
                pluginId = "subghz_tools",
                action = "replay_frame",
                requiresAuditUnlock = true,
                defaultParams = mapOf(
                    "hex" to "2A00AAAAAAAAA9555556699965A5",
                    "repeats" to 3,
                    "pause_ms" to 40
                )
            ),

            /* ========== NFC CLONE/WRITE ========== */
            Script(
                id = "NFC_READ_MIFARE_1K",
                title = "NFC Clone · Leer Mifare 1K (default keys)",
                summary = "Dump completo 1024 bytes con 16 keys default (FF F7 D3 00 B0 4D). Requiere tag sobre PN532.",
                category = ScriptCategory.NFC_CLONE,
                pluginId = "nfc_clone",
                action = "read_mifare",
                icon = "savings",
                defaultParams = mapOf(
                    "keys_csv" to "FFFFFFFFFFFF,A0A1A2A3A4A5,D3F7D3F7D3F7,000000000000,B0B1B2B3B4B5,4D3A99C351DD"
                )
            ),
            Script(
                id = "NFC_WRITE_MIFARE_1K",
                title = "NFC Clone · Write dump al target",
                summary = "Escribe un dump hex a Mifare Classic 1K (UID sector 0 omitido por seguridad).",
                category = ScriptCategory.NFC_CLONE,
                pluginId = "nfc_clone",
                action = "write_mifare",
                requiresAuditUnlock = true,
                icon = "save",
                parameters = listOf(
                    ScriptParameter("dump_hex", "1024 bytes en HEX (2048 chars)", "text", "", true)
                ),
                defaultParams = mapOf("dest_uid" to "", "dump_hex" to "", "force_uid_write" to 0)
            ),
            Script(
                id = "NFC_NTAG_WRITE_URL",
                title = "NFC NTAG · Write NDEF URL",
                summary = "Escribe URI record tipo 'U' (https://...) en páginas 4..N de NTAG 213/215/216.",
                category = ScriptCategory.NFC_CLONE,
                pluginId = "nfc_clone",
                action = "write_ntag_url",
                icon = "link",
                parameters = listOf(
                    ScriptParameter("url", "URL completa", "text", "https://example.com", true)
                ),
                defaultParams = mapOf("url" to "https://example.com")
            ),
            Script(
                id = "NFC_NTAG_WRITE_WIFI",
                title = "NFC NTAG · Write WiFi WSC QR",
                summary = "Registro MIME application/vnd.wfa.wsc (WPA2 PSK) — tap to connect en Android 10+.",
                category = ScriptCategory.NFC_CLONE,
                pluginId = "nfc_clone",
                action = "write_ntag_wifi",
                icon = "qr_code_scanner",
                parameters = listOf(
                    ScriptParameter("ssid", "SSID", "string", "EmbedSuite", true),
                    ScriptParameter("password", "Contraseña (8..63)", "string", "12345678", true),
                    ScriptParameter("auth", "Auth", "enum", "1", true, listOf("0 Abierto", "1 WPA2 PSK", "2 WPA3 SAE"))
                ),
                defaultParams = mapOf("ssid" to "EmbedSuite", "password" to "12345678", "auth" to 1)
            )
        )
    }
}
