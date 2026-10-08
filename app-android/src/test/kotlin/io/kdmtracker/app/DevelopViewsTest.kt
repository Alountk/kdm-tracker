package io.kdmtracker.app

import io.kdmtracker.core.persistence.InMemoryCampaignStore
import io.kdmtracker.core.rules.InnovationStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pins the derived Develop views on [UiState]: resources grouped by source, every innovation
 * classified into its access state, and each built location's recipes carrying a matching
 * craftability check.
 */
class DevelopViewsTest {
    private fun controller(): LanternYearController =
        LanternYearController(SettlementRepository(InMemoryCampaignStore()))

    @Test
    fun resourceGroupsCoverEveryResource() {
        val state = controller().state.value

        assertEquals(state.resourceDefinitions.size, state.resourceGroups.values.sumOf { it.size })
        assertTrue("Básicos" in state.resourceGroups)
    }

    @Test
    fun craftRecipesCarryAMatchingCheck() {
        val state = controller().state.value

        val armeria = state.craftRecipes.filter { it.locationName == "Armería" }
        assertTrue(armeria.isNotEmpty(), "the seed builds Armería with recipes")
        assertTrue(armeria.all { !it.check.craftable && it.check.missing != null })
    }

    @Test
    fun everyInnovationIsClassified() {
        val state = controller().state.value

        assertEquals(state.innovationDefinitions.size, state.innovationViews.size)
        assertTrue(state.innovationViews.all { it.status == InnovationStatus.NO_ACCESS })
    }
}
