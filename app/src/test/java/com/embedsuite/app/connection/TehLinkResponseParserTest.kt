package com.embedsuite.app.connection

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TehLinkResponseParserTest {

    @Test
    fun parseDeviceInfo_readsPlugins() {
        val data = JSONObject(
            """
            {
              "product": "T-Embed Xibalba",
              "version": "0.12",
              "codename": "Mimic",
              "channel": "release",
              "proto": "teh-link",
              "proto_ver": 1,
              "plugins": [
                {
                  "id": "badusb",
                  "name": "BadUSB",
                  "version": "1.0.0",
                  "author": "Xibalba"
                }
              ]
            }
            """.trimIndent()
        )

        val info = TehLinkResponseParser.parseDeviceInfo(data)
        assertEquals("Mimic", info.codename)
        assertEquals(1, info.plugins.size)
        assertEquals("badusb", info.plugins.first().id)
    }

    @Test
    fun parseScreenInfo_readsActivePlugin() {
        val data = JSONObject(
            """
            {
              "ui_screen": "BadUSB",
              "active_plugin": "badusb",
              "plugin_id": "badusb"
            }
            """.trimIndent()
        )

        val screen = TehLinkResponseParser.parseScreenInfo(data)
        assertEquals("BadUSB", screen.uiScreen)
        assertEquals("badusb", screen.activePlugin)
        assertEquals("badusb", screen.openedPluginId)
    }

    @Test
    fun isTehLinkLine_detectsResponse() {
        val line = """{"ok":true,"id":1,"data":{"pong":true}}"""
        assertTrue(TehLinkResponseParser.isTehLinkLine(line))
    }

    @Test
    fun validateRawRequest_requiresCmdAndId() {
        val ok = TehLinkResponseParser.validateRawRequest("""{"cmd":"ping","id":7}""")
        assertTrue(ok.isSuccess)
        assertEquals(7, ok.getOrNull())

        assertTrue(TehLinkResponseParser.validateRawRequest("not json").isFailure)
        assertTrue(TehLinkResponseParser.validateRawRequest("""{"id":1}""").isFailure)
        assertTrue(TehLinkResponseParser.validateRawRequest("""{"cmd":"ping"}""").isFailure)
        assertTrue(TehLinkResponseParser.validateRawRequest("""{"cmd":"","id":1}""").isFailure)
    }

    @Test
    fun parseActionList_readsActions() {
        val data = JSONObject(
            """
            {
              "actions": [
                {"plugin_id": "badusb", "action": "run_script", "params": ["path"]},
                {"plugin_id": "badusb", "action": "stop"},
                {"plugin_id": "subghz_analyzer", "action": "capture_start", "params": ["seconds"]}
              ]
            }
            """.trimIndent()
        )

        val actions = TehLinkResponseParser.parseActionList(data)
        assertEquals(3, actions.size)
        assertEquals("badusb", actions[0].pluginId)
        assertEquals("run_script", actions[0].action)
        assertEquals(listOf("path"), actions[0].params)
        assertEquals("subghz_analyzer", actions[2].pluginId)
        assertEquals("capture_start", actions[2].action)
        assertEquals(listOf("seconds"), actions[2].params)
    }

    @Test
    fun parseActionState_readsBadusbStatus() {
        val data = JSONObject(
            """
            {
              "plugin_id": "badusb",
              "action": "status",
              "state": "running",
              "progress": 42,
              "message": "HID SIM: typing",
              "loaded_path": "/sdcard/plugins/badusb/demo.txt",
              "running": true
            }
            """.trimIndent()
        )

        val state = TehLinkResponseParser.parseActionState(data)
        assertEquals("badusb", state.pluginId)
        assertEquals("running", state.state)
        assertEquals(42, state.progress)
        assertTrue(state.running)
        assertEquals("/sdcard/plugins/badusb/demo.txt", state.loadedPath)
    }

    @Test
    fun parseActionState_readsSubghzCapture() {
        val data = JSONObject(
            """
            {
              "plugin_id": "subghz_analyzer",
              "action": "status",
              "state": "capturing",
              "capturing": true,
              "packets": 12,
              "seconds_remaining": 8,
              "message": "RX 15s @ 433.92 MHz"
            }
            """.trimIndent()
        )

        val state = TehLinkResponseParser.parseActionState(data)
        assertEquals("subghz_analyzer", state.pluginId)
        assertTrue(state.capturing)
        assertEquals(12, state.packets)
        assertEquals(8, state.secondsRemaining)
    }

    @Test
    fun parseActionResult_wrapsState() {
        val data = JSONObject(
            """
            {
              "plugin_id": "badusb",
              "action": "run_script",
              "state": "started",
              "progress": 0,
              "running": true,
              "loaded_path": "/sdcard/plugins/badusb/demo.txt"
            }
            """.trimIndent()
        )

        val result = TehLinkResponseParser.parseActionResult(data)
        assertEquals("badusb", result.pluginId)
        assertEquals("run_script", result.action)
        assertEquals("started", result.state.state)
        assertTrue(result.state.running)
    }

    @Test
    fun parseActionState_readsWifiAps() {
        val data = JSONObject(
            """
            {
              "plugin_id": "wifi_toolkit",
              "action": "status",
              "state": "idle",
              "running": false,
              "message": "3 APs",
              "aps": [
                {"ssid": "HomeWiFi_5G", "bssid": "AA:BB:CC:DD:EE:01", "channel": 6, "rssi": -42, "security": "WPA2"},
                {"ssid": "Guest_Open", "bssid": "AA:BB:CC:DD:EE:03", "channel": 1, "rssi": -71, "security": "Open"}
              ]
            }
            """.trimIndent()
        )

        val state = TehLinkResponseParser.parseActionState(data)
        assertEquals("wifi_toolkit", state.pluginId)
        assertEquals(2, state.aps.size)
        assertEquals("HomeWiFi_5G", state.aps[0].ssid)
        assertEquals(-42, state.aps[0].rssi)
        assertEquals("WPA2", state.aps[0].security)
    }

    @Test
    fun parseActionState_readsBleDevices() {
        val data = JSONObject(
            """
            {
              "plugin_id": "ble_toolkit",
              "action": "status",
              "state": "scanning",
              "running": true,
              "seconds_remaining": 5,
              "devices": [
                {"name": "AirTag", "address": "A1:B2:C3:D4:E5:F6", "rssi": -45, "is_tracker": true},
                {"name": "iPhone", "address": "11:22:33:44:55:66", "rssi": -55, "is_tracker": false}
              ]
            }
            """.trimIndent()
        )

        val state = TehLinkResponseParser.parseActionState(data)
        assertEquals("ble_toolkit", state.pluginId)
        assertTrue(state.running)
        assertEquals(5, state.secondsRemaining)
        assertEquals(2, state.devices.size)
        assertEquals("AirTag", state.devices[0].name)
        assertTrue(state.devices[0].isTracker)
    }

    @Test
    fun parseActionState_readsWardrivingStatus() {
        val data = JSONObject(
            """
            {
              "plugin_id": "wardriving",
              "action": "status",
              "state": "recording",
              "running": true,
              "ap_count": 42,
              "csv_path": "/sdcard/wardriving/session.csv",
              "message": "Wardriving… 42 APs"
            }
            """.trimIndent()
        )

        val state = TehLinkResponseParser.parseActionState(data)
        assertEquals("wardriving", state.pluginId)
        assertTrue(state.running)
        assertEquals("recording", state.state)
        val wd = state.wardriving
        assertEquals(true, wd?.running)
        assertEquals(42, wd?.apCount)
        assertEquals("/sdcard/wardriving/session.csv", wd?.csvPath)
    }

    @Test
    fun parseCryptoResult_readsDigestAndResult() {
        val data = JSONObject(
            """
            {
              "plugin_id": "crypto_toolkit",
              "action": "hash",
              "state": "done",
              "digest": "9d9be79f54df5b2b1e48d36c03e6be7d5ea65949015cd9a9e2b43722e3d6bce0",
              "result": "9d9be79f54df5b2b1e48d36c03e6be7d5ea65949015cd9a9e2b43722e3d6bce0",
              "algo": "sha256",
              "message": "Hash OK"
            }
            """.trimIndent()
        )

        val crypto = TehLinkResponseParser.parseCryptoResult(data)
        assertEquals("sha256", crypto.algo)
        assertEquals(
            "9d9be79f54df5b2b1e48d36c03e6be7d5ea65949015cd9a9e2b43722e3d6bce0",
            crypto.digest
        )
        assertEquals(
            "9d9be79f54df5b2b1e48d36c03e6be7d5ea65949015cd9a9e2b43722e3d6bce0",
            crypto.result
        )
    }

    @Test
    fun parseActionState_readsCryptoStatus() {
        val data = JSONObject(
            """
            {
              "plugin_id": "crypto_toolkit",
              "action": "gen_password",
              "state": "done",
              "result": "Kx9!mP2vQw7nRt4L",
              "message": "Password generated"
            }
            """.trimIndent()
        )

        val state = TehLinkResponseParser.parseActionState(data)
        assertEquals("crypto_toolkit", state.pluginId)
        assertEquals("done", state.state)
        val crypto = state.crypto
        assertEquals("Kx9!mP2vQw7nRt4L", crypto?.result)
        assertEquals("", crypto?.digest)
    }

    @Test
    fun parseCryptoResult_fallsBackToLastResult() {
        val data = JSONObject(
            """
            {
              "plugin_id": "crypto_toolkit",
              "action": "status",
              "state": "idle",
              "last_result": "alpha-bravo-cascade-delta"
            }
            """.trimIndent()
        )

        val crypto = TehLinkResponseParser.parseCryptoResult(data)
        assertEquals("alpha-bravo-cascade-delta", crypto.result)
    }

    @Test
    fun redactSensitiveResponse_hidesCryptoResult() {
        val line = """{"ok":true,"id":1,"data":{"result":"secret123","plugin_id":"crypto_toolkit"}}"""
        val redacted = TehLinkResponseParser.redactSensitiveResponse(line)
        assertTrue(redacted.contains("[REDACTED]"))
        assertFalse(redacted.contains("secret123"))
    }

    @Test
    fun validateConsoleRequest_blocksOpenPlugin() {
        val err = TehLinkCommandPolicy.validateConsoleRequest(
            """{"cmd":"open_plugin","id":1,"plugin_id":"badusb"}"""
        )
        assertTrue(err.isFailure)
    }

    @Test
    fun validateConsoleRequest_blocksOta() {
        val err = TehLinkCommandPolicy.validateConsoleRequest("""{"cmd":"ota_begin","id":1,"size":1000}""")
        assertTrue(err.isFailure)
        assertTrue(err.exceptionOrNull()?.message?.contains("no permitido") == true)
    }

    @Test
    fun validateConsoleRequest_allowsPing() {
        val ok = TehLinkCommandPolicy.validateConsoleRequest("""{"cmd":"ping","id":1}""")
        assertTrue(ok.isSuccess)
    }

    @Test
    fun parseDeviceStatus_readsCapabilities() {
        val status = TehLinkResponseParser.parseDeviceStatus(
            JSONObject(
                """
                {
                  "sd_mounted": true,
                  "flash_mounted": true,
                  "ui_screen": "Home",
                  "uptime_ms": 5000,
                  "sim": {"pn532": false},
                  "capabilities": {"nfc": true, "ir": true, "gps_external": false},
                  "battery_pct": 72
                }
                """.trimIndent()
            )
        )
        assertTrue(status.capabilities["nfc"] == true)
        assertTrue(status.capabilities["ir"] == true)
        assertFalse(status.capabilities["gps_external"] == true)
        assertEquals(72, status.batteryPct)
    }

    @Test
    fun parseActionState_readsNfcUid() {
        val state = TehLinkResponseParser.parseActionState(
            JSONObject(
                """
                {
                  "plugin_id": "nfc_toolkit",
                  "action": "read",
                  "ready": true,
                  "uid": "04:A1:B2:C3",
                  "sak": 8,
                  "state": "tag_found"
                }
                """.trimIndent()
            )
        )
        assertEquals("04:A1:B2:C3", state.nfc?.uid)
        assertEquals(8, state.nfc?.sak)
    }

    @Test
    fun validateConsoleRequest_blocksRunAction() {
        val err = TehLinkCommandPolicy.validateConsoleRequest(
            """{"cmd":"run_action","id":2,"plugin_id":"badusb","action":"run_script"}"""
        )
        assertTrue(err.isFailure)
    }

    // ========== XIBALBA v0.17 HARDENING + OTA SHA256 + SOAK ==========

    @Test
    fun parseDeviceInfo_readsHardeningFlags() {
        val info = TehLinkResponseParser.parseDeviceInfo(
            JSONObject(
                """
                {
                  "product": "T-Embed Xibalba",
                  "version": "0.17.1",
                  "codename": "Spark",
                  "channel": "release",
                  "proto": "teh-link",
                  "proto_ver": 3,
                  "hardening": {
                    "twdt_enabled": true,
                    "twdt_timeout_seconds": 30,
                    "bod_enabled": true,
                    "bod_voltage": 3.0,
                    "secure_boot": true,
                    "flash_encryption": true,
                    "nvs_encryption": true,
                    "stack_canaries": true,
                    "heap_poisoning": true
                  },
                  "plugins": []
                }
                """.trimIndent()
            )
        )
        assertTrue(info.hardening.twdtEnabled)
        assertEquals(30, info.hardening.twdtTimeoutSeconds)
        assertTrue(info.hardening.bodEnabled)
        assertEquals(3.0f, info.hardening.bodVoltage ?: 0f, 0.01f)
        assertTrue(info.hardening.secureBoot)
        assertTrue(info.hardening.flashEncryption)
        assertTrue(info.hardening.nvsEncryption)
        assertTrue(info.hardening.stackCanaries)
        assertTrue(info.hardening.heapPoisoning)
    }

    @Test
    fun parseDeviceInfo_readsHardeningFlags_backwardCompatOldKeys() {
        val info = TehLinkResponseParser.parseDeviceInfo(
            JSONObject(
                """
                {
                  "product": "T-Embed Xibalba",
                  "version": "0.16",
                  "hardening": {
                    "twdt_enabled": true,
                    "twdt_timeout_s": 10,
                    "bod_enabled": true,
                    "bod_v_mv": 2800
                  },
                  "plugins": []
                }
                """.trimIndent()
            )
        )
        assertEquals(10, info.hardening.twdtTimeoutSeconds)
        assertEquals(2.8f, info.hardening.bodVoltage ?: 0f, 0.01f)
    }

    @Test
    fun parseHardeningInfo_nullObject_producesAllFalse() {
        val h = TehLinkResponseParser.parseHardeningInfo(null)
        assertFalse(h.twdtEnabled)
        assertEquals(0, h.twdtTimeoutSeconds)
        assertFalse(h.secureBoot)
        assertFalse(h.flashEncryption)
        assertFalse(h.nvsEncryption)
    }

    @Test
    fun parseDeviceStatus_readsHeapCoredumpAndWdtReason() {
        val status = TehLinkResponseParser.parseDeviceStatus(
            JSONObject(
                """
                {
                  "uptime_ms": 3600000,
                  "ui_screen": "Sub-GHz Analyzer",
                  "sd_mounted": true,
                  "heap_free_bytes": 123456,
                  "psram_free_bytes": 8388608,
                  "coredump_present": true,
                  "wdt_panic_reason": "Task watchdog fired (ui_shell)"
                }
                """.trimIndent()
            )
        )
        assertEquals(123456, status.heapFreeBytes)
        assertEquals(8388608, status.psramFreeBytes)
        assertTrue(status.coredumpPresent)
        assertEquals("Task watchdog fired (ui_shell)", status.wdtPanicReason)
    }

    @Test
    fun parseDeviceStatus_wdtPanic_backwardCompatOldKey() {
        val status = TehLinkResponseParser.parseDeviceStatus(
            JSONObject(
                """
                {
                  "uptime_ms": 0,
                  "ui_screen": "Home",
                  "sd_mounted": false,
                  "panic_reason": "TG0WDT_SYS_RESET"
                }
                """.trimIndent()
            )
        )
        assertEquals("TG0WDT_SYS_RESET", status.wdtPanicReason)
    }

    @Test
    fun parseOtaStatus_sha256Verified_producesProgressAndState() {
        val ota = TehLinkResponseParser.parseOtaStatus(
            JSONObject(
                """
                {
                  "state": "verified",
                  "bytes_written": 2876416,
                  "total_size": 2876416,
                  "sha256_verified": true
                }
                """.trimIndent()
            )
        )
        assertEquals("verified", ota.state)
        assertEquals(2876416L, ota.bytesWritten)
        assertEquals(2876416L, ota.totalSize)
        assertTrue(ota.sha256Verified)
        assertEquals(100, ota.progressPct)
        assertTrue(ota.isComplete)
        assertFalse(ota.hasError)
    }

    @Test
    fun parseOtaStatus_inProgress_partialBytes() {
        val ota = TehLinkResponseParser.parseOtaStatus(
            JSONObject(
                """
                {
                  "state": "writing",
                  "bytes_written": 287641,
                  "total_size": 2876416,
                  "sha256_verified": false
                }
                """.trimIndent()
            )
        )
        assertEquals(10, ota.progressPct)
        assertFalse(ota.isComplete)
        assertFalse(ota.sha256Verified)
    }

    @Test
    fun parseOtaStatus_errorState_hasErrorFlag() {
        val ota = TehLinkResponseParser.parseOtaStatus(
            JSONObject(
                """
                {
                  "state": "error",
                  "error": "sha256_mismatch",
                  "bytes_written": 0,
                  "total_size": 0,
                  "sha256_verified": false
                }
                """.trimIndent()
            )
        )
        assertTrue(ota.hasError)
        assertEquals("error", ota.state)
    }

    @Test
    fun parseActionState_containsOtaAndSoakFields() {
        val state = TehLinkResponseParser.parseActionState(
            JSONObject(
                """
                {
                  "plugin_id": "ota_toolkit",
                  "action": "upload",
                  "state": "running",
                  "progress": 55,
                  "running": true,
                  "ota_status": {
                    "state": "writing",
                    "bytes_written": 1572864,
                    "total_size": 2876416,
                    "sha256_verified": false
                  },
                  "soak": {
                    "iterations": 500,
                    "failures": 0,
                    "heap_before": 262144,
                    "heap_after": 258048,
                    "leak_bytes": 4096,
                    "completed": 500
                  }
                }
                """.trimIndent()
            )
        )
        assertEquals("writing", state.ota?.state)
        assertEquals(55, state.ota?.progressPct)
        assertFalse(state.ota?.sha256Verified!!)
        val s = state.soak
        assertNotNull(s)
        assertEquals(500, s!!.iterations)
        assertEquals(4096, s.leakBytes)
        assertFalse(s.isHealthy)   // 4096 B leak ≥ threshold
    }

    @Test
    fun parseSoakResult_healthyWhenLeakUnderThreshold() {
        val soak = TehLinkResponseParser.parseSoakResult(
            JSONObject(
                """
                {
                  "iterations": 1000,
                  "failures": 0,
                  "heap_before": 262144,
                  "heap_after": 261900,
                  "leak_bytes": 244,
                  "completed": 1000
                }
                """.trimIndent()
            )
        )
        assertTrue(soak.isHealthy)
        assertEquals(0, soak.failures)
        assertEquals(244, soak.leakBytes)
    }

    @Test
    fun parseSoakResult_unhealthyWhenFailuresPresent() {
        val soak = TehLinkResponseParser.parseSoakResult(
            JSONObject(
                """
                {
                  "iterations": 500,
                  "failures": 3,
                  "heap_before": 262144,
                  "heap_after": 262144,
                  "leak_bytes": 0,
                  "completed": 497
                }
                """.trimIndent()
            )
        )
        assertFalse(soak.isHealthy)
        assertEquals(3, soak.failures)
    }
}
