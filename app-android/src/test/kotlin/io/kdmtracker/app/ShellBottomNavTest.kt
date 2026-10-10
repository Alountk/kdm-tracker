package io.kdmtracker.app

import io.kdmtracker.app.ui.shell.ShellBottomNav
import io.kdmtracker.app.ui.shell.ShellTab
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Pins the bottom navigation's active-tab rule: exactly one tab lights up for an in-campaign
 * screen, and neither Home nor Account lights any.
 */
class ShellBottomNavTest {
    @Test
    fun exactlyOneTabIsActiveForAnInCampaignScreen() {
        assertEquals(1, ShellTab.entries.count { it == ShellBottomNav.activeTab(Screen.Settlement) })
        assertEquals(1, ShellTab.entries.count { it == ShellBottomNav.activeTab(Screen.Survivors) })
        assertEquals(1, ShellTab.entries.count { it == ShellBottomNav.activeTab(Screen.Hunt) })
        assertEquals(1, ShellTab.entries.count { it == ShellBottomNav.activeTab(Screen.Showdown) })
    }

    @Test
    fun neitherHomeNorAccountActivatesATab() {
        assertNull(ShellBottomNav.activeTab(Screen.Home))
        assertNull(ShellBottomNav.activeTab(Screen.Account))
    }
}
