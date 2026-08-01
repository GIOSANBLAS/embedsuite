package com.embedsuite.app.connection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TehLinkActionPolicyTest {

    @Test
    fun validate_blocksBadUsbRunScript() {
        val result = TehLinkActionPolicy.validate("badusb", "run_script")
        assertTrue(result.isFailure)
    }

    @Test
    fun validate_blocksWardrivingStartViaGenericRunAction() {
        val result = TehLinkActionPolicy.validate("wardriving", "start")
        assertTrue(result.isFailure)
    }

    @Test
    fun validate_allowsWardrivingGpsUpdate() {
        val result = TehLinkActionPolicy.validate("wardriving", "gps_update")
        assertTrue(result.isSuccess)
    }

    @Test
    fun validate_allowsWardrivingStop() {
        val result = TehLinkActionPolicy.validate("wardriving", "stop")
        assertTrue(result.isSuccess)
    }

    @Test
    fun validate_allowsUnlistedActions() {
        val result = TehLinkActionPolicy.validate("wifi_toolkit", "scan_start")
        assertTrue(result.isSuccess)
        assertEquals(Unit, result.getOrNull())
    }
}
