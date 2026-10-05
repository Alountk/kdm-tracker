package io.kdmtracker.app

import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.rules.SettlementStepCatalog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LanternYearControllerTest {
    @Test
    fun initialStateIsTheOpeningSettlementStep() {
        val controller = LanternYearController(SettlementRepository())

        val state = controller.state.value

        assertEquals(Screen.Settlement, state.screen)
        assertEquals(1, state.lanternYear)
        assertEquals(SettlementPhase.SETTLEMENT, state.phase)
        assertEquals(1, state.currentStep)
        assertEquals(4, state.population)
        assertEquals("Set Up the Settlement", state.stepTitle)
        assertNull(state.lastTransition)
    }

    @Test
    fun exposesTheElevenSettlementStepsInOrder() {
        val controller = LanternYearController(SettlementRepository())

        assertEquals(SettlementStepCatalog.ALL, controller.state.value.steps)
        assertEquals(11, controller.state.value.steps.size)
    }

    @Test
    fun advancingFromStepSevenYieldsStepEightWithCatalogTitle() {
        val controller = LanternYearController(SettlementRepository())
        controller.selectStep(7)

        controller.advance()

        val state = controller.state.value
        assertEquals(8, state.currentStep)
        assertEquals(SettlementStepCatalog.byNumber(8).title, state.stepTitle)
        assertEquals(Screen.Settlement, state.screen)
        assertEquals("Advance → SETTLEMENT 8", state.lastTransition)
    }

    @Test
    fun committingTheSettlementPhaseLandsOnTheHuntScreen() {
        val controller = LanternYearController(SettlementRepository())
        controller.selectStep(11)

        controller.advance()

        val state = controller.state.value
        assertEquals(Screen.Hunt, state.screen)
        assertEquals(SettlementPhase.HUNT, state.phase)
    }

    @Test
    fun previousStepsBackWithoutCrossingPhaseBoundaries() {
        val controller = LanternYearController(SettlementRepository())
        controller.selectStep(3)

        controller.previous()

        assertEquals(2, controller.state.value.currentStep)
        assertEquals(Screen.Settlement, controller.state.value.screen)
    }

    @Test
    fun previousIsFlooredAtTheFirstStep() {
        val controller = LanternYearController(SettlementRepository())

        controller.previous()

        assertEquals(1, controller.state.value.currentStep)
    }

    @Test
    fun selectStepIgnoresNumbersOutsideTheCatalogRange() {
        val controller = LanternYearController(SettlementRepository())

        controller.selectStep(0)
        assertEquals(1, controller.state.value.currentStep)

        controller.selectStep(12)
        assertEquals(1, controller.state.value.currentStep)

        controller.selectStep(11)
        assertEquals(11, controller.state.value.currentStep)
    }
}
