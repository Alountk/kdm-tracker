package io.kdmtracker

import io.kdmtracker.domain.SettlementSteps
import io.kdmtracker.domain.StepType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SettlementStepsTests {
    @Test
    fun tableHasElevenEntriesInOfficialOrderWithUniqueNumbers() {
        assertEquals(11, SettlementSteps.TOTAL)
        assertEquals(11, SettlementSteps.ALL.size)
        assertEquals((1..11).toList(), SettlementSteps.ALL.map { it.number })
        assertEquals(
            SettlementSteps.ALL.size,
            SettlementSteps.ALL
                .map { it.number }
                .distinct()
                .size,
        )
    }

    @Test
    fun typesAndContentFollowTheResearchClassification() {
        val expected =
            mapOf(
                1 to StepType.BOOKKEEPING,
                2 to StepType.DECISION,
                3 to StepType.BOOKKEEPING,
                4 to StepType.BRANCHING,
                5 to StepType.BRANCHING,
                6 to StepType.BRANCHING,
                7 to StepType.DECISION,
                8 to StepType.DECISION,
                9 to StepType.BRANCHING,
                10 to StepType.BOOKKEEPING,
                11 to StepType.BOOKKEEPING,
            )
        expected.forEach { (number, type) ->
            assertEquals(type, SettlementSteps.byNumber(number).type, "step $number type")
        }
        SettlementSteps.ALL.forEach { step ->
            assertTrue(step.title.isNotBlank(), "step ${step.number} title")
            assertTrue(step.summary.isNotBlank(), "step ${step.number} summary")
        }
    }
}
