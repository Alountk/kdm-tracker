package io.kdmtracker.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InnovationTest {
    private val iron =
        Innovation(
            name = "Iron",
            description = "Smelt iron ore into tools.",
            consequences = listOf("Blade Sharpen", "Iron Weapons"),
            survivalLimitBonus = 1,
        )
    private val ammonia =
        Innovation(
            name = "Ammonia",
            description = "Refine ammonia from soil.",
            consequences = listOf("Ointment"),
            survivalLimitBonus = 0,
        )
    private val catalog = listOf(iron, ammonia)

    @Test
    fun defaultsAreEmpty() {
        val collection = InnovationCollection()

        assertTrue(collection.innovations.isEmpty())
        assertEquals(InnovationDeck(emptyList()), InnovationDeck.of(collection))
    }

    @Test
    fun withAddsAnInnovation() {
        val collection = InnovationCollection().with(iron)

        assertEquals(listOf(iron), collection.innovations)
    }

    @Test
    fun withDedupesByNameKeepingTheFirst() {
        val replacement = iron.copy(description = "changed")

        val collection = InnovationCollection().with(iron).with(replacement)

        assertEquals(listOf(iron), collection.innovations)
    }

    @Test
    fun ofResolvesKnownNamesAgainstTheCatalog() {
        val collection = InnovationCollection.of(listOf("Ammonia", "Iron"), catalog)

        assertEquals(listOf(ammonia, iron), collection.innovations)
    }

    @Test
    fun ofIgnoresUnknownNamesDeterministically() {
        val first = InnovationCollection.of(listOf("Iron", "Does Not Exist"), catalog)
        val second = InnovationCollection.of(listOf("Iron", "Does Not Exist"), catalog)

        assertEquals(listOf(iron), first.innovations)
        assertEquals(first, second)
    }

    @Test
    fun ofIgnoresARunOfUnknownNames() {
        val collection = InnovationCollection.of(listOf("Ghost", "Iron", "Phantom"), catalog)

        assertEquals(listOf(iron), collection.innovations)
    }

    @Test
    fun ofDedupesRepeatedNames() {
        val collection = InnovationCollection.of(listOf("Iron", "Iron"), catalog)

        assertEquals(listOf(iron), collection.innovations)
    }

    @Test
    fun deckContainsUnlockedNamesAndTheirConsequences() {
        val collection = InnovationCollection().with(iron)

        assertEquals(
            listOf("Blade Sharpen", "Iron", "Iron Weapons"),
            InnovationDeck.of(collection).cards,
        )
    }

    @Test
    fun deckIsDedupedAndSorted() {
        val other =
            Innovation(
                name = "Soap",
                description = "Boil fat with ash.",
                consequences = listOf("Iron Weapons"),
            )
        val collection = InnovationCollection().with(other).with(iron).with(ammonia)

        val cards = InnovationDeck.of(collection).cards

        assertEquals(listOf("Ammonia", "Blade Sharpen", "Iron", "Iron Weapons", "Ointment", "Soap"), cards)
        assertEquals(cards, cards.distinct())
        assertEquals(cards.sorted(), cards)
    }

    @Test
    fun survivalLimitBonusSumsTheInnovations() {
        val collection = InnovationCollection().with(iron).with(ammonia)

        assertEquals(1, collection.survivalLimitBonus())
    }

    @Test
    fun survivalLimitBonusIsZeroWithoutInnovations() {
        assertEquals(0, InnovationCollection().survivalLimitBonus())
    }
}
