package io.kdmtracker.core.persistence

import io.kdmtracker.core.domain.Campaign
import io.kdmtracker.core.domain.HybridLogicalClock
import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.domain.SyncMeta
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.ruleset.RulesetOverrides
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Contract every [CampaignStore] implementation must satisfy.
 *
 * The in-memory [FakeCampaignStore] is the executable specification: the SQLDelight
 * store in `:data` must exhibit the same observable semantics.
 */
class CampaignStoreContractTest {
    private fun ulid(seed: Long): Ulid = Ulid.generate(seed) { 0 }

    private fun campaign(
        seed: Long,
        name: String = "Grey Havens",
    ): Campaign =
        Campaign(
            id = ulid(seed),
            name = name,
            rulesetId = "core-1.6",
            rulesetOverrides = RulesetOverrides(survivalLimit = 12),
            settlementIds = emptyList(),
            sync = SyncMeta(HybridLogicalClock(seed, 0), rev = 1),
        )

    private fun settlement(
        seed: Long,
        campaignId: Ulid,
        name: String = "First Lantern",
    ): Settlement = Settlement.new(id = ulid(seed), campaignId = campaignId, name = name)

    @Test
    fun upsertingSameSettlementTwiceYieldsOneEntry() {
        val store = FakeCampaignStore()
        val parent = campaign(1)
        store.upsertCampaign(parent)
        store.upsertSettlement(settlement(2, parent.id, name = "First Lantern"))
        store.upsertSettlement(settlement(2, parent.id, name = "Renamed"))

        val settlements = store.loadSnapshots().single().settlements
        assertEquals(1, settlements.size)
        assertEquals("Renamed", settlements.single().name)
    }

    @Test
    fun loadSnapshotsReturnsWhatWasWritten() {
        val store = FakeCampaignStore()
        val parent = campaign(1)
        val child = settlement(2, parent.id)
        store.upsertCampaign(parent)
        store.upsertSettlement(child)

        val snapshot = store.loadSnapshots().single()
        assertEquals(parent, snapshot.campaign)
        assertEquals(listOf(child), snapshot.settlements)
    }

    @Test
    fun campaignWithItsSettlementRoundTrips() {
        val store = FakeCampaignStore()
        val parent = campaign(1)
        val child = settlement(2, parent.id)
        store.upsertCampaign(parent)
        store.upsertSettlement(child)

        val reloaded = store.loadSnapshots()
        assertEquals(listOf(CampaignSnapshot(parent, listOf(child))), reloaded)
    }

    @Test
    fun emptyStoreLoadsNothing() {
        assertTrue(FakeCampaignStore().loadSnapshots().isEmpty())
    }

    @Test
    fun upsertingCampaignWithoutSettlementYieldsEmptySettlementList() {
        val store = FakeCampaignStore()
        store.upsertCampaign(campaign(1))
        assertNull(
            store
                .loadSnapshots()
                .single()
                .settlements
                .firstOrNull(),
        )
    }
}

/** Minimal in-memory store proving the [CampaignStore] contract is satisfiable. */
private class FakeCampaignStore : CampaignStore {
    private val campaigns = LinkedHashMap<String, Campaign>()
    private val settlements = LinkedHashMap<String, Settlement>()

    override fun loadSnapshots(): List<CampaignSnapshot> =
        campaigns.values.map { campaign ->
            CampaignSnapshot(
                campaign = campaign,
                settlements = settlements.values.filter { it.campaignId == campaign.id },
            )
        }

    override fun upsertCampaign(campaign: Campaign) {
        campaigns[campaign.id.value] = campaign
    }

    override fun upsertSettlement(settlement: Settlement) {
        settlements[settlement.id.value] = settlement
    }
}
