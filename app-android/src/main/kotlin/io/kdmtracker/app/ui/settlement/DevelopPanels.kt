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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kdmtracker.app.ui.theme.KdmSymbols
import io.kdmtracker.app.ui.theme.KdmType

/**
 * The Develop step's Innovations panel. `Innovar` is drawn and disabled: the design's flow draws
 * several cards and keeps one, and that flow is not modelled — the Develop body below is where an
 * innovation is actually unlocked.
 */
@Composable
fun InnovationsPanel(model: InnovationsModel) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        PanelTitle(icon = "auto_stories", title = "Innovaciones", trailing = "Mazo: ${model.deckSize} cartas")
        model.principleSlots.forEach { principle ->
            SlotRow(icon = "verified", title = principle, subtitle = "Activo Permanente")
        }
        repeat(model.emptySlots) { index ->
            SlotRow(
                icon = "crop_square",
                title =
                    if (index ==
                        0
                    ) {
                        "Por Descubrir"
                    } else {
                        "Futuro"
                    },
                subtitle = "Sin asignar",
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = model.drawLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(onClick = {}, enabled = false) {
                Text(text = "Innovar", style = KdmType.labelCaps)
            }
        }
    }
}

/** The Develop step's Settlement Locations panel. */
@Composable
fun LocationsPanel(
    model: LocationsModel,
    onBuild: (String) -> Unit,
    onAction: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        PanelTitle(
            icon = "domain",
            title = "Localizaciones de Asentamiento",
            trailing = "${model.built.size + model.buildable.size} Opciones",
        )
        model.built.forEach { location ->
            LocationRow(
                entry = location,
                heading = "Fundacional",
                state = "Construido",
                onAction = onAction,
                actionEnabled = false,
            )
        }
        model.buildable.forEach { location ->
            LocationRow(
                entry = location,
                heading = "Por Erigir",
                state = "No construido",
                onAction = onBuild,
                actionEnabled = true,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Symbol("warning", MaterialTheme.colorScheme.secondary, 16.sp)
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Regla de Año Linterna 1 · Sáltate el Paso 9",
                style = KdmType.labelCaps,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PanelTitle(
    icon: String,
    title: String,
    trailing: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Symbol(icon, MaterialTheme.colorScheme.secondary, 16.sp)
            Spacer(Modifier.width(6.dp))
            Text(
                text = title.uppercase(),
                style = KdmType.labelCaps,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = trailing,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SlotRow(
    icon: String,
    title: String,
    subtitle: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.small)
                .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Symbol(icon, MaterialTheme.colorScheme.secondary, 16.sp)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LocationRow(
    entry: LocationEntry,
    heading: String,
    state: String,
    onAction: (String) -> Unit,
    actionEnabled: Boolean,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.small)
                .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = heading.uppercase(), style = KdmType.labelCaps, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = entry.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = state,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (entry.description.isNotBlank()) {
            Text(
                text = "Requiere: ${entry.description}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${entry.endeavors} Esfuerzo",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            entry.buttonLabel?.let { label ->
                OutlinedButton(onClick = { onAction(entry.name) }, enabled = actionEnabled) {
                    Text(text = label, style = KdmType.labelCaps)
                }
            }
        }
    }
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
