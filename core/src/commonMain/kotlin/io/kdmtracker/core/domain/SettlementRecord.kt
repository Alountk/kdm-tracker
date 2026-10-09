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
     * Replaces the roster and keeps the legacy [Settlement.population] scalar equal to the derived
     * [population], so the scalar (used by the Home cards and exports) never drifts from the roster.
     */
    fun withSurvivors(survivors: List<Survivor>): SettlementRecord =
        copy(
            settlement = settlement.copy(population = survivors.count { it.status != SurvivorStatus.DEAD }),
            state = state.copy(survivors = survivors),
        )

    /**
     * Seeds the roster from the legacy [Settlement.population] when it is empty but the scalar is
     * positive (a record written before the roster existed). A record that already has a roster, or
     * a zero population, is returned unchanged.
     *
     * [newId] supplies a fresh id per seeded survivor; the default derives a **deterministic** id
     * from the settlement id and the survivor index, so re-running the migration (e.g. on each
     * `reload()`) yields the same ids instead of churning them.
     */
    fun migrated(
        newId: (Int) -> Ulid = { index -> Ulid.generate(index.toLong()) { settlement.id.value.hashCode() } },
    ): SettlementRecord {
        if (state.survivors.isNotEmpty() || settlement.population <= 0) {
            return this
        }
        return withSurvivors(List(settlement.population) { Survivor(id = newId(it)) })
    }

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

    /**
     * The nemesis a lantern [year]'s timeline lists, or `null` when the year has none.
     *
     * A label like `"Nemesis: The Butcher Lvl 2"` yields `"The Butcher"` (the `Nemesis:` prefix and
     * the ` Lvl N` suffix stripped).
     */
    fun nemesisOf(year: Int): String? =
        state.timeline
            .entriesFor(year)
            .map { it.label }
            .firstOrNull { it.startsWith("Nemesis") }
            ?.removePrefix("Nemesis:")
            ?.trim()
            ?.replace(Regex("\\s+Lvl\\s+\\d+$"), "")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    /** The storage lines projected from the state's resource ledger. */
    fun storage(): List<StorageLine> = state.storage()

    /** The derived survival limit for [ruleset]. */
    fun survivalLimit(ruleset: Ruleset): Int = state.survivalLimit(ruleset)
}
