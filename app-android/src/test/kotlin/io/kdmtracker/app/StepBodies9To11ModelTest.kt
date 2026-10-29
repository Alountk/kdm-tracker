package io.kdmtracker.app

import io.kdmtracker.app.ui.GearChip
import io.kdmtracker.app.ui.gearChips
import io.kdmtracker.app.ui.resourceGridRows
import io.kdmtracker.app.ui.showYearOneExemption
import io.kdmtracker.core.domain.LocalizedText
import io.kdmtracker.core.domain.ResourceCategory
import io.kdmtracker.core.domain.ResourceDefinition
import io.kdmtracker.core.domain.ResourceId
import io.kdmtracker.core.domain.StorageLine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * S9–S11: the step-bodies' pure derivations — the step-9 year gate, the step-10 basic-resource
 * grid's rows (ids, labels, sub-labels, glyph, counts) and its stored-gear chip grouping. Verbatim
 * design copy is pinned separately in `SettlementStepBodiesTest`; these tests only pin the
 * derivations' behavior.
 */
class StepBodies9To11ModelTest {
    private fun definition(
        id: String,
        es: String,
    ): ResourceDefinition =
        ResourceDefinition(
            id = ResourceId(id),
            names = LocalizedText(es),
            category = ResourceCategory.BASIC,
        )

    private val bone = definition("bone", "Hueso de Monstruo")
    private val organ = definition("organ", "Órgano de Monstruo")
    private val hide = definition("hide", "Piel de Monstruo")
    private val scrap = definition("scrap", "Scrap")
    private val catalog = listOf(bone, organ, hide, scrap)

    // -- S9: the year-1 exemption card renders during the first settlement phase --

    @Test
    fun theYearOneExemptionRendersDuringTheFirstSettlementPhase() {
        // AdvanceTimeline increments the year when leaving step 4 and forward skips are forbidden,
        // so the first settlement phase at step 9 always runs under lantern year 2.
        assertTrue(showYearOneExemption(1))
        assertTrue(showYearOneExemption(2))
        assertFalse(showYearOneExemption(3))
    }

    // -- S10: the four basic resources, in the design's order --

    @Test
    fun theGridCarriesTheFourBasicResourcesInTheDesignsOrder() {
        val rows = resourceGridRows(definitions = catalog, storage = emptyList())

        assertEquals(listOf("bone", "organ", "hide", "scrap"), rows.map { it.id.value })
        assertEquals(listOf("HUESO", "ÓRGANO", "PELLEJO", "CHATARRA"), rows.map { it.label })
        assertEquals(listOf("Básico", "Básico", "Básico", "Raro"), rows.map { it.subLabel })
        assertEquals(listOf("radiology", "vital_signs", "texture", "hardware"), rows.map { it.glyph })
    }

    @Test
    fun theGridCountsComeFromStorageAndDefaultToZero() {
        val rows =
            resourceGridRows(
                definitions = catalog,
                storage = listOf(StorageLine(ResourceId("hide"), 3), StorageLine(ResourceId("scrap"), 1)),
            )

        assertEquals(listOf(0, 0, 3, 1), rows.map { it.count })
    }

    @Test
    fun resourcesTheDesignDoesNotNameDoNotReachTheGrid() {
        val rows =
            resourceGridRows(
                definitions = catalog + definition("herb", "Hierba"),
                storage = emptyList(),
            )

        assertEquals(4, rows.size, "only the four designed basics render (the rest is backlog)")
    }

    @Test
    fun theRemoveButtonIsInertAtZeroLikeTheSummarysStepper() {
        val rows =
            resourceGridRows(
                definitions = catalog,
                storage = listOf(StorageLine(ResourceId("bone"), 0), StorageLine(ResourceId("organ"), 1)),
            )

        assertFalse(rows[0].canDecrement, "minus is inert at zero — AddResourceEntry refuses the overdraw")
        assertTrue(rows[1].canDecrement)
    }

    // -- S10: one chip per distinct stored item, with its count --

    @Test
    fun gearChipsGroupDuplicatesWithCountsInFirstAppearanceOrder() {
        assertEquals(
            listOf(GearChip("Tela", 2), GearChip("Piedra", 1)),
            gearChips(listOf("Tela", "Tela", "Piedra")),
        )
    }

    @Test
    fun noStoredGearYieldsNoChips() {
        assertEquals(emptyList(), gearChips(emptyList()))
    }
}
