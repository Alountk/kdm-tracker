package io.kdmtracker.domain

/**
 * Outcome of a hunt phase run (research §3): the hunt ends by finding the quarry,
 * by ambush, by failing (all lost) or by running away (Pictographs).
 *
 * [ACTIVE] is the only non-terminal state — exactly one per settlement, enforced in code.
 */
enum class HuntStatus(
    val label: String,
) {
    ACTIVE("in the hunt"),
    QUARRY_FOUND("quarry found"),
    AMBUSH("ambush"),
    FAILED("hunt failed"),
    RUN_AWAY("ran away"),
}
