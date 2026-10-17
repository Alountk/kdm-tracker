package io.kdmtracker.app

import io.kdmtracker.app.ui.advance
import io.kdmtracker.app.ui.clampIndex
import io.kdmtracker.app.ui.genderLabel
import io.kdmtracker.app.ui.isEmptyRoster
import io.kdmtracker.core.domain.Sex
import io.kdmtracker.core.domain.Survivor
import io.kdmtracker.core.domain.Ulid
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The survivor pager's rules, pinned before the UI touches them: the design's single prev button
 * cycles with modulo arithmetic, the gender chip reads from [Sex], an empty roster is its own
 * state, and a shrinking roster cannot strand the index out of bounds.
 */
class SurvivorPagerModelTest {
    private fun survivor(sex: Sex = Sex.MALE): Survivor = Survivor(id = Ulid.generate(1L) { 0 }, sex = sex)

    @Test
    fun advanceCyclesFromLastToFirst() {
        assertEquals(0, advance(index = 2, size = 3), "the last survivor wraps to the first")
    }

    @Test
    fun advanceMovesOneStepForward() {
        assertEquals(1, advance(index = 0, size = 3))
        assertEquals(2, advance(index = 1, size = 3))
    }

    @Test
    fun advanceOnAnEmptyRosterStaysAtZero() {
        assertEquals(0, advance(index = 0, size = 0), "no divide-by-zero on an empty roster")
    }

    @Test
    fun advanceOnASingleSurvivorStaysThere() {
        assertEquals(0, advance(index = 0, size = 1))
    }

    @Test
    fun genderChipLabels() {
        assertEquals("H", genderLabel(Sex.MALE), "the design's H chip")
        assertEquals("M", genderLabel(Sex.FEMALE), "the design's M chip")
        assertEquals("O", genderLabel(Sex.OTHER), "a third value the design does not show")
    }

    @Test
    fun emptyRosterPredicate() {
        assertTrue(isEmptyRoster(emptyList<Survivor>()))
        assertFalse(isEmptyRoster(listOf(survivor())))
    }

    @Test
    fun clampKeepsAShrinkingRosterInBounds() {
        // Index 4 of a 5-survivor roster, then two die: 4 must clamp to the last valid slot.
        assertEquals(2, clampIndex(index = 4, size = 3))
        assertEquals(0, clampIndex(index = 4, size = 0), "an emptied roster resets to 0")
        assertEquals(2, clampIndex(index = 2, size = 3), "an in-range index is untouched")
    }
}
