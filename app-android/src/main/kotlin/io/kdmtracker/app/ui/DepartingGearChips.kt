package io.kdmtracker.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.kdmtracker.app.ui.theme.KdmType
import io.kdmtracker.core.domain.Survivor
import io.kdmtracker.core.domain.Ulid

/** One carried-gear chip: the design's uniform secondary dot plus the item name. */
@Composable
internal fun GearStripChip(item: String) {
    Row(
        modifier =
            Modifier
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.small)
                .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(6.dp)
                    .background(MaterialTheme.colorScheme.secondary, CircleShape),
        )
        Text(
            text = item,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * The `grid_view` expansion: the gear-assign pattern reused from the sheet, kept to a wrap of chips
 * — the carried items (checked, tap to unassign) followed by the settlement's stock (tap to
 * assign), both wired to the existing callbacks.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GearChipEditor(
    survivor: Survivor,
    settlementGear: List<String>,
    onAssignGear: (Ulid, String) -> Unit,
    onUnassignGear: (Ulid, String) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest, MaterialTheme.shapes.small)
                .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = PrepareDepartingStepCopy.GEAR_BUTTON.uppercase(),
                style = KdmType.labelCaps,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                text = gearGridLabel(survivor.gear),
                style = KdmType.labelCaps,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            survivor.gear.forEach { item ->
                GearEditChip(
                    item = item,
                    assigned = true,
                    onClick = { onUnassignGear(survivor.id, item) },
                )
            }
            settlementGear.filter { it !in survivor.gear }.forEach { item ->
                GearEditChip(
                    item = item,
                    assigned = false,
                    onClick = { onAssignGear(survivor.id, item) },
                )
            }
        }
    }
}

/** One assignable gear chip: filled check while assigned (tap unassigns), blank check otherwise. */
@Composable
internal fun GearEditChip(
    item: String,
    assigned: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .background(
                    if (assigned) {
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    MaterialTheme.shapes.small,
                ).clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Symbol(
            if (assigned) "check_box" else "check_box_outline_blank",
            MaterialTheme.colorScheme.secondary,
            14,
        )
        Text(
            text = item,
            style = MaterialTheme.typography.bodySmall,
            color = if (assigned) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
        )
    }
}
