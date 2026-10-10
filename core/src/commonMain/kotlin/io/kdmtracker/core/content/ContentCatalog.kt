package io.kdmtracker.core.content

import io.kdmtracker.core.domain.ContentEntry
import io.kdmtracker.core.domain.Gear
import io.kdmtracker.core.domain.GearId
import io.kdmtracker.core.domain.HuntEvent
import io.kdmtracker.core.domain.HuntEventKind
import io.kdmtracker.core.domain.Innovation
import io.kdmtracker.core.domain.LocalizedText
import io.kdmtracker.core.domain.Modifier
import io.kdmtracker.core.domain.MonsterCard
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
        mergeInnovationDefinitions(packs.flatMap { it.bundle.innovations })
    val locationDefinitions: List<SettlementLocation> =
        mergeLocationDefinitions(packs.flatMap { it.bundle.locations })
    val resourceDefinitions: List<ResourceDefinition> =
        mergeDefinition(packs.flatMap { it.bundle.resources }) { it.id.value }
    val fightingArtDefinitions: List<ContentEntry> =
        mergeDefinition(packs.flatMap { it.bundle.fightingArts }) { it.name }
    val disorderDefinitions: List<ContentEntry> =
        mergeDefinition(packs.flatMap { it.bundle.disorders }) { it.name }
    val settlementEventDefinitions: List<SettlementEvent> =
        mergeDefinition(packs.flatMap { it.bundle.settlementEvents }) { it.name }
    val huntEventDefinitions: List<HuntEvent> =
        mergeDefinition(packs.flatMap { it.bundle.huntEvents }) { it.name }
    val monsterCardDefinitions: List<MonsterCard> =
        mergeDefinition(packs.flatMap { it.bundle.monsterCards }) { "${it.monster}|${it.kind}|${it.name}" }
    val gearDefinitions: List<Gear> =
        mergeDefinition(packs.flatMap { it.bundle.gear }) { it.id.value }
    val timeline: List<TimelineEntry> =
        packs
            .flatMap { it.bundle.timeline }
            .distinct()
            .sortedWith(compareBy({ it.lanternYear }, { it.label }))

    val innovations: List<String> = innovationDefinitions.map { it.names.es }.sorted()
    val locations: List<String> = locationDefinitions.map { it.names.es }.sorted()
    val resources: List<String> = resourceDefinitions.map { it.names.es }.sorted()
    val fightingArts: List<String> = fightingArtDefinitions.map { it.name }.sorted()
    val disorders: List<String> = disorderDefinitions.map { it.name }.sorted()
    val settlementEvents: List<String> = settlementEventDefinitions.map { it.name }.sorted()
    val quarries: List<String> = packs.flatMap { it.bundle.quarries }.distinct().sorted()
    val nemeses: List<String> = packs.flatMap { it.bundle.nemeses }.distinct().sorted()

    private fun <T> mergeDefinition(
        definitions: List<T>,
        name: (T) -> String,
    ): List<T> = definitions.sortedBy(name).distinctBy(name)

    /**
     * The definition whose Spanish **or** English name is [name], or `null`.
     *
     * A transitional lookup, and deliberately language-tolerant: gear recipes, location build costs
     * and settlement-action costs still name their resources, and the 1.6 seed names the basic four
     * in English (`"Bone"`) while the 1.5 pack names them in Spanish (`"Hueso de Monstruo"`). Once
     * those costs are re-keyed to [ResourceId] this goes away and nothing resolves names again.
     */
    fun resourceDefinitionFor(name: String): ResourceDefinition? =
        resourceDefinitions.firstOrNull { it.names.es == name || it.names.en == name }

    /**
     * The modifiers of every gear named in [gearNames]; unknown names are ignored. A name
     * resolves in either language — the crafted-gear lists still hold display names until the
     * state re-keys them to [GearId].
     */
    fun gearModifiersFor(gearNames: Collection<String>): List<Modifier> =
        gearNames.flatMap { name ->
            gearDefinitions.firstOrNull { it.names.es == name || it.names.en == name }?.modifiers ?: emptyList()
        }

    /**
     * The resource definitions grouped by their [ResourceDefinition.source] (falling back to
     * [ResourceSources.sourceOf] when a definition carries no source), sorted by source then name.
     * Every resource appears in exactly one group — nothing is dropped.
     */
    fun resourceGroups(): Map<String, List<ResourceDefinition>> =
        resourceDefinitions
            .groupBy { it.source.ifBlank { ResourceSources.sourceOf(it.names.es) } }
            .toList()
            .sortedBy { (source, _) -> source }
            .associate { (source, definitions) -> source to definitions.sortedBy { it.names.es } }

    /**
     * The hunt events [quarry] draws on a hunt: every basic and special event plus the monster
     * events whose [HuntEvent.monster] is [quarry].
     */
    fun huntEventsFor(quarry: String): List<HuntEvent> =
        huntEventDefinitions.filter { it.kind != HuntEventKind.MONSTER || it.monster == quarry }

    /** The showdown cards (AI, Basic Action, hit locations) for [monster]. */
    fun monsterCardsFor(monster: String): List<MonsterCard> = monsterCardDefinitions.filter { it.monster == monster }
}

