package io.kdmtracker.app.ui.settlement

import io.kdmtracker.core.domain.ResourceDefinition
import io.kdmtracker.core.domain.ResourceId
import io.kdmtracker.core.domain.ResourceMaterial
import io.kdmtracker.core.domain.StorageLine

/** A basic resource's colour role, and the Material Symbol the design draws with it. */
enum class ResourceColour(
    val icon: String,
) {
    BONE("nutrition"),
    CORAL("psychology"),
    GOLD("layers"),
    DIM("hardware"),
    ;

    companion object {
        /** The role the design gives each material's cell. */
        fun of(material: ResourceMaterial): ResourceColour =
            when (material) {
                ResourceMaterial.BONE -> BONE
                ResourceMaterial.ORGAN -> CORAL
                ResourceMaterial.HIDE -> GOLD
                ResourceMaterial.SCRAP -> DIM
            }
    }
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
 * Pure: the caller supplies the scalars, so tests build one directly. The resource grid is one cell
 * per [ResourceMaterial], in the design's order; each cell adjusts the material's
 * [ResourceMaterial.basicId] resource and takes the material's own label and colour.
 */
data class SettlementSummaryModel(
    val survivalLimit: Int,
    val endeavors: Int,
    val maxEndeavors: Int,
    val resources: List<SummaryResource>,
) {
    companion object {
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

        /**
         * Builds the grid: a cell per material whose basic resource the catalog carries, counting
         * that resource alone. A material with no basic resource in the catalog has no id to adjust,
         * so its cell drops out rather than render dead.
         */
        fun basicResources(
            definitions: List<ResourceDefinition>,
            storage: List<StorageLine>,
        ): List<SummaryResource> =
            ResourceMaterial.entries.mapNotNull { material ->
                val definition = definitions.firstOrNull { it.id == material.basicId } ?: return@mapNotNull null
                SummaryResource(
                    id = definition.id,
                    name = definition.names.es,
                    label = material.names.es,
                    quantity = storage.firstOrNull { it.id == definition.id }?.quantity ?: 0,
                    colour = ResourceColour.of(material),
                )
            }
    }
}
