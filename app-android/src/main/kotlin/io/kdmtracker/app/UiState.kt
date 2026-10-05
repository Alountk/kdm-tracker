package io.kdmtracker.app

import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.rules.SettlementStep

/** The top-level destination the UI renders, mirroring the settlement's [SettlementPhase]. */
sealed interface Screen {
    data object Settlement : Screen

    data object Hunt : Screen

    data object Showdown : Screen
}

/** Everything the screens need to render one frame of the lantern-year loop. */
data class UiState(
    val screen: Screen,
    val lanternYear: Int,
    val phase: SettlementPhase,
    val currentStep: Int,
    val stepTitle: String,
    val population: Int,
    val endeavors: Int,
    val steps: List<SettlementStep>,
    val reachableSteps: Set<Int>,
    val lastTransition: String?,
)
