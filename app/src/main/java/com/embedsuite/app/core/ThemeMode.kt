package com.embedsuite.app.core

import com.embedsuite.app.R

/** Visual theme: dark hacker (Obscuro) or light terminal (Diurnal). */
enum class ThemeMode(val prefValue: String, val labelRes: Int) {
    OBSCURO("obscuro", R.string.theme_mode_obscuro),
    DIURNAL("diurnal", R.string.theme_mode_diurnal);

    val isDark: Boolean get() = this == OBSCURO

    companion object {
        fun fromPref(value: String?): ThemeMode =
            entries.firstOrNull { it.prefValue == value } ?: OBSCURO
    }
}
