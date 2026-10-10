package io.kdmtracker.app.ui.shell

import io.kdmtracker.app.Screen
import io.kdmtracker.core.domain.SettlementPhase

/**
 * The four tabs of the design's bottom navigation, each bound to the screen it shows and carrying
 * the design's Spanish label and Material Symbol name.
 */
enum class ShellTab(
    val screen: Screen,
    val label: String,
    val icon: String,
) {
    SETTLEMENT(Screen.Settlement, "Asentamiento", "fort"),
    SURVIVORS(Screen.Survivors, "Supervivientes", "shield_person"),
    HUNT(Screen.Hunt, "Caza", "explore"),
    SHOWDOWN(Screen.Showdown, "Enfrentamiento", "swords"),
    ;

    companion object {
        /** The tab showing [screen], or `null` when the screen lives outside the shell. */
        fun of(screen: Screen): ShellTab? = entries.firstOrNull { it.screen == screen }

        /** The tab a campaign opens on, given its current lantern-year [phase]. */
        fun initialFor(phase: SettlementPhase): ShellTab =
            when (phase) {
                SettlementPhase.SETTLEMENT -> SETTLEMENT
                SettlementPhase.HUNT -> HUNT
                SettlementPhase.SHOWDOWN -> SHOWDOWN
            }
    }
}
