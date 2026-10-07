package io.kdmtracker.core.usecase

import io.kdmtracker.core.domain.ResourceEntry
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.domain.Storage
import io.kdmtracker.core.rules.StepResult

/**
 * Appends one resource entry to the settlement ledger, refusing spends the ledger cannot cover.
 *
 * A gain (`delta >= 0`) is always accepted. A spend (`delta < 0`) is accepted only when the
 * resource's projected net would stay non-negative, so the ledger never records an
 * overdraw; the refusal is a [StepResult.Blocked] rather than a thrown exception.
 */
object AddResourceEntry {
    /**
     * Returns [StepResult.Ok] with [entry] appended, or [StepResult.Blocked] when a spend
     * would drive the named resource below zero.
     */
    fun run(
        record: SettlementRecord,
        entry: ResourceEntry,
    ): StepResult<SettlementRecord> {
        val projected =
            Storage
                .project(record.state.resourceEntries)
                .filter { it.name == entry.name }
                .sumOf { it.quantity }
        if (entry.delta < 0 && projected + entry.delta < 0) {
            return StepResult.Blocked("insufficient ${entry.name}")
        }
        return StepResult.Ok(
            record.copy(
                state =
                    record.state.copy(
                        resourceEntries = record.state.resourceEntries + entry,
                    ),
            ),
        )
    }
}
