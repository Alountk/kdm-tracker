package io.kdmtracker.core.sync

import kotlinx.serialization.Serializable

/** Operation applied to a single entity within a [ChangeRecord]. */
enum class ChangeOp {
    UPSERT,
    DELETE,
}

/**
 * One entity mutation in the sync wire protocol.
 *
 * [baseRev] is the revision the change was computed against, or `null` when the change does not
 * depend on a prior revision. It is carried on the wire but the server does not interpret it yet:
 * conflict resolution (last-write-wins/merge) is deferred to plan 3c and will run client-side, using
 * [baseRev] to detect whether the server has replaced the revision the change was computed against.
 * [rev] is the server-assigned revision and is `0` for client-originated records that have not yet
 * been acknowledged.
 */
@Serializable
data class ChangeRecord(
    val campaignId: String,
    val entityType: String,
    val entityId: String,
    val op: ChangeOp,
    val baseRev: Long?,
    val payload: String,
    val updatedAtMillis: Long,
    val updatedAtCounter: Int,
    val rev: Long = 0,
)

/** Ordered batch of [ChangeRecord]s exchanged with the sync hub. */
@Serializable
data class ChangeSet(
    val records: List<ChangeRecord>,
)

/** Result of a pull request: the hub's current revision plus the changes since the client's revision. */
@Serializable
data class PullResponse(
    val latestRev: Long,
    val records: List<ChangeRecord>,
)
