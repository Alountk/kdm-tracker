package io.kdmtracker.core.usecase

import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.rules.StepResult

/**
 * Resolves the hunt-event card the party drew on its last advance, in one step: places it on the
 * party's current space (if it is not there yet) and marks it resolved, lifting the advance gate.
 *
 * Blocks when no card is drawn. Resolving an already-resolved card is a no-op.
 */
object ResolveDrawnHuntEvent {
    /** Returns [StepResult.Ok] with the drawn card resolved on the party's space, or [StepResult.Blocked]. */
    fun run(record: SettlementRecord): StepResult<SettlementRecord> {
        val hunt = record.state.hunt
        val drawn = hunt.drawnEvent ?: return StepResult.Blocked("no event drawn")
        return when (val placed = AssignHuntEvent.run(record, hunt.position, drawn.name)) {
            is StepResult.Ok -> ResolveHuntEvent.run(placed.value, hunt.position, drawn.name)
            is StepResult.Blocked -> placed
        }
    }
}
