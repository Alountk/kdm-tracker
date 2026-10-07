package io.kdmtracker.core.usecase

import io.kdmtracker.core.content.ContentBundle
import io.kdmtracker.core.content.ContentCatalog
import io.kdmtracker.core.content.ContentPack
import io.kdmtracker.core.domain.Gear
import io.kdmtracker.core.domain.GearRecipe
import io.kdmtracker.core.domain.HybridLogicalClock
import io.kdmtracker.core.domain.Innovation
import io.kdmtracker.core.domain.InnovationCollection
import io.kdmtracker.core.domain.LocationSet
import io.kdmtracker.core.domain.ResourceCategory
import io.kdmtracker.core.domain.ResourceDefinition
import io.kdmtracker.core.domain.ResourceEntry
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

class CraftUseCaseTest {
    private val taller =
        SettlementLocation(
            name = "Taller de Huesos",
            recipes =
                listOf(
                    GearRecipe("Daga de Hueso", resourceCost = mapOf("Hueso" to 1)),
                    GearRecipe("Espada Caliente", resourceCost = mapOf("Hueso" to 1), requires = listOf("Calor")),
                ),
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
                            locations = listOf(taller),
                            resources = listOf(ResourceDefinition("Hueso", ResourceCategory.BASIC)),
                            gear = listOf(Gear("Daga de Hueso"), Gear("Espada Caliente")),
                        ),
                ),
            ),
        )

    private fun record(state: SettlementState = SettlementState()): SettlementRecord =
        SettlementRecord(
            settlement =
                Settlement(
                    id = Ulid.generate(1L) { 0 },
                    campaignId = Ulid.generate(2L) { 0 },
                    name = "Kingsport",
                    lanternYear = 1,
                    phase = SettlementPhase.SETTLEMENT,
                    currentStep = 7,
                    endeavors = 0,
                    population = 4,
                    settlementEvent = null,
                    sync = SyncMeta(HybridLogicalClock(0, 0)),
                ),
            state = state,
        )

    private fun ready(bones: Int = 2): SettlementState =
        SettlementState(
            locations = LocationSet(built = listOf("Taller de Huesos")),
            resourceEntries = listOf(ResourceEntry("Hueso", ResourceCategory.BASIC, bones)),
        )

    @Test
    fun aValidCraftSpendsTheCostAndAddsTheGear() {
        val result = Craft.run(record(state = ready()), "Taller de Huesos", "Daga de Hueso", catalog)

        val ok = assertIs<StepResult.Ok<SettlementRecord>>(result)
        assertEquals(
            1,
            ok.value
                .storage()
                .single { it.name == "Hueso" }
                .quantity,
        )
        assertEquals(listOf("Daga de Hueso"), ok.value.state.gear)
    }

    @Test
    fun anUnbuiltLocationIsBlocked() {
        val state = ready().copy(locations = LocationSet())

        val result = Craft.run(record(state = state), "Taller de Huesos", "Daga de Hueso", catalog)

        assertIs<StepResult.Blocked>(result)
    }

    @Test
    fun anUnknownLocationOrGearIsBlocked() {
        assertIs<StepResult.Blocked>(
            Craft.run(record(state = ready()), "Nowhere", "Daga de Hueso", catalog),
        )
        assertIs<StepResult.Blocked>(
            Craft.run(record(state = ready()), "Taller de Huesos", "Nowhere", catalog),
        )
    }

    @Test
    fun aMissingPrerequisiteIsBlocked() {
        val result = Craft.run(record(state = ready()), "Taller de Huesos", "Espada Caliente", catalog)

        assertIs<StepResult.Blocked>(result)
    }

    @Test
    fun anUnlockedPrerequisiteAllowsTheCraft() {
        val state = ready().copy(innovations = InnovationCollection(listOf(Innovation("Calor", "…"))))

        val result = Craft.run(record(state = state), "Taller de Huesos", "Espada Caliente", catalog)

        assertIs<StepResult.Ok<SettlementRecord>>(result)
    }

    @Test
    fun insufficientStorageIsBlockedAndLeavesTheRecordUnchanged() {
        val before = record(state = ready(bones = 0))

        val result = Craft.run(before, "Taller de Huesos", "Daga de Hueso", catalog)

        assertIs<StepResult.Blocked>(result)
        assertEquals(0, before.storage().count { it.name == "Hueso" })
        assertTrue(before.state.gear.isEmpty())
    }

    @Test
    fun reCraftingTheSameGearDoesNotDuplicateIt() {
        val first =
            assertIs<StepResult.Ok<SettlementRecord>>(
                Craft.run(record(state = ready()), "Taller de Huesos", "Daga de Hueso", catalog),
            ).value

        val second =
            assertIs<StepResult.Ok<SettlementRecord>>(
                Craft.run(first, "Taller de Huesos", "Daga de Hueso", catalog),
            ).value

        assertEquals(listOf("Daga de Hueso"), second.state.gear)
    }
}
