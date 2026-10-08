package io.kdmtracker.core.content

import io.kdmtracker.core.domain.ContentEntry
import io.kdmtracker.core.domain.Gear
import io.kdmtracker.core.domain.Innovation
import io.kdmtracker.core.domain.Modifier
import io.kdmtracker.core.domain.ResourceDefinition
import io.kdmtracker.core.domain.SettlementEvent
import io.kdmtracker.core.domain.SettlementLocation
import io.kdmtracker.core.domain.TimelineEntry

sealed interface CatalogResult {
    data class Ok(
        val catalog: ContentCatalog,
    ) : CatalogResult

    data class MissingRequirement(
        val packId: String,
        val missing: String,
    ) : CatalogResult
}

/**
 * The merged content of the enabled [packs].
 *
 * The typed `*Definitions` accessors are the definitions the domain reads; the
 * `innovations`/`locations`/`resources`/`fightingArts`/`disorders`/`quarries`/`nemeses`
 * name lists are **derived** for display and are the deduped, sorted union across
 * packs, so composition is order-independent and deterministic.
 */
data class ContentCatalog(
    val packs: List<ContentPack>,
) {
    val innovationDefinitions: List<Innovation> =
        mergeDefinition(packs.flatMap { it.bundle.innovations }) { it.name }
    val locationDefinitions: List<SettlementLocation> =
        mergeLocationDefinitions(packs.flatMap { it.bundle.locations })
    val resourceDefinitions: List<ResourceDefinition> =
        mergeDefinition(packs.flatMap { it.bundle.resources }) { it.name }
    val fightingArtDefinitions: List<ContentEntry> =
        mergeDefinition(packs.flatMap { it.bundle.fightingArts }) { it.name }
    val disorderDefinitions: List<ContentEntry> =
        mergeDefinition(packs.flatMap { it.bundle.disorders }) { it.name }
    val settlementEventDefinitions: List<SettlementEvent> =
        mergeDefinition(packs.flatMap { it.bundle.settlementEvents }) { it.name }
    val gearDefinitions: List<Gear> =
        mergeDefinition(packs.flatMap { it.bundle.gear }) { it.name }
    val timeline: List<TimelineEntry> =
        packs
            .flatMap { it.bundle.timeline }
            .distinct()
            .sortedWith(compareBy({ it.lanternYear }, { it.label }))

    val innovations: List<String> = innovationDefinitions.map { it.name }.sorted()
    val locations: List<String> = locationDefinitions.map { it.name }.sorted()
    val resources: List<String> = resourceDefinitions.map { it.name }.sorted()
    val fightingArts: List<String> = fightingArtDefinitions.map { it.name }.sorted()
    val disorders: List<String> = disorderDefinitions.map { it.name }.sorted()
    val settlementEvents: List<String> = settlementEventDefinitions.map { it.name }.sorted()
    val quarries: List<String> = packs.flatMap { it.bundle.quarries }.distinct().sorted()
    val nemeses: List<String> = packs.flatMap { it.bundle.nemeses }.distinct().sorted()

    private fun <T> mergeDefinition(
        definitions: List<T>,
        name: (T) -> String,
    ): List<T> = definitions.sortedBy(name).distinctBy(name)

    /** The modifiers of every gear named in [gearNames]; unknown names are ignored. */
    fun gearModifiersFor(gearNames: Collection<String>): List<Modifier> {
        val byName = gearDefinitions.associateBy { it.name }
        return gearNames.flatMap { byName[it]?.modifiers ?: emptyList() }
    }

    /**
     * The resource definitions grouped by their [ResourceDefinition.source] (falling back to
     * [ResourceSources.sourceOf] when a definition carries no source), sorted by source then name.
     * Every resource appears in exactly one group — nothing is dropped.
     */
    fun resourceGroups(): Map<String, List<ResourceDefinition>> =
        resourceDefinitions
            .groupBy { it.source.ifBlank { ResourceSources.sourceOf(it.name) } }
            .toList()
            .sortedBy { (source, _) -> source }
            .associate { (source, definitions) -> source to definitions.sortedBy { it.name } }
}

/**
 * Merges [locations] by name, combining the fields of same-name definitions so separate
 * packs can contribute different aspects (one the build data, another the recipes). The
 * result is deterministic: fields keep the first non-empty value in pack order, and lists
 * are de-duplicated.
 */
private fun mergeLocationDefinitions(locations: List<SettlementLocation>): List<SettlementLocation> =
    locations
        .groupBy { it.name }
        .map { (_, sameName) -> sameName.reduce(::combineLocations) }
        .sortedBy { it.name }

private fun combineLocations(
    current: SettlementLocation,
    candidate: SettlementLocation,
): SettlementLocation =
    SettlementLocation(
        name = current.name,
        requires = (current.requires + candidate.requires).distinct(),
        buildEndeavors = if (current.buildEndeavors != 0) current.buildEndeavors else candidate.buildEndeavors,
        buildCost = if (current.buildCost.isNotEmpty()) current.buildCost else candidate.buildCost,
        actions = (current.actions + candidate.actions).distinctBy { it.name },
        recipes = (current.recipes + candidate.recipes).distinctBy { it.gearName },
    )

object ContentCatalogComposer {
    fun compose(packs: List<ContentPack>): CatalogResult {
        val available = packs.mapTo(mutableSetOf()) { it.id }
        for (pack in packs) {
            val missing = pack.requires.firstOrNull { it !in available } ?: continue
            return CatalogResult.MissingRequirement(packId = pack.id, missing = missing)
        }
        return CatalogResult.Ok(ContentCatalog(packs))
    }
}
