package io.kdmtracker.app

import io.kdmtracker.core.content.ContentBundle
import io.kdmtracker.core.content.ContentCatalog
import io.kdmtracker.core.content.ContentPack
import io.kdmtracker.core.content.resourceDefinition
import io.kdmtracker.core.domain.Gear
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
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pins the controller's Craft action: it delegates to `:core`'s `Craft`, persists an `Ok`
 * result through [SettlementRepository.update], and surfaces a `Blocked` reason without
 * changing the record.
 */
class CraftControllerTest {
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
                                                    "Hacha de Contrapeso",
                                                    resourceCost = mapOf("Hueso de Monstruo" to 2),
                                                ),
                                            ),
                                    ),
                                ),
                            resources = listOf(resourceDefinition("Hueso de Monstruo", ResourceCategory.MONSTER)),
                            gear = listOf(Gear("Hacha de Contrapeso")),
                        ),
                ),
            ),
        )

    private fun storeWith(
        built: List<LocationId>,
        bones: Int,
    ): InMemoryCampaignStore {
        val store = InMemoryCampaignStore()
        val repository = SettlementRepository(store)
        repository.update(
            repository.record.copy(
                state =
                    repository.record.state.copy(
                        locations = LocationSet(built = built),
                        resourceEntries = listOf(ResourceEntry(ResourceId("Hueso de Monstruo"), bones)),
                    ),
            ),
        )
        return store
    }

    private fun controller(store: InMemoryCampaignStore): LanternYearController =
        LanternYearController(SettlementRepository(store), catalog = catalog)

    @Test
    fun aValidCraftPersistsAndAFreshRepositoryReadsIt() {
        val store = storeWith(built = listOf(LocationId("weapon_crafter")), bones = 3)
        val controller = controller(store)

        controller.craft("Armería", "Hacha de Contrapeso")

        assertNull(controller.state.value.actionError)
        assertTrue("Hacha de Contrapeso" in controller.state.value.gear)
        val fresh = SettlementRepository(store).record
        assertTrue("Hacha de Contrapeso" in fresh.state.gear)
        assertEquals(1, fresh.storage().single { it.id.value == "Hueso de Monstruo" }.quantity)
    }

    @Test
    fun craftingAtAnUnbuiltLocationIsBlockedAndChangesNothing() {
        val store = storeWith(built = emptyList(), bones = 3)
        val controller = controller(store)

        controller.craft("Armería", "Hacha de Contrapeso")

        assertNotNull(controller.state.value.actionError)
        assertTrue(
            controller.state.value.gear
                .isEmpty(),
        )
        assertTrue(
            SettlementRepository(store)
                .record.state.gear
                .isEmpty(),
        )
    }

    @Test
    fun theSeededCampaignHasAReachableRecipe() {
        val controller = LanternYearController(SettlementRepository(InMemoryCampaignStore()))

        fun record(
            name: String,
            count: Int,
        ) {
            val definition =
                controller.state.value.resourceDefinitions
                    .single { it.names.es == name }
            controller.addResource(ResourceEntry(definition.id, count))
        }
        record("Hueso de Monstruo", 2)
        record("Piel de Monstruo", 1)
        record("Órgano de Monstruo", 1)

        controller.craft("Armería", "Hacha de Contrapeso")

        assertNull(controller.state.value.actionError)
        assertTrue("Hacha de Contrapeso" in controller.state.value.gear)
    }
}
