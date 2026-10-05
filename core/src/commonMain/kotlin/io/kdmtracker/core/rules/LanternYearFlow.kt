package io.kdmtracker.core.rules

import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.ruleset.Ruleset

/**
 * Extra state the flow needs to decide a transition without owning game state itself.
 *
 * All fields are declared up front so later tasks can consume them without changing
 * this signature.
 */
data class FlowContext(
    val ruleset: Ruleset,
    val isNemesisYear: Boolean = false,
    val hasSpecialShowdown: Boolean = false,
    val huntFailed: Boolean = false,
    val returnedSurvivors: Int = 0,
    val unspentEndeavors: Int = 0,
)

/**
 * The lantern-year phase/step state machine.
 *
 * Forward navigation moves one Settlement step at a time; the last Settlement step
 * commits to the Hunt (or straight to the Showdown on a nemesis year). The branch and
 * loop semantics come from spec §7: the Special Showdown loops back to the death-count
 * step, a failed Hunt restarts the settlement year, and the Showdown Aftermath resolves
 * into the next settlement year.
 *
 * [FlowTransition.Blocked] is reserved for genuinely invalid input (a Settlement step
 * outside `1..total`), never for a legitimate phase transition.
 */
object LanternYearFlow {
    fun next(
        current: PhaseStep,
        context: FlowContext,
    ): FlowTransition =
        when (current.phase) {
            SettlementPhase.SETTLEMENT -> nextSettlementStep(current.step, context)
            SettlementPhase.HUNT -> nextHuntStep(context)
            SettlementPhase.SHOWDOWN ->
                FlowTransition.Advance(PhaseStep(SettlementPhase.SETTLEMENT, 1))
        }

    private fun nextSettlementStep(
        step: Int,
        context: FlowContext,
    ): FlowTransition {
        val total = context.ruleset.settlementSteps.size
        if (step !in 1..total) {
            return FlowTransition.Blocked("unknown settlement step $step")
        }
        return when {
            step == context.ruleset.specialShowdownStep && context.hasSpecialShowdown ->
                FlowTransition.LoopBack(PhaseStep(SettlementPhase.SETTLEMENT, context.ruleset.deathCountStep))

            step < total ->
                FlowTransition.Advance(PhaseStep(SettlementPhase.SETTLEMENT, step + 1))

            else -> {
                val nextPhase =
                    if (context.isNemesisYear) SettlementPhase.SHOWDOWN else SettlementPhase.HUNT
                FlowTransition.Commit(PhaseStep(nextPhase, 1), context.unspentEndeavors)
            }
        }
    }

    private fun nextHuntStep(context: FlowContext): FlowTransition {
        val nextPhase =
            if (context.huntFailed) SettlementPhase.SETTLEMENT else SettlementPhase.SHOWDOWN
        return FlowTransition.Advance(PhaseStep(nextPhase, 1))
    }

    /**
     * Whether [step] can be entered from [from] in [context].
     *
     * Within a phase a step may not be skipped forward: anything past [from]` + 1`
     * is not enterable, while revisiting [from] or any earlier step is allowed.
     */
    fun canEnterStep(
        step: Int,
        from: PhaseStep,
        context: FlowContext,
    ): Boolean {
        val total = context.ruleset.settlementSteps.size
        if (step !in 1..total) return false
        return step <= from.step + 1
    }
}
