package com.embedsuite.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.embedsuite.app.connection.ConnectionState
import com.embedsuite.app.connection.DeviceConnectionManager
import com.embedsuite.app.core.AppPreferences
import com.embedsuite.app.scripting.Script
import com.embedsuite.app.scripting.ScriptCategory
import com.embedsuite.app.scripting.ScriptDialect
import com.embedsuite.app.scripting.ScriptRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ScriptExplorerViewModel(
    private val connectionManager: DeviceConnectionManager,
    private val scriptRepository: ScriptRepository,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategory = MutableStateFlow<ScriptCategory?>(null)
    val selectedCategory: StateFlow<ScriptCategory?> = _selectedCategory

    private val _selectedScriptId = MutableStateFlow<String?>(null)
    val selectedScriptId: StateFlow<String?> = _selectedScriptId

    private val _runningScripts = MutableStateFlow<Map<String, ScriptRunState>>(emptyMap())
    val runningScripts: StateFlow<Map<String, ScriptRunState>> = _runningScripts

    val connectionState: StateFlow<ConnectionState> = connectionManager.connectionState
    val auditModeEnabled: StateFlow<Boolean> = appPreferences.auditModeEnabled

    val availableCategories: StateFlow<List<ScriptCategory>> =
        MutableStateFlow(scriptRepository.scripts().map { it.category }.distinct())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scripts: StateFlow<List<Script>> = combine(
        MutableStateFlow(scriptRepository.scripts()),
        selectedCategory,
        searchQuery
    ) { all, cat, q ->
        all
            .let { if (cat != null) it.filter { s -> s.category == cat } else it }
            .let { if (q.isBlank()) it else it.filter { s ->
                s.title.contains(q, true) || s.summary.contains(q, true) || s.id.contains(q, true)
            } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), scriptRepository.scripts())

    val selectedScript: StateFlow<Script?> = selectedScriptId.map { id ->
        id?.let { scriptRepository.byId(it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun search(s: String) { _searchQuery.value = s }
    fun pickCategory(c: ScriptCategory?) { _selectedCategory.value = c; _selectedScriptId.value = null }
    fun pickScript(id: String) { _selectedScriptId.value = id }

    fun runSelected() {
        val s = selectedScript.value ?: return
        run(s)
    }

    fun run(script: Script) {
        if (script.dialect != ScriptDialect.BRUCE_CLI || script.cliCommand.isBlank()) {
            _runningScripts.value = _runningScripts.value.toMutableMap().apply {
                put(script.id, ScriptRunState.Error("Script no compatible con Bruce CLI stock."))
            }
            return
        }
        if (script.requiresAuditUnlock && !appPreferences.auditModeEnabled.value) {
            _runningScripts.value = _runningScripts.value.toMutableMap().apply {
                put(script.id, ScriptRunState.Blocked("Activa Modo Auditoría en Ajustes para reboot."))
            }
            return
        }
        _runningScripts.value = _runningScripts.value.toMutableMap().apply {
            put(script.id, ScriptRunState.Running)
        }
        viewModelScope.launch {
            val r = connectionManager.sendBruceCliLine(script.cliCommand)
            _runningScripts.value = _runningScripts.value.toMutableMap().apply {
                put(
                    script.id,
                    r.fold(
                        onSuccess = { response ->
                            ScriptRunState.Done(
                                summary = response.lineSequence().firstOrNull()?.take(80) ?: "ok",
                                dataJson = response.take(2000)
                            )
                        },
                        onFailure = { t -> ScriptRunState.Error(t.message ?: "error") }
                    )
                )
            }
        }
    }
}

sealed class ScriptRunState {
    object Idle : ScriptRunState()
    object Running : ScriptRunState()
    data class Done(val summary: String, val dataJson: String = "") : ScriptRunState()
    data class Error(val message: String) : ScriptRunState()
    data class Blocked(val message: String) : ScriptRunState()
}
