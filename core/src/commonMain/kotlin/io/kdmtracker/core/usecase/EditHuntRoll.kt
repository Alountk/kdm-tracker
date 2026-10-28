package io.kdmtracker.core.usecase

import io.kdmtracker.core.domain.HuntRoll
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.rules.StepResult

/**
 * Lets the player overwrite a step's stored hunt roll by hand (`roll_edit`).
 *
 * The roll is per-step: [index] selects the board space whose roll is edited, and [roll] must be a
 * valid d100 value ([HuntRoll.VALID], `00` stored as 100). The edit persists on that step, so moving back shows the player's own
 * number. Out-of-range values are [StepResult.Blocked].
 */
object EditHuntRoll {
    /** Returns [StepResult.Ok] with the step's roll set to [roll], or [StepResult.Blocked] when out of range. */
    fun run(
        record: SettlementRecord,
        index: Int,
        roll: Int,
    ): StepResult<SettlementRecord> {
        if (roll !in HuntRoll.VALID) {
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
