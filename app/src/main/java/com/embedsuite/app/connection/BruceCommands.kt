package com.embedsuite.app.connection

import com.embedsuite.app.rf.RfFrequencyPresets

/**
 * Comandos alineados con la wiki Serial oficial de Bruce
 * (https://wiki.bruce.computer/controlling-device/serial/).
 *
 * Solo genera strings documentados. Lo no documentado (NFC CLI, ble scan,
 * setfrequency, scan, reset, rx 0) no se expone aquí.
 */
object BruceCommands {

    fun info() = "info"
    fun free() = "free"
    fun uptime() = "uptime"
    fun webui() = "webui"
    fun settings() = "settings"
    fun i2cScan() = "i2c scan"

    /** Captura Sub-GHz RAW documentada. */
    fun subGhzRxRaw(seconds: Int = 10): String {
        val sec = seconds.coerceIn(1, 120)
        return "subghz rx raw $sec"
    }

    /**
     * TX decodificado: `{hex_key} {freq_hz} {te} {count}`
     * Ejemplo wiki: `subghz tx 445533 433920000 174 10`
     */
    fun subGhzTx(
        hexKey: String,
        frequencyMhz: String = RfFrequencyPresets.DEFAULT,
        te: Int = 174,
        count: Int = 10
    ): String {
        val key = normalizeRfHexKey(hexKey)
        val hz = RfFrequencyPresets.toHz(frequencyMhz)
        val teSafe = te.coerceIn(50, 2000)
        val countSafe = count.coerceIn(1, 50)
        return "subghz tx $key $hz $teSafe $countSafe"
    }

    /**
     * TX desde archivo **ya presente** en SD/LittleFS del T-Embed.
     * Ejemplo: `subghz tx_from_file plug1_on.sub`
     */
    fun subGhzTxFromFile(deviceRelativePath: String): String {
        val path = sanitizeDeviceRelativePath(deviceRelativePath)
        return "subghz tx_from_file $path"
    }

    fun irRxRaw(seconds: Int = 10): String {
        val sec = seconds.coerceIn(1, 60)
        return "ir rx raw $sec"
    }

    /**
     * IR TX documentado: `ir tx NEC 04000000 08000000` (sin prefijo 0x).
     */
    fun irTx(protocol: String, addressWord: String, commandWord: String): String {
        val proto = protocol.trim().ifBlank { "NEC" }
        return "ir tx $proto ${normalizeIrWord(addressWord)} ${normalizeIrWord(commandWord)}"
    }

    /** Normaliza comandos IR legacy `ir tx NEC 0x00FF 0x00FF` → formato Bruce. */
    fun normalizeIrCommand(command: String): String {
        val trimmed = command.trim()
        val match = Regex(
            """(?i)^ir\s+tx\s+(\w+)\s+(0x)?([0-9a-f]+)\s+(0x)?([0-9a-f]+)$"""
        ).matchEntire(trimmed) ?: return trimmed
        return irTx(match.groupValues[1], match.groupValues[3], match.groupValues[5])
    }

    fun storageList(path: String = "/"): String {
        val p = path.trim().ifBlank { "/" }
        return if (p == "/") "storage list /" else "storage list ${sanitizeDeviceRelativePath(p)}"
    }

    fun storageRead(path: String): String = "storage read ${sanitizeDeviceRelativePath(path)}"

    fun storageMkdir(path: String): String =
        "storage mkdir ${sanitizeDeviceRelativePath(path)}"

    /**
     * Escribe archivo por Serial: tras el comando, Bruce lee líneas hasta `EOF`.
     * Solo fiable por USB (no WebUI `/cm`).
     */
    fun storageWrite(path: String, sizeHint: Int): String {
        val size = sizeHint.coerceIn(256, MAX_PUSH_BYTES + 256)
        return "storage write ${sanitizeDeviceRelativePath(path)} $size"
    }

    /** Path relativo para .sub empujados desde la app. */
    fun embedPushSubPath(signalId: Long): String = "BruceRF/embed_$signalId.sub"

