package com.embedsuite.app.core.bruce

/**
 * Limitaciones documentadas de Bruce stock (Serial Wiki + ble_api).
 * https://github.com/BruceDevices/firmware/wiki/Serial
 */
object BruceLimits {
    const val NO_CLI =
        "Bruce stock no expone esta función por CLI serial/BLE. Configúrala en el menú del T-Embed."

    const val NO_OTA =
        "OTA inalámbrica no disponible en Bruce stock. Flashea vía USB con esptool."

    const val NO_SPECTRUM_STREAM =
        "Bruce stock no expone streaming espectral por WebSocket. Usa captura local (.sub) o subghz rx."

    const val NO_FILE_UPLOAD_BLE =
        "Subida de archivos grandes requiere WiFi WebUI. Conéctate al AP del T-Embed (192.168.4.1) y usa Forja o Archivos Bruce."

    const val WIFI_UPLOAD_HINT =
        "Conecta el teléfono al WiFi del T-Embed antes de subir .txt / .sub a la SD."

    const val BADUSB_HINT =
        "Sube el .txt a la SD y ejecuta: badusb run_from_file /ruta/payload.txt"

    const val JSON_REJECTED =
        "JSON TEH-Link obsoleto. Usa comandos CLI Bruce (texto) o la terminal integrada."
}
