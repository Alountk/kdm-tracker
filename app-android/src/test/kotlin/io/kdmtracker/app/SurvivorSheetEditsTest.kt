package io.kdmtracker.app

import io.kdmtracker.app.ui.SurvivorSheetEdits
import io.kdmtracker.core.domain.HitLocation
import io.kdmtracker.core.domain.Survivor
import io.kdmtracker.core.domain.Ulid
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pins the clamping every sheet control relies on: counters never leave their range and the
 * toggles are involutions.
 */
class SurvivorSheetEditsTest {
    private fun survivor(): Survivor = Survivor(id = Ulid.generate(1L) { 0 })

    @Test
    fun survivalStaysWithinItsLimitAndAtZero() {
        assertEquals(2, SurvivorSheetEdits.survival(survivor().copy(survival = 2), limit = 2, delta = 1).survival)
        assertEquals(0, SurvivorSheetEdits.survival(survivor().copy(survival = 0), limit = 2, delta = -1).survival)
        assertEquals(1, SurvivorSheetEdits.survival(survivor().copy(survival = 0), limit = 2, delta = 1).survival)
    }

    @Test
    fun insanityAndHuntXpNeverGoBelowZeroAndHuntXpStopsAtTheCap() {
        assertEquals(0, SurvivorSheetEdits.insanity(survivor(), delta = -1).insanity)
        assertEquals(16, SurvivorSheetEdits.huntXp(survivor().copy(huntXp = 16), max = 16, delta = 1).huntXp)
        assertEquals(0, SurvivorSheetEdits.huntXp(survivor(), max = 16, delta = -1).huntXp)
    }

    @Test
    fun armorNeverGoesBelowZero() {
        assertEquals(0, SurvivorSheetEdits.armor(survivor(), HitLocation.HEAD, delta = -1).armorAt(HitLocation.HEAD))
        assertEquals(3, SurvivorSheetEdits.armor(survivor(), HitLocation.HEAD, delta = 3).armorAt(HitLocation.HEAD))
    }

    @Test
    fun theWoundTogglesAreInvolutions() {
        val survivor = survivor()

        val lightTwice =
            SurvivorSheetEdits.toggleLight(
                SurvivorSheetEdits.toggleLight(survivor, HitLocation.TORSO),
                HitLocation.TORSO,
            )
        val heavyTwice =
            SurvivorSheetEdits.toggleHeavy(
                SurvivorSheetEdits.toggleHeavy(survivor, HitLocation.TORSO),
                HitLocation.TORSO,
            )

        assertFalse(lightTwice.wounds.getValue(HitLocation.TORSO).light)
        assertFalse(heavyTwice.wounds.getValue(HitLocation.TORSO).heavy)
    }

    @Test
    fun aWoundToggleTouchesOnlyItsOwnLocation() {
        val survivor = SurvivorSheetEdits.toggleHeavy(survivor(), HitLocation.ARMS)

        assertTrue(survivor.wounds.getValue(HitLocation.ARMS).heavy)
        assertEquals(1, survivor.wounds.size)
    }

    @Test
    fun theTokenToggleIsAnInvolution() {
        val survivor = survivor()

        val twice = SurvivorSheetEdits.toggleToken(SurvivorSheetEdits.toggleToken(survivor, "Esquivar"), "Esquivar")

        assertEquals(survivor.tokens, twice.tokens)
        assertTrue(SurvivorSheetEdits.toggleToken(survivor, "Esquivar").tokens.contains("Esquivar"))
    }

    private fun Survivor.armorAt(location: HitLocation): Int = wounds[location]?.armor ?: 0
}
