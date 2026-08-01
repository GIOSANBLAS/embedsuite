package com.embedsuite.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupManager(
    private val context: Context,
    private val database: EmbedDatabase
) {
    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    suspend fun exportFullBackup(): Result<File> = runCatching {
        val signals = database.capturedSignalDao().getRecent(10_000)
        val irButtons = database.irButtonDao().getAll()
        val macros = database.macroDao().getAll()
        val profiles = database.profileDao().getAll()
        val txHistory = database.txHistoryDao().getAll()
        val nfcDumps = database.nfcDumpDao().getAll()
        val bleProfiles = database.bleProfileDao().getAll()
        val rfRules = database.rfAutomationDao().getAll()

        val root = JSONObject().apply {
            put("version", BACKUP_VERSION)
            put("exportedAt", System.currentTimeMillis())
            put("appVersion", com.embedsuite.app.core.AppVersion.NAME)
            put("signals", signals.toJsonArray { s ->
                JSONObject().apply {
                    put("type", s.signalType); put("name", s.name); put("label", s.label)
                    put("tags", s.tags); put("protocol", s.protocol); put("frequency", s.frequency)
                    put("deviceId", s.deviceId); put("macAddress", s.macAddress); put("rssi", s.rssi)
                    put("latitude", s.latitude); put("longitude", s.longitude)
                    put("rawData", s.rawData); put("detail", s.detail); put("decodedFields", s.decodedFields)
                    put("favorite", s.favorite)
                    put("timestamp", s.timestamp)
                }
            })
            put("irButtons", irButtons.toJsonArray { b ->
                JSONObject().apply {
                    put("panelName", b.panelName); put("buttonName", b.buttonName)
                    put("protocol", b.protocol); put("hexCode", b.hexCode); put("bruceCommand", b.bruceCommand)
                }
            })
            put("macros", macros.toJsonArray { m ->
                JSONObject().apply {
                    put("name", m.name); put("commands", m.commands); put("description", m.description)
                }
            })
            put("profiles", profiles.toJsonArray { p ->
                JSONObject().apply {
                    put("name", p.name); put("category", p.category); put("commands", p.commands)
                    put("description", p.description); put("icon", p.icon)
                }
            })
            put("txHistory", txHistory.toJsonArray { t ->
                JSONObject().apply {
                    put("signalId", t.signalId); put("label", t.label); put("protocol", t.protocol)
                    put("command", t.command); put("success", t.success); put("timestamp", t.timestamp)
                }
            })
            put("nfcDumps", nfcDumps.toJsonArray { d ->
                JSONObject().apply {
                    put("uid", d.uid); put("tagType", d.tagType); put("rawDump", d.rawDump)
                    put("parsedSectors", d.parsedSectors); put("timestamp", d.timestamp)
                }
            })
            put("bleProfiles", bleProfiles.toJsonArray { b ->
                JSONObject().apply {
                    put("name", b.name); put("address", b.address); put("services", b.services)
                    put("manufacturerData", b.manufacturerData); put("notes", b.notes); put("timestamp", b.timestamp)
                }
            })
            put("rfAutomationRules", rfRules.toJsonArray { r ->
                JSONObject().apply {
                    put("name", r.name); put("enabled", r.enabled)
                    put("matchProtocol", r.matchProtocol); put("matchFrequency", r.matchFrequency)
                    put("actionType", r.actionType); put("actionPayload", r.actionPayload)
                    put("createdAt", r.createdAt)
                }
            })
        }
        val file = File(context.getExternalFilesDir(null), "embed_backup_${dateFormat.format(Date())}.json")
        file.writeText(root.toString(2))
        file
    }

    suspend fun importFullBackup(jsonContent: String): Result<BackupImportResult> = runCatching {
        val root = JSONObject(jsonContent)
        val version = root.optInt("version", 1)
        if (version > BACKUP_VERSION) {
            throw IllegalArgumentException(
                "Backup versión $version no soportada (máx $BACKUP_VERSION)"
            )
        }
        var signals = 0
        var ir = 0
        var macros = 0
        var profiles = 0
        var nfc = 0
        var ble = 0
        var rfRules = 0

        root.optJSONArray("signals")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                database.capturedSignalDao().insert(
                    CapturedSignalEntity(
                        signalType = o.optString("type", "RF"),
                        name = o.optString("name", o.optString("protocol", "")),
                        label = o.optString("label", ""),
                        tags = o.optString("tags", ""),
                        protocol = o.optString("protocol", ""),
                        frequency = o.optString("frequency", ""),
                        deviceId = o.optString("deviceId", ""),
                        macAddress = o.optString("macAddress", ""),
                        rssi = o.optInt("rssi", 0),
                        latitude = o.optDouble("latitude").takeIf { !it.isNaN() },
                        longitude = o.optDouble("longitude").takeIf { !it.isNaN() },
                        rawData = o.optString("rawData", ""),
                        detail = o.optString("detail", ""),
                        decodedFields = o.optString("decodedFields", ""),
                        favorite = o.optBoolean("favorite", false),
                        timestamp = o.optLong("timestamp", System.currentTimeMillis())
                    )
                )
                signals++
            }
        }

        // v1 backups only had signals
        if (version >= BACKUP_VERSION) {
            root.optJSONArray("irButtons")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    database.irButtonDao().insert(
                        IrButtonEntity(
                            panelName = o.optString("panelName", "Default"),
                            buttonName = o.optString("buttonName", "Imported"),
                            protocol = o.optString("protocol", "NEC"),
                            hexCode = o.optString("hexCode", ""),
                            bruceCommand = o.optString("bruceCommand", "")
                        )
                    )
                    ir++
                }
            }
            root.optJSONArray("macros")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val commands = o.optString("commands")
                    commands.lines()
                        .map { it.trim() }
                        .filter { it.isNotBlank() && !it.startsWith("#") }
                        .forEach { line ->
                            if (!line.startsWith("wait ", ignoreCase = true)) {
                                com.embedsuite.app.connection.BruceCommandValidator.validate(line).getOrThrow()
                            }
                        }
                    database.macroDao().insert(MacroEntity(name = o.optString("name"), commands = commands, description = o.optString("description", "")))
                    macros++
                }
            }
            root.optJSONArray("profiles")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    database.profileDao().insert(
                        ProfileEntity(name = o.optString("name"), category = o.optString("category"), commands = o.optString("commands"), description = o.optString("description", ""), icon = o.optString("icon", ""))
                    )
                    profiles++
                }
            }
            root.optJSONArray("nfcDumps")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    database.nfcDumpDao().insert(
                        NfcDumpEntity(uid = o.optString("uid"), tagType = o.optString("tagType", ""), rawDump = o.optString("rawDump", ""), parsedSectors = o.optString("parsedSectors", ""), timestamp = o.optLong("timestamp", System.currentTimeMillis()))
                    )
                    nfc++
                }
            }
            root.optJSONArray("bleProfiles")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    database.bleProfileDao().insert(
                        BleProfileEntity(name = o.optString("name"), address = o.optString("address"), services = o.optString("services", ""), manufacturerData = o.optString("manufacturerData", ""), notes = o.optString("notes", ""), timestamp = o.optLong("timestamp", System.currentTimeMillis()))
                    )
                    ble++
                }
            }
            root.optJSONArray("rfAutomationRules")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    database.rfAutomationDao().insert(
                        RfAutomationRuleEntity(
                            name = o.optString("name", "Imported"),
                            enabled = o.optBoolean("enabled", true),
                            matchProtocol = o.optString("matchProtocol", ""),
                            matchFrequency = o.optString("matchFrequency", ""),
                            actionType = o.optString("actionType", "NOTIFY"),
                            actionPayload = o.optString("actionPayload", ""),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                    rfRules++
                }
            }
        }

        BackupImportResult(signals, ir, macros, profiles, nfc, ble, rfRules)
    }

    data class BackupImportResult(
        val signals: Int,
        val irButtons: Int,
        val macros: Int,
        val profiles: Int,
        val nfcDumps: Int,
        val bleProfiles: Int,
        val rfAutomationRules: Int = 0
    ) {
        val total: Int get() = signals + irButtons + macros + profiles + nfcDumps + bleProfiles + rfAutomationRules
    }

    companion object {
        const val BACKUP_VERSION = 2
    }

    private inline fun <T> List<T>.toJsonArray(mapper: (T) -> JSONObject): JSONArray {
        return JSONArray().also { arr -> forEach { arr.put(mapper(it)) } }
    }
}
