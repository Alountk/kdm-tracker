package io.kdmtracker.core.usecase

import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.domain.ShowdownState
import io.kdmtracker.core.rules.StepResult

/**
 * Clears the current encounter, returning [SettlementState.showdown] to its empty state.
 *
 * Used when a Special Showdown concludes so the year's later phase Showdown sets up fresh instead
 * of reusing the resolved encounter.
 */
object ClearShowdown {
    /** Returns [StepResult.Ok] with the encounter reset. */
    fun run(record: SettlementRecord): StepResult<SettlementRecord> =
        StepResult.Ok(record.copy(state = record.state.copy(showdown = ShowdownState())))
}
