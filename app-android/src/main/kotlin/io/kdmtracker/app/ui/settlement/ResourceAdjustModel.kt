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
