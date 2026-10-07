package io.kdmtracker.app

import io.kdmtracker.core.domain.InnovationDeck
import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.rules.DerivedValues
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
        MutableStateFlow(uiState(repository.record, lastTransition = null))

    val state: StateFlow<UiState> = _state.asStateFlow()

    /** Every stored campaign as a Home card, joined with its sharing badge (Ruling AJ/AK). */
    fun cards(): List<CampaignCard> = repository.listCards()

    /**
     * Swaps the held campaign and settlement to [campaignId]'s and republishes the cockpit.
     *
     * Returns `false` and changes nothing when no stored campaign has that id (Review Focus 3).
     */
    fun selectCampaign(campaignId: String): Boolean {
        if (!repository.select(campaignId)) return false
        reload()
        return true
    }

    /**
     * Creates [name] on the hub, persists and opens it, and returns its Home card.
     *
     * The hub round-trip and any local write live in [SettlementRepository.create]; a thrown
     * failure escapes unchanged so the caller can surface it, and the prior selection is kept
     * (Review Focus 4).
     */
    fun createCampaign(name: String): CampaignCard {
        val card = repository.create(name)
        reload()
        return card
    }

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
        val current = repository.record
        val state =
            if (result.transition.landsOnNewYear()) {
                current.state.copy(phaseActionsUsed = emptySet())
            } else {
                current.state
            }
        repository.update(current.copy(settlement = result.settlement, state = state))
        _state.value = uiState(repository.record, result.transition.describe())
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
        _state.value = uiState(repository.record, _state.value.lastTransition)
    }

    private fun moveToStep(step: Int) {
        val updated = repository.settlement.copy(currentStep = step)
        repository.update(updated)
        _state.value = uiState(repository.record, _state.value.lastTransition)
    }

    private fun uiState(
        record: SettlementRecord,
        lastTransition: String?,
    ): UiState {
        val settlement = record.settlement
        val from = PhaseStep(settlement.phase, settlement.currentStep)
        val context = FlowContext(ruleset = ruleset)
        return UiState(
            screen = settlement.phase.toScreen(),
            lanternYear = settlement.lanternYear,
            phase = settlement.phase,
            settlementName = settlement.name,
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
            record = record,
            buildableLocations = repository.catalog.locationDefinitions,
            innovationDefinitions = repository.catalog.innovationDefinitions,
            storageLines = record.storage(),
            endeavorBreakdown = DerivedValues.endeavorsFor(settlement.population, ruleset),
            catalogError = repository.catalogError,
            innovationDeck = InnovationDeck.of(record.state.innovations).cards,
            builtLocations = record.state.locations.built,
            principles =
                ruleset.principles.entries
                    .sortedBy { it.key.name }
                    .flatMap { (category, choices) -> choices.map { "${category.name}: ${it.label}" } },
            yearEvents =
                repository.catalog.timeline
                    .filter { it.lanternYear == settlement.lanternYear }
                    .map { it.label } +
                    listOfNotNull(settlement.settlementEvent),
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

/**
 * Whether this transition starts a fresh settlement year, so the once-per-phase actions reset.
 *
 * Committing out of the Settlement Phase ends the phase, and any transition landing on Settlement
 * step 1 opens the next year empty (a successful Showdown's aftermath or a failed hunt). Every
 * other move stays within the current year and must keep the spent phase actions.
 */
private fun FlowTransition.landsOnNewYear(): Boolean =
    when (this) {
        is FlowTransition.Commit -> true
        is FlowTransition.Advance -> to == PhaseStep(SettlementPhase.SETTLEMENT, 1)
        is FlowTransition.LoopBack -> to == PhaseStep(SettlementPhase.SETTLEMENT, 1)
        is FlowTransition.Blocked -> false
    }
