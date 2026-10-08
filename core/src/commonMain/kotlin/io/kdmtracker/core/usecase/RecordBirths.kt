package io.kdmtracker.core.usecase

import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.rules.StepResult

/**
 * Records survivors born during the Settlement Phase (step 5's counter panel).
 *
 * One transaction: a positive [count] adds to the birth counter and raises the settlement
 * population. A non-positive count is refused and leaves the record untouched. Births
 * typically arrive from story events (Intimacy) and the New Life principle; this use case
 * only records the counter, it never invents the event.
 */
object RecordBirths {
    /**
     * Returns [StepResult.Ok] with the counter raised and the population raised, or
     * [StepResult.Blocked] with `"invalid birth count"` when [count] is not positive.
     *
     * @param count the number of births to record; must be a positive amount.
     */
    fun run(
        record: SettlementRecord,
        count: Int,
    ): StepResult<SettlementRecord> {
        if (count < 1) {
            return StepResult.Blocked("invalid birth count")
        }
        return StepResult.Ok(
            record.copy(
                settlement = record.settlement.copy(population = record.settlement.population + count),
                state = record.state.copy(births = record.state.births + count),
            ),
        )
    }
}
