package com.embedsuite.app.core

import android.content.Context

object LocaleManager {

    /** Applies device locale only — manual language override is no longer supported. */
    fun wrap(context: Context): Context = context

    fun resolveLanguage(context: Context): AppLanguage {
        return when (context.resources.configuration.locales.get(0).language) {
            "en" -> AppLanguage.ENGLISH
            "zh" -> AppLanguage.CHINESE
            else -> AppLanguage.SPANISH
        }
    }
}
