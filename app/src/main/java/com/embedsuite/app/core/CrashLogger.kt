package com.embedsuite.app.core

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashLogger {

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            logCrash(thread.name, throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    fun logCrash(threadName: String, throwable: Throwable) {
        val ctx = appContext ?: return
        runCatching {
            val dir = File(ctx.getExternalFilesDir(null), "crashes").apply { mkdirs() }
            val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            File(dir, "crash_$ts.txt").writeText("Thread: $threadName\n$sw")
            trimOldFiles(dir, maxFiles = 10)
        }
    }

    private fun trimOldFiles(dir: File, maxFiles: Int) {
        dir.listFiles()?.sortedByDescending { it.lastModified() }?.drop(maxFiles)?.forEach { it.delete() }
    }
}
