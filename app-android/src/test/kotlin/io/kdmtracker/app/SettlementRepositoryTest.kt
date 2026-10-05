package io.kdmtracker.app

import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.persistence.CampaignStore
import io.kdmtracker.core.ruleset.Core16Ruleset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettlementRepositoryTest {
    private fun repository(store: CampaignStore = FakeCampaignStore()): SettlementRepository =
        SettlementRepository(store)

    @Test
    fun exposesCampaignUsingTheCoreRuleset() {
        val repository = repository()

        assertEquals(Core16Ruleset.id, repository.campaign.rulesetId)
        assertEquals("Walking Skeleton", repository.campaign.name)
    }

    @Test
    fun campaignReferencesTheSampleSettlement() {
        val repository = repository()

        assertEquals(listOf(repository.settlement.id), repository.campaign.settlementIds)
        assertEquals(repository.campaign.id, repository.settlement.campaignId)
    }

    @Test
    fun sampleSettlementStartsWithNewSettlementDefaults() {
        val repository = repository()
        val settlement = repository.settlement

        assertEquals(1, settlement.lanternYear)
        assertEquals(SettlementPhase.SETTLEMENT, settlement.phase)
        assertEquals(1, settlement.currentStep)
        assertEquals(4, settlement.population)
        assertTrue(settlement.endeavors == 0)
    }

    @Test
    fun updateReplacesTheCurrentSettlement() {
        val repository = repository()
        val next = repository.settlement.copy(lanternYear = 3, currentStep = 7, population = 9)

        repository.update(next)

        assertEquals(next, repository.settlement)
        assertEquals(3, repository.settlement.lanternYear)
        assertEquals(7, repository.settlement.currentStep)
        assertEquals(9, repository.settlement.population)
    }

    @Test
    fun seedsTheSampleOnAnEmptyStore() {
        val store = FakeCampaignStore()

        val repository = repository(store)

        val snapshot = store.loadSnapshots().single()
        assertEquals(repository.campaign, snapshot.campaign)
        assertEquals(listOf(repository.settlement), snapshot.settlements)
    }

    @Test
    fun seedsOnlyOnceForAnAlreadyPopulatedStore() {
        val store = FakeCampaignStore()
        val seeded = repository(store)

        val reloaded = repository(store)

        assertEquals(seeded.campaign, reloaded.campaign)
        assertEquals(seeded.settlement, reloaded.settlement)
        assertEquals(1, store.loadSnapshots().size)
    }

    @Test
    fun loadsThePersistedSettlementInsteadOfReseeding() {
        val store = FakeCampaignStore()
        val first = repository(store)
        first.update(first.settlement.copy(lanternYear = 4, currentStep = 6, population = 7))

        val reloaded = repository(store)

        assertEquals(4, reloaded.settlement.lanternYear)
        assertEquals(6, reloaded.settlement.currentStep)
        assertEquals(7, reloaded.settlement.population)
    }

    @Test
    fun updateWritesThroughToTheStore() {
        val store = FakeCampaignStore()
        val repository = repository(store)
        val next = repository.settlement.copy(lanternYear = 2)

        repository.update(next)

        assertEquals(
            next,
            store
                .loadSnapshots()
                .single()
                .settlements
                .single(),
        )
    }
}
