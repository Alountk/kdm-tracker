package io.kdmtracker.core.usecase

import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.rules.FlowContext
import io.kdmtracker.core.rules.FlowTransition
import io.kdmtracker.core.rules.LanternYearFlow
import io.kdmtracker.core.rules.PhaseStep
import io.kdmtracker.core.rules.recompute

/**
 * Advances a settlement one position in the lantern-year flow and recomputes its derived values.
 *
 * The caller supplies the number of returning survivors; the settlement's own unspent
 * endeavors feed the transition, so a step-11 commit reports the exact count being
 * discarded. Derived scalars are recomputed after the move rather than carried forward.
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
        val advanced = applyTransition(settlement, transition)
        return Result(
            settlement = recompute(advanced, returnedSurvivors, context.ruleset),
            transition = transition,
        )
    }

    private fun applyTransition(
        settlement: Settlement,
        transition: FlowTransition,
    ): Settlement =
        when (transition) {
            is FlowTransition.Advance ->
                settlement.copy(phase = transition.to.phase, currentStep = transition.to.step)

            is FlowTransition.LoopBack ->
                settlement.copy(phase = transition.to.phase, currentStep = transition.to.step)

            is FlowTransition.Commit ->
                settlement.copy(phase = transition.to.phase, currentStep = transition.to.step)

            is FlowTransition.Blocked -> settlement
        }
}
