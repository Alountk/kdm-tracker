package io.kdmtracker.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.kdmtracker.app.ui.AccountScreen
import io.kdmtracker.app.ui.AppChrome
import io.kdmtracker.app.ui.AppTopBar
import io.kdmtracker.app.ui.ChipAction
import io.kdmtracker.app.ui.HomeScreen
import io.kdmtracker.app.ui.HuntScreen
import io.kdmtracker.app.ui.PendientePlaceholder
import io.kdmtracker.app.ui.SettlementScreen
import io.kdmtracker.app.ui.ShowdownScreen
import io.kdmtracker.app.ui.shell.AppShell
import io.kdmtracker.app.ui.shell.ShellTab
import io.kdmtracker.app.ui.theme.KdmTheme

@Composable
fun KdmTrackerApp(
    viewModel: LanternYearViewModel =
        viewModel(factory = LanternYearViewModel.factory(LocalContext.current)),
) {
    val state by viewModel.state.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val loadError by viewModel.loadError.collectAsState()
    val homeCards by viewModel.homeCards.collectAsState()
    val actionError by viewModel.actionError.collectAsState()
    val signInError by viewModel.signInError.collectAsState()
    val createSucceeded by viewModel.createSucceeded.collectAsState()
    var newSettlementName by remember { mutableStateOf("") }

    // Clear the New-settlement field only once the create actually landed, so a failure keeps the
    // typed name (cheap minor).
    LaunchedEffect(createSucceeded) {
        if (createSucceeded > 0L) newSettlementName = ""
    }

    KdmTheme {
        Scaffold(
            topBar = {
                state?.let { current ->
                    // Inside a campaign the design's own top bar lives in the shell, so the
                    // app chrome would be a second bar stacked above it.
                    if (AppShell.inShell(current.screen)) return@let
                    AppTopBar(
                        chrome = AppChrome.of(current.screen, current.settlementName, syncState),
                        onChipAction = { action ->
                            if (action == ChipAction.ACCOUNT) viewModel.goAccount() else viewModel.syncNow()
                        },
                        onHome = viewModel::goHome,
                        onAccount = viewModel::goAccount,
                    )
                }
            },
        ) { innerPadding ->
            Surface(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
            ) {
                val current = state
                // System back on a phase returns to the Home list, mirroring the visible Home action
                // (the Account overlay registers its own handler below and takes precedence).
                BackHandler(
                    enabled =
                        current?.screen == Screen.Settlement ||
                            current?.screen == Screen.Survivors ||
                            current?.screen == Screen.Hunt ||
                            current?.screen == Screen.Showdown,
                ) { viewModel.goHome() }
                val error = loadError
                when {
                    current == null && error != null -> LoadFailure(error)
                    current == null -> Loading()
                    else ->
                        when (current.screen) {
                            Screen.Home ->
                                HomeScreen(
                                    cards = homeCards,
                                    onSelect = viewModel::select,
                                    onCreate = viewModel::create,
                                    newSettlementName = newSettlementName,
                                    onNewSettlementName = { newSettlementName = it },
                                    error = actionError,
                                )
                            Screen.Account -> {
                                BackHandler { viewModel.closeAccount() }
                                AccountScreen(
                                    syncState = syncState,
                                    onSignIn = viewModel::signIn,
                                    onSignOut = viewModel::signOut,
                                    onSync = viewModel::syncNow,
                                    onClose = viewModel::closeAccount,
                                    error = signInError,
                                )
                            }
                            else -> InCampaignScreen(state = current, viewModel = viewModel)
                        }
                }
            }
        }
    }
}

/**
 * The design's shell around the four in-campaign views. The tab navigates freely; the record's phase
 * only decided which tab [LanternYearViewModel.select] opened the campaign on.
 */
@Composable
private fun InCampaignScreen(
    state: UiState,
    viewModel: LanternYearViewModel,
) {
    val tab = ShellTab.of(state.screen) ?: return
    AppShell(
        state = state,
        tab = tab,
        onSelectTab = { viewModel.show(it.screen) },
        onAccount = viewModel::goAccount,
        onSelectStep = viewModel::selectStep,
    ) {
        when (state.screen) {
            Screen.Settlement ->
                SettlementScreen(
                    state = state,
                    onAdvance = viewModel::advance,
                    onPrevious = viewModel::previous,
                    onSelectStep = viewModel::selectStep,
                    onInnovate = viewModel::innovate,
                    onBuild = viewModel::build,
                    onSpendEndeavor = viewModel::spendEndeavor,
                    onAddResource = viewModel::addResource,
                    onCraft = viewModel::craft,
                    onRecordEvent = viewModel::recordSettlementEvent,
                    onDrawEvent = viewModel::drawSettlementEvent,
                    onRecordDeaths = viewModel::recordDeaths,
                    onRecordBirths = viewModel::recordBirths,
                    onChoosePrinciple = viewModel::choosePrinciple,
                    onResolveMilestone = viewModel::resolveMilestone,
                    onHealSurvivors = viewModel::healSurvivors,
                    onDepartingChange = viewModel::markDeparting,
                    onAssignGear = viewModel::assignGear,
                    onUnassignGear = viewModel::unassignGear,
                    onSaveSurvivor = viewModel::updateSurvivor,
                    onDeclareSpecialShowdown = viewModel::declareSpecialShowdown,
                    onSetUpShowdown = viewModel::setUpShowdown,
                    onDrawShowdownCard = viewModel::drawShowdownCard,
                    onWoundMonster = viewModel::woundMonster,
                    onGrantHuntXp = viewModel::grantHuntXp,
                )
            // The design's Supervivientes tab is the paged survivor sheet; that view is its own
            // slice, so the tab is deliberately empty rather than a sheet with a dead button.
            Screen.Survivors -> PendientePlaceholder()
            Screen.Hunt ->
                HuntScreen(
                    state = state,
                    onSetUpHunt = viewModel::setUpHunt,
                    onAssignPlayer = viewModel::assignPlayer,
                    onMarkSpaceKind = viewModel::markSpaceKind,
                    onAssignHuntEvent = viewModel::assignHuntEvent,
                    onResolveHuntEvent = viewModel::resolveHuntEvent,
                    onAdvanceHunt = viewModel::advanceHunt,
                    onFinishHunt = viewModel::finishHunt,
                )
            Screen.Showdown ->
                ShowdownScreen(
                    state = state,
                    onSetUpShowdown = viewModel::setUpShowdown,
                    onDrawShowdownCard = viewModel::drawShowdownCard,
                    onWoundMonster = viewModel::woundMonster,
                    onGrantHuntXp = viewModel::grantHuntXp,
                    onRecordDeaths = viewModel::recordDeaths,
                    onFinishShowdown = viewModel::finishShowdown,
                )
            else -> Unit
        }
    }
}

@Composable
private fun Loading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun LoadFailure(message: String) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("Failed to load: $message", style = MaterialTheme.typography.bodyLarge)
    }
}
