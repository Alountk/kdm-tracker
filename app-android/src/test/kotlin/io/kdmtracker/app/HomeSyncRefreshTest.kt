package io.kdmtracker.app

import io.kdmtracker.app.sync.SyncCoordinator
import io.kdmtracker.app.sync.SyncState
import io.kdmtracker.core.domain.Campaign
import io.kdmtracker.core.domain.HybridLogicalClock
import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.domain.SyncMeta
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.persistence.InMemoryCampaignStore
import io.kdmtracker.core.ruleset.Core16Ruleset
import io.kdmtracker.core.ruleset.RulesetOverrides
import io.kdmtracker.core.sync.CampaignSummary
import io.kdmtracker.core.sync.ChangeRecord
import io.kdmtracker.core.sync.ChangeSet
import io.kdmtracker.core.sync.InMemoryMembershipStore
import io.kdmtracker.core.sync.PullResponse
import io.kdmtracker.core.sync.SyncApi
import io.kdmtracker.core.sync.SyncEngine
import io.kdmtracker.core.sync.SyncStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Covers two whole-branch review rulings at the ViewModel seam:
 *
 * - **Ruling AP (C1):** the Home card list refreshes after `syncAll` **regardless** of the outcome.
 *   The old code refreshed only on a success (`Idle` with a timestamp), so one failing campaign hid
 *   every result.
 * - **Ruling AQ (C2):** `syncAll` submits exactly the hub-listed campaigns (plus hub-created ones),
 *   never the client-minted first-run seed, so the seed cannot 403 the whole pass.
 */
class HomeSyncRefreshTest {
    private var clock = 1_000L

    private fun nextId(): Ulid = Ulid.generate(clock++) { 9 }

    @Test
    fun aFailingSyncStillRefreshesTheHomeCards() {
        val store = InMemoryCampaignStore()
        val hubId = nextId().value
        val harness = harness(store = store, hubIds = listOf(hubId), failPullFor = hubId)

        assertEquals(1, harness.viewModel.homeCards.value.size, "the local seed is the only card at load")

        // A second campaign lands in the store after the load (as an applied remote/local change
        // would); the card list must pick it up on the next refresh.
        seedCampaign(store, "Added while syncing")

        harness.viewModel.syncAll()

        assertIs<SyncState.Error>(harness.viewModel.syncState.value, "the hub-listed campaign failed on pull")
        assertEquals(
            2,
            harness.viewModel.homeCards.value.size,
            "a failing sync must not hide the refreshed Home cards (Ruling AP)",
        )
    }

    @Test
    fun theHubUnknownLocalSeedIsNeverSubmitted() {
        val store = InMemoryCampaignStore()
        val harness = harness(store = store, hubIds = emptyList(), failPullFor = null)

        harness.viewModel.syncAll()

        assertTrue(harness.api.pulled.isEmpty(), "the hub-unknown local seed must never be submitted (Ruling AQ)")
        assertIs<SyncState.Idle>(harness.viewModel.syncState.value, "no campaign was submitted, so no failure")
    }

    private fun seedCampaign(
        store: InMemoryCampaignStore,
        name: String,
    ) {
        val campaignId = nextId()
        val settlementId = nextId()
        store.transaction {
            store.upsertCampaign(
                Campaign(
                    id = campaignId,
                    name = name,
                    rulesetId = Core16Ruleset.id,
                    rulesetOverrides = RulesetOverrides(),
                    settlementIds = listOf(settlementId),
                    sync = SyncMeta(HybridLogicalClock(0, 0)),
                ),
            )
            store.upsertSettlement(
                Settlement.new(id = settlementId, campaignId = campaignId, name = name),
            )
        }
    }

    private data class Harness(
        val viewModel: LanternYearViewModel,
        val api: HubApi,
    )

    private fun harness(
        store: InMemoryCampaignStore,
        hubIds: List<String>,
        failPullFor: String?,
    ): Harness {
        val api = HubApi(hubIds, failPullFor)
        val memberships = InMemoryMembershipStore()
        val viewModel =
            LanternYearViewModel(
                buildComponents = { scope ->
                    val repository = SettlementRepository(store, memberships = memberships)
                    val controller = LanternYearController(repository)
                    val coordinator =
                        SyncCoordinator(
                            engine = SyncEngine(api, NoopSyncStore()),
                            campaignId = repository.campaign.id.value,
                            memberships = memberships,
                            scope = scope,
                            tokenProvider = { "token" },
                            dispatcher = Dispatchers.Unconfined,
                            onSynced = controller::reload,
                        )
                    AppComponents(controller = controller, syncCoordinator = coordinator)
                },
                dispatcher = Dispatchers.Unconfined,
                scope = CoroutineScope(Dispatchers.Unconfined),
            )
        return Harness(viewModel, api)
    }

    private class HubApi(
        private val hubIds: List<String>,
        private val failPullFor: String?,
    ) : SyncApi {
        val pulled = mutableListOf<String>()

        override fun listCampaigns(): List<CampaignSummary> =
            hubIds.map { id -> CampaignSummary(id, "Hub campaign", "OWNER", "me", emptyList()) }

        override fun createCampaign(name: String): CampaignSummary =
            throw UnsupportedOperationException("createCampaign is not exercised by HomeSyncRefreshTest")

        override fun pull(
            campaignId: String,
            since: Long,
        ): PullResponse {
            pulled += campaignId
            if (campaignId == failPullFor) throw IOException("hub down for $campaignId")
            return PullResponse(latestRev = since, records = emptyList())
        }

        override fun push(
            campaignId: String,
            changeSet: ChangeSet,
        ): Long = changeSet.records.maxOfOrNull { it.rev } ?: 0L
    }

    private class NoopSyncStore : SyncStore {
        override fun cursor(campaignId: String): Long = 0L

        override fun setCursor(
            campaignId: String,
            rev: Long,
        ) = Unit

        override fun pending(campaignId: String): List<ChangeRecord> = emptyList()

        override fun local(
            campaignId: String,
            entityType: String,
            entityId: String,
        ): ChangeRecord? = null

        override fun enqueue(
            campaignId: String,
            record: ChangeRecord,
        ) = Unit

        override fun applyMerged(
            campaignId: String,
            record: ChangeRecord,
        ) = Unit

        override fun clearAcknowledged(
            campaignId: String,
            records: List<ChangeRecord>,
        ) = Unit
    }
}
