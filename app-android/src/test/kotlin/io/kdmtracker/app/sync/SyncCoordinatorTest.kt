package io.kdmtracker.app.sync

import io.kdmtracker.core.sync.CampaignSummary
import io.kdmtracker.core.sync.ChangeOp
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
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Covers the app-side sync orchestration [SyncCoordinator].
 *
 * The coordinator is deliberately driven with [Dispatchers.Unconfined] so a test observes the exact
 * state transitions without a test dispatcher: the engine is synchronous, so the launched block runs
 * to completion on the calling thread and the "in-flight" state can be captured from inside the fake
 * hub. The fakes mirror the `:core` ports, so no Android or HTTP machinery is involved.
 */
class SyncCoordinatorTest {
    @Test
    fun aSuccessfulSyncMovesFromIdleThroughSyncingBackToIdle() {
        val store = FakeSyncStore()
        lateinit var coordinator: SyncCoordinator
        var stateDuringPull: SyncState? = null
        val api =
            FakeSyncApi(
                onPull = { _, _ ->
                    stateDuringPull = coordinator.state.value
                    PullResponse(latestRev = 7L, records = emptyList())
                },
            )
        coordinator =
            SyncCoordinator(
                engine = SyncEngine(api, store),
                campaignId = CAMPAIGN_ID,
                memberships = InMemoryMembershipStore(),
                scope = CoroutineScope(Dispatchers.Unconfined),
                tokenProvider = { "token" },
                dispatcher = Dispatchers.Unconfined,
                clock = { 1_000L },
            )

        assertEquals(SyncState.Idle(), coordinator.state.value)

        coordinator.syncNow()

        assertEquals(SyncState.Syncing, stateDuringPull)
        assertEquals(SyncState.Idle(lastSyncedAtMillis = 1_000L), coordinator.state.value)
        assertEquals(7L, store.cursorOf(CAMPAIGN_ID))
    }

    @Test
    fun aFailingPushSurfacesAnErrorAndLeavesTheOutboxIntact() {
        val store =
            FakeSyncStore().apply {
                enqueue(record(entityType = "settlement", entityId = "s1"))
            }
        val api = FakeSyncApi(pushError = IOException("hub down"))
        val coordinator =
            SyncCoordinator(
                engine = SyncEngine(api, store),
                campaignId = CAMPAIGN_ID,
                memberships = InMemoryMembershipStore(),
                scope = CoroutineScope(Dispatchers.Unconfined),
                tokenProvider = { "token" },
                dispatcher = Dispatchers.Unconfined,
                clock = { 1_000L },
            )

        coordinator.syncNow()

        val state = coordinator.state.value
        assertIs<SyncState.Error>(state)
        assertEquals("hub down", state.message)
        assertEquals(listOf("s1"), store.pendingOf(CAMPAIGN_ID).map(ChangeRecord::entityId))
    }

    @Test
    fun syncWithoutATokenReportsNotConfiguredAndNeverCallsTheHub() {
        var pullCalls = 0
        val api =
            FakeSyncApi(
                onPull = { _, since ->
                    pullCalls++
                    PullResponse(since, emptyList())
                },
            )
        val coordinator =
            SyncCoordinator(
                engine = SyncEngine(api, FakeSyncStore()),
                campaignId = CAMPAIGN_ID,
                memberships = InMemoryMembershipStore(),
                scope = CoroutineScope(Dispatchers.Unconfined),
                tokenProvider = { null },
                dispatcher = Dispatchers.Unconfined,
            )

        coordinator.syncNow()

        assertEquals(SyncState.NotConfigured, coordinator.state.value)
        assertEquals(0, pullCalls, "a doomed unauthorised request must not be attempted")
    }

    @Test
    fun aSuccessfulSyncInvokesTheOnSyncedCallback() {
        var reloads = 0
        val coordinator =
            SyncCoordinator(
                engine = SyncEngine(FakeSyncApi(), FakeSyncStore()),
                campaignId = CAMPAIGN_ID,
                memberships = InMemoryMembershipStore(),
                scope = CoroutineScope(Dispatchers.Unconfined),
                tokenProvider = { "token" },
                dispatcher = Dispatchers.Unconfined,
                onSynced = { reloads++ },
            )

        coordinator.syncNow()

        assertEquals(1, reloads)
    }

    /**
     * Hammers [SyncCoordinator.syncNow] from many threads released together by a barrier so their
     * guard checks genuinely race, and records the highest number of `engine.sync` passes that were
     * ever in flight at once. With an atomic in-flight claim that peak is always 1; a non-atomic
     * check-then-set lets two threads both start a pass (concurrent SQLDelight access + duplicate
     * pushes). The pull holds a short sleep to widen the overlap window, so the race is not a
     * matter of chance across the repeated rounds.
     */
    @Test
    fun overlappingSyncRequestsNeverRunTwoEnginePasses() {
        val concurrentPasses = AtomicInteger(0)
        val maxConcurrentPasses = AtomicInteger(0)
        val api =
            FakeSyncApi(
                onPull = { _, _ ->
                    val running = concurrentPasses.incrementAndGet()
                    maxConcurrentPasses.updateAndGet { peak -> maxOf(peak, running) }
                    Thread.sleep(5)
                    concurrentPasses.decrementAndGet()
                    PullResponse(latestRev = 1L, records = emptyList())
                },
            )
        val coordinator =
            SyncCoordinator(
                engine = SyncEngine(api, FakeSyncStore()),
                campaignId = CAMPAIGN_ID,
                memberships = InMemoryMembershipStore(),
                scope = CoroutineScope(Dispatchers.IO),
                tokenProvider = { "token" },
                dispatcher = Dispatchers.IO,
            )

        repeat(ROUNDS) {
            val barrier = CyclicBarrier(THREADS)
            (0 until THREADS)
                .map {
                    thread {
                        barrier.await()
                        coordinator.syncNow()
                    }
                }.forEach { it.join() }
        }

        assertEquals(1, maxConcurrentPasses.get(), "two engine.sync passes overlapped")
    }

    private fun record(
        entityType: String,
        entityId: String,
    ): ChangeRecord =
        ChangeRecord(
            campaignId = CAMPAIGN_ID,
            entityType = entityType,
            entityId = entityId,
            op = ChangeOp.UPSERT,
            baseRev = null,
            payload = "{}",
            updatedAtMillis = 1L,
            updatedAtCounter = 0,
            rev = 0,
        )

    private class FakeSyncApi(
        private val onPull: (String, Long) -> PullResponse = { _, since -> PullResponse(since, emptyList()) },
        private val pushError: Exception? = null,
    ) : SyncApi {
        override fun listCampaigns(): List<CampaignSummary> = emptyList()

        override fun createCampaign(name: String): CampaignSummary =
            throw UnsupportedOperationException("createCampaign is not exercised by SyncCoordinatorTest")

        override fun pull(
            campaignId: String,
            since: Long,
        ): PullResponse = onPull(campaignId, since)

        override fun push(
            campaignId: String,
            changeSet: ChangeSet,
        ): Long {
            pushError?.let { throw it }
            return 1L
        }
    }

    private class FakeSyncStore : SyncStore {
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

        fun enqueue(record: ChangeRecord) {
            outbox[record.entityType to record.entityId] = record
        }

        fun cursorOf(campaignId: String): Long = cursor(campaignId)

        fun pendingOf(campaignId: String): List<ChangeRecord> = pending(campaignId)
    }

    private companion object {
        const val CAMPAIGN_ID = "01J0000000000000000000000A"
        const val THREADS = 16
        const val ROUNDS = 200
    }
}
