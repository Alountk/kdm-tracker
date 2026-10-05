package io.kdmtracker.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.kdmtracker.app.UiState

/**
 * Stateless Hunt Phase stub.
 *
 * The Hunt Phase has no screens yet, so this renders a prominent "Pendiente" marker and the
 * resolve/fail controls; the real hunt flow arrives in a later plan. No state and no rules live here.
 */
@Composable
fun HuntScreen(
    state: UiState,
    onAdvance: () -> Unit,
    onFail: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = "Hunt", style = MaterialTheme.typography.headlineMedium)
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(onClick = onFail, modifier = Modifier.weight(1f)) {
                Text("Fail Hunt")
            }
            Button(onClick = onAdvance, modifier = Modifier.weight(1f)) {
                Text("Advance")
            }
        }
    }
}
