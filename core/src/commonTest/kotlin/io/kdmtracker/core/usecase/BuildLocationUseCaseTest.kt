package io.kdmtracker.core.usecase

import io.kdmtracker.core.content.ContentBundle
import io.kdmtracker.core.content.ContentCatalog
import io.kdmtracker.core.content.ContentPack
import io.kdmtracker.core.content.resourceDefinition
import io.kdmtracker.core.domain.HybridLogicalClock
import io.kdmtracker.core.domain.LocalizedText
import io.kdmtracker.core.domain.LocationId
import io.kdmtracker.core.domain.LocationSet
import io.kdmtracker.core.domain.ResourceCategory
import io.kdmtracker.core.domain.ResourceEntry
import io.kdmtracker.core.domain.ResourceId
import io.kdmtracker.core.domain.Settlement
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

class BuildLocationUseCaseTest {
    private val blacksmith =
        SettlementLocation(
            id = LocationId("blacksmith"),
            names = LocalizedText(es = "Blacksmith"),
            requires = listOf(LocationId("stone_circle")),
            buildEndeavors = 2,
            buildCost = mapOf(ResourceId("Bone") to 1, ResourceId("Hide") to 1),
        )

    private val stoneCircle = SettlementLocation(LocationId("stone_circle"), LocalizedText(es = "Stone Circle"))

    private val catalog =
        ContentCatalog(
            listOf(
                ContentPack(
                    id = "test",
                    version = "1.0.0",
                    name = "Test",
                    bundle =
                        ContentBundle(
                            locations = listOf(blacksmith, stoneCircle),
                            resources =
                                listOf(
                                    resourceDefinition("Bone", ResourceCategory.BASIC),
                                    resourceDefinition("Hide", ResourceCategory.BASIC),
                                ),
                        ),
                ),
            ),
        )

    private fun record(
        endeavors: Int = 3,
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
                    currentStep = 6,
                    endeavors = endeavors,
                    population = 4,
                    settlementEvent = null,
                    sync = SyncMeta(HybridLogicalClock(0, 0)),
                ),
            state = state,
        )

    private fun ready(): SettlementState =
        SettlementState(
            locations = LocationSet(built = listOf(LocationId("stone_circle"))),
            resourceEntries =
                listOf(
                    ResourceEntry(ResourceId("Bone"), 2),
                    ResourceEntry(ResourceId("Hide"), 2),
                ),
        )

    @Test
    fun aSatisfiedBuildSpendsTheCostAndEndeavorsAndAddsTheLocation() {
        val result = BuildLocation.run(record(state = ready()), LocationId("blacksmith"), catalog)

        val ok = assertIs<StepResult.Ok<SettlementRecord>>(result)
        assertEquals(1, ok.value.settlement.endeavors)
        assertTrue(LocationId("blacksmith") in ok.value.state.locations.built)
        assertTrue(LocationId("stone_circle") in ok.value.state.locations.built)
        for (name in listOf("Bone", "Hide")) {
            val line = ok.value.storage().single { it.id.value == name }
            assertEquals(1, line.quantity)
        }
    }

    @Test
    fun unmetRequiresIsBlocked() {
        val state = ready().copy(locations = LocationSet())

        val result = BuildLocation.run(record(state = state), LocationId("blacksmith"), catalog)

        assertIs<StepResult.Blocked>(result)
    }

    @Test
    fun insufficientStorageIsBlockedAndLeavesTheRecordUnchanged() {
        val before =
            record(
                state =
                    ready().copy(
                        resourceEntries = listOf(ResourceEntry(ResourceId("Bone"), 1)),
                    ),
            )

        val result = BuildLocation.run(before, LocationId("blacksmith"), catalog)

        assertIs<StepResult.Blocked>(result)
        assertEquals(1, before.storage().single { it.id.value == "Bone" }.quantity)
        assertEquals(3, before.settlement.endeavors)
        assertTrue(LocationId("blacksmith") !in before.state.locations.built)
    }

    @Test
    fun insufficientEndeavorsIsBlocked() {
        val result = BuildLocation.run(record(endeavors = 1, state = ready()), LocationId("blacksmith"), catalog)

        assertIs<StepResult.Blocked>(result)
    }

    @Test
    fun anAlreadyBuiltLocationIsBlocked() {
        val state =
            ready().copy(
                locations = LocationSet(built = listOf(LocationId("stone_circle"), LocationId("blacksmith"))),
            )

        val result = BuildLocation.run(record(state = state), LocationId("blacksmith"), catalog)

        assertIs<StepResult.Blocked>(result)
    }

    @Test
    fun anUnknownLocationIsBlocked() {
        val result = BuildLocation.run(record(state = ready()), LocationId("nowhere"), catalog)

        assertIs<StepResult.Blocked>(result)
    }
}
