package com.embedsuite.app.engine.autopilot

enum class AutopilotProfile {
    AUDIT,
    DEFENSIVE,
    STEALTH
}

data class RiskScore(
    val value: Int,
    val label: String = "",
    val profile: AutopilotProfile = AutopilotProfile.AUDIT
) {
    init {
        require(value in 0..100) { "RiskScore must be 0..100" }
    }
}
