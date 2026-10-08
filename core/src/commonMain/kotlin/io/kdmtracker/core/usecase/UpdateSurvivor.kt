package io.kdmtracker.core.usecase

import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.domain.Survivor
import io.kdmtracker.core.rules.StepResult

/**
 * Replaces a roster survivor with an edited copy (the Survivor Editor's save).
 *
 * The whole rule set is enforced here, never in the UI: the survivor must exist, carry at most three
 * disorders and three fighting arts, and keep every countable field non-negative. A refusal leaves
 * the record untouched.
 */
object UpdateSurvivor {
    private const val MAX_DISORDERS = 3
    private const val MAX_FIGHTING_ARTS = 3

    /**
     * Returns [StepResult.Ok] with the survivor replaced, or [StepResult.Blocked] with
     * `"unknown survivor"` or `"invalid survivor"`.
     */
    fun run(
        record: SettlementRecord,
        survivor: Survivor,
    ): StepResult<SettlementRecord> {
        if (record.state.survivors.none { it.id == survivor.id }) {
            return StepResult.Blocked("unknown survivor")
        }
        if (!isValid(survivor)) {
            return StepResult.Blocked("invalid survivor")
        }
        val updated = record.state.survivors.map { if (it.id == survivor.id) survivor else it }
        return StepResult.Ok(record.withSurvivors(updated))
    }

    private fun isValid(survivor: Survivor): Boolean =
        survivor.disorders.size <= MAX_DISORDERS &&
            survivor.fightingArts.size <= MAX_FIGHTING_ARTS &&
            survivor.wounds.values.all { it.armor >= 0 } &&
            listOf(
                survivor.survival,
                survivor.insanity,
                survivor.huntXp,
                survivor.courage,
                survivor.understanding,
                survivor.weapon.xp,
                survivor.attributes.movement,
                survivor.attributes.accuracy,
                survivor.attributes.strength,
                survivor.attributes.evasion,
                survivor.attributes.luck,
                survivor.attributes.speed,
            ).all { it >= 0 }
}
