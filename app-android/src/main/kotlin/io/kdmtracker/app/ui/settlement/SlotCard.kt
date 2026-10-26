package io.kdmtracker.app.ui.settlement

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kdmtracker.app.ui.settlement.common.Symbol
import io.kdmtracker.app.ui.settlement.common.microLabel
import io.kdmtracker.app.ui.theme.InnovarPalette
import io.kdmtracker.app.ui.theme.KdmFontFamilies
import io.kdmtracker.app.ui.theme.KdmPalette
import io.kdmtracker.app.ui.theme.KdmType

@Composable
internal fun SlotCard(
    card: SlotCardData,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.small
    val filled = card.kind == SlotKind.FILLED
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 110.dp)
                .background(
                    if (filled) {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    } else if (card.kind == SlotKind.ACTIVE_DECK) {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    shape,
                ).border(
                    1.dp,
                    when {
                        card.kind == SlotKind.ACTIVE_DECK -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                        card.state == SlotState.OWNED -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                        card.state == SlotState.NEW_THIS_YEAR -> InnovarPalette.green.copy(alpha = 0.6f)
                        else -> KdmPalette.outlineVariant.copy(alpha = 0.3f)
                    },
                    shape,
                ).padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            if (card.kind == SlotKind.ACTIVE_DECK) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = card.label,
                        style = KdmType.labelCaps,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Box(
                        modifier =
                            Modifier
                                .size(6.dp)
                                .background(MaterialTheme.colorScheme.secondary, CircleShape),
                    )
                }
            } else {
                Text(
                    text = card.label,
                    style = KdmType.labelCaps,
                    color =
                        when {
                            card.state == SlotState.NEW_THIS_YEAR -> InnovarPalette.green
                            card.state == SlotState.OWNED -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }
            Symbol(card.icon, card.iconColor, card.iconSize)
        }

        when {
            filled || card.kind == SlotKind.ACTIVE_DECK ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = card.title,
                        style = slotTitle,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    card.note?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color =
                                if (card.kind == SlotKind.ACTIVE_DECK) {
                                    MaterialTheme.colorScheme.secondary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        )
                    }
                }
            else ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Symbol(card.centerIcon ?: card.icon, MaterialTheme.colorScheme.outline, 22.sp)
                    Text(
                        text = card.title,
                        style = KdmType.labelCaps,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
        }

        if (card.kind == SlotKind.ACTIVE_DECK) {
            // Design estado A: the deck slot carries a static count line, not the old toggle —
            // the entrance row below the grid is the single affordance now.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Symbol("style", MaterialTheme.colorScheme.secondary, 12.sp)
                Text(
                    text = card.footer,
                    style = microLabel,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        } else {
            Text(
                text = card.footer,
                style = microLabel,
                color =
                    when (card.state) {
                        SlotState.OWNED -> MaterialTheme.colorScheme.secondary
                        SlotState.NEW_THIS_YEAR -> InnovarPalette.green
                        SlotState.EMPTY -> MaterialTheme.colorScheme.outline
                    },
                modifier =
                    if (card.footerCentered) {
                        Modifier.fillMaxWidth()
                    } else {
                        Modifier
                            .background(
                                when (card.state) {
                                    SlotState.OWNED -> InnovarPalette.goldWash
                                    SlotState.NEW_THIS_YEAR -> InnovarPalette.greenWash
                                    SlotState.EMPTY -> Color.Transparent
                                },
                                MaterialTheme.shapes.small,
                            ).padding(horizontal = 4.dp, vertical = 2.dp)
                    },
                textAlign = if (card.footerCentered) TextAlign.Center else TextAlign.Start,
            )
        }
    }
}

/** The slot grid's title: `font-headline-sm text-[14px] font-bold` — Playfair, not Inter. */
private val slotTitle =
    TextStyle(
        fontFamily = KdmFontFamilies.playfair,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Bold,
    )
