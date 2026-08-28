package com.embedsuite.app.scripting

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface ScriptRepository {
    val categories: List<ScriptCategory> get() = ScriptCategory.values().toList()
    fun scripts(): List<Script>
    fun byId(id: String): Script? = scripts().firstOrNull { it.id == id }
    fun byCategory(cat: ScriptCategory): List<Script> = scripts().filter { it.category == cat }
}

/** Solo presets con CLI Bruce documentado — sin plugins TEH-Link. */
class BuiltInScriptRepository : ScriptRepository {

    private val _reload = MutableStateFlow(0)
    val reloadTick: StateFlow<Int> = _reload

    fun invalidate() { _reload.value = _reload.value + 1 }

    override fun scripts(): List<Script> = BruceCliScripts.all
}
