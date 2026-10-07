package io.kdmtracker.app

import io.kdmtracker.core.domain.Innovation
import io.kdmtracker.core.domain.ResourceDefinition
import io.kdmtracker.core.domain.SettlementLocation
import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.domain.StorageLine
import io.kdmtracker.core.rules.EndeavorBreakdown
import io.kdmtracker.core.rules.SettlementStep

/** The top-level destination the UI renders: the Home campaign list, or the cockpit's phases. */
sealed interface Screen {
    data object Home : Screen

    data object Settlement : Screen

    data object Hunt : Screen

    data object Showdown : Screen
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
)
