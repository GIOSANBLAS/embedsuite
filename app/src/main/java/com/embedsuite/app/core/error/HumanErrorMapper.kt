package com.embedsuite.app.core.error

object HumanErrorMapper {

    fun map(throwable: Throwable?): String = mapMessage(throwable?.message)

    fun mapMessage(message: String?): String {
        val raw = message?.trim().orEmpty()
        if (raw.isBlank()) return "Ocurrió un error desconocido. Revisa la conexión USB/WiFi."
        val lower = raw.lowercase()
        return when {
            lower.startsWith("nl_not_understood:") ->
                "No entendí el comando. Prueba JSON TEH-Link o frases como «ping», «escanea wifi», «emparejar»."
            lower.contains("auth_required") || lower.contains("unauthorized") ->
                "Se requiere emparejamiento TEH-Link. Mantén pulsado el botón del T-Embed e intenta «emparejar»."
            lower.contains("usb") && (lower.contains("disconnect") || lower.contains("not found")) ->
                "USB desconectado. Conecta el T-Embed con OTG y vuelve a intentar."
            lower.contains("timeout") || lower.contains("timed out") ->
                "Tiempo de espera agotado. El dispositivo no respondió a tiempo."
            lower.contains("pair_window") || lower.contains("pairing") ->
                "Ventana de emparejamiento cerrada. Repite el gesto de emparejamiento en el hardware."
            lower.contains("not connected") || lower.contains("offline") ->
                "Sin enlace activo. Conecta por USB o WiFi TEH-Link."
            lower.contains("teh-link solo disponible") || lower.contains("solo disponible con t-embed xibalba") ->
                "El perfil no es Xibalba (o se perdió tras reconectar). Pulsa Conectar, espera LINK verde y reintenta."
            lower.contains("invalid workflow") || lower.contains("workflow") && lower.contains("invalid") ->
                "El archivo .ewf no es un workflow válido."
            else -> raw
        }
    }
}
