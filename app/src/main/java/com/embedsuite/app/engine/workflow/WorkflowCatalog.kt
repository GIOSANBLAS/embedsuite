package com.embedsuite.app.engine.workflow

object WorkflowCatalog {

    fun builtIns(): List<Workflow> = listOf(
        Workflow(
            id = "recon_get_info",
            name = "Recon · get_info",
            description = "Consulta info del dispositivo al conectar",
            trigger = WorkflowTrigger.ON_CONNECT,
            steps = listOf(
                WorkflowStep.Action(
                    id = "info",
                    label = "get_info",
                    pluginId = "device",
                    action = "get_info"
                )
            )
        ),
        Workflow(
            id = "wifi_scan_chain",
            name = "WiFi scan chain",
            description = "Escaneo WiFi con pausa entre pasos",
            steps = listOf(
                WorkflowStep.Action(
                    id = "wifi",
                    label = "wifi_scan",
                    pluginId = "wifi_toolkit",
                    action = "wifi_scan"
                ),
                WorkflowStep.Delay(id = "wait", label = "Pausa 2s", delayMs = 2000L),
                WorkflowStep.Action(
                    id = "status",
                    label = "get_status",
                    pluginId = "device",
                    action = "get_status"
                )
            )
        ),
        Workflow(
            id = "subghz_rx_15s",
            name = "Sub-GHz RX 15s",
            description = "Captura Sub-GHz durante 15 segundos",
            steps = listOf(
                WorkflowStep.Action(
                    id = "rx",
                    label = "subghz_rx",
                    pluginId = "subghz",
                    action = "rx",
                    params = mapOf("duration_ms" to "15000")
                )
            )
        )
    )
}
