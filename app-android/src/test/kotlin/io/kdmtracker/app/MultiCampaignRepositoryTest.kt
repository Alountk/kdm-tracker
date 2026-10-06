package io.kdmtracker.app

import io.kdmtracker.app.sync.SyncCoordinator
import io.kdmtracker.app.sync.SyncState
import io.kdmtracker.core.domain.Campaign
import io.kdmtracker.core.domain.HybridLogicalClock
import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.domain.SyncMeta
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.persistence.CampaignStore
import io.kdmtracker.core.persistence.InMemoryCampaignStore
import io.kdmtracker.core.ruleset.Core16Ruleset
import io.kdmtracker.core.ruleset.RulesetOverrides
import io.kdmtracker.core.sync.CampaignBadge
import io.kdmtracker.core.sync.CampaignSummary
import io.kdmtracker.core.sync.ChangeRecord
import io.kdmtracker.core.sync.ChangeSet
import io.kdmtracker.core.sync.MembershipStore
import io.kdmtracker.core.sync.PullResponse
import io.kdmtracker.core.sync.SyncApi
import io.kdmtracker.core.sync.SyncEngine
import io.kdmtracker.core.sync.SyncStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Covers the multi-campaign app surface: the campaign list with sharing badges, switching the
 * held settlement, creating a campaign through the hub, and syncing every listed campaign on sign-in.
 *
 * Driven entirely with fakes and [Dispatchers.Unconfined], so the whole file is JVM-only and the
 * scheduled sync pass runs to completion synchronously.
 */
class MultiCampaignRepositoryTest {
    private var clock = 1_000L

    private fun nextId(): Ulid = Ulid.generate(clock++) { 42 }

