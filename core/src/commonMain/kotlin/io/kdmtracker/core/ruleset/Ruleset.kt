package io.kdmtracker.core.ruleset

import io.kdmtracker.core.domain.MilestoneKey
import io.kdmtracker.core.domain.PrincipleCategory
import io.kdmtracker.core.rules.SettlementStep

/** A milestone whose [condition] triggers a story event when reached. */
data class MilestoneDefinition(
    val key: MilestoneKey,
    val condition: String,
)

/** One selectable side of a principle story event, with its rulebook [effect]. */
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
    val survivalLimit: Int
    val milestones: List<MilestoneDefinition>
    val principles: Map<PrincipleCategory, List<PrincipleChoice>>
}

/**
 * Returns a new [Ruleset] with [overrides] applied, leaving this receiver unchanged.
 *
 * A no-op [overrides] (all defaults) short-circuits and returns this exact instance.
 * Otherwise the survival limit is replaced when set, [RulesetOverrides.extraMilestones]
 * are appended to the base milestones, and [RulesetOverrides.principleOverrides] replace
 * whole per-category choice lists while untouched categories are kept.
 */
fun Ruleset.applyOverrides(overrides: RulesetOverrides): Ruleset {
    if (overrides.survivalLimit == null &&
        overrides.extraMilestones.isEmpty() &&
        overrides.principleOverrides.isEmpty()
    ) {
        return this
    }
    return OverrideRuleset(
        id = id,
        version = version,
        settlementSteps = settlementSteps,
        survivalLimit = overrides.survivalLimit ?: survivalLimit,
        milestones = milestones + overrides.extraMilestones,
        principles = principles + overrides.principleOverrides,
    )
}

private data class OverrideRuleset(
    override val id: String,
    override val version: String,
    override val settlementSteps: List<SettlementStep>,
    override val survivalLimit: Int,
    override val milestones: List<MilestoneDefinition>,
    override val principles: Map<PrincipleCategory, List<PrincipleChoice>>,
) : Ruleset
