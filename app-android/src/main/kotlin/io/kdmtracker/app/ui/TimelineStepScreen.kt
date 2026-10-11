package io.kdmtracker.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.kdmtracker.app.UiState

/**
 * Step 4 "Update Timeline" body.
 *
 * Shows the current lantern year, lets the table record the Settlement Event drawn this year
 * (from the ingested deck, or a random draw), and lists this year's story events read-only.
 * Stateless: content comes from [state], every choice is reported through the callbacks. The
 * lantern-year increment itself is applied by the controller when the step is left.
 */
@Composable
fun TimelineStepScreen(
    state: UiState,
    onRecordEvent: (String) -> Unit,
    onDrawEvent: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(text = state.stepTitle, style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "Lantern Year ${state.lanternYear}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(onClick = onDrawEvent, modifier = Modifier.fillMaxWidth()) {
            Text("Draw a settlement event")
        }

        state.settlementEvent?.let { name ->
            SettlementEventCard(
                name = name,
                text =
                    state.settlementEventDeck
                        .firstOrNull { it.name == name }
                        ?.text
                        .orEmpty(),
            )
        }

        Text(text = "Settlement Events", style = MaterialTheme.typography.titleSmall)
        state.settlementEventDeck.forEach { event ->
            OutlinedButton(onClick = { onRecordEvent(event.name) }, modifier = Modifier.fillMaxWidth()) {
                Text(event.name)
            }
        }

        TimelineSection(title = "This Year", entries = state.yearEvents)
    }
}

@Composable
private fun SettlementEventCard(
    name: String,
    text: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = name, style = MaterialTheme.typography.titleSmall)
            Text(text = text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun TimelineSection(
    title: String,
    entries: List<String>,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            if (entries.isEmpty()) {
                Text(text = "None", style = MaterialTheme.typography.bodySmall)
            } else {
                entries.forEach { entry -> Text(text = entry, style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}
