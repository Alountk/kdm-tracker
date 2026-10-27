package io.kdmtracker.app

import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.persistence.InMemoryCampaignStore
import io.kdmtracker.core.sync.CampaignSummary
import kotlin.test.Test
import kotlin.test.assertEquals

class SettlementRepositoryCreateTest {
    private val serverId = Ulid.generate(5_000L) { 7 }.value

    private fun repository(store: InMemoryCampaignStore) =
        SettlementRepository(
            store,
            createCampaign = { name -> CampaignSummary(serverId, name, "OWNER", "", emptyList()) },
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
}
