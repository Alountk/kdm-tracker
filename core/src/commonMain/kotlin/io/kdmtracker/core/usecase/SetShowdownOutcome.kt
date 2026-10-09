package io.kdmtracker.core.usecase

import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.domain.ShowdownStatus
import io.kdmtracker.core.rules.StepResult

/**
 * Records the Showdown's outcome.
 *
 * Only a terminal status is accepted ([ShowdownStatus.ACTIVE] is refused), and the transition must
 * start from [ShowdownStatus.ACTIVE] so a re-entry cannot advance the phase twice.
 */
object SetShowdownOutcome {
    /** Returns [StepResult.Ok] with the status set, or [StepResult.Blocked]. */
    fun run(
        record: SettlementRecord,
        status: ShowdownStatus,
    ): StepResult<SettlementRecord> {
        if (status == ShowdownStatus.ACTIVE) {
            return StepResult.Blocked("invalid outcome")
        }
        if (record.state.showdown.status != ShowdownStatus.ACTIVE) {
            return StepResult.Blocked("showdown is over")
        }
        return StepResult.Ok(
            record.copy(state = record.state.copy(showdown = record.state.showdown.copy(status = status))),
        )
    }
}
