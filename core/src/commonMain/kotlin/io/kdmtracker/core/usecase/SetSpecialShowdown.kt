package io.kdmtracker.core.usecase

import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.rules.StepResult

/**
 * Declares (or clears) a Special Showdown for the current Settlement Phase year.
 *
 * A declared Special Showdown makes Settlement step 9 run an encounter and loop back to the
 * death-count step ([io.kdmtracker.core.rules.LanternYearFlow]).
 */
object SetSpecialShowdown {
    /** Returns [StepResult.Ok] with the flag set to [enabled]. */
    fun run(
        record: SettlementRecord,
        enabled: Boolean,
    ): StepResult<SettlementRecord> = StepResult.Ok(record.copy(state = record.state.copy(specialShowdown = enabled)))
}
