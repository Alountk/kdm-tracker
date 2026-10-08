package io.kdmtracker.core.usecase

import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.rules.FlowContext
import io.kdmtracker.core.rules.FlowTransition
import io.kdmtracker.core.rules.LanternYearFlow
import io.kdmtracker.core.rules.PhaseStep

/**
 * Advances a settlement one position in the lantern-year flow.
 *
 * `Settlement.endeavors` is the current *unspent* pool for the lantern year, so this use
 * case never rewrites it from the returnee count. The move preserves that pool except at a
 * year boundary: a [FlowTransition.Commit] out of the Settlement Phase discards the pool
 * (the commit already carries the exact count being discarded), and any transition landing
 * on Settlement step 1 starts the new year empty — endeavors are not earned until the
 * Gains Endeavors step. At the same boundary the drawn [Settlement.settlementEvent] is
 * cleared, since it belongs to the year that just ended and step 4 records the next one.
 * [io.kdmtracker.core.rules.recompute] stays a pure helper for the future earn/spend ledger.
 */
object AdvanceStep {
    /** The resulting settlement together with the transition that produced it. */
    data class Result(
        val settlement: Settlement,
        val transition: FlowTransition,
    )

    fun run(
        settlement: Settlement,
        returnedSurvivors: Int,
        context: FlowContext,
    ): Result {
        val effectiveContext =
            context.copy(
                returnedSurvivors = returnedSurvivors,
                unspentEndeavors = settlement.endeavors,
            )
        val transition =
            LanternYearFlow.next(
                PhaseStep(settlement.phase, settlement.currentStep),
                effectiveContext,
            )
        return Result(
            settlement = applyTransition(settlement, transition),
            transition = transition,
        )
    }

    private fun applyTransition(
        settlement: Settlement,
        transition: FlowTransition,
    ): Settlement {
        val to = transition.target() ?: return settlement
        val moved = settlement.copy(phase = to.phase, currentStep = to.step)
        val discardsEndeavors =
            transition is FlowTransition.Commit ||
                to == PhaseStep(SettlementPhase.SETTLEMENT, 1)
        return if (discardsEndeavors) {
            moved.copy(endeavors = 0, settlementEvent = null)
        } else {
            moved
        }
    }

    private fun FlowTransition.target(): PhaseStep? =
        when (this) {
            is FlowTransition.Advance -> to
            is FlowTransition.LoopBack -> to
            is FlowTransition.Commit -> to
            is FlowTransition.Blocked -> null
        }
}
