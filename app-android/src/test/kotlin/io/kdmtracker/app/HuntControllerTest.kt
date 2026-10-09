package io.kdmtracker.app

import io.kdmtracker.core.domain.HuntSpaceKind
import io.kdmtracker.core.domain.HuntStatus
import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.persistence.InMemoryCampaignStore
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins the controller's hunt views and the outcome→phase routing: setting up the board, editing a
 * space, and finishing a hunt (quarry found → Showdown; failed → the settlement restart).
 */
class HuntControllerTest {
    private fun controller(): Pair<LanternYearController, SettlementRepository> {
        val repository = SettlementRepository(InMemoryCampaignStore())
        repository.update(repository.settlement.copy(phase = SettlementPhase.HUNT, currentStep = 1))
        return LanternYearController(repository) to repository
    }

    @Test
    fun setUpHuntExposesThirteenSpaces() {
        val (controller, _) = controller()

        controller.setUpHunt("White Lion", level = 1, players = 4)

        assertEquals(
            13,
            controller.state.value.hunt
                ?.spaces
                ?.size,
        )
    }

    @Test
    fun markSpaceKindPersists() {
        val (controller, repository) = controller()
        controller.setUpHunt("White Lion", 1, 4)

        controller.markSpaceKind(5, HuntSpaceKind.OVERWHELMING_DARKNESS)

        assertEquals(
            HuntSpaceKind.OVERWHELMING_DARKNESS,
            repository.record.state.hunt.spaces[5]
                .kind,
        )
    }

    @Test
    fun finishingWithQuarryFoundMovesToTheShowdown() {
        val (controller, repository) = controller()
        controller.setUpHunt("White Lion", 1, 4)

        controller.finishHunt(HuntStatus.QUARRY_FOUND)

        assertEquals(SettlementPhase.SHOWDOWN, controller.state.value.phase)
        assertEquals(HuntStatus.QUARRY_FOUND, repository.record.state.hunt.status)
    }

    @Test
    fun finishingWithAmbushMovesToTheShowdown() {
        val (controller, _) = controller()
        controller.setUpHunt("White Lion", 1, 4)

        controller.finishHunt(HuntStatus.AMBUSH)

        assertEquals(SettlementPhase.SHOWDOWN, controller.state.value.phase)
    }

    @Test
    fun finishingWithRunAwayRestartsTheSettlement() {
        val (controller, _) = controller()
        controller.setUpHunt("White Lion", 1, 4)

        controller.finishHunt(HuntStatus.RUN_AWAY)

        assertEquals(SettlementPhase.SETTLEMENT, controller.state.value.phase)
    }

    @Test
    fun finishingWithFailedRestartsTheSettlement() {
        val (controller, _) = controller()
        controller.setUpHunt("White Lion", 1, 4)

        controller.finishHunt(HuntStatus.FAILED)

        assertEquals(SettlementPhase.SETTLEMENT, controller.state.value.phase)
        assertEquals(1, controller.state.value.currentStep)
    }
}
