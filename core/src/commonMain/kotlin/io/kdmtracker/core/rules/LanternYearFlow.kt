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
 * This task implements only forward navigation through the Settlement phase's steps.
 * The special-showdown loop, hunt and showdown branches arrive in a later task; here
 * [next] returns a safe [FlowTransition.Blocked] for those unfinished branches instead
 * of crashing.
 */
object LanternYearFlow {
    fun next(
        current: PhaseStep,
        context: FlowContext,
    ): FlowTransition {
        if (current.phase != SettlementPhase.SETTLEMENT) {
            return FlowTransition.Blocked("phase ${current.phase} flow is not implemented yet")
        }

        val total = context.ruleset.settlementSteps.size
        return when {
            current.step in 1 until total ->
                FlowTransition.Advance(PhaseStep(SettlementPhase.SETTLEMENT, current.step + 1))

            current.step == total -> {
                val nextPhase =
                    if (context.isNemesisYear) SettlementPhase.SHOWDOWN else SettlementPhase.HUNT
                FlowTransition.Commit(PhaseStep(nextPhase, 1), context.unspentEndeavors)
            }

            else -> FlowTransition.Blocked("unknown settlement step ${current.step}")
        }
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
