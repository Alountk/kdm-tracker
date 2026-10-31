package io.kdmtracker.app

import io.kdmtracker.core.content.ContentPack
import io.kdmtracker.core.content.Core16Content
import io.kdmtracker.core.content.Legacy15Content
import io.kdmtracker.core.content.PackRegistry
import io.kdmtracker.core.content.RegisteredPack
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.persistence.InMemoryCampaignStore
import io.kdmtracker.core.sync.CampaignSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class SettlementRepositoryCreateTest {
    private val serverId = Ulid.generate(5_000L) { 7 }.value

    private val expansion =
        ContentPack(id = "test.expansion", version = "1", name = "Test", requires = listOf(Core16Content.pack.id))

    private val registry =
        PackRegistry(
            listOf(
                RegisteredPack(Core16Content.pack, base = true),
                RegisteredPack(Legacy15Content.pack, base = true),
                RegisteredPack(expansion),
            ),
        )

    private var hubCalls = 0

    private fun repository(store: InMemoryCampaignStore) =
        SettlementRepository(
            store,
            createCampaign = { name ->
                hubCalls++
                CampaignSummary(serverId, name, "OWNER", "", emptyList())
            },
            packs = registry,
        )

    @Test
    fun theHeldRecordIsTheOneThatWasStoredWhenACampaignIsCreated() {
        val store = InMemoryCampaignStore()
        val repository = repository(store)

        repository.create("Probe")

        assertEquals(store.loadRecords(serverId).single(), repository.record)
    }

    @Test
    fun aNewCampaignOpensWithFourFoundingSurvivors() {
        val repository = repository(InMemoryCampaignStore())

        repository.create("Probe")

        assertEquals(4, repository.record.state.survivors.size)
    }

    @Test
    fun aNewCampaignEnablesTheBasePacksWhenNoExpansionIsPicked() {
        val repository = repository(InMemoryCampaignStore())

        repository.create("Probe")

        assertEquals(registry.baseIds, repository.campaign.enabledPacks)
    }

    @Test
    fun anExpansionPickedAtCreationIsEnabledAndTheCatalogStillComposes() {
        val store = InMemoryCampaignStore()
        val repository = repository(store)

        repository.create("Probe", setOf(expansion.id))

        assertEquals(registry.baseIds + expansion.id, repository.campaign.enabledPacks)
        assertEquals(
            repository.campaign.enabledPacks,
            store
                .loadSnapshots()
                .first { it.campaign.id.value == serverId }
                .campaign.enabledPacks,
        )
        assertNull(repository.catalogError)
    }

    @Test
    fun anUnknownExpansionIsRefusedBeforeTheHubIsCalled() {
        val store = InMemoryCampaignStore()
        val repository = repository(store)
        val campaignsBefore = store.loadSnapshots().size

        val failure = assertFailsWith<IllegalArgumentException> { repository.create("Probe", setOf("nope")) }

        assertEquals("unknown content pack 'nope'", failure.message)
        assertEquals(0, hubCalls)
        assertEquals(campaignsBefore, store.loadSnapshots().size)
    }
}
