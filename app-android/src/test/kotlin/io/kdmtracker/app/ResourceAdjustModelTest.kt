package io.kdmtracker.app

import io.kdmtracker.app.ui.settlement.AdjustResult
import io.kdmtracker.app.ui.settlement.ResourceEdit
import io.kdmtracker.core.domain.ResourceId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The inline resource stepper's rules, pinned before the UI touches them: the count cannot fall
 * below zero, the delta is relative to where editing started, and a save names both counts.
 */
class ResourceAdjustModelTest {
    /** Starts a cell at [original] and moves it by [delta], the way the stepper does. */
    private fun edit(
        original: Int,
        delta: Int,
    ) = ResourceEdit(ResourceId("bone"), "Hueso", original, original).bump(delta)

    @Test
    fun bumpingBelowZeroStaysAtZero() {
        val at = edit(original = 0, delta = 0).bump(-1)

        assertEquals(0, at.pending, "the ledger never holds a negative quantity")
        assertTrue(at.atMinimum, "the minus button is inert at zero")
    }

    @Test
    fun bumpingFromPositiveKeepsTheFloor() {
        val edit = edit(original = 12, delta = -14)

        assertEquals(0, edit.pending)
    }

    @Test
    fun aCellAboveZeroIsNotAtTheMinimum() {
        assertFalse(edit(original = 12, delta = 1).atMinimum)
    }

    @Test
    fun theDeltaIsMeasuredFromWhereEditingBegan() {
        assertEquals(2, edit(original = 12, delta = 2).delta)
        assertEquals(-3, edit(original = 12, delta = -3).delta)
        assertEquals(0, edit(original = 12, delta = 0).delta)
    }

    @Test
    fun confirmingNamesTheLabelAndBothCounts() {
        assertEquals("Hueso: 12 → 14", AdjustResult("Hueso", from = 12, to = 14).summary)
    }
}
