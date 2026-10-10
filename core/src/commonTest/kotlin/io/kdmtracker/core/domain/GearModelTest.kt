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
                id = GearId("daga_de_hueso"),
                names = LocalizedText(es = "Daga de Hueso"),
                keywords = listOf("arma", "melé"),
                rules = "Lento, Débil.",
                modifiers = listOf(Modifier(ModifierTarget.STRENGTH, 1, ModifierScope.SURVIVOR)),
            )

        val decoded = json.decodeFromString<Gear>(json.encodeToString(gear))

        assertEquals(gear, decoded)
    }

    @Test
    fun defaultsAreEmptyAndSurvivorScoped() {
        val gear = Gear(GearId("daga"), LocalizedText(es = "Daga"))

        assertTrue(gear.keywords.isEmpty())
        assertTrue(gear.modifiers.isEmpty())
        assertEquals("", gear.rules)

        val modifier = Modifier(ModifierTarget.STRENGTH, 1)
        assertEquals(ModifierScope.SURVIVOR, modifier.scope)
        assertNull(modifier.condition)
    }

    @Test
    fun recipeDefaultsAreEmpty() {
        val recipe = GearRecipe(gearId = GearId("daga"))

        assertTrue(recipe.resourceCost.isEmpty())
        assertTrue(recipe.requires.isEmpty())
    }

    @Test
    fun gearJsonWithoutModifiersDecodesEmpty() {
        val shaped = """{"id":"daga","names":{"es":"Daga"},"keywords":[],"rules":""}"""

        val gear = json.decodeFromString<Gear>(shaped)

        assertTrue(gear.modifiers.isEmpty())
    }

    @Test
    fun recipeRequiresTextRoundTrips() {
        val recipe = GearRecipe(GearId("katana_del_arcoiris"), requiresText = "Calor")

        val decoded = json.decodeFromString<GearRecipe>(json.encodeToString(recipe))

        assertEquals(recipe, decoded)
    }

    @Test
    fun recipeJsonWithoutRequiresTextDecodesNull() {
        val recipe = json.decodeFromString<GearRecipe>("""{"gearId":"x"}""")

        assertNull(recipe.requiresText)
    }
}
