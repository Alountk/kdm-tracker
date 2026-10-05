package io.kdmtracker.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HybridLogicalClockTest {
    @Test
    fun firstTickUsesWallClockWithZeroCounter() {
        assertEquals(HybridLogicalClock(1_000L, 0), HybridLogicalClock.tick(previous = null, wallMillis = 1_000L))
    }

    @Test
    fun forwardWallClockResetsCounterToZero() {
        val previous = HybridLogicalClock(1_000L, 5)

        assertEquals(HybridLogicalClock(2_000L, 0), HybridLogicalClock.tick(previous, 2_000L))
    }

    @Test
    fun stalledWallClockIncrementsCounterKeepingMillis() {
        val previous = HybridLogicalClock(1_000L, 5)

        val next = HybridLogicalClock.tick(previous, 1_000L)

        assertEquals(HybridLogicalClock(1_000L, 6), next)
        assertTrue(next > previous)
    }

    @Test
    fun backwardWallClockIncrementsCounterKeepingMillis() {
        val previous = HybridLogicalClock(1_000L, 5)

        val next = HybridLogicalClock.tick(previous, 500L)

        assertEquals(HybridLogicalClock(1_000L, 6), next)
        assertTrue(next > previous)
    }

    @Test
    fun consecutiveTicksAreStrictlyIncreasing() {
        val first = HybridLogicalClock.tick(null, 1_000L)
        val second = HybridLogicalClock.tick(first, 1_000L)
        val third = HybridLogicalClock.tick(second, 1_000L)

        assertEquals(listOf(0, 1, 2), listOf(first, second, third).map { it.counter })
        assertTrue(first < second && second < third)
    }

    @Test
    fun compareToOrdersByMillisThenCounter() {
        assertTrue(HybridLogicalClock(1L, 0) < HybridLogicalClock(2L, 0))
        assertTrue(HybridLogicalClock(2L, 0) < HybridLogicalClock(2L, 1))
        assertEquals(0, HybridLogicalClock(2L, 1).compareTo(HybridLogicalClock(2L, 1)))
    }
}
