package io.kdmtracker.app

import io.kdmtracker.app.ui.SurvivorFilter
import io.kdmtracker.app.ui.SurvivorTone
import io.kdmtracker.app.ui.filterSurvivors
import io.kdmtracker.app.ui.survivorTone
import io.kdmtracker.core.domain.HitLocation
import io.kdmtracker.core.domain.Survivor
import io.kdmtracker.core.domain.SurvivorStatus
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.domain.WoundState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SurvivorRosterTest {
    private fun survivor(
        seed: Long,
        status: SurvivorStatus = SurvivorStatus.ALIVE,
        name: String = "Eli",
        injured: Boolean = false,
    ): Survivor =
        Survivor(
            id = Ulid.generate(seed) { 0 },
            name = name,
            status = status,
            wounds = if (injured) mapOf(HitLocation.HEAD to WoundState(light = true)) else emptyMap(),
        )

    @Test
    fun filterAllReturnsEverySurvivor() {
        val roster = listOf(survivor(1), survivor(2, SurvivorStatus.DEPARTING), survivor(3, SurvivorStatus.DEAD))

        assertEquals(roster, filterSurvivors(roster, SurvivorFilter.ALL))
    }

    @Test
    fun filterDepartingKeepsOnlyDeparting() {
        val roster = listOf(survivor(1), survivor(2, SurvivorStatus.DEPARTING))

        assertEquals(listOf(roster[1]), filterSurvivors(roster, SurvivorFilter.DEPARTING))
    }

    @Test
    fun filterDeadKeepsOnlyDead() {
        val roster = listOf(survivor(1), survivor(2, SurvivorStatus.DEAD))

        assertEquals(listOf(roster[1]), filterSurvivors(roster, SurvivorFilter.DEAD))
    }

    @Test
    fun filterUnnamedKeepsBlankNames() {
        val roster = listOf(survivor(1, name = "Eli"), survivor(2, name = ""))

        assertEquals(listOf(roster[1]), filterSurvivors(roster, SurvivorFilter.UNNAMED))
    }

    @Test
    fun filterInjuredKeepsAnyLightOrHeavyWound() {
        val lightHead = survivor(1, injured = true).copy(wounds = mapOf(HitLocation.HEAD to WoundState(light = true)))
        val heavyTorso = survivor(2).copy(wounds = mapOf(HitLocation.TORSO to WoundState(heavy = true)))
        val clean = survivor(3)
        val roster = listOf(lightHead, heavyTorso, clean)

        assertEquals(listOf(lightHead, heavyTorso), filterSurvivors(roster, SurvivorFilter.INJURED))
    }

    @Test
    fun filtersAgreeWithTheDotsOnOverlappingStates() {
        val departingInjured = survivor(1, SurvivorStatus.DEPARTING, injured = true)
        val deadInjured = survivor(2, SurvivorStatus.DEAD, injured = true)
        val unnamedInjured = survivor(3, name = "", injured = true)
        val roster = listOf(departingInjured, deadInjured, unnamedInjured)

        // Each survivor appears under exactly the chip its dot matches — dead/departing never under
        // "Injured"; an injured blank-name survivor is "Injured", not "Unnamed".
        assertEquals(listOf(departingInjured), filterSurvivors(roster, SurvivorFilter.DEPARTING))
        assertEquals(listOf(deadInjured), filterSurvivors(roster, SurvivorFilter.DEAD))
        assertEquals(listOf(unnamedInjured), filterSurvivors(roster, SurvivorFilter.INJURED))
        assertTrue(filterSurvivors(roster, SurvivorFilter.UNNAMED).isEmpty())
    }

    @Test
    fun tonePrecedence() {
        assertEquals(SurvivorTone.DEAD, survivorTone(survivor(1, SurvivorStatus.DEAD, injured = true)))
        assertEquals(SurvivorTone.DEPARTING, survivorTone(survivor(2, SurvivorStatus.DEPARTING, injured = true)))
        assertEquals(SurvivorTone.INJURED, survivorTone(survivor(3, injured = true)))
        assertEquals(SurvivorTone.UNNAMED, survivorTone(survivor(4, name = "")))
        assertEquals(SurvivorTone.ALIVE, survivorTone(survivor(5)))
    }
}
