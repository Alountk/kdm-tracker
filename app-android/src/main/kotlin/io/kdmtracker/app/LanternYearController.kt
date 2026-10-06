package io.kdmtracker.app

import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.rules.FlowContext
import io.kdmtracker.core.rules.FlowTransition
import io.kdmtracker.core.rules.LanternYearFlow
import io.kdmtracker.core.rules.PhaseStep
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
        applyStep(
            returnees = repository.settlement.population,
            context = FlowContext(ruleset = ruleset),
        )
    }

    /**
     * Records a failed hunt (for example a total party kill) and restarts the settlement year.
     *
     * The restart is produced entirely by `:core`'s hunt-failure branch; the controller only
     * supplies the [FlowContext] that selects it, so the settlement re-enters at step 1. No
     * survivors return from a failed hunt, so [returnees] is zero.
     */
    fun failHunt() {
        applyStep(
            returnees = 0,
            context = FlowContext(ruleset = ruleset, huntFailed = true),
        )
    }

    /**
     * Runs one `:core` transition for [context] with [returnees] returning survivors and mirrors
     * the resulting settlement.
     *
     * This is the single path both [advance] and [failHunt] take; neither reimplements any step or
     * phase rule — the target always comes from `:core`. The returnee count is passed through so a
     * TPK can express "0 returning survivors" instead of silently reusing the population.
     */
    private fun applyStep(
        returnees: Int,
        context: FlowContext,
    ) {
        val settlement = repository.settlement
        val result =
            AdvanceStep.run(
                settlement = settlement,
                returnedSurvivors = returnees,
                context = context,
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

    /**
     * Jumps to [number] when `:core`'s flow allows entering it from the current position.
     *
     * The gate is [LanternYearFlow.canEnterStep]: a step may not be skipped forward (step 11 from
     * step 1 is ignored), while revisiting the current step or any earlier one is allowed. The
     * controller never decides this itself.
     */
    fun selectStep(number: Int) {
        val settlement = repository.settlement
        val from = PhaseStep(settlement.phase, settlement.currentStep)
        if (!LanternYearFlow.canEnterStep(number, from, FlowContext(ruleset = ruleset))) return
        moveToStep(number)
    }

    /**
     * Re-reads the repository after an external (sync) change and republishes [state].
     *
     * The controller mirrors the settlement into [state] once per mutation; a sync pass mutates the
     * store behind the controller's back, so without this the UI would keep rendering the pre-sync
     * frame (Ruling AE).
     */
    fun reload() {
        repository.reload()
        _state.value = uiState(repository.settlement, _state.value.lastTransition)
    }

    private fun moveToStep(step: Int) {
        val updated = repository.settlement.copy(currentStep = step)
        repository.update(updated)
        _state.value = uiState(updated, _state.value.lastTransition)
    }

    private fun uiState(
        settlement: Settlement,
        lastTransition: String?,
    ): UiState {
        val from = PhaseStep(settlement.phase, settlement.currentStep)
        val context = FlowContext(ruleset = ruleset)
        return UiState(
            screen = settlement.phase.toScreen(),
            lanternYear = settlement.lanternYear,
            phase = settlement.phase,
            currentStep = settlement.currentStep,
            stepTitle =
                ruleset.settlementSteps
                    .firstOrNull { it.number == settlement.currentStep }
                    ?.title
                    .orEmpty(),
            population = settlement.population,
            endeavors = settlement.endeavors,
            steps = ruleset.settlementSteps,
            reachableSteps =
                ruleset.settlementSteps
                    .filter { LanternYearFlow.canEnterStep(it.number, from, context) }
                    .map { it.number }
                    .toSet(),
            lastTransition = lastTransition,
        )
    }
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
