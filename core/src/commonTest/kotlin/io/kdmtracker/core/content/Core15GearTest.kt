package io.kdmtracker.core.content

import io.kdmtracker.core.content.generated.Core15Gear
import io.kdmtracker.core.content.generated.Core15Locations
import io.kdmtracker.core.domain.GearId
import io.kdmtracker.core.domain.ResourceId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Core15GearTest {
    @Test
    fun theGeneratedPackHasGear() {
        assertTrue(Core15Gear.bundle.gear.size > 50)
        assertTrue(Core15Gear.bundle.gear.any { it.names.es == "Yelmo de Calavera" })
    }

    @Test
    fun aGearCarriesItsKeywords() {
        val yelmo = Core15Gear.bundle.gear.single { it.names.es == "Yelmo de Calavera" }

        assertTrue("armadura" in yelmo.keywords, "keywords: ${yelmo.keywords}")
    }

    @Test
    fun aRecipeCarriesItsCanonicalCost() {
        val hacha =
            Core15Gear.bundle.locations
                .flatMap { it.recipes }
                .single { it.gearId == GearId("hacha_de_contrapeso") }

        assertEquals(
            mapOf(
                ResourceId("bone.monster") to 2,
                ResourceId("hide.monster") to 1,
                ResourceId("organ.monster") to 1,
            ),
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
            ContentCatalog(listOf(locationsPack, gearPack)).locationDefinitions.single { it.names.es == "Armería" }

        assertTrue(armeria.buildCost.isNotEmpty())
        assertTrue(armeria.recipes.isNotEmpty())
    }
}
