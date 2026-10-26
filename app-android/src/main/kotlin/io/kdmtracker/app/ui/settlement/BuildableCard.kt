package io.kdmtracker.app.ui.settlement

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import io.kdmtracker.app.ui.settlement.common.topHairline
import io.kdmtracker.app.ui.theme.KdmActionButton
import io.kdmtracker.app.ui.theme.KdmActionVariant
import io.kdmtracker.app.ui.theme.KdmPalette

/** One "Por erigir" card for a location that is not built and has no ready upgrade. */
@Composable
internal fun BuildableCard(
    card: BuildableLocationCard,
    onBuild: (String) -> Unit,
) {
    val shape = MaterialTheme.shapes.small
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .alpha(0.9f)
                .background(MaterialTheme.colorScheme.surfaceContainer, shape)
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "POR ERIGIR",
                    style = microBold.widest,
                    color = MaterialTheme.colorScheme.outline,
                )
                Text(
                    text = card.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Pill(
                text = "NO CONSTRUIDO",
                container = MaterialTheme.colorScheme.surfaceContainerLow,
                content = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        card.description?.let { description ->
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (card.costChips.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                card.costChips.forEach { chip ->
                    Chip(text = chip)
                }
            }
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .topHairline(KdmPalette.outlineVariant.copy(alpha = 0.2f))
                    .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                if (card.requires.isNotEmpty()) {
                    Text(
                        text = "Requiere: ${card.requires.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (card.endeavors > 0) {
                    Text(
                        text = "${card.endeavors} Esfuerzo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            KdmActionButton(
                text = "CONSTRUIR",
                onClick = { onBuild(card.name) },
                icon = "handyman",
                variant = KdmActionVariant.LIGHT,
            )
        }
    }
}
