package io.kdmtracker.app

import io.kdmtracker.core.domain.SettlementPhase
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins the full lantern-year loop end to end: Settlement step 11 → Hunt → Showdown → Settlement
 * step 1, plus the hunt-failure branch back to Settlement step 1.
 *
 * Every transition is produced by `:core` [io.kdmtracker.core.rules.LanternYearFlow] via
 * [io.kdmtracker.core.usecase.AdvanceStep]; the controller only mirrors the result. Scenarios are
 * built by copying the seeded settlement into the starting phase/step directly so the tests never
 * depend on the loop itself to reach their starting point.
 */
class LanternYearLoopTest {
    private fun controllerAt(
        phase: SettlementPhase,
        step: Int,
    ): LanternYearController {
        val repository = SettlementRepository()
        repository.update(repository.settlement.copy(phase = phase, currentStep = step))
        return LanternYearController(repository)
    }

    @Test
    fun advancingFromTheFinalSettlementStepOpensTheHunt() {
        val controller = controllerAt(SettlementPhase.SETTLEMENT, 11)

        controller.advance()

        val state = controller.state.value
        assertEquals(Screen.Hunt, state.screen)
        assertEquals(SettlementPhase.HUNT, state.phase)
        assertEquals(1, state.currentStep)
    }

    @Test
    fun aSuccessfulHuntAdvancesToTheShowdown() {
        val controller = controllerAt(SettlementPhase.HUNT, 1)

        controller.advance()

        val state = controller.state.value
        assertEquals(Screen.Showdown, state.screen)
        assertEquals(SettlementPhase.SHOWDOWN, state.phase)
        assertEquals(1, state.currentStep)
    }

    @Test
    fun theShowdownAftermathRestartsTheSettlementAtStepOne() {
        val controller = controllerAt(SettlementPhase.SHOWDOWN, 1)
        val yearBefore = controller.state.value.lanternYear

        controller.advance()

        val state = controller.state.value
        assertEquals(Screen.Settlement, state.screen)
        assertEquals(SettlementPhase.SETTLEMENT, state.phase)
        assertEquals(1, state.currentStep)
        assertEquals(yearBefore, state.lanternYear)
    }

    @Test
    fun aFailedHuntRestartsTheSettlementAtStepOne() {
        val controller = controllerAt(SettlementPhase.HUNT, 1)

        controller.failHunt()

        val state = controller.state.value
        assertEquals(Screen.Settlement, state.screen)
        assertEquals(SettlementPhase.SETTLEMENT, state.phase)
        assertEquals(1, state.currentStep)
    }

    @Test
    fun aFailedHuntDiscardsUnspentEndeavors() {
        val repository = SettlementRepository()
        repository.update(
            repository.settlement.copy(
                phase = SettlementPhase.HUNT,
                currentStep = 1,
                endeavors = 3,
            ),
        )
        val controller = LanternYearController(repository)

        controller.failHunt()

        val state = controller.state.value
        assertEquals(SettlementPhase.SETTLEMENT, state.phase)
        assertEquals(1, state.currentStep)
        assertEquals(0, state.endeavors)
    }
}
