package io.kdmtracker.core.usecase

import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.rules.StepResult

/**
 * Records survivors lost during the Settlement Phase (step 5, Update the Death Count).
 *
 * One transaction: a positive [count] adds to the death counter and lowers the settlement
 * population, floored at zero so a batch larger than the living population still records
 * every death without a negative population. A non-positive count is refused and leaves
 * the record untouched.
 */
object RecordDeaths {
    /**
     * Returns [StepResult.Ok] with the counter raised and the population lowered, or
     * [StepResult.Blocked] with `"invalid death count"` when [count] is not positive.
     *
     * @param count the number of deaths to record; must be a positive amount.
     */
    fun run(
        record: SettlementRecord,
        count: Int,
    ): StepResult<SettlementRecord> {
        if (count < 1) {
            return StepResult.Blocked("invalid death count")
        }
        return StepResult.Ok(
            record.copy(
                settlement =
                    record.settlement.copy(
                        population = (record.settlement.population - count).coerceAtLeast(0),
                    ),
                state = record.state.copy(deaths = record.state.deaths + count),
            ),
        )
    }
}
