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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kdmtracker.app.ui.settlement.AdjustResult
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
    var editing by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf(0) }
    var saved by remember { mutableStateOf<AdjustResult?>(null) }

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
                val isSelected = editing == resource.name
                ResourceCell(
                    resource = resource,
                    selected = isSelected,
                    pending = if (isSelected) pending else resource.quantity,
                    onSelect = {
                        editing = resource.name
                        pending = resource.quantity
                        saved = null
                    },
                    onBump = { pending = (pending + it).coerceAtLeast(0) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Resolved once: the edited name can go stale if the model changes under the editor,
        // and a missing resource must degrade to the hint, never throw mid-composition.
        val target = editing?.let { name -> model.resources.firstOrNull { it.name == name } }
        if (target == null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Symbol("touch_app", MaterialTheme.colorScheme.onSurfaceVariant, 14.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Mantén pulsada una celda para ajustarla.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (pending <= 0) {
                    Text(
                        text = "Mínimo alcanzado — el almacén no admite negativos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = InnovarPalette.red,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Text(
                        text = "previo: ${target.quantity}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                }
                KdmActionButton(
                    text = "Guardar ajuste",
                    onClick = {
                        val delta = pending - target.quantity
                        if (delta != 0) onAdjust(target.id, delta)
                        saved = AdjustResult(target.label, target.quantity, pending)
                        editing = null
                    },
                    icon = "check_circle",
                    variant = KdmActionVariant.GOLD,
                )
            }
        }

        saved?.let { result ->
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
                    text = result.summary,
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
        }
    }
}

@Composable
private fun ResourceCell(
    resource: SummaryResource,
    selected: Boolean,
    pending: Int,
    onSelect: () -> Unit,
    onBump: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.small
    val colour = if (selected) MaterialTheme.colorScheme.secondary else resource.colour.tint()
    Column(
        modifier =
            modifier
                .background(MaterialTheme.colorScheme.surfaceContainer, shape)
                .border(
                    1.dp,
                    if (selected) MaterialTheme.colorScheme.secondary else Color.Transparent,
                    shape,
                ).clickable(onClick = onSelect)
                .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = resource.label.uppercase(),
            style = KdmType.labelCaps,
            color = colour,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
        if (selected) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                StepperIcon(name = "remove", enabled = pending > 0, onClick = { onBump(-1) })
                Text(
                    text = pending.toString(),
                    style = KdmType.labelCaps,
                    fontWeight = FontWeight.Bold,
                    color = colour,
                )
                StepperIcon(name = "add", enabled = true, onClick = { onBump(1) })
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Symbol(resource.colour.icon, colour, 13.sp)
                Spacer(Modifier.width(4.dp))
                Text(
                    text = resource.quantity.toString(),
                    style = KdmType.labelCaps,
                    fontWeight = FontWeight.Bold,
                    color = colour,
                )
            }
        }
    }
}

@Composable
private fun StepperIcon(
    name: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val shape = MaterialTheme.shapes.small
    Box(
        modifier =
            Modifier
                .size(24.dp)
                .background(
                    if (enabled) {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    shape,
                ).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Symbol(
            name,
            if (enabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
            16.sp,
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
