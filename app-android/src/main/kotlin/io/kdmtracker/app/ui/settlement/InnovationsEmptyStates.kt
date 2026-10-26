package io.kdmtracker.app.ui.settlement

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kdmtracker.app.ui.settlement.common.Symbol
import io.kdmtracker.app.ui.settlement.common.microLabel
import io.kdmtracker.app.ui.theme.InnovarPalette
import io.kdmtracker.app.ui.theme.KdmActionButton
import io.kdmtracker.app.ui.theme.KdmActionVariant
import io.kdmtracker.app.ui.theme.KdmPalette
import io.kdmtracker.app.ui.theme.KdmType

/**
 * The design's `ESTADO 6 - SIN CARTAS EN EL MAZO`: the grid is unchanged — an empty deck says
 * nothing about what the settlement owns — and only the part below it differs.
 */
@Composable
internal fun NoCardsBlock(
    model: InnovationsModel,
    onOpenCatalogue: () -> Unit,
) {
    val shape = MaterialTheme.shapes.small

    slotCards(model).chunked(2).forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            row.forEach { card ->
                SlotCard(card = card, modifier = Modifier.weight(1f))
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Symbol("swap_horizontal_circle", MaterialTheme.colorScheme.outline, 28.sp)
        Text(
            text = "No quedan innovaciones en el mazo",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Text(
            text =
                "No hay cartas con acceso directo en el mazo de robo actual: todas las " +
                    "consecuencias requieren innovaciones previas que todavía no se han desbloqueado.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer, shape)
                    .border(1.dp, KdmPalette.outlineVariant.copy(alpha = 0.5f), shape)
                    .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Symbol("lightbulb", InnovarPalette.gold, 16.sp)
                Text(
                    text = "¿Cómo desbloquear nuevas cartas?",
                    style = KdmType.labelCaps,
                    color = InnovarPalette.gold,
                )
            }
            listOf(
                "Construye localizaciones que añadan consecuencias al mazo.",
                "Supera hitos de población para desbloquear nuevos principios.",
                "Usa eventos de historia para introducir cartas raras.",
            ).forEach { line ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "•", style = microLabel, color = InnovarPalette.gold.copy(alpha = 0.7f))
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        KdmActionButton(
            text = "Innovar no disponible",
            onClick = {},
            icon = "lock",
            variant = KdmActionVariant.BLOCKED,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    CatalogueEntrance(deckSize = model.deckSize, onClick = onOpenCatalogue)
}

/** The design's `ESTADO 1 - SIN ACCESO`: the only thing on screen when Innovar cannot run at all. */
@Composable
internal fun NoAccessBlock() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Symbol("lock", KdmPalette.outline, 28.sp)
        Text(
            text = "Acción de Innovar no disponible",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text =
                "El asentamiento aún no ha construido el Cúmulo de Linternas " +
                    "o la fase actual prohíbe el desarrollo.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest, MaterialTheme.shapes.small)
                    .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Innovar no disponible",
                style = microLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
        }
    }
}
