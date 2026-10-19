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
 */
object AdvanceHunt {
    /**
     * Returns [StepResult.Ok] with the party one space ahead (and a drawn card when [deck] is
     * non-empty), or [StepResult.Blocked].
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
        val next = minOf(hunt.position + 1, hunt.spaces.lastIndex)
        val drawn = deck.randomOrNull(random)
        return StepResult.Ok(
            record.copy(
                state =
                    record.state.copy(
                        hunt =
                            hunt.copy(
                                position = next,
                                drawnEvent =
                                    drawn ?: hunt.drawnEvent,
                            ),
                    ),
            ),
        )
    }
}
