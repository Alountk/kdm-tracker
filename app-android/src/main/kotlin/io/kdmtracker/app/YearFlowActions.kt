package io.kdmtracker.app

import io.kdmtracker.core.domain.HuntStatus
import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.domain.ShowdownStatus
import io.kdmtracker.core.rules.FlowContext
import io.kdmtracker.core.rules.FlowTransition
import io.kdmtracker.core.rules.LanternYearFlow
import io.kdmtracker.core.rules.PhaseStep
import io.kdmtracker.core.rules.StepResult
import io.kdmtracker.core.usecase.AdvanceStep
import io.kdmtracker.core.usecase.AdvanceTimeline
import io.kdmtracker.core.usecase.ClearShowdown
import io.kdmtracker.core.usecase.GainEndeavors
import io.kdmtracker.core.usecase.SetHuntOutcome
import io.kdmtracker.core.usecase.SetShowdownOutcome
import io.kdmtracker.core.usecase.SetSpecialShowdown

/** Phase and step transitions of the lantern-year loop; every target comes from `:core`. */
internal class YearFlowActions(
    session: ControllerSession,
) : FeatureActions(session) {
    /**
     * The step button and the showdown's end. A no-op while hunting: the hunt leaves through
     * [finishHunt] or [startShowdownOnContact], never through a stray second tap here.
     */
    fun advance() {
        if (repository.settlement.phase == SettlementPhase.HUNT) return
        val context = flowContext()
        applyStep(
            returnees = repository.settlement.population,
            context = context,
        )
        if (context.hasSpecialShowdown) {
            // The special showdown is resolved: clear it and the encounter so the phase continues
            // normally instead of looping at step 9 forever and reusing the encounter.
            applyResult(SetSpecialShowdown.run(repository.record, false))
            applyResult(ClearShowdown.run(repository.record))
        }
    }

    /** A nemesis year lists a nemesis in its timeline; a declared Special Showdown loops step 9 back. */
    private fun flowContext(): FlowContext =
        FlowContext(
            ruleset = ruleset,
            isNemesisYear = repository.record.currentNemesis() != null,
            hasSpecialShowdown = repository.record.state.specialShowdown,
        )

    /** No survivors return from a failed hunt, so the restart gets zero returnees. */
    fun failHunt() {
        applyStep(
            returnees = 0,
            context = FlowContext(ruleset = ruleset, huntFailed = true),
        )
    }

    /**
     * The one path [advance], [failHunt] and [finishHunt] take. Returnees are explicit so a total party
     * kill can express "0 returning survivors" instead of reusing the population.
     */
    private fun applyStep(
        returnees: Int,
        context: FlowContext,
    ) {
        val settlement = repository.settlement
        val fromStep = settlement.currentStep
        val result =
            AdvanceStep.run(
                settlement = settlement,
                returnedSurvivors = returnees,
                context = context,
            )
        val current = repository.record
        val moved =
            if (result.transition.landsOnNewYear(settlement.phase)) {
                current.copy(
                    settlement = result.settlement,
                    state = current.state.copy(phaseActionsUsed = emptySet()),
                )
            } else {
                current.copy(settlement = result.settlement)
            }
        repository.update(fundGainEndeavors(advanceTimelineIfNeeded(fromStep, moved)))
        session.publish(result.transition.describe())
    }

    /** No-op outside the settlement screen; the step is floored at 1. */
    fun previous() {
        val settlement = repository.settlement
        if (settlement.phase != SettlementPhase.SETTLEMENT || settlement.currentStep <= 1) return
        moveToStep(settlement.currentStep - 1)
    }

    /**
     * Gated by [LanternYearFlow.canEnterStep]: no skipping forward, revisiting earlier steps is fine.
     * A no-op outside the Settlement Phase, where the step chips have no step to move.
     */
    fun selectStep(number: Int) {
        val settlement = repository.settlement
        if (settlement.phase != SettlementPhase.SETTLEMENT) return
        val from = PhaseStep(settlement.phase, settlement.currentStep)
        if (!LanternYearFlow.canEnterStep(number, from, FlowContext(ruleset = ruleset))) return
        moveToStep(number)
    }

    /** Found or ambushed goes to the Showdown; failed or run away re-enters the Settlement Phase. */
    fun finishHunt(outcome: HuntStatus) {
        when (val result = SetHuntOutcome.run(repository.record, outcome)) {
            is StepResult.Ok -> {
                repository.update(result.value)
                when (outcome) {
                    // Found or ambushed: the showdown begins.
                    HuntStatus.QUARRY_FOUND, HuntStatus.AMBUSH -> enterShowdown()
                    // The hunt ends without a showdown: a party wipe (no returnees) or a run away
                    // (the survivors return); both re-enter the Settlement Phase.
                    HuntStatus.FAILED ->
                        applyStep(returnees = 0, context = FlowContext(ruleset = ruleset, huntFailed = true))
                    HuntStatus.RUN_AWAY ->
                        applyStep(
                            returnees = repository.settlement.population,
                            context = FlowContext(ruleset = ruleset, huntFailed = true),
                        )
                    HuntStatus.ACTIVE -> publishBlocked("invalid outcome")
                }
            }
            is StepResult.Blocked -> publishBlocked(result.reason)
        }
    }

    /**
     * The hunt ended by itself on the board (the party reached the beast or the beast reached the
     * party): the showdown begins without a separate confirmation. No-op while the hunt is active or
     * once the step has already moved on.
     */
    fun startShowdownOnContact() {
        val record = repository.record
        val status = record.state.hunt.status
        if (record.settlement.phase != SettlementPhase.HUNT) return
        if (status == HuntStatus.QUARRY_FOUND || status == HuntStatus.AMBUSH) enterShowdown()
    }

    private fun enterShowdown() =
        applyStep(
            returnees = repository.settlement.population,
            context = FlowContext(ruleset = ruleset),
        )

    /** Records the outcome, then advances out of the Showdown. */
    fun finishShowdown(status: ShowdownStatus) {
        when (val result = SetShowdownOutcome.run(repository.record, status)) {
            is StepResult.Ok -> {
                repository.update(result.value)
                advance()
            }
            is StepResult.Blocked -> publishBlocked(result.reason)
        }
    }

    private fun moveToStep(step: Int) {
        val fromStep = repository.settlement.currentStep
        val moved = repository.record.copy(settlement = repository.settlement.copy(currentStep = step))
        repository.update(fundGainEndeavors(advanceTimelineIfNeeded(fromStep, moved)))
        session.republish()
    }

    /**
     * Fires only when leaving Step 4 forward. [AdvanceTimeline] is idempotent per phase, so revisiting
     * Step 4 cannot double-increment.
     */
    private fun advanceTimelineIfNeeded(
        fromStep: Int,
        record: SettlementRecord,
    ): SettlementRecord =
        if (fromStep == UPDATE_TIMELINE_STEP && record.settlement.currentStep > UPDATE_TIMELINE_STEP) {
            AdvanceTimeline.run(record)
        } else {
            record
        }

    /**
     * Funds the pool once per Settlement Phase on entering Step 3; [GAIN_ENDEAVORS_MARKER] stops a
     * revisit from funding twice.
     */
    private fun fundGainEndeavors(record: SettlementRecord): SettlementRecord {
        val settlement = record.settlement
        if (settlement.phase != SettlementPhase.SETTLEMENT || settlement.currentStep != GAIN_ENDEAVORS_STEP) {
            return record
        }
        if (record.state.usedInPhase(GAIN_ENDEAVORS_MARKER)) return record
        val gained = GainEndeavors.run(record, returnedSurvivors = settlement.population, ruleset = ruleset)
        return gained.copy(state = gained.state.withPhaseAction(GAIN_ENDEAVORS_MARKER))
    }
}

