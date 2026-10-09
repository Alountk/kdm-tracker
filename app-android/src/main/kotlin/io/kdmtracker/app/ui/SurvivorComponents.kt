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
    trailing: @Composable () -> Unit = {},
    expandedExtra: @Composable () -> Unit = {},
) {
    Surface(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth().alpha(if (muted) 0.55f else 1f),
        color =
            if (expanded) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
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
                SurvivorSheet(survivor = survivor, survivalLimit = survivalLimit)
                expandedExtra()
                OutlinedButton(onClick = onEdit) { Text("Edit sheet") }
            }
        }
    }
}

/** The read-only expanded sheet content. */
@Composable
fun SurvivorSheet(
    survivor: Survivor,
    survivalLimit: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text =
                "Survival ${survivor.survival}/$survivalLimit · Insanity ${survivor.insanity} · " +
                    "Hunt XP ${survivor.huntXp} · Courage ${survivor.courage} · Understanding ${survivor.understanding}",
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text =
                "MOV ${survivor.attributes.movement} · ACC ${survivor.attributes.accuracy} · " +
                    "STR ${survivor.attributes.strength} · EVA ${survivor.attributes.evasion} · " +
                    "LUCK ${survivor.attributes.luck} · SPD ${survivor.attributes.speed}",
            style = MaterialTheme.typography.bodySmall,
        )
        val wounds =
            HitLocation.entries.mapNotNull { location ->
                val wound = survivor.wounds[location] ?: return@mapNotNull null
                when {
                    wound.heavy -> "${location.name} grave"
                    wound.light -> "${location.name} firme"
                    else -> null
                }
            }
        if (wounds.isNotEmpty()) {
            Text(text = "Wounds: ${wounds.joinToString()}", style = MaterialTheme.typography.bodySmall)
        }
        if (survivor.tokens.isNotEmpty()) {
            Text(text = "Tokens: ${survivor.tokens.joinToString()}", style = MaterialTheme.typography.bodySmall)
        }
        if (survivor.gear.isNotEmpty()) {
            Text(text = "Gear: ${survivor.gear.joinToString()}", style = MaterialTheme.typography.bodySmall)
        }
        if (survivor.disorders.isNotEmpty() || survivor.fightingArts.isNotEmpty()) {
            Text(
                text =
                    "Disorders: ${survivor.disorders.ifEmpty { listOf("—") }.joinToString()} · " +
                        "Fighting arts: ${survivor.fightingArts.ifEmpty { listOf("—") }.joinToString()}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (survivor.notes.isNotBlank()) {
            Text(text = survivor.notes, style = MaterialTheme.typography.bodySmall)
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
