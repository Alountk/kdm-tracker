package io.kdmtracker.core.sync

/**
 * Outbound port over the sync hub wire protocol.
 *
 * Implementations live outside `:core` (the Android app talks HTTP); the engine depends only on this
 * interface so pull/merge/push orchestration stays pure and JVM-testable.
 */
interface SyncApi {
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
     * Applies a winning merged record to the canonical state.
     *
     * A merged record supersedes any pending outbox entry for the same `(entityType, entityId)`, so
     * the entry is removed as part of applying it (Ruling Y). This prevents re-pushing an edit the
     * remote has already overwritten.
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
