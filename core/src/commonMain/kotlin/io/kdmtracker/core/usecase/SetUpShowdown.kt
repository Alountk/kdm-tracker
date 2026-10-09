package io.kdmtracker.core.usecase

import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.domain.ShowdownState
import io.kdmtracker.core.rules.StepResult

/**
 * Sets up the Showdown encounter with a monster, its level and the assembled AI deck.
 *
 * The deck is provided by the caller (the level assembly is content the app does not model); it
 * must be non-empty.
 */
object SetUpShowdown {
    /** Returns [StepResult.Ok] with the encounter set, or [StepResult.Blocked]. */
    fun run(
        record: SettlementRecord,
        monster: String,
        level: Int,
        deck: List<String>,
    ): StepResult<SettlementRecord> {
        if (monster.isBlank()) {
            return StepResult.Blocked("unknown monster")
        }
        if (level !in 1..3) {
            return StepResult.Blocked("invalid level")
        }
        if (deck.isEmpty()) {
            return StepResult.Blocked("empty ai deck")
        }
        val showdown = ShowdownState(monster = monster.trim(), level = level, aiDeck = deck)
        return StepResult.Ok(record.copy(state = record.state.copy(showdown = showdown)))
    }
}
