package io.kdmtracker.core.rules

import io.kdmtracker.core.domain.StepType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
    fun byNumberReturnsStepSevenTitledDevelop() {
        assertEquals("Develop", SettlementStepCatalog.byNumber(7).title)
    }

    @Test
    fun byNumberThrowsForUnknownNumber() {
        assertFailsWith<NoSuchElementException> {
            SettlementStepCatalog.byNumber(12)
        }
    }
}
