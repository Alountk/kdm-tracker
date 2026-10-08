package io.kdmtracker.core.usecase

import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.domain.SurvivorStatus
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.rules.StepResult

/**
 * Chooses the departing survivors for the next hunt (Settlement Phase step 8).
 *
 * At most `min(limit, living)` survivors may depart (rulebook 1.6: 4, fewer when the living
 * population is smaller). The chosen ids must all be living survivors; a larger selection or an
 * unknown/dead id is refused and leaves the record untouched. The chosen become `DEPARTING` and
 * every other living survivor returns to `ALIVE`.
 */
object MarkDeparting {
    /**
     * Returns [StepResult.Ok] with the selection applied, or [StepResult.Blocked] with
     * `"invalid departing selection"`.
     */
    fun run(
        record: SettlementRecord,
        ids: List<Ulid>,
        limit: Int,
    ): StepResult<SettlementRecord> {
        val living = record.state.survivors.filter { it.status != SurvivorStatus.DEAD }
        val livingIds = living.mapTo(mutableSetOf()) { it.id }
        if (ids.size > minOf(limit, living.size) || ids.any { it !in livingIds }) {
            return StepResult.Blocked("invalid departing selection")
        }
        val chosen = ids.toSet()
        val updated =
            record.state.survivors.map { survivor ->
                when {
                    survivor.status == SurvivorStatus.DEAD -> survivor
                    survivor.id in chosen -> survivor.copy(status = SurvivorStatus.DEPARTING)
                    else -> survivor.copy(status = SurvivorStatus.ALIVE)
                }
            }
        return StepResult.Ok(record.withSurvivors(updated))
    }
}
