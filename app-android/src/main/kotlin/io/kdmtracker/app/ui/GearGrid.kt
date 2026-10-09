package io.kdmtracker.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.kdmtracker.core.usecase.AssignGear

/**
 * The 3×3 gear grid for a departing survivor (Settlement Phase step 8): nine uniform cells, one item
 * per slot, assigned from the settlement's crafted gear. This is the *configuration* view — no armor
 * derivation; the playable, card-faithful grid belongs to the Showdown.
 */
@Composable
fun GearGrid(
    assigned: List<String>,
    settlementGear: List<String>,
    onAssign: (String) -> Unit,
    onUnassign: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Gear grid (${assigned.size}/${AssignGear.MAX_GEAR_SLOTS})",
            style = MaterialTheme.typography.titleSmall,
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (row in 0 until ROWS) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    for (col in 0 until COLUMNS) {
                        val name = assigned.getOrNull(row * COLUMNS + col)
                        GearCell(
                            name = name,
                            onRemove = name?.let { gear -> { onUnassign(gear) } },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        val available = settlementGear.filter { it !in assigned }
        if (available.isEmpty()) {
            Text(
                text = "No crafted gear left to assign.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            available.forEach { gear ->
                OutlinedButton(
                    onClick = { onAssign(gear) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Assign $gear")
                }
            }
        }
    }
}

@Composable
private fun GearCell(
    name: String?,
    onRemove: (() -> Unit)?,
    modifier: Modifier,
) {
    Surface(
        modifier = modifier.height(CELL_HEIGHT),
        color =
            if (name == null) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            },
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = MaterialTheme.shapes.small,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (name == null) {
                Text(
                    text = "empty",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.weight(1f))
                onRemove?.let { remove ->
                    TextButton(onClick = remove) { Text("Remove", style = MaterialTheme.typography.labelSmall) }
                }
            }
        }
    }
}

private const val ROWS = 3
private const val COLUMNS = 3
private val CELL_HEIGHT = 84.dp
