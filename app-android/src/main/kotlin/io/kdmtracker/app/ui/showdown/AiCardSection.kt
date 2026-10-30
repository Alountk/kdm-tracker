package io.kdmtracker.app.ui.showdown

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.kdmtracker.app.ui.components.SheetPanel
import io.kdmtracker.app.ui.theme.KdmActionButton
import io.kdmtracker.app.ui.theme.KdmActionVariant
import io.kdmtracker.app.ui.theme.KdmType

/**
 * The monster's turn: the last drawn AI card (name, rank badge, Move & Attack numbers and its
 * rules text) and the button that draws the next one.
 *
 * Target selection and the round/turn counter are part of the design but not of the stored
 * encounter, so the card shows the printed text as it is.
 */
@Composable
internal fun AiCardSection(
    drawn: DrawnCard?,
    onDraw: () -> Unit,
) {
    SheetPanel(title = "Carta de IA en curso", icon = "psychology_alt") {
        if (drawn == null) {
            Text(
                text = "Todavía no se ha robado ninguna carta de IA.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            DrawnCardBody(drawn)
        }
        KdmActionButton(
            text = "Robar siguiente IA",
            onClick = onDraw,
            modifier = Modifier.fillMaxWidth(),
            icon = "style",
            variant = KdmActionVariant.DARK,
        )
    }
}

@Composable
private fun DrawnCardBody(drawn: DrawnCard) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.secondaryContainer,
                            MaterialTheme.colorScheme.secondary,
                            MaterialTheme.colorScheme.secondaryContainer,
                        ),
                    ),
                ),
        )
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = drawn.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                drawn.rank?.let { RankBadge(it) }
            }
            drawn.stats?.let { StatsRow(it) }
            if (drawn.text.isNotBlank()) {
                Text(
                    text = drawn.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RankBadge(rank: String) {
    Box(
        modifier =
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = rank, style = KdmType.labelCaps, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun StatsRow(stats: CardStats) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCell("Velocidad", stats.speed.toString(), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
        StatCell("Precisión", stats.accuracy, MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
        StatCell("Daño", stats.damage.toString(), MaterialTheme.colorScheme.error, Modifier.weight(1f))
    }
}

@Composable
private fun StatCell(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier,
) {
    Column(
        modifier =
            modifier
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label.uppercase(),
            style = KdmType.labelCaps,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = KdmType.statDisplay, fontWeight = FontWeight.Bold, color = color)
    }
}
