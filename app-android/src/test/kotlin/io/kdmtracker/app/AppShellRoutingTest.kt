package io.kdmtracker.app

import io.kdmtracker.app.ui.shell.AppShell
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pins which screens get the shell. Home and Account live outside it — they keep the existing chrome
 * and must never show the bottom navigation.
 */
class AppShellRoutingTest {
    @Test
    fun onlyInCampaignScreensGetTheShell() {
        assertTrue(AppShell.inShell(Screen.Settlement))
        assertTrue(AppShell.inShell(Screen.Survivors))
        assertTrue(AppShell.inShell(Screen.Hunt))
        assertTrue(AppShell.inShell(Screen.Showdown))
    }

    @Test
    fun homeAndAccountStayOutsideTheShell() {
        assertFalse(AppShell.inShell(Screen.Home))
        assertFalse(AppShell.inShell(Screen.Account))
    }
}
