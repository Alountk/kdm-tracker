package io.kdmtracker.core.rules

import io.kdmtracker.core.content.ContentBundle
import io.kdmtracker.core.content.ContentCatalog
import io.kdmtracker.core.content.ContentPack
import io.kdmtracker.core.domain.Gear
import io.kdmtracker.core.domain.GearRecipe
import io.kdmtracker.core.domain.HybridLogicalClock
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
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CraftabilityTest {
    private val daga = GearRecipe("Daga", resourceCost = mapOf("Hueso" to 1))
    private val taller =
        SettlementLocation(
            name = "Taller",
            recipes =
                listOf(
                    daga,
                    GearRecipe("Espada", resourceCost = mapOf("Hueso" to 1), requires = listOf("Calor")),
                ),
        )

    private val catalog =
        ContentCatalog(
            listOf(
                ContentPack(
                    id = "p",
                    version = "1",
                    name = "p",
                    bundle =
                        ContentBundle(
                            locations = listOf(taller),
                            resources = listOf(ResourceDefinition("Hueso", ResourceCategory.BASIC)),
                            gear = listOf(Gear("Daga"), Gear("Espada")),
                        ),
                ),
            ),
        )

    private fun record(
        built: List<String>,
        bones: Int,
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
                    endeavors = 0,
                    population = 4,
                    settlementEvent = null,
                    sync = SyncMeta(HybridLogicalClock(0, 0)),
                ),
            state =
                SettlementState(
                    locations = LocationSet(built = built),
                    resourceEntries = listOf(ResourceEntry("Hueso", ResourceCategory.BASIC, bones)),
                ),
        )

    @Test
    fun anAffordableRecipeIsCraftable() {
        val check = Craftability.check(record(listOf("Taller"), bones = 2), "Taller", daga, catalog)

        assertTrue(check.craftable)
        assertNull(check.missing)
    }

    @Test
    fun anUnbuiltLocationIsNotCraftableAndSaysSo() {
        val check = Craftability.check(record(emptyList(), bones = 2), "Taller", daga, catalog)

        assertFalse(check.craftable)
        assertNotNull(check.missing)
        assertTrue(check.missing!!.contains("built"), check.missing!!)
    }

    @Test
    fun insufficientStorageIsNotCraftableAndSaysSo() {
        val check = Craftability.check(record(listOf("Taller"), bones = 0), "Taller", daga, catalog)

        assertFalse(check.craftable)
        assertTrue(check.missing!!.contains("insufficient"), check.missing!!)
    }

    @Test
    fun aMissingPrerequisiteIsNotCraftableAndNamesIt() {
        val espada = taller.recipes.single { it.gearName == "Espada" }

        val check = Craftability.check(record(listOf("Taller"), bones = 2), "Taller", espada, catalog)

        assertFalse(check.craftable)
        assertTrue(check.missing!!.contains("Calor"), check.missing!!)
    }
}
