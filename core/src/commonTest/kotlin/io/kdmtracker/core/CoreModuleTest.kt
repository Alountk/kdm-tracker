package io.kdmtracker.core

import kotlin.test.Test
import kotlin.test.assertEquals

class CoreModuleTest {
    @Test
    fun exposesVersion() {
        assertEquals("0.1.0", CoreModule.VERSION)
    }
}
