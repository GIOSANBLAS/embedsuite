package com.embedsuite.app.core.bruce

import org.junit.Assert.assertEquals
import org.junit.Test

class BruceCliTest {

    @Test
    fun mhzToHz_matchesBruceSubghzScanFormat() {
        assertEquals(433_920_000L, BruceCli.mhzToHz(433.92))
        assertEquals(868_000_000L, BruceCli.mhzToHz(868.0))
    }

    @Test
    fun mhzStringToHz_parsesPreset() {
        assertEquals(433_920_000L, BruceCli.mhzStringToHz("433.92"))
    }
}
