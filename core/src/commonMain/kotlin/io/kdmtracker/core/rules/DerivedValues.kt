package io.kdmtracker.core.rules

import io.kdmtracker.core.domain.Modifier
import io.kdmtracker.core.domain.ModifierScope
import io.kdmtracker.core.domain.ModifierTarget
import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.ruleset.Ruleset

/**
 * The components of an endeavor count, split so callers can see *why* a total is
 * what it is instead of trusting a single carried scalar.
 *
 * @property base the endeavors granted by returning survivors.
 * @property bonuses named additions, empty until an innovation or principle model exists.
 */
data class EndeavorBreakdown(
    val base: Int,
    val bonuses: List<Pair<String, Int>>,
) {
    val total: Int = base + bonuses.sumOf { it.second }
}

/**
 * Derives settlement values from primitives and the active [Ruleset].
 *
 * Every function reads its inputs afresh; nothing here stores a derived scalar.
 * Numbers come from the ruleset, never from literals baked into the mechanism.
 */
object DerivedValues {
    /**
     * Endeavors earned this lantern year for [returnedSurvivors].
     *
     * The base is `returnedSurvivors * ruleset.endeavorsPerReturnee`. Bonuses are
     * deliberately empty: no innovation or principle model exists yet, so the
     * [EndeavorBreakdown.bonuses] list is the seam future rules plug into rather than a
     * hardcoded number.
     */
    fun endeavorsFor(
        returnedSurvivors: Int,
        ruleset: Ruleset,
    ): EndeavorBreakdown =
        EndeavorBreakdown(
            base = returnedSurvivors * ruleset.endeavorsPerReturnee,
            bonuses = emptyList(),
        )

    /** Survival limit: the ruleset base plus any [innovationBonus]. */
    fun survivalLimit(
        ruleset: Ruleset,
        innovationBonus: Int = 0,
    ): Int = ruleset.survivalLimit + innovationBonus

    /**
     * The sum of every modifier in [modifiers] whose target and scope both match.
     *
     * Only [ModifierScope.SETTLEMENT] modifiers feed derived values; a survivor-scope
     * modifier of the same target is ignored here.
     */
    fun modifierTotal(
        modifiers: List<Modifier>,
        target: ModifierTarget,
        scope: ModifierScope,
    ): Int = modifiers.filter { it.target == target && it.scope == scope }.sumOf { it.value }
}

/**
 * Returns a copy of [settlement] whose derived scalars are recomputed from primitives.
 *
 * The input's own [Settlement.endeavors] is discarded outright — the result depends
 * only on [returnedSurvivors] and [ruleset], so editing an earlier step changes the
 * recomputed output instead of carrying stale state forward.
 */
fun recompute(
    settlement: Settlement,
    returnedSurvivors: Int,
    ruleset: Ruleset,
): Settlement =
    settlement.copy(
        endeavors = DerivedValues.endeavorsFor(returnedSurvivors, ruleset).total,
    )
