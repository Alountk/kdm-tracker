package io.kdmtracker.app

import io.kdmtracker.app.ui.shell.PhaseCarouselModel
import io.kdmtracker.core.rules.SettlementStepCatalog
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins the phase carousel to the design's eleven chips — their icons, short labels and the single
 * crimson special step — and keeps it in step with the real settlement step catalog.
 */
class PhaseCarouselTest {
    @Test
    fun theStripRunsTheDesignsElevenSteps() {
        val steps = PhaseCarouselModel.steps

        assertEquals(11, steps.size)
        assertEquals((1..11).toList(), steps.map { it.number })
        assertEquals(
            listOf(
                "group_add",
                "account_balance",
                "emergency",
                "hourglass_empty",
                "skull",
                "check_box_outline_blank",
                "handyman",
                "shield",
                "swords",
                "edit_note",
                "flare",
            ),
            steps.map { it.icon },
        )
        assertEquals(
            listOf(
                "Prep.",
                "Regreso",
                "Esfuerzo",
                "Tiempo",
                "Muertes",
                "Hitos",
                "Desarrollo",
                "Partida",
                "Especial",
                "Almacén",
                "Fin",
            ),
            steps.map { it.shortLabel },
        )
    }

    @Test
    fun theStripMirrorsTheRealStepCatalog() {
        assertEquals(SettlementStepCatalog.ALL.map { it.number }, PhaseCarouselModel.steps.map { it.number })
    }

    @Test
    fun onlyStepNineIsSpecial() {
        assertEquals(listOf(9), PhaseCarouselModel.steps.filter { it.isSpecial }.map { it.number })
    }

    @Test
    fun anOutOfRangeStepNeverBreaksTheStrip() {
        assertEquals(1, PhaseCarouselModel.active(0))
        assertEquals(11, PhaseCarouselModel.active(99))
        assertEquals(7, PhaseCarouselModel.active(7))
    }
}
