package io.kdmtracker.core.usecase

import io.kdmtracker.core.content.ContentCatalog
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.rules.StepResult

/**
 * Records the Settlement Event drawn at Settlement Phase step 4 (Update Timeline).
 *
 * The name must be one the [catalog] ships (the deck is content); an unknown name is refused
 * and leaves the record untouched. The event's text is displayed, never evaluated.
 */
object RecordSettlementEvent {
    /**
     * Returns [StepResult.Ok] with the event recorded on the settlement, or [StepResult.Blocked]
     * with `"unknown settlement event <name>"` when [name] is not in the catalog.
     */
    fun run(
        record: SettlementRecord,
        name: String,
        catalog: ContentCatalog,
    ): StepResult<SettlementRecord> {
        if (name !in catalog.settlementEvents) {
            return StepResult.Blocked("unknown settlement event $name")
        }
        return StepResult.Ok(
            record.copy(settlement = record.settlement.copy(settlementEvent = name)),
        )
    }
}
