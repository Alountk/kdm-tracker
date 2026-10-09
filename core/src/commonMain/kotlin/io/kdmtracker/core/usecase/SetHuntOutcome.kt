package io.kdmtracker.core.usecase

import io.kdmtracker.core.domain.HuntStatus
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.rules.StepResult

/**
 * Records the hunt's outcome.
 *
 * Only a terminal status is accepted ([HuntStatus.ACTIVE] is refused): quarry found, ambush, failed
 * (a starvation or party wipe), or run away. The transition itself is driven by the caller.
 */
object SetHuntOutcome {
    /** Returns [StepResult.Ok] with the status set, or [StepResult.Blocked] with `"invalid outcome"`. */
    fun run(
        record: SettlementRecord,
        status: HuntStatus,
    ): StepResult<SettlementRecord> {
        if (status == HuntStatus.ACTIVE) {
            return StepResult.Blocked("invalid outcome")
        }
        return StepResult.Ok(record.copy(state = record.state.copy(hunt = record.state.hunt.copy(status = status))))
    }
}
