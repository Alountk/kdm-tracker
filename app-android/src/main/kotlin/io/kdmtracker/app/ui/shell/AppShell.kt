package io.kdmtracker.app.ui.shell

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.kdmtracker.app.Screen
import io.kdmtracker.app.UiState

/** Which screens the shell wraps. Kept beside the frame it gates. */
object AppShell {
    /** Whether [screen] renders inside the shell (i.e. it is one of the four in-campaign views). */
    fun inShell(screen: Screen): Boolean = ShellTab.of(screen) != null
}

/**
 * The design's frame: the fixed top bar, the selected view, and the four-tab bottom navigation.
 *
 * The bars are laid out in the column rather than as overlays, so content never scrolls under them.
 * The design achieves the same with fixed bars plus `pt-20 pb-24`; with no backdrop filter to
 * reproduce, the in-flow column is equivalent and cannot overlap its content.
 *
 * @param tab the view to show; its label is also the bar's title, as each design screen names itself.
 */
@Composable
fun AppShell(
    state: UiState,
    tab: ShellTab,
    onSelectTab: (ShellTab) -> Unit,
    onAccount: () -> Unit,
    onExit: () -> Unit,
    onSelectStep: (Int) -> Unit,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ShellTopBar(
            title = tab.label,
            lanternYear = state.lanternYear,
            settlementName = state.settlementName,
            population = state.population,
            deaths = state.deaths,
            onAccount = onAccount,
        )
        Column(modifier = Modifier.weight(1f)) {
            if (tab == ShellTab.SETTLEMENT) {
                PhaseCarousel(
                    currentStep = state.currentStep,
                    reachableSteps = state.reachableSteps,
                    onSelectStep = onSelectStep,
                )
            }
            content()
        }
        ShellBottomNav(selected = tab, onSelect = onSelectTab, onExit = onExit)
    }
}
