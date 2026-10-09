package io.kdmtracker.app

import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.domain.Timeline
import io.kdmtracker.core.domain.TimelineEntry
import io.kdmtracker.core.persistence.InMemoryCampaignStore
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins the settlement-flow branches: a nemesis year commits to the Showdown (skipping the Hunt),
 * a plain year to the Hunt, and a declared Special Showdown at step 9 loops back to step 5.
 */
class SettlementFlowBranchControllerTest {
    private fun controller(
        year: Int,
        step: Int,
        nemesis: Boolean = false,
        special: Boolean = false,
    ): Pair<LanternYearController, SettlementRepository> {
        val repository = SettlementRepository(InMemoryCampaignStore())
        val settlement =
            repository.settlement.copy(
                lanternYear = year,
                phase = SettlementPhase.SETTLEMENT,
                currentStep = step,
            )
        var state = repository.record.state
        if (nemesis) {
            state = state.copy(timeline = Timeline(entries = listOf(TimelineEntry(year, "Nemesis: The Butcher Lvl 2"))))
        }
        if (special) {
            state = state.copy(specialShowdown = true)
        }
        repository.update(repository.record.copy(settlement = settlement, state = state))
        return LanternYearController(repository) to repository
    }

    @Test
    fun nemesisYearAdvancesToTheShowdown() {
        val (controller, _) = controller(year = 16, step = 11, nemesis = true)

        controller.advance()

        assertEquals(SettlementPhase.SHOWDOWN, controller.state.value.phase)
    }

    @Test
    fun plainYearAdvancesToTheHunt() {
        val (controller, _) = controller(year = 1, step = 11)

        controller.advance()

        assertEquals(SettlementPhase.HUNT, controller.state.value.phase)
    }

    @Test
    fun specialShowdownLoopsBackToTheDeathCountStep() {
        val (controller, _) = controller(year = 9, step = 9, special = true)

        controller.advance()

        assertEquals(SettlementPhase.SETTLEMENT, controller.state.value.phase)
        assertEquals(5, controller.state.value.currentStep)
    }

    @Test
    fun declaringASpecialShowdownPersists() {
        val (controller, repository) = controller(year = 9, step = 9)

        controller.declareSpecialShowdown(true)

        assertEquals(true, repository.record.state.specialShowdown)
    }
}
