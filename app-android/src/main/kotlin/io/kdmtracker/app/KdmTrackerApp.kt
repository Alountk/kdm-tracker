package io.kdmtracker.app

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.kdmtracker.app.sync.SyncState
import io.kdmtracker.app.ui.HuntScreen
import io.kdmtracker.app.ui.SettlementScreen
import io.kdmtracker.app.ui.ShowdownScreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun KdmTrackerApp(
    viewModel: LanternYearViewModel =
        viewModel(factory = LanternYearViewModel.factory(LocalContext.current)),
) {
    val state by viewModel.state.collectAsState()
    val syncState by viewModel.syncState.collectAsState()

    MaterialTheme {
        Scaffold { innerPadding ->
            Surface(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
            ) {
                val current = state
                if (current == null) {
                    Loading()
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        SyncBar(syncState = syncState, onSync = viewModel::syncNow)
                        Box(modifier = Modifier.weight(1f)) {
                            when (current.screen) {
                                Screen.Settlement ->
                                    SettlementScreen(
                                        state = current,
                                        onAdvance = viewModel::advance,
                                        onPrevious = viewModel::previous,
                                        onSelectStep = viewModel::selectStep,
                                    )
                                Screen.Hunt ->
                                    HuntScreen(
                                        state = current,
                                        onAdvance = viewModel::advance,
                                        onFail = viewModel::failHunt,
                                    )
                                Screen.Showdown ->
                                    ShowdownScreen(
                                        state = current,
                                        onAdvance = viewModel::advance,
                                    )
                            }
                        }
                    }
                }
            }
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
private fun SyncBar(
    syncState: SyncState,
    onSync: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
}

private fun SyncState.describe(): String =
    when (this) {
        is SyncState.Idle -> lastSyncedAtMillis?.let { "Last sync ${formatTime(it)}" } ?: "Not synced yet"
        SyncState.Syncing -> "Syncing…"
        is SyncState.Error -> "Sync error: $message"
    }

private fun formatTime(millis: Long): String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(millis))
