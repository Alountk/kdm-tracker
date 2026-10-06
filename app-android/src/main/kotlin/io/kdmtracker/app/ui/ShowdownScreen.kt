package io.kdmtracker.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.kdmtracker.app.UiState

/**
 * Stateless Showdown Phase stub.
 *
 * The Showdown Phase has no screens yet, so this renders a prominent "Pendiente" marker and the
 * advance control; the real showdown flow arrives in a later plan. No state and no rules live here.
 */
@Composable
fun ShowdownScreen(
    state: UiState,
    onAdvance: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = state.settlementName, style = MaterialTheme.typography.headlineMedium)
        Text(text = "Showdown", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Lantern Year ${state.lanternYear}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            PendientePlaceholder(modifier = Modifier.fillMaxWidth())
        }
        Button(onClick = onAdvance, modifier = Modifier.fillMaxWidth()) {
            Text("Advance")
        }
    }
}
