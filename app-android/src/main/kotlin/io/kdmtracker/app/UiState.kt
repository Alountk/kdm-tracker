package io.kdmtracker.app

import io.kdmtracker.core.domain.Innovation
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
 * compiling: [record] is the full step-facing settlement, [buildableLocations] and
 * [innovationDefinitions] are the composed catalog's definitions for the screens,
 * [storageLines] is the resource ledger projected for display, and [endeavorBreakdown] is
 * the derived endeavor composition for the current population. [catalogError] is set when
 * the held campaign names a pack the app cannot resolve.
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
    val buildableLocations: List<SettlementLocation> = emptyList(),
    val innovationDefinitions: List<Innovation> = emptyList(),
    val storageLines: List<StorageLine> = emptyList(),
    val endeavorBreakdown: EndeavorBreakdown = EndeavorBreakdown(base = 0, bonuses = emptyList()),
    val catalogError: String? = null,
)
