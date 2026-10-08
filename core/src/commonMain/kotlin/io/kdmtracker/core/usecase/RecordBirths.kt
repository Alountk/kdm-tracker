package io.kdmtracker.core.usecase

import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.domain.Survivor
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.rules.StepResult

/**
 * Records survivors born during the Settlement Phase (step 5's counter panel).
 *
 * One transaction: a positive [count] adds to the birth counter and appends [count] new unnamed
 * survivors to the roster, so the derived population rises. The new ids come from [newId], keeping
 * this use case pure and deterministic in tests. A non-positive count is refused and leaves the
 * record untouched.
 */
object RecordBirths {
    /**
     * Returns [StepResult.Ok] with the counter raised and the newborns appended, or
     * [StepResult.Blocked] with `"invalid birth count"` when [count] is not positive.
     *
     * @param count the number of births to record; must be a positive amount.
     * @param newId supplies a fresh survivor id per newborn.
     */
    fun run(
        record: SettlementRecord,
        count: Int,
        newId: () -> Ulid,
    ): StepResult<SettlementRecord> {
        if (count < 1) {
            return StepResult.Blocked("invalid birth count")
        }
        val newborns = List(count) { Survivor(id = newId()) }
        val withRoster = record.withSurvivors(record.state.survivors + newborns)
        return StepResult.Ok(withRoster.copy(state = withRoster.state.copy(births = withRoster.state.births + count)))
    }
}
