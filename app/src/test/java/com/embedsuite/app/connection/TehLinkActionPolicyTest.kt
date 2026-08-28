package com.embedsuite.app.connection

import com.embedsuite.app.core.bruce.BruceLimits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TehLinkActionPolicyTest {

    @Test
    fun validate_blocksEvilPortal() {
        assertTrue(TehLinkActionPolicy.validate("evil_portal", "start").isFailure)
    }

    @Test
    fun validate_blocksBeaconSpam() {
        assertTrue(TehLinkActionPolicy.validate("beacon_spam", "start").isFailure)
    }

    @Test
    fun validate_blocksWardriving() {
        assertTrue(TehLinkActionPolicy.validate("wardriving", "start").isFailure)
    }

    @Test
    fun validate_blocksWifiToolkit() {
        val result = TehLinkActionPolicy.validate("wifi_toolkit", "scan_start")
        assertTrue(result.isFailure)
        assertEquals(BruceLimits.NO_CLI, result.exceptionOrNull()?.message)
    }

    @Test
    fun validate_allowsIrSend() {
        assertTrue(TehLinkActionPolicy.validate("ir_toolkit", "send").isSuccess)
    }
}
