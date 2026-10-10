package io.kdmtracker.app

import io.kdmtracker.core.domain.GearId
import io.kdmtracker.core.domain.ResourceEntry
import io.kdmtracker.core.persistence.InMemoryCampaignStore
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Cross-tab integration: recording resources (Recursos tab) flips a recipe's craftability
 * (Fabricar tab), which is the whole point of the derived views.
 */
class DevelopTabsTest {
    @Test
    fun recordingResourcesMakesARecipeCraftable() {
        val controller = LanternYearController(SettlementRepository(InMemoryCampaignStore()))

        fun record(name: String) {
            val definition =
                controller.state.value.resourceDefinitions
                    .single { it.names.es == name }
            controller.addResource(ResourceEntry(definition.id, 1))
        }

        fun hacha() =
            controller.state.value.craftRecipes
                .single { it.recipe.gearId == GearId("hacha_de_contrapeso") }

        assertFalse(hacha().check.craftable)

        record("Hueso de Monstruo")
        record("Hueso de Monstruo")
        record("Piel de Monstruo")
        record("Órgano de Monstruo")

        assertTrue(hacha().check.craftable)
    }
}
