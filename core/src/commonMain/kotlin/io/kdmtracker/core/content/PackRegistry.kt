package io.kdmtracker.core.content

/** A pack the app ships. A [base] pack goes into every campaign; any other is an expansion the player opts into. */
data class RegisteredPack(
    val pack: ContentPack,
    val base: Boolean = false,
)

/** What is wrong with two packs sitting in one campaign. */
enum class RelationKind { INCOMPATIBLE, COMPATIBILITY_ISSUES }

/** Who says so, strongest first: only an [OFFICIAL] incompatibility stops a campaign. */
enum class RelationSource { OFFICIAL, COMMUNITY, CHART }

/** A known problem between packs [a] and [b]. The order of the two ids does not matter. */
data class PackRelation(
    val a: String,
    val b: String,
    val kind: RelationKind,
    val source: RelationSource,
)

/** The packs a campaign ends up with, or why a choice of expansions cannot be honoured. */
sealed interface PackSelection {
    /**
     * [enabled] lists the base packs first, then the chosen expansions, in registry order. [warnings] are the
     * relations between the packs that were not stopped, for the player to confirm.
     */
    data class Ok(
        val enabled: List<String>,
        val warnings: List<PackRelation> = emptyList(),
    ) : PackSelection

    data class Blocked(
        val reason: String,
    ) : PackSelection
}

/**
 * Every content pack the app knows, in one place.
 *
 * A campaign stores only pack ids; this registry is what turns an id back into a pack and what decides
 * which ids a new campaign may enable.
 */
class PackRegistry(
    private val entries: List<RegisteredPack>,
    private val relations: List<PackRelation> = emptyList(),
) {
    private val byId: Map<String, ContentPack> = entries.associate { it.pack.id to it.pack }

    /** The ids every campaign carries, in registry order. */
    val baseIds: List<String> = entries.filter { it.base }.map { it.pack.id }

    /** The expansions a campaign may leave out. */
    val optional: List<ContentPack> = entries.filterNot { it.base }.map { it.pack }

    fun find(id: String): ContentPack? = byId[id]

    /**
     * Resolves the expansions the player picked into the ids a campaign enables.
     *
     * The base packs are always in. A pack nobody registered is refused, and so is one whose
     * `requires` are not all enabled: the player picks the requirement too, nothing is enabled on
     * their behalf. A relation between two chosen packs warns, or refuses when it is an official
     * incompatibility.
     */
    fun select(chosen: Collection<String>): PackSelection {
        chosen.firstOrNull { it !in byId }?.let { return PackSelection.Blocked("unknown content pack '$it'") }
        val enabled = entries.map { it.pack.id }.filter { it in baseIds || it in chosen }
        for (id in enabled) {
            val missing = byId.getValue(id).requires.firstOrNull { it !in enabled } ?: continue
            return PackSelection.Blocked("content pack '$id' requires '$missing'")
        }
        return judge(enabled, enabled)
    }

    /**
     * Resolves the packs an existing campaign ends up with when [wanted] is the full set it should have.
     *
     * Packs only get added: a pack in [current] that [wanted] leaves out is refused, because nothing
     * yet says whether the campaign's state already uses its content. A pack in [current] that this
     * build does not know stays as it is, so a campaign made on a newer build keeps its ids. Each added
     * pack must be registered and have its `requires` among the packs the campaign ends up with. Only
     * the relations of an added pack count: a pair the campaign already has is left alone.
     */
    fun change(
        current: List<String>,
        wanted: Collection<String>,
    ): PackSelection {
        wanted.firstOrNull { it !in byId && it !in current }?.let {
            return PackSelection.Blocked("unknown content pack '$it'")
        }
        current.firstOrNull { it !in wanted }?.let {
            return PackSelection.Blocked("content pack '$it' is already enabled and cannot be disabled")
        }
        val added = entries.map { it.pack.id }.filter { it in wanted && it !in current }
        val enabled = current + added
        for (id in added) {
            val missing = byId.getValue(id).requires.firstOrNull { it !in enabled } ?: continue
            return PackSelection.Blocked("content pack '$id' requires '$missing'")
        }
        return judge(enabled, added)
    }

    /** Refuses on an official incompatibility among [enabled] that involves an [added] pack; warns about the rest. */
    private fun judge(
        enabled: List<String>,
        added: List<String>,
    ): PackSelection {
        val found =
            relations
                .filter { it.a in enabled && it.b in enabled && (it.a in added || it.b in added) }
                .groupBy { setOf(it.a, it.b) to it.kind }
                .values
                .map { pair -> pair.minBy { it.source } }
        found.firstOrNull { it.kind == RelationKind.INCOMPATIBLE && it.source == RelationSource.OFFICIAL }?.let {
            val (first, second) = listOf(it.a, it.b).sortedBy(enabled::indexOf)
            return PackSelection.Blocked("content pack '$first' is incompatible with '$second'")
        }
        return PackSelection.Ok(enabled, found)
    }

    companion object {
        /** The packs this build ships. */
        val shipped =
            PackRegistry(
                listOf(
                    RegisteredPack(Core16Content.pack, base = true),
                    RegisteredPack(Legacy15Content.pack, base = true),
                ),
            )
    }
}
