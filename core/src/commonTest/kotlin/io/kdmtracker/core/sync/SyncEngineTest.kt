package io.kdmtracker.core.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SyncEngineTest {
    private val campaignId = "campaign-1"

    private fun record(
        entityId: String = "settlement-1",
        entityType: String = "settlement",
        op: ChangeOp = ChangeOp.UPSERT,
        updatedAtMillis: Long = 1_700_000_000_000L,
        updatedAtCounter: Int = 0,
        payload: String = """{"name":"First Cradle"}""",
        rev: Long = 0,
    ) = ChangeRecord(
        campaignId = campaignId,
        entityType = entityType,
        entityId = entityId,
        op = op,
        baseRev = null,
        payload = payload,
        updatedAtMillis = updatedAtMillis,
        updatedAtCounter = updatedAtCounter,
        rev = rev,
    )

    @Test
    fun advancesCursorToLatestRevAfterMergingThePulledBatch() {
        val api = FakeSyncApi()
        api.pullResponse = PullResponse(latestRev = 7, records = listOf(record(rev = 5)))
        val store = FakeSyncStore()
        val engine = SyncEngine(api, store)

        val outcome = engine.sync(campaignId)

        assertEquals(0, api.pulledSince)
        assertEquals(7, store.cursor(campaignId))
        assertEquals(SyncOutcome(pulled = 1, pushed = 0, cursor = 7), outcome)
    }

    @Test
    fun newerRemoteOverwritesLocalAndDropsTheSupersededOutboxEntry() {
        val local = record(updatedAtMillis = 1_700_000_000_100L, payload = """{"name":"Local"}""")
        val remote = record(updatedAtMillis = 1_700_000_000_200L, payload = """{"name":"Remote"}""", rev = 3)
        val api = FakeSyncApi()
        api.pullResponse = PullResponse(latestRev = 3, records = listOf(remote))
        val store = FakeSyncStore()
        store.seedLocal(local)
        val engine = SyncEngine(api, store)

        val outcome = engine.sync(campaignId)

        assertEquals("""{"name":"Remote"}""", store.canonicalPayload("settlement", "settlement-1"))
        assertTrue(store.pending(campaignId).isEmpty(), "superseded local entry must be dropped from the outbox")
        assertTrue(api.pushedSets.isEmpty(), "a superseded local edit must not be re-pushed")
        assertEquals(0, outcome.pushed)
    }

    @Test
    fun canonicalLocalNewerThanStaleRemoteIsPreservedAndReEnqueued() {
        // The local edit was already acknowledged (outbox empty); only the canonical record remains.
        val canonicalLocal = record(updatedAtMillis = 1_700_000_000_500L, payload = """{"name":"Local acked"}""")
        val staleRemote = record(updatedAtMillis = 1_700_000_000_100L, payload = """{"name":"Remote"}""", rev = 6)
        val api = FakeSyncApi()
        api.pullResponse = PullResponse(latestRev = 6, records = listOf(staleRemote))
        val store = FakeSyncStore()
        store.seedCanonical(canonicalLocal)
        assertTrue(store.pending(campaignId).isEmpty(), "precondition: the local edit is not in the outbox")
        val engine = SyncEngine(api, store)

        val outcome = engine.sync(campaignId)

        assertEquals(
            """{"name":"Local acked"}""",
            store.canonicalPayload("settlement", "settlement-1"),
            "the canonical local winner must not be overwritten by the stale remote",
        )
        assertEquals(
            listOf(canonicalLocal),
            api.pushedSets.single().records,
            "the canonical local winner must be re-enqueued and pushed so the hub converges",
        )
        assertEquals(1, outcome.pushed)
        assertTrue(store.pending(campaignId).isEmpty(), "the re-enqueued entry is cleared once acked")
    }

    @Test
    fun canonicalLocalOlderThanRemoteIsOverwrittenAndNotReEnqueued() {
        val canonicalLocal = record(updatedAtMillis = 1_700_000_000_100L, payload = """{"name":"Local old"}""")
        val newerRemote = record(updatedAtMillis = 1_700_000_000_500L, payload = """{"name":"Remote"}""", rev = 8)
        val api = FakeSyncApi()
        api.pullResponse = PullResponse(latestRev = 8, records = listOf(newerRemote))
        val store = FakeSyncStore()
        store.seedCanonical(canonicalLocal)
        val engine = SyncEngine(api, store)

        val outcome = engine.sync(campaignId)

        assertEquals("""{"name":"Remote"}""", store.canonicalPayload("settlement", "settlement-1"))
        assertTrue(store.pending(campaignId).isEmpty(), "a canonical record the remote beat must not be re-enqueued")
        assertTrue(api.pushedSets.isEmpty())
        assertEquals(0, outcome.pushed)
    }

    @Test
    fun newerLocalIsPreservedAndPushedThenClearedOnAck() {
        val local = record(updatedAtMillis = 1_700_000_000_300L, payload = """{"name":"Local"}""")
        val remote = record(updatedAtMillis = 1_700_000_000_100L, payload = """{"name":"Remote"}""", rev = 4)
        val api = FakeSyncApi()
        api.pullResponse = PullResponse(latestRev = 4, records = listOf(remote))
        val store = FakeSyncStore()
        store.seedLocal(local)
        val engine = SyncEngine(api, store)

        val outcome = engine.sync(campaignId)

        assertEquals("""{"name":"Local"}""", store.canonicalPayload("settlement", "settlement-1"))
        assertEquals(1, outcome.pushed)
        assertEquals(listOf(local), api.pushedSets.single().records)
        assertTrue(store.pending(campaignId).isEmpty(), "outbox entry must be cleared once the push is acknowledged")
    }

    @Test
    fun pushFailureLeavesTheOutboxIntactAndStillAdvancesTheCursor() {
        val local = record(updatedAtMillis = 1_700_000_000_300L, payload = """{"name":"Local"}""")
        val remote = record(updatedAtMillis = 1_700_000_000_100L, payload = """{"name":"Remote"}""", rev = 4)
        val api = FakeSyncApi()
        api.pullResponse = PullResponse(latestRev = 9, records = listOf(remote))
        api.pushFailure = RuntimeException("network down")
        val store = FakeSyncStore()
        store.seedLocal(local)
        val engine = SyncEngine(api, store)

        assertFailsWith<RuntimeException> { engine.sync(campaignId) }

        assertEquals(listOf(local), store.pending(campaignId), "the outbox must survive a failed push for replay")
        assertEquals(
            9,
            store.cursor(campaignId),
            "a failed push must not block the cursor already advanced by the merge",
        )
    }

    @Test
    fun secondSyncPullsNothingWhenTheCursorIsCurrent() {
        val remote = record(rev = 5)
        val api = FakeSyncApi()
        api.pullResponse = PullResponse(latestRev = 5, records = listOf(remote))
        val store = FakeSyncStore()
        val engine = SyncEngine(api, store)
        engine.sync(campaignId)

        val second = engine.sync(campaignId)

        assertEquals(5, api.pulledSince, "the second pull must resume from the stored cursor")
        assertEquals(SyncOutcome(pulled = 0, pushed = 0, cursor = 5), second)
    }

    @Test
    fun cursorDoesNotAdvanceWhenAMergeFailsMidBatch() {
        val first = record(entityId = "settlement-1", updatedAtMillis = 1_700_000_000_200L, rev = 2)
        val second = record(entityId = "settlement-2", updatedAtMillis = 1_700_000_000_300L, rev = 3)
        val api = FakeSyncApi()
        api.pullResponse = PullResponse(latestRev = 3, records = listOf(first, second))
        val store = FakeSyncStore()
        store.failApplyMergedFor = "settlement" to "settlement-2"
        val engine = SyncEngine(api, store)

        assertFailsWith<IllegalStateException> { engine.sync(campaignId) }

        assertEquals(0, store.cursor(campaignId), "the cursor must only advance after the whole batch merges")
    }

    private class FakeSyncApi : SyncApi {
        var pullResponse: PullResponse = PullResponse(latestRev = 0, records = emptyList())
        var pulledSince: Long = -1
        var pushFailure: Throwable? = null
        val pushedSets = mutableListOf<ChangeSet>()

        override fun pull(
            campaignId: String,
            since: Long,
        ): PullResponse {
            pulledSince = since
            return pullResponse.copy(records = pullResponse.records.filter { it.rev > since })
        }

        override fun push(
            campaignId: String,
            changeSet: ChangeSet,
        ): Long {
            pushFailure?.let { throw it }
            pushedSets += changeSet
            return changeSet.records.maxOfOrNull { it.rev } ?: 0L
        }
    }

    private class FakeSyncStore : SyncStore {
        private val cursors = mutableMapOf<String, Long>()
        private val canonical = mutableMapOf<Pair<String, String>, ChangeRecord>()
        private val outbox = linkedMapOf<Pair<String, String>, ChangeRecord>()
        var failApplyMergedFor: Pair<String, String>? = null

        override fun cursor(campaignId: String): Long = cursors[campaignId] ?: 0L

        override fun setCursor(
            campaignId: String,
            rev: Long,
        ) {
            cursors[campaignId] = rev
        }

        override fun pending(campaignId: String): List<ChangeRecord> = outbox.values.toList()

        override fun local(
            campaignId: String,
            entityType: String,
            entityId: String,
        ): ChangeRecord? = canonical[entityType to entityId]

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
            val key = record.entityType to record.entityId
            if (key == failApplyMergedFor) throw IllegalStateException("merge failed for $key")
            canonical[key] = record
            outbox.remove(key)
        }

        override fun clearAcknowledged(
            campaignId: String,
            records: List<ChangeRecord>,
        ) {
            records.forEach { outbox.remove(it.entityType to it.entityId) }
        }

        fun seedLocal(record: ChangeRecord) {
            val key = record.entityType to record.entityId
            outbox[key] = record
            canonical[key] = record
        }

        /** Seeds the canonical row only, as it is after an acknowledged local edit (outbox empty). */
        fun seedCanonical(record: ChangeRecord) {
            canonical[record.entityType to record.entityId] = record
        }

        fun canonicalPayload(
            entityType: String,
            entityId: String,
        ): String? = canonical[entityType to entityId]?.payload
    }
}
