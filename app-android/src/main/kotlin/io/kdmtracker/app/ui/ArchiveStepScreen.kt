package io.kdmtracker.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.kdmtracker.app.UiState
import io.kdmtracker.core.domain.ResourceId

/**
 * Step 10 "Record & Archive Resources" body.
 *
 * Read-only projection of the settlement's storage (the resource ledger written during Develop).
 * A short note frames it as the archive/reconciliation checkpoint; there is nothing to decide
 * here. Stateless.
 */
@Composable
fun ArchiveStepScreen(state: UiState) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(text = state.stepTitle, style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "Write unspent resources into settlement storage and archive the physical cards.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
                Text(text = "Storage", style = MaterialTheme.typography.titleSmall)
                if (state.storageLines.isEmpty()) {
                    Text(text = "Empty", style = MaterialTheme.typography.bodyMedium)
                } else {
                    state.storageLines.forEach { line ->
                        Text(
                            text = "${state.resourceName(line.id)} × ${line.quantity}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

/**
 * The localized name of a storage line's resource, or its id when the catalog does not carry it.
 *
 * The ledger keeps ids only, so the display name is resolved here from the catalog rather than
 * being stored and going stale.
 */
private fun UiState.resourceName(id: ResourceId): String =
    resourceDefinitions.firstOrNull { it.id == id }?.names?.es ?: id.value
