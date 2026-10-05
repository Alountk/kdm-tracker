package io.kdmtracker.core.rules

import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.ruleset.Core16Ruleset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Branches, loops and gates of the lantern-year flow (spec §7).
 *
 * Each Review Focus branch has its own test:
 *  - Step 9 loops back to step 5 only on a Special Showdown year.
 *  - A nemesis year skips the Hunt Phase and commits straight to Showdown 1.
 *  - A failed Hunt restarts the settlement year at step 1; a successful one goes to Showdown 1.
 *  - The Showdown Aftermath resolves into the next settlement year.
 *  - The step-11 [FlowTransition.Commit] carries the exact unspent endeavor count.
 */
class LanternYearFlowBranchesTest {
    private val context = FlowContext(Core16Ruleset)

    private fun fromSettlement(
        step: Int,
        context: FlowContext = this.context,
    ): FlowTransition = LanternYearFlow.next(PhaseStep(SettlementPhase.SETTLEMENT, step), context)

    @Test
    fun specialShowdownLoopsBackToDeathCountStep() {
        assertEquals(
            FlowTransition.LoopBack(PhaseStep(SettlementPhase.SETTLEMENT, 5)),
            fromSettlement(9, FlowContext(Core16Ruleset, hasSpecialShowdown = true)),
        )
    }

    @Test
    fun stepNineAdvancesToTenWithoutASpecialShowdown() {
        assertEquals(
            FlowTransition.Advance(PhaseStep(SettlementPhase.SETTLEMENT, 10)),
            fromSettlement(9),
        )
    }

    @Test
    fun nemesisYearSkipsHuntAndCommitsToShowdown() {
        assertEquals(
            FlowTransition.Commit(PhaseStep(SettlementPhase.SHOWDOWN, 1), 5),
            fromSettlement(
                SettlementStepCatalog.TOTAL,
                FlowContext(Core16Ruleset, isNemesisYear = true, unspentEndeavors = 5),
            ),
        )
    }

    @Test
    fun failedHuntRestartsTheSettlementYear() {
        assertEquals(
            FlowTransition.Advance(PhaseStep(SettlementPhase.SETTLEMENT, 1)),
            LanternYearFlow.next(
                PhaseStep(SettlementPhase.HUNT, 1),
                FlowContext(Core16Ruleset, huntFailed = true, returnedSurvivors = 0),
            ),
        )
    }

    @Test
    fun successfulHuntProceedsToShowdown() {
        assertEquals(
            FlowTransition.Advance(PhaseStep(SettlementPhase.SHOWDOWN, 1)),
            LanternYearFlow.next(
                PhaseStep(SettlementPhase.HUNT, 1),
                FlowContext(Core16Ruleset, returnedSurvivors = 3),
            ),
        )
    }

    @Test
    fun showdownAftermathResolvesIntoTheNextSettlementYear() {
        assertEquals(
            FlowTransition.Advance(PhaseStep(SettlementPhase.SETTLEMENT, 1)),
            LanternYearFlow.next(PhaseStep(SettlementPhase.SHOWDOWN, 1), context),
        )
    }

    @Test
    fun finalSettlementStepCommitsTheExactUnspentEndeavorCount() {
        assertEquals(
            FlowTransition.Commit(PhaseStep(SettlementPhase.HUNT, 1), 5),
            fromSettlement(SettlementStepCatalog.TOTAL, FlowContext(Core16Ruleset, unspentEndeavors = 5)),
        )
    }

    @Test
    fun finalSettlementStepNeverSilentlyZeroesUnspentEndeavors() {
        val transition =
            fromSettlement(SettlementStepCatalog.TOTAL, FlowContext(Core16Ruleset, unspentEndeavors = 7))

        assertEquals(7, assertIs<FlowTransition.Commit>(transition).discardedEndeavors)
    }

    @Test
    fun settlementStepBelowOneIsBlocked() {
        assertIs<FlowTransition.Blocked>(fromSettlement(0))
    }

    @Test
    fun settlementStepAboveTotalIsBlocked() {
        assertIs<FlowTransition.Blocked>(fromSettlement(SettlementStepCatalog.TOTAL + 1))
    }
}
