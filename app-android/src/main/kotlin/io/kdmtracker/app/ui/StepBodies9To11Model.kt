package io.kdmtracker.app.ui

import io.kdmtracker.app.ui.settlement.SettlementSummaryModel
import io.kdmtracker.core.domain.ResourceDefinition
import io.kdmtracker.core.domain.ResourceId
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

/** The design's row per basic resource, keyed by the catalog's Spanish display name. */
private data class GridDesign(
    val label: String,
    val subLabel: String,
    val glyph: String,
    val tone: ResourceGridTone,
)

private val GRID_DESIGN =
    mapOf(
        "Hueso de Monstruo" to GridDesign("HUESO", "Básico", "radiology", ResourceGridTone.PRIMARY),
        "Órgano de Monstruo" to GridDesign("ÓRGANO", "Básico", "vital_signs", ResourceGridTone.SECONDARY),
        "Piel de Monstruo" to GridDesign("PELLEJO", "Básico", "texture", ResourceGridTone.PRIMARY),
        "Scrap" to GridDesign("CHATARRA", "Raro", "hardware", ResourceGridTone.DIM),
    )

/**
 * The step-10 grid's rows — the four basic resources, in the design's order, with its short
 * labels and sub-labels.
 *
 * Id resolution mirrors [SettlementSummaryModel.basicResources]: rows are keyed by the catalog's
 * Spanish display name over [SettlementSummaryModel.DESIGN_ORDER] (the same four the summary
 * shows) and the count comes from the settlement's storage. A name the catalog no longer carries
 * drops out rather than rendering a cell with no id to adjust; storage lines the design does not
 * name stay unrendered (accepted consequence, backlog).
 */
fun resourceGridRows(
    definitions: List<ResourceDefinition>,
    storage: List<StorageLine>,
): List<ResourceGridRow> =
    SettlementSummaryModel.DESIGN_ORDER.mapNotNull { name ->
        val design = GRID_DESIGN.getValue(name)
        val definition = definitions.firstOrNull { it.names.es == name } ?: return@mapNotNull null
        ResourceGridRow(
            id = definition.id,
            label = design.label,
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
