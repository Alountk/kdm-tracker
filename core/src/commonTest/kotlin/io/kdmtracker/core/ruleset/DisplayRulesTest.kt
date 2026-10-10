package io.kdmtracker.core.ruleset

import io.kdmtracker.core.domain.Survivor
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.domain.isInsane
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pins the two display rules the survivor sheet reads: the hunt-XP track length and the insanity
 * threshold that flips the mental state.
 */
class DisplayRulesTest {
    @Test
    fun coreRulesetPinsTheDisplayRules() {
        assertEquals(16, Core16Ruleset.huntXpTrackMax)
        assertEquals(3, Core16Ruleset.insanityThreshold)
    }

    @Test
    fun isInsaneFlipsExactlyAtTheThreshold() {
        val survivor = Survivor(id = Ulid.generate(1L) { 0 })

        assertFalse(survivor.copy(insanity = 2).isInsane(3))
        assertTrue(survivor.copy(insanity = 3).isInsane(3))
        assertTrue(survivor.copy(insanity = 4).isInsane(3))
    }

    @Test
    fun overridesCarryTheDisplayRulesThrough() {
        val overridden = Core16Ruleset.applyOverrides(RulesetOverrides(survivalLimit = 99))

        assertEquals(16, overridden.huntXpTrackMax)
        assertEquals(3, overridden.insanityThreshold)
    }

    @Test
    fun coreRulesetPinsTheInnovationDraw() {
        assertEquals(2, Core16Ruleset.innovationDraw)
        assertEquals(1, Core16Ruleset.innovationKeep)
    }

    @Test
    fun everyHouseRuleKnobSurvivesAnOverride() {
        val overridden =
            Core16Ruleset.applyOverrides(
                RulesetOverrides(
                    innovationDraw = 4,
                    innovationKeep = 1,
                    huntXpTrackMax = 20,
                    insanityThreshold = 5,
                ),
            )

        assertEquals(4, overridden.innovationDraw)
        assertEquals(1, overridden.innovationKeep)
        assertEquals(20, overridden.huntXpTrackMax)
        assertEquals(5, overridden.insanityThreshold)
    }

    @Test
    fun anUntouchedKnobKeepsTheOfficialValue() {
        val overridden = Core16Ruleset.applyOverrides(RulesetOverrides(survivalLimit = 9))

        assertEquals(2, overridden.innovationDraw)
        assertEquals(16, overridden.huntXpTrackMax)
    }
}
