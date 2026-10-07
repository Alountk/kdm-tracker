package io.kdmtracker.core.content

import io.kdmtracker.core.domain.ContentEntry
import io.kdmtracker.core.domain.Innovation
import io.kdmtracker.core.domain.ResourceDefinition
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
    val quarries: List<String> = packs.flatMap { it.bundle.quarries }.distinct().sorted()
    val nemeses: List<String> = packs.flatMap { it.bundle.nemeses }.distinct().sorted()

    private fun <T> mergeDefinition(
        definitions: List<T>,
        name: (T) -> String,
    ): List<T> = definitions.sortedBy(name).distinctBy(name)
}

/**
 * Merges [locations] by name, preferring the definition that carries modeled data
 * (a non-empty [SettlementLocation.buildCost] or [SettlementLocation.actions]) over
 * a bare seed. Ties — both modeled or both bare — keep the first occurrence in pack
 * order, so the merge is deterministic and order-independent.
 */
private fun mergeLocationDefinitions(locations: List<SettlementLocation>): List<SettlementLocation> =
    locations
        .groupBy { it.name }
        .map { (_, sameName) -> sameName.reduce(::preferModeledLocation) }
        .sortedBy { it.name }

private fun preferModeledLocation(
    current: SettlementLocation,
    candidate: SettlementLocation,
): SettlementLocation = if (current.hasModeledData() || !candidate.hasModeledData()) current else candidate

private fun SettlementLocation.hasModeledData(): Boolean = buildCost.isNotEmpty() || actions.isNotEmpty()

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
