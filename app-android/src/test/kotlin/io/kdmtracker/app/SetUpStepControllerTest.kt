package io.kdmtracker.app

import io.kdmtracker.core.content.Core16Content
import io.kdmtracker.core.content.Legacy15Content
import io.kdmtracker.core.domain.HuntStatus
import io.kdmtracker.core.domain.Innovation
import io.kdmtracker.core.domain.InnovationCollection
import io.kdmtracker.core.domain.InnovationId
import io.kdmtracker.core.domain.LocalizedText
import io.kdmtracker.core.domain.LocationId
import io.kdmtracker.core.domain.LocationSet
import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.persistence.InMemoryCampaignStore
import io.kdmtracker.core.rules.FlowTransition
import io.kdmtracker.core.rules.PhaseStep
import io.kdmtracker.core.sync.CampaignSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
                                    Innovation(
                                        InnovationId("language"),
                                        LocalizedText(es = "Language"),
                                        "speech",
                                        consequences = listOf("Storytelling"),
                                    ),
                                    Innovation(
                                        InnovationId("symposium"),
                                        LocalizedText(es = "Symposium"),
                                        "debate",
                                        consequences = listOf("Storytelling", "Paint"),
                                    ),
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
                        locations = LocationSet(built = listOf(LocationId("lantern_hoard"), LocationId("bone_smith"))),
                    ),
            ),
        )

        val built = LanternYearController(repository).state.value.builtLocations

        assertEquals(listOf("Cúmulo de Linternas", "Taller de Huesos"), built)
    }

    @Test
    fun exposesThePrinciplesAndThisYearsEventsForStepOne() {
        val state = LanternYearController(repository()).state.value

        assertTrue(state.principles.isNotEmpty(), "the core ruleset exposes principle choices")
        assertTrue(state.yearEvents.isNotEmpty(), "lantern year 1 has a timeline entry")
    }

    @Test
    fun thePrincipleListShowsTheChoiceLabelsNotTheCategoryPrefix() {
        // The controller used to hand-build "DEATH: The First Harvest → Cannibalize"; the label
        // is domain content and the section shows it bare.
        val state = LanternYearController(repository()).state.value

        assertEquals(
            listOf(
                "The First Harvest → Cannibalize",
                "The First Grave → Graves",
                "Without Mercy → Survival of the Fittest",
                "With Affection → Protect the Young",
                "Collective Toil",
                "Accept Darkness",
            ),
            state.principles,
        )
    }

    @Test
    fun advancingStillBehavesAsBefore() {
        val controller = controllerAt(SettlementPhase.SETTLEMENT, 11)

        controller.flow.advance()

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

        controller.flow.advance()

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

        controller.flow.advance()

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

        controller.flow.advance()

        assertEquals(2, controller.state.value.currentStep)
        assertEquals(
            setOf("Innovate"),
            controller.state.value.record
                ?.state
                ?.phaseActionsUsed,
        )
    }

    @Test
    fun aHuntToShowdownAdvanceKeepsThePhaseActionsUsed() {
        val repository = repository()
        repository.update(
            repository.record.copy(
                settlement = repository.settlement.copy(phase = SettlementPhase.HUNT, currentStep = 1),
                state = repository.record.state.copy(phaseActionsUsed = setOf("Innovate")),
            ),
        )
        val controller = LanternYearController(repository)
        controller.hunt.setUpHunt("White Lion", 1, 4)

        controller.flow.finishHunt(HuntStatus.QUARRY_FOUND)

        val state = controller.state.value
        assertEquals(SettlementPhase.SHOWDOWN, state.phase)
        assertEquals(
            setOf("Innovate"),
            state.record
                ?.state
                ?.phaseActionsUsed,
        )
    }

    @Test
    fun aMidYearCommitDoesNotStartANewYear() {
        val midYearCommit = FlowTransition.Commit(PhaseStep(SettlementPhase.HUNT, 1), 0)

        assertFalse(midYearCommit.landsOnNewYear(SettlementPhase.HUNT))
        assertFalse(midYearCommit.landsOnNewYear(SettlementPhase.SHOWDOWN))
    }

    @Test
    fun aSettlementPhaseCommitStartsANewYear() {
        val commit = FlowTransition.Commit(PhaseStep(SettlementPhase.HUNT, 1), 0)

        assertTrue(commit.landsOnNewYear(SettlementPhase.SETTLEMENT))
    }

    @Test
    fun returningToSettlementStepOneStartsANewYear() {
        val aftermath = FlowTransition.Advance(PhaseStep(SettlementPhase.SETTLEMENT, 1))

        assertTrue(aftermath.landsOnNewYear(SettlementPhase.SHOWDOWN))
        assertTrue(aftermath.landsOnNewYear(SettlementPhase.HUNT))
        assertFalse(aftermath.landsOnNewYear(SettlementPhase.SETTLEMENT))
    }

    @Test
    fun aSeededRepositoryComposesANonEmptyCatalog() {
        val repository = SettlementRepository(InMemoryCampaignStore())

        assertEquals(listOf("core-1.6", "core-1.5"), repository.campaign.enabledPacks)
        assertEquals(listOf(Core16Content.pack.id, Legacy15Content.pack.id), repository.campaign.enabledPacks)
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

        assertEquals(listOf("core-1.6", "core-1.5"), repository.campaign.enabledPacks)
        assertEquals(listOf(Core16Content.pack.id, Legacy15Content.pack.id), repository.campaign.enabledPacks)
        assertTrue(repository.catalog.locationDefinitions.isNotEmpty())
    }
}
