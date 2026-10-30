package io.kdmtracker.core.usecase

import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.domain.ShowdownStatus
import io.kdmtracker.core.rules.StepResult

/**
 * Undoes the last wound: the most recent card in the wound stack goes back on top of the AI deck.
 *
 * Refused when there is no wound to return or the showdown is already over.
 */
object HealMonster {
    /** Returns [StepResult.Ok] with the wound returned, or [StepResult.Blocked]. */
    fun run(record: SettlementRecord): StepResult<SettlementRecord> {
        val showdown = record.state.showdown
        if (showdown.status != ShowdownStatus.ACTIVE) {
            return StepResult.Blocked("showdown is over")
        }
        val healed = showdown.wounds.lastOrNull() ?: return StepResult.Blocked("no wounds to heal")
        return StepResult.Ok(
            record.copy(
                state =
                    record.state.copy(
                        showdown =
                            showdown.copy(
                                aiDeck = listOf(healed) + showdown.aiDeck,
                                wounds = showdown.wounds.dropLast(1),
                            ),
                    ),
            ),
        )
    }
}
