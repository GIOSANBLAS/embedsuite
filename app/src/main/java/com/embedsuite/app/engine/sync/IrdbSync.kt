package com.embedsuite.app.engine.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** Sincroniza IRDB Flipper desde GitHub (skill Módulo C). */
class IrdbSync(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        const val IRDB_TREE_URL =
            "https://api.github.com/repos/Flipper-XFW/Flipper-IRDB/git/trees/main?recursive=1"
        const val IRDB_RAW_BASE =
            "https://raw.githubusercontent.com/Flipper-XFW/Flipper-IRDB/main/"
    }

    data class IrdbEntry(val path: String, val name: String, val category: String)

    suspend fun listRemoteIrFiles(): Result<List<IrdbEntry>> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder().url(IRDB_TREE_URL).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("GitHub IRDB: HTTP ${resp.code}")
                val body = resp.body?.string().orEmpty()
                val json = org.json.JSONObject(body)
                val tree = json.optJSONArray("tree") ?: org.json.JSONArray()
                buildList {
                    for (i in 0 until tree.length()) {
                        val item = tree.getJSONObject(i)
                        val path = item.optString("path", "")
                        if (path.endsWith(".ir", ignoreCase = true)) {
                            val parts = path.split('/')
                            add(
                                IrdbEntry(
                                    path = path,
                                    name = parts.lastOrNull()?.removeSuffix(".ir") ?: path,
                                    category = parts.dropLast(1).joinToString("/")
                                )
                            )
                        }
                    }
                }.sortedBy { it.path }
            }
        }
    }

    suspend fun downloadIrFile(relativePath: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val url = IRDB_RAW_BASE + relativePath.trimStart('/')
            val req = Request.Builder().url(url).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("Download failed: ${resp.code}")
                resp.body?.string() ?: error("Empty IR file")
            }
        }
    }
}

object IrdbParser {
    data class IrButton(val name: String, val protocol: String, val data: String)

    fun parse(content: String): List<IrButton> {
        val buttons = mutableListOf<IrButton>()
        var currentName = "Button"
        var protocol = "RAW"
        var data = ""
        content.lineSequence().forEach { line ->
            val t = line.trim()
            when {
                t.startsWith("name:") -> currentName = t.substringAfter(":").trim()
                t.startsWith("type:") -> protocol = t.substringAfter(":").trim()
                t.startsWith("protocol:") -> protocol = t.substringAfter(":").trim()
                t.startsWith("data:") -> data = t.substringAfter(":").trim()
                t.isEmpty() && data.isNotBlank() -> {
                    buttons += IrButton(currentName, protocol, data)
                    data = ""
                }
            }
        }
        if (data.isNotBlank()) buttons += IrButton(currentName, protocol, data)
        return buttons
    }
}
