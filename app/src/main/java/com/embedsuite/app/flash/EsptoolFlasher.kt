package com.embedsuite.app.flash

import com.embedsuite.app.UsbSerialManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

class EsptoolFlasher(private val usbSerialManager: UsbSerialManager) {

    companion object {
        private const val CMD_SYNC = 0x08
        private const val CMD_SPI_ATTACH = 0x0D
        private const val CMD_FLASH_BEGIN = 0x02
        private const val CMD_FLASH_DATA = 0x03
        private const val CMD_FLASH_END = 0x04
        private const val CMD_SPI_SET_PARAMS = 0x0B
        private const val BLOCK_SIZE = 0x4000
        private const val FLASH_OFFSET = 0x0
    }

    suspend fun flashFirmware(
        binFile: File,
        onProgress: (Int, String) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!binFile.exists()) {
            return@withContext Result.failure(Exception("Archivo .bin no encontrado."))
        }

        val devices = usbSerialManager.listarDispositivosConectados()
        if (devices.isEmpty()) {
            return@withContext Result.failure(
                Exception("Conecta el T-Embed en modo bootloader (Encoder + RST + USB).")
            )
        }

        try {
            onProgress(5, "Entrando modo bootloader...")
            usbSerialManager.enterBootloader()
            delay(150)
            usbSerialManager.pauseForFlash()
            delay(200)

            if (!usbSerialManager.openRawPort(devices.first(), 115200)) {
                return@withContext Result.failure(Exception("No se pudo abrir puerto raw para flasheo."))
            }

            onProgress(10, "Sincronizando con ESP32-S3...")
            if (!sync()) {
                usbSerialManager.setBaudRate(460800)
                delay(100)
                if (!sync()) {
                    return@withContext Result.failure(
                        Exception("Sync falló. Mantén Encoder+RST al conectar USB.")
                    )
                }
            }

            onProgress(15, "Configurando SPI flash...")
            spiAttach()
            spiSetParams()

            val data = binFile.readBytes()
            val blocks = (data.size + BLOCK_SIZE - 1) / BLOCK_SIZE

            onProgress(20, "Iniciando escritura flash (${data.size / 1024} KB)...")
            flashBegin(data.size, blocks)

            data.toList().chunked(BLOCK_SIZE).forEachIndexed { index, chunk ->
                val block = chunk.toByteArray()
                val padded = if (block.size < BLOCK_SIZE) {
                    block + ByteArray(BLOCK_SIZE - block.size)
                } else block

                flashData(padded, index, blocks)
                val pct = 20 + ((index + 1) * 70 / blocks)
                onProgress(pct, "Escribiendo bloque ${index + 1}/$blocks...")
            }

            onProgress(95, "Finalizando y reiniciando...")
            flashEnd(reboot = true)

            usbSerialManager.closeRawPort()
            onProgress(100, "Flasheo USB completado.")
            Result.success("Firmware escrito en 0x0 (${data.size} bytes)")
        } catch (e: Exception) {
            usbSerialManager.closeRawPort()
            Result.failure(e)
        } finally {
            usbSerialManager.resumeAfterFlash()
        }
    }

    private fun sync(): Boolean {
        val syncData = byteArrayOf(
            0x07, 0x07, 0x12, 0x20,
            0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55,
            0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55,
            0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55,
            0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55
        )

        repeat(10) {
            val response = sendCommand(CMD_SYNC, syncData)
            if (response != null && response.size >= 2 && response[0] == 0x01.toByte()) {
                return true
            }
            Thread.sleep(50)
        }
        return false
    }

    private fun spiAttach() {
        sendCommand(CMD_SPI_ATTACH, byteArrayOf(0x00))
    }

    private fun spiSetParams() {
        val params = byteArrayOf(
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00
        )
        sendCommand(CMD_SPI_SET_PARAMS, params)
    }

    private fun flashBegin(totalSize: Int, blockCount: Int) {
        val size = totalSize
        val data = byteArrayOf(
            (size and 0xFF).toByte(), ((size shr 8) and 0xFF).toByte(),
            ((size shr 16) and 0xFF).toByte(), ((size shr 24) and 0xFF).toByte(),
            (blockCount and 0xFF).toByte(), ((blockCount shr 8) and 0xFF).toByte(),
            ((blockCount shr 16) and 0xFF).toByte(), ((blockCount shr 24) and 0xFF).toByte(),
            (BLOCK_SIZE and 0xFF).toByte(), ((BLOCK_SIZE shr 8) and 0xFF).toByte(),
            ((BLOCK_SIZE shr 16) and 0xFF).toByte(), ((BLOCK_SIZE shr 24) and 0xFF).toByte(),
            (FLASH_OFFSET and 0xFF).toByte(), ((FLASH_OFFSET shr 8) and 0xFF).toByte(),
            ((FLASH_OFFSET shr 16) and 0xFF).toByte(), ((FLASH_OFFSET shr 24) and 0xFF).toByte(),
            0x00
        )
        sendCommand(CMD_FLASH_BEGIN, data)
    }

    private fun flashData(data: ByteArray, seq: Int, totalBlocks: Int) {
        val header = byteArrayOf(
            (data.size and 0xFF).toByte(), ((data.size shr 8) and 0xFF).toByte(),
            ((data.size shr 16) and 0xFF).toByte(), ((data.size shr 24) and 0xFF).toByte(),
            (seq and 0xFF).toByte(), ((seq shr 8) and 0xFF).toByte(),
            ((seq shr 16) and 0xFF).toByte(), ((seq shr 24) and 0xFF).toByte(),
            (totalBlocks and 0xFF).toByte(), ((totalBlocks shr 8) and 0xFF).toByte(),
            ((totalBlocks shr 16) and 0xFF).toByte(), ((totalBlocks shr 24) and 0xFF).toByte(),
            0x00
        )
        val checksum = data.fold(0xEF) { acc, b -> acc xor (b.toInt() and 0xFF) }.toByte()
        sendCommand(CMD_FLASH_DATA, header + data + checksum)
    }

    private fun flashEnd(reboot: Boolean) {
        val data = byteArrayOf(
            if (reboot) 0x00 else 0x01,
            0x00, 0x00, 0x00
        )
        sendCommand(CMD_FLASH_END, data)
    }

    private fun sendCommand(op: Int, data: ByteArray = byteArrayOf()): ByteArray? {
        val packet = SlipEncoder.buildCommand(op, data)
        val encoded = SlipEncoder.encode(packet)
        if (!usbSerialManager.writeRaw(encoded)) return null

        val raw = usbSerialManager.readRaw(timeoutMs = 3000) ?: return null
        val decoded = SlipEncoder.decode(raw)
        return decoded.firstOrNull()
    }
}
