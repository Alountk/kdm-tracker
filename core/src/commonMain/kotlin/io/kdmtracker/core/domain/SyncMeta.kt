package io.kdmtracker.core.domain

/**
 * Sync metadata carried by every mutable record.
 *
 * @property updatedAt the hybrid logical clock of the last write.
 * @property rev a monotonic revision counter, incremented on every local mutation.
 * @property deletedAt the hybrid logical clock of the delete, or null when the record is live.
 */
data class SyncMeta(
    val updatedAt: HybridLogicalClock,
    val rev: Long = 0,
    val deletedAt: HybridLogicalClock? = null,
)
