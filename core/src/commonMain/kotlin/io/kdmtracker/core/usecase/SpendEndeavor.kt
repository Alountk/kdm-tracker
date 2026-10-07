package io.kdmtracker.core.usecase

import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.rules.StepResult

/**
 * Spends endeavors from a settlement's pool for a Settlement Phase action.
 *
 * Many actions cost one or more endeavors (innovating, augury, hunting the phoenix and
 * so on). This use case enforces the single shared rule — a settlement may never spend
 * more endeavors than it holds — as one transaction: the guard runs before any state
 * change, so a refusal leaves [record] untouched. It reads and writes only the endeavor
 * scalar; it carries no action-specific cost literal, so callers pass the price in.
 */
object SpendEndeavor {
    /**
     * Returns [StepResult.Ok] with [record]'s endeavor pool decremented by [count], or
     * [StepResult.Blocked] with `"no endeavors"` when the pool cannot cover [count].
     *
     * @param count the number of endeavors to spend; defaults to a single endeavor.
     */
    fun run(
        record: SettlementRecord,
        count: Int = 1,
    ): StepResult<SettlementRecord> {
        if (record.settlement.endeavors < count) {
            return StepResult.Blocked("no endeavors")
        }

        return StepResult.Ok(
            record.copy(
                settlement = record.settlement.copy(endeavors = record.settlement.endeavors - count),
            ),
        )
    }
}
