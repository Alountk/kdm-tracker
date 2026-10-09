package io.kdmtracker.core.content

import io.kdmtracker.core.domain.MonsterCard
import io.kdmtracker.core.domain.MonsterCardKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MonsterCardsCatalogTest {
    private fun pack(
        id: String,
        vararg cards: MonsterCard,
    ) = ContentPack(id = id, version = "1", name = id, bundle = ContentBundle(monsterCards = cards.toList()))

    private fun ok(result: CatalogResult): ContentCatalog {
        assertTrue(result is CatalogResult.Ok, "expected Ok but was $result")
        return result.catalog
    }

    @Test
    fun monsterCardsForReturnsOnlyTheNamedMonster() {
        val catalog =
            ok(
                ContentCatalogComposer.compose(
                    listOf(
                        pack(
                            "a",
                            MonsterCard("Zarpazo", "t", MonsterCardKind.AI, "White Lion"),
                            MonsterCard("Cabeza", "t", MonsterCardKind.HIT_LOCATION, "White Lion"),
                            MonsterCard("Mutilar", "t", MonsterCardKind.AI, "Phoenix"),
                        ),
                    ),
                ),
            )

        assertEquals(setOf("Zarpazo", "Cabeza"), catalog.monsterCardsFor("White Lion").map { it.name }.toSet())
    }

    @Test
    fun monsterCardsMergeDedupesByMonsterKindAndName() {
        val a = pack("a", MonsterCard("Zarpazo", "t", MonsterCardKind.AI, "White Lion"))
        val b = pack("b", MonsterCard("Zarpazo", "t", MonsterCardKind.AI, "White Lion"))

        val catalog = ok(ContentCatalogComposer.compose(listOf(a, b)))

        assertEquals(1, catalog.monsterCardDefinitions.size)
    }
}
