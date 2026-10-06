package io.kdmtracker.core.sync

import io.kdmtracker.core.domain.HybridLogicalClock

/** Outcome of merging a remote [ChangeRecord] into the local record for the same entity. */
data class MergeResult(
    val record: ChangeRecord?,
    val localWins: Boolean,
)

/**
 * Pure per-record last-write-wins merge policy.
 *
 * Records for the same entity are ordered by [HybridLogicalClock] over
 * (`updatedAtMillis`, `updatedAtCounter`). A tombstone is modelled as [ChangeOp.DELETE] and is
 * ordered by the same clock, so a newer delete supersedes an older upsert and vice versa.
 *
 * When the clocks are equal the local record wins: a local edit is never dropped on a tie.
 */
object MergePolicy {
    fun merge(
        local: ChangeRecord?,
        remote: ChangeRecord,
    ): MergeResult {
        if (local == null) return MergeResult(record = remote, localWins = false)

        val remoteClock = HybridLogicalClock(remote.updatedAtMillis, remote.updatedAtCounter)
        val localClock = HybridLogicalClock(local.updatedAtMillis, local.updatedAtCounter)

        return if (remoteClock > localClock) {
            MergeResult(record = remote, localWins = false)
        } else {
            MergeResult(record = local, localWins = true)
        }
    }
}
