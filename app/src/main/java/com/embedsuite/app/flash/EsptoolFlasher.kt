package com.embedsuite.app.flash

import com.embedsuite.app.UsbSerialManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Flasher ESP32-S3 estilo Bruce Web Flasher / esptool.py:
 * - merged .bin → write_flash 0x0 (instalación completa)
 * - app .bin    → write_flash 0x10000 + borrar otadata (actualiza ota_0)
 */
class EsptoolFlasher(private val usbSerialManager: UsbSerialManager) {

    companion object {
        private const val CMD_SYNC = 0x08
        private const val CMD_SPI_ATTACH = 0x0D
        private const val CMD_FLASH_BEGIN = 0x02
        private const val CMD_FLASH_DATA = 0x03
        private const val CMD_FLASH_END = 0x04
        private const val CMD_SPI_SET_PARAMS = 0x0B
        private const val CMD_CHANGE_BAUDRATE = 0x0F
        private const val BLOCK_SIZE = 0x4000
        private const val FLASH_SIZE_BYTES = 16 * 1024 * 1024
        private const val SECTOR_SIZE = 4 * 1024
    }

    suspend fun flashFirmware(
        binFile: File,
        onProgress: (Int, String) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!binFile.exists()) {
            return@withContext Result.failure(Exception("Archivo .bin no encontrado."))
        }

        val analysis = FirmwareImageAnalyzer.analyze(binFile)
        analysis.warning?.let { onProgress(3, it) }

        val flashLabel = when (analysis.kind) {
            FirmwareImageAnalyzer.ImageKind.MERGED_FULL ->
                "Merged @ 0x0 (${analysis.sizeBytes / 1024} KB) — instalación completa"
            FirmwareImageAnalyzer.ImageKind.APP_ONLY ->
                "App @ 0x${analysis.flashOffset.toString(16)} (${analysis.appVersion ?: "?"}) — estilo OTA USB"
        }
        onProgress(5, "Plan: $flashLabel")

        delay(300)
        var devices = usbSerialManager.listarDispositivosConectados()
        if (devices.isEmpty()) {
            delay(1000)
            devices = usbSerialManager.listarDispositivosConectados()
        }
        if (devices.isEmpty()) {
            return@withContext Result.failure(
                Exception(
                    "No hay T-Embed USB. Pulsa Encoder+RST (como Bruce flasher) y concede permiso OTG."
                )
            )
        }

        val device = Esp32UsbIds.pickFlashDevice(devices)
            ?: return@withContext Result.failure(Exception("No hay dispositivo USB Espressif."))

        onProgress(
            7,
            "USB VID=0x${device.vendorId.toString(16)} PID=0x${device.productId.toString(16)} " +
                "(bootloader preferido)"
        )

        if (!usbSerialManager.tienePermiso(device)) {
            usbSerialManager.solicitarPermiso(device)
            return@withContext Result.failure(
                Exception("Concede permiso USB y vuelve a pulsar Flash USB.")
            )
        }

        usbSerialManager.pauseForFlash()
        delay(250)

        var activeBaud = 115200
        var syncOk = false
        for (baud in intArrayOf(115200, 460800)) {
            if (!usbSerialManager.openRawPort(device, baud)) continue
            usbSerialManager.enterDownloadMode()
            delay(200)
            usbSerialManager.purgeInput()

            onProgress(10, "Sync ROM @ $baud…")
            if (sync()) {
                syncOk = true
                activeBaud = baud
                onProgress(14, "Bootloader OK @ $baud")
                break
            }
            usbSerialManager.closeRawPort()
            delay(300)
        }

        if (!syncOk) {
            usbSerialManager.resumeAfterFlash()
            return@withContext Result.failure(
                Exception(
                    "Sync ESP32-S3 falló. Mantén Encoder+RST al conectar (modo Bruce) e inténtalo de nuevo."
                )
            )
        }

        if (activeBaud == 115200) {
            changeBaudRate(460800)
            usbSerialManager.setBaudRate(460800)
            activeBaud = 460800
            onProgress(16, "Baud 460800")
        }

