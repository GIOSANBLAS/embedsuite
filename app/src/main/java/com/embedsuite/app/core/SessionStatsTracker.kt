package com.embedsuite.app.core

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar

class SessionStatsTracker(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("embed_stats", Context.MODE_PRIVATE)

    init { resetIfNewDay() }

    fun incrementSignals() = increment(KEY_SIGNALS)
    fun incrementAps() = increment(KEY_APS)
    fun incrementMacros() = increment(KEY_MACROS)

    fun signalsToday(): Int = get(KEY_SIGNALS)
    fun apsToday(): Int = get(KEY_APS)
    fun macrosToday(): Int = get(KEY_MACROS)

    private fun increment(key: String) {
        resetIfNewDay()
        prefs.edit().putInt(key, get(key) + 1).apply()
    }

    private fun get(key: String): Int = prefs.getInt(key, 0)

    private fun resetIfNewDay() {
        val today = dayKey()
        if (prefs.getString(KEY_DAY, "") != today) {
            prefs.edit()
                .putString(KEY_DAY, today)
                .putInt(KEY_SIGNALS, 0)
                .putInt(KEY_APS, 0)
                .putInt(KEY_MACROS, 0)
                .apply()
        }
    }

    private fun dayKey(): String {
        val cal = Calendar.getInstance()
        return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
    }

    companion object {
        private const val KEY_DAY = "day"
        private const val KEY_SIGNALS = "signals"
        private const val KEY_APS = "aps"
        private const val KEY_MACROS = "macros"
    }
}
