package io.kdmtracker.core.content

import io.kdmtracker.core.domain.Gear
import io.kdmtracker.core.domain.GearId
import io.kdmtracker.core.domain.LocalizedText
import io.kdmtracker.core.domain.Modifier
import io.kdmtracker.core.domain.ModifierScope
import io.kdmtracker.core.domain.ModifierTarget
import io.kdmtracker.core.domain.SettlementLocation
import io.kdmtracker.core.domain.SettlementState
import io.kdmtracker.core.rules.DerivedValues
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GearCatalogTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun pack(
        id: String,
        gear: List<Gear>,
    ): ContentPack = ContentPack(id = id, version = "1", name = id, bundle = ContentBundle(gear = gear))

    @Test
    fun legacySettlementStateJsonWithoutGearDecodesEmpty() {
        val state = json.decodeFromString<SettlementState>("""{"deaths":1}""")

        assertTrue(state.gear.isEmpty())
    }

    @Test
    fun settlementLocationJsonWithoutRecipesDecodesEmpty() {
        val location = json.decodeFromString<SettlementLocation>("""{"id":"x","names":{"es":"X"}}""")

        assertTrue(location.recipes.isEmpty())
    }

    @Test
    fun catalogDedupsGearByName() {
        val catalog =
            ContentCatalog(
                listOf(
                    pack("a", listOf(Gear(GearId("x"), LocalizedText(es = "X"), rules = "first"))),
                    pack("b", listOf(Gear(GearId("x"), LocalizedText(es = "X"), rules = "second"))),
                ),
            )

        assertEquals(1, catalog.gearDefinitions.size)
        assertEquals("first", catalog.gearDefinitions.single().rules)
    }

    @Test
    fun gearModifiersForResolvesKnownNamesAndIgnoresUnknown() {
        val gear = Gear(GearId("a"), LocalizedText(es = "A"), modifiers = listOf(Modifier(ModifierTarget.STRENGTH, 1)))
        val catalog = ContentCatalog(listOf(pack("a", listOf(gear))))

        val modifiers = catalog.gearModifiersFor(listOf("A", "Unknown"))

        assertEquals(gear.modifiers, modifiers)
    }

    @Test
    fun modifierTotalSumsOnlyMatchingTargetAndScope() {
        val modifiers =
            listOf(
                Modifier(ModifierTarget.SURVIVAL_LIMIT, 2, ModifierScope.SETTLEMENT),
                Modifier(ModifierTarget.SURVIVAL_LIMIT, 5, ModifierScope.SURVIVOR),
                Modifier(ModifierTarget.STRENGTH, 3, ModifierScope.SETTLEMENT),
            )

        assertEquals(2, DerivedValues.modifierTotal(modifiers, ModifierTarget.SURVIVAL_LIMIT, ModifierScope.SETTLEMENT))
        assertEquals(3, DerivedValues.modifierTotal(modifiers, ModifierTarget.STRENGTH, ModifierScope.SETTLEMENT))
    }
}
