package com.embedsuite.app.flipper

import android.content.Context
import com.embedsuite.app.data.CapturedSignalEntity
import com.embedsuite.app.data.IrButtonEntity
import com.embedsuite.app.data.NfcDumpEntity
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Export bundle `.zip` estilo Flipper (skill Fase 6). */
object FlipperZipExporter {

    data class ExportBundle(
        val signals: List<CapturedSignalEntity> = emptyList(),
        val irButtons: List<IrButtonEntity> = emptyList(),
        val nfcDumps: List<NfcDumpEntity> = emptyList()
    )

    fun export(context: Context, bundle: ExportBundle, label: String = "embed_export"): File {
        val dir = File(context.cacheDir, "flipper_export").apply { mkdirs() }
        val zipFile = File(dir, "${label}_${System.currentTimeMillis()}.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            bundle.signals.forEachIndexed { i, sig ->
                val name = "subghz/${sanitize(sig.label.ifBlank { "signal_$i" })}.sub"
                addEntry(zos, name, FlipperFileManager.toSubContent(sig))
            }
            bundle.irButtons.forEachIndexed { i, btn ->
                val name = "infrared/${sanitize(btn.buttonName.ifBlank { "ir_$i" })}.ir"
                addEntry(zos, name, FlipperFileManager.toIrContent(btn))
            }
            bundle.nfcDumps.forEachIndexed { i, dump ->
                val name = "nfc/${sanitize(dump.uid.ifBlank { "nfc_$i" })}.nfc"
                addEntry(zos, name, FlipperFileManager.toNfcContent(dump))
            }
            addEntry(zos, "README.txt", "EMBED SUITE export for Flipper Zero / Bruce\n")
        }
        return zipFile
    }

    private fun addEntry(zos: ZipOutputStream, path: String, content: String) {
        zos.putNextEntry(ZipEntry(path))
        zos.write(content.toByteArray(Charsets.UTF_8))
        zos.closeEntry()
    }

    private fun sanitize(raw: String): String =
        raw.replace(Regex("""[^\w.\-]"""), "_").take(48).ifBlank { "item" }
}
