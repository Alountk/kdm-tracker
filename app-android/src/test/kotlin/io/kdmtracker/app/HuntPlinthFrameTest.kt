package io.kdmtracker.app

import io.kdmtracker.app.ui.hunt.HuntPlinthFrame
import io.kdmtracker.core.domain.HuntState
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins the quarry-plinth data the `QuarryPlinth` composable renders 1:1 from the Stitch design:
 * the `Presa Activa`/level label, the quarry (falling back to the design's example `León Blanco`),
 * the `Variante Primera Historia` variant, and the derived `Distancia`.
 *
 * The plinth's layout and colours are styling (verified by emulator capture, not a JVM test), so
 * this pins only the model behind them.
 */
class HuntPlinthFrameTest {
    @Test
    fun plinthLabelsTheActivePreyAndLevel() {
        val frame = HuntPlinthFrame.from(HuntState(quarry = "White Lion", level = 2))

        assertEquals("Presa Activa", frame.eyebrow)
        assertEquals("Nivel 2", frame.levelLabel)
        assertEquals("White Lion", frame.quarry)
        assertEquals("Variante Primera Historia", frame.variant)
    }

    @Test
    fun plinthFallsBackToTheDesignExampleQuarryBeforeSetup() {
        val frame = HuntPlinthFrame.from(HuntState())

        assertEquals("León Blanco", frame.quarry)
    }

    @Test
    fun plinthReportsTheDerivedDistanceInCasillas() {
        // party on space 9, monster on space 12 -> Distancia 3 casillas (the design's example)
        val hunt = HuntState(position = 9, monsterPosition = 12)

        assertEquals("3", HuntPlinthFrame.from(hunt).distance)
    }

    @Test
    fun plinthNeverReportsANegativeDistance() {
        // monster behind the party -> clamp at 0, never a negative number of casillas
        val hunt = HuntState(position = 5, monsterPosition = 3)

        assertEquals("0", HuntPlinthFrame.from(hunt).distance)
    }
}
