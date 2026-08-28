package com.embedsuite.app.core

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleManager {

    fun wrap(context: Context): Context {
        val tag = context.getSharedPreferences(AppPreferences.PREFS_NAME, Context.MODE_PRIVATE)
            .getString(AppPreferences.KEY_APP_LANGUAGE, AppLanguage.SYSTEM.tag)
            ?: AppLanguage.SYSTEM.tag
        return applyLanguage(context, AppLanguage.fromTag(tag))
    }

    fun applyLanguage(context: Context, language: AppLanguage): Context {
        if (language == AppLanguage.SYSTEM) return context
        val locale = when (language) {
            AppLanguage.SPANISH -> Locale("es")
            AppLanguage.ENGLISH -> Locale.ENGLISH
            AppLanguage.CHINESE -> Locale.SIMPLIFIED_CHINESE
            AppLanguage.SYSTEM -> return context
        }
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    fun resolveLanguage(context: Context): AppLanguage {
        val tag = context.getSharedPreferences(AppPreferences.PREFS_NAME, Context.MODE_PRIVATE)
            .getString(AppPreferences.KEY_APP_LANGUAGE, AppLanguage.SYSTEM.tag)
            ?: AppLanguage.SYSTEM.tag
        if (tag != AppLanguage.SYSTEM.tag) return AppLanguage.fromTag(tag)
        return when (context.resources.configuration.locales.get(0).language) {
            "en" -> AppLanguage.ENGLISH
            "zh" -> AppLanguage.CHINESE
            else -> AppLanguage.SPANISH
        }
    }
}
