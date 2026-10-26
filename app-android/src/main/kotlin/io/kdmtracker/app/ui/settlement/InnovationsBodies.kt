package io.kdmtracker.app.ui.settlement

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kdmtracker.app.ui.settlement.common.Symbol
import io.kdmtracker.app.ui.settlement.common.microLabel
import io.kdmtracker.app.ui.theme.InnovarPalette
import io.kdmtracker.app.ui.theme.KdmActionButton
import io.kdmtracker.app.ui.theme.KdmActionVariant
import io.kdmtracker.app.ui.theme.KdmPalette
import io.kdmtracker.app.ui.theme.KdmType

/** Everything below the gate once the settlement can both reach Innovar and pay for it. */
@Composable
internal fun ReadyBody(
    model: InnovationsModel,
    picked: String?,
    onPick: (String) -> Unit,
    drawn: Boolean,
    onDraw: () -> Unit,
    onOpenCatalogue: () -> Unit,
    onConfirm: (String) -> Unit,
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

    if (!drawn) {
        // The design's cost row: no wrapper around the row itself, full width — the label
        // sits left and the minified price is boxed on the right.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "COSTE", style = microLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier =
                    Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainerLow, shape)
                        .border(1.dp, MaterialTheme.colorScheme.surfaceContainerHigh, shape)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${model.costChip.endeavors}*",
                    style = KdmType.labelCaps,
                    color = InnovarPalette.gold,
                )
                Text(
                    text = "| ${model.costChip.abbreviated}",
                    style = microLabel,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        KdmActionButton(
            text = "Robar cartas para innovar",
            onClick = onDraw,
            icon = "style",
            variant = KdmActionVariant.GOLD,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    // The offer, exactly as the HTML draws it: its own container, its own gold border.
    if (drawn) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OfferHeader(model = model)

            model.hand.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { card ->
                        InnovationCardView(
                            card = card,
                            phaseState = model.phaseState,
                            selected = picked == card.name,
                            onPick = onPick,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                    if (row.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }

            picked?.let { chosen ->
                KdmActionButton(
                    text = "ACEPTAR ELECCIÓN",
                    onClick = { onConfirm(chosen) },
                    icon = "check_circle",
                    variant = KdmActionVariant.GOLD,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    CatalogueEntrance(deckSize = model.deckSize, onClick = onOpenCatalogue)
}

/** The design's `ESTADO 2 - CONSUMADA`: the resolution of the draw and the year's gold note. */
@Composable
internal fun ConsumedBlock(
    model: InnovationsModel,
    resolution: Pair<String, String>?,
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
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer, shape)
                .border(1.dp, KdmPalette.outlineVariant.copy(alpha = 0.3f), shape)
                .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Symbol("sync_alt", InnovarPalette.green, 16.sp)
            Text(
                text = "Resolución del robo de cartas",
                style = KdmType.labelCaps,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "Completado",
                style = microLabel,
                color = InnovarPalette.green,
                modifier =
                    Modifier
                        .background(InnovarPalette.greenWash, shape)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        if (resolution != null) {
            Text(
                text = "${resolution.first} · ${resolution.second} devuelta al fondo del mazo de innovación",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = "1 CARTA DEVUELTA", style = microLabel, color = MaterialTheme.colorScheme.outline)
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(InnovarPalette.goldWash, shape)
                .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f), shape)
                .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Symbol("check_circle", MaterialTheme.colorScheme.secondary, 16.sp)
            Text(
                text = "Innovación Realizada en este Año Linterna",
                style = KdmType.labelCaps,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        Text(
            text =
                "El esfuerzo gastado y los 3 recursos (1 Hueso, 1 Órgano, 1 Pellejo) " +
                    "ya fueron deducidos del almacén.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    CatalogueEntrance(deckSize = model.deckSize, onClick = onOpenCatalogue)
}
