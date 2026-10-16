package io.kdmtracker.app.ui.settlement

import io.kdmtracker.core.domain.ResourceId

/**
 * The inline edit the design opens on a resource cell — no modal, no bottom sheet.
 *
 * @property original the count the cell held when editing began, so `delta` is always the
 *   correction to apply rather than the new absolute value.
 * @property pending the count while the stepper is being used; it is never allowed below zero,
 *   because the ledger refuses to hold a negative quantity.
 */
data class ResourceEdit(
    val id: ResourceId,
    val label: String,
    val original: Int,
    val pending: Int,
) {
    /** The design's `ESTADO 3`: the minus button is inert at zero. */
    val atMinimum: Boolean get() = pending <= 0

    val delta: Int get() = pending - original

    fun bump(by: Int): ResourceEdit = copy(pending = (pending + by).coerceAtLeast(0))
}

/**
 * What the design shows once an adjustment has been saved: the green strip naming the change,
 * `Hueso: 12 → 14`.
 */
data class AdjustResult(
    val label: String,
    val from: Int,
    val to: Int,
) {
    val summary: String get() = "$label: $from → $to"
}

/** A cell that has just been saved: its identity plus the strip the design shows under the grid. */
data class AdjustedCell(
    val id: ResourceId,
    val result: AdjustResult,
)

/**
 * The summary's adjust flow: which cell is open, what it holds, what was just saved.
 *
 * Pure state behind the design's four states — rest, editing, minimum and confirmed. The
 * composable renders it and the controller stays out: [save] only *describes* the change, the
 * caller applies it through `onAdjust` when [delta] is non-zero, so a save with nothing to save
 * never reaches the ledger and never shows a success strip.
 *
 * Matching cells goes by [ResourceId], never by display label, so a confirmed cell re-greens
 * after every model rebuild even when two resources share a label.
 */
data class ResourceAdjustFlow(
    val edit: ResourceEdit? = null,
    val confirmed: AdjustedCell? = null,
) {
    /** The design's cell states, one per row position of the flow. */
    enum class CellState { REST, EDITING, MINIMUM, CONFIRMED }

    /** The correction the caller must apply, `0` when no cell is open or nothing changed. */
    val delta: Int get() = edit?.delta ?: 0

    /** A cell is open ⇒ the non-selected cells dim (the design's `opacity-60`). */
    val isEditing: Boolean get() = edit != null

    /** Nothing open and nothing confirmed ⇒ the hint row, and only then. */
    val isIdle: Boolean get() = edit == null && confirmed == null

    /** Opens [resource] for editing, discarding any previous confirmation. */
    fun select(resource: SummaryResource): ResourceAdjustFlow =
        ResourceAdjustFlow(edit = ResourceEdit(resource.id, resource.label, resource.quantity, resource.quantity))

    /** Moves the open cell's pending count, floored at zero by [ResourceEdit.bump]. */
    fun bump(by: Int): ResourceAdjustFlow {
        val open = edit ?: return this
        return copy(edit = open.bump(by))
    }

    /** Closes the editor; records [AdjustedCell] only when there is something to confirm. */
    fun save(): ResourceAdjustFlow {
        val open = edit ?: return this
        if (open.delta == 0) return copy(edit = null)
        return copy(
            edit = null,
            confirmed = AdjustedCell(open.id, AdjustResult(open.label, open.original, open.pending)),
        )
    }

    /** How [resource] renders right now, by id: confirmed beats open, open at zero is the minimum. */
    fun cellState(resource: SummaryResource): CellState =
        when {
            confirmed?.id == resource.id -> CellState.CONFIRMED
            edit?.id == resource.id && edit.atMinimum -> CellState.MINIMUM
            edit?.id == resource.id -> CellState.EDITING
            else -> CellState.REST
        }
}
