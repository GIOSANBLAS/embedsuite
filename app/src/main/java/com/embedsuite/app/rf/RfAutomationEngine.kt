package com.embedsuite.app.rf

import android.content.Context
import com.embedsuite.app.connection.BruceEvent
import com.embedsuite.app.connection.DeviceConnectionManager
import com.embedsuite.app.connection.SignalEntry
import com.embedsuite.app.core.SoundFeedback
import com.embedsuite.app.data.MacroRepository
import com.embedsuite.app.data.RfAutomationRepository
import com.embedsuite.app.data.RfAutomationRuleEntity
import com.embedsuite.app.data.SignalRepository
import com.embedsuite.app.macro.MacroEngine
import com.embedsuite.app.notifications.EmbedNotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class RfAutomationEngine(
    private val context: Context,
    private val repository: RfAutomationRepository,
    private val signalRepository: SignalRepository,
    private val connectionManager: DeviceConnectionManager,
    private val macroRepository: MacroRepository,
    private val macroEngine: MacroEngine
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun start() {
        connectionManager.events
            .onEach { event ->
                if (event is BruceEvent.SubGhzSignalSaved) {
                    scope.launch { onSignalCaptured(event.entry, event.signalId) }
                }
            }
            .launchIn(scope)
    }

    private suspend fun onSignalCaptured(entry: SignalEntry, signalId: Long) {
        val rules = repository.getEnabled()
        if (rules.isEmpty()) return

        for (rule in rules) {
            if (!matches(rule, entry)) continue
            when (rule.actionType.uppercase()) {
                ACTION_NOTIFY -> EmbedNotificationHelper.notifyRfMatch(
                    context,
                    rule.name,
                    "${entry.protocol} @ ${entry.frequency}",
                    signalId
                )
                ACTION_TAG -> {
                    applyTag(signalId, rule.actionPayload)
                    EmbedNotificationHelper.notifyRfMatch(
                        context,
                        rule.name,
                        "Etiqueta aplicada: ${rule.actionPayload}",
                        signalId
                    )
                }
                ACTION_ALERT -> {
                    SoundFeedback.playCapture()
                    EmbedNotificationHelper.notifyRfMatch(
                        context,
                        "⚠ ${rule.name}",
                        "${entry.protocol} detectado",
                        signalId
                    )
                }
                ACTION_MACRO -> runMacro(rule)
            }
        }
    }

    private suspend fun runMacro(rule: RfAutomationRuleEntity) {
        val macroName = rule.actionPayload.trim()
        if (macroName.isBlank()) return
        val macro = macroRepository.getAll().firstOrNull {
            it.name.equals(macroName, ignoreCase = true)
        } ?: return
        macroEngine.execute(macro).fold(
            onSuccess = {
                EmbedNotificationHelper.notifyRfMatch(
                    context,
                    "Macro: ${macro.name}",
                    "$it comandos ejecutados",
                    null
                )
            },
            onFailure = {
                EmbedNotificationHelper.notifyRfMatch(
                    context,
                    "Macro falló",
                    it.message ?: "Error",
                    null
                )
            }
        )
    }

    private suspend fun applyTag(signalId: Long, tag: String) {
        if (tag.isBlank()) return
        val target = signalRepository.getById(signalId) ?: return
        val existingTags = target.tags.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableSet()
        if (existingTags.add(tag.trim())) {
            signalRepository.update(target.copy(tags = existingTags.joinToString(",")))
        }
    }

    private fun matches(rule: RfAutomationRuleEntity, entry: SignalEntry): Boolean {
        if (rule.matchProtocol.isNotBlank() &&
            !entry.protocol.contains(rule.matchProtocol, ignoreCase = true)
        ) return false
        if (rule.matchFrequency.isNotBlank() &&
            !entry.frequency.contains(rule.matchFrequency, ignoreCase = true)
        ) return false
        return true
    }

    companion object {
        const val ACTION_NOTIFY = "NOTIFY"
        const val ACTION_TAG = "TAG"
        const val ACTION_ALERT = "ALERT"
        const val ACTION_MACRO = "MACRO"
    }
}
