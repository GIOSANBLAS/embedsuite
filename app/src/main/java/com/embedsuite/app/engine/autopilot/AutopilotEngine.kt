package com.embedsuite.app.engine.autopilot

import android.util.Log
import com.embedsuite.app.connection.ConnectionState
import com.embedsuite.app.connection.DeviceConnectionManager
import com.embedsuite.app.connection.FirmwareProfile
import com.embedsuite.app.engine.predictive.ThreatPredictor
import com.embedsuite.app.engine.risk.DeviceRiskInput
import com.embedsuite.app.engine.risk.RiskScorer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed class AutopilotEvent {
    data class Tick(val profile: AutopilotProfile, val action: String) : AutopilotEvent()
    data class RiskDetected(val score: Int, val label: String) : AutopilotEvent()
    data class SoftFailure(val action: String, val message: String) : AutopilotEvent()
    data class Stopped(val profile: AutopilotProfile) : AutopilotEvent()
}

interface AutopilotEngine {
    val profile: AutopilotProfile
    val events: SharedFlow<AutopilotEvent>
    fun start()
    fun stop()
    fun isRunning(): Boolean
}

class NoOpAutopilotEngine(
    override val profile: AutopilotProfile = AutopilotProfile.AUDIT
) : AutopilotEngine {

    private var running = false
    private val _events = MutableSharedFlow<AutopilotEvent>(extraBufferCapacity = 16)
    override val events: SharedFlow<AutopilotEvent> = _events.asSharedFlow()

    override fun start() {
        running = true
    }

    override fun stop() {
        running = false
    }

    override fun isRunning(): Boolean = running
}

class TehLinkAutopilotEngine(
    private val connectionManager: DeviceConnectionManager,
    private val scope: CoroutineScope,
    override val profile: AutopilotProfile,
    private val riskScorer: RiskScorer = RiskScorer,
    private val predictor: ThreatPredictor = ThreatPredictor()
) : AutopilotEngine {

    private val _events = MutableSharedFlow<AutopilotEvent>(extraBufferCapacity = 64)
    override val events: SharedFlow<AutopilotEvent> = _events.asSharedFlow()

    private var loopJob: Job? = null

    override fun start() {
        if (loopJob?.isActive == true) return
        loopJob = scope.launch {
            while (isActive) {
                runCycle()
                delay(intervalMs())
            }
        }
    }

    override fun stop() {
        loopJob?.cancel()
        loopJob = null
        _events.tryEmit(AutopilotEvent.Stopped(profile))
    }

    override fun isRunning(): Boolean = loopJob?.isActive == true

    private suspend fun runCycle() {
        val connected = connectionManager.connectionState.value is ConnectionState.Connected
        if (!connected || connectionManager.detectedProfile.value != FirmwareProfile.BRUCE) {
            delay(intervalMs())
            return
        }

        val action = pickAction()
        _events.tryEmit(AutopilotEvent.Tick(profile, action))

        val result = when (action) {
            "wifi_scan" -> connectionManager.tehLinkRunWifiScan(wifiScanSeconds())
            "get_status" -> runCatching {
                connectionManager.refreshSystemInfo()
            }
            else -> Result.failure(IllegalStateException("acción desconocida"))
        }

        result.onFailure { err ->
            Log.w(TAG, "Autopilot soft failure: $action — ${err.message}")
            _events.tryEmit(AutopilotEvent.SoftFailure(action, err.message ?: "?"))
        }

        if (profile == AutopilotProfile.DEFENSIVE || profile == AutopilotProfile.AUDIT) {
            scoreAndEmit(action)
        }
    }

    private fun scoreAndEmit(action: String) {
        val guess = predictor.guess(action)
        val score = riskScorer.score(
            DeviceRiskInput(
                id = "autopilot-${System.currentTimeMillis()}",
                label = guess.rationale,
                kind = guess.kind
            )
        )
        if (score >= riskThreshold()) {
            _events.tryEmit(AutopilotEvent.RiskDetected(score, guess.countermeasure))
        }
    }

    private fun pickAction(): String = when (profile) {
        AutopilotProfile.AUDIT -> if ((System.currentTimeMillis() / 10_000) % 2 == 0L) "wifi_scan" else "get_status"
        AutopilotProfile.DEFENSIVE -> "get_status"
        AutopilotProfile.STEALTH -> "get_status"
    }

    private fun intervalMs(): Long = when (profile) {
        AutopilotProfile.AUDIT -> 8_000L
        AutopilotProfile.DEFENSIVE -> 15_000L
        AutopilotProfile.STEALTH -> 30_000L
    }

    private fun wifiScanSeconds(): Int = when (profile) {
        AutopilotProfile.AUDIT -> 15
        AutopilotProfile.DEFENSIVE -> 8
        AutopilotProfile.STEALTH -> 5
    }

    private fun riskThreshold(): Int = when (profile) {
        AutopilotProfile.AUDIT -> 40
        AutopilotProfile.DEFENSIVE -> 55
        AutopilotProfile.STEALTH -> 70
    }

    companion object {
        private const val TAG = "TehLinkAutopilot"
    }
}
