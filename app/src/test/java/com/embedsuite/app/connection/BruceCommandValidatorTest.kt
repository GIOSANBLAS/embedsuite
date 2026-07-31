package com.embedsuite.app.connection

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BruceCommandValidatorTest {

    @Test
    fun acceptsNormalCommand() {
        assertTrue(BruceCommandValidator.validate("info").isSuccess)
        assertTrue(BruceCommandValidator.validate("subghz rx 433.92").isSuccess)
        assertTrue(BruceCommandValidator.validate("storage list /").isSuccess)
    }

    @Test
    fun rejectsMultilineInjection() {
        assertFalse(BruceCommandValidator.validate("info\nreboot").isSuccess)
    }

    @Test
    fun rejectsBlockedCommands() {
        assertFalse(BruceCommandValidator.validate("reboot").isSuccess)
        assertFalse(BruceCommandValidator.validate("storage rm -rf /").isSuccess)
        assertFalse(BruceCommandValidator.validate("storage rm /foo").isSuccess)
        assertFalse(BruceCommandValidator.validate("rm -rf /").isSuccess)
        assertFalse(BruceCommandValidator.validate("wifi deauth").isSuccess)
        assertFalse(BruceCommandValidator.validate("mkfs").isSuccess)
    }

    @Test
    fun rejectsOversizedPayload() {
        val long = "a".repeat(BruceCommandValidator.MAX_LENGTH + 1)
        assertFalse(BruceCommandValidator.validate(long).isSuccess)
    }
}
