package io.kdmtracker.app

import io.kdmtracker.core.domain.Core16Milestones
import io.kdmtracker.core.domain.PrincipleCategory
import io.kdmtracker.core.domain.ResourceEntry
import io.kdmtracker.core.persistence.InMemoryCampaignStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Pins the controller's settlement-bookkeeping views and actions (Steps 4, 5, 6, 10, 11):
 * the settlement-event deck, the death/birth counters and the derived milestone views, the
 * principle/milestone/event actions, and the lantern-year advance when leaving Step 4.
 */
class SettlementBookkeepingControllerTest {
    private fun repository(): SettlementRepository {
        val repository = SettlementRepository(InMemoryCampaignStore())
        assertTrue(
            repository.catalog.settlementEventDefinitions.isNotEmpty(),
            "the seeded campaign must compose core content",
        )
        return repository
    }

    private fun controllerAt(step: Int): Pair<LanternYearController, SettlementRepository> {
        val repository = repository()
        repository.update(repository.settlement.copy(currentStep = step))
        return LanternYearController(repository) to repository
    }

    private fun LanternYearController.deathMilestone() =
        state.value.milestoneViews.single { it.id == Core16Milestones.PRINCIPLE_DEATH }

    @Test
    fun exposesTheSettlementEventDeck() {
        val controller = LanternYearController(repository())

        val deck = controller.state.value.settlementEventDeck

        assertTrue(deck.any { it.name == "Rivalidad" }, "was ${deck.map { it.name }}")
    }

    @Test
    fun recordingADeathUpdatesCountersAndView() {
        val (controller, _) = controllerAt(5)

        controller.recordDeaths(1)

        assertEquals(1, controller.state.value.deaths)
        assertEquals(3, controller.state.value.population)
        assertTrue(controller.deathMilestone().reached)
    }

    @Test
    fun recordingABirthMarksNewLifeReached() {
        val (controller, _) = controllerAt(5)

        controller.recordBirths(1)

        assertEquals(1, controller.state.value.births)
        assertEquals(5, controller.state.value.population)
        assertTrue(
            controller.state.value.milestoneViews
                .single { it.id == Core16Milestones.PRINCIPLE_NEW_LIFE }
                .reached,
        )
    }

    @Test
    fun choosingAPrincipleIsPersistedAndExposed() {
        val (controller, repository) = controllerAt(6)

        controller.choosePrinciple(PrincipleCategory.DEATH, "The First Grave → Graves")

        assertEquals("The First Grave → Graves", controller.state.value.chosenPrinciples[PrincipleCategory.DEATH])
        assertEquals(
            "The First Grave → Graves",
            repository.record.state.principles[PrincipleCategory.DEATH],
        )
    }

    @Test
    fun resolvingAReachedMilestoneMarksItResolved() {
        val (controller, _) = controllerAt(6)
        controller.recordDeaths(1)

        controller.resolveMilestone(Core16Milestones.PRINCIPLE_DEATH)

        assertTrue(controller.deathMilestone().resolved)
    }

    @Test
    fun recordingASettlementEventStoresIt() {
        val (controller, _) = controllerAt(4)

        controller.recordSettlementEvent("Rivalidad")

        assertEquals("Rivalidad", controller.state.value.settlementEvent)
    }

    @Test
    fun advancingPastStepFourIncrementsTheYearOnce() {
        val (controller, _) = controllerAt(4)

        controller.advance()
        assertEquals(2, controller.state.value.lanternYear)

        controller.previous()
        controller.advance()
        assertEquals(2, controller.state.value.lanternYear)
    }

    @Test
    fun aBadPrincipleChoiceSurfacesAnErrorAndChangesNothing() {
        val (controller, repository) = controllerAt(6)

        controller.choosePrinciple(PrincipleCategory.DEATH, "Not A Choice")

        assertNotNull(controller.state.value.actionError)
        assertTrue(
            repository.record.state.principles
                .isEmpty(),
        )
    }

    @Test
    fun stepFlowDeathPrincipleResolveFlipsEachView() {
        val (controller, _) = controllerAt(5)

        controller.recordDeaths(1)
        assertTrue(controller.deathMilestone().reached)

        controller.choosePrinciple(PrincipleCategory.DEATH, "The First Grave → Graves")
        assertEquals(
            "The First Grave → Graves",
            controller.state.value.chosenPrinciples[PrincipleCategory.DEATH],
        )

        controller.resolveMilestone(Core16Milestones.PRINCIPLE_DEATH)
        assertTrue(controller.deathMilestone().resolved)
    }

    @Test
    fun stepTenExposesStorage() {
        val (controller, _) = controllerAt(10)
        val definition =
            controller.state.value.resourceDefinitions
                .single { it.names.es == "Hueso de Monstruo" }

        controller.addResource(ResourceEntry(definition.names.es, definition.category, 3))

        assertTrue(
            controller.state.value.storageLines
                .isNotEmpty(),
        )
    }

    @Test
    fun stepElevenReportsTheUnspentEndeavors() {
        val repository = repository()
        repository.update(repository.settlement.copy(currentStep = 11, endeavors = 3))

        val controller = LanternYearController(repository)

        assertEquals(3, controller.state.value.endeavors)
    }
}
