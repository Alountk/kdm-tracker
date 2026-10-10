package io.kdmtracker.core.content

import io.kdmtracker.core.content.generated.Core15Gear
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Pins that the generated gear pack is self-consistent with the rest of the 1.5 content: every
 * recipe's resources and innovation prerequisites resolve against [Legacy15Content], so `Craft`
 * can never be blocked by content the pack does not carry.
 */
class Core15GearContentTest {
    @Test
    fun everyRecipeResolvesAgainstTheShippedPack() {
        val resources =
            Legacy15Content.pack.bundle.resources
                .map { it.names.es }
                .toSet()
        val innovations =
            Legacy15Content.pack.bundle.innovations
                .map { it.names.es }
                .toSet()

        Core15Gear.bundle.locations.flatMap { it.recipes }.forEach { recipe ->
            assertTrue(recipe.resourceCost.isNotEmpty(), "${recipe.gearName} has no cost")
            assertTrue(
                recipe.resourceCost.keys.all { it in resources },
                "${recipe.gearName} costs ${recipe.resourceCost.keys}",
            )
            assertTrue(
                recipe.requires.all { it in innovations },
                "${recipe.gearName} requires ${recipe.requires}",
            )
        }
    }

    @Test
    fun everyGearNameIsNonBlank() {
        assertTrue(Core15Gear.bundle.gear.all { it.name.isNotBlank() })
    }

    @Test
    fun theArmeriaLocationKeepsItsRecipes() {
        val armeria = Core15Gear.bundle.locations.single { it.names.es == "Armería" }

        assertTrue(armeria.recipes.size >= 6, "Armería recipes: ${armeria.recipes.map { it.gearName }}")
        assertTrue(armeria.recipes.any { it.gearName == "Katana del Arcoíris" })
    }
}