private fun FlowTransition.describe(): String =
    when (this) {
        is FlowTransition.Advance -> "Advance → ${to.phase} ${to.step}"
        is FlowTransition.LoopBack -> "LoopBack → ${to.phase} ${to.step}"
        is FlowTransition.Commit -> "Commit → ${to.phase} ${to.step}"
        is FlowTransition.Blocked -> "Blocked: $reason"
    }

/**
 * Whether this transition out of [from] opens a fresh year, resetting once-per-phase actions.
 *
 * Scoped to the **source** phase: a mid-year move (even a hypothetical `Commit` from Hunt or
 * Showdown) keeps the spent actions.
 */
internal fun FlowTransition.landsOnNewYear(from: SettlementPhase): Boolean =
    when (this) {
        is FlowTransition.Commit -> from == SettlementPhase.SETTLEMENT
        is FlowTransition.Advance ->
            from != SettlementPhase.SETTLEMENT && to == PhaseStep(SettlementPhase.SETTLEMENT, 1)
        is FlowTransition.LoopBack ->
            from != SettlementPhase.SETTLEMENT && to == PhaseStep(SettlementPhase.SETTLEMENT, 1)
        is FlowTransition.Blocked -> false
    }

private const val GAIN_ENDEAVORS_STEP = 3

private const val UPDATE_TIMELINE_STEP = 4

/** The phase marker recording that Step 3 funded the pool, so a revisit cannot double-fund it. */
private const val GAIN_ENDEAVORS_MARKER = "Gain Endeavors"
