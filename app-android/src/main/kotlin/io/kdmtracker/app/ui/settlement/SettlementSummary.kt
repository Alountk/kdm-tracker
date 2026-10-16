package io.kdmtracker.app.ui.settlement

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kdmtracker.app.ui.theme.InnovarPalette
import io.kdmtracker.app.ui.theme.KdmActionButton
import io.kdmtracker.app.ui.theme.KdmActionVariant
import io.kdmtracker.app.ui.theme.KdmSymbols
import io.kdmtracker.app.ui.theme.KdmType
import io.kdmtracker.core.domain.ResourceId

/**
 * The design's settlement summary — the *monolito superior*: the active phase and survival limit,
 * the year and its narrative slot, the available endeavors and the four basic resources.
 */
@Composable
fun SettlementSummary(
    model: SettlementSummaryModel,
    year: Int,
    phaseLabel: String,
    onAdjust: (ResourceId, Int) -> Unit = { _, _ -> },
) {
    // One pure flow owns selection, the pending count and the last confirmation; the composable
    // only renders its four design states (rest, editing, minimum, confirmed).
    var flow by remember { mutableStateOf(ResourceAdjustFlow()) }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Symbol("local_fire_department", MaterialTheme.colorScheme.secondary, 14.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = phaseLabel.uppercase(),
                    style = KdmType.labelCaps,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Row(
                modifier =
                    Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.small)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Límite Supervivencia".uppercase(),
                    style = KdmType.labelCaps,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = model.survivalLimit.toString(),
                    style = KdmType.labelCaps,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column {
                Text(
                    text = "Año Linterna $year",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    // The design's year story is not modelled: the slot renders empty rather than
                    // showing invented narrative.
                    text = "—",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Esfuerzos Disponibles".uppercase(),
                    style = KdmType.labelCaps,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Symbol("emergency", MaterialTheme.colorScheme.secondary, 18.sp)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = model.endeavors.toString(),
                        style = KdmType.statDisplay,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "/ ${model.maxEndeavors}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            model.resources.forEach { resource ->
                val state = flow.cellState(resource)
                val open = flow.edit?.takeIf { it.id == resource.id }
                ResourceCell(
                    resource = resource,
                    state = state,
                    pending = open?.pending ?: resource.quantity,
                    original = open?.original ?: resource.quantity,
                    onSelect = { flow = flow.select(resource) },
                    onBump = { flow = flow.bump(it) },
                    modifier =
                        Modifier
                            .weight(1f)
                            // The sheet dims the cells the editor is not touching (opacity-60).
                            .alpha(if (flow.isEditing && state == ResourceAdjustFlow.CellState.REST) 0.6f else 1f),
                )
            }
        }

        // Footer precedence from the sheet: an open cell shows the save action (plus the
        // minimum caption when it bottoms out), a confirmed save shows its strip, and only an
        // untouched flow gets the hint.
        val openEdit = flow.edit
        val confirmed = flow.confirmed
        when {
            openEdit != null -> {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (openEdit.atMinimum) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .background(InnovarPalette.redBg, MaterialTheme.shapes.small)
                                    .border(1.dp, InnovarPalette.red.copy(alpha = 0.2f), MaterialTheme.shapes.small)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Symbol("error", InnovarPalette.red, 14.sp)
                            Text(
                                text = "Mínimo alcanzado — el almacén no admite negativos.",
                                style = MaterialTheme.typography.bodySmall,
                                color = InnovarPalette.red,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    KdmActionButton(
                        text = "Guardar ajuste",
                        onClick = {
                            // A save with nothing to save never reaches the ledger or the strip.
                            if (flow.delta != 0) onAdjust(openEdit.id, flow.delta)
                            flow = flow.save()
                        },
                        icon = "check_circle",
                        variant = KdmActionVariant.GOLD,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            confirmed != null ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(InnovarPalette.greenWash, MaterialTheme.shapes.small)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Symbol("check_circle", InnovarPalette.green, 14.sp)
                    Text(
                        text = confirmed.result.summary,
                        style = KdmType.labelCaps,
                        color = InnovarPalette.green,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "Ajustado ahora",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

            else ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Symbol("touch_app", MaterialTheme.colorScheme.onSurfaceVariant, 14.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Toca una celda para ajustarla.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
        }
    }
}

@Composable
private fun ResourceCell(
    resource: SummaryResource,
    state: ResourceAdjustFlow.CellState,
    pending: Int,
    original: Int,
    onSelect: () -> Unit,
    onBump: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.small
    val restColour = resource.colour.tint()
    val editing = state == ResourceAdjustFlow.CellState.EDITING || state == ResourceAdjustFlow.CellState.MINIMUM
    // The open cell paints gold (red at zero); the confirmed cell only marks its icon green.
    val accent =
        when (state) {
            ResourceAdjustFlow.CellState.EDITING -> MaterialTheme.colorScheme.secondary
            ResourceAdjustFlow.CellState.MINIMUM -> InnovarPalette.red
            ResourceAdjustFlow.CellState.CONFIRMED -> InnovarPalette.green
            ResourceAdjustFlow.CellState.REST -> restColour
        }
    val (borderWidth, borderColor) =
        when (state) {
            ResourceAdjustFlow.CellState.EDITING -> 2.dp to MaterialTheme.colorScheme.secondary
            ResourceAdjustFlow.CellState.MINIMUM -> 2.dp to InnovarPalette.red.copy(alpha = 0.8f)
            ResourceAdjustFlow.CellState.CONFIRMED -> 1.dp to InnovarPalette.green.copy(alpha = 0.6f)
            ResourceAdjustFlow.CellState.REST -> 1.dp to MaterialTheme.colorScheme.outlineVariant
        }
    Box(
        modifier =
            modifier
                .background(
                    if (editing) {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    shape,
                ).border(borderWidth, borderColor, shape)
                .clickable(onClick = onSelect),
    ) {
        if (state == ResourceAdjustFlow.CellState.CONFIRMED) {
            // The sheet's dot marks the cell that just changed.
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 6.dp, end = 6.dp)
                        .size(6.dp)
                        .background(InnovarPalette.green, CircleShape),
            )
        }
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 74.dp)
                    .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            if (editing) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Symbol(resource.colour.icon, accent, 12.sp)
                    Text(
                        text = resource.label.uppercase(),
                        style = KdmType.labelCaps,
                        color = accent,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    StepperIcon(name = "remove", enabled = pending > 0, filled = false, onClick = { onBump(-1) })
                    Text(
                        text = pending.toString(),
                        style = KdmType.labelCaps,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                    )
                    StepperIcon(name = "add", enabled = true, filled = true, onClick = { onBump(1) })
                }
                Text(
                    text = if (pending <= 0) "agotado" else "previo: $original",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Symbol(
                    resource.colour.icon,
                    if (state == ResourceAdjustFlow.CellState.CONFIRMED) accent else restColour,
                    13.sp,
                )
                Text(
                    text = resource.label.uppercase(),
                    style = KdmType.labelCaps,
                    color = restColour,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = resource.quantity.toString(),
                    style = KdmType.labelCaps,
                    fontWeight = FontWeight.Bold,
                    color = restColour,
                )
            }
        }
    }
}

@Composable
private fun StepperIcon(
    name: String,
    enabled: Boolean,
    filled: Boolean,
    onClick: () -> Unit,
) {
    val shape = MaterialTheme.shapes.small
    Box(
        modifier =
            Modifier
                .size(20.dp)
                .background(
                    if (filled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape,
                ).then(
                    if (filled) {
                        Modifier
                    } else {
                        Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                    },
                ).alpha(if (enabled) 1f else 0.35f)
                .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Symbol(
            name,
            if (filled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
            14.sp,
        )
    }
}

@Composable
private fun ResourceColour.tint(): Color =
    when (this) {
        ResourceColour.BONE -> MaterialTheme.colorScheme.primary
        // The design's `tertiary-fixed-dim`; the Material scheme has no role carrying it.
        ResourceColour.CORAL -> Color(0xFFFFB4AC)
        ResourceColour.GOLD -> MaterialTheme.colorScheme.secondary
        ResourceColour.DIM -> MaterialTheme.colorScheme.outline
    }

@Composable
private fun Symbol(
    name: String,
    color: Color,
    size: androidx.compose.ui.unit.TextUnit,
) {
    Text(
        text = KdmSymbols.glyph(name),
        fontFamily = KdmSymbols.family,
        fontSize = size,
        color = color,
    )
}
