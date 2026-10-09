package io.kdmtracker.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.kdmtracker.core.domain.HitLocation
import io.kdmtracker.core.domain.Survivor
import io.kdmtracker.core.domain.Ulid

/** The coloured status dot for a survivor. */
@Composable
fun StatusDot(
    tone: SurvivorTone,
    modifier: Modifier = Modifier,
) {
    val color =
        when (tone) {
            SurvivorTone.ALIVE -> Color(0xFF137333)
            SurvivorTone.DEPARTING -> Color(0xFF1A73E8)
            SurvivorTone.DEAD -> Color(0xFF9AA0A6)
            SurvivorTone.INJURED -> Color(0xFFB06000)
            SurvivorTone.UNNAMED -> Color(0xFF9AA0A6)
        }
    Box(modifier = modifier.size(10.dp).background(color, CircleShape))
}

/** The filter chips over the roster. */
@Composable
fun RosterFilterChips(
    selected: SurvivorFilter,
    onSelect: (SurvivorFilter) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SurvivorFilter.entries.forEach { filter ->
            ChoiceChip(label = filter.label(), selected = filter == selected) { onSelect(filter) }
        }
    }
}

private fun SurvivorFilter.label(): String =
    when (this) {
        SurvivorFilter.ALL -> "All"
        SurvivorFilter.INJURED -> "Injured"
        SurvivorFilter.DEPARTING -> "Departing"
        SurvivorFilter.UNNAMED -> "Unnamed"
        SurvivorFilter.DEAD -> "Dead"
    }

/**
 * A survivor card that collapses to one scannable line and expands in place to the sheet.
 *
 * Stateless: [expanded] is owned by the caller, the header toggles it, [trailing] carries the
 * step's contextual control and [onEdit] opens the full-screen editor.
 */
@Composable
fun SurvivorCard(
    survivor: Survivor,
    survivalLimit: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    muted: Boolean = false,
    huntXpTrackMax: Int = 16,
    insanityThreshold: Int = 3,
    settlementGear: List<String> = emptyList(),
    onSave: (Survivor) -> Unit,
    onAssignGear: (Ulid, String) -> Unit = { _, _ -> },
    onUnassignGear: (Ulid, String) -> Unit = { _, _ -> },
    trailing: @Composable () -> Unit = {},
    expandedExtra: @Composable () -> Unit = {},
) {
    Surface(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth().alpha(if (muted) 0.55f else 1f),
        color =
            if (expanded) {
                MaterialTheme.colorScheme.surfaceContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(survivorTone(survivor))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = survivor.name.ifBlank { "Unnamed" },
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${survivor.sex.name.take(1)} · ${survivor.status.name}",
                    style = MaterialTheme.typography.bodySmall,
                )
                trailing()
            }
            Text(text = survivor.headerSummary(), style = MaterialTheme.typography.bodySmall)
            if (expanded) {
                SurvivorSheet(
                    survivor = survivor,
                    survivalLimit = survivalLimit,
                    huntXpTrackMax = huntXpTrackMax,
                    insanityThreshold = insanityThreshold,
                    settlementGear = settlementGear,
                    onSave = onSave,
                    onAssignGear = { onAssignGear(survivor.id, it) },
                    onUnassignGear = { onUnassignGear(survivor.id, it) },
                    onEditSheet = onEdit,
                )
                expandedExtra()
                OutlinedButton(onClick = onEdit) { Text("Edit sheet") }
            }
        }
    }
}

private fun Survivor.headerSummary(): String {
    val base = "MOV ${attributes.movement} · STR ${attributes.strength} · Hunt XP $huntXp"
    val marks =
        HitLocation.entries.count { location ->
            val wound = wounds[location]
            wound != null && (wound.light || wound.heavy)
        }
    val token = if (tokens.isEmpty()) "" else " · ${tokens.size} token(s)"
    return base + if (marks == 0) token else " · $marks wound(s)$token"
}
