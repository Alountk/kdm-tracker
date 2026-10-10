package io.kdmtracker.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.kdmtracker.app.UiState
import io.kdmtracker.core.domain.ResourceEntry

/**
 * The Recursos tab: every resource grouped by its source, with an "Todos" / per-source filter and a
 * `+1` per resource (the manual bridge that funds the ledger until hunt rewards are modelled).
 */
@Composable
internal fun RecursosTab(
    state: UiState,
    onAddResource: (ResourceEntry) -> Unit,
) {
    var selected by remember { mutableStateOf<String?>(null) }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ChoiceChip(label = "Todos", selected = selected == null) { selected = null }
            state.resourceGroups.keys.forEach { source ->
                ChoiceChip(label = source, selected = selected == source) { selected = source }
            }
        }

        val visible =
            if (selected == null) state.resourceGroups else state.resourceGroups.filterKeys { it == selected }
        visible.forEach { (source, definitions) ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = source, style = MaterialTheme.typography.titleSmall)
                definitions.forEach { definition ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = definition.names.es, style = MaterialTheme.typography.bodyMedium)
                        Button(
                            onClick = { onAddResource(ResourceEntry(definition.names.es, definition.category, 1)) },
                        ) {
                            Text("+1")
                        }
                    }
                }
            }
        }
    }
}
