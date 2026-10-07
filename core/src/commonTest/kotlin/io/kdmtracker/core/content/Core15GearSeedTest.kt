package io.kdmtracker.core.content

import io.kdmtracker.core.content.generated.Core15Locations
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Core15GearSeedTest {
    @Test
    fun theSeedPinsTheArmeriaRecipes() {
        val armeria = Core15GearSeed.bundle.locations.single { it.name == "Armería" }
        val hacha = armeria.recipes.single { it.gearName == "Hacha de Contrapeso" }

        assertEquals(
            mapOf("Hueso de Monstruo" to 2, "Piel de Monstruo" to 1, "Órgano de Monstruo" to 1),
            hacha.resourceCost,
        )
    }

    @Test
    fun composingCombinesBuildDataAndRecipesForTheSameLocation() {
        val locationsPack =
            ContentPack(
                id = "loc",
                version = "1.5",
                name = "loc",
                bundle = ContentBundle(locations = Core15Locations.bundle.locations),
            )
        val seedPack = ContentPack(id = "seed", version = "1.5", name = "seed", bundle = Core15GearSeed.bundle)

        val armeria =
            ContentCatalog(
                listOf(locationsPack, seedPack),
            ).locationDefinitions.single { it.name == "Armería" }

        assertTrue(armeria.buildCost.isNotEmpty())
        assertTrue(armeria.recipes.isNotEmpty())
    }
}
