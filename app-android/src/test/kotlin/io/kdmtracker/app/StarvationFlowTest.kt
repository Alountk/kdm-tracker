package io.kdmtracker.app

import io.kdmtracker.core.domain.HuntStatus
import io.kdmtracker.core.domain.ResourceEntry
import io.kdmtracker.core.domain.ResourceId
import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.persistence.InMemoryCampaignStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Starvation through the real controller (rulebook "Fase de Caza", pp. 61-63): a failed hunt, or one
 * that ends on the starvation space, costs a d5 of stored resources, and the player picks which.
 */
class StarvationFlowTest {
    private val bone = ResourceId("bone.monster")
    private val hide = ResourceId("hide.monster")
    private val repository = SettlementRepository(InMemoryCampaignStore())
    private val controller = LanternYearController(repository)

    private val state get() = controller.state.value
    private val stored get() = state.storageLines.sumOf { it.quantity }

    private fun huntWithAFullStorage() {
        controller.settlement.addResource(ResourceEntry(bone, 6))
        controller.settlement.addResource(ResourceEntry(hide, 6))
        while (state.currentStep < 11) controller.flow.advance()
        controller.chooseTheDepartingParty()
        controller.flow.advance()
        assertEquals(SettlementPhase.HUNT, state.phase)
        controller.hunt.setUpHunt("White Lion", 1, 4)
    }

    @Test
    fun aFailedHuntOwesBetweenOneAndFiveResourcesAndTheStorageWaitsForThePlayer() {
        huntWithAFullStorage()

        controller.flow.finishHunt(HuntStatus.FAILED)

        assertEquals(SettlementPhase.SETTLEMENT, state.phase)
        assertTrue(state.starvationOwed in 1..5, "owes ${state.starvationOwed}")
        assertEquals(12, stored, "the app never discards on its own")
    }

    @Test
    fun thePlayerPaysTheDebtWithTheResourcesTheyChoose() {
        huntWithAFullStorage()
        controller.flow.finishHunt(HuntStatus.FAILED)
        val owed = state.starvationOwed
        assertTrue(owed >= 1)

        repeat(owed) { controller.settlement.discardForStarvation(hide) }

        assertEquals(0, state.starvationOwed)
        assertEquals(12 - owed, stored)
        assertEquals(6, state.storageLines.single { it.id == bone }.quantity, "only the chosen resource is lost")
        controller.settlement.discardForStarvation(hide)
        assertEquals(12 - owed, stored, "a paid debt discards nothing more")
    }

    @Test
    fun theDebtSurvivesClosingTheApp() {
        huntWithAFullStorage()
        controller.flow.finishHunt(HuntStatus.FAILED)
        val owed = state.starvationOwed
        assertTrue(owed >= 1)

        val reopened = LanternYearController(repository)

        assertEquals(owed, reopened.state.value.starvationOwed)
    }

    @Test
    fun aHuntThatEndsOnTheStarvationSpaceOwesTheSameLoss() {
        huntWithAFullStorage()
        repeat(8) { controller.hunt.retreatMonster() }
        var steps = 0
        while (state.phase == SettlementPhase.HUNT && steps++ < 20) {
            controller.hunt.advanceHunt()
            controller.hunt.resolveDrawnEvent()
        }

        assertEquals(SettlementPhase.SHOWDOWN, state.phase)
        assertEquals(12, repository.record.state.hunt.position)
        assertTrue(state.starvationOwed in 1..5, "owes ${state.starvationOwed}")
    }

    @Test
    fun aHuntThatEndsAnywhereElseCostsNothing() {
        huntWithAFullStorage()

        controller.flow.finishHunt(HuntStatus.RUN_AWAY)
        assertEquals(0, state.starvationOwed, "running away does not starve the settlement")
    }

    @Test
    fun findingTheQuarryBeforeTheLastSpaceCostsNothing() {
        huntWithAFullStorage()
        var steps = 0
        while (state.phase == SettlementPhase.HUNT && steps++ < 20) {
            controller.hunt.advanceHunt()
            controller.hunt.resolveDrawnEvent()
        }

        assertEquals(SettlementPhase.SHOWDOWN, state.phase)
        assertEquals(0, state.starvationOwed)
    }
}
