package com.embedsuite.app.widget

import android.content.Context
import java.util.UUID

object WidgetStateStore {

    private const val PREFS = "embed_widget_state"
    const val EXTRA_ACTION_TOKEN = "embed_widget_action_token"

    fun updateLastSignal(context: Context, protocol: String, frequency: String) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_PROTOCOL, protocol.ifBlank { "RAW" })
            .putString(KEY_FREQUENCY, frequency.ifBlank { "—" })
            .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
            .apply()
        EmbedWidgetProvider.updateAllWidgets(context)
    }

    fun updateFavoriteLabel(context: Context, label: String?) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_FAV_LABEL, label?.take(28) ?: "")
            .apply()
        EmbedWidgetProvider.updateAllWidgets(context)
    }

    fun lastFrequency(context: Context): String {
        return context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_FREQUENCY, "—") ?: "—"
    }

    fun lastProtocol(context: Context): String {
        return context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_PROTOCOL, "—") ?: "—"
    }

    fun favoriteLabel(context: Context): String {
        return context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_FAV_LABEL, "") ?: ""
    }

    /** Token anti-broadcast: solo PendingIntents del widget pueden TX/RX. */
    fun ensureActionToken(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_ACTION_TOKEN, null)
        if (!existing.isNullOrBlank()) return existing
        val token = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_ACTION_TOKEN, token).apply()
        return token
    }

    fun isValidActionToken(context: Context, token: String?): Boolean {
        if (token.isNullOrBlank()) return false
        val expected = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_ACTION_TOKEN, null)
        return !expected.isNullOrBlank() && expected == token
    }

    private const val KEY_PROTOCOL = "last_protocol"
    private const val KEY_FREQUENCY = "last_frequency"
    private const val KEY_TIMESTAMP = "last_timestamp"
    private const val KEY_FAV_LABEL = "fav_label"
    private const val KEY_ACTION_TOKEN = "action_token"
}
