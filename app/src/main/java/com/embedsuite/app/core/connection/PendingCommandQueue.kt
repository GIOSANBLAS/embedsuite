package com.embedsuite.app.core.connection

import java.util.ArrayDeque

/**
 * Soft queue for TEH-Link JSON commands while disconnected.
 * Oldest entries drop when [MAX_SIZE] is exceeded.
 */
class PendingCommandQueue(
    private val maxSize: Int = MAX_SIZE
) {
    private val deque = ArrayDeque<String>()

    val size: Int get() = deque.size

    fun enqueue(json: String) {
        if (json.isBlank()) return
        while (deque.size >= maxSize) {
            deque.removeFirst()
        }
        deque.addLast(json.trim())
    }

    fun drain(): List<String> {
        if (deque.isEmpty()) return emptyList()
        val copy = deque.toList()
        deque.clear()
        return copy
    }

    fun peekAll(): List<String> = deque.toList()

    companion object {
        const val MAX_SIZE = 20
    }
}
