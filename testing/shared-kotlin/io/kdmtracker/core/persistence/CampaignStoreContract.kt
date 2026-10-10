package io.kdmtracker.core.persistence

import io.kdmtracker.core.domain.Campaign
import io.kdmtracker.core.domain.LocalizedText
import io.kdmtracker.core.domain.InnovationId
import io.kdmtracker.core.domain.HybridLogicalClock
import io.kdmtracker.core.domain.Innovation
import io.kdmtracker.core.domain.InnovationCollection
import io.kdmtracker.core.domain.LocationId
import io.kdmtracker.core.domain.LocationSet
import io.kdmtracker.core.domain.ResourceCategory
import io.kdmtracker.core.domain.ResourceEntry
import io.kdmtracker.core.domain.ResourceId
import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.domain.SettlementState
import io.kdmtracker.core.domain.SyncMeta
import io.kdmtracker.core.domain.Timeline
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.ruleset.RulesetOverrides
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The behaviour every [CampaignStore] must exhibit, expressed once so each implementation runs the
 * identical suite.
 *
 * Subclasses return an empty store from [createStore]. [InMemoryCampaignStore] is the executable
 * specification for the port; the SQLDelight store in `:data` runs the same suite over a temp-file
 * database, so the specification is verified against the production implementation too.
 */
abstract class CampaignStoreContract {
    private lateinit var store: CampaignStore

    /** Returns a fresh, empty store. Called before every test. */
    protected abstract fun createStore(): CampaignStore

    /** Releases whatever [createStore] opened. Called after every test. */
    protected open fun closeStore(store: CampaignStore) {}

    @BeforeTest
    fun openStore() {
        store = createStore()
    }

    @AfterTest
    fun closeOpenStore() {
        closeStore(store)
    }

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

    /** A non-trivial state so the round-trip cannot pass by writing an empty default. */
    private fun populatedState(): SettlementState =
        SettlementState(
            timeline = Timeline.core16().withEvent(1, "Returning Survivors"),
            innovations =
                InnovationCollection(
                    innovations =
                        listOf(
                            Innovation(id = InnovationId("lantern_oven"), names = LocalizedText(es = "Lantern Oven"),
                                description = "Cook a resource to gain its benefit.",
                                consequences = listOf("Lantern Oven", "Cooking"),
                                survivalLimitBonus = 1,
                            ),
                        ),
                ),
            locations = LocationSet(built = listOf(LocationId("lantern_hoard"))),
            resourceEntries =
                listOf(
                    ResourceEntry(ResourceId("Broken Lantern"), 2),
                    ResourceEntry(ResourceId("Broken Lantern"), -1),
                ),
            deaths = 3,
            births = 5,
        )

    @Test
    fun upsertingSameSettlementTwiceYieldsOneEntry() {
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
        val parent = campaign(1)
        val child = settlement(2, parent.id)
        store.upsertCampaign(parent)
        store.upsertSettlement(child)

        val snapshot = store.loadSnapshots().single()
        assertEquals(parent.copy(settlementIds = listOf(child.id)), snapshot.campaign)
        assertEquals(listOf(child), snapshot.settlements)
    }

    @Test
    fun campaignWithItsSettlementRoundTrips() {
        val parent = campaign(1)
        val child = settlement(2, parent.id)
        store.upsertCampaign(parent)
        store.upsertSettlement(child)

        assertEquals(
            listOf(CampaignSnapshot(parent.copy(settlementIds = listOf(child.id)), listOf(child))),
            store.loadSnapshots(),
        )
    }

    @Test
    fun aPopulatedSettlementStateRoundTripsThroughTheRecord() {
        val parent = campaign(1)
        val record = SettlementRecord(settlement(2, parent.id), populatedState())
        store.upsertCampaign(parent)
        store.upsertSettlement(record)

        assertEquals(listOf(record), store.loadRecords(parent.id.value))
    }

    @Test
    fun emptyStoreLoadsNothing() {
        assertTrue(store.loadSnapshots().isEmpty())
    }

    @Test
    fun upsertingCampaignWithoutSettlementYieldsEmptySettlementList() {
        store.upsertCampaign(campaign(1))

        val snapshot = store.loadSnapshots().single()
        assertTrue(snapshot.settlements.isEmpty())
        assertTrue(snapshot.campaign.settlementIds.isEmpty())
    }

    @Test
    fun derivesSettlementIdsFromTheSettlementsOwnedByTheCampaign() {
        val parent = campaign(1)
        val first = settlement(2, parent.id)
        val second = settlement(3, parent.id)
        store.upsertCampaign(parent)
        store.upsertSettlement(first)
        store.upsertSettlement(second)

        val snapshot = store.loadSnapshots().single()
        assertEquals(setOf(first.id, second.id), snapshot.campaign.settlementIds.toSet())
    }
}
