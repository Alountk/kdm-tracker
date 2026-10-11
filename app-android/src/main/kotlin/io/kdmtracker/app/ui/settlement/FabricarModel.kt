package io.kdmtracker.app.ui.settlement

import io.kdmtracker.app.UiState
import io.kdmtracker.core.domain.InnovationId
import io.kdmtracker.core.domain.LocationId
import io.kdmtracker.core.domain.ResourceId
import io.kdmtracker.core.domain.SettlementLocation

/** The location a settlement starts with; nothing builds it. */
private const val FOUNDING_LOCATION = "lantern_hoard"

/**
 * The card's category line, derived from the location's prerequisites rather than authored:
 * the founding location, the ones that need nothing beyond it, and the ones built on another.
 */
private fun categoryOf(
    id: LocationId,
    requires: List<LocationId>,
): String =
    when {
        id.value == FOUNDING_LOCATION -> "FUNDACIONAL"
        requires.isEmpty() || requires.all { it.value == FOUNDING_LOCATION } -> "ARTESANÍA PRIMARIA"
        else -> "DERIVADA"
    }

/** How a recipe row renders its button. */
enum class RecipeStatus { CRAFTABLE, OUT_OF_STOCK, MISSING_INNOVATION }

/** One craftable gear row inside a built location's catalog. */
data class RecipeRow(
    val gearName: String,
    val costChips: List<String>,
    val status: RecipeStatus,
    val missingInnovation: String?,
    val requiresText: String?,
)

/** An inline "Construir X (MEJORA)" offer inside the location that unlocks it. */
data class UpgradeRow(
    val locationName: String,
    val costChips: List<String>,
    val endeavors: Int,
)

/** A location that is not built and has no ready inline upgrade. */
data class BuildableLocationCard(
    val name: String,
    /** The design's one-line description; null when it draws none for this location. */
    val description: String?,
    val requires: List<String>,
    val endeavors: Int,
    val costChips: List<String>,
)

/** One built location: its action, its crafting catalog and its ready build upgrades. */
data class BuiltLocationCard(
    val name: String,
    /** The category line above the title — `FUNDACIONAL`, `ARTESANÍA PRIMARIA`, `DERIVADA`. */
    val category: String,
    /** The design's one-line description under the title; null when it draws none. */
    val description: String?,
    val actionLabel: String?,
    /** The action's endeavor cost, shown as "Costo: N Esfuerzo"; null when the card has no action. */
    val actionEndeavorCost: Int?,
    val catalogSize: Int,
    val recipes: List<RecipeRow>,
    val upgrades: List<UpgradeRow>,
)

/**
 * The Fabricar section's view values, derived purely from [UiState].
 *
 * Every decision the design pins lives here — statuses, chips, upgrade placement and the
 * header partition — so the composables only render and the rules stay unit-testable on the JVM.
 */
data class FabricarModel(
    val builtCount: Int,
    val pendingCount: Int,
    val built: List<BuiltLocationCard>,
    val buildable: List<BuildableLocationCard>,
) {
    companion object {
        fun of(state: UiState): FabricarModel {
            val catalog = state.catalogLocations
            val builtNames = state.builtLocations.toSet()
            val unlockedIds =
                state.record
                    ?.state
                    ?.innovations
                    ?.innovations
                    ?.mapTo(mutableSetOf()) { it.id } ?: mutableSetOf()
            val builtLocations = catalog.filter { it.names.es in builtNames }
            val builtIds = builtLocations.mapTo(mutableSetOf()) { it.id }

            fun innovationName(id: InnovationId): String =
                state.innovationDefinitions
                    .firstOrNull { it.id == id }
                    ?.names
                    ?.es ?: id.value

            fun locationName(id: LocationId): String = catalog.firstOrNull { it.id == id }?.names?.es ?: id.value

            fun chips(cost: Map<ResourceId, Int>): List<String> =
                cost.entries
                    .sortedBy { it.key.value }
                    .map { (id, count) ->
                        val name =
                            state.resourceDefinitions
                                .firstOrNull { it.id == id }
                                ?.names
                                ?.es ?: id.value
                        "$count $name"
                    }

            // A ready upgrade lives inside its first requiring built location (by id), and only there.
            val upgradesByHost = mutableMapOf<LocationId, MutableList<SettlementLocation>>()
            val claimed = mutableSetOf<LocationId>()
            catalog
                .filter { it.names.es !in builtNames }
                .sortedBy { it.id.value }
                .forEach { candidate ->
                    if (candidate.requires.isEmpty() || !candidate.requires.all { it in builtIds }) return@forEach
                    val host =
                        builtLocations.filter { it.id in candidate.requires }.minByOrNull { it.id.value }
                            ?: return@forEach
                    upgradesByHost.getOrPut(host.id) { mutableListOf() }.add(candidate)
                    claimed += candidate.id
                }

            val built =
                builtLocations.map { location ->
                    val action = location.actions.firstOrNull()
                    val rows =
                        state.craftRecipes
                            .filter { it.locationName == location.names.es }
                            .map { view ->
                                val missing = view.recipe.requires.firstOrNull { it !in unlockedIds }
                                RecipeRow(
                                    gearName = view.gearName.ifBlank { view.recipe.gearId.value },
                                    costChips = chips(view.recipe.resourceCost),
                                    status =
                                        when {
                                            view.check.craftable -> RecipeStatus.CRAFTABLE
                                            missing != null -> RecipeStatus.MISSING_INNOVATION
                                            else -> RecipeStatus.OUT_OF_STOCK
                                        },
                                    missingInnovation =
                                        if (view.check.craftable || missing == null) null else innovationName(missing),
                                    requiresText = view.recipe.requiresText,
                                )
                            }
                    BuiltLocationCard(
                        name = location.names.es,
                        category = categoryOf(location.id, location.requires),
                        description = location.description?.es,
                        actionLabel = action?.names?.es,
                        actionEndeavorCost = action?.endeavorCost,
                        catalogSize = rows.size,
                        recipes = rows,
                        upgrades =
                            upgradesByHost[location.id]
                                .orEmpty()
                                .sortedBy { it.id.value }
                                .map { candidate ->
                                    UpgradeRow(
                                        locationName = candidate.names.es,
                                        costChips = chips(candidate.buildCost),
                                        endeavors = candidate.buildEndeavors,
                                    )
                                },
                    )
                }

            val buildable =
                catalog
                    .filter { it.names.es !in builtNames && it.id !in claimed }
                    .map { location ->
                        BuildableLocationCard(
                            name = location.names.es,
                            description = location.description?.es,
                            requires = location.requires.map { locationName(it) },
                            endeavors = location.buildEndeavors,
                            costChips = chips(location.buildCost),
                        )
                    }

            return FabricarModel(
                builtCount = builtLocations.size,
                pendingCount = catalog.size - builtLocations.size,
                built = built,
                buildable = buildable,
            )
        }
    }
}
