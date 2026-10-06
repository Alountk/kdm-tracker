package io.kdmtracker.core.sync

/**
 * Outbound port over the sync hub wire protocol.
 *
 * Implementations live outside `:core` (the Android app talks HTTP); the engine depends only on this
 * interface so pull/merge/push orchestration stays pure and JVM-testable.
 */
interface SyncApi {
    /** Lists the campaigns the signed-in user can reach. */
    fun listCampaigns(): List<CampaignSummary>

    /** Pulls remote changes for [campaignId] recorded after [since], plus the hub's current revision. */
    fun pull(
        campaignId: String,
        since: Long,
    ): PullResponse

    /** Pushes a batch of local changes and returns the hub's new latest revision. */
    fun push(
        campaignId: String,
        changeSet: ChangeSet,
    ): Long
}

/**
 * Local persistence port the engine reads and writes.
 *
 * The store owns the sync cursor, the pending outbox, and the canonical entity state. The engine is
 * otherwise stateless.
 */
interface SyncStore {
    /** The revision the client last synced through; `0` when it has never synced. */
    fun cursor(campaignId: String): Long

    /** Persists the client's new sync cursor. */
    fun setCursor(
        campaignId: String,
        rev: Long,
    )

    /** Local changes not yet acknowledged by the hub, keyed by `(entityType, entityId)`. */
    fun pending(campaignId: String): List<ChangeRecord>

    /**
     * The canonical local record for the entity, or `null` when the store holds none.
     *
     * The outbox alone is not the truth: once the hub acknowledges an edit the pending entry is
     * cleared while the canonical row remains. Merging a remote change must compare against this
     * record (Ruling AD), otherwise an already-acknowledged local edit looks absent and a stale
     * remote silently reverts it.
     */
    fun local(
        campaignId: String,
        entityType: String,
        entityId: String,
    ): ChangeRecord?

    /**
     * Records [record] as a pending outbox entry for its `(entityType, entityId)`.
     *
     * Used to re-enqueue a canonical local winner the hub has not seen: when a remote change is
     * older than the canonical local record, the local record wins the merge but its outbox entry
     * was already cleared, so without re-enqueuing the hub would never converge (Ruling AD).
     */
    fun enqueue(
        campaignId: String,
        record: ChangeRecord,
    )

    /**
     * Applies a winning merged record to the canonical state.
     *
     * A merged record supersedes any pending outbox entry for the same `(entityType, entityId)`, so
     * the entry is removed as part of applying it (Ruling Y). This prevents re-pushing an edit the
     * remote has already overwritten. An unknown entity type is skipped, not fatal (Ruling AF), so
     * one unrecognised record cannot wedge the whole sync pass.
     */
    fun applyMerged(
        campaignId: String,
        record: ChangeRecord,
    )

    /** Removes exactly [records] from the outbox once the hub acknowledges them. */
    fun clearAcknowledged(
        campaignId: String,
        records: List<ChangeRecord>,
    )
}
