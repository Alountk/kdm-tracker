package io.kdmtracker.core.domain

import io.kdmtracker.core.rules.DerivedValues
import io.kdmtracker.core.ruleset.Ruleset
import kotlinx.serialization.Serializable

/**
 * The **extra** settlement record the phase steps read, separate from [Settlement].
 *
 * It holds only what the settlement aggregate does not own — the [Timeline], the
 * unlocked [InnovationCollection], the built [LocationSet], the resource ledger and
 * the live counters the milestone rules inspect. Scalars that belong to [Settlement]
 * (`name`, `lanternYear`, `population`, `phase`, `currentStep`, `endeavors`, `sync`)
 * are never duplicated here; [SettlementRecord] composes the full step-facing view.
 *
 * @property resourceEntries the append-only resource ledger; storage is projected from it.
 * @property deaths the survivors lost so far.
 * @property births the survivors born so far.
 * @property phaseActionsUsed the action identifiers already spent this Settlement Phase, so
 *   once-per-phase actions (such as Innovate) can be refused on a repeat attempt.
 * @property gear the names of the gear the settlement has crafted.
 */
@Serializable
data class SettlementState(
    val timeline: Timeline = Timeline(),
    val innovations: InnovationCollection = InnovationCollection(),
    val locations: LocationSet = LocationSet(),
    val resourceEntries: List<ResourceEntry> = emptyList(),
    val deaths: Int = 0,
    val births: Int = 0,
    val phaseActionsUsed: Set<String> = emptySet(),
    val gear: List<String> = emptyList(),
) {
    /** Whether [key] has already been used during the current Settlement Phase. */
    fun usedInPhase(key: String): Boolean = key in phaseActionsUsed

    /**
     * Returns a copy marking [key] as used this phase. Idempotent: a key already
     * present leaves the state unchanged.
     */
    fun withPhaseAction(key: String): SettlementState =
        if (key in phaseActionsUsed) this else copy(phaseActionsUsed = phaseActionsUsed + key)

    /** Returns a copy that also holds [name] in its gear, deduped by name. */
    fun withGear(name: String): SettlementState = if (name in gear) this else copy(gear = gear + name)
}

/** The storage lines projected from this state's resource ledger. */
fun SettlementState.storage(): List<StorageLine> = Storage.project(resourceEntries)

/**
 * The settlement's survival limit: the ruleset base plus the survival limit bonus of
 * every unlocked innovation. Derived, never stored.
 */
fun SettlementState.survivalLimit(ruleset: Ruleset): Int =
    DerivedValues.survivalLimit(ruleset, innovations.survivalLimitBonus())
