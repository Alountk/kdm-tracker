package io.kdmtracker.app.ui

import io.kdmtracker.app.Screen
import io.kdmtracker.app.sync.SyncState

/** The colour tone of the status chip, so the composable never re-derives it from [SyncState]. */
enum class ChipTone { CONNECTED, SYNCING, ERROR, SIGNED_OUT }

/** What tapping the status chip does: run a sync, or open the Account screen. */
enum class ChipAction { SYNC, ACCOUNT }

/**
 * The chrome the global `AppTopBar` renders for a frame, derived purely from the screen, the
 * settlement name and the sync state.
 *
 * A pure seam so the bar's copy, tone and visible actions are testable without Compose. The bar
 * itself only renders this and reports the chip action, Home and Account through callbacks.
 */
data class AppChrome(
    val title: String,
    val chipLabel: String,
    val chipTone: ChipTone,
    val chipAction: ChipAction,
    val showHome: Boolean,
    val showAccount: Boolean,
) {
    companion object {
        fun of(
            screen: Screen,
            settlementName: String,
            syncState: SyncState,
        ): AppChrome =
            AppChrome(
                title =
                    when (screen) {
                        Screen.Home -> "Settlements"
                        Screen.Account -> "Account"
                        else -> settlementName
                    },
                chipLabel =
                    when (syncState) {
                        is SyncState.Idle -> "Connected"
                        SyncState.Syncing -> "Syncing…"
                        is SyncState.Error -> "Sync error"
                        SyncState.NotConfigured -> "Not signed in"
                    },
                chipTone =
                    when (syncState) {
                        is SyncState.Idle -> ChipTone.CONNECTED
                        SyncState.Syncing -> ChipTone.SYNCING
                        is SyncState.Error -> ChipTone.ERROR
                        SyncState.NotConfigured -> ChipTone.SIGNED_OUT
                    },
                chipAction = if (syncState is SyncState.NotConfigured) ChipAction.ACCOUNT else ChipAction.SYNC,
                showHome = screen == Screen.Settlement || screen == Screen.Hunt || screen == Screen.Showdown,
                showAccount = true,
            )
    }
}
