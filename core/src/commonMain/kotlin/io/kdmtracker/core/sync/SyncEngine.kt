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
 * Ordering and failure semantics:
 * 1. Pull from the stored cursor.
 * 2. Merge each pulled record against the **canonical local record**, falling back to
 *    the outbox when the store has no canonical entry. A remote that wins is applied and its
 *    superseded local outbox entry dropped. A local that wins stays pending; when it had
 *    already been acknowledged (so it is canonical-only), it is re-enqueued so the hub converges.
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
    /** The campaigns the signed-in user can reach, straight from the hub. */
    fun listCampaigns(): List<CampaignSummary> = api.listCampaigns()

    fun sync(campaignId: String): SyncOutcome {
        val response = api.pull(campaignId, store.cursor(campaignId))

        val pending = store.pending(campaignId)
        val pendingByKey = pending.associateBy { it.entityType to it.entityId }
        val pendingKeys = pending.mapTo(mutableSetOf()) { it.entityType to it.entityId }
        val known = mutableMapOf<Pair<String, String>, ChangeRecord?>()

        response.records.forEach { remote ->
            val key = remote.entityType to remote.entityId
            val local =
                if (known.containsKey(key)) {
                    known[key]
                } else {
                    (store.local(campaignId, remote.entityType, remote.entityId) ?: pendingByKey[key])
                        .also { known[key] = it }
                }

            if (MergePolicy.merge(local = local, remote = remote).localWins) {
                if (local != null && key !in pendingKeys) {
                    // The canonical local record won but is no longer in the outbox (already
                    // acknowledged); re-enqueue it so the hub learns the newer local value.
                    store.enqueue(campaignId, local)
                    pendingKeys += key
                }
            } else {
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