    private fun seedCampaign(
        store: CampaignStore,
        name: String,
        lanternYear: Int = 1,
        population: Int = 4,
    ): String {
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
                Settlement
                    .new(id = settlementId, campaignId = campaignId, name = name)
                    .copy(lanternYear = lanternYear, population = population),
            )
        }
        return campaignId.value
    }

    @Test
    fun listCardsReturnsOneCardPerCampaignWithTheRightBanner() {
        val store = InMemoryCampaignStore()
        val memberships = FakeMembershipStore()
        val ownedAlone = seedCampaign(store, "Alone", lanternYear = 1, population = 4)
        val ownedShared = seedCampaign(store, "Shared by me", lanternYear = 2, population = 8)
        val sharedWithMe = seedCampaign(store, "Shared with me", lanternYear = 3, population = 11)
        memberships.upsertMembership(CampaignSummary(ownedAlone, "Alone", "OWNER", "me", emptyList()))
        memberships.upsertMembership(CampaignSummary(ownedShared, "Shared by me", "OWNER", "me", listOf("bob")))
        memberships.upsertMembership(CampaignSummary(sharedWithMe, "Shared with me", "EDITOR", "alice", emptyList()))

        val cards = SettlementRepository(store, memberships = memberships).listCards()

        assertEquals(3, cards.size)
        val byId = cards.associateBy { it.campaignId }
        assertEquals(CampaignBadge.OWNED, byId.getValue(ownedAlone).banner.badge)
        assertEquals("Tuyo", byId.getValue(ownedAlone).banner.label)
        assertEquals(CampaignBadge.SHARED_BY_ME, byId.getValue(ownedShared).banner.badge)
        assertEquals("Compartido con: bob", byId.getValue(ownedShared).banner.label)
        assertEquals(CampaignBadge.SHARED_WITH_ME, byId.getValue(sharedWithMe).banner.badge)
        assertEquals("Compartido con vos por alice", byId.getValue(sharedWithMe).banner.label)
        assertEquals("Alone", byId.getValue(ownedAlone).name)
        assertEquals(2, byId.getValue(ownedShared).lanternYear)
        assertEquals(11, byId.getValue(sharedWithMe).population)
    }

    @Test
    fun campaignsWithoutAMembershipFallBackToAnOwnedBanner() {
        val store = InMemoryCampaignStore()
        val id = seedCampaign(store, "Untracked")

        val card = SettlementRepository(store, memberships = FakeMembershipStore()).listCards().single()

        assertEquals(id, card.campaignId)
        assertEquals(CampaignBadge.OWNED, card.banner.badge)
        assertEquals("Tuyo", card.banner.label)
    }

    @Test
    fun selectScopesTheSettlementToTheChosenCampaign() {
        val store = InMemoryCampaignStore()
        seedCampaign(store, "First", lanternYear = 1)
        val second = seedCampaign(store, "Second", lanternYear = 7)
        val repository = SettlementRepository(store, memberships = FakeMembershipStore())

        assertTrue(repository.select(second))
        assertEquals(second, repository.campaign.id.value)
        assertEquals(7, repository.settlement.lanternYear)
        assertEquals(second, repository.settlement.campaignId.value)

        assertFalse(repository.select("missing-campaign"))
        assertEquals(second, repository.campaign.id.value, "an unknown id must not change the selection")
        assertEquals(7, repository.settlement.lanternYear)
    }

    @Test
    fun createUsesTheServerReturnedIdAndOpensIt() {
        val store = InMemoryCampaignStore()
        val memberships = FakeMembershipStore()
        val serverId = nextId().value
        var requestedName: String? = null
        val repository =
            SettlementRepository(
                store,
                memberships = memberships,
                createCampaign = { name ->
                    requestedName = name
                    CampaignSummary(serverId, name, "OWNER", "", emptyList())
                },
            )

        val card = repository.create("Gloom")

        assertEquals("Gloom", requestedName, "create must round-trip the hub")
        assertEquals(serverId, card.campaignId, "the card must use the server-issued id")
        assertEquals("Gloom", card.name)
        assertEquals(serverId, repository.campaign.id.value)
        assertEquals(serverId, repository.settlement.campaignId.value)
        assertEquals(1, repository.settlement.lanternYear)
        assertEquals(4, repository.settlement.population)
        assertEquals(CampaignBadge.OWNED, card.banner.badge)

        val stored = store.loadSnapshots().single { it.campaign.id.value == serverId }
        assertEquals(listOf(repository.settlement.id), stored.settlements.map { it.id })
        assertEquals("OWNER", memberships.membership(serverId)?.role)

        val fromList = repository.listCards().single { it.campaignId == serverId }
        assertEquals("Gloom", fromList.name)
    }

    @Test
    fun createFailureLeavesNoLocalCampaign() {
        val store = InMemoryCampaignStore()
        val memberships = FakeMembershipStore()
        val repository =
            SettlementRepository(
                store,
                memberships = memberships,
                createCampaign = { throw IOException("hub down") },
            )
        val before = store.loadSnapshots().map { it.campaign.id.value }.toSet()

        assertFailsWith<IOException> { repository.create("Gloom") }

        assertEquals(before, store.loadSnapshots().map { it.campaign.id.value }.toSet())
        assertTrue(memberships.memberships().isEmpty(), "no membership may be cached for a failed create")
        assertEquals(before.single(), repository.campaign.id.value, "the prior selection must be kept")
    }

    @Test
    fun syncAllSyncsEveryListedCampaign() {
        val store = FakeSyncStore()
        val first = nextId().value
        val second = nextId().value
        val api = FakeSyncApi(summaries = listOf(summary(first, "First"), summary(second, "Second")))
        val coordinator = coordinator(api, store)

        coordinator.syncAll()

        assertEquals(9L, store.cursor(first))
        assertEquals(9L, store.cursor(second))
        assertEquals(setOf(first, second), store.memberships().map { it.id }.toSet())
        assertIs<SyncState.Idle>(coordinator.state.value)
    }

    @Test
    fun syncAllKeepsGoingWhenOneCampaignFails() {
        val store = FakeSyncStore()
        val healthy = nextId().value
        val broken = nextId().value
        val api =
            FakeSyncApi(
                summaries = listOf(summary(healthy, "Healthy"), summary(broken, "Broken")),
                failPullFor = broken,
            )
        val coordinator = coordinator(api, store)

        coordinator.syncAll()

        assertEquals(9L, store.cursor(healthy), "a healthy campaign must still advance its own cursor")
        assertEquals(0L, store.cursor(broken), "the failing campaign must not advance")
        val state = coordinator.state.value
        assertIs<SyncState.Error>(state)
        assertTrue(state.message.contains(broken), "the failure must name the campaign: ${state.message}")
    }

    private fun summary(
        id: String,
        name: String,
    ) = CampaignSummary(id, name, "OWNER", "me", emptyList())

    private fun coordinator(
        api: SyncApi,
        store: FakeSyncStore,
    ) = SyncCoordinator(
        engine = SyncEngine(api, store),
        campaignId = "unused",
        memberships = store,
        scope = CoroutineScope(Dispatchers.Unconfined),
        tokenProvider = { "token" },
        dispatcher = Dispatchers.Unconfined,
    )

    private class FakeMembershipStore : MembershipStore {
        private val stored = LinkedHashMap<String, CampaignSummary>()

        override fun memberships(): List<CampaignSummary> = stored.values.toList()

        override fun membership(campaignId: String): CampaignSummary? = stored[campaignId]

        override fun upsertMembership(summary: CampaignSummary) {
            stored[summary.id] = summary
        }
    }

    private class FakeSyncStore :
        SyncStore,
        MembershipStore {
        private val cursors = mutableMapOf<String, Long>()
        private val outbox = LinkedHashMap<Pair<String, String>, ChangeRecord>()
        private val storedMemberships = LinkedHashMap<String, CampaignSummary>()

        override fun cursor(campaignId: String): Long = cursors[campaignId] ?: 0L

        override fun setCursor(
            campaignId: String,
            rev: Long,
        ) {
            cursors[campaignId] = rev
        }

        override fun pending(campaignId: String): List<ChangeRecord> =
            outbox.values.filter { it.campaignId == campaignId }

        override fun local(
            campaignId: String,
            entityType: String,
            entityId: String,
        ): ChangeRecord? = outbox[entityType to entityId]

        override fun enqueue(
            campaignId: String,
            record: ChangeRecord,
        ) {
            outbox[record.entityType to record.entityId] = record
        }

        override fun applyMerged(
            campaignId: String,
            record: ChangeRecord,
        ) {
            outbox.remove(record.entityType to record.entityId)
        }

        override fun clearAcknowledged(
            campaignId: String,
            records: List<ChangeRecord>,
        ) {
            records.forEach { outbox.remove(it.entityType to it.entityId) }
        }

        override fun memberships(): List<CampaignSummary> = storedMemberships.values.toList()

        override fun membership(campaignId: String): CampaignSummary? = storedMemberships[campaignId]

        override fun upsertMembership(summary: CampaignSummary) {
            storedMemberships[summary.id] = summary
        }
    }

    private class FakeSyncApi(
        private val summaries: List<CampaignSummary> = emptyList(),
        private val failPullFor: String? = null,
    ) : SyncApi {
        override fun listCampaigns(): List<CampaignSummary> = summaries

        override fun createCampaign(name: String): CampaignSummary =
            throw UnsupportedOperationException("createCampaign is injected as a lambda in this test")

        override fun pull(
            campaignId: String,
            since: Long,
        ): PullResponse {
            if (campaignId == failPullFor) throw IOException("hub down for $campaignId")
            return PullResponse(latestRev = 9L, records = emptyList())
        }

        override fun push(
            campaignId: String,
            changeSet: ChangeSet,
        ): Long = 9L
    }
}
