package io.kdmtracker.app

import io.kdmtracker.app.ui.settlement.InnovationsModel
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins the Develop step's Innovations panel model: the draw text must come from the ruleset (so a
 * house rule changes it) and the principle slots pass the chosen labels through unchanged.
 * The locations side of the old panel now lives in `FabricarModelTest`.
 */
class DevelopPanelsTest {
    @Test
    fun theDrawLabelFollowsTheRulesetNotALiteral() {
        assertEquals("Robar 2, elegir 1", InnovationsModel.of(emptyList(), emptyList(), draw = 2, keep = 1).drawLabel)
        assertEquals("Robar 4, elegir 1", InnovationsModel.of(emptyList(), emptyList(), draw = 4, keep = 1).drawLabel)
    }

    @Test
    fun theDeckSizeCountsTheCards() {
        assertEquals(14, InnovationsModel.of(List(14) { "i$it" }, emptyList(), 2, 1).deckSize)
    }

    @Test
    fun aPrincipleSlotShowsTheChoiceLabelNotTheCategoryPrefix() {
        // The controller used to hand-build "DEATH: …"; S2 removed the prefix at the source and
        // the slot passes the chosen label through unchanged (the model expects the chosen
        // principles — three slots max, empties for the rest).
        val model = InnovationsModel.of(emptyList(), listOf("The First Harvest → Cannibalize"), draw = 2, keep = 1)

        assertEquals(listOf("The First Harvest → Cannibalize"), model.principleSlots)
        assertEquals(2, model.emptySlots)
    }
}
