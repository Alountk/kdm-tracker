package io.kdmtracker.app

import io.kdmtracker.core.domain.HuntStatus
import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.domain.SurvivorStatus
import io.kdmtracker.core.persistence.InMemoryCampaignStore
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Edge cases where the lantern-year controller could let a step move happen outside the phase or
 * state it belongs to. Each test states a guard that must hold.
 */
class PhaseGuardsTest {
    private val controller = LanternYearController(SettlementRepository(InMemoryCampaignStore()))

    private val state get() = controller.state.value

    private fun commitToTheHunt() {
        while (state.currentStep < 11) controller.flow.advance()
        controller.survivors.markDeparting(state.survivors.map { it.id }.take(4))
        controller.flow.advance()
        assertEquals(SettlementPhase.HUNT, state.phase)
    }

    @Test
    fun advancingWhileTheHuntIsActiveDoesNotSkipIt() {
        commitToTheHunt()
        assertEquals(HuntStatus.ACTIVE, state.hunt?.status)

        controller.flow.advance()

        assertEquals(SettlementPhase.HUNT, state.phase)
    }

    @Test
    fun theSettlementStepChipsDoNothingOutsideTheSettlementPhase() {
        commitToTheHunt()

        controller.flow.selectStep(2)

        assertEquals(1, state.currentStep)
    }

    @Test
    fun theSettlementDoesNotCommitToTheHuntWithoutADepartingSurvivor() {
        while (state.currentStep < 11) controller.flow.advance()
        assertEquals(0, state.survivors.count { it.status == SurvivorStatus.DEPARTING })

        controller.flow.advance()

        assertEquals(SettlementPhase.SETTLEMENT, state.phase)
        assertEquals(NO_PARTY, state.actionError)
    }
}
