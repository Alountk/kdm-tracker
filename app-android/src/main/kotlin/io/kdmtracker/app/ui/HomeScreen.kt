package io.kdmtracker.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.kdmtracker.app.CampaignCard
import io.kdmtracker.app.sync.SyncState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Stateless rendering of the Home: one tappable card per campaign, a "New settlement" form and the
 * sync/sign-in control (Ruling AK).
 *
 * Each card shows `name · Lantern Year N · Population P` and the campaign's sharing badge
 * [CampaignCard.banner], so an owner-alone, owner-with-others and shared-with-me campaign are told
 * apart at a glance (Review Focus 2). The screen owns no state or rules: selections, creates,
 * syncs and sign-ins are reported through the callbacks.
 */
@Composable
fun HomeScreen(
    cards: List<CampaignCard>,
    syncState: SyncState,
    onSelect: (String) -> Unit,
    onCreate: (String) -> Unit,
    onSync: () -> Unit,
    onSignIn: (String, String) -> Unit,
    username: String,
    password: String,
    onUsername: (String) -> Unit,
    onPassword: (String) -> Unit,
    newSettlementName: String,
    onNewSettlementName: (String) -> Unit,
    error: String?,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Settlements", style = MaterialTheme.typography.headlineMedium)

        HomeSyncControl(
            syncState = syncState,
            onSync = onSync,
            onSignIn = onSignIn,
            username = username,
            password = password,
            onUsername = onUsername,
            onPassword = onPassword,
        )

        error?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        NewSettlementForm(
            name = newSettlementName,
            onNameChange = onNewSettlementName,
            onCreate = onCreate,
        )

        if (cards.isEmpty()) {
            Text(
                text = "No settlements yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            cards.forEach { card ->
                CampaignCardItem(card = card, onSelect = onSelect)
            }
        }
    }
}

@Composable
private fun CampaignCardItem(
    card: CampaignCard,
    onSelect: (String) -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onSelect(card.campaignId) },
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "${card.name} · Lantern Year ${card.lanternYear} · Population ${card.population}",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = card.banner.label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NewSettlementForm(
    name: String,
    onNameChange: (String) -> Unit,
    onCreate: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("New settlement") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        Button(
            onClick = { onCreate(name) },
            enabled = name.isNotBlank(),
        ) {
            Text("Create")
        }
    }
}

@Composable
private fun HomeSyncControl(
    syncState: SyncState,
    onSync: () -> Unit,
    onSignIn: (String, String) -> Unit,
    username: String,
    password: String,
    onUsername: (String) -> Unit,
    onPassword: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
        if (syncState is SyncState.NotConfigured) {
            SignInForm(
                username = username,
                password = password,
                onUsername = onUsername,
                onPassword = onPassword,
                onSignIn = onSignIn,
            )
        }
    }
}

/** Minimal sign-in form shown while no bearer token is stored (Ruling AG). */
@Composable
private fun SignInForm(
    username: String,
    password: String,
    onUsername: (String) -> Unit,
    onPassword: (String) -> Unit,
    onSignIn: (String, String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = username,
            onValueChange = onUsername,
            label = { Text("Username") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = password,
            onValueChange = onPassword,
            label = { Text("Password") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        Button(onClick = { onSignIn(username, password) }) {
            Text("Sign in")
        }
    }
}

/** Human-readable status of the last sync attempt, shared by the Home and the cockpit bar. */
internal fun SyncState.describe(): String =
    when (this) {
        is SyncState.Idle -> lastSyncedAtMillis?.let { "Last sync ${formatTime(it)}" } ?: "Not synced yet"
        SyncState.Syncing -> "Syncing…"
        SyncState.NotConfigured -> "Not signed in"
        is SyncState.Error -> "Sync error: $message"
    }

private fun formatTime(millis: Long): String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(millis))
