package io.kdmtracker.core.rules

import io.kdmtracker.core.domain.MilestoneKey

/** Snapshot of the settlement counters a [MilestoneCondition] inspects. */
data class MilestoneState(
    val deaths: Int,
    val births: Int,
    val population: Int,
    val innovations: Int,
)

/** A milestone story event's trigger, evaluated against a [MilestoneState]. */
fun interface MilestoneCondition {
    fun reached(state: MilestoneState): Boolean
}

/**
 * Machine-checkable predicates for `Core16Ruleset`'s milestone story events,
 * replacing the prose conditions the ruleset declares.
 *
 * The thresholds are named constants rather than values read from a ruleset at
 * call time: [forKey] receives only a [MilestoneKey], so no ruleset is in scope.
 * `MilestoneConditionsTest.thresholdsMatchCore16RulesetDeclaredMilestoneConditions`
 * guards them against silently drifting from the declared content.
 */
object MilestoneConditions {
    const val DEATHS: Int = 1
    const val BIRTHS: Int = 1
    const val SOCIETY_POPULATION: Int = 15
    const val HOODED_KNIGHT_INNOVATIONS: Int = 5
    const val GAME_OVER_POPULATION: Int = 0

    val death: MilestoneCondition = MilestoneCondition { it.deaths >= DEATHS }
    val newLife: MilestoneCondition = MilestoneCondition { it.births >= BIRTHS }
    val society: MilestoneCondition = MilestoneCondition { it.population >= SOCIETY_POPULATION }
    val hoodedKnight: MilestoneCondition = MilestoneCondition { it.innovations >= HOODED_KNIGHT_INNOVATIONS }
    val gameOver: MilestoneCondition = MilestoneCondition { it.population <= GAME_OVER_POPULATION }

    /** The condition that fires [key]'s milestone story event. */
    fun forKey(key: MilestoneKey): MilestoneCondition =
        when (key) {
            MilestoneKey.PRINCIPLE_DEATH -> death
            MilestoneKey.PRINCIPLE_NEW_LIFE -> newLife
            MilestoneKey.PRINCIPLE_SOCIETY -> society
            MilestoneKey.HOODED_KNIGHT -> hoodedKnight
            MilestoneKey.GAME_OVER -> gameOver
        }
}

/** Exactly the [keys] whose condition is reached in [state]. */
fun reachedMilestones(
    state: MilestoneState,
    keys: Collection<MilestoneKey>,
): Set<MilestoneKey> = keys.filter { MilestoneConditions.forKey(it).reached(state) }.toSet()
