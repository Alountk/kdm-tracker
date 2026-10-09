package io.kdmtracker.core.usecase

import io.kdmtracker.core.domain.HuntStatus
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.rules.StepResult

/**
 * Advances the hunt party one space toward the monster.
 *
 * Blocks when the board is not set up or the hunt is already over; the position is capped at the
 * last space.
 */
object AdvanceHunt {
    /** Returns [StepResult.Ok] with the party one space ahead, or [StepResult.Blocked]. */
    fun run(record: SettlementRecord): StepResult<SettlementRecord> {
        val hunt = record.state.hunt
        if (hunt.spaces.isEmpty()) {
            return StepResult.Blocked("hunt not set up")
        }
        if (hunt.status != HuntStatus.ACTIVE) {
            return StepResult.Blocked("hunt is over")
        }
        val next = minOf(hunt.position + 1, hunt.spaces.lastIndex)
        return StepResult.Ok(record.copy(state = record.state.copy(hunt = hunt.copy(position = next))))
    }
}
