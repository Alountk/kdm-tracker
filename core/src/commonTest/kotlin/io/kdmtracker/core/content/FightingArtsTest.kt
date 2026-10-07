package io.kdmtracker.core.content

import io.kdmtracker.core.content.generated.Core15FightingArts
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FightingArtsTest {
    private val fightingArts = Legacy15Content.pack.bundle.fightingArts
    private val disorders = Legacy15Content.pack.bundle.disorders

    @Test
    fun fightingArtsAreNotEmpty() {
        assertTrue(fightingArts.isNotEmpty(), "expected generated 1.5 fighting arts")
    }

    @Test
    fun disordersAreNotEmpty() {
        assertTrue(disorders.isNotEmpty(), "expected generated 1.5 disorders")
    }

    @Test
    fun fightingArtsHaveTheExpectedCardCount() {
        assertEquals(24, fightingArts.size)
    }

    @Test
    fun disordersHaveTheExpectedCardCount() {
        assertEquals(23, disorders.size)
    }

    @Test
    fun knownFightingArtIsPresentWithNonBlankDescription() {
        val duro = fightingArts.first { it.name == "Duro" }

        assertTrue(duro.description.contains("lesiones graves"), "was ${duro.description}")
    }

    @Test
    fun knownDisorderIsPresentWithNonBlankDescription() {
        val acaparador = disorders.first { it.name == "Acaparador" }

        assertTrue(acaparador.description.isNotBlank(), "expected the Acaparador effect text")
    }

    @Test
    fun packCarriesTheGeneratedFightingArtsAndDisorders() {
        assertEquals(Core15FightingArts.bundle.fightingArts, fightingArts)
        assertEquals(Core15FightingArts.bundle.disorders, disorders)
    }

    @Test
    fun packComposes() {
        val result = ContentCatalogComposer.compose(listOf(Legacy15Content.pack))

        assertTrue(result is CatalogResult.Ok, "expected the 1.5 pack to compose, was $result")
    }

    @Test
    fun namesAreSorted() {
        assertEquals(fightingArts.map { it.name }.sorted(), fightingArts.map { it.name })
        assertEquals(disorders.map { it.name }.sorted(), disorders.map { it.name })
    }
}
