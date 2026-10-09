package io.kdmtracker.core.usecase

import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.domain.SurvivorStatus
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.rules.StepResult

/**
 * Assigns a party survivor to a player (1-based) for the hunt.
 *
 * The survivor must be a departing party member (the Step-8 selection) and the player index must be
 * within `1..players`.
 */
object AssignPlayer {
    /** Returns [StepResult.Ok] with the assignment, or [StepResult.Blocked]. */
    fun run(
        record: SettlementRecord,
        survivorId: Ulid,
        player: Int,
    ): StepResult<SettlementRecord> {
        val survivor = record.state.survivors.firstOrNull { it.id == survivorId }
        if (survivor == null) {
            return StepResult.Blocked("unknown survivor")
        }
        if (survivor.status != SurvivorStatus.DEPARTING) {
            return StepResult.Blocked("survivor is not departing")
        }
        val hunt = record.state.hunt
        if (player !in 1..hunt.players) {
            return StepResult.Blocked("invalid player")
        }
        return StepResult.Ok(
            record.copy(
                state =
                    record.state.copy(
                        hunt =
                            hunt.copy(
                                assignments =
                                    hunt.assignments + (survivorId to player),
                            ),
                    ),
            ),
        )
    }
}
