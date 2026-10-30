package io.kdmtracker.app.ui.showdown

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.kdmtracker.app.ui.components.Symbol
import io.kdmtracker.app.ui.theme.KdmType

private val STAT_LABELS = listOf("Mov", "Res", "Vel", "Daño")

/**
 * The encounter banner: the status chip, the monster card with its level badge and blind-spot
 * widget, the stat strip and the `Vida / Mazo IA` track with its wound buttons.
 *
 * The monster's Movement, Toughness, Speed and Damage are not stored anywhere, so the strip draws
 * the design's labels over a dash rather than invent numbers.
 */
@Composable
internal fun ShowdownHeader(
    model: ShowdownBoardModel,
    onWound: () -> Unit,
    onHeal: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatusChip()
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MonsterRow(model)
            StatStrip()
            HealthTrack(model, onWound, onHeal)
        }
    }
}

@Composable
private fun StatusChip() {
    Row(
        modifier =
            Modifier
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary))
        Text(
            text = "ENFRENTAMIENTO EN CURSO",
            style = KdmType.labelCaps,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun MonsterRow(model: ShowdownBoardModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Symbol("pets", MaterialTheme.colorScheme.secondary)
        }
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = model.monster,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Text(
                text = "NV. ${model.level}",
                style = KdmType.labelCaps,
                color = MaterialTheme.colorScheme.secondary,
                modifier =
                    Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        BlindSpot()
    }
}

/** The design's 4x4 grid with the 2x2 monster base facing right; the blind-spot cells are gold. */
@Composable
private fun BlindSpot() {
    Column(
        modifier =
            Modifier
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "PUNTO CIEGO",
            style = KdmType.labelCaps,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(
            modifier =
                Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            BLIND_SPOT_GRID.forEach { row ->
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    row.forEach { cell ->
                        Box(Modifier.weight(1f).fillMaxHeight().background(cell.color()))
                    }
                }
            }
        }
    }
}

private enum class Cell { EMPTY, BASE, BLIND }

private val BLIND_SPOT_GRID =
    listOf(
        listOf(Cell.EMPTY, Cell.EMPTY, Cell.EMPTY, Cell.EMPTY),
        listOf(Cell.BLIND, Cell.BASE, Cell.BASE, Cell.EMPTY),
        listOf(Cell.BLIND, Cell.BASE, Cell.BASE, Cell.EMPTY),
        listOf(Cell.EMPTY, Cell.EMPTY, Cell.EMPTY, Cell.EMPTY),
    )

@Composable
private fun Cell.color() =
    when (this) {
        Cell.EMPTY -> MaterialTheme.colorScheme.surfaceContainerHigh
        Cell.BASE -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        Cell.BLIND -> MaterialTheme.colorScheme.secondary
    }

@Composable
private fun StatStrip() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .padding(8.dp),
    ) {
        STAT_LABELS.forEach { label ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = label.uppercase(),
                    style = KdmType.labelCaps,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "—",
                    style = KdmType.statDisplay,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HealthTrack(
    model: ShowdownBoardModel,
    onWound: () -> Unit,
    onHeal: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = "VIDA / MAZO IA",
                style = KdmType.labelCaps,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = model.health.toString(),
                    style = KdmType.statDisplay,
                    color =
                        if (model.health == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                )
                Text(
                    text = "/ ${model.deckSize} cartas",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }
        Row(
            modifier =
                Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            WoundButton("remove", "Curar herida", MaterialTheme.colorScheme.surfaceContainerHighest, onHeal)
            Column(Modifier.widthIn(min = 50.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "HERIDAS", style = KdmType.labelCaps, color = MaterialTheme.colorScheme.error)
                Text(
                    text = model.wounds.toString(),
                    style = KdmType.statDisplay,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            WoundButton("add", "Herir al monstruo", MaterialTheme.colorScheme.errorContainer, onWound)
        }
    }
}

@Composable
private fun WoundButton(
    glyph: String,
    description: String,
    container: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(32.dp)
                .clip(MaterialTheme.shapes.small)
                .background(container)
                .semantics {
                    contentDescription = description
                    role = Role.Button
                }.clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Symbol(glyph, MaterialTheme.colorScheme.onSurface)
    }
}
