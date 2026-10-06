package io.kdmtracker.core.rules

import io.kdmtracker.core.domain.MilestoneId
import io.kdmtracker.core.ruleset.MilestoneDefinition
import kotlinx.serialization.Serializable

/** The settlement counter a milestone condition measures. */
enum class MilestoneMetric {
    DEATHS,
    BIRTHS,
    POPULATION,
    INNOVATIONS,
}

/**
 * A machine-checkable milestone trigger, declared as content by a ruleset.
 *
 * The threshold and comparison live in the spec, never in the mechanism, so an
 * override can change when a milestone fires without editing `rules/`.
 */
@Serializable
sealed interface MilestoneConditionSpec {
    /** The metric is at least [value] (`deaths >= 1`, `population >= 15`). */
    @Serializable
    data class AtLeast(
        val metric: MilestoneMetric,
        val value: Int,
    ) : MilestoneConditionSpec

    /** The metric is at most [value] (`population <= 0` for Game Over). */
    @Serializable
    data class AtMost(
        val metric: MilestoneMetric,
        val value: Int,
    ) : MilestoneConditionSpec
}

/** Snapshot of the settlement counters a milestone condition inspects. */
data class MilestoneState(
    val deaths: Int,
    val births: Int,
    val population: Int,
    val innovations: Int,
)

/**
 * Evaluates the typed milestone conditions a ruleset declares.
 *
 * No threshold is hardcoded here: every value and comparison comes from the
 * [MilestoneConditionSpec] the caller supplies.
 */
object MilestoneConditions {
    /** Whether [state] satisfies [spec]. */
    fun evaluate(
        state: MilestoneState,
        spec: MilestoneConditionSpec,
    ): Boolean =
        when (spec) {
            is MilestoneConditionSpec.AtLeast -> valueFor(state, spec.metric) >= spec.value
            is MilestoneConditionSpec.AtMost -> valueFor(state, spec.metric) <= spec.value
        }

    private fun valueFor(
        state: MilestoneState,
        metric: MilestoneMetric,
    ): Int =
        when (metric) {
            MilestoneMetric.DEATHS -> state.deaths
            MilestoneMetric.BIRTHS -> state.births
            MilestoneMetric.POPULATION -> state.population
            MilestoneMetric.INNOVATIONS -> state.innovations
        }
}

/** Exactly the ids whose ruleset [definitions] [state] has reached. */
fun reachedMilestones(
    state: MilestoneState,
    definitions: List<MilestoneDefinition>,
): Set<MilestoneId> =
    definitions
        .filter { MilestoneConditions.evaluate(state, it.condition) }
        .mapTo(mutableSetOf()) { it.id }
