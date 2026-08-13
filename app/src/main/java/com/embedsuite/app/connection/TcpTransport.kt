package com.embedsuite.app.connection

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicReference

/**
 * TEH-Link v3 transport over raw TCP (NDJSON, one JSON object per line).
 * Default: T-Embed AP @ 192.168.4.1:8888.
 */
class TcpTransport(
    private var host: String = DEFAULT_HOST,
    private var port: Int = DEFAULT_PORT,
    private val connectTimeoutMs: Int = 8_000,
    private val readTimeoutMs: Int = 30_000
) : TEmbedTransport {

    override val type: TransportType = TransportType.WIFI

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val socketRef = AtomicReference<Socket?>(null)
    private val writerRef = AtomicReference<PrintWriter?>(null)
    private val _incoming = MutableSharedFlow<String>(extraBufferCapacity = 128)
    private var readerJob: Job? = null

    override val isConnected: Boolean
        get() = socketRef.get()?.isConnected == true && socketRef.get()?.isClosed == false

    fun updateEndpoint(newHost: String, newPort: Int = port) {
        val cleaned = newHost.trim()
            .removePrefix("http://")
            .removePrefix("https://")
            .trimEnd('/')
        require(WifiTransport.isValidHost(cleaned)) { "Invalid TCP host: $cleaned" }
        require(newPort in 1..65535) { "Invalid port: $newPort" }
        host = cleaned
        port = newPort
    }

    override suspend fun connect(): Result<String> = withContext(Dispatchers.IO) {
        disconnectInternal()
        val candidates = listOf(host to port, DEFAULT_HOST to DEFAULT_PORT).distinct()
        for ((candidateHost, candidatePort) in candidates) {
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(candidateHost, candidatePort), connectTimeoutMs)
                socket.soTimeout = readTimeoutMs
                val writer = PrintWriter(socket.getOutputStream(), true)
                val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))

                socketRef.set(socket)
                writerRef.set(writer)
                host = candidateHost
                port = candidatePort

                readerJob = ioScope.launch { readLoop(reader) }
                _incoming.tryEmit("[TCP] Connected to $candidateHost:$candidatePort")
                return@withContext Result.success("TCP: $candidateHost:$candidatePort")
            } catch (_: Exception) {
                // try next candidate
            }
        }
        Result.failure(Exception("TCP unreachable at $host:$port"))
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        disconnectInternal()
    }

    private fun disconnectInternal() {
        readerJob?.cancel()
        readerJob = null
        runCatching { writerRef.getAndSet(null)?.close() }
        runCatching { socketRef.getAndSet(null)?.close() }
    }

    override suspend fun sendCommand(command: String): Result<String> = withContext(Dispatchers.IO) {
        val writer = writerRef.get()
            ?: return@withContext Result.failure(Exception("TCP not connected."))
        val payload = command.trim()
        if (payload.isBlank()) {
            return@withContext Result.failure(Exception("Empty command."))
        }
        return@withContext try {
            writer.print(payload)
            if (!payload.endsWith("\n")) writer.print("\n")
            writer.flush()
            Result.success("OK")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun incomingLines(): Flow<String> = _incoming.asSharedFlow()

    private suspend fun readLoop(reader: BufferedReader) {
        try {
            while (ioScope.isActive) {
                val line = reader.readLine() ?: break
                if (line.isNotBlank()) {
                    _incoming.emit(line.trim())
                }
            }
        } catch (_: Exception) {
            // socket closed
        } finally {
            disconnectInternal()
        }
    }

    companion object {
        const val DEFAULT_HOST = "192.168.4.1"
        const val DEFAULT_PORT = 8888
    }
}
