package io.kdmtracker.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.kdmtracker.app.UiState
import io.kdmtracker.core.domain.ResourceDefinition
import io.kdmtracker.core.domain.ResourceEntry
import io.kdmtracker.core.domain.StorageLine

/**
 * The Step 7 "Develop" body: the resource ledger, inside the cockpit's chrome. The other two
 * flows of this step — the fabricar section and the innovar offer — live in the settlement
 * screen's panels above it, so there is no tab to switch any more; every action is reported
 * through a callback and every list comes from [state].
 */
@Composable
internal fun DevelopStepScreen(
    state: UiState,
    onAddResource: (ResourceEntry) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        state.actionError?.let { reason ->
            Text(
                text = reason,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Text(
            text = "Endeavors ${state.endeavors} · ${storageText(state.storageLines, state.resourceDefinitions)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        RecursosTab(state = state, onAddResource = onAddResource)
    }
}

/** A small selectable chip, shared by the Develop tabs. */
@Composable
internal fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color =
            if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        contentColor =
            if (selected) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

private fun storageText(
    lines: List<StorageLine>,
    definitions: List<ResourceDefinition>,
): String {
    if (lines.isEmpty()) return "Storage: empty"
    val byId = definitions.associateBy { it.id }
    return "Storage: " + lines.joinToString(" · ") { "${byId[it.id]?.names?.es ?: it.id.value} ${it.quantity}" }
}
