package io.kdmtracker.app.sync

import io.kdmtracker.core.sync.ChangeOp
import io.kdmtracker.core.sync.ChangeRecord
import io.kdmtracker.core.sync.ChangeSet
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
                scope = CoroutineScope(Dispatchers.Unconfined),
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
                scope = CoroutineScope(Dispatchers.Unconfined),
                dispatcher = Dispatchers.Unconfined,
                clock = { 1_000L },
            )

        coordinator.syncNow()

        val state = coordinator.state.value
        assertIs<SyncState.Error>(state)
        assertEquals("hub down", state.message)
        assertEquals(listOf("s1"), store.pendingOf(CAMPAIGN_ID).map(ChangeRecord::entityId))
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
    }
}
