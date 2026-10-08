package io.kdmtracker.core.domain

import io.kdmtracker.core.rules.MilestoneState
import io.kdmtracker.core.rules.reachedMilestones
import io.kdmtracker.core.ruleset.Ruleset

/**
 * The full step-facing view of a settlement, composed from the [Settlement] scalars and
 * the extra [SettlementState] record.
 *
 * This is the single adapter the phase steps read: it supplies the milestone snapshot
 * (population from the settlement, counters and innovation count from the state) and the
 * derived storage and survival limit. No scalar is stored twice.
 */
data class SettlementRecord(
    val settlement: Settlement,
    val state: SettlementState = SettlementState(),
) {
    /** The living survivors: everyone in the roster whose status is not [SurvivorStatus.DEAD]. */
    val population: Int get() = state.survivors.count { it.status != SurvivorStatus.DEAD }

    /**
     * Adapts the counters and the roster population to the milestone snapshot.
     *
     * The innovation counter is the number of unlocked innovations, not the deck size.
     */
    fun milestoneState(): MilestoneState =
        MilestoneState(
            deaths = state.deaths,
            births = state.births,
            population = population,
            innovations = state.innovations.innovations.size,
        )

    /** Exactly the milestones the [ruleset]'s definitions mark as reached. */
    fun reachedMilestones(ruleset: Ruleset): Set<MilestoneId> = reachedMilestones(milestoneState(), ruleset.milestones)

    /** The storage lines projected from the state's resource ledger. */
    fun storage(): List<StorageLine> = state.storage()

    /** The derived survival limit for [ruleset]. */
    fun survivalLimit(ruleset: Ruleset): Int = state.survivalLimit(ruleset)
}
