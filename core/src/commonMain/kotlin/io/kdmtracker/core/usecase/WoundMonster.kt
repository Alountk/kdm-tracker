package io.kdmtracker.core.usecase

import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.domain.ShowdownStatus
import io.kdmtracker.core.rules.StepResult

/**
 * Wounds the monster: the top AI card is removed to the wound stack (the monster's health).
 *
 * When the AI deck is empty, the discard is reshuffled into it first; wounding with no cards left
 * (deck and discard both empty) is refused — the monster is already dead.
 */
object WoundMonster {
    /** Returns [StepResult.Ok] with the card wounded, or [StepResult.Blocked]. */
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
        val wounded = deck.first()
        return StepResult.Ok(
            record.copy(
                state =
                    record.state.copy(
                        showdown =
                            showdown.copy(
                                aiDeck = deck.drop(1),
                                discard = discard,
                                wounds = showdown.wounds + wounded,
                            ),
                    ),
            ),
        )
    }
}
