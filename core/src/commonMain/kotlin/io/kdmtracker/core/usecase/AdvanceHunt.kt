package io.kdmtracker.core.usecase

import io.kdmtracker.core.domain.HuntEvent
import io.kdmtracker.core.domain.HuntStatus
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.rules.StepResult
import kotlin.random.Random

/**
 * Advances the hunt party one space toward the monster, drawing a random hunt-event card.
 *
 * Blocks when the board is not set up or the hunt is already over; the position is capped at the
 * last space. When a non-empty [deck] is supplied, one card is drawn at random and stored as the
 * hunt's active [HuntState.drawnEvent] (S5) — the player resolves it by hand. With the bare
 * signature (no deck) nothing is drawn, so existing callers are unchanged.
 *
 * **S7 gate:** while a drawn card is not yet marked resolved on the party's current space, the
 * party cannot step forward — [StepResult.Blocked]. Resolving it (assign + resolve on that space)
 * lifts the gate. No drawn card means no gate.
 */
object AdvanceHunt {
    /**
     * Returns [StepResult.Ok] with the party one space ahead (and a drawn card when [deck] is
     * non-empty), or [StepResult.Blocked] — including when the active event card is unresolved (S7).
     */
    fun run(
        record: SettlementRecord,
        deck: List<HuntEvent> = emptyList(),
        random: Random = Random.Default,
    ): StepResult<SettlementRecord> {
        val hunt = record.state.hunt
        if (hunt.spaces.isEmpty()) {
            return StepResult.Blocked("hunt not set up")
        }
        if (hunt.status != HuntStatus.ACTIVE) {
            return StepResult.Blocked("hunt is over")
        }
        // S7: a drawn card must be resolved on the party's current space before advancing.
        val drawn = hunt.drawnEvent
        if (drawn != null) {
            val current =
                hunt.spaces.getOrElse(hunt.position) {
                    io.kdmtracker.core.domain
                        .HuntSpace()
                }
            if (drawn.name !in current.resolved) {
                return StepResult.Blocked("event not resolved")
            }
        }
        val next = minOf(hunt.position + 1, hunt.spaces.lastIndex)
        val newDrawn = deck.randomOrNull(random)
        return StepResult.Ok(
            record.copy(
                state =
                    record.state.copy(
                        hunt =
                            hunt.copy(
                                position = next,
                                drawnEvent = newDrawn ?: hunt.drawnEvent,
                            ),
                    ),
            ),
        )
    }
}
