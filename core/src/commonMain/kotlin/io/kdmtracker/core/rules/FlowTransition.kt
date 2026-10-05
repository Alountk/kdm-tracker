package io.kdmtracker.core.rules

/** The outcome of asking the lantern-year flow to move on from the current [PhaseStep]. */
sealed interface FlowTransition {
    /** Move forward one step within the current phase. */
    data class Advance(
        val to: PhaseStep,
    ) : FlowTransition

    /** Jump back to an earlier step, for example the special-showdown loop. */
    data class LoopBack(
        val to: PhaseStep,
    ) : FlowTransition

    /** Finish the settlement phase, discarding [discardedEndeavors] on the way out. */
    data class Commit(
        val to: PhaseStep,
        val discardedEndeavors: Int,
    ) : FlowTransition

    /** The requested move is not legal; [reason] explains why. */
    data class Blocked(
        val reason: String,
    ) : FlowTransition
}
