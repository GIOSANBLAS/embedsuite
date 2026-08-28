package com.embedsuite.app.core.connection

import com.embedsuite.app.connection.TransportType

/** Selección task-aware USB / BLE / WiFi (11 tareas del orquestador). */
enum class TransportTask {
    FILE_UPLOAD,
    FILE_DOWNLOAD,
    CLI_TRIGGER,
    CAPTURE_SUBGHZ,
    REPLAY_SUBGHZ,
    CAPTURE_IR,
    TRANSMIT_IR,
    BADUSB_RUN,
    TELEMETRY,
    DISCOVERY,
    DEBUG_SERIAL,
    /** @deprecated Alias — usar tipos específicos arriba. */
    HEAVY_FILE,
    QUICK_COMMAND,
    SPECTRUM_STREAM
}

object TransportSelector {
    fun preferred(task: TransportTask): TransportType = when (task) {
        TransportTask.FILE_UPLOAD,
        TransportTask.FILE_DOWNLOAD,
        TransportTask.REPLAY_SUBGHZ,
        TransportTask.TRANSMIT_IR,
        TransportTask.HEAVY_FILE,
        TransportTask.SPECTRUM_STREAM -> TransportType.WIFI

        TransportTask.CLI_TRIGGER,
        TransportTask.CAPTURE_SUBGHZ,
        TransportTask.CAPTURE_IR,
        TransportTask.BADUSB_RUN,
        TransportTask.TELEMETRY,
        TransportTask.QUICK_COMMAND -> TransportType.BLE

        TransportTask.DISCOVERY,
        TransportTask.DEBUG_SERIAL -> TransportType.USB
    }

    fun fallback(task: TransportTask, primaryFailed: TransportType): TransportType? = when (task) {
        TransportTask.FILE_UPLOAD,
        TransportTask.FILE_DOWNLOAD,
        TransportTask.REPLAY_SUBGHZ,
        TransportTask.TRANSMIT_IR,
        TransportTask.HEAVY_FILE,
        TransportTask.SPECTRUM_STREAM ->
            if (primaryFailed == TransportType.WIFI) TransportType.USB else null

        TransportTask.CLI_TRIGGER,
        TransportTask.CAPTURE_SUBGHZ,
        TransportTask.CAPTURE_IR,
        TransportTask.BADUSB_RUN,
        TransportTask.TELEMETRY,
        TransportTask.QUICK_COMMAND ->
            if (primaryFailed == TransportType.BLE) TransportType.USB else null

        TransportTask.DISCOVERY,
        TransportTask.DEBUG_SERIAL -> null
    }
}
