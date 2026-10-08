package io.kdmtracker.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import io.kdmtracker.app.sync.SyncState
import io.kdmtracker.app.ui.HomeScreen
import io.kdmtracker.app.ui.HuntScreen
import io.kdmtracker.app.ui.SettlementScreen
import io.kdmtracker.app.ui.ShowdownScreen
import io.kdmtracker.app.ui.describe

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
    val createSucceeded by viewModel.createSucceeded.collectAsState()
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var newSettlementName by remember { mutableStateOf("") }

    // Clear the New-settlement field only once the create actually landed, so a failure keeps the
    // typed name (cheap minor).
    LaunchedEffect(createSucceeded) {
        if (createSucceeded > 0L) newSettlementName = ""
    }

    MaterialTheme {
        Scaffold { innerPadding ->
            Surface(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
            ) {
                val current = state
                val error = loadError
                when {
                    current == null && error != null -> LoadFailure(error)
                    current == null -> Loading()
                    else ->
                        when (current.screen) {
                            Screen.Home ->
                                HomeScreen(
                                    cards = homeCards,
                                    syncState = syncState,
                                    onSelect = viewModel::select,
                                    onCreate = viewModel::create,
                                    onSync = viewModel::syncAll,
                                    onSignIn = viewModel::signIn,
                                    username = username,
                                    password = password,
                                    onUsername = { username = it },
                                    onPassword = { password = it },
                                    newSettlementName = newSettlementName,
                                    onNewSettlementName = { newSettlementName = it },
                                    error = actionError,
                                )
                            Screen.Settlement ->
                                Cockpit(syncState, viewModel) {
                                    SettlementScreen(
                                        state = current,
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
                                    )
                                }
                            Screen.Hunt ->
                                Cockpit(syncState, viewModel) {
                                    HuntScreen(
                                        state = current,
                                        onAdvance = viewModel::advance,
                                        onFail = viewModel::failHunt,
                                    )
                                }
                            Screen.Showdown ->
                                Cockpit(syncState, viewModel) {
                                    ShowdownScreen(
                                        state = current,
                                        onAdvance = viewModel::advance,
                                    )
                                }
                            // The Account body arrives with the AppTopBar shell (Task 4).
                            Screen.Account -> Unit
                        }
                }
            }
        }
    }
}

/** Wraps a cockpit screen with the manual sync/sign-in bar shared by the three phases. */
@Composable
private fun Cockpit(
    syncState: SyncState,
    viewModel: LanternYearViewModel,
    content: @Composable () -> Unit,
) {
    // System back returns to the Home list, mirroring the visible "Home" control (Ruling AR).
    BackHandler { viewModel.goHome() }
    Column(modifier = Modifier.fillMaxSize()) {
        SyncBar(
            syncState = syncState,
            onSync = viewModel::syncNow,
            onSignIn = viewModel::signIn,
            onHome = viewModel::goHome,
        )
        Box(modifier = Modifier.weight(1f)) { content() }
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

@Composable
private fun SyncBar(
    syncState: SyncState,
    onSync: () -> Unit,
    onSignIn: (String, String) -> Unit,
    onHome: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = onHome) {
                Text("Home")
            }
            Text(
                text = syncState.describe(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = onSync, enabled = syncState !is SyncState.Syncing) {
                Text("Sync")
            }
        }
        if (syncState is SyncState.NotConfigured) {
            SignInForm(onSignIn = onSignIn)
        }
    }
}

/** Minimal sign-in form shown while no bearer token is stored (Ruling AG). */
@Composable
private fun SignInForm(onSignIn: (String, String) -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        Button(onClick = { onSignIn(username, password) }) {
            Text("Sign in")
        }
    }
}
