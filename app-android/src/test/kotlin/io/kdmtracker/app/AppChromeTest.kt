package io.kdmtracker.app

import io.kdmtracker.app.sync.SyncState
import io.kdmtracker.app.ui.AppChrome
import io.kdmtracker.app.ui.ChipAction
import io.kdmtracker.app.ui.ChipTone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pins the pure top-bar derivation: the title per screen, the status chip (label, tone and the
 * action its tap must route to) per [SyncState], and which actions are visible.
 */
class AppChromeTest {
    private val name = "New Settlement"

    @Test
    fun homeTitleIsSettlements() {
        assertEquals("Settlements", AppChrome.of(Screen.Home, name, SyncState.Idle()).title)
    }

    @Test
    fun phaseTitleIsTheSettlementName() {
        for (screen in listOf(Screen.Settlement, Screen.Hunt, Screen.Showdown)) {
            assertEquals(name, AppChrome.of(screen, name, SyncState.Idle()).title, "screen $screen")
        }
    }

    @Test
    fun accountTitleIsAccount() {
        assertEquals("Account", AppChrome.of(Screen.Account, name, SyncState.Idle()).title)
    }

    @Test
    fun chipToneAndLabelPerSyncState() {
        assertEquals(ChipTone.CONNECTED, AppChrome.of(Screen.Home, name, SyncState.Idle()).chipTone)
        assertEquals("Connected", AppChrome.of(Screen.Home, name, SyncState.Idle()).chipLabel)

        assertEquals(ChipTone.SYNCING, AppChrome.of(Screen.Home, name, SyncState.Syncing).chipTone)
        assertEquals("Syncing…", AppChrome.of(Screen.Home, name, SyncState.Syncing).chipLabel)

        assertEquals(ChipTone.ERROR, AppChrome.of(Screen.Home, name, SyncState.Error("boom")).chipTone)
        assertEquals("Sync error", AppChrome.of(Screen.Home, name, SyncState.Error("boom")).chipLabel)

        assertEquals(ChipTone.SIGNED_OUT, AppChrome.of(Screen.Home, name, SyncState.NotConfigured).chipTone)
        assertEquals("Not signed in", AppChrome.of(Screen.Home, name, SyncState.NotConfigured).chipLabel)
    }

    @Test
    fun signedOutChipOpensAccountOthersSync() {
        assertEquals(
            ChipAction.ACCOUNT,
            AppChrome.of(Screen.Home, name, SyncState.NotConfigured).chipAction,
        )
        assertEquals(ChipAction.SYNC, AppChrome.of(Screen.Home, name, SyncState.Idle()).chipAction)
        assertEquals(ChipAction.SYNC, AppChrome.of(Screen.Home, name, SyncState.Syncing).chipAction)
        assertEquals(ChipAction.SYNC, AppChrome.of(Screen.Home, name, SyncState.Error("boom")).chipAction)
    }

    @Test
    fun homeIconOnlyOnPhases() {
        assertFalse(AppChrome.of(Screen.Home, name, SyncState.Idle()).showHome)
        assertFalse(AppChrome.of(Screen.Account, name, SyncState.Idle()).showHome)
        for (screen in listOf(Screen.Settlement, Screen.Hunt, Screen.Showdown)) {
            assertTrue(AppChrome.of(screen, name, SyncState.Idle()).showHome, "screen $screen")
        }
    }

    @Test
    fun accountIconAlwaysVisible() {
        for (screen in listOf(Screen.Home, Screen.Settlement, Screen.Hunt, Screen.Showdown, Screen.Account)) {
            assertTrue(AppChrome.of(screen, name, SyncState.Idle()).showAccount, "screen $screen")
        }
    }
}
