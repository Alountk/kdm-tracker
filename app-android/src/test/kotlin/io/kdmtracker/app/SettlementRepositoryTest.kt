package io.kdmtracker.app

import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.ruleset.Core16Ruleset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettlementRepositoryTest {
    @Test
    fun exposesCampaignUsingTheCoreRuleset() {
        val repository = SettlementRepository()

        assertEquals(Core16Ruleset.id, repository.campaign.rulesetId)
        assertEquals("Walking Skeleton", repository.campaign.name)
    }

    @Test
    fun campaignReferencesTheSampleSettlement() {
        val repository = SettlementRepository()

        assertEquals(listOf(repository.settlement.id), repository.campaign.settlementIds)
        assertEquals(repository.campaign.id, repository.settlement.campaignId)
    }

    @Test
    fun sampleSettlementStartsWithNewSettlementDefaults() {
        val repository = SettlementRepository()
        val settlement = repository.settlement

        assertEquals(1, settlement.lanternYear)
        assertEquals(SettlementPhase.SETTLEMENT, settlement.phase)
        assertEquals(1, settlement.currentStep)
        assertEquals(4, settlement.population)
        assertTrue(settlement.endeavors == 0)
    }

    @Test
    fun updateReplacesTheCurrentSettlement() {
        val repository = SettlementRepository()
        val next = repository.settlement.copy(lanternYear = 3, currentStep = 7, population = 9)

        repository.update(next)

        assertEquals(next, repository.settlement)
        assertEquals(3, repository.settlement.lanternYear)
        assertEquals(7, repository.settlement.currentStep)
        assertEquals(9, repository.settlement.population)
    }
}
