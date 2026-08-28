package com.embedsuite.app.rf

object RfFrequencyPresets {
    val PRESETS = listOf("315.00", "433.92", "868.35", "915.00")
    const val DEFAULT = "433.92"

    fun toHz(mhz: String): Long {
        val value = mhz.toDoubleOrNull() ?: DEFAULT.toDouble()
        return (value * 1_000_000).toLong()
    }

    fun label(mhz: String): String = "${mhz.trim()} MHz"
}
