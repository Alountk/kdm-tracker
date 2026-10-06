package io.kdmtracker.core.ruleset

import io.kdmtracker.core.domain.MilestoneId
import io.kdmtracker.core.domain.PrincipleCategory
import io.kdmtracker.core.rules.MilestoneConditionSpec
import io.kdmtracker.core.rules.SettlementStep
import kotlinx.serialization.Serializable

/** A milestone story event: a stable [id], its checkable [condition] and its [description]. */
@Serializable
data class MilestoneDefinition(
    val id: MilestoneId,
    val condition: MilestoneConditionSpec,
    val description: String,
)

/** One selectable side of a principle story event, with its rulebook [effect]. */
@Serializable
data class PrincipleChoice(
    val category: PrincipleCategory,
    val label: String,
    val effect: String,
)

/**
 * Versioned rules *content* (numbers, lists, conditions) for a campaign.
 *
 * Mechanisms live in `rules/`; the concrete values they read live here, so a
 * campaign can swap in house rules without touching the mechanisms.
 */
interface Ruleset {
    val id: String
    val version: String
    val settlementSteps: List<SettlementStep>

    /** Settlement step number of the Special Showdown (rulebook 1.6: 9). */
    val specialShowdownStep: Int

    /** Settlement step number the Special Showdown loops back to (rulebook 1.6: 5). */
    val deathCountStep: Int

    /** Endeavors earned per returning survivor at the Gain Endeavors step (rulebook 1.6: 1). */
    val endeavorsPerReturnee: Int

    val survivalLimit: Int
    val milestones: List<MilestoneDefinition>
    val principles: Map<PrincipleCategory, List<PrincipleChoice>>
}

/**
 * Returns a new [Ruleset] with [overrides] applied, leaving this receiver unchanged.
 *
 * A no-op [overrides] (all defaults) short-circuits and returns this exact instance.
 * Otherwise each scalar field is replaced when set, [RulesetOverrides.extraMilestones]
 * replace a base milestone with the same [MilestoneDefinition.id] (so a changed
 * threshold takes effect) and append the ones with new ids, and
 * [RulesetOverrides.principleOverrides] replace whole per-category choice lists while
 * untouched categories are kept.
 */
fun Ruleset.applyOverrides(overrides: RulesetOverrides): Ruleset {
    if (overrides.survivalLimit == null &&
        overrides.specialShowdownStep == null &&
        overrides.deathCountStep == null &&
        overrides.endeavorsPerReturnee == null &&
        overrides.extraMilestones.isEmpty() &&
        overrides.principleOverrides.isEmpty()
    ) {
        return this
    }
    return OverrideRuleset(
        id = id,
        version = version,
        settlementSteps = settlementSteps,
        specialShowdownStep = overrides.specialShowdownStep ?: specialShowdownStep,
        deathCountStep = overrides.deathCountStep ?: deathCountStep,
        endeavorsPerReturnee = overrides.endeavorsPerReturnee ?: endeavorsPerReturnee,
        survivalLimit = overrides.survivalLimit ?: survivalLimit,
        milestones = mergeMilestones(milestones, overrides.extraMilestones),
        principles = principles + overrides.principleOverrides,
    )
}

/**
 * Merges [extra] into [base] by [MilestoneDefinition.id]: a base milestone whose id
 * appears in [extra] is replaced, and the remaining extras are appended in order.
 */
private fun mergeMilestones(
    base: List<MilestoneDefinition>,
    extra: List<MilestoneDefinition>,
): List<MilestoneDefinition> {
    if (extra.isEmpty()) return base
    val byId = extra.associateBy { it.id }
    val overridden = base.map { byId[it.id] ?: it }
    val baseIds = base.mapTo(mutableSetOf()) { it.id }
    return overridden + extra.filter { it.id !in baseIds }
}

private data class OverrideRuleset(
    override val id: String,
    override val version: String,
    override val settlementSteps: List<SettlementStep>,
    override val specialShowdownStep: Int,
    override val deathCountStep: Int,
    override val endeavorsPerReturnee: Int,
    override val survivalLimit: Int,
    override val milestones: List<MilestoneDefinition>,
    override val principles: Map<PrincipleCategory, List<PrincipleChoice>>,
) : Ruleset
