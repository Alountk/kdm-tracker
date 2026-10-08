package io.kdmtracker.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.kdmtracker.app.UiState

/**
 * Step 11 "End Settlement Phase" body.
 *
 * The gated commit warning: it names how many unspent endeavors proceed will discard, plus a
 * summary. The discard itself is done by `:core` when the step advances (the commit transition),
 * so this body only warns. Stateless.
 */
@Composable
fun EndPhaseStepScreen(state: UiState) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(text = state.stepTitle, style = MaterialTheme.typography.headlineMedium)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Text(
                    text = "${state.endeavors} unspent endeavors will be discarded when you proceed.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
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
                Text(text = "Summary", style = MaterialTheme.typography.titleSmall)
                Text(text = "Lantern Year ${state.lanternYear}", style = MaterialTheme.typography.bodyMedium)
                Text(text = "Population ${state.population}", style = MaterialTheme.typography.bodyMedium)
                Text(text = "Storage lines ${state.storageLines.size}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
