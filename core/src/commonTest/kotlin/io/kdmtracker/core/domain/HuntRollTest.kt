package io.kdmtracker.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HuntRollTest {
    @Test
    fun theTensDieAndTheUnitsDieMakeTheValue() {
        assertEquals(42, HuntRoll.of(tens = 4, units = 2))
        assertEquals(7, HuntRoll.of(tens = 0, units = 7))
        assertEquals(90, HuntRoll.of(tens = 9, units = 0))
    }

    @Test
    fun bothDiceAtZeroAreOneHundred() {
        assertEquals(100, HuntRoll.of(0, 0))
        assertEquals("00", HuntRoll.label(100))
        assertEquals(0, HuntRoll.tens(100))
        assertEquals(0, HuntRoll.units(100))
    }

    @Test
    fun aStoredRollSplitsBackIntoItsDice() {
        assertEquals(4, HuntRoll.tens(42))
        assertEquals(2, HuntRoll.units(42))
        assertEquals("07", HuntRoll.label(7))
    }

    @Test
    fun aDieOutsideZeroToNineIsRejected() {
        assertFailsWith<IllegalArgumentException> { HuntRoll.of(10, 0) }
        assertFailsWith<IllegalArgumentException> { HuntRoll.of(0, -1) }
    }
}
