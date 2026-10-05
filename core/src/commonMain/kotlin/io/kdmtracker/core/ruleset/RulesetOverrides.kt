package io.kdmtracker.core.ruleset

import io.kdmtracker.core.domain.PrincipleCategory

/**
 * Per-campaign overrides applied on top of a base [Ruleset].
 *
 * Every field defaults to "no change", so `RulesetOverrides()` is a no-op and
 * [applyOverrides] returns the base ruleset unchanged.
 */
data class RulesetOverrides(
    val survivalLimit: Int? = null,
    val extraMilestones: List<MilestoneDefinition> = emptyList(),
    val principleOverrides: Map<PrincipleCategory, List<PrincipleChoice>> = emptyMap(),
)
