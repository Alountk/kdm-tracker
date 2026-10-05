package io.kdmtracker.core.domain

/**
 * A hybrid logical clock used to order last-write-wins updates across clients.
 *
 * It combines a physical [millis] reading with a logical [counter] that breaks ties when the
 * wall clock stalls or moves backwards. Values are totally ordered by [millis] then [counter].
 */
data class HybridLogicalClock(
    val millis: Long,
    val counter: Int,
) : Comparable<HybridLogicalClock> {
    override fun compareTo(other: HybridLogicalClock): Int {
        val millisComparison = millis.compareTo(other.millis)
        return if (millisComparison != 0) millisComparison else counter.compareTo(other.counter)
    }

    companion object {
        /**
         * Produces the next clock value.
         *
         * When [previous] is null or the [wallMillis] moved forward the counter resets to 0,
         * otherwise the physical time is kept and the counter is incremented.
         */
        fun tick(
            previous: HybridLogicalClock?,
            wallMillis: Long,
        ): HybridLogicalClock =
            when {
                previous == null -> HybridLogicalClock(wallMillis, 0)
                wallMillis > previous.millis -> HybridLogicalClock(wallMillis, 0)
                else -> HybridLogicalClock(previous.millis, previous.counter + 1)
            }
    }
}
