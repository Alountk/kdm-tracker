package io.kdmtracker.core.usecase

import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.rules.StepResult

/**
 * Lets the player overwrite a step's stored 2d10 roll by hand (`roll_edit`).
 *
 * The roll is per-step: [index] selects the board space whose roll is edited, and [roll] must be a
 * valid 2d10 sum (2..20). The edit persists on that step, so moving back shows the player's own
 * number. Out-of-range values are [StepResult.Blocked].
 */
object EditHuntRoll {
    /** Returns [StepResult.Ok] with the step's roll set to [roll], or [StepResult.Blocked] when out of range. */
    fun run(
        record: SettlementRecord,
        index: Int,
        roll: Int,
    ): StepResult<SettlementRecord> {
        if (roll !in 2..20) {
            return StepResult.Blocked("roll out of range")
        }
        val hunt = record.state.hunt
        val spaces = hunt.spaces
        if (index !in spaces.indices) {
            return StepResult.Blocked("no such step")
        }
        val updated = spaces.mapIndexed { i, space -> if (i == index) space.copy(roll = roll) else space }
        return StepResult.Ok(record.copy(state = record.state.copy(hunt = hunt.copy(spaces = updated))))
    }
}
