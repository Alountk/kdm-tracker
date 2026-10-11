package io.kdmtracker.app.ui.settlement

import io.kdmtracker.core.domain.SettlementLocation

/**
 * One location as its panel shows it. The design's prose description is not modelled, so the entry
 * carries the location's real prerequisites instead of invented copy.
 *
 * [buttonLabel] is the section's button text: a **buildable** location's button reads
 * `Construir`; a **built** one shows its own action (e.g. `Innovar`) and gets **no button at all**
 * when it offers none — never a bogus `Construir` on a site that is already built.
 */
data class LocationEntry(
    val name: String,
    val description: String,
    val endeavors: Int,
    val buttonLabel: String?,
)

/** The Settlement Locations panel: what is built, and what can still be erected. */
data class LocationsModel(
    val built: List<LocationEntry>,
    val buildable: List<LocationEntry>,
) {
    companion object {
        private const val BUILD_CTA = "Construir"

        fun of(
            built: List<String>,
            catalog: List<SettlementLocation>,
        ): LocationsModel =
            LocationsModel(
                built = catalog.filter { it.names.es in built }.map { entry(it, catalog, built = true) },
                buildable = catalog.filter { it.names.es !in built }.map { entry(it, catalog, built = false) },
            )

        private fun entry(
            location: SettlementLocation,
            catalog: List<SettlementLocation>,
            built: Boolean,
        ): LocationEntry =
            LocationEntry(
                name = location.names.es,
                description =
                    location.requires.joinToString(", ") { required ->
                        catalog.firstOrNull { it.id == required }?.names?.es ?: required.value
                    },
                endeavors = location.buildEndeavors,
                buttonLabel =
                    if (built) {
                        location.actions
                            .firstOrNull()
                            ?.names
                            ?.es
                    } else {
                        BUILD_CTA
                    },
            )
    }
}