        try {
            spiAttach()
            spiSetParams16Mb()

            val imageBytes = binFile.readBytes()
            when (analysis.kind) {
                FirmwareImageAnalyzer.ImageKind.MERGED_FULL -> {
                    onProgress(20, "Borrando y escribiendo flash completa @ 0x0…")
                    writeRegion(
                        offset = FirmwareImageAnalyzer.MERGED_OFFSET,
                        data = imageBytes,
                        onProgress = onProgress,
                        progressStart = 20,
                        progressEnd = 92
                    )
                }
                FirmwareImageAnalyzer.ImageKind.APP_ONLY -> {
                    onProgress(18, "Reset otadata @ 0xE000 + 0xF000…")
                    writeErasedRegion(FirmwareImageAnalyzer.OTADATA_OFFSET, FirmwareImageAnalyzer.OTADATA_SIZE)
                    writeErasedRegion(FirmwareImageAnalyzer.OTADATA_ALT_OFFSET, FirmwareImageAnalyzer.OTADATA_ALT_SIZE)
                    onProgress(22, "Escribiendo app @ 0x${analysis.flashOffset.toString(16)}…")
                    writeRegion(
                        offset = analysis.flashOffset,
                        data = imageBytes,
                        onProgress = onProgress,
                        progressStart = 22,
                        progressEnd = 92
                    )
                }
            }

            onProgress(96, "Reiniciando T-Embed…")
            flashEnd(reboot = true)
            delay(1200)

            usbSerialManager.closeRawPort()
            val ver = analysis.appVersion?.let { " ($it)" }.orEmpty()
            onProgress(100, "Flash OK$ver — espera arranque Xibalba (~15 s).")
            Result.success(
                when (analysis.kind) {
                    FirmwareImageAnalyzer.ImageKind.MERGED_FULL ->
                        "Merged ${imageBytes.size} B @ 0x0$ver"
                    FirmwareImageAnalyzer.ImageKind.APP_ONLY ->
                        "App ${imageBytes.size} B @ 0x${analysis.flashOffset.toString(16)}$ver"
                }
            )
        } catch (e: Exception) {
            try { usbSerialManager.closeRawPort() } catch (_: Exception) {}
            Result.failure(Exception("Flash falló: ${e.message}", e))
        } finally {
            usbSerialManager.resumeAfterFlash()
        }
    }

    private fun writeErasedRegion(offset: Int, size: Int) {
        val blank = ByteArray(size) { 0xFF.toByte() }
        writeRegion(offset, blank, onProgress = { _, _ -> }, progressStart = 0, progressEnd = 0)
    }

    private fun writeRegion(
        offset: Int,
        data: ByteArray,
        onProgress: (Int, String) -> Unit,
        progressStart: Int,
        progressEnd: Int
    ) {
        val blocks = (data.size + BLOCK_SIZE - 1) / BLOCK_SIZE
        val eraseSize = alignUp(data.size, SECTOR_SIZE)
        flashBegin(eraseSize, blocks, offset)

        data.toList().chunked(BLOCK_SIZE).forEachIndexed { index, chunk ->
            val block = chunk.toByteArray()
            val padded = if (block.size < BLOCK_SIZE) {
                block + ByteArray(BLOCK_SIZE - block.size) { 0xFF.toByte() }
            } else block

            flashData(padded, index)
            if (progressEnd > progressStart && blocks > 0) {
                val pct = progressStart + ((index + 1) * (progressEnd - progressStart) / blocks)
                onProgress(pct.coerceAtMost(progressEnd), "Bloque ${index + 1}/$blocks")
            }
            if (index % 4 == 0) Thread.sleep(2)
        }
    }

    private fun alignUp(value: Int, alignment: Int): Int =
        ((value + alignment - 1) / alignment) * alignment

    private fun sync(): Boolean {
        val syncPayload = byteArrayOf(
            0x07, 0x07, 0x12, 0x20,
            0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55,
            0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55,
            0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55,
            0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55
        )
        repeat(10) {
            val response = sendCommand(CMD_SYNC, syncPayload, timeoutMs = 600)
            if (response != null &&
                response[0] == 0x01.toByte() &&
                (response[1].toInt() and 0xFF) == CMD_SYNC
            ) {
                return true
            }
            Thread.sleep(40)
        }
        return false
    }

    private fun changeBaudRate(baud: Int) {
        val params = ByteArray(8)
        writeLe32(params, 0, baud)
        writeLe32(params, 4, 0)
        sendCommand(CMD_CHANGE_BAUDRATE, params, timeoutMs = 1000)
        Thread.sleep(50)
    }

    private fun spiAttach() {
        requireOk(sendCommand(CMD_SPI_ATTACH, byteArrayOf(0x00)), CMD_SPI_ATTACH)
    }

    private fun spiSetParams16Mb() {
        val params = ByteArray(24)
        writeLe32(params, 0, 0)
        writeLe32(params, 4, FLASH_SIZE_BYTES)
        writeLe32(params, 8, 64 * 1024)
        writeLe32(params, 12, SECTOR_SIZE)
        writeLe32(params, 16, 256)
        writeLe32(params, 20, 0xFFFF)
        requireOk(sendCommand(CMD_SPI_SET_PARAMS, params), CMD_SPI_SET_PARAMS)
    }

    private fun writeLe32(buf: ByteArray, offset: Int, value: Int) {
        buf[offset] = (value and 0xFF).toByte()
        buf[offset + 1] = ((value shr 8) and 0xFF).toByte()
        buf[offset + 2] = ((value shr 16) and 0xFF).toByte()
        buf[offset + 3] = ((value shr 24) and 0xFF).toByte()
    }

    private fun flashBegin(eraseSize: Int, blockCount: Int, flashOffset: Int) {
        // ESP32-S3: 5×uint32 (20 B) — incluye encrypted_write=0 (esptool v4+)
        val data = ByteArray(20)
        writeLe32(data, 0, eraseSize)
        writeLe32(data, 4, blockCount)
        writeLe32(data, 8, BLOCK_SIZE)
        writeLe32(data, 12, flashOffset)
        writeLe32(data, 16, 0)
        requireOk(sendCommand(CMD_FLASH_BEGIN, data, timeoutMs = 120_000), CMD_FLASH_BEGIN)
    }

    private fun flashData(data: ByteArray, seq: Int) {
        val header = ByteArray(16)
        writeLe32(header, 0, data.size)
        writeLe32(header, 4, seq)
        writeLe32(header, 8, data.size)
        writeLe32(header, 12, 0)
        val payload = header + data
        requireOk(
            sendCommand(
                CMD_FLASH_DATA,
                payload,
                timeoutMs = 8000,
                dataBlockChecksum = SlipEncoder.checksum(data)
            ),
            CMD_FLASH_DATA
        )
    }

    private fun flashEnd(reboot: Boolean) {
        val data = byteArrayOf(if (reboot) 0x00 else 0x01, 0x00, 0x00, 0x00)
        requireOk(sendCommand(CMD_FLASH_END, data), CMD_FLASH_END)
    }

    private fun requireOk(response: ByteArray?, op: Int) {
        if (response == null || response.size < 8) {
            throw IOException("Timeout esptool op=0x${op.toString(16)}")
        }
        if (response[0] != 0x01.toByte() || (response[1].toInt() and 0xFF) != op) {
            throw IOException("Respuesta inválida op=0x${op.toString(16)}")
        }
        // Status ROM: 2 bytes tras el header de 8 (value @ 4-7 se ignora)
        if (response.size >= 10) {
            val status = response[8].toInt() and 0xFF
            if (status != 0) {
                val reason = response[9].toInt() and 0xFF
                throw IOException("esptool falló status=$status reason=$reason op=0x${op.toString(16)}")
            }
        }
    }

    private fun sendCommand(
        op: Int,
        data: ByteArray = byteArrayOf(),
        timeoutMs: Int = 3000,
        dataBlockChecksum: Int? = null
    ): ByteArray? {
        val packet = SlipEncoder.buildCommand(op, data, dataBlockChecksum)
        if (!usbSerialManager.writeRaw(SlipEncoder.encode(packet))) return null
        return readSlipResponse(timeoutMs, op)
    }

    private fun readSlipResponse(timeoutMs: Int, expectedOp: Int): ByteArray? {
        val accumulated = ArrayList<Byte>(512)
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val chunk = usbSerialManager.readRaw(200) ?: continue
            accumulated.addAll(chunk.toList())
            val raw = accumulated.toByteArray()
            val packets = SlipEncoder.decode(raw)
            for (pkt in packets) {
                if (pkt.size >= 2 &&
                    pkt[0] == 0x01.toByte() &&
                    (pkt[1].toInt() and 0xFF) == expectedOp
                ) {
                    return pkt
                }
            }
            if (accumulated.size > 8192) accumulated.clear()
        }
        return null
    }
}
