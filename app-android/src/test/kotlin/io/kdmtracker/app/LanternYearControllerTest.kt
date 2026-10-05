package io.kdmtracker.app

import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.rules.SettlementStepCatalog
import io.kdmtracker.core.ruleset.Core16Ruleset
import io.kdmtracker.core.ruleset.Ruleset
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
    fun derivesStepsAndTitleFromTheInjectedRuleset() {
        val controller = LanternYearController(SettlementRepository(), SingleStepRuleset())

        val state = controller.state.value

        assertEquals(SingleStepRuleset().settlementSteps, state.steps)
        assertEquals(SingleStepRuleset().settlementSteps.single().title, state.stepTitle)
    }

    @Test
    fun advancingFromStepSevenYieldsStepEightWithCatalogTitle() {
        val controller = controllerAt(7)

        controller.advance()

        val state = controller.state.value
        assertEquals(8, state.currentStep)
        assertEquals(SettlementStepCatalog.byNumber(8).title, state.stepTitle)
        assertEquals(Screen.Settlement, state.screen)
        assertEquals("Advance → SETTLEMENT 8", state.lastTransition)
    }

    @Test
    fun committingTheSettlementPhaseLandsOnTheHuntScreen() {
        val controller = controllerAt(11)

        controller.advance()

        val state = controller.state.value
        assertEquals(Screen.Hunt, state.screen)
        assertEquals(SettlementPhase.HUNT, state.phase)
    }

    @Test
    fun previousStepsBackWithoutCrossingPhaseBoundaries() {
        val controller = controllerAt(3)

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
    fun selectStepAppliesTheNextStep() {
        val controller = LanternYearController(SettlementRepository())

        controller.selectStep(2)

        assertEquals(2, controller.state.value.currentStep)
    }

    @Test
    fun selectStepIgnoresAStepThatSkipsForward() {
        val controller = LanternYearController(SettlementRepository())

        controller.selectStep(11)

        assertEquals(1, controller.state.value.currentStep)
    }

    @Test
    fun selectStepAllowsRevisitingAnEarlierStep() {
        val controller = controllerAt(5)

        controller.selectStep(3)

        assertEquals(3, controller.state.value.currentStep)
    }

    @Test
    fun selectStepIgnoresNumbersOutsideTheRulesetRange() {
        val controller = LanternYearController(SettlementRepository())

        controller.selectStep(0)
        assertEquals(1, controller.state.value.currentStep)

        controller.selectStep(12)
        assertEquals(1, controller.state.value.currentStep)
    }

    @Test
    fun reachableStepsExposeOnlyEnterableSteps() {
        val controller = LanternYearController(SettlementRepository())

        assertEquals(setOf(1, 2), controller.state.value.reachableSteps)
    }

    private fun controllerAt(step: Int): LanternYearController {
        val repository = SettlementRepository()
        repository.update(repository.settlement.copy(currentStep = step))
        return LanternYearController(repository)
    }

    /** A ruleset exposing a single settlement step, to prove the UI derives from the injected one. */
    private class SingleStepRuleset : Ruleset by Core16Ruleset {
        override val settlementSteps = listOf(Core16Ruleset.settlementSteps.first())
    }
}
