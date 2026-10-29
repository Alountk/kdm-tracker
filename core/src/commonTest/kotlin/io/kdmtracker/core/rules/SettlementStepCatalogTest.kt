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
    fun threeSummariesCarryTheDesignsVerbatimCopy() {
        assertEquals(
            "Cada superviviente que sobrevive y regresa de la cacería genera 1 esfuerzo (*). " +
                "Se extinguen al concluir esta fase si no son consumidos.",
            SettlementStepCatalog.byNumber(3).summary,
        )
        assertEquals(
            "Selecciona una presa de la Lista de Presas y cuatro supervivientes con el valor " +
                "para adentrarse en la oscuridad infinita.",
            SettlementStepCatalog.byNumber(8).summary,
        )
        assertEquals(
            "Guarda cualquier recurso no invertido y equipo sin asignar en el almacén del asentamiento " +
                "para futuras innovaciones o cacerías.",
            SettlementStepCatalog.byNumber(10).summary,
        )
    }

    @Test
    fun everyStepStillCarriesASummary() {
        assertEquals(11, SettlementStepCatalog.ALL.size)
        assertTrue(SettlementStepCatalog.ALL.all { it.summary.isNotBlank() })
    }
}
