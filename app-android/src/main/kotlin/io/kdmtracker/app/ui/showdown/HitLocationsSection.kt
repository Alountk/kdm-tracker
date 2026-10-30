package io.kdmtracker.app.ui.showdown

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.kdmtracker.app.ui.components.SheetPanel
import io.kdmtracker.app.ui.components.StoneButton
import io.kdmtracker.app.ui.components.Symbol
import io.kdmtracker.app.ui.theme.KdmActionButton
import io.kdmtracker.app.ui.theme.KdmActionVariant
import io.kdmtracker.app.ui.theme.KdmType

/**
 * The survivors' attack: a hits counter and the monster's hit-location deck.
 *
 * The encounter does not store which locations were revealed, so resolving one (a miss, a wound
 * or a critical wound) only removes it from this list until the deck is shuffled again; the wound
 * buttons also wound the monster through [onWound]. The hits counter is a scratch pad.
 */
@Composable
internal fun HitLocationsSection(
    locations: List<HitLocationRow>,
    onWound: () -> Unit,
) {
    var hits by rememberSaveable { mutableIntStateOf(0) }
    var open by rememberSaveable { mutableStateOf(false) }
    val resolved = rememberSaveable { mutableStateListOf<String>() }
    val pending = locations.filter { it.name !in resolved }

    SheetPanel(
        title = "Ataque de supervivientes y localizaciones",
        icon = "colorize",
        trailing = { DeckChip(pending.size, locations.size) },
    ) {
        HitsCounter(hits = hits, onChange = { hits = (hits + it).coerceAtLeast(0) })
        KdmActionButton(
            text = if (open) "Ocultar localizaciones" else "Ver localizaciones (${pending.size})",
            onClick = { open = !open },
            modifier = Modifier.fillMaxWidth(),
            icon = if (open) "expand_less" else "visibility",
            variant = KdmActionVariant.DARK,
        )
        if (open) {
            pending.forEachIndexed { index, location ->
                HitLocationCard(
                    number = index + 1,
                    location = location,
                    onMiss = { resolved.add(location.name) },
                    onWound = {
                        onWound()
                        resolved.add(location.name)
                    },
                )
            }
            if (resolved.isNotEmpty()) {
                KdmActionButton(
                    text = "Barajar el mazo (${resolved.size} resueltas)",
                    onClick = { resolved.clear() },
                    modifier = Modifier.fillMaxWidth(),
                    icon = "replay",
                    variant = KdmActionVariant.DARK,
                )
            }
        }
    }
}

@Composable
private fun DeckChip(
    pending: Int,
    total: Int,
) {
    Text(
        text = "MAZO LI · $pending/$total",
        style = KdmType.labelCaps,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
            Modifier
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
private fun HitsCounter(
    hits: Int,
    onChange: (Int) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Symbol("casino", MaterialTheme.colorScheme.secondary)
        Column(Modifier.weight(1f)) {
            Text(
                text = "IMPACTOS CONSEGUIDOS",
                style = KdmType.labelCaps,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Dados con éxito contra la Resistencia",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        StoneButton("−", onClick = { onChange(-1) })
        Text(text = hits.toString(), style = KdmType.statDisplay, color = MaterialTheme.colorScheme.primary)
        StoneButton("+", onClick = { onChange(1) })
    }
}

@Composable
private fun HitLocationCard(
    number: Int,
    location: HitLocationRow,
    onMiss: () -> Unit,
    onWound: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = number.toString(),
                style = KdmType.labelCaps,
                color = MaterialTheme.colorScheme.secondary,
                modifier =
                    Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(horizontal = 6.dp, vertical = 3.dp),
            )
            Text(
                text = location.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            ReactionBadge(location.reaction)
        }
        Text(
            text = location.text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
        ) {
            KdmActionButton("Fallo", onMiss, variant = KdmActionVariant.DARK)
            KdmActionButton("Herida", onWound, variant = KdmActionVariant.GOLD)
            KdmActionButton("Crítico", onWound, icon = "star", variant = KdmActionVariant.LIGHT)
        }
    }
}

@Composable
private fun ReactionBadge(reaction: Boolean) {
    Row(
        modifier =
            Modifier
                .clip(MaterialTheme.shapes.small)
                .background(
                    if (reaction) {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLowest
                    },
                ).padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = if (reaction) "REACCIÓN" else "SIN REACCIÓN",
            style = KdmType.labelCaps,
            color = if (reaction) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
