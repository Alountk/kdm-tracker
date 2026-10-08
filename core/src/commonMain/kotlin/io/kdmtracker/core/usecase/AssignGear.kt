package io.kdmtracker.core.usecase

import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.rules.StepResult

/**
 * Assigns a piece of settlement gear to a survivor (Settlement Phase step 8).
 *
 * The survivor must exist; assigning a name already on their list is a no-op success. An unknown
 * survivor id is refused and leaves the record untouched. The assigned gear's survivor-scope
 * modifiers feed the effective attributes ([io.kdmtracker.core.rules.SurvivorValues]).
 */
object AssignGear {
    /** Returns [StepResult.Ok] with the gear assigned, or [StepResult.Blocked] with `"unknown survivor"`. */
    fun run(
        record: SettlementRecord,
        survivorId: Ulid,
        gearName: String,
    ): StepResult<SettlementRecord> {
        val survivor =
            record.state.survivors.firstOrNull { it.id == survivorId }
                ?: return StepResult.Blocked("unknown survivor")
        if (gearName in survivor.gear) {
            return StepResult.Ok(record)
        }
        val updated =
            record.state.survivors.map { if (it.id == survivorId) it.copy(gear = it.gear + gearName) else it }
        return StepResult.Ok(record.withSurvivors(updated))
    }
}

/**
 * Removes a piece of gear from a survivor (Settlement Phase step 8).
 *
 * The survivor must exist; removing a name they do not carry is a no-op success. An unknown
 * survivor id is refused and leaves the record untouched.
 */
object UnassignGear {
    /** Returns [StepResult.Ok] with the gear removed, or [StepResult.Blocked] with `"unknown survivor"`. */
    fun run(
        record: SettlementRecord,
        survivorId: Ulid,
        gearName: String,
    ): StepResult<SettlementRecord> {
        val survivor =
            record.state.survivors.firstOrNull { it.id == survivorId }
                ?: return StepResult.Blocked("unknown survivor")
        if (gearName !in survivor.gear) {
            return StepResult.Ok(record)
        }
        val updated =
            record.state.survivors.map {
                if (it.id == survivorId) it.copy(gear = it.gear - gearName) else it
            }
        return StepResult.Ok(record.withSurvivors(updated))
    }
}
