package io.kdmtracker.core.domain

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GearModelTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun gearRoundTripsThroughJson() {
        val gear =
            Gear(
                name = "Daga de Hueso",
                keywords = listOf("arma", "melé"),
                rules = "Lento, Débil.",
                modifiers = listOf(Modifier(ModifierTarget.STRENGTH, 1, ModifierScope.SURVIVOR)),
            )

        val decoded = json.decodeFromString<Gear>(json.encodeToString(gear))

        assertEquals(gear, decoded)
    }

    @Test
    fun defaultsAreEmptyAndSurvivorScoped() {
        val gear = Gear("Daga")

        assertTrue(gear.keywords.isEmpty())
        assertTrue(gear.modifiers.isEmpty())
        assertEquals("", gear.rules)

        val modifier = Modifier(ModifierTarget.STRENGTH, 1)
        assertEquals(ModifierScope.SURVIVOR, modifier.scope)
        assertNull(modifier.condition)
    }

    @Test
    fun recipeDefaultsAreEmpty() {
        val recipe = GearRecipe(gearName = "Daga")

        assertTrue(recipe.resourceCost.isEmpty())
        assertTrue(recipe.requires.isEmpty())
    }

    @Test
    fun legacyGearJsonWithoutModifiersDecodesEmpty() {
        val legacy = """{"name":"Daga","keywords":[],"rules":""}"""

        val gear = json.decodeFromString<Gear>(legacy)

        assertTrue(gear.modifiers.isEmpty())
    }
}
