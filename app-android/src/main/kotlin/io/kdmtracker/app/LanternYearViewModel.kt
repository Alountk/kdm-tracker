package io.kdmtracker.app

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow

/**
 * Retains the lantern-year loop across recomposition and configuration changes.
 *
 * The [LanternYearController] is created once in the constructor and held here, so a
 * rotation or process re-creation no longer resets the loop to year 1 / step 1. The
 * ViewModel owns no rules: it exposes the controller's [state] and forwards navigation.
 */
class LanternYearViewModel(
    private val controller: LanternYearController = LanternYearController(SettlementRepository()),
) : ViewModel() {
    val state: StateFlow<UiState> = controller.state

    fun advance() = controller.advance()

    fun failHunt() = controller.failHunt()

    fun previous() = controller.previous()

    fun selectStep(number: Int) = controller.selectStep(number)
}
