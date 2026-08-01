package com.embedsuite.app.connection

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TehLinkResponseParserTest {

    @Test
    fun parseDeviceInfo_readsPlugins() {
        val data = JSONObject()
            .put("product", "T-Embed Xibalba")
            .put("version", "0.12")
            .put("codename", "Mimic")
            .put("channel", "release")
            .put("proto", "teh-link")
            .put("proto_ver", 1)
            .put("plugins", JSONArray().put(
                JSONObject()
                    .put("id", "badusb")
                    .put("name", "BadUSB")
                    .put("version", "1.0.0")
                    .put("author", "Xibalba")
            ))

        val info = TehLinkResponseParser.parseDeviceInfo(data)
        assertEquals("Mimic", info.codename)
        assertEquals(1, info.plugins.size)
        assertEquals("badusb", info.plugins.first().id)
    }

    @Test
    fun isTehLinkLine_detectsResponse() {
        val line = """{"ok":true,"id":1,"data":{"pong":true}}"""
        assertTrue(TehLinkResponseParser.isTehLinkLine(line))
    }
}
