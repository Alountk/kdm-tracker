package io.kdmtracker.core.rules

import io.kdmtracker.core.domain.StepType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SettlementStepCatalogTest {
    @Test
    fun allHasElevenStepsNumberedInOrder() {
        assertEquals(11, SettlementStepCatalog.ALL.size)
        assertEquals(11, SettlementStepCatalog.TOTAL)
        SettlementStepCatalog.ALL.forEachIndexed { index, step ->
            assertEquals(index + 1, step.number)
        }
    }

    @Test
    fun byNumberReturnsStepNineAsBranching() {
        assertEquals(StepType.BRANCHING, SettlementStepCatalog.byNumber(9).type)
    }

    @Test
    fun byNumberReturnsStepSevenTitledDesarrollo() {
        assertEquals("Desarrollo", SettlementStepCatalog.byNumber(7).title)
    }

    @Test
    fun byNumberThrowsForUnknownNumber() {
        assertFailsWith<NoSuchElementException> {
            SettlementStepCatalog.byNumber(12)
        }
    }

    @Test
    fun theStepsCarryTheDesignsSpanishNames() {
        assertEquals(
            listOf(
                "Preparación",
                "Supervivientes Regresan",
                "Ganar Esfuerzos",
                "Actualizar Línea de Tiempo",
                "Actualizar Contador de Muertes",
                "Comprobar Hitos",
                "Desarrollo",
                "Preparar Supervivientes",
                "Enfrentamiento Especial",
                "Anotar y Almacenar",
                "El Fin del Principio",
            ),
            SettlementStepCatalog.ALL.map { it.title },
        )
    }

    @Test
    fun everyStepStillCarriesASummary() {
        assertEquals(11, SettlementStepCatalog.ALL.size)
        assertTrue(SettlementStepCatalog.ALL.all { it.summary.isNotBlank() })
    }
}
