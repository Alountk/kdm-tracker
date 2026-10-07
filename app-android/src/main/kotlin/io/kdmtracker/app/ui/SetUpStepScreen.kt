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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.kdmtracker.app.UiState

/**
 * Read-only Step 1 "Set Up the Settlement" body.
 *
 * Renders what already exists when the settlement opens: the principles in play, the derived
 * Innovation Deck (the consequences the unlocked innovations add), the locations already built and
 * this lantern year's timeline/event. A single confirm action reports the advance upward. Stateless:
 * all content comes from [state] and the move is reported through [onConfirm]; no rules live here.
 */
@Composable
fun SetUpStepScreen(
    state: UiState,
    onConfirm: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = state.stepTitle, style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "Lantern Year ${state.lanternYear} · Population ${state.population}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SetUpSection(title = "Principles", entries = state.principles)
        SetUpSection(title = "Innovation Deck", entries = state.innovationDeck)
        SetUpSection(title = "Built Locations", entries = state.builtLocations)
        SetUpSection(title = "This Year", entries = state.yearEvents)
        Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) {
            Text("Confirm Setup")
        }
    }
}

@Composable
private fun SetUpSection(
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
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            if (entries.isEmpty()) {
                Text(text = "None", style = MaterialTheme.typography.bodySmall)
            } else {
                entries.forEach { entry ->
                    Text(text = entry, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
