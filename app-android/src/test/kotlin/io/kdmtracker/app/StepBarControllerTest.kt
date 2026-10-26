package io.kdmtracker.app

import io.kdmtracker.core.persistence.InMemoryCampaignStore
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Pins the reachability the top step bar renders: the current step is reachable, and steps ahead
 * cannot be skipped to (they stay disabled).
 */
class StepBarControllerTest {
    @Test
    fun theCurrentStepIsReachableAndAFarForwardStepIsNot() {
        val controller = LanternYearController(SettlementRepository(InMemoryCampaignStore()))
        controller.flow.advance()
        controller.flow.advance()

        val state = controller.state.value

        assertTrue(state.currentStep in state.reachableSteps)
        assertTrue(state.currentStep + 5 !in state.reachableSteps)
    }
}
