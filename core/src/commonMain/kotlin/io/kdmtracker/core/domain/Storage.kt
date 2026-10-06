package io.kdmtracker.core.domain

/**
 * A single append-only entry in the resource ledger.
 *
 * @property name the resource's human-readable name.
 * @property category the sheet grouping the resource belongs to.
 * @property delta the change in quantity: positive when gained, negative when spent.
 */
data class ResourceEntry(
    val name: String,
    val category: String,
    val delta: Int,
)

/**
 * A projected inventory line: the net quantity of one resource in one category.
 *
 * @property name the resource's human-readable name.
 * @property category the sheet grouping the resource belongs to.
 * @property quantity the net quantity summed from the ledger; may be negative.
 */
data class StorageLine(
    val name: String,
    val category: String,
    val quantity: Int,
)

/**
 * Projects the append-only [ResourceEntry] ledger into storage lines grouped by category.
 *
 * Quantities are the net sum of deltas. A negative net is kept rather than clamped to zero,
 * so overspending stays visible in the projection.
 */
object Storage {
    /**
     * Projects [entries] into [StorageLine]s, summing deltas per `(name, category)`.
     *
     * Lines whose net quantity is zero are dropped, and the result is sorted by category
     * then name so the output is deterministic.
     */
    fun project(entries: List<ResourceEntry>): List<StorageLine> =
        entries
            .groupBy { it.name to it.category }
            .map { (key, group) ->
                val (name, category) = key
                StorageLine(name = name, category = category, quantity = group.sumOf { it.delta })
            }.filter { it.quantity != 0 }
            .sortedWith(compareBy({ it.category }, { it.name }))

    /** Sums the net quantity of every resource in [entries]. */
    fun total(entries: List<ResourceEntry>): Int = project(entries).sumOf { it.quantity }

    /** Projects [entries], returning only the lines whose category equals [category]. */
    fun forCategory(
        entries: List<ResourceEntry>,
        category: String,
    ): List<StorageLine> = project(entries).filter { it.category == category }
}
