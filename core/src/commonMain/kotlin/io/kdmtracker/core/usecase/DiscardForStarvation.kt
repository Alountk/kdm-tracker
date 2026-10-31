package io.kdmtracker.core.usecase

import io.kdmtracker.core.domain.ResourceEntry
import io.kdmtracker.core.domain.ResourceId
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.rules.StepResult

/**
 * Pays one unit of the starvation debt with a resource the player chose.
 *
 * One transaction: the resource is spent through [AddResourceEntry] (so an absent resource is refused
 * and the ledger never goes negative) and the owed amount drops by one. Nothing owed is refused.
 */
object DiscardForStarvation {
    /**
     * Returns [StepResult.Ok] with one [id] discarded and one less owed, or [StepResult.Blocked] with
     * `"no starvation owed"` when the debt is paid, or the ledger's refusal when [id] is not stored.
     */
    fun run(
        record: SettlementRecord,
        id: ResourceId,
    ): StepResult<SettlementRecord> {
        if (record.state.starvationOwed < 1) {
            return StepResult.Blocked("no starvation owed")
        }
        return when (val spent = AddResourceEntry.run(record, ResourceEntry(id, -1))) {
            is StepResult.Ok ->
                StepResult.Ok(
                    spent.value.copy(
                        state = spent.value.state.copy(starvationOwed = record.state.starvationOwed - 1),
                    ),
                )
            is StepResult.Blocked -> spent
        }
    }
}
