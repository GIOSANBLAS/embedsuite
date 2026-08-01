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
}
