package io.kdmtracker.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.kdmtracker.app.sync.SyncState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The dedicated Account screen: the sync status plus the sign-in form when no session is stored, or
 * Sync now / Sign out when one is. Stateless beyond the two credential fields; every action is
 * reported through a callback and no rule lives here.
 */
@Composable
fun AccountScreen(
    syncState: SyncState,
    onSignIn: (String, String) -> Unit,
    onSignOut: () -> Unit,
    onSync: () -> Unit,
    onClose: () -> Unit,
    error: String? = null,
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Account", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = syncState.describe(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        error?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        if (syncState is SyncState.NotConfigured) {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { onSignIn(username, password) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Sign in")
            }
        } else {
            Button(
                onClick = onSync,
                enabled = syncState !is SyncState.Syncing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Sync now")
            }
            OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                Text("Sign out")
            }
        }

        OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text("Close")
        }
    }
}

/** Human-readable status of the last sync attempt, shown on the Account screen. */
internal fun SyncState.describe(): String =
    when (this) {
        is SyncState.Idle -> lastSyncedAtMillis?.let { "Last sync ${formatTime(it)}" } ?: "Not synced yet"
        SyncState.Syncing -> "Syncing…"
        SyncState.NotConfigured -> "Not signed in"
        is SyncState.Error -> "Sync error: $message"
    }

private fun formatTime(millis: Long): String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(millis))
