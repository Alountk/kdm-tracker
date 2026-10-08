package io.kdmtracker.core.usecase

import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.domain.SurvivorStatus
import io.kdmtracker.core.rules.StepResult

/**
 * Records survivors lost during the Settlement Phase (step 5, Update the Death Count).
 *
 * One transaction: a positive [count] adds to the death counter and marks the first [count] living
 * survivors `DEAD`, so the derived population drops. A batch larger than the living count kills
 * everyone living but still records every death; a non-positive count is refused and leaves the
 * record untouched.
 */
object RecordDeaths {
    /**
     * Returns [StepResult.Ok] with the counter raised and [count] living survivors marked dead, or
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
        var remaining = count
        val marked =
            record.state.survivors.map { survivor ->
                if (remaining > 0 && survivor.status != SurvivorStatus.DEAD) {
                    remaining--
                    survivor.copy(status = SurvivorStatus.DEAD)
                } else {
                    survivor
                }
            }
        val withRoster = record.withSurvivors(marked)
        return StepResult.Ok(withRoster.copy(state = withRoster.state.copy(deaths = withRoster.state.deaths + count)))
    }
}