/**
 * Merges [locations] by id, combining the fields of same-id definitions so separate
 * packs can contribute different aspects (one the build data, another the recipes). Two
 * packs naming the same location in different languages — `Lantern Hoard` and
 * `Cúmulo de Linternas` — therefore collapse into one definition. The result is
 * deterministic: fields keep the first non-empty value in pack order, names fall back to
 * the other pack's translation, and lists are de-duplicated.
 */
private fun mergeLocationDefinitions(locations: List<SettlementLocation>): List<SettlementLocation> =
    locations
        .groupBy { it.id }
        .map { (_, sameId) -> sameId.reduce(::combineLocations) }
        .sortedBy { it.names.es }

private fun combineLocations(
    current: SettlementLocation,
    candidate: SettlementLocation,
): SettlementLocation =
    SettlementLocation(
        id = current.id,
        names = LocalizedText(es = current.names.es, en = current.names.en ?: candidate.names.en),
        requires = (current.requires + candidate.requires).distinct(),
        buildEndeavors = if (current.buildEndeavors != 0) current.buildEndeavors else candidate.buildEndeavors,
        buildCost = if (current.buildCost.isNotEmpty()) current.buildCost else candidate.buildCost,
        actions = (current.actions + candidate.actions).distinctBy { it.kind },
        recipes = (current.recipes + candidate.recipes).distinctBy { it.gearId },
    )

/**
 * Merges [innovations] by id, combining the fields of same-id definitions so separate
 * packs can contribute different aspects — the unlock **tree** is the union of both packs'
 * consequences, so `Language` (1.6 seed) and `Lenguaje` (1.5) unlock the same branches.
 * The result is deterministic: fields keep the first non-empty value in pack order, names
 * fall back to the other pack's translation, and consequence lists are de-duplicated.
 */
private fun mergeInnovationDefinitions(innovations: List<Innovation>): List<Innovation> =
    innovations
        .groupBy { it.id }
        .map { (_, sameId) -> sameId.reduce(::combineInnovations) }
        .sortedBy { it.names.es }

private fun combineInnovations(
    current: Innovation,
    candidate: Innovation,
): Innovation =
    Innovation(
        id = current.id,
        names = LocalizedText(es = current.names.es, en = current.names.en ?: candidate.names.en),
        description = current.description.ifBlank { candidate.description },
        consequences = (current.consequences + candidate.consequences).distinct(),
        survivalLimitBonus =
            if (current.survivalLimitBonus != 0) current.survivalLimitBonus else candidate.survivalLimitBonus,
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
