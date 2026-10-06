package io.kdmtracker.core.domain

import io.kdmtracker.core.rules.MilestoneState
import io.kdmtracker.core.rules.reachedMilestones

/**
 * The complete, immutable snapshot of a settlement that the phase steps read.
 *
 * It composes the sheet's record pieces — the [Timeline], the unlocked
 * [InnovationCollection], the built [LocationSet] and the projected [StorageLine]s —
 * with the live counters the milestone rules inspect.
 *
 * @property lanternYear the current lantern year, one-based.
 * @property survivalLimit the settlement's survival limit.
 * @property timeline the story and nemesis events scheduled across the campaign.
 * @property innovations the innovations the settlement has unlocked.
 * @property locations the settlement locations that have been built.
 * @property storage the projected resource inventory.
 * @property population the number of living survivors; defaults to the four
 *   founding survivors of [Settlement.new]. A default state therefore does not
 *   reach GAME_OVER, which requires an explicit `population <= 0`.
 * @property deaths the survivors lost so far.
 * @property births the survivors born so far.
 * @property enabledPacks the content packs enabled for this settlement's campaign.
 */
data class SettlementState(
    val lanternYear: Int = 1,
    val survivalLimit: Int,
    val timeline: Timeline = Timeline(),
    val innovations: InnovationCollection = InnovationCollection(),
    val locations: LocationSet = LocationSet(),
    val storage: List<StorageLine> = emptyList(),
    val population: Int = 4,
    val deaths: Int = 0,
    val births: Int = 0,
    val enabledPacks: List<String> = emptyList(),
)

/**
 * Adapts this settlement to the [MilestoneState] the milestone conditions inspect.
 *
 * The innovation counter is the number of unlocked innovations, not the deck size.
 */
fun SettlementState.milestoneState(): MilestoneState =
    MilestoneState(
        deaths = deaths,
        births = births,
        population = population,
        innovations = innovations.innovations.size,
    )

/** Exactly the milestones this settlement's counters have reached. */
fun SettlementState.reachedMilestones(): Set<MilestoneKey> = reachedMilestones(milestoneState(), MilestoneKey.entries)
