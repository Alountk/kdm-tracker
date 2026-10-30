package io.kdmtracker.app

import io.kdmtracker.core.domain.HuntStatus
import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.domain.SurvivorStatus
import io.kdmtracker.core.persistence.InMemoryCampaignStore
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Edge cases where the lantern-year controller lets a step move happen outside the phase or state it
 * belongs to. Each test states the guard that should hold; they run once the guard exists. The
 * reasons are recorded in `docs/backlog.md`.
 */
class PhaseGuardGapsTest {
    private val controller = LanternYearController(SettlementRepository(InMemoryCampaignStore()))

    private val state get() = controller.state.value

    private fun commitToTheHunt() {
        while (state.currentStep < 11) controller.flow.advance()
        controller.survivors.markDeparting(state.survivors.map { it.id }.take(4))
        controller.flow.advance()
        assertEquals(SettlementPhase.HUNT, state.phase)
    }

    @Ignore("backlog: advance() has no phase guard, so a second tap at step 11 skips the unresolved hunt")
    @Test
    fun advancingWhileTheHuntIsActiveDoesNotSkipIt() {
        commitToTheHunt()
        assertEquals(HuntStatus.ACTIVE, state.hunt?.status)

        controller.flow.advance()

        assertEquals(SettlementPhase.HUNT, state.phase)
    }

    @Ignore("backlog: selectStep() checks the step number but not the phase, so a chip moves the hunt's step")
    @Test
    fun theSettlementStepChipsDoNothingOutsideTheSettlementPhase() {
        commitToTheHunt()

        controller.flow.selectStep(2)

        assertEquals(1, state.currentStep)
    }

    @Ignore("backlog: the commit out of step 11 does not require a departing survivor")
    @Test
    fun theSettlementDoesNotCommitToTheHuntWithoutADepartingSurvivor() {
        while (state.currentStep < 11) controller.flow.advance()
        assertEquals(0, state.survivors.count { it.status == SurvivorStatus.DEPARTING })

        controller.flow.advance()

        assertEquals(SettlementPhase.SETTLEMENT, state.phase)
    }
}
