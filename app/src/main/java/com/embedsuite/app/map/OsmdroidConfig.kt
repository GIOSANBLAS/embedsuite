package com.embedsuite.app.map

import android.content.Context
import org.osmdroid.config.Configuration
import java.io.File

object OsmdroidConfig {

    fun init(context: Context) {
        val appContext = context.applicationContext
        Configuration.getInstance().apply {
            userAgentValue = appContext.packageName
            val base = File(appContext.getExternalFilesDir(null), "osmdroid")
            base.mkdirs()
            osmdroidBasePath = base
            osmdroidTileCache = File(base, "tiles")
        }
    }

    fun cacheSizeBytes(): Long {
        val cacheDir = Configuration.getInstance().osmdroidTileCache
        if (!cacheDir.exists()) return 0L
        return cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    fun formatCacheSize(bytes: Long): String = when {
        bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
        bytes >= 1024 -> "%.0f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }

    fun clearCache(): Boolean {
        val cacheDir = Configuration.getInstance().osmdroidTileCache
        return cacheDir.deleteRecursively()
    }
}
