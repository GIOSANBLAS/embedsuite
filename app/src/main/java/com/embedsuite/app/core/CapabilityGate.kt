package com.embedsuite.app.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

/** Capabilities reportadas por get_info (array de strings del firmware). */
object CapabilityGate {
    private val _capabilities = MutableStateFlow<Set<String>>(emptySet())
    val capabilities: StateFlow<Set<String>> = _capabilities.asStateFlow()

    fun updateFromJsonArray(arr: JSONArray?) {
        if (arr == null) {
            _capabilities.value = emptySet()
            return
        }
        val set = buildSet {
            for (i in 0 until arr.length()) {
                arr.optString(i)?.takeIf { it.isNotBlank() }?.let { add(it.lowercase()) }
            }
        }
        _capabilities.value = set
    }

    fun updateFromList(list: List<String>) {
        _capabilities.value = list.map { it.lowercase() }.toSet()
    }

    fun has(cap: String): Boolean = _capabilities.value.contains(cap.lowercase())

    fun clear() {
        _capabilities.value = emptySet()
    }
}
