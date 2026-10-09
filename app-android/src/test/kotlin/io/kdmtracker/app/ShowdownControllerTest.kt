package io.kdmtracker.app

import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.domain.ShowdownStatus
import io.kdmtracker.core.persistence.InMemoryCampaignStore
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins the controller's showdown views and routing: setting up an encounter, drawing/wounding, and
 * finishing (victory/defeat → the Settlement restart).
 */
class ShowdownControllerTest {
    private fun controller(): Pair<LanternYearController, SettlementRepository> {
        val repository = SettlementRepository(InMemoryCampaignStore())
        repository.update(repository.settlement.copy(phase = SettlementPhase.SHOWDOWN, currentStep = 1))
        return LanternYearController(repository) to repository
    }

    @Test
    fun setUpShowdownExposesTheEncounter() {
        val (controller, _) = controller()

        controller.setUpShowdown("White Lion", level = 1, deck = listOf("A", "B"))

        assertEquals(
            "White Lion",
            controller.state.value.showdown
                ?.monster,
        )
        assertEquals(
            2,
            controller.state.value.showdown
                ?.aiDeck
                ?.size,
        )
    }

    @Test
    fun drawingAndWoundingUpdateTheDeck() {
        val (controller, _) = controller()
        controller.setUpShowdown("White Lion", 1, listOf("A", "B"))

        controller.drawShowdownCard()
        assertEquals(
            "A",
            controller.state.value.showdown
                ?.lastDrawn,
        )

        controller.woundMonster()
        assertEquals(
            emptyList(),
            controller.state.value.showdown
                ?.aiDeck,
        )
        assertEquals(
            listOf("B"),
            controller.state.value.showdown
                ?.wounds,
        )
    }

    @Test
    fun finishingWithVictoryReturnsToTheSettlement() {
        val (controller, repository) = controller()
        controller.setUpShowdown("White Lion", 1, listOf("A"))

        controller.finishShowdown(ShowdownStatus.VICTORY)

        assertEquals(SettlementPhase.SETTLEMENT, controller.state.value.phase)
        assertEquals(1, controller.state.value.currentStep)
        assertEquals(ShowdownStatus.VICTORY, repository.record.state.showdown.status)
    }

    @Test
    fun finishingWithDefeatReturnsToTheSettlement() {
        val (controller, _) = controller()
        controller.setUpShowdown("White Lion", 1, listOf("A"))

        controller.finishShowdown(ShowdownStatus.DEFEAT)

        assertEquals(SettlementPhase.SETTLEMENT, controller.state.value.phase)
    }
}
