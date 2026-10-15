package io.kdmtracker.app

import io.kdmtracker.app.ui.settlement.AdjustResult
import io.kdmtracker.app.ui.settlement.ResourceAdjustFlow
import io.kdmtracker.app.ui.settlement.ResourceColour
import io.kdmtracker.app.ui.settlement.ResourceEdit
import io.kdmtracker.app.ui.settlement.SummaryResource
import io.kdmtracker.core.domain.ResourceId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The inline resource stepper's rules, pinned before the UI touches them: the count cannot fall
 * below zero, the delta is relative to where editing started, and a save names both counts.
 */
class ResourceAdjustModelTest {
    /** Starts a cell at [original] and moves it by [delta], the way the stepper does. */
    private fun edit(
        original: Int,
        delta: Int,
    ) = ResourceEdit(ResourceId("bone"), "Hueso", original, original).bump(delta)

    @Test
    fun bumpingBelowZeroStaysAtZero() {
        val at = edit(original = 0, delta = 0).bump(-1)

        assertEquals(0, at.pending, "the ledger never holds a negative quantity")
        assertTrue(at.atMinimum, "the minus button is inert at zero")
    }

    @Test
    fun bumpingFromPositiveKeepsTheFloor() {
        val edit = edit(original = 12, delta = -14)

        assertEquals(0, edit.pending)
    }

    @Test
    fun aCellAboveZeroIsNotAtTheMinimum() {
        assertFalse(edit(original = 12, delta = 1).atMinimum)
    }

    @Test
    fun theDeltaIsMeasuredFromWhereEditingBegan() {
        assertEquals(2, edit(original = 12, delta = 2).delta)
        assertEquals(-3, edit(original = 12, delta = -3).delta)
        assertEquals(0, edit(original = 12, delta = 0).delta)
    }

    @Test
    fun confirmingNamesTheLabelAndBothCounts() {
        assertEquals("Hueso: 12 → 14", AdjustResult("Hueso", from = 12, to = 14).summary)
    }

    /** A cell of the summary's grid, the way the flow opens it. */
    private fun cell(
        id: String = "bone",
        label: String = "Hueso",
        quantity: Int = 12,
    ) = SummaryResource(ResourceId(id), "$label de Monstruo", label, quantity, ResourceColour.BONE)

    @Test
    fun selectingOpensTheCellAndClearsAnyConfirmedResult() {
        val saved = ResourceAdjustFlow().select(cell(quantity = 12)).bump(2).save()
        val reopened = saved.select(cell(quantity = 14))

        assertNull(reopened.confirmed, "a new edit hides the previous success strip")
        assertEquals(ResourceId("bone"), reopened.edit?.id)
        assertEquals(14, reopened.edit?.original, "the delta starts from the fresh quantity")
        assertEquals(14, reopened.edit?.pending)
    }

    @Test
    fun theFlowFloorsAtZeroAndReportsTheMinimum() {
        val flow = ResourceAdjustFlow().select(cell(quantity = 3)).bump(-9)

        assertEquals(0, flow.edit?.pending)
        assertEquals(ResourceAdjustFlow.CellState.MINIMUM, flow.cellState(cell(quantity = 3)))
    }

    @Test
    fun cellStateMatchesByResourceIdAfterTheModelRebuilds() {
        val saved = ResourceAdjustFlow().select(cell(quantity = 12)).bump(2).save()

        assertEquals(ResourceAdjustFlow.CellState.CONFIRMED, saved.cellState(cell(quantity = 14)))
        assertEquals(
            ResourceAdjustFlow.CellState.REST,
            saved.cellState(cell(id = "organ", label = "Hueso", quantity = 9)),
            "another cell with the same label stays at rest — matching is by id",
        )
    }

    @Test
    fun editingAndRestStatesFollowTheOpenCell() {
        val flow = ResourceAdjustFlow().select(cell(quantity = 5))

        assertEquals(ResourceAdjustFlow.CellState.EDITING, flow.cellState(cell(quantity = 5)))
        assertEquals(ResourceAdjustFlow.CellState.REST, flow.cellState(cell(id = "organ", label = "Hueso")))
        assertTrue(flow.isEditing, "the non-selected cells dim while a cell is open")
        assertFalse(flow.isIdle)
    }

    @Test
    fun savingWithNoChangeEmitsNoConfirmedResult() {
        val flow = ResourceAdjustFlow().select(cell(quantity = 12)).save()

        assertNull(flow.edit, "the save closes the editor")
        assertNull(flow.confirmed, "nothing changed, nothing to celebrate")
        assertTrue(flow.isIdle)
        assertEquals(0, ResourceAdjustFlow().delta, "no open cell means no ledger call")
    }

    @Test
    fun savingARealChangeRecordsTheCellAndTheSummary() {
        val editing = ResourceAdjustFlow().select(cell(quantity = 12)).bump(2)

        assertEquals(2, editing.delta, "the caller gates onAdjust on this")

        val saved = editing.save()

        assertNull(saved.edit)
        assertEquals(ResourceId("bone"), saved.confirmed?.id)
        assertEquals("Hueso: 12 → 14", saved.confirmed?.result?.summary)
        assertFalse(saved.isIdle, "the success strip keeps the flow out of idle")
    }

    @Test
    fun bumpingWithoutAnOpenCellDoesNothing() {
        val flow = ResourceAdjustFlow().bump(1)

        assertNull(flow.edit)
        assertEquals(0, flow.delta)
    }
}
