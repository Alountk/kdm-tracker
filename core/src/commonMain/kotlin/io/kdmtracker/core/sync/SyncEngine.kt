package io.kdmtracker.core.sync

/** Result of one [SyncEngine.sync] pass. */
data class SyncOutcome(
    val pulled: Int,
    val pushed: Int,
    val cursor: Long,
)

/**
 * Orchestrates one sync pass: pull → merge (per-record LWW) → push.
 *
 * Ordering and failure semantics (Ruling Z, Review Focus 3–4):
 * 1. Pull from the stored cursor.
 * 2. Merge each pulled record into the pending outbox; a remote that wins is applied and its
 *    superseded local outbox entry dropped (Ruling Y). A local that wins stays pending.
 * 3. Advance the cursor to the pulled `latestRev` **only after the whole batch merges**.
 * 4. Push whatever is still pending; clear it on acknowledgement.
 *
 * A push failure propagates without clearing the outbox (the caller retries); the cursor stays
 * advanced because the merged batch is already durable.
 */
class SyncEngine(
    private val api: SyncApi,
    private val store: SyncStore,
) {
    fun sync(campaignId: String): SyncOutcome {
        val response = api.pull(campaignId, store.cursor(campaignId))

        val known = store.pending(campaignId).associateBy { it.entityType to it.entityId }.toMutableMap()
        response.records.forEach { remote ->
            val key = remote.entityType to remote.entityId
            if (!MergePolicy.merge(local = known[key], remote = remote).localWins) {
                store.applyMerged(campaignId, remote)
                known[key] = remote
            }
        }
        store.setCursor(campaignId, response.latestRev)

        val toPush = store.pending(campaignId)
        if (toPush.isNotEmpty()) {
            api.push(campaignId, ChangeSet(toPush))
            store.clearAcknowledged(campaignId, toPush)
        }

        return SyncOutcome(pulled = response.records.size, pushed = toPush.size, cursor = response.latestRev)
    }
}
