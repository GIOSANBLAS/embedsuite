package com.embedsuite.app.core.bruce

/** Atajos de consola — solo comandos documentados en Bruce Serial Wiki. */
object BruceConsoleChips {
    data class Chip(val label: String, val command: String)

    val chips = listOf(
        Chip("info", "info"),
        Chip("uptime", "uptime"),
        Chip("free", "free"),
        Chip("date", "date"),
        Chip("tone", "tone 1000 200"),
        Chip("nav esc", "nav esc"),
        Chip("nav up", "nav up"),
        Chip("nav down", "nav down"),
        Chip("nav select", "nav select"),
        Chip("loader RF", "loader open RF"),
        Chip("loader WiFi", "loader open WiFi"),
        Chip("subghz rx", "subghz rx 433920000"),
        Chip("ir rx", "ir rx"),
        Chip("rfid read", "rfid read 5000"),
        Chip("storage list", "storage list /"),
        Chip("SD free", "storage free sd"),
        Chip("reboot", "reboot")
    )
}
