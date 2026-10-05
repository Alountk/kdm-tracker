package io.kdmtracker.core.rules

import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.ruleset.Core16Ruleset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LanternYearFlowTest {
    private val context = FlowContext(Core16Ruleset)

    @Test
    fun forwardNavigationAdvancesOneStepAtATime() {
        for (step in 1 until SettlementStepCatalog.TOTAL) {
            assertEquals(
                FlowTransition.Advance(PhaseStep(SettlementPhase.SETTLEMENT, step + 1)),
                LanternYearFlow.next(PhaseStep(SettlementPhase.SETTLEMENT, step), context),
                "expected settlement step $step to advance to ${step + 1}",
            )
        }
    }

    @Test
    fun finalSettlementStepCommitsUnspentEndeavorsAndProceedsToHunt() {
        assertEquals(
            FlowTransition.Commit(PhaseStep(SettlementPhase.HUNT, 1), 5),
            LanternYearFlow.next(
                PhaseStep(SettlementPhase.SETTLEMENT, SettlementStepCatalog.TOTAL),
                FlowContext(Core16Ruleset, unspentEndeavors = 5),
            ),
        )
    }

    @Test
    fun finalSettlementStepProceedsToShowdownInNemesisYear() {
        assertEquals(
            FlowTransition.Commit(PhaseStep(SettlementPhase.SHOWDOWN, 1), 5),
            LanternYearFlow.next(
                PhaseStep(SettlementPhase.SETTLEMENT, SettlementStepCatalog.TOTAL),
                FlowContext(Core16Ruleset, isNemesisYear = true, unspentEndeavors = 5),
            ),
        )
    }

    @Test
    fun cannotSkipForwardToStepEightFromStepSix() {
        assertFalse(
            LanternYearFlow.canEnterStep(
                8,
                PhaseStep(SettlementPhase.SETTLEMENT, 6),
                context,
            ),
        )
    }

    @Test
    fun canEnterStepEightFromStepSeven() {
        assertTrue(
            LanternYearFlow.canEnterStep(
                8,
                PhaseStep(SettlementPhase.SETTLEMENT, 7),
                context,
            ),
        )
    }
}
