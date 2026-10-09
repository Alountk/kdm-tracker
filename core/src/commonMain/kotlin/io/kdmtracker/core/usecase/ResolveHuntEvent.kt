package io.kdmtracker.core.usecase

import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.rules.StepResult

/**
 * Marks a hunt event on a board space as resolved.
 *
 * The event must already be placed on the space; a repeated resolve is a no-op.
 */
object ResolveHuntEvent {
    /** Returns [StepResult.Ok] with the event resolved, or [StepResult.Blocked]. */
    fun run(
        record: SettlementRecord,
        index: Int,
        eventName: String,
    ): StepResult<SettlementRecord> {
        val hunt = record.state.hunt
        if (index !in hunt.spaces.indices) {
            return StepResult.Blocked("invalid space")
        }
        val space = hunt.spaces[index]
        if (eventName !in space.events) {
            return StepResult.Blocked("event not assigned")
        }
        if (eventName in space.resolved) {
            return StepResult.Ok(record)
        }
        val spaces =
            hunt.spaces.toMutableList().also { list ->
                list[index] = space.copy(resolved = space.resolved + eventName)
            }
        return StepResult.Ok(record.copy(state = record.state.copy(hunt = hunt.copy(spaces = spaces))))
    }
}
