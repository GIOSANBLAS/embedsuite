package com.embedsuite.app.core.bruce

/** Referencia de comandos CLI Bruce documentados (companion — no menú espejo). */
object BruceCliCatalog {

    data class Entry(
        val name: String,
        val command: String,
        val description: String,
        val category: String
    )

    val entries: List<Entry> = listOf(
        Entry("Info dispositivo", "info", "Versión Bruce, hardware, SD", "Sistema"),
        Entry("Uptime", "uptime", "Tiempo encendido", "Sistema"),
        Entry("Memoria libre", "free", "Heap y PSRAM", "Sistema"),
        Entry("Fecha", "date", "Reloj del T-Embed", "Sistema"),
        Entry("Reiniciar", "reboot", "Reboot soft", "Sistema"),
        Entry("Menú Bruce", "nav esc", "Volver al menú principal", "Nav"),
        Entry("Arriba", "nav up", "Navegación remota", "Nav"),
        Entry("Abajo", "nav down", "Navegación remota", "Nav"),
        Entry("Select", "nav select", "Confirmar en menú Bruce", "Nav"),
        Entry("Captura Sub-GHz 15s", "subghz rx 433920000 15", "Escuchar CC1101 @ 433.92 MHz", "RF"),
        Entry("TX ejemplo", "subghz tx AABBCC 433920000 174 10", "Transmitir clave decoded", "RF"),
        Entry("Barrido RF", "subghz scan 300000000 928000000", "Scan rango CC1101", "RF"),
        Entry("IR escuchar 10s", "ir rx 10", "Captura código IR", "IR"),
        Entry("IR NEC ejemplo", "ir tx NEC 00FF00FF 00FF00FF", "Enviar IR", "IR"),
        Entry("Leer NFC", "rfid read 5000", "Leer tag PN532", "NFC"),
        Entry("Listar SD", "storage list /bruce", "Explorar microSD", "Storage"),
        Entry("Espacio SD", "storage free sd", "Espacio libre SD", "Storage"),
        Entry("BadUSB archivo", "badusb run_from_file /badusb/payload.txt", "Ejecutar Ducky en SD", "BadUSB"),
        Entry("Beep", "tone 1000 200", "Confirmación auditiva", "Audio"),
        Entry("Abrir RF", "loader open RF", "Atajo menú Sub-GHz", "Loader"),
        Entry("Abrir WiFi", "loader open WiFi", "Atajo menú WiFi", "Loader"),
        Entry("Listar apps", "loader list", "Apps disponibles en Bruce", "Loader"),
        Entry("Apagar", "poweroff", "Apagado soft", "Sistema"),
        Entry("WiFi ON", "wifi on", "Activa radio WiFi", "WiFi"),
        Entry("ARP scan", "arp", "Tabla ARP (WiFi conectado)", "WiFi"),
        Entry("Leer archivo SD", "storage read /bruce/subghz/capture.sub", "Contenido texto de archivo", "Storage"),
        Entry("TX desde SD", "subghz tx_from_file /bruce/subghz/capture.sub false", "Replay .sub en SD", "RF")
    )

    fun categories(): List<String> = entries.map { it.category }.distinct()
    fun byCategory(category: String): List<Entry> =
        entries.filter { it.category.equals(category, ignoreCase = true) }
}
