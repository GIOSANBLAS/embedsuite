package com.embedsuite.app.connection

import kotlinx.coroutines.flow.Flow

interface TEmbedTransport {
    val type: TransportType
    val isConnected: Boolean

    suspend fun connect(): Result<String>
    suspend fun disconnect()
    suspend fun sendCommand(command: String): Result<String>
    fun incomingLines(): Flow<String>
}
