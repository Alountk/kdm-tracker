package io.kdmtracker.core.usecase

import io.kdmtracker.core.content.ContentBundle
import io.kdmtracker.core.content.ContentCatalog
import io.kdmtracker.core.content.ContentPack
import io.kdmtracker.core.content.resourceDefinition
import io.kdmtracker.core.domain.Gear
import io.kdmtracker.core.domain.GearId
import io.kdmtracker.core.domain.GearRecipe
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
            id = LocationId("bone_smith"),
            names = LocalizedText(es = "Taller de Huesos"),
            recipes =
                listOf(
                    GearRecipe(GearId("daga_de_hueso"), resourceCost = mapOf(ResourceId("Hueso") to 1)),
                    GearRecipe(
                        GearId("espada_caliente"),
                        resourceCost = mapOf(ResourceId("Hueso") to 1),
                        requires = listOf(InnovationId("calor")),
                    ),
                    GearRecipe(
                        GearId("equipo_raro"),
                        resourceCost = mapOf(ResourceId("Hueso") to 1, ResourceId("no_such_resource") to 1),
                    ),
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
                            resources = listOf(resourceDefinition("Hueso", ResourceCategory.BASIC)),
                            gear =
                                listOf(
                                    Gear(GearId("daga_de_hueso"), LocalizedText(es = "Daga de Hueso")),
                                    Gear(GearId("espada_caliente"), LocalizedText(es = "Espada Caliente")),
                                ),
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
            locations = LocationSet(built = listOf(LocationId("bone_smith"))),
            resourceEntries = listOf(ResourceEntry(ResourceId("Hueso"), bones)),
        )

    @Test
    fun aValidCraftSpendsTheCostAndAddsTheGear() {
        val result = Craft.run(record(state = ready()), LocationId("bone_smith"), GearId("daga_de_hueso"), catalog)

        val ok = assertIs<StepResult.Ok<SettlementRecord>>(result)
        assertEquals(
            1,
            ok.value
                .storage()
                .single { it.id.value == "Hueso" }
                .quantity,
        )
        assertEquals(listOf(GearId("daga_de_hueso")), ok.value.state.gear)
    }

    @Test
    fun anUnbuiltLocationIsBlocked() {
        val state = ready().copy(locations = LocationSet())

        val result = Craft.run(record(state = state), LocationId("bone_smith"), GearId("daga_de_hueso"), catalog)

        assertIs<StepResult.Blocked>(result)
    }

    @Test
    fun anUnknownLocationOrGearIsBlocked() {
        assertIs<StepResult.Blocked>(
            Craft.run(record(state = ready()), LocationId("nowhere"), GearId("daga_de_hueso"), catalog),
        )
        assertIs<StepResult.Blocked>(
            Craft.run(record(state = ready()), LocationId("bone_smith"), GearId("nowhere"), catalog),
        )
    }

    @Test
    fun aMissingPrerequisiteIsBlocked() {
        val result = Craft.run(record(state = ready()), LocationId("bone_smith"), GearId("espada_caliente"), catalog)

        assertIs<StepResult.Blocked>(result)
    }

    @Test
    fun anUnlockedPrerequisiteAllowsTheCraft() {
        val state =
            ready().copy(
                innovations =
                    InnovationCollection(
                        listOf(Innovation(InnovationId("calor"), LocalizedText(es = "Calor"), "…")),
                    ),
            )

        val result = Craft.run(record(state = state), LocationId("bone_smith"), GearId("espada_caliente"), catalog)

        assertIs<StepResult.Ok<SettlementRecord>>(result)
    }

    @Test
    fun insufficientStorageIsBlockedAndLeavesTheRecordUnchanged() {
        val before = record(state = ready(bones = 0))

        val result = Craft.run(before, LocationId("bone_smith"), GearId("daga_de_hueso"), catalog)

        assertIs<StepResult.Blocked>(result)
        assertEquals(0, before.storage().count { it.id.value == "Hueso" })
        assertTrue(before.state.gear.isEmpty())
    }

    @Test
    fun reCraftingTheSameGearDoesNotDuplicateIt() {
        val first =
            assertIs<StepResult.Ok<SettlementRecord>>(
                Craft.run(record(state = ready()), LocationId("bone_smith"), GearId("daga_de_hueso"), catalog),
            ).value

        val second =
            assertIs<StepResult.Ok<SettlementRecord>>(
                Craft.run(first, LocationId("bone_smith"), GearId("daga_de_hueso"), catalog),
            ).value

        assertEquals(listOf(GearId("daga_de_hueso")), second.state.gear)
    }

    @Test
    fun aRecipeCostingAResourceTheCatalogDoesNotCarryIsBlockedWithoutSpending() {
        // Review Focus 3: the name bridge is gone, so an unknown cost id must surface as a
        // Blocked reason — never a throw, and never a partial spend. The first cost in this
        // recipe resolves and would spend; the second does not, and Craft returns Blocked
        // without committing anything (records are immutable, so Blocked is the only
        // spend-free outcome a regression could produce here).
        val before = record(state = ready(bones = 2))

        val result = Craft.run(before, LocationId("bone_smith"), GearId("equipo_raro"), catalog)

        val blocked = assertIs<StepResult.Blocked>(result)
        assertTrue(blocked.reason.contains("no_such_resource"), blocked.reason)
        assertEquals(2, before.storage().single { it.id.value == "Hueso" }.quantity)
        assertTrue(before.state.gear.isEmpty())
    }
}
