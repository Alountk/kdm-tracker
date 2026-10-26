package io.kdmtracker.app.ui.hunt

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.kdmtracker.core.domain.HuntEvent
import io.kdmtracker.core.domain.HuntSpace
import io.kdmtracker.core.domain.HuntSpaceKind

/**
 * The selected hunt space's panel: its kind markers, its placed events (with their text and a
 * `mark resolved` control), a collapsible picker to place a new event, and the advance control.
 */
@Composable
fun HuntEventPanel(
    space: HuntSpace,
    index: Int,
    huntEvents: List<HuntEvent>,
    onAssign: (String) -> Unit,
    onResolve: (String) -> Unit,
    onMarkKind: (HuntSpaceKind) -> Unit,
    onAdvance: () -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Space $index · ${space.kind.name.lowercase().replace('_', ' ')}",
            style = MaterialTheme.typography.titleSmall,
        )

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            OutlinedButton(onClick = { onMarkKind(HuntSpaceKind.NORMAL) }) { Text("Normal") }
            OutlinedButton(onClick = { onMarkKind(HuntSpaceKind.MONSTER_E) }) { Text("E") }
            OutlinedButton(onClick = { onMarkKind(HuntSpaceKind.OVERWHELMING_DARKNESS) }) { Text("◎ OD") }
            OutlinedButton(onClick = { onMarkKind(HuntSpaceKind.STARVATION) }) { Text("☠ Starvation") }
            OutlinedButton(onClick = { onMarkKind(HuntSpaceKind.MONSTER) }) { Text("◆ Monster") }
        }

        if (space.events.isEmpty()) {
            Text(
                text = "No events placed on this space.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        space.events.forEach { name ->
            val resolved = name in space.resolved
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row {
                        Text(text = name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        if (resolved) {
                            Text(text = "done", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    huntEvents.firstOrNull { it.name == name }?.let { event ->
                        Text(text = event.text, style = MaterialTheme.typography.bodySmall)
                    }
                    if (!resolved) {
                        OutlinedButton(onClick = { onResolve(name) }) { Text("Mark resolved") }
                    }
                }
            }
        }

        OutlinedButton(onClick = { showPicker = !showPicker }, modifier = Modifier.fillMaxWidth()) {
            Text(if (showPicker) "Hide events" else "Place a hunt event")
        }
        if (showPicker) {
            huntEvents
                .filter { it.name !in space.events }
                .forEach { event ->
                    OutlinedButton(
                        onClick = {
                            onAssign(event.name)
                            showPicker = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(event.name)
                    }
                }
        }

        Button(onClick = onAdvance, modifier = Modifier.fillMaxWidth()) { Text("Advance ▸") }
    }
}
