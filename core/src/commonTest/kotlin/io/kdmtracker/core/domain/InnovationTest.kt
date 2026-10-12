package io.kdmtracker.core.domain

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InnovationTest {
    private val iron =
        Innovation(
            id = InnovationId("iron"),
            names = LocalizedText(es = "Iron"),
            description = "Smelt iron ore into tools.",
            consequences = listOf("Blade Sharpen", "Iron Weapons"),
            survivalLimitBonus = 1,
        )
    private val ammonia =
        Innovation(
            id = InnovationId("ammonia"),
            names = LocalizedText(es = "Ammonia"),
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
    fun deckContainsOnlyTheUnlockedConsequences() {
        val collection = InnovationCollection().with(iron)

        val cards = InnovationDeck.of(collection).cards

        assertEquals(listOf("Blade Sharpen", "Iron Weapons"), cards)
        assertFalse("Iron" in cards)
    }

    @Test
    fun deckIgnoresAnInnovationWithNoConsequences() {
        val plain =
            Innovation(
                id = InnovationId("hovel"),
                names = LocalizedText(es = "Hovel"),
                description = "A place to sleep.",
            )

        assertTrue(InnovationDeck.of(InnovationCollection().with(plain)).cards.isEmpty())
    }

    @Test
    fun deckIsDedupedAndSorted() {
        val other =
            Innovation(
                id = InnovationId("soap"),
                names = LocalizedText(es = "Soap"),
                description = "Boil fat with ash.",
                consequences = listOf("Iron Weapons"),
            )
        val collection = InnovationCollection().with(other).with(iron).with(ammonia)

        val cards = InnovationDeck.of(collection).cards

        assertEquals(listOf("Blade Sharpen", "Iron Weapons", "Ointment"), cards)
        assertEquals(cards, cards.distinct())
        assertEquals(cards.sorted(), cards)
        assertFalse("Iron" in cards)
        assertFalse("Ammonia" in cards)
        assertFalse("Soap" in cards)
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

    @Test
    fun categoryIsNeverSerialized() {
        val innovation =
            Innovation(
                id = InnovationId("pintura"),
                names = LocalizedText(es = "Pintura"),
                description = "Pintura ritual.",
                category = "Arte",
            )

        val json = Json.encodeToString(Innovation.serializer(), innovation)

        assertFalse(json.contains("category"), "category is content, not state — it never travels")
        assertEquals("Arte", innovation.category, "encoding must not touch the value in memory")
        assertEquals(
            "",
            Json.decodeFromString<Innovation>(json).category,
            "a document written before the column existed decodes to none, not to a crash",
        )
    }
}
