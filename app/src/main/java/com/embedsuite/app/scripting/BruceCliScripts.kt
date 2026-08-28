package com.embedsuite.app.scripting

/** Presets ejecutables vía CLI serial Bruce documentado (wiki Serial). */
object BruceCliScripts {
    val all: List<Script> = listOf(
        Script(
            id = "CLI_INFO",
            title = "info · dispositivo",
            summary = "Versión firmware, board, perfil y estado básico.",
            category = ScriptCategory.RECON,
            dialect = ScriptDialect.BRUCE_CLI,
            cliCommand = "info",
            icon = "info"
        ),
        Script(
            id = "CLI_UPTIME",
            title = "uptime",
            summary = "Tiempo encendido del T-Embed.",
            category = ScriptCategory.RECON,
            dialect = ScriptDialect.BRUCE_CLI,
            cliCommand = "uptime"
        ),
        Script(
            id = "CLI_FREE",
            title = "free · heap",
            summary = "Memoria libre del dispositivo.",
            category = ScriptCategory.RECON,
            dialect = ScriptDialect.BRUCE_CLI,
            cliCommand = "free"
        ),
        Script(
            id = "CLI_DATE",
            title = "date",
            summary = "Fecha/hora RTC del dispositivo.",
            category = ScriptCategory.RECON,
            dialect = ScriptDialect.BRUCE_CLI,
            cliCommand = "date"
        ),
        Script(
            id = "CLI_NAV_ESC",
            title = "nav esc · menú principal",
            summary = "Vuelve al menú raíz de Bruce.",
            category = ScriptCategory.RECON,
            dialect = ScriptDialect.BRUCE_CLI,
            cliCommand = "nav esc"
        ),
        Script(
            id = "CLI_LOADER_RF",
            title = "loader open RF",
            summary = "Abre el módulo Sub-GHz en el T-Embed.",
            category = ScriptCategory.RF,
            dialect = ScriptDialect.BRUCE_CLI,
            cliCommand = "loader open RF"
        ),
        Script(
            id = "CLI_SUBGHZ_RX_433",
            title = "subghz rx · 433.92 MHz",
            summary = "Escucha Sub-GHz en 433920000 Hz hasta Ctrl+C en consola del dispositivo.",
            category = ScriptCategory.RF,
            dialect = ScriptDialect.BRUCE_CLI,
            cliCommand = "subghz rx 433920000"
        ),
        Script(
            id = "CLI_SUBGHZ_SCAN_ISM",
            title = "subghz scan · 300–928 MHz",
            summary = "Barrido puntual documentado en Bruce Serial (no streaming).",
            category = ScriptCategory.RF,
            dialect = ScriptDialect.BRUCE_CLI,
            cliCommand = "subghz scan 300000000 928000000"
        ),
        Script(
            id = "CLI_IR_RX",
            title = "ir rx",
            summary = "Escucha infrarrojo (requiere tag IR en campo).",
            category = ScriptCategory.IR,
            dialect = ScriptDialect.BRUCE_CLI,
            cliCommand = "ir rx"
        ),
        Script(
            id = "CLI_RFID_READ",
            title = "rfid read · 5 s",
            summary = "Lectura NFC/RFID con timeout 5000 ms.",
            category = ScriptCategory.NFC,
            dialect = ScriptDialect.BRUCE_CLI,
            cliCommand = "rfid read 5000"
        ),
        Script(
            id = "CLI_STORAGE_LIST",
            title = "storage list /",
            summary = "Lista raíz del almacenamiento interno/SD.",
            category = ScriptCategory.RECON,
            dialect = ScriptDialect.BRUCE_CLI,
            cliCommand = "storage list /"
        ),
        Script(
            id = "CLI_STORAGE_FREE",
            title = "storage free sd",
            summary = "Espacio libre en microSD.",
            category = ScriptCategory.RECON,
            dialect = ScriptDialect.BRUCE_CLI,
            cliCommand = "storage free sd"
        ),
        Script(
            id = "CLI_TONE",
            title = "tone · beep 1 kHz",
            summary = "Beep corto en el buzzer del T-Embed.",
            category = ScriptCategory.RECON,
            dialect = ScriptDialect.BRUCE_CLI,
            cliCommand = "tone 1000 200"
        ),
        Script(
            id = "CLI_LOADER_LIST",
            title = "loader list",
            summary = "Lista apps del menú Bruce.",
            category = ScriptCategory.RECON,
            dialect = ScriptDialect.BRUCE_CLI,
            cliCommand = "loader list"
        ),
        Script(
            id = "CLI_LOADER_IR",
            title = "loader open IR",
            summary = "Abre módulo infrarrojo.",
            category = ScriptCategory.IR,
            dialect = ScriptDialect.BRUCE_CLI,
            cliCommand = "loader open IR"
        ),
        Script(
            id = "CLI_WIFI_ON",
            title = "wifi on",
            summary = "Enciende radio WiFi del T-Embed.",
            category = ScriptCategory.RECON,
            dialect = ScriptDialect.BRUCE_CLI,
            cliCommand = "wifi on"
        ),
        Script(
            id = "CLI_ARP",
            title = "arp",
            summary = "Tabla ARP (requiere WiFi conectado en el T-Embed).",
            category = ScriptCategory.RECON,
            dialect = ScriptDialect.BRUCE_CLI,
            cliCommand = "arp"
        ),
        Script(
            id = "CLI_REBOOT",
            title = "reboot",
            summary = "Reinicia el dispositivo Bruce.",
            category = ScriptCategory.RECON,
            dialect = ScriptDialect.BRUCE_CLI,
            cliCommand = "reboot",
            requiresAuditUnlock = true
        )
    )
}
