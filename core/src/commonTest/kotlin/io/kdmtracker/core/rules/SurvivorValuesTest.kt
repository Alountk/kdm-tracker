package io.kdmtracker.core.rules

import io.kdmtracker.core.content.ContentBundle
import io.kdmtracker.core.content.ContentCatalog
import io.kdmtracker.core.content.ContentPack
import io.kdmtracker.core.domain.Attributes
import io.kdmtracker.core.domain.Gear
import io.kdmtracker.core.domain.GearId
import io.kdmtracker.core.domain.HybridLogicalClock
import io.kdmtracker.core.domain.LocalizedText
import io.kdmtracker.core.domain.Modifier
import io.kdmtracker.core.domain.ModifierScope
import io.kdmtracker.core.domain.ModifierTarget
import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.domain.SettlementState
import io.kdmtracker.core.domain.Survivor
import io.kdmtracker.core.domain.SurvivorStatus
import io.kdmtracker.core.domain.SyncMeta
import io.kdmtracker.core.domain.Ulid
import kotlin.test.Test
import kotlin.test.assertEquals

class SurvivorValuesTest {
    private val catalog =
        ContentCatalog(
            listOf(
                ContentPack(
                    id = "t",
                    version = "1",
                    name = "t",
                    bundle =
                        ContentBundle(
                            gear =
                                listOf(
                                    Gear(
                                        id = GearId("hacha"),
                                        names = LocalizedText(es = "Hacha"),
                                        modifiers =
                                            listOf(
                                                Modifier(ModifierTarget.STRENGTH, 1, ModifierScope.SURVIVOR),
                                                Modifier(ModifierTarget.MOVEMENT, 5, ModifierScope.SETTLEMENT),
                                            ),
                                    ),
                                ),
                        ),
                ),
            ),
        )

    private fun survivor(
        seed: Long,
        status: SurvivorStatus = SurvivorStatus.ALIVE,
        strength: Int = 2,
        gear: List<String> = emptyList(),
    ): Survivor =
        Survivor(
            id = Ulid.generate(seed) { 0 },
            attributes = Attributes(strength = strength),
            gear = gear,
            status = status,
        )

    private fun record(vararg survivors: Survivor): SettlementRecord =
        SettlementRecord(
            settlement =
                Settlement(
                    id = Ulid.generate(90L) { 0 },
                    campaignId = Ulid.generate(91L) { 0 },
                    name = "Kingsport",
                    lanternYear = 1,
                    phase = SettlementPhase.SETTLEMENT,
                    currentStep = 2,
                    endeavors = 0,
                    population = 0,
                    settlementEvent = null,
                    sync = SyncMeta(HybridLogicalClock(0, 0)),
                ),
            state = SettlementState(survivors = survivors.toList()),
        )

    @Test
    fun populationCountsLivingSurvivorsOnly() {
        val record =
            record(
                survivor(1, SurvivorStatus.ALIVE),
                survivor(2, SurvivorStatus.ALIVE),
                survivor(3, SurvivorStatus.DEPARTING),
                survivor(4, SurvivorStatus.DEAD),
            )

        assertEquals(3, record.population)
    }

    @Test
    fun milestoneStateUsesTheRosterPopulation() {
        val record = record(survivor(1), survivor(2, SurvivorStatus.DEAD))

        assertEquals(1, record.milestoneState().population)
    }

    @Test
    fun effectiveAttributeAddsSurvivorScopedGearModifiers() {
        val eli = survivor(1, strength = 2, gear = listOf("Hacha"))

        assertEquals(3, SurvivorValues.effectiveAttribute(eli, ModifierTarget.STRENGTH, catalog))
    }

    @Test
    fun effectiveAttributeIgnoresSettlementScopedModifiers() {
        val eli = survivor(1, strength = 2, gear = listOf("Hacha"))

        // "Hacha" carries a SETTLEMENT-scope +5 movement, which must not reach the survivor.
        assertEquals(0, SurvivorValues.effectiveAttribute(eli, ModifierTarget.MOVEMENT, catalog))
    }
}
