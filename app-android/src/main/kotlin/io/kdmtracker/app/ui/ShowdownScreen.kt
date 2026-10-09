package io.kdmtracker.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.kdmtracker.app.UiState
import io.kdmtracker.core.domain.ShowdownStatus
import io.kdmtracker.core.domain.Ulid

/**
 * Stateless Showdown Phase body.
 *
 * Before setup it offers a monster/level form; afterwards it tracks the encounter and records the
 * outcome plus the aftermath. No state and no rules live here: everything is reported through the
 * callbacks.
 */
@Composable
fun ShowdownScreen(
    state: UiState,
    onSetUpShowdown: (String, Int, List<String>) -> Unit,
    onDrawShowdownCard: () -> Unit,
    onWoundMonster: () -> Unit,
    onGrantHuntXp: (List<Ulid>) -> Unit,
    onRecordDeaths: (Int) -> Unit,
    onFinishShowdown: (ShowdownStatus) -> Unit,
) {
    val showdown = state.showdown
    if (showdown == null || showdown.monster.isBlank()) {
        EncounterSetup(state = state, onSetUpShowdown = onSetUpShowdown)
        return
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = state.settlementName, style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "Showdown · ${showdown.monster} · Level ${showdown.level}",
            style = MaterialTheme.typography.titleMedium,
        )
        state.actionError?.let { message ->
            Text(text = message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }

        EncounterPanel(
            state = state,
            onDrawShowdownCard = onDrawShowdownCard,
            onWoundMonster = onWoundMonster,
            onGrantHuntXp = onGrantHuntXp,
            onRecordDeaths = onRecordDeaths,
            outcome = {
                Text(text = "Outcome", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onFinishShowdown(ShowdownStatus.VICTORY) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Victory")
                    }
                    OutlinedButton(
                        onClick = { onFinishShowdown(ShowdownStatus.DEFEAT) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Defeat")
                    }
                }
            },
        )
    }
}
