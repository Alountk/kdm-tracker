package io.kdmtracker.core.domain

/**
 * A settlement location the settlement can build.
 *
 * @property name the location's unique, human-readable name.
 * @property requires the names of the locations that must already be built.
 */
data class SettlementLocation(
    val name: String,
    val requires: List<String> = emptyList(),
)

/**
 * The settlement locations that have been built, keyed by [SettlementLocation.name].
 *
 * The set holds content only; the location definitions (with their [SettlementLocation.requires])
 * are passed in by the caller, so this type stays catalog-independent.
 */
data class LocationSet(
    val built: List<String> = emptyList(),
) {
    /**
     * Whether [location] can be built: every one of its
     * [SettlementLocation.requires] is already in [built].
     */
    fun canBuild(location: SettlementLocation): Boolean = location.requires.all { it in built }

    /**
     * Returns a set that also holds [location], but only when [canBuild] allows
     * it and its name is not already present. Otherwise returns this set
     * unchanged, making repeated builds idempotent.
     */
    fun with(location: SettlementLocation): LocationSet =
        if (location.name in built || !canBuild(location)) this else copy(built = built + location.name)
}
