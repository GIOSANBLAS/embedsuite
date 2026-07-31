package com.embedsuite.app.core

import com.embedsuite.app.R

enum class AppLanguage(val tag: String, val labelRes: Int) {
    SYSTEM("system", R.string.lang_system),
    SPANISH("es", R.string.lang_spanish),
    ENGLISH("en", R.string.lang_english),
    PORTUGUESE("pt", R.string.lang_portuguese);

    companion object {
        fun fromTag(tag: String?): AppLanguage =
            entries.firstOrNull { it.tag == tag } ?: SYSTEM
    }
}
