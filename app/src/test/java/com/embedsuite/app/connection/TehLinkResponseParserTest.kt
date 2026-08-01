package com.embedsuite.app.connection

import org.json.JSONObject
import org.junit.Assert.assertEquals
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
}
