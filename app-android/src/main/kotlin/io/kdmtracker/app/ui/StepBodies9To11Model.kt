package io.kdmtracker.app.ui

import io.kdmtracker.core.domain.ResourceDefinition
import io.kdmtracker.core.domain.ResourceId
import io.kdmtracker.core.domain.ResourceMaterial
import io.kdmtracker.core.domain.StorageLine

/**
 * The design's year-1 exemption card renders only during the first settlement phase,
 * `lanternYear <= 2` — the design gives no later-year body, so past the first phase the step shows
 * its meta row and nothing else. `AdvanceTimeline` increments the year when leaving
 * step 4 and `LanternYearFlow.canEnterStep` forbids forward skips, so at step 9 the first
 * settlement phase always runs under lantern year 2 — the original `== 1` gate could never open.
 */
fun showYearOneExemption(lanternYear: Int): Boolean = lanternYear <= 2

/** The icon-box tone the design gives one resource cell of the step-10 grid. */
enum class ResourceGridTone {
    PRIMARY,
    SECONDARY,
    DIM,
}

/** One cell of step 10's 2-column basic-resource grid. */
data class ResourceGridRow(
    val id: ResourceId,
    val label: String,
    val subLabel: String,
    val glyph: String,
    val tone: ResourceGridTone,
    val count: Int,
) {
    /**
     * The minus button's enabled state — inert at zero, the same semantics as the summary's adjust
     * stepper (`ResourceAdjustFlow`'s `atMinimum`); [io.kdmtracker.core.usecase.AddResourceEntry]
     * refuses the overdraw upstream anyway.
     */
    val canDecrement: Boolean get() = count > 0
}

/** One distinct stored-gear chip: the item and how many the settlement holds. */
data class GearChip(
    val name: String,
    val count: Int,
)

/** The design's row per material; the cell's label is the material's own name. */
private data class GridDesign(
    val subLabel: String,
    val glyph: String,
    val tone: ResourceGridTone,
)

private val GRID_DESIGN =
    mapOf(
        ResourceMaterial.BONE to GridDesign("Básico", "radiology", ResourceGridTone.PRIMARY),
        ResourceMaterial.ORGAN to GridDesign("Básico", "vital_signs", ResourceGridTone.SECONDARY),
        ResourceMaterial.HIDE to GridDesign("Básico", "texture", ResourceGridTone.PRIMARY),
        ResourceMaterial.SCRAP to GridDesign("Raro", "hardware", ResourceGridTone.DIM),
    )

/**
 * The step-10 grid's rows: one per [ResourceMaterial] in the design's order, each adjusting the
 * material's [ResourceMaterial.basicId] resource with the design's sub-label and glyph. The count
 * is that resource's own quantity in the settlement's storage, the same as the summary's cell. A
 * material whose basic resource the catalog lacks drops out rather than render a cell with no id to
 * adjust; other stored resources stay unrendered (accepted consequence, backlog).
 */
fun resourceGridRows(
    definitions: List<ResourceDefinition>,
    storage: List<StorageLine>,
): List<ResourceGridRow> =
    ResourceMaterial.entries.mapNotNull { material ->
        val design = GRID_DESIGN.getValue(material)
        val definition = definitions.firstOrNull { it.id == material.basicId } ?: return@mapNotNull null
        ResourceGridRow(
            id = definition.id,
            label = material.names.es.uppercase(),
            subLabel = design.subLabel,
            glyph = design.glyph,
            tone = design.tone,
            count = storage.firstOrNull { it.id == definition.id }?.quantity ?: 0,
        )
    }

/**
 * One chip per distinct item in the settlement's stored gear, with its count, in first
 * appearance order (the design's per-item icons have no mapping and are omitted).
 */
fun gearChips(gear: List<String>): List<GearChip> =
    gear.groupingBy { it }.eachCount().map { (name, count) -> GearChip(name = name, count = count) }
