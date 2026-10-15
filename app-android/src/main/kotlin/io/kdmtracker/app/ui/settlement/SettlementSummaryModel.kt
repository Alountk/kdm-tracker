package io.kdmtracker.app.ui.settlement

import io.kdmtracker.core.domain.ResourceDefinition
import io.kdmtracker.core.domain.ResourceId
import io.kdmtracker.core.domain.StorageLine

/** A basic resource's colour role, and the Material Symbol the design draws with it. */
enum class ResourceColour(
    val icon: String,
) {
    BONE("format_h4"),
    CORAL("vital_signs"),
    GOLD("layers"),
    DIM("hardware"),
}

/** One cell of the summary's resource grid. */
data class SummaryResource(
    val id: ResourceId,
    val name: String,
    val label: String,
    val quantity: Int,
    val colour: ResourceColour,
)

/**
 * The settlement summary's display values.
 *
 * Pure: the caller supplies the scalars, so tests build one directly. The resource grid is keyed by
 * the catalog's Spanish display name and takes the design's short label and colour; a resource the design
 * does not name keeps its catalog name and the dim role rather than disappearing.
 */
data class SettlementSummaryModel(
    val survivalLimit: Int,
    val endeavors: Int,
    val maxEndeavors: Int,
    val resources: List<SummaryResource>,
) {
    companion object {
        /** The four resources the design's grid shows, in its order. */
        val DESIGN_ORDER: List<String> =
            listOf("Hueso de Monstruo", "Órgano de Monstruo", "Piel de Monstruo", "Scrap")

        /** The design's short label and colour per catalog resource name. */
        private val DESIGN =
            mapOf(
                "Hueso de Monstruo" to (("Hueso") to ResourceColour.BONE),
                "Órgano de Monstruo" to (("Órgano") to ResourceColour.CORAL),
                "Piel de Monstruo" to (("Pellejo") to ResourceColour.GOLD),
                "Scrap" to (("Chatarra") to ResourceColour.DIM),
            )

        fun of(
            survivalLimit: Int,
            endeavors: Int,
            maxEndeavors: Int,
            resources: List<SummaryResource>,
        ): SettlementSummaryModel =
            SettlementSummaryModel(
                survivalLimit = survivalLimit,
                endeavors = endeavors,
                maxEndeavors = maxEndeavors,
                resources = resources,
            )

        /** Builds the grid from the catalog's basic resources and the settlement's storage. */
        fun basicResources(
            definitions: List<ResourceDefinition>,
            storage: List<StorageLine>,
        ): List<SummaryResource> =
            definitions.map { definition ->
                val design = DESIGN[definition.names.es]
                SummaryResource(
                    id = definition.id,
                    name = definition.names.es,
                    label = design?.first ?: definition.names.es,
                    quantity = storage.firstOrNull { it.id == definition.id }?.quantity ?: 0,
                    colour = design?.second ?: ResourceColour.DIM,
                )
            }
    }
}
