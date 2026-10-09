package io.kdmtracker.core.usecase

import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.rules.StepResult

/**
 * Grants +1 hunt XP to each surviving participant after a victorious showdown.
 *
 * Every id must be a roster survivor; an unknown id is refused and leaves the record untouched.
 */
object RecordHuntXp {
    /** Returns [StepResult.Ok] with the survivors' hunt XP raised, or [StepResult.Blocked]. */
    fun run(
        record: SettlementRecord,
        survivorIds: List<Ulid>,
    ): StepResult<SettlementRecord> {
        val ids = survivorIds.toSet()
        if (ids.any { id -> record.state.survivors.none { it.id == id } }) {
            return StepResult.Blocked("unknown survivor")
        }
        val updated =
            record.state.survivors.map { survivor ->
                if (survivor.id in ids) survivor.copy(huntXp = survivor.huntXp + 1) else survivor
            }
        return StepResult.Ok(record.withSurvivors(updated))
    }
}
