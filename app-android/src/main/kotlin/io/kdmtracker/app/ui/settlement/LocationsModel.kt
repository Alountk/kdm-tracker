package io.kdmtracker.app.ui.settlement

import io.kdmtracker.core.domain.SettlementLocation

/**
 * One location as its panel shows it. The design's prose description is not modelled, so the entry
 * carries the location's real prerequisites instead of invented copy.
 */
data class LocationEntry(
    val name: String,
    val description: String,
    val endeavors: Int,
    val actionLabel: String?,
)

/** The Settlement Locations panel: what is built, and what can still be erected. */
data class LocationsModel(
    val built: List<LocationEntry>,
    val buildable: List<LocationEntry>,
) {
    companion object {
        fun of(
            built: List<String>,
            catalog: List<SettlementLocation>,
        ): LocationsModel =
            LocationsModel(
                built = catalog.filter { it.name in built }.map { entry(it) },
                buildable = catalog.filter { it.name !in built }.map { entry(it) },
            )

        private fun entry(location: SettlementLocation): LocationEntry =
            LocationEntry(
                name = location.name,
                description = location.requires.joinToString(", "),
                endeavors = location.buildEndeavors,
                actionLabel = location.actions.firstOrNull()?.name,
            )
    }
}
