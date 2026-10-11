package io.kdmtracker.core.usecase

import io.kdmtracker.core.domain.SettlementRecord

/**
 * Advances the settlement's lantern year by one (Settlement Phase step 4, Actualizar Línea de Tiempo).
 *
 * The increment is idempotent per Settlement Phase: it is guarded by a phase marker carried
 * in [io.kdmtracker.core.domain.SettlementState.phaseActionsUsed], which resets at the year
 * boundary, so revisiting step 4 cannot double-increment while the next year's step 4 still
 * does. The caller decides *when* to call it (leaving step 4 forward); this use case only
 * performs the increment and records the marker.
 */
object AdvanceTimeline {
    private const val MARKER = "Actualizar Línea de Tiempo"

    /** Returns [record] with the lantern year incremented once, or unchanged if already done. */
    fun run(record: SettlementRecord): SettlementRecord {
        if (record.state.usedInPhase(MARKER)) {
            return record
        }
        return record.copy(
            settlement = record.settlement.copy(lanternYear = record.settlement.lanternYear + 1),
            state = record.state.withPhaseAction(MARKER),
        )
    }
}
