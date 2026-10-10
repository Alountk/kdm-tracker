package io.kdmtracker.core.usecase

import io.kdmtracker.core.content.ContentBundle
import io.kdmtracker.core.content.ContentCatalog
import io.kdmtracker.core.content.ContentPack
import io.kdmtracker.core.content.resourceDefinition
import io.kdmtracker.core.domain.ActionKind
import io.kdmtracker.core.domain.HybridLogicalClock
import io.kdmtracker.core.domain.Innovation
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
            name = "Innovate",
            kind = ActionKind.INNOVATE,
            endeavorCost = 1,
            resourceCost = mapOf("Bone" to 1, "Organ" to 1, "Hide" to 1),
            oncePerPhase = true,
            draw = 2,
            keep = 1,
        )

    private val ammonia = Innovation(name = "Ammonia", description = "Acids for tanning.")

    private val catalog =
        ContentCatalog(
            listOf(
                ContentPack(
                    id = "test",
                    version = "1.0.0",
                    name = "Test",
                    bundle =
                        ContentBundle(
                            innovations = listOf(ammonia),
                            locations =
                                listOf(
                                    SettlementLocation(name = "Lantern Hoard", actions = listOf(innovateAction)),
                                    SettlementLocation(name = "Stone Circle"),
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
        state: SettlementState = SettlementState(),
    ): SettlementRecord =
        SettlementRecord(
            settlement =
                Settlement(
                    id = Ulid.generate(1L) { 0 },
                    campaignId = Ulid.generate(2L) { 0 },
                    name = "Kingsport",
                    lanternYear = 1,
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
            locations = LocationSet(built = listOf("Lantern Hoard")),
            resourceEntries =
                listOf(
                    ResourceEntry(ResourceId("Bone"), 2),
                    ResourceEntry(ResourceId("Organ"), 2),
                    ResourceEntry(ResourceId("Hide"), 2),
                ),
        )

    private fun builtButUnstocked(): SettlementState =
        SettlementState(locations = LocationSet(built = listOf("Lantern Hoard")))

    @Test
    fun aValidInnovateSpendsTheCostAddsTheInnovationAndMarksThePhase() {
        val result = Innovate.run(record(state = stocked()), "Lantern Hoard", "Ammonia", catalog)

        val ok = assertIs<StepResult.Ok<SettlementRecord>>(result)
        assertEquals(0, ok.value.settlement.endeavors)
        assertTrue(
            ok.value.state.innovations.innovations
                .any { it.name == "Ammonia" },
        )
        assertTrue(ok.value.state.usedInPhase("Lantern Hoard"))
        for (name in listOf("Bone", "Organ", "Hide")) {
            val line = ok.value.storage().single { it.id.value == name }
            assertEquals(1, line.quantity)
        }
    }

    @Test
    fun aSecondInnovateInTheSamePhaseIsBlocked() {
        val first =
            assertIs<StepResult.Ok<SettlementRecord>>(
                Innovate.run(record(state = stocked()), "Lantern Hoard", "Ammonia", catalog),
            )

        val second = Innovate.run(first.value, "Lantern Hoard", "Ammonia", catalog)

        assertIs<StepResult.Blocked>(second)
    }

    @Test
    fun insufficientStorageIsBlocked() {
        val result = Innovate.run(record(state = builtButUnstocked()), "Lantern Hoard", "Ammonia", catalog)

        assertIs<StepResult.Blocked>(result)
    }

    @Test
    fun anUnbuiltInnovateLocationIsBlocked() {
        val unbuilt = stocked().copy(locations = LocationSet())

        val result = Innovate.run(record(state = unbuilt), "Lantern Hoard", "Ammonia", catalog)

        assertIs<StepResult.Blocked>(result)
    }

    @Test
    fun aLocationWithoutAnInnovateActionIsBlocked() {
        val result = Innovate.run(record(state = stocked()), "Stone Circle", "Ammonia", catalog)

        assertIs<StepResult.Blocked>(result)
    }

    @Test
    fun anUnknownLocationIsBlocked() {
        val result = Innovate.run(record(state = stocked()), "Nowhere", "Ammonia", catalog)

        assertIs<StepResult.Blocked>(result)
    }

    @Test
    fun anUnknownInnovationIsBlocked() {
        val result = Innovate.run(record(state = stocked()), "Lantern Hoard", "Alchemy", catalog)

        assertIs<StepResult.Blocked>(result)
    }

    @Test
    fun insufficientEndeavorsIsBlocked() {
        val result = Innovate.run(record(endeavors = 0, state = stocked()), "Lantern Hoard", "Ammonia", catalog)

        assertIs<StepResult.Blocked>(result)
    }
}
