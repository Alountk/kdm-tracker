package io.kdmtracker.app

import io.kdmtracker.app.ui.settlement.InnovationsModel
import io.kdmtracker.app.ui.settlement.LocationsModel
import io.kdmtracker.core.domain.SettlementLocation
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins the Develop step's two panels: the innovation draw text must come from the ruleset (so a
 * house rule changes it) and the locations must split into built and buildable by their requirement.
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
    fun aLocationWithNoBuildEndeavorsIsAlreadyBuilt() {
        val model =
            LocationsModel.of(
                built = listOf("El Cúmulo de Linternas"),
                catalog =
                    listOf(
                        SettlementLocation("El Cúmulo de Linternas"),
                        SettlementLocation("Taller de Huesos", buildEndeavors = 1),
                    ),
            )

        assertEquals(listOf("El Cúmulo de Linternas"), model.built.map { it.name })
        assertEquals(listOf("Taller de Huesos"), model.buildable.map { it.name })
        assertEquals(1, model.buildable.single().endeavors)
    }
}
