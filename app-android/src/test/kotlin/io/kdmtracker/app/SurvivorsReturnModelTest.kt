package io.kdmtracker.app

import io.kdmtracker.app.ui.survivorStatusLine
import io.kdmtracker.core.domain.HitLocation
import io.kdmtracker.core.domain.WoundState
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * S3: the step-2 survivor card's status line derives from the wound boxes — `Sin lesiones
 * permanentes` while no hit location carries a light (`herida firme`) or heavy (`herida grave`)
 * mark, and the counted `Heridas leves {n} · graves {m}` once any is set. Armor hits are not
 * injuries and never move the line.
 */
class SurvivorsReturnModelTest {
    @Test
    fun aWoundFreeSurvivorHasNoPermanentInjuries() {
        assertEquals("Sin lesiones permanentes", survivorStatusLine(emptyMap()))
    }

    @Test
    fun armorHitsAreNotInjuries() {
        val wounds =
            mapOf(
                HitLocation.HEAD to WoundState(armor = 2),
                HitLocation.TORSO to WoundState(armor = 1),
            )

        assertEquals("Sin lesiones permanentes", survivorStatusLine(wounds))
    }

    @Test
    fun lightWoundsAreCountedAsLeves() {
        val wounds = mapOf(HitLocation.ARMS to WoundState(light = true))

        assertEquals("Heridas leves 1 · graves 0", survivorStatusLine(wounds))
    }

    @Test
    fun mixedLightAndHeavyWoundsCountBoth() {
        val wounds =
            mapOf(
                HitLocation.HEAD to WoundState(light = true, heavy = true),
                HitLocation.TORSO to WoundState(light = true),
                HitLocation.LEGS to WoundState(heavy = true),
            )

        assertEquals("Heridas leves 2 · graves 2", survivorStatusLine(wounds))
    }
}
