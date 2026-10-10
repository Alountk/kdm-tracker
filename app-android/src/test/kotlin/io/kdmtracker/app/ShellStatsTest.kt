package io.kdmtracker.app

import io.kdmtracker.app.ui.shell.ShellStats
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins the top bar's stat cluster to the design's three entries, in its order, with the lantern
 * reserve honestly marked as unbacked rather than faked.
 */
class ShellStatsTest {
    @Test
    fun theClusterIsPopulationThenDeathsThenTheLanternReserve() {
        val stats = ShellStats.of(population = 12, deaths = 0)

        assertEquals(listOf("group", "skull", "flare"), stats.map { it.icon })
        assertEquals(listOf("12", "0", "—"), stats.map { it.value })
    }

    @Test
    fun onlyTheLanternReserveIsUnbacked() {
        assertEquals(listOf(false, false, true), ShellStats.of(population = 0, deaths = 0).map { it.unbacked })
    }

    @Test
    fun zeroStatsStillRenderEveryChip() {
        val stats = ShellStats.of(population = 0, deaths = 0)

        assertEquals(3, stats.size)
        assertEquals("0", stats.first().value)
    }
}
