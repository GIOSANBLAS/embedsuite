package com.embedsuite.app.connection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BruceCommandsTest {

    @Test
    fun subGhzTx_matchesWikiFormat() {
        val cmd = BruceCommands.subGhzTx("445533", "433.92", 174, 10)
        assertEquals("subghz tx 445533 433920000 174 10", cmd)
    }

    @Test
    fun subGhzTx_strips0xAndPadsKey() {
        assertEquals(
            "subghz tx 00AABB 433920000 174 10",
            BruceCommands.subGhzTx("0xAABB", "433.92", 174, 10)
        )
    }

    @Test
    fun irTx_matchesWikiStyle() {
        assertEquals(
            "ir tx NEC FF000000 FF000000",
            BruceCommands.irTx("NEC", "0x00FF", "0x00FF")
        )
    }

    @Test
    fun normalizeIrCommand_legacy() {
        assertEquals(
            "ir tx NEC FF000000 FF000000",
            BruceCommands.normalizeIrCommand("ir tx NEC 0x00FF 0x00FF")
        )
    }

    @Test
    fun storageWrite_andPushPath() {
        assertEquals("storage write BruceRF/embed_1.sub 256", BruceCommands.storageWrite("BruceRF/embed_1.sub", 100))
        assertEquals("storage write BruceRF/embed_1.sub 1024", BruceCommands.storageWrite("BruceRF/embed_1.sub", 1024))
        assertEquals("BruceRF/embed_42.sub", BruceCommands.embedPushSubPath(42))
        assertEquals("storage mkdir BruceRF", BruceCommands.storageMkdir("BruceRF"))
    }

    @Test
    fun storageCommands() {
        assertEquals("storage list /", BruceCommands.storageList())
        assertEquals("storage read BruceRF/a.sub", BruceCommands.storageRead("BruceRF/a.sub"))
    }

    @Test
    fun txFromFile_sanitizes() {
        assertEquals(
            "subghz tx_from_file BruceRF/gate.sub",
            BruceCommands.subGhzTxFromFile("/BruceRF/gate.sub")
        )
    }

    @Test
    fun safeChips_areDocumented() {
        assertTrue(BruceCommands.safeConsoleChips.none { it.startsWith("nfc") })
        assertTrue(BruceCommands.safeConsoleChips.none { it.contains("ble scan") })
        assertTrue(BruceCommands.safeConsoleChips.none { it.contains("rx 0") })
    }

    @Test
    fun preparePushContent_rejectsEofLineAndOversize() {
        val ok = BruceCommands.preparePushContent("Filetype: Flipper\nRAW_Data: 1 -1\n")
        assertTrue(ok.isSuccess)

        val eof = BruceCommands.preparePushContent("a\nEOF\nb\n")
        assertTrue(eof.isFailure)

        val huge = "x".repeat(BruceCommands.MAX_PUSH_BYTES + 1)
        assertTrue(BruceCommands.preparePushContent(huge).isFailure)
    }

    @Test
    fun sanitizePath_rejectsTraversal() {
        try {
            BruceCommands.sanitizeDeviceRelativePath("../etc/passwd")
            assertTrue(false)
        } catch (_: IllegalArgumentException) {
            assertTrue(true)
        }
    }
}
