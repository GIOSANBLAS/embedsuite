package com.embedsuite.app.core.connection

import kotlin.math.min
import kotlin.math.pow

/**
 * Exponential backoff with jitter cap for transport reconnect attempts.
 */
object ReconnectPolicy {

    const val DEFAULT_BASE_MS = 1_500L
    const val DEFAULT_MAX_MS = 30_000L
    const val DEFAULT_MULTIPLIER = 2.0

    fun delayMs(
        attempt: Int,
        baseMs: Long = DEFAULT_BASE_MS,
        maxMs: Long = DEFAULT_MAX_MS,
        multiplier: Double = DEFAULT_MULTIPLIER
    ): Long {
        if (attempt <= 0) return baseMs
        val exp = baseMs * multiplier.pow(attempt.coerceAtMost(10) - 1)
        return min(exp.toLong(), maxMs)
    }
}
