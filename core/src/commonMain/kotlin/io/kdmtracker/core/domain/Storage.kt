package io.kdmtracker.core.domain

import io.kdmtracker.core.content.ContentIdentity
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * A single append-only entry in the resource ledger.
 *
 * @property id the resource it records, by its stable identity.
 * @property delta the change in quantity: positive when gained, negative when spent.
 */
@Serializable(with = ResourceEntrySerializer::class)
data class ResourceEntry(
    val id: ResourceId,
    val delta: Int,
)

/**
 * Reads both shapes a [ResourceEntry] has ever been written in.
 *
 * The new shape names the resource by [ResourceId]. The old one named it by its display string,
 * because that *was* the identity — so it is mapped through [ContentIdentity]. An old name the
 * table does not know is **not** dropped: it survives under a synthesized `unknown.…` id, because
 * a discarded entry is a count the user silently loses rather than a validation error they can fix.
 */
object ResourceEntrySerializer : KSerializer<ResourceEntry> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("io.kdmtracker.core.domain.ResourceEntry") {
            element<String>("id")
            element<Int>("delta")
        }

    override fun serialize(
        encoder: Encoder,
        value: ResourceEntry,
    ) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.id.value)
            encodeIntElement(descriptor, 1, value.delta)
        }
    }

    override fun deserialize(decoder: Decoder): ResourceEntry {
        val json = decoder as? JsonDecoder ?: error("ResourceEntry is persisted as JSON")
        val element: JsonElement = json.decodeJsonElement()
        val fields = element.jsonObject
        val delta =
            fields
                .getValue("delta")
                .jsonPrimitive.content
                .toInt()
        val id =
            fields["id"]?.jsonPrimitive?.content
                ?: fields["name"]?.jsonPrimitive?.content?.let { name ->
                    ContentIdentity.resourceIdFor(name)?.value ?: synthesizedId(name)
                }
                ?: error("a resource entry needs an id, or a legacy name to resolve one")

        return ResourceEntry(ResourceId(id), delta)
    }

    private fun synthesizedId(name: String): String =
        "unknown." + name.lowercase().map { if (it.isLetterOrDigit()) it else '_' }.joinToString("")
}

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
 * @property materials the aggregate dimension — which of the summary's four buckets the resource
 *   can pay. Empty for a herb, a bar of iron or a skull, which feed none of them. **Several for a
 *   wildcard**, like the `???` card that pays a bone, a hide or an organ; while it sits in storage
 *   it is committed to no single bucket, so the summary counts only a one-element set.
 * @property source the monster/deck the resource comes from, for grouping in the UI. Empty when
 *   unknown; it never affects the ledger.
 */
@Serializable
data class ResourceDefinition(
    val id: ResourceId,
    val names: LocalizedText,
    val category: ResourceCategory,
    val materials: Set<ResourceMaterial> = emptySet(),
    val source: String = "",
)

/**
 * A projected inventory line: the net quantity of one resource.
 *
 * @property id the resource it counts.
 * @property quantity the net quantity summed from the ledger; may be negative.
 */
data class StorageLine(
    val id: ResourceId,
    val quantity: Int,
)

/**
 * Projects the append-only [ResourceEntry] ledger into storage lines keyed by resource.
 *
 * Quantities are the net sum of deltas. A negative net is kept rather than clamped to zero, so
 * overspending stays visible in the projection.
 *
 * The line carries no name and no material: both are content, resolved from the catalog by [id]
 * where they are displayed. Keeping them out is what lets an entry written before ids existed
 * decode without losing its count.
 */
object Storage {
    /**
     * Projects [entries] into [StorageLine]s, summing deltas per [ResourceId].
     *
     * Lines whose net quantity is zero are dropped, and the result is sorted by id so the output
     * is deterministic.
     */
    fun project(entries: List<ResourceEntry>): List<StorageLine> =
        entries
            .groupBy { it.id }
            .map { (id, group) -> StorageLine(id = id, quantity = group.sumOf { it.delta }) }
            .filter { it.quantity != 0 }
            .sortedBy { it.id.value }

    /** Sums the net quantity of every resource in [entries]. */
    fun total(entries: List<ResourceEntry>): Int = project(entries).sumOf { it.quantity }

    /** The net quantity of [id], or zero when the ledger has no entry for it. */
    fun quantityOf(
        entries: List<ResourceEntry>,
        id: ResourceId,
    ): Int = project(entries).firstOrNull { it.id == id }?.quantity ?: 0

    /**
     * The net quantity per [ResourceMaterial] — the settlement summary's four buckets.
     *
     * Every variant of a material adds into one bucket, which is the whole point: a monster's fur
     * and the generic hide are both [ResourceMaterial.HIDE] and must not split across two cells.
     *
     * A resource counts only when it commits to **exactly one** material. A wildcard — the `???`
     * card, which can pay a bone, a hide or an organ — commits to none of them while it is held,
     * and neither does a herb or a bar of iron, whose set is empty. Counting a wildcard in each of
     * its buckets would make the four cells add up to more stock than the settlement holds.
     */
    fun byMaterial(
        entries: List<ResourceEntry>,
        definitions: List<ResourceDefinition>,
    ): Map<ResourceMaterial, Int> {
        val bucketsById =
            definitions
                .filter { it.materials.size == 1 }
                .associate { it.id to it.materials.single() }
        return project(entries)
            .mapNotNull { line -> bucketsById[line.id]?.let { it to line.quantity } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, quantities) -> quantities.sum() }
    }
}
