package io.kdmtracker.core.domain

import kotlinx.serialization.Serializable

/**
 * A single append-only entry in the resource ledger.
 *
 * @property name the resource's human-readable name.
 * @property category the sheet grouping the resource belongs to.
 * @property delta the change in quantity: positive when gained, negative when spent.
 */
@Serializable
data class ResourceEntry(
    val name: String,
    val category: ResourceCategory,
    val delta: Int,
)

/**
 * A resource the settlement can store.
 *
 * Content packs carry these definitions, so a resource's [category] and [material] are content,
 * never inferred from its name. The [id] is the stable key: the ledger, the recipes and the UI all
 * refer to a resource by it, so a re-translated name can never orphan the data.
 *
 * @property id the stable, language-neutral identity, chosen once and never derived from a name.
 * @property names the display name per language (Spanish always present, English growing).
 * @property category the rules dimension — which sheet section the resource belongs to.
 * @property material the aggregate dimension — which of the summary's four buckets the resource
 *   feeds. `null` when it feeds none of them (a herb, a bar of iron, a skull): the resource is
 *   still real and still craftable, it simply does not appear in the four-cell grid.
 * @property source the monster/deck the resource comes from, for grouping in the UI. Empty when
 *   unknown; it never affects the ledger.
 */
@Serializable
data class ResourceDefinition(
    val id: ResourceId,
    val names: LocalizedText,
    val category: ResourceCategory,
    val material: ResourceMaterial? = null,
    val source: String = "",
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
    val category: ResourceCategory,
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
        category: ResourceCategory,
    ): List<StorageLine> = project(entries).filter { it.category == category }
}
