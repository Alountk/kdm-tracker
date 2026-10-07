package io.kdmtracker.core.content

import io.kdmtracker.core.content.generated.Core15Gear
import io.kdmtracker.core.content.generated.Core15Locations
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Core15GearTest {
    @Test
    fun theGeneratedPackHasGear() {
        assertTrue(Core15Gear.bundle.gear.size > 50)
        assertTrue(Core15Gear.bundle.gear.any { it.name == "Yelmo de Calavera" })
    }

    @Test
    fun aGearCarriesItsKeywords() {
        val yelmo = Core15Gear.bundle.gear.single { it.name == "Yelmo de Calavera" }

        assertTrue("armadura" in yelmo.keywords, "keywords: ${yelmo.keywords}")
    }

    @Test
    fun aRecipeCarriesItsCanonicalCost() {
        val hacha =
            Core15Gear.bundle.locations
                .flatMap { it.recipes }
                .single { it.gearName == "Hacha de Contrapeso" }

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
        val gearPack = ContentPack(id = "gear", version = "1.5", name = "gear", bundle = Core15Gear.bundle)

        val armeria =
            ContentCatalog(listOf(locationsPack, gearPack)).locationDefinitions.single { it.name == "Armería" }

        assertTrue(armeria.buildCost.isNotEmpty())
        assertTrue(armeria.recipes.isNotEmpty())
    }
}
