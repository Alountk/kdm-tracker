package io.kdmtracker.core.usecase

import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.rules.StepResult

/**
 * Places a hunt event on a board space.
 *
 * The space index must be in range and the event name non-blank; an already-placed event is a no-op.
 */
object AssignHuntEvent {
    /** Returns [StepResult.Ok] with the event placed, or [StepResult.Blocked]. */
    fun run(
        record: SettlementRecord,
        index: Int,
        eventName: String,
    ): StepResult<SettlementRecord> {
        val hunt = record.state.hunt
        if (index !in hunt.spaces.indices) {
            return StepResult.Blocked("invalid space")
        }
        if (eventName.isBlank()) {
            return StepResult.Blocked("unknown event")
        }
        val space = hunt.spaces[index]
        if (eventName in space.events) {
            return StepResult.Ok(record)
        }
        val spaces =
            hunt.spaces.toMutableList().also { list ->
                list[index] = space.copy(events = space.events + eventName)
            }
        return StepResult.Ok(record.copy(state = record.state.copy(hunt = hunt.copy(spaces = spaces))))
    }
}
