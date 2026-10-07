package io.kdmtracker.app

import io.kdmtracker.core.content.Core16Content
import io.kdmtracker.core.domain.Innovation
import io.kdmtracker.core.domain.InnovationCollection
import io.kdmtracker.core.domain.LocationSet
import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.persistence.InMemoryCampaignStore
import io.kdmtracker.core.sync.CampaignSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pins the controller's Step 1 (Set Up) view: the derived Innovation Deck and the built
 * locations it exposes, the unchanged advance behavior, the once-per-phase reset at a year
 * boundary, and the seeded campaign composing real content (Ruling CC).
 *
 * Everything is JVM-only: the deck and locations come from the held [SettlementRecord], so the
 * scenarios copy them onto the record directly rather than driving the UI.
 */
class SetUpStepControllerTest {
    private fun repository(): SettlementRepository {
        val repository = SettlementRepository(InMemoryCampaignStore())
        assertTrue(
            repository.catalog.locationDefinitions.isNotEmpty(),
            "the seeded campaign must compose core content",
        )
        return repository
    }

    private fun controllerAt(
        phase: SettlementPhase,
        step: Int,
    ): LanternYearController {
        val repository = repository()
        repository.update(repository.settlement.copy(phase = phase, currentStep = step))
        return LanternYearController(repository)
    }

    @Test
    fun exposesTheDerivedInnovationDeckForStepOne() {
        val repository = repository()
        repository.update(
            repository.record.copy(
                state =
                    repository.record.state.copy(
                        innovations =
                            InnovationCollection(
                                listOf(
                                    Innovation("Language", "speech", consequences = listOf("Storytelling")),
                                    Innovation("Symposium", "debate", consequences = listOf("Storytelling", "Paint")),
                                ),
                            ),
                    ),
            ),
        )

        val deck = LanternYearController(repository).state.value.innovationDeck

        assertEquals(listOf("Paint", "Storytelling"), deck)
    }

    @Test
    fun exposesTheBuiltLocationsForStepOne() {
        val repository = repository()
        repository.update(
            repository.record.copy(
                state =
                    repository.record.state.copy(
                        locations = LocationSet(built = listOf("Lantern Hoard", "Bone Smith")),
                    ),
            ),
        )

        val built = LanternYearController(repository).state.value.builtLocations

        assertEquals(listOf("Lantern Hoard", "Bone Smith"), built)
    }

    @Test
    fun exposesThePrinciplesAndThisYearsEventsForStepOne() {
        val state = LanternYearController(repository()).state.value

        assertTrue(state.principles.isNotEmpty(), "the core ruleset exposes principle choices")
        assertTrue(state.yearEvents.isNotEmpty(), "lantern year 1 has a timeline entry")
    }

    @Test
    fun advancingStillBehavesAsBefore() {
        val controller = controllerAt(SettlementPhase.SETTLEMENT, 11)

        controller.advance()

        val state = controller.state.value
        assertEquals(Screen.Hunt, state.screen)
        assertEquals(SettlementPhase.HUNT, state.phase)
        assertEquals(1, state.currentStep)
    }

    @Test
    fun committingOutOfTheSettlementPhaseClearsThePhaseActionsUsed() {
        val repository = repository()
        repository.update(
            repository.record.copy(
                settlement = repository.settlement.copy(phase = SettlementPhase.SETTLEMENT, currentStep = 11),
                state = repository.record.state.copy(phaseActionsUsed = setOf("Innovate")),
            ),
        )
        val controller = LanternYearController(repository)

        controller.advance()

        assertEquals(
            emptySet(),
            controller.state.value.record
                ?.state
                ?.phaseActionsUsed,
        )
    }

    @Test
    fun returningToSettlementStepOneClearsThePhaseActionsUsed() {
        val repository = repository()
        repository.update(
            repository.record.copy(
                settlement = repository.settlement.copy(phase = SettlementPhase.SHOWDOWN, currentStep = 1),
                state = repository.record.state.copy(phaseActionsUsed = setOf("Innovate")),
            ),
        )
        val controller = LanternYearController(repository)

        controller.advance()

        val state = controller.state.value
        assertEquals(SettlementPhase.SETTLEMENT, state.phase)
        assertEquals(1, state.currentStep)
        assertEquals(emptySet(), state.record?.state?.phaseActionsUsed)
    }

    @Test
    fun aStepWithinTheSettlementPhaseKeepsThePhaseActionsUsed() {
        val repository = repository()
        repository.update(
            repository.record.copy(
                state = repository.record.state.copy(phaseActionsUsed = setOf("Innovate")),
            ),
        )
        val controller = LanternYearController(repository)

        controller.advance()

        assertEquals(2, controller.state.value.currentStep)
        assertEquals(
            setOf("Innovate"),
            controller.state.value.record
                ?.state
                ?.phaseActionsUsed,
        )
    }

    @Test
    fun aSeededRepositoryComposesANonEmptyCatalog() {
        val repository = SettlementRepository(InMemoryCampaignStore())

        assertEquals(listOf(Core16Content.pack.id), repository.campaign.enabledPacks)
        assertTrue(repository.catalog.locationDefinitions.isNotEmpty())
        assertTrue(repository.catalog.innovationDefinitions.isNotEmpty())
        assertNull(repository.catalogError)
    }

    @Test
    fun aCreatedCampaignComposesANonEmptyCatalog() {
        val repository =
            SettlementRepository(
                InMemoryCampaignStore(),
                createCampaign = { name ->
                    CampaignSummary(Ulid.generate(7L) { 7 }.value, name, "OWNER", "", emptyList())
                },
            )

        repository.create("Gloom")

        assertEquals(listOf(Core16Content.pack.id), repository.campaign.enabledPacks)
        assertTrue(repository.catalog.locationDefinitions.isNotEmpty())
    }
}
