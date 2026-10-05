package io.kdmtracker.core.domain

import io.kdmtracker.core.rules.SettlementStepCatalog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class SettlementTest {
    private val id = Ulid.generate(1L) { 0 }
    private val campaignId = Ulid.generate(2L) { 0 }

    private fun validSettlement(
        lanternYear: Int = 1,
        phase: SettlementPhase = SettlementPhase.SETTLEMENT,
        currentStep: Int = 1,
        endeavors: Int = 0,
        population: Int = 4,
        settlementEvent: String? = null,
    ): Settlement =
        Settlement(
            id = id,
            campaignId = campaignId,
            name = "Kingsport",
            lanternYear = lanternYear,
            phase = phase,
            currentStep = currentStep,
            endeavors = endeavors,
            population = population,
            settlementEvent = settlementEvent,
            sync = SyncMeta(HybridLogicalClock(0, 0)),
        )

    @Test
    fun validSettlementKeepsItsValues() {
        val settlement =
            validSettlement(
                lanternYear = 3,
                phase = SettlementPhase.HUNT,
                currentStep = 7,
                endeavors = 2,
                population = 9,
                settlementEvent = "Skittering Darkness",
            )

        assertEquals(id, settlement.id)
        assertEquals(campaignId, settlement.campaignId)
        assertEquals("Kingsport", settlement.name)
        assertEquals(3, settlement.lanternYear)
        assertEquals(SettlementPhase.HUNT, settlement.phase)
        assertEquals(7, settlement.currentStep)
        assertEquals(2, settlement.endeavors)
        assertEquals(9, settlement.population)
        assertEquals("Skittering Darkness", settlement.settlementEvent)
        assertEquals(SyncMeta(HybridLogicalClock(0, 0)), settlement.sync)
    }

    @Test
    fun currentStepAboveCatalogTotalIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            validSettlement(currentStep = SettlementStepCatalog.TOTAL + 1)
        }
        assertFailsWith<IllegalArgumentException> {
            validSettlement(currentStep = 12)
        }
    }

    @Test
    fun currentStepBelowOneIsRejected() {
        assertFailsWith<IllegalArgumentException> { validSettlement(currentStep = 0) }
    }

    @Test
    fun lanternYearBelowOneIsRejected() {
        assertFailsWith<IllegalArgumentException> { validSettlement(lanternYear = 0) }
    }

    @Test
    fun negativeEndeavorsIsRejected() {
        assertFailsWith<IllegalArgumentException> { validSettlement(endeavors = -1) }
    }

    @Test
    fun negativePopulationIsRejected() {
        assertFailsWith<IllegalArgumentException> { validSettlement(population = -1) }
    }

    @Test
    fun newYieldsDocumentedDefaults() {
        val settlement = Settlement.new(id = id, campaignId = campaignId, name = "Kingsport")

        assertEquals(id, settlement.id)
        assertEquals(campaignId, settlement.campaignId)
        assertEquals("Kingsport", settlement.name)
        assertEquals(1, settlement.lanternYear)
        assertEquals(SettlementPhase.SETTLEMENT, settlement.phase)
        assertEquals(1, settlement.currentStep)
        assertEquals(0, settlement.endeavors)
        assertEquals(4, settlement.population)
        assertNull(settlement.settlementEvent)
        assertEquals(SyncMeta(HybridLogicalClock(0, 0)), settlement.sync)
    }
}
