package io.kdmtracker.app

import io.kdmtracker.core.domain.Campaign
import io.kdmtracker.core.domain.HybridLogicalClock
import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.domain.SyncMeta
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.persistence.CampaignStore
import io.kdmtracker.core.persistence.InMemoryCampaignStore
import io.kdmtracker.core.persistence.SyncClock
import io.kdmtracker.core.ruleset.Core16Ruleset
import io.kdmtracker.core.ruleset.RulesetOverrides
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettlementRepositoryTest {
    private fun repository(store: CampaignStore = InMemoryCampaignStore()): SettlementRepository =
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

        assertEquals(3, repository.settlement.lanternYear)
        assertEquals(7, repository.settlement.currentStep)
        assertEquals(9, repository.settlement.population)
    }

    @Test
    fun seedsTheSampleOnAnEmptyStore() {
        val store = InMemoryCampaignStore()

        val repository = repository(store)

        val snapshot = store.loadSnapshots().single()
        assertEquals(repository.campaign, snapshot.campaign)
        assertEquals(listOf(repository.settlement), snapshot.settlements)
    }

    @Test
    fun seedsOnlyOnceForAnAlreadyPopulatedStore() {
        val store = InMemoryCampaignStore()
        val seeded = repository(store)

        val reloaded = repository(store)

        assertEquals(seeded.campaign, reloaded.campaign)
        assertEquals(seeded.settlement, reloaded.settlement)
        assertEquals(1, store.loadSnapshots().size)
    }

    @Test
    fun loadsThePersistedSettlementInsteadOfReseeding() {
        val store = InMemoryCampaignStore()
        val first = repository(store)
        first.update(first.settlement.copy(lanternYear = 4, currentStep = 6, population = 7))

        val reloaded = repository(store)

        assertEquals(4, reloaded.settlement.lanternYear)
        assertEquals(6, reloaded.settlement.currentStep)
        assertEquals(7, reloaded.settlement.population)
    }

    @Test
    fun updateWritesThroughToTheStore() {
        val store = InMemoryCampaignStore()
        val repository = repository(store)
        val next = repository.settlement.copy(lanternYear = 2)

        repository.update(next)

        val stored =
            store
                .loadSnapshots()
                .single()
                .settlements
                .single()
        assertEquals(2, stored.lanternYear)
        assertEquals(next.sync.rev + 1, stored.sync.rev)
        assertTrue(stored.sync.updatedAt > next.sync.updatedAt)
    }

    @Test
    fun updateStampsAnAdvancingRevisionAndClock() {
        val repository = SettlementRepository(InMemoryCampaignStore(), RecordingSyncClock())
        val seeded = repository.settlement.sync

        repository.update(repository.settlement.copy(lanternYear = 2))
        val first = repository.settlement.sync
        repository.update(repository.settlement.copy(lanternYear = 3))
        val second = repository.settlement.sync

        assertEquals(seeded.rev + 1, first.rev)
        assertEquals(seeded.rev + 2, second.rev)
        assertTrue(first.updatedAt > seeded.updatedAt)
        assertTrue(second.updatedAt > first.updatedAt)
    }

    @Test
    fun reloadPicksUpChangesWrittenBehindTheRepositorysBack() {
        val store = InMemoryCampaignStore()
        val repository = repository(store)
        val original = repository.settlement
        store.upsertSettlement(
            original.copy(
                name = "Remote edit",
                population = 42,
                sync =
                    original.sync.copy(
                        updatedAt = HybridLogicalClock(original.sync.updatedAt.millis + 10_000, 0),
                        rev = 99,
                    ),
            ),
        )

        repository.reload()

        assertEquals("Remote edit", repository.settlement.name)
        assertEquals(42, repository.settlement.population)
        assertEquals(99, repository.settlement.sync.rev)
    }

    @Test
    fun recoversWhenACampaignExistsWithoutALiveSettlement() {
        val store = InMemoryCampaignStore()
        store.upsertCampaign(
            Campaign(
                id = Ulid.generate(0L) { 0 },
                name = "Walking Skeleton",
                rulesetId = Core16Ruleset.id,
                rulesetOverrides = RulesetOverrides(),
                settlementIds = emptyList(),
                sync = SyncMeta(HybridLogicalClock(0, 0)),
            ),
        )

        val repository = SettlementRepository(store)

        val snapshot = store.loadSnapshots().single()
        assertEquals(repository.campaign.id, snapshot.campaign.id)
        assertEquals(listOf(repository.settlement), snapshot.settlements)
    }
}

/** Deterministic clock: each tick advances the wall time by one millisecond. */
private class RecordingSyncClock : SyncClock {
    private var millis = 1_000L

    override fun tick(previous: HybridLogicalClock?): HybridLogicalClock = HybridLogicalClock.tick(previous, millis++)
}
