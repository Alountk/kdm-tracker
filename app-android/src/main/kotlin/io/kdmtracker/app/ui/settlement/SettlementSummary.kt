package io.kdmtracker.app.ui.settlement

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kdmtracker.app.ui.theme.KdmSymbols
import io.kdmtracker.app.ui.theme.KdmType

/**
 * The design's settlement summary — the *monolito superior*: the active phase and survival limit,
 * the year and its narrative slot, the available endeavors and the four basic resources.
 */
@Composable
fun SettlementSummary(
    model: SettlementSummaryModel,
    year: Int,
    phaseLabel: String,
) {
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
                ResourceCell(resource = resource, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ResourceCell(
    resource: SummaryResource,
    modifier: Modifier = Modifier,
) {
    val colour = resource.colour.tint()
    Column(
        modifier =
            modifier
                .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.small)
                .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = resource.label.uppercase(),
            style = KdmType.labelCaps,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
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
