package io.kdmtracker.core.rules

/**
 * The typed outcome of running a settlement step.
 *
 * Use cases return this instead of throwing so callers can branch on the outcome
 * without exception control flow. A step either completed with a [value] ([Ok]) or
 * was refused with a human-readable [reason] ([Blocked]).
 *
 * @param T the value a successful step produces; [Blocked] carries none.
 */
sealed interface StepResult<out T> {
    /** The step completed, producing [value]. */
    data class Ok<T>(
        val value: T,
    ) : StepResult<T>

    /** The step could not run; [reason] explains why. */
    data class Blocked(
        val reason: String,
    ) : StepResult<Nothing>
}
