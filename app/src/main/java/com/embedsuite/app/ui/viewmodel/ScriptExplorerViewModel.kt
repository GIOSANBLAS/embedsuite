package com.embedsuite.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.embedsuite.app.connection.ConnectionState
import com.embedsuite.app.connection.DeviceConnectionManager
import com.embedsuite.app.connection.FirmwareProfile
import com.embedsuite.app.connection.TehLinkActionState
import com.embedsuite.app.core.AppPreferences
import com.embedsuite.app.scripting.Script
import com.embedsuite.app.scripting.ScriptCategory
import com.embedsuite.app.scripting.ScriptRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalCoroutinesApi::class)
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

    private val _paramOverrides = MutableStateFlow<Map<String, String>>(emptyMap())
    val paramOverrides: StateFlow<Map<String, String>> = _paramOverrides

    private val _runningScripts = MutableStateFlow<Map<String, ScriptRunState>>(emptyMap())
    val runningScripts: StateFlow<Map<String, ScriptRunState>> = _runningScripts

    val connectionState: StateFlow<ConnectionState> = connectionManager.connectionState
    val auditModeEnabled: StateFlow<Boolean> = appPreferences.auditModeEnabled
    val detectedProfile: StateFlow<FirmwareProfile?> = connectionManager.detectedProfile

    val availableCategories: StateFlow<List<ScriptCategory>> = combine(
        scriptRepository.scripts().let { MutableStateFlow(it.groupBy { s -> s.category }.keys.toList()) },
        detectedProfile
    ) { cats, profile ->
        if (profile != FirmwareProfile.XIBALBA) {
            cats.filterNot { c -> c == ScriptCategory.EVIL_PORTAL || c == ScriptCategory.BEACON_SPAM }
        } else cats
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scripts: StateFlow<List<Script>> = combine(
        MutableStateFlow(scriptRepository.scripts()),
        selectedCategory,
        searchQuery,
        detectedProfile
    ) { all, cat, q, profile ->
        all
            .let { list ->
                if (profile != FirmwareProfile.XIBALBA) {
                    list.filterNot { it.category == ScriptCategory.EVIL_PORTAL || it.category == ScriptCategory.BEACON_SPAM }
                } else list
            }
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
    fun pickScript(id: String) { _selectedScriptId.value = id; _paramOverrides.value = emptyMap() }
    fun overrideParam(key: String, value: String) {
        _paramOverrides.value = _paramOverrides.value.toMutableMap().apply { put(key, value) }
    }

    fun runSelected() {
        val s = selectedScript.value ?: return
        run(s)
    }

    private fun coerceParamValue(key: String, raw: String, script: Script): Any? {
        val def = script.defaultParams[key]
        val overridden = if (raw.isBlank()) return def else raw
        return when (def) {
            is Int -> overridden.toIntOrNull() ?: def
            is Long -> overridden.toLongOrNull() ?: def
            is Double -> overridden.toDoubleOrNull() ?: def
            is Boolean -> overridden.toBooleanStrictOrNull() ?: def
            is List<*> -> overridden.split(",").map { it.trim() }.filter { it.isNotBlank() }
            else -> overridden
        }
    }

    fun run(script: Script) {
        if (script.pluginId.isBlank() || script.action.isBlank()) {
            _runningScripts.value = _runningScripts.value.toMutableMap().apply {
                put(script.id, ScriptRunState.Error("Macro secuencia/legacy; lanzar desde Map Tools > Macro runner"))
            }
            return
        }
        if (script.requiresAuditUnlock && appPreferences.auditModeEnabled.value) {
            _runningScripts.value = _runningScripts.value.toMutableMap().apply {
                put(script.id, ScriptRunState.Blocked("Modo Auditoría bloquea ${script.category.label}"))
            }
            return
        }
        val overrides = script.parameters.associate { p ->
            val raw = _paramOverrides.value[p.key].orEmpty()
            p.key to (coerceParamValue(p.key, raw, script) ?: return@associate p.key to (script.defaultParams[p.key] ?: ""))
        }
        val params = script.buildParams(overrides)
        _runningScripts.value = _runningScripts.value.toMutableMap().apply {
            put(script.id, ScriptRunState.Running)
        }
        viewModelScope.launch {
            val r = connectionManager.tehLinkRunAction(script.pluginId, script.action, params)
            _runningScripts.value = _runningScripts.value.toMutableMap().apply {
                put(script.id, r.fold(
                    onSuccess = { res ->
                        val s: TehLinkActionState = res.state
                        val dataJson = org.json.JSONObject().apply {
                            put("state", s.state)
                            put("progress", s.progress)
                            put("message", s.message)
                            put("running", s.running)
                            if (s.loadedPath.isNotBlank()) put("loaded_path", s.loadedPath)
                            if (s.packets > 0) put("packets", s.packets)
                            if (s.aps.isNotEmpty()) put("aps", s.aps.size)
                        }.toString(2)
                        ScriptRunState.Done(
                            summary = s.state.ifBlank { s.message.ifBlank { "ok" } },
                            dataJson = dataJson
                        )
                    },
                    onFailure = { t -> ScriptRunState.Error(t.message ?: "error") }
                ))
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
