package com.embedsuite.app.ai

enum class AiMode {
    LOCAL,
    GEMINI,
    OLLAMA
}

enum class AiActionType {
    EXECUTE_COMMAND,
    ANALYZE_ONLY,
    CHAT_ONLY
}

data class AiResponse(
    val message: String,
    val suggestedCommand: String? = null,
    val actionType: AiActionType = AiActionType.CHAT_ONLY,
    val confidence: Float = 0f
)

data class AiChatMessage(
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val commandExecuted: String? = null
)

data class SignalAnalysis(
    val protocol: String,
    val frequency: String,
    val summary: String,
    val threatLevel: String,
    val recommendations: List<String>
)
