package com.embedsuite.app.services

import com.embedsuite.app.connection.IrSignal
import com.embedsuite.app.connection.XibalbaAdapter

/**
 * IrService — captura y replay IR vía TEH-Link (plugin ir_toolkit).
 */
class IrService(
    private val xibalba: XibalbaAdapter
) {
    suspend fun captureSignal(seconds: Int = 10): Result<IrSignal> {
        return xibalba.irCapture(seconds)
    }

    suspend fun transmitSignal(signal: IrSignal): Result<Unit> {
        return xibalba.irTransmit(signal)
    }

    /** Captura una señal y la retransmite inmediatamente (learn & replay). */
    suspend fun learnAndReplay(seconds: Int = 10): Result<IrSignal> {
        val captured = captureSignal(seconds)
        return captured.fold(
            onSuccess = { signal ->
                if (signal.raw.isBlank() && signal.protocol.isBlank()) {
                    Result.failure(Exception("ir_capture_empty"))
                } else {
                    transmitSignal(signal).map { signal }
                }
            },
            onFailure = { Result.failure(it) }
        )
    }
}
