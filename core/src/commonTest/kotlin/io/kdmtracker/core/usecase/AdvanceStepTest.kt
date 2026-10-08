package io.kdmtracker.core.usecase

import io.kdmtracker.core.domain.HybridLogicalClock
import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.domain.SyncMeta
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.rules.FlowContext
import io.kdmtracker.core.rules.FlowTransition
import io.kdmtracker.core.rules.PhaseStep
import io.kdmtracker.core.ruleset.Core16Ruleset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class AdvanceStepTest {
    private val id = Ulid.generate(1L) { 0 }
    private val campaignId = Ulid.generate(2L) { 0 }
    private val context = FlowContext(Core16Ruleset)

    private fun settlement(
        phase: SettlementPhase,
        step: Int,
        endeavors: Int,
    ): Settlement =
        Settlement(
            id = id,
            campaignId = campaignId,
            name = "Kingsport",
            lanternYear = 1,
            phase = phase,
            currentStep = step,
            endeavors = endeavors,
            population = 4,
            settlementEvent = null,
            sync = SyncMeta(HybridLogicalClock(0, 0)),
        )

    @Test
    fun advancingWithinTheSettlementPhasePreservesTheUnspentPool() {
        val result = AdvanceStep.run(settlement(SettlementPhase.SETTLEMENT, 7, endeavors = 4), 3, context)

        assertEquals(
            FlowTransition.Advance(PhaseStep(SettlementPhase.SETTLEMENT, 8)),
            result.transition,
        )
        assertEquals(8, result.settlement.currentStep)
        assertEquals(SettlementPhase.SETTLEMENT, result.settlement.phase)
        assertEquals(4, result.settlement.endeavors)
    }

    @Test
    fun committingFromFinalStepDiscardsTheUnspentPoolAndMovesToHunt() {
        val result = AdvanceStep.run(settlement(SettlementPhase.SETTLEMENT, 11, endeavors = 5), 3, context)

        val commit = assertIs<FlowTransition.Commit>(result.transition)
        assertEquals(5, commit.discardedEndeavors)
        assertEquals(PhaseStep(SettlementPhase.HUNT, 1), commit.to)
        assertEquals(SettlementPhase.HUNT, result.settlement.phase)
        assertEquals(1, result.settlement.currentStep)
        assertEquals(0, result.settlement.endeavors)
    }

    @Test
    fun failedHuntStartsTheNewYearWithAnEmptyPool() {
        val result =
            AdvanceStep.run(
                settlement(SettlementPhase.HUNT, 1, endeavors = 7),
                0,
                context.copy(huntFailed = true),
            )

        assertEquals(
            FlowTransition.Advance(PhaseStep(SettlementPhase.SETTLEMENT, 1)),
            result.transition,
        )
        assertEquals(SettlementPhase.SETTLEMENT, result.settlement.phase)
        assertEquals(1, result.settlement.currentStep)
        assertEquals(0, result.settlement.endeavors)
    }

    @Test
    fun committingClearsTheYearsSettlementEvent() {
        val source = settlement(SettlementPhase.SETTLEMENT, 11, endeavors = 0).copy(settlementEvent = "Rivalidad")

        val result = AdvanceStep.run(source, 3, context)

        assertEquals(SettlementPhase.HUNT, result.settlement.phase)
        assertNull(result.settlement.settlementEvent)
    }

    @Test
    fun landingOnTheNewYearClearsTheYearsSettlementEvent() {
        val source = settlement(SettlementPhase.HUNT, 1, endeavors = 0).copy(settlementEvent = "Rivalidad")

        val result = AdvanceStep.run(source, 0, context.copy(huntFailed = true))

        assertEquals(SettlementPhase.SETTLEMENT, result.settlement.phase)
        assertEquals(1, result.settlement.currentStep)
        assertNull(result.settlement.settlementEvent)
    }
}
