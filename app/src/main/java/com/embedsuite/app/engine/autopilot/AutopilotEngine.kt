package com.embedsuite.app.engine.autopilot

interface AutopilotEngine {
    val profile: AutopilotProfile
    fun start()
    fun stop()
    fun isRunning(): Boolean
}

class NoOpAutopilotEngine(
    override val profile: AutopilotProfile = AutopilotProfile.AUDIT
) : AutopilotEngine {

    private var running = false

    override fun start() {
        running = true
    }

    override fun stop() {
        running = false
    }

    override fun isRunning(): Boolean = running
}
