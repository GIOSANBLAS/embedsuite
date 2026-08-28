package com.embedsuite.app.core.bruce

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BruceStorageParserTest {

    @Test
    fun parseListResponse_findsFilesAndDirs() {
        val raw = """
            payload.txt
            [D] badusb
            capture.sub 1024 bytes
        """.trimIndent()
        val entries = BruceStorageParser.parseListResponse(raw)
        assertEquals(3, entries.size)
        assertTrue(entries.any { it.name == "payload.txt" && !it.isDir })
        assertTrue(entries.any { it.name == "badusb" && it.isDir })
    }

    @Test
    fun childPath_joinsCorrectly() {
        assertEquals("/bruce/subghz/foo.sub", BruceStorageParser.childPath("/bruce/subghz", "foo.sub"))
    }
}
