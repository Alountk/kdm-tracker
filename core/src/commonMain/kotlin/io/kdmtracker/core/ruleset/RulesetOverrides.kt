package io.kdmtracker.core.ruleset

import io.kdmtracker.core.domain.PrincipleCategory
import kotlinx.serialization.Serializable

/**
 * Per-campaign overrides applied on top of a base [Ruleset].
 *
 * Every field defaults to "no change", so `RulesetOverrides()` is a no-op and
 * [applyOverrides] returns the base ruleset unchanged.
 */
@Serializable
data class RulesetOverrides(
    val survivalLimit: Int? = null,
    val specialShowdownStep: Int? = null,
    val deathCountStep: Int? = null,
    val endeavorsPerReturnee: Int? = null,
    val huntXpTrackMax: Int? = null,
    val insanityThreshold: Int? = null,
    val innovationDraw: Int? = null,
    val innovationKeep: Int? = null,
    val extraMilestones: List<MilestoneDefinition> = emptyList(),
    val principleOverrides: Map<PrincipleCategory, List<PrincipleChoice>> = emptyMap(),
)
