package io.kdmtracker.core.usecase

import io.kdmtracker.core.content.ContentBundle
import io.kdmtracker.core.content.ContentCatalog
import io.kdmtracker.core.content.ContentPack
import io.kdmtracker.core.content.resourceDefinition
import io.kdmtracker.core.domain.ActionKind
import io.kdmtracker.core.domain.HybridLogicalClock
import io.kdmtracker.core.domain.Innovation
import io.kdmtracker.core.domain.InnovationCollection
import io.kdmtracker.core.domain.InnovationId
import io.kdmtracker.core.domain.LocalizedText
import io.kdmtracker.core.domain.LocationId
import io.kdmtracker.core.domain.LocationSet
import io.kdmtracker.core.domain.ResourceCategory
import io.kdmtracker.core.domain.ResourceEntry
import io.kdmtracker.core.domain.ResourceId
import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.domain.SettlementAction
import io.kdmtracker.core.domain.SettlementLocation
import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.domain.SettlementState
import io.kdmtracker.core.domain.SyncMeta
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.rules.StepResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class InnovateUseCaseTest {
    private val innovateAction =
        SettlementAction(
            names = LocalizedText(es = "Innovar", en = "Innovate"),
            kind = ActionKind.INNOVATE,
            endeavorCost = 1,
            resourceCost = mapOf(ResourceId("Bone") to 1, ResourceId("Organ") to 1, ResourceId("Hide") to 1),
            oncePerPhase = true,
            draw = 2,
            keep = 1,
        )

    private val amoniaco =
        Innovation(
            id = InnovationId("amoniaco"),
            names = LocalizedText(es = "Amoníaco"),
            description = "Acids for tanning.",
        )

    /**
     * The fixture's deck root: unlocked by the default state, it lists [amoniaco] among its
     * consequences, which is what puts that card in the deck. The consequence string is the
     * Spanish name because [io.kdmtracker.core.content.ContentIdentity] resolves deck cards
     * through that table, exactly as the shipped catalog does.
     */
    private val startingPrinciple =
        Innovation(
            id = InnovationId("starting_principle"),
            names = LocalizedText(es = "Principio Inicial", en = "Starting Principle"),
            description = "The root the deck grows from.",
            consequences = listOf("Amoníaco"),
        )

    private val catalog =
        ContentCatalog(
            listOf(
                ContentPack(
                    id = "test",
                    version = "1.0.0",
                    name = "Test",
                    bundle =
                        ContentBundle(
                            innovations = listOf(startingPrinciple, amoniaco),
                            locations =
                                listOf(
                                    SettlementLocation(
                                        LocationId("lantern_hoard"),
                                        LocalizedText(es = "Lantern Hoard"),
                                        actions = listOf(innovateAction),
                                    ),
                                    SettlementLocation(LocationId("stone_circle"), LocalizedText(es = "Stone Circle")),
                                ),
                            resources =
                                listOf(
                                    resourceDefinition("Bone", ResourceCategory.BASIC),
                                    resourceDefinition("Organ", ResourceCategory.BASIC),
                                    resourceDefinition("Hide", ResourceCategory.BASIC),
                                ),
                        ),
                ),
            ),
        )

    private fun record(
        endeavors: Int = 1,
        lanternYear: Int = 1,
        state: SettlementState = SettlementState(),
    ): SettlementRecord =
        SettlementRecord(
            settlement =
                Settlement(
                    id = Ulid.generate(1L) { 0 },
                    campaignId = Ulid.generate(2L) { 0 },
                    name = "Kingsport",
                    lanternYear = lanternYear,
                    phase = SettlementPhase.SETTLEMENT,
                    currentStep = 7,
                    endeavors = endeavors,
                    population = 4,
                    settlementEvent = null,
                    sync = SyncMeta(HybridLogicalClock(0, 0)),
                ),
            state = state,
        )

    private fun stocked(): SettlementState =
        SettlementState(
            locations = LocationSet(built = listOf(LocationId("lantern_hoard"))),
            innovations = InnovationCollection(listOf(startingPrinciple)),
            resourceEntries =
                listOf(
                    ResourceEntry(ResourceId("Bone"), 2),
                    ResourceEntry(ResourceId("Organ"), 2),
                    ResourceEntry(ResourceId("Hide"), 2),
                ),
        )

    private fun builtButUnstocked(): SettlementState =
        SettlementState(
            locations = LocationSet(built = listOf(LocationId("lantern_hoard"))),
            innovations = InnovationCollection(listOf(startingPrinciple)),
        )

    @Test
    fun aValidInnovateSpendsTheCostAddsTheInnovationAndMarksThePhase() {
        val result =
            Innovate.run(
                record(state = stocked()),
                LocationId("lantern_hoard"),
                InnovationId("amoniaco"),
                catalog,
            )

        val ok = assertIs<StepResult.Ok<SettlementRecord>>(result)
        assertEquals(0, ok.value.settlement.endeavors)
        assertTrue(
            ok.value.state.innovations.innovations
                .any { it.names.es == "Amoníaco" },
        )
        assertTrue(ok.value.state.usedInPhase("lantern_hoard"))
        for (name in listOf("Bone", "Organ", "Hide")) {
            val line = ok.value.storage().single { it.id.value == name }
            assertEquals(1, line.quantity)
        }
    }

    @Test
    fun aSecondInnovateInTheSamePhaseIsBlocked() {
        val first =
            assertIs<StepResult.Ok<SettlementRecord>>(
                Innovate.run(record(state = stocked()), LocationId("lantern_hoard"), InnovationId("amoniaco"), catalog),
            )

        val second = Innovate.run(first.value, LocationId("lantern_hoard"), InnovationId("amoniaco"), catalog)

        assertIs<StepResult.Blocked>(second)
    }

    @Test
    fun insufficientStorageIsBlocked() {
        val result =
            Innovate.run(
                record(state = builtButUnstocked()),
                LocationId("lantern_hoard"),
                InnovationId("amoniaco"),
                catalog,
            )

        assertIs<StepResult.Blocked>(result)
    }

    @Test
    fun anUnbuiltInnovateLocationIsBlocked() {
        val unbuilt = stocked().copy(locations = LocationSet())

        val result =
            Innovate.run(
                record(state = unbuilt),
                LocationId("lantern_hoard"),
                InnovationId("amoniaco"),
                catalog,
            )

        assertIs<StepResult.Blocked>(result)
    }

    @Test
    fun aLocationWithoutAnInnovateActionIsBlocked() {
        val result =
            Innovate.run(
                record(state = stocked()),
                LocationId("stone_circle"),
                InnovationId("amoniaco"),
                catalog,
            )

        assertIs<StepResult.Blocked>(result)
    }

    @Test
    fun anUnknownLocationIsBlocked() {
        val result = Innovate.run(record(state = stocked()), LocationId("nowhere"), InnovationId("amoniaco"), catalog)

        assertIs<StepResult.Blocked>(result)
    }

    @Test
    fun anUnknownInnovationIsBlocked() {
        val result =
            Innovate.run(
                record(state = stocked()),
                LocationId("lantern_hoard"),
                InnovationId("alchemy"),
                catalog,
            )

        assertIs<StepResult.Blocked>(result)
    }

    @Test
    fun insufficientEndeavorsIsBlocked() {
        val result =
            Innovate.run(
                record(endeavors = 0, state = stocked()),
                LocationId("lantern_hoard"),
                InnovationId("amoniaco"),
                catalog,
            )

        assertIs<StepResult.Blocked>(result)
    }

    @Test
    fun aValidInnovateStampsTheUnlockedYear() {
        val result =
            Innovate.run(
                record(lanternYear = 3, state = stocked()),
                LocationId("lantern_hoard"),
                InnovationId("amoniaco"),
                catalog,
            )

        val ok = assertIs<StepResult.Ok<SettlementRecord>>(result)
        assertEquals(
            3,
            ok.value.state.innovations.innovations
                .single { it.id == InnovationId("amoniaco") }
                .unlockedInYear,
        )
    }

    @Test
    fun innovatingACardThatIsNotInTheDeckIsBlocked() {
        val state = stocked().copy(innovations = InnovationCollection())

        val result =
            Innovate.run(
                record(state = state),
                LocationId("lantern_hoard"),
                InnovationId("amoniaco"),
                catalog,
            )

        val blocked = assertIs<StepResult.Blocked>(result)
        assertTrue(
            blocked.reason.contains("not available"),
            "the refusal names the missing access: ${blocked.reason}",
        )
    }

    @Test
    fun innovatingAnAlreadyUnlockedCardIsBlocked() {
        val state = stocked().copy(innovations = InnovationCollection(listOf(amoniaco)))

        val result =
            Innovate.run(
                record(state = state),
                LocationId("lantern_hoard"),
                InnovationId("amoniaco"),
                catalog,
            )

        val blocked = assertIs<StepResult.Blocked>(result)
        assertTrue(
            blocked.reason.contains("already unlocked"),
            "the refusal names the duplicate unlock: ${blocked.reason}",
        )
    }
}
