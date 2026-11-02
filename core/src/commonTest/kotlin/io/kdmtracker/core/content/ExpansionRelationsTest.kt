package io.kdmtracker.core.content

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExpansionRelationsTest {
    private val relations = ExpansionRelations.chart

    private fun of(kind: RelationKind) = relations.filter { it.kind == kind }

    @Test
    fun theChartContributesItsSixRedPairsAndSeventeenOrangeOnes() {
        assertEquals(6, of(RelationKind.INCOMPATIBLE).size)
        assertEquals(17, of(RelationKind.COMPATIBILITY_ISSUES).size)
    }

    @Test
    fun noRelationIsOfficialBecauseNobodyConfirmedTheChart() {
        assertTrue(relations.all { it.source == RelationSource.CHART })
    }

    @Test
    fun noPairIsListedTwiceInEitherDirection() {
        val pairs = relations.map { setOf(it.a, it.b) to it.kind }

        assertEquals(pairs.distinct().size, pairs.size)
    }

    @Test
    fun theShippedRegistryCarriesThemYetWarnsAboutNothingWhileNoExpansionShips() {
        val selection = PackRegistry.shipped.select(emptyList())

        assertEquals(PackSelection.Ok(PackRegistry.shipped.baseIds), selection)
    }

    @Test
    fun onceTheirPacksExistTwoOfThemWarnButStayEnabled() {
        val registry =
            PackRegistry(
                listOf(
                    RegisteredPack(ContentPack("base", "1", "base"), base = true),
                    RegisteredPack(ContentPack("ivory-dragon", "1", "Ivory Dragon", listOf("base"))),
                    RegisteredPack(ContentPack("lion-god", "1", "Lion God", listOf("base"))),
                ),
                relations,
            )

        val selection = registry.select(listOf("ivory-dragon", "lion-god"))

        assertTrue(selection is PackSelection.Ok)
        assertEquals(listOf("base", "ivory-dragon", "lion-god"), selection.enabled)
        assertEquals(listOf(RelationKind.INCOMPATIBLE), selection.warnings.map { it.kind })
    }
}
