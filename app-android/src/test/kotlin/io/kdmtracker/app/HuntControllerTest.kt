package io.kdmtracker.app

import io.kdmtracker.core.domain.HuntSpaceKind
import io.kdmtracker.core.domain.HuntStatus
import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.persistence.InMemoryCampaignStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

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

        controller.hunt.setUpHunt("White Lion", level = 1, players = 4)

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
        controller.hunt.setUpHunt("White Lion", 1, 4)

        controller.hunt.markSpaceKind(5, HuntSpaceKind.OVERWHELMING_DARKNESS)

        assertEquals(
            HuntSpaceKind.OVERWHELMING_DARKNESS,
            repository.record.state.hunt.spaces[5]
                .kind,
        )
    }

    @Test
    fun finishingWithQuarryFoundMovesToTheShowdown() {
        val (controller, repository) = controller()
        controller.hunt.setUpHunt("White Lion", 1, 4)

        controller.flow.finishHunt(HuntStatus.QUARRY_FOUND)

        assertEquals(SettlementPhase.SHOWDOWN, controller.state.value.phase)
        assertEquals(HuntStatus.QUARRY_FOUND, repository.record.state.hunt.status)
    }

    @Test
    fun finishingWithAmbushMovesToTheShowdown() {
        val (controller, _) = controller()
        controller.hunt.setUpHunt("White Lion", 1, 4)

        controller.flow.finishHunt(HuntStatus.AMBUSH)

        assertEquals(SettlementPhase.SHOWDOWN, controller.state.value.phase)
    }

    @Test
    fun finishingWithRunAwayRestartsTheSettlement() {
        val (controller, _) = controller()
        controller.hunt.setUpHunt("White Lion", 1, 4)

        controller.flow.finishHunt(HuntStatus.RUN_AWAY)

        assertEquals(SettlementPhase.SETTLEMENT, controller.state.value.phase)
    }

    @Test
    fun finishingWithFailedRestartsTheSettlement() {
        val (controller, _) = controller()
        controller.hunt.setUpHunt("White Lion", 1, 4)

        controller.flow.finishHunt(HuntStatus.FAILED)

        assertEquals(SettlementPhase.SETTLEMENT, controller.state.value.phase)
        assertEquals(1, controller.state.value.currentStep)
    }

    @Test
    fun resolvingTheDrawnEventTakesOneAction() {
        val (controller, repository) = controller()
        controller.hunt.setUpHunt("White Lion", 1, 4)
        controller.hunt.advanceHunt()
        val drawn = repository.record.state.hunt.drawnEvent
        assertNotNull(drawn)

        controller.hunt.resolveDrawnEvent()

        val hunt = repository.record.state.hunt
        assertEquals(listOf(drawn.name), hunt.spaces[hunt.position].resolved)
    }

    @Test
    fun reachingTheBeastStartsTheShowdown() {
        val (controller, repository) = controller()
        controller.hunt.setUpHunt("White Lion", 1, 4)

        var guard = 0
        while (controller.state.value.phase == SettlementPhase.HUNT && guard++ < 20) {
            controller.hunt.advanceHunt()
            controller.hunt.resolveDrawnEvent()
        }

        assertEquals(SettlementPhase.SHOWDOWN, controller.state.value.phase)
        assertEquals(HuntStatus.QUARRY_FOUND, repository.record.state.hunt.status)
    }

    @Test
    fun theBeastReachingThePartyAmbushesIntoTheShowdown() {
        val (controller, repository) = controller()
        controller.hunt.setUpHunt("White Lion", 1, 4)

        var guard = 0
        while (controller.state.value.phase == SettlementPhase.HUNT && guard++ < 20) {
            controller.hunt.moveMonster()
        }

        assertEquals(SettlementPhase.SHOWDOWN, controller.state.value.phase)
        assertEquals(HuntStatus.AMBUSH, repository.record.state.hunt.status)
    }
}
