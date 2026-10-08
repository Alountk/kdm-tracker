package io.kdmtracker.app

import io.kdmtracker.app.sync.SyncState
import io.kdmtracker.core.persistence.InMemoryCampaignStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Pins the Account overlay navigation and sign-out: opening Account from Home or a phase and
 * closing it must return to where it was opened from, and signing out must delegate to the
 * components and leave the session signed out.
 */
class AccountNavigationTest {
    @Test
    fun openingAccountFromHomeAndClosingReturnsToHome() {
        val viewModel = viewModel()
        assertEquals(Screen.Home, viewModel.state.value?.screen)

        viewModel.goAccount()
        assertEquals(Screen.Account, viewModel.state.value?.screen)

        viewModel.closeAccount()
        assertEquals(Screen.Home, viewModel.state.value?.screen)
    }

    @Test
    fun closingAccountReturnsToThePhaseItWasOpenedFrom() {
        val viewModel = viewModel()
        viewModel.select(
            viewModel.homeCards.value
                .first()
                .campaignId,
        )
        val phaseScreen = viewModel.state.value?.screen
        assertNotEquals(Screen.Home, phaseScreen, "selecting a campaign must open a phase")

        viewModel.goAccount()
        assertEquals(Screen.Account, viewModel.state.value?.screen)

        viewModel.closeAccount()
        assertEquals(phaseScreen, viewModel.state.value?.screen)
    }

    @Test
    fun signOutDelegatesAndMarksTheSessionSignedOut() {
        var signedOut = false
        val viewModel = viewModel(signOut = { signedOut = true })

        viewModel.signOut()

        assertTrue(signedOut, "sign out must reach the components")
        assertEquals(SyncState.NotConfigured, viewModel.syncState.value)
    }

    private fun viewModel(signOut: (() -> Unit)? = null): LanternYearViewModel =
        LanternYearViewModel(
            buildComponents = {
                AppComponents(
                    LanternYearController(SettlementRepository(InMemoryCampaignStore())),
                    signOut = signOut,
                )
            },
            dispatcher = Dispatchers.Unconfined,
            scope = CoroutineScope(Dispatchers.Unconfined),
        )
}
