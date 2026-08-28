package com.embedsuite.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IrdbRepositoryFtsTest {

    @Test
    fun buildFtsQuery_singleToken() {
        assertEquals("samsung*", IrdbRepository.buildFtsQuery("Samsung"))
    }

    @Test
    fun buildFtsQuery_multiToken() {
        assertEquals("samsung* aire*", IrdbRepository.buildFtsQuery("Samsung aire"))
    }

    @Test
    fun buildFtsQuery_tooShort() {
        assertNull(IrdbRepository.buildFtsQuery("a"))
    }
}