    /** Límite de contenido para `storage write` (evita floods por Serial). */
    const val MAX_PUSH_BYTES = 48_000

    const val STORAGE_WRITE_EOF = "EOF"

    /**
     * Normaliza y valida texto a subir al T-Embed.
     * Rechaza EOF embebido (terminaría el protocolo), binario y payloads enormes.
     */
    fun preparePushContent(content: String): Result<String> {
        if (content.isBlank()) {
            return Result.failure(IllegalArgumentException("Contenido .sub vacío"))
        }
        if (content.length > MAX_PUSH_BYTES) {
            return Result.failure(
                IllegalArgumentException("Archivo demasiado grande (máx. $MAX_PUSH_BYTES bytes)")
            )
        }
        if (content.any { it.code < 9 || (it.code in 14..31) }) {
            return Result.failure(IllegalArgumentException("Contenido no textual / control chars"))
        }
        val normalized = content.replace("\r\n", "\n").replace('\r', '\n')
        if (normalized.lineSequence().any { it.trim() == STORAGE_WRITE_EOF }) {
            return Result.failure(
                IllegalArgumentException("El .sub no puede contener una línea EOF (protocolo Serial)")
            )
        }
        return Result.success(normalized)
    }

    const val TX_PUSH_USB_HINT =
        "RAW: se subirá el .sub por USB (storage write) y luego tx_from_file. Conecta OTG."

    /**
     * Mensaje cuando la app no puede TX porque el .sub solo existe en el teléfono.
     */
    const val TX_REQUIRES_DEVICE_FILE =
        "TX RAW requiere USB OTG para subir el .sub al T-Embed, o un archivo ya en Sync SD."

    const val NFC_CLI_UNSUPPORTED =
        "NFC por Serial no está en la wiki oficial de Bruce. Usa el menú NFC del T-Embed; " +
            "la app guarda dumps si llegan por consola."

    const val BLE_TRANSPORT_EXPERIMENTAL =
        "BLE como CLI Bruce es experimental (no documentado). Preferir USB OTG o WiFi WebUI (/cm)."

    const val WIFI_TRANSPORT_HINT =
        "WiFi: comandos y OTA vía BruceNet (192.168.4.1). RF Live mejor por USB."

    const val FREQ_LOCAL_HINT =
        "Frecuencia guardada en la app. Ajusta también RfModule/freq en el menú RF del T-Embed si hace falta."

    /** Chips CLI seguros para consola (solo documentados). */
    val safeConsoleChips: List<String> = listOf(
        info(), free(), uptime(),
        subGhzRxRaw(10), subGhzRxRaw(15),
        irRxRaw(10),
        irTx("NEC", "00FF", "00FF"),
        webui(), settings(), i2cScan(),
        storageList("/")
    )

    fun normalizeRfHexKey(raw: String): String {
        val hex = raw.removePrefix("0x").removePrefix("0X")
            .filter { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
            .uppercase()
        require(hex.isNotBlank()) { "Clave RF vacía" }
        return when {
            hex.length >= 6 -> hex.takeLast(6)
            else -> hex.padStart(6, '0')
        }
    }

    fun normalizeIrWord(raw: String): String {
        val hex = raw.removePrefix("0x").removePrefix("0X")
            .filter { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
            .uppercase()
        if (hex.isBlank()) return "00000000"
        if (hex.length >= 8) return hex.take(8)
        val value = hex.toLongOrNull(16) ?: 0L
        val b0 = (value and 0xFF).toInt()
        val b1 = ((value shr 8) and 0xFF).toInt()
        val b2 = ((value shr 16) and 0xFF).toInt()
        val b3 = ((value shr 24) and 0xFF).toInt()
        return "%02X%02X%02X%02X".format(b0, b1, b2, b3)
    }

    fun sanitizeDeviceRelativePath(path: String): String {
        val normalized = path.trim().trimStart('/').replace('\\', '/')
        require(normalized.isNotBlank()) { "Path vacío" }
        require(!normalized.contains("..")) { "Path inválido" }
        require(normalized.matches(Regex("""[\w./\-]+"""))) { "Path inválido" }
        return normalized
    }
}
