package io.kdmtracker.core.usecase

import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.domain.ShowdownStatus
import io.kdmtracker.core.rules.StepResult

/**
 * Draws the monster's turn: the top AI card moves to the discard and becomes the last drawn card.
 *
 * When the AI deck is empty, the discard is reshuffled into it first; drawing with no cards left
 * (deck and discard both empty) is refused.
 */
object DrawShowdownCard {
    /** Returns [StepResult.Ok] with the card drawn, or [StepResult.Blocked]. */
    fun run(record: SettlementRecord): StepResult<SettlementRecord> {
        val showdown = record.state.showdown
        if (showdown.status != ShowdownStatus.ACTIVE) {
            return StepResult.Blocked("showdown is over")
        }
        var deck = showdown.aiDeck
        var discard = showdown.discard
        if (deck.isEmpty()) {
            if (discard.isEmpty()) {
                return StepResult.Blocked("no ai cards left")
            }
            deck = discard
            discard = emptyList()
        }
        val drawn = deck.first()
        return StepResult.Ok(
            record.copy(
                state =
                    record.state.copy(
                        showdown =
                            showdown.copy(
                                aiDeck = deck.drop(1),
                                discard = discard + drawn,
                                lastDrawn = drawn,
                            ),
                    ),
            ),
        )
    }
}
