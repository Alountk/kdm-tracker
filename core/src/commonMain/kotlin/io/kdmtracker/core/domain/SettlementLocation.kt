package io.kdmtracker.core.domain

import kotlinx.serialization.Serializable

/**
 * A settlement location the settlement can build.
 *
 * The location carries everything the rules layer can enforce when building it —
 * its prerequisites, its build cost, and the actions it offers once built.
 *
 * @property id the location's stable, language-neutral identity.
 * @property names the location's localized display names.
 * @property requires the ids of the locations that must already be built.
 * @property buildEndeavors the number of endeavors required to build the location.
 * @property buildCost the resources required to build the location, as id → count.
 * @property actions the actions the location offers once built.
 * @property recipes the gear this location can craft once built.
 */
@Serializable
data class SettlementLocation(
    val id: LocationId,
    val names: LocalizedText,
    val requires: List<LocationId> = emptyList(),
    val buildEndeavors: Int = 0,
    val buildCost: Map<ResourceId, Int> = emptyMap(),
    val actions: List<SettlementAction> = emptyList(),
    val recipes: List<GearRecipe> = emptyList(),
)

/**
 * The settlement locations that have been built, keyed by [SettlementLocation.id].
 *
 * The set holds content only; the location definitions (with their [SettlementLocation.requires])
 * are passed in by the caller, so this type stays catalog-independent.
 */
@Serializable
data class LocationSet(
    val built: List<LocationId> = emptyList(),
) {
    /**
     * Whether [location] can be built: every one of its
     * [SettlementLocation.requires] is already in [built].
     */
    fun canBuild(location: SettlementLocation): Boolean = location.requires.all { it in built }

    /**
     * Returns a set that also holds [location], but only when [canBuild] allows
     * it and its id is not already present. Otherwise returns this set
     * unchanged, making repeated builds idempotent.
     */
    fun with(location: SettlementLocation): LocationSet =
        if (location.id in built || !canBuild(location)) this else copy(built = built + location.id)
}
