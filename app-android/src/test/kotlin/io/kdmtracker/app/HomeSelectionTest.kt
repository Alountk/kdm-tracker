package io.kdmtracker.app

import io.kdmtracker.app.sync.SyncCoordinator
import io.kdmtracker.app.sync.SyncHttpException
import io.kdmtracker.core.domain.Campaign
import io.kdmtracker.core.domain.HybridLogicalClock
import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.domain.SyncMeta
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.persistence.InMemoryCampaignStore
import io.kdmtracker.core.ruleset.Core16Ruleset
import io.kdmtracker.core.ruleset.RulesetOverrides
import io.kdmtracker.core.sync.CampaignBadge
import io.kdmtracker.core.sync.CampaignBadges
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
import kotlinx.serialization.SerializationException
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers the Home-first navigation (Ruling AK) and the sharing badges (Review Focus 2).
 *
 * The ViewModel is driven on [Dispatchers.Unconfined] so the deferred load and every scheduled
 * mutation/sync run to completion on the calling thread; the fakes mirror the `:core` ports, so the
 * file stays a pure JVM test with no Android or HTTP machinery.
 */
class HomeSelectionTest {
    private var clock = 1_000L

    private fun nextId(): Ulid = Ulid.generate(clock++) { 7 }

    private fun seedCampaign(
        store: InMemoryCampaignStore,
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
    fun startsOnHomeListingTheSeededSettlement() {
        val harness = harness()

        assertEquals(
            Screen.Home,
            harness.viewModel.state.value
                ?.screen,
        )
        val card =
            harness.viewModel.homeCards.value
                .single()
        assertEquals("New Settlement", card.name)
        assertEquals("Tuyo", card.banner.label)
    }

    @Test
    fun selectingACardOpensThatCampaignAndRebindsSync() {
        val store = InMemoryCampaignStore()
        seedCampaign(store, "First", lanternYear = 1, population = 4)
        val second = seedCampaign(store, "Second", lanternYear = 7, population = 9)
        val harness = harness(store = store)

        harness.viewModel.select(second)

        val state = harness.viewModel.state.value
        assertEquals(Screen.Settlement, state?.screen, "a selection must open the cockpit, not stay on Home")
        assertEquals("Second", state?.settlementName)
        assertEquals(7, state?.lanternYear)
        assertEquals(9, state?.population)
        assertEquals(second, harness.coordinator.campaignId, "select must re-bind the coordinator")
        assertTrue(harness.api.pulled.contains(second), "the sync after select must target the chosen campaign")
    }

    @Test
    fun creatingAddsACardAndOpensIt() {
        val serverId = nextId().value
        val harness = harness(createCampaign = { name -> CampaignSummary(serverId, name, "OWNER", "", emptyList()) })

        harness.viewModel.create("Gloom")

        assertEquals(2, harness.viewModel.homeCards.value.size)
        assertTrue(
            harness.viewModel.homeCards.value
                .any { it.campaignId == serverId && it.name == "Gloom" },
        )
        val state = harness.viewModel.state.value
        assertEquals(Screen.Settlement, state?.screen)
        assertEquals("Gloom", state?.settlementName)
        assertEquals(serverId, harness.coordinator.campaignId)
    }

    @Test
    fun aFailedCreateSurfacesTheErrorAndStaysOnHome() {
        val harness = harness(createCampaign = { throw IOException("hub down") })

        harness.viewModel.create("Gloom")

        assertEquals("hub down", harness.viewModel.actionError.value)
        assertEquals(
            Screen.Home,
            harness.viewModel.state.value
                ?.screen,
        )
        assertEquals(1, harness.viewModel.homeCards.value.size)
    }

    @Test
    fun aRefusedExpansionSurfacesTheErrorAndNeverReachesTheHub() {
        var hubCalls = 0
        val harness =
            harness(
                createCampaign = { name ->
                    hubCalls++
                    CampaignSummary(nextId().value, name, "OWNER", "", emptyList())
                },
            )

        harness.viewModel.create("Gloom", setOf("nope"))

        assertEquals("unknown content pack 'nope'", harness.viewModel.actionError.value)
        assertEquals(0, hubCalls)
        assertEquals(1, harness.viewModel.homeCards.value.size)
    }

    @Test
    fun aHubRejectedCreateSurfacesTheHttpError() {
        val harness = harness(createCampaign = { throw SyncHttpException(409, "duplicate name") })

        harness.viewModel.create("Gloom")

        assertEquals("duplicate name", harness.viewModel.actionError.value)
        assertEquals(
            Screen.Home,
            harness.viewModel.state.value
                ?.screen,
        )
    }

    @Test
    fun aMalformedCreateResponseSurfacesAnErrorInsteadOfCrashing() {
        val harness = harness(createCampaign = { throw SerializationException("malformed 2xx body") })

        harness.viewModel.create("Gloom")

        assertEquals(
            Screen.Home,
            harness.viewModel.state.value
                ?.screen,
        )
        assertTrue(
            harness.viewModel.actionError.value != null,
            "a malformed 2xx create response must surface as actionError, not crash",
        )
    }

    @Test
    fun returningHomeFromTheCockpitShowsTheHomeListAgain() {
        val harness = harness()
        val seededId =
            harness.viewModel.homeCards.value
                .single()
                .campaignId

        harness.viewModel.select(seededId)
        assertEquals(
            Screen.Settlement,
            harness.viewModel.state.value
                ?.screen,
        )

        harness.viewModel.goHome()

        assertEquals(
            Screen.Home,
            harness.viewModel.state.value
                ?.screen,
            "the cockpit must offer a way back to Home (Ruling AR)",
        )
    }

    @Test
    fun badgeLabelsMatchTheSharingRule() {
        val store = InMemoryCampaignStore()
        val memberships = InMemoryMembershipStore()
        val alone = seedCampaign(store, "Alone")
        val sharedByMe = seedCampaign(store, "Shared by me")
        val sharedWithMe = seedCampaign(store, "Shared with me")
        val aloneSummary = CampaignSummary(alone, "Alone", "OWNER", "me", emptyList())
        val sharedByMeSummary = CampaignSummary(sharedByMe, "Shared by me", "OWNER", "me", listOf("bob"))
        val sharedWithMeSummary = CampaignSummary(sharedWithMe, "Shared with me", "EDITOR", "alice", emptyList())
        memberships.upsertMembership(aloneSummary)
        memberships.upsertMembership(sharedByMeSummary)
        memberships.upsertMembership(sharedWithMeSummary)

        val byId =
            harness(
                store = store,
                memberships = memberships,
            ).viewModel.homeCards.value.associateBy { it.campaignId }

        assertEquals(CampaignBadges.of(aloneSummary), byId.getValue(alone).banner)
        assertEquals(CampaignBadge.OWNED, byId.getValue(alone).banner.badge)
        assertEquals(CampaignBadges.of(sharedByMeSummary), byId.getValue(sharedByMe).banner)
        assertEquals(CampaignBadge.SHARED_BY_ME, byId.getValue(sharedByMe).banner.badge)
        assertEquals(CampaignBadges.of(sharedWithMeSummary), byId.getValue(sharedWithMe).banner)
        assertEquals(CampaignBadge.SHARED_WITH_ME, byId.getValue(sharedWithMe).banner.badge)
    }

    private data class Harness(
        val viewModel: LanternYearViewModel,
        val coordinator: SyncCoordinator,
        val api: RecordingSyncApi,
    )

    private fun harness(
        store: InMemoryCampaignStore = InMemoryCampaignStore(),
        memberships: InMemoryMembershipStore = InMemoryMembershipStore(),
        createCampaign: (String) -> CampaignSummary = { name ->
            CampaignSummary(nextId().value, name, "OWNER", "", emptyList())
        },
    ): Harness {
        val api = RecordingSyncApi()
        lateinit var coordinator: SyncCoordinator
        val viewModel =
            LanternYearViewModel(
                buildComponents = { scope ->
                    val repository =
                        SettlementRepository(
                            store = store,
                            memberships = memberships,
                            createCampaign = createCampaign,
                        )
                    val controller = LanternYearController(repository)
                    coordinator =
                        SyncCoordinator(
                            engine = SyncEngine(api, SyncStoreFake()),
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
        return Harness(viewModel, coordinator, api)
    }

    private class RecordingSyncApi : SyncApi {
        val pulled = mutableListOf<String>()

        override fun listCampaigns(): List<CampaignSummary> = emptyList()

        override fun createCampaign(name: String): CampaignSummary =
            throw UnsupportedOperationException("createCampaign is injected as a lambda in this test")

        override fun pull(
            campaignId: String,
            since: Long,
        ): PullResponse {
            pulled += campaignId
            return PullResponse(latestRev = since, records = emptyList())
        }

        override fun push(
            campaignId: String,
            changeSet: ChangeSet,
        ): Long = changeSet.records.maxOfOrNull { it.rev } ?: 0L
    }

    private class SyncStoreFake : SyncStore {
        private val cursors = mutableMapOf<String, Long>()
        private val outbox = LinkedHashMap<Pair<String, String>, ChangeRecord>()

        override fun cursor(campaignId: String): Long = cursors[campaignId] ?: 0L

        override fun setCursor(
            campaignId: String,
            rev: Long,
        ) {
            cursors[campaignId] = rev
        }

        override fun pending(campaignId: String): List<ChangeRecord> =
            outbox.values.filter {
                it.campaignId ==
                    campaignId
            }

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
    }
}
