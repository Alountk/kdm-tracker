package io.kdmtracker.core.persistence

import io.kdmtracker.core.domain.HybridLogicalClock
import kotlin.time.Clock

/**
 * Supplies the hybrid logical clock stamped on a local mutation, so last-write-wins has an
 * ordering signal to compare.
 *
 * Implementations are injected into the mutation path, which lets tests drive a deterministic
 * clock and lets the production clock read the device wall time.
 */
fun interface SyncClock {
    /** Returns the next clock value, ordered after [previous] when wall time has not advanced. */
    fun tick(previous: HybridLogicalClock?): HybridLogicalClock

    companion object {
        /** A clock backed by the system wall time, breaking ties with the HLC counter. */
        fun system(): SyncClock =
            SyncClock { previous ->
                HybridLogicalClock.tick(previous, Clock.System.now().toEpochMilliseconds())
            }
    }
}
