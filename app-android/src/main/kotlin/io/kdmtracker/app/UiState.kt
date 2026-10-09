package io.kdmtracker.app

import io.kdmtracker.core.domain.GearRecipe
import io.kdmtracker.core.domain.HuntEvent
import io.kdmtracker.core.domain.HuntState
import io.kdmtracker.core.domain.Innovation
import io.kdmtracker.core.domain.MilestoneId
import io.kdmtracker.core.domain.MonsterCard
import io.kdmtracker.core.domain.PrincipleCategory
import io.kdmtracker.core.domain.ResourceDefinition
import io.kdmtracker.core.domain.SettlementEvent
import io.kdmtracker.core.domain.SettlementLocation
import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.domain.ShowdownState
import io.kdmtracker.core.domain.StorageLine
import io.kdmtracker.core.domain.Survivor
import io.kdmtracker.core.rules.CraftCheck
import io.kdmtracker.core.rules.EndeavorBreakdown
import io.kdmtracker.core.rules.InnovationView
import io.kdmtracker.core.rules.SettlementStep
import io.kdmtracker.core.ruleset.PrincipleChoice

/** The top-level destination the UI renders: the Home campaign list, or the cockpit's phases. */
sealed interface Screen {
    data object Home : Screen

    data object Settlement : Screen

    data object Hunt : Screen

    data object Showdown : Screen

    data object Account : Screen
}

/**
 * Everything the screens need to render one frame of the lantern-year loop.
 *
 * The trailing content-view fields are additive and defaulted so existing consumers keep
 * compiling: [record] is the full step-facing settlement, [catalogLocations] and
 * [innovationDefinitions] are the composed catalog's definitions for the screens,
 * [resourceDefinitions] is the catalog's resource list (so the Develop screen can record a
 * resource with content-true categories), [storageLines] is the resource ledger projected for
 * display, and [endeavorBreakdown] is the derived endeavor composition for the current
 * population. [catalogError] is set when the held campaign names a pack the app cannot resolve.
 *
 * The trailing Step 1 (Set Up) view fields are additive and defaulted: [innovationDeck] is the
 * consequence cards the unlocked innovations add, [builtLocations] is what the settlement has
 * already built, [principles] is the ruleset's principle choices, and [yearEvents] is this
 * year's timeline/event. They render the read-only set-up frame; the step screen owns no rules.
 *
 * [actionError] is the trailing Step 7 (Develop) field: the reason the last develop action was
 * refused, or `null` after a successful action or a navigation. The Develop screen shows it
 * inline and owns no rules.
 *
 * [gear] is the settlement's crafted gear, by name.
 *
 * [survivalLimit] is the derived survival limit — the ruleset base plus innovation bonuses plus any
 * settlement-scope `SURVIVAL_LIMIT` modifier on the settlement's crafted gear.
 *
 * The trailing settlement-bookkeeping fields drive Steps 4, 5, 6, 10 and 11: [settlementEventDeck]
 * is the content deck, [settlementEvent] the event recorded this year, [deaths]/[births] the
 * counters, [milestoneViews] the Step 6 checklist, and [chosenPrinciples] the chosen principle per
 * category.
 */
data class UiState(
    val screen: Screen,
    val lanternYear: Int,
    val phase: SettlementPhase,
    val settlementName: String,
    val currentStep: Int,
    val stepTitle: String,
    val population: Int,
    val endeavors: Int,
    val steps: List<SettlementStep>,
    val reachableSteps: Set<Int>,
    val lastTransition: String?,
    val record: SettlementRecord? = null,
    val catalogLocations: List<SettlementLocation> = emptyList(),
    val resourceDefinitions: List<ResourceDefinition> = emptyList(),
    val innovationDefinitions: List<Innovation> = emptyList(),
    val storageLines: List<StorageLine> = emptyList(),
    val endeavorBreakdown: EndeavorBreakdown = EndeavorBreakdown(base = 0, bonuses = emptyList()),
    val catalogError: String? = null,
    val innovationDeck: List<String> = emptyList(),
    val builtLocations: List<String> = emptyList(),
    val principles: List<String> = emptyList(),
    val yearEvents: List<String> = emptyList(),
    val actionError: String? = null,
    val gear: List<String> = emptyList(),
    val survivalLimit: Int = 0,
    val resourceGroups: Map<String, List<ResourceDefinition>> = emptyMap(),
    val innovationViews: List<InnovationView> = emptyList(),
    val craftRecipes: List<CraftRecipeView> = emptyList(),
    val settlementEventDeck: List<SettlementEvent> = emptyList(),
    val settlementEvent: String? = null,
    val deaths: Int = 0,
    val births: Int = 0,
    val milestoneViews: List<MilestoneView> = emptyList(),
    val chosenPrinciples: Map<PrincipleCategory, String> = emptyMap(),
    val survivors: List<Survivor> = emptyList(),
    val hunt: HuntState? = null,
    val quarries: List<String> = emptyList(),
    val nemeses: List<String> = emptyList(),
    val huntEvents: List<HuntEvent> = emptyList(),
    val showdown: ShowdownState? = null,
    val monsterCards: List<MonsterCard> = emptyList(),
)

/**
 * One row of the Step 6 milestone checklist: the milestone, its reached/resolved state and the
 * principle choices it exposes (empty for the non-principle milestones).
 */
data class MilestoneView(
    val id: MilestoneId,
    val description: String,
    val reached: Boolean,
    val resolved: Boolean,
    val choices: List<PrincipleChoice>,
)

/** A built location's recipe together with whether the settlement can craft it right now. */
data class CraftRecipeView(
    val locationName: String,
    val recipe: GearRecipe,
    val check: CraftCheck,
    val type: String = "",
)
