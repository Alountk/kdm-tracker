package io.kdmtracker.core.content

import io.kdmtracker.core.content.generated.Core15Innovations
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GeneratedInnovationsTest {
    private val innovations = Core15Innovations.bundle.innovations

    @Test
    fun bundleIsNotEmpty() {
        assertTrue(innovations.isNotEmpty(), "expected generated 1.5 innovations")
    }

    @Test
    fun bundleHasTheExpectedCardCount() {
        assertEquals(42, innovations.size)
    }

    @Test
    fun lenguajeExistsWithSurvivalLimitBonusOne() {
        val lenguaje = innovations.first { it.name == "Lenguaje" }

        assertEquals(1, lenguaje.survivalLimitBonus)
    }

    @Test
    fun lenguajeListsLinternaInteriorAsConsequence() {
        val lenguaje = innovations.first { it.name == "Lenguaje" }

        assertTrue("Linterna Interior" in lenguaje.consequences, "was ${lenguaje.consequences}")
    }

    @Test
    fun simposioListsItsConsequences() {
        val simposio = innovations.first { it.name == "Simposio" }

        assertEquals(listOf("Entrenamiento de Pesadilla", "Narración"), simposio.consequences)
    }

    @Test
    fun knownDescriptionIsNonBlank() {
        val lenguaje = innovations.first { it.name == "Lenguaje" }

        assertTrue(lenguaje.description.isNotBlank())
    }

    @Test
    fun namesAreSorted() {
        val names = innovations.map { it.name }

        assertEquals(names.sorted(), names)
    }
}
