package com.embedsuite.app.connection

import org.junit.Assert.*
import org.junit.Test

class FirmwareRepositoryTest {

    @Test
    fun isNewer_detectsNewVersion() {
        assertTrue(FirmwareRepository.isNewer("1.8.0", "1.7.5"))
        assertFalse(FirmwareRepository.isNewer("1.7.0", "1.7.5"))
        assertFalse(FirmwareRepository.isNewer("1.7.5", "1.7.5"))
    }

    @Test
    fun isNewer_sameNumericParts_returnsFalse() {
        assertFalse(FirmwareRepository.isNewer("1.0.0", "1.0.0"))
        assertFalse(FirmwareRepository.isNewer("1.0.0-beta", "1.0.0"))
    }
}
