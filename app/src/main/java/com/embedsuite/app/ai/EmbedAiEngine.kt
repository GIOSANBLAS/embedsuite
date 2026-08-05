package com.embedsuite.app.ai

import com.embedsuite.app.connection.DeviceConnectionManager
import com.embedsuite.app.connection.SignalEntry
import com.embedsuite.app.data.CapturedSignalEntity
import com.embedsuite.app.data.SignalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EmbedAiEngine(
    val preferences: AiPreferences,
    private val connectionManager: DeviceConnectionManager,
    private val signalRepository: SignalRepository
) {
    private val gemini = GeminiAiProvider()
    private val ollama = OllamaAiProvider()

    private val _messages = MutableStateFlow<List<AiChatMessage>>(listOf(welcomeMessage()))
    val messages: StateFlow<List<AiChatMessage>> = _messages.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    suspend fun processUserInput(input: String): AiResponse {
        _isProcessing.value = true
        try {
            addMessage("user", input)

            val response = when (preferences.getMode()) {
                AiMode.LOCAL -> processLocal(input)
                AiMode.GEMINI -> processGemini(input)
                AiMode.OLLAMA -> processOllama(input)
            }

            addMessage("assistant", response.message, response.suggestedCommand)

            if (preferences.isAutoExecute() &&
                response.actionType == AiActionType.EXECUTE_COMMAND &&
                response.suggestedCommand != null
            ) {
                val cmd = response.suggestedCommand.trim()
                if (cmd.startsWith("{")) {
                    connectionManager.sendTehLinkRaw(cmd).fold(
                        onSuccess = {
                            addMessage("system", "✓ TEH-Link enviado")
                        },
                        onFailure = { error ->
                            addMessage("system", "✗ Error: ${error.message}")
                        }
                    )
                } else {
                    addMessage("system", "✗ Auto-ejecución solo acepta JSON TEH-Link.")
                }
            }

            return response
        } finally {
            _isProcessing.value = false
        }
    }

    suspend fun analyzeLastSignal(): String {
        val signals = signalRepository.getRecent(1)
        if (signals.isEmpty()) {
            val msg = "No hay señales capturadas. Usa RF → CAPTURAR TEH-Link primero."
            addMessage("assistant", msg)
            return msg
        }

        val analysis = signals.firstOrNull()?.let { SignalAnalyzer.analyzeEntity(it) }
            ?: return Result.failure(Exception("No hay señales recientes para analizar"))
        val report = buildString {
            appendLine("ANÁLISIS DE SEÑAL — EMBED AI")
            appendLine("Protocolo: ${analysis.protocol}")
            appendLine("Frecuencia: ${analysis.frequency}")
            appendLine("Nivel: ${analysis.threatLevel}")
            appendLine()
            appendLine(analysis.summary)
            appendLine()
            appendLine("Recomendaciones:")
            analysis.recommendations.forEach { appendLine("→ $it") }
        }
        addMessage("assistant", report)
        return report
    }

    suspend fun generateSessionReport(): String {
        val signals = signalRepository.getRecent(100)
        val report = SignalAnalyzer.summarizeSession(signals)

        when (preferences.getMode()) {
            AiMode.GEMINI -> {
                if (preferences.getGeminiApiKey().isNotBlank()) {
                    gemini.chat(
                        apiKey = preferences.getGeminiApiKey(),
                        userMessage = "Genera un informe de auditoría RF profesional basado en estos datos:\n$report",
                        context = "T-Embed CC1101 Plus + Xibalba TEH-Link session"
                    ).onSuccess { enhanced ->
                        addMessage("assistant", enhanced)
                        return enhanced
                    }
                }
            }
            AiMode.OLLAMA -> {
                ollama.chat(
                    baseUrl = preferences.getOllamaHost(),
                    model = preferences.getOllamaModel(),
                    userMessage = "Genera un informe de auditoría RF profesional basado en estos datos:\n$report",
                    context = "T-Embed CC1101 Plus + Xibalba TEH-Link session"
                ).onSuccess { enhanced ->
                    addMessage("assistant", enhanced)
                    return enhanced
                }
            }
            AiMode.LOCAL -> Unit
        }

        addMessage("assistant", report)
        return report
    }

    fun analyzeSignalEntry(entry: SignalEntry): SignalAnalysis = SignalAnalyzer.analyze(entry)

    suspend fun analyzeCapturedSignal(signal: CapturedSignalEntity): String {
        val analysis = SignalAnalyzer.analyzeEntity(signal)
        val gps = if (signal.latitude != null) "GPS: ${signal.latitude}, ${signal.longitude}" else "Sin GPS"
        return buildString {
            appendLine("Análisis: ${analysis.protocol} @ ${analysis.frequency}")
            appendLine("Amenaza: ${analysis.threatLevel}")
            appendLine(gps)
            appendLine(analysis.summary)
            when {
                analysis.protocol.contains("PT2262", ignoreCase = true) || analysis.protocol.contains("Princeton", ignoreCase = true) ->
                    appendLine("→ Parece mando fijo 433.92 MHz (portón/garage). Replay viable vía TEH-Link.")
                analysis.protocol.contains("Keeloq", ignoreCase = true) ->
                    appendLine("→ Rolling code. Replay limitado; captura múltiples tramas.")
                analysis.protocol.equals("RAW", ignoreCase = true) ->
                    appendLine("→ Señal RAW. Analiza pulsos en tab Análisis RF.")
                else -> appendLine("→ Revisa biblioteca para retransmitir vía TEH-Link.")
            }
            analysis.recommendations.take(2).forEach { appendLine("• $it") }
        }
    }

    fun getContextualHint(activeTab: String): String = when (activeTab) {
        "dashboard" -> "Dashboard: verifica LINK OK antes de operar."
        "rf" -> "RF: usa CAPTURAR TEH-Link 15s en 433.92 MHz."
        "wireless" -> "WiFi: activa WAR-DRIVE para guardar APs con GPS."
        "nfc_ir" -> "NFC: acerca tag al T-Embed y pulsa LEER TAG."
        "terminal" -> "CLI: JSON TEH-Link. Ej: {\"cmd\":\"ping\"}"
        "ai" -> "Pregunta: 'Capturé esto, ¿qué es?' o 'Genera macro TEH-Link para...'"
        "map_tools" -> "Tools: exporta KML para Google Earth."
        else -> ""
    }

    fun clearChat() {
        _messages.value = listOf(welcomeMessage())
    }

    private suspend fun processLocal(input: String): AiResponse {
        val normalized = input.lowercase()

        if (normalized.contains("analiza") && (normalized.contains("señal") || normalized.contains("senal") || normalized.contains("ultima") || normalized.contains("última"))) {
            analyzeLastSignal()
            return AiResponse("Análisis completado. Revisa el mensaje anterior.", actionType = AiActionType.ANALYZE_ONLY)
        }

        if (normalized.contains("reporte") || normalized.contains("informe") || normalized.contains("resumen")) {
            generateSessionReport()
            return AiResponse("Reporte generado.", actionType = AiActionType.ANALYZE_ONLY)
        }

        if (normalized.contains("cuantas") || normalized.contains("cuántas") || normalized.contains("total")) {
            val count = signalRepository.count()
            return AiResponse("Biblioteca local: $count señales capturadas con GPS/metadata.")
        }

        return TehLinkActionSuggester.parse(input)
    }

    private suspend fun processOllama(input: String): AiResponse {
        val host = preferences.getOllamaHost()
        val model = preferences.getOllamaModel()
        if (host.isBlank() || model.isBlank()) {
            return AiResponse(
                "Modo OLLAMA requiere host y modelo. Configúralos en ajustes AI.",
                actionType = AiActionType.CHAT_ONLY
            )
        }

        val signalCount = signalRepository.count()
        val context = "Señales guardadas: $signalCount. T-Embed CC1101 Plus + Xibalba TEH-Link."

        return ollama.chat(host, model, input, context).fold(
            onSuccess = { text ->
                val cmd = ollama.extractCommand(text)
                AiResponse(
                    message = text,
                    suggestedCommand = cmd,
                    actionType = if (cmd != null) AiActionType.EXECUTE_COMMAND else AiActionType.CHAT_ONLY,
                    confidence = 0.85f
                )
            },
            onFailure = { error ->
                val fallback = TehLinkActionSuggester.parse(input)
                AiResponse(
                    message = "Ollama falló: ${error.message}. Usando motor local...\n\n${fallback.message}",
                    suggestedCommand = fallback.suggestedCommand,
                    actionType = fallback.actionType,
                    confidence = 0.6f
                )
            }
        )
    }

    private suspend fun processGemini(input: String): AiResponse {
        val apiKey = preferences.getGeminiApiKey()
        if (apiKey.isBlank()) {
            return AiResponse(
                "Modo GEMINI requiere API key. Configúrala abajo o cambia a modo LOCAL (offline).",
                actionType = AiActionType.CHAT_ONLY
            )
        }

        val signalCount = signalRepository.count()
        val context = "Señales guardadas: $signalCount. T-Embed CC1101 Plus + Xibalba TEH-Link."

        return gemini.chat(apiKey, input, context).fold(
            onSuccess = { text ->
                val cmd = gemini.extractCommand(text)
                AiResponse(
                    message = text,
                    suggestedCommand = cmd,
                    actionType = if (cmd != null) AiActionType.EXECUTE_COMMAND else AiActionType.CHAT_ONLY,
                    confidence = 0.9f
                )
            },
            onFailure = { error ->
                val fallback = TehLinkActionSuggester.parse(input)
                AiResponse(
                    message = "Gemini falló: ${error.message}. Usando motor local...\n\n${fallback.message}",
                    suggestedCommand = fallback.suggestedCommand,
                    actionType = fallback.actionType,
                    confidence = 0.6f
                )
            }
        )
    }

    private fun addMessage(role: String, content: String, command: String? = null) {
        _messages.value = _messages.value + AiChatMessage(role, content, commandExecuted = command)
    }

    private fun welcomeMessage() = AiChatMessage(
        role = "assistant",
        content = """
            EMBED AI v1.0 — Motor de inteligencia T-Embed Xibalba
            
            Modo LOCAL activo (offline, sin internet).
            Puedo sugerir acciones TEH-Link JSON y analizar señales.
            
            Prueba: "Captura RF 15 segundos" o "Analiza última señal"
        """.trimIndent()
    )
}
