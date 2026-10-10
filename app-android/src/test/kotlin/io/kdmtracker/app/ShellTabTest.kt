package io.kdmtracker.app

import io.kdmtracker.app.ui.shell.ShellTab
import io.kdmtracker.core.domain.SettlementPhase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Pins the shell's four tabs to the design's labels and icons, and the rules that map a screen to
 * its tab and a lantern-year phase to the tab a campaign opens on.
 */
class ShellTabTest {
    @Test
    fun everyTabCarriesTheDesignsLabelAndIcon() {
        assertEquals(
            listOf("Asentamiento", "Supervivientes", "Caza", "Enfrentamiento"),
            ShellTab.entries.map { it.label },
        )
        assertEquals(
            listOf("fort", "shield_person", "explore", "swords"),
            ShellTab.entries.map { it.icon },
        )
    }

    @Test
    fun onlyInCampaignScreensHaveATab() {
        assertEquals(ShellTab.SETTLEMENT, ShellTab.of(Screen.Settlement))
        assertEquals(ShellTab.SURVIVORS, ShellTab.of(Screen.Survivors))
        assertEquals(ShellTab.HUNT, ShellTab.of(Screen.Hunt))
        assertEquals(ShellTab.SHOWDOWN, ShellTab.of(Screen.Showdown))
        assertNull(ShellTab.of(Screen.Home))
        assertNull(ShellTab.of(Screen.Account))
    }

    @Test
    fun thePhasePicksTheOpeningTab() {
        assertEquals(ShellTab.SETTLEMENT, ShellTab.initialFor(SettlementPhase.SETTLEMENT))
        assertEquals(ShellTab.HUNT, ShellTab.initialFor(SettlementPhase.HUNT))
        assertEquals(ShellTab.SHOWDOWN, ShellTab.initialFor(SettlementPhase.SHOWDOWN))
    }

    @Test
    fun everyTabResolvesToItsOwnScreen() {
        ShellTab.entries.forEach { tab -> assertEquals(tab, ShellTab.of(tab.screen)) }
    }
}
