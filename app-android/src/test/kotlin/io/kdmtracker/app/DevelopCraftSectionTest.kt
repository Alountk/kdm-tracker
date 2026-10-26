package io.kdmtracker.app

import io.kdmtracker.core.content.ContentBundle
import io.kdmtracker.core.content.ContentCatalog
import io.kdmtracker.core.content.ContentPack
import io.kdmtracker.core.content.resourceDefinition
import io.kdmtracker.core.domain.Gear
import io.kdmtracker.core.domain.GearId
import io.kdmtracker.core.domain.GearRecipe
import io.kdmtracker.core.domain.LocalizedText
import io.kdmtracker.core.domain.LocationId
import io.kdmtracker.core.domain.LocationSet
import io.kdmtracker.core.domain.ResourceCategory
import io.kdmtracker.core.domain.ResourceEntry
import io.kdmtracker.core.domain.ResourceId
import io.kdmtracker.core.domain.SettlementLocation
import io.kdmtracker.core.persistence.InMemoryCampaignStore
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Pins what the Develop Craft section reads from [UiState]: the built location's recipes are
 * exposed (so the section can render them), and the settlement's crafted gear is surfaced.
 */
class DevelopCraftSectionTest {
    private val catalog =
        ContentCatalog(
            listOf(
                ContentPack(
                    id = "test",
                    version = "1",
                    name = "Test",
                    bundle =
                        ContentBundle(
                            locations =
                                listOf(
                                    SettlementLocation(
                                        LocationId("weapon_crafter"),
                                        LocalizedText(es = "Armería"),
                                        recipes =
                                            listOf(
                                                GearRecipe(
                                                    GearId("hacha_de_contrapeso"),
                                                    resourceCost =
                                                        mapOf(
                                                            ResourceId("Hueso de Monstruo") to 2,
                                                        ),
                                                ),
                                            ),
                                    ),
                                ),
                            resources = listOf(resourceDefinition("Hueso de Monstruo", ResourceCategory.MONSTER)),
                            gear =
                                listOf(
                                    Gear(GearId("hacha_de_contrapeso"), LocalizedText(es = "Hacha de Contrapeso")),
                                ),
                        ),
                ),
            ),
        )

    private fun controller(): LanternYearController {
        val store = InMemoryCampaignStore()
        val repository = SettlementRepository(store)
        repository.update(
            repository.record.copy(
                state =
                    repository.record.state.copy(
                        locations = LocationSet(built = listOf(LocationId("weapon_crafter"))),
                        resourceEntries = listOf(ResourceEntry(ResourceId("Hueso de Monstruo"), 2)),
                    ),
            ),
        )
        return LanternYearController(SettlementRepository(store), catalog = catalog)
    }

    @Test
    fun theDevelopViewExposesTheBuiltLocationsRecipes() {
        val state = controller().state.value

        assertTrue("Armería" in state.builtLocations)
        assertTrue(
            state.catalogLocations
                .single { it.names.es == "Armería" }
                .recipes
                .any { it.gearId == GearId("hacha_de_contrapeso") },
        )
    }

    @Test
    fun craftingSurfacesTheGearInTheState() {
        val controller = controller()

        controller.settlement.craft("Armería", "Hacha de Contrapeso")

        assertTrue("Hacha de Contrapeso" in controller.state.value.gear)
    }
}
