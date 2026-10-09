package io.kdmtracker.app

import io.kdmtracker.app.ui.MentalState
import io.kdmtracker.app.ui.SurvivorSheetModel
import io.kdmtracker.core.domain.Survivor
import io.kdmtracker.core.domain.Ulid
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pins the derivations the survivor sheet renders: every hit location shows even with no wounds
 * recorded, the mental state flips exactly at the ruleset threshold, survival spending is closed at
 * the limit, and the gear grid always offers its nine slots.
 */
class SurvivorSheetViewsTest {
    private fun survivor(): Survivor = Survivor(id = Ulid.generate(1L) { 0 }, name = "Erza")

    private fun modelOf(survivor: Survivor): SurvivorSheetModel =
        SurvivorSheetModel.from(
            survivor = survivor,
            survivalLimit = 2,
            huntXpTrackMax = 16,
            insanityThreshold = 3,
        )

    @Test
    fun anEmptyWoundMapStillShowsEveryHitLocation() {
        val rows = modelOf(survivor()).hitLocations

        assertEquals(5, rows.size)
        assertTrue(rows.all { it.armor == 0 && !it.light && !it.heavy })
    }

    @Test
    fun theMentalStateFlipsExactlyAtTheThreshold() {
        assertEquals(MentalState.SANE, modelOf(survivor().copy(insanity = 2)).mentalState)
        assertEquals(MentalState.INSANE, modelOf(survivor().copy(insanity = 3)).mentalState)
    }

    @Test
    fun survivalSpendingClosesAtTheLimit() {
        assertTrue(modelOf(survivor().copy(survival = 1)).canSpendSurvival)
        assertFalse(modelOf(survivor().copy(survival = 2)).canSpendSurvival)
    }

    @Test
    fun theGearGridAlwaysOffersNineSlots() {
        assertEquals(9, modelOf(survivor()).gearSlots)
        assertEquals(9, modelOf(survivor().copy(gear = List(9) { "gear-$it" })).gearSlots)
    }

    @Test
    fun theSheetCarriesTheDesignsSpanishLabels() {
        val model = modelOf(survivor())

        assertEquals(listOf("MOV", "PREC", "FZA", "EVA", "SUE", "VEL"), model.attributes.map { it.label })
        assertEquals(
            listOf("Cabeza", "Brazos", "Torso", "Cintura", "Piernas"),
            model.hitLocations.map { it.label },
        )
    }

    @Test
    fun theHuntXpTrackTakesItsLengthAndItsCarryFromTheRuleset() {
        val model = modelOf(survivor().copy(huntXp = 4))

        assertEquals(4, model.huntXp)
        assertEquals(16, model.huntXpMax)
    }
}
