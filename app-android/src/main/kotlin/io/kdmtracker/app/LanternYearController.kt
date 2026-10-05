package io.kdmtracker.app

import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.rules.FlowContext
import io.kdmtracker.core.rules.FlowTransition
import io.kdmtracker.core.rules.SettlementStepCatalog
import io.kdmtracker.core.ruleset.Core16Ruleset
import io.kdmtracker.core.ruleset.Ruleset
import io.kdmtracker.core.usecase.AdvanceStep
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Drives the lantern-year loop for the UI.
 *
 * This class is deliberately plain Kotlin: it holds no Android APIs so it can be unit-tested on
 * the JVM. Every phase/step transition is delegated to `:core` ([AdvanceStep] over
 * [io.kdmtracker.core.rules.LanternYearFlow]); the controller only mirrors the resulting
 * [Settlement] into a [UiState] and maps the phase to a [Screen]. No business rules live here.
 */
class LanternYearController(
    private val repository: SettlementRepository,
    private val ruleset: Ruleset = Core16Ruleset,
) {
    private val _state =
        MutableStateFlow(uiState(repository.settlement, lastTransition = null))

    val state: StateFlow<UiState> = _state.asStateFlow()

    /** Advances one position in the lantern-year flow, delegating the move to `:core`. */
    fun advance() {
        val settlement = repository.settlement
        val result =
            AdvanceStep.run(
                settlement = settlement,
                returnedSurvivors = settlement.population,
                context = FlowContext(ruleset = ruleset),
            )
        repository.update(result.settlement)
        _state.value = uiState(result.settlement, result.transition.describe())
    }

    /**
     * Moves the settlement one step back without crossing a phase boundary.
     *
     * Only meaningful on the settlement screen; on Hunt/Showdown it is a no-op, and the step is
     * floored at 1.
     */
    fun previous() {
        val settlement = repository.settlement
        if (settlement.phase != SettlementPhase.SETTLEMENT || settlement.currentStep <= 1) return
        moveToStep(settlement.currentStep - 1)
    }

    /** Jumps to [number] when it is a valid settlement step, otherwise a no-op. */
    fun selectStep(number: Int) {
        if (number !in 1..SettlementStepCatalog.TOTAL) return
        moveToStep(number)
    }

    private fun moveToStep(step: Int) {
        val updated = repository.settlement.copy(currentStep = step)
        repository.update(updated)
        _state.value = uiState(updated, _state.value.lastTransition)
    }

    private fun uiState(
        settlement: Settlement,
        lastTransition: String?,
    ): UiState =
        UiState(
            screen = settlement.phase.toScreen(),
            lanternYear = settlement.lanternYear,
            phase = settlement.phase,
            currentStep = settlement.currentStep,
            stepTitle = SettlementStepCatalog.byNumber(settlement.currentStep).title,
            population = settlement.population,
            endeavors = settlement.endeavors,
            steps = SettlementStepCatalog.ALL,
            lastTransition = lastTransition,
        )
}

private fun SettlementPhase.toScreen(): Screen =
    when (this) {
        SettlementPhase.SETTLEMENT -> Screen.Settlement
        SettlementPhase.HUNT -> Screen.Hunt
        SettlementPhase.SHOWDOWN -> Screen.Showdown
    }

private fun FlowTransition.describe(): String =
    when (this) {
        is FlowTransition.Advance -> "Advance → ${to.phase} ${to.step}"
        is FlowTransition.LoopBack -> "LoopBack → ${to.phase} ${to.step}"
        is FlowTransition.Commit -> "Commit → ${to.phase} ${to.step}"
        is FlowTransition.Blocked -> "Blocked: $reason"
    }
