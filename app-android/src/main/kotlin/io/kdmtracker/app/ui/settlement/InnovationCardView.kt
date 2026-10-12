package io.kdmtracker.app.ui.settlement

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.kdmtracker.app.ui.theme.KdmActionButton
import io.kdmtracker.app.ui.theme.KdmActionVariant
import io.kdmtracker.app.ui.theme.KdmFontFamilies
import io.kdmtracker.app.ui.theme.KdmPalette
import io.kdmtracker.app.ui.theme.KdmType

/**
 * One card of the drawn hand, exactly as the reference draws it: category badge and unlock
 * subtitle on the header row, the name, the survival bonus, the rules text, the footnote, and
 * the one action the current [phaseState] allows.
 *
 * Only [PhaseState.OFFER] selects; the other two render the same card with an inert button so
 * the user can still read what they were offered.
 */
@Composable
fun InnovationCardView(
    card: InnovationCard,
    phaseState: PhaseState,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.small
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, shape)
                .border(1.dp, KdmPalette.outlineVariant.copy(alpha = 0.3f), shape)
                .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = card.category.uppercase(),
                style = microLabel,
                color = MaterialTheme.colorScheme.secondary,
                modifier =
                    Modifier
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f), shape)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
            )
            Text(
                text = card.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text(
            text = card.name,
            style = cardTitle,
            color = MaterialTheme.colorScheme.primary,
        )

        if (card.survivalBonus != 0) {
            Text(
                text = "Límite de Supervivencia +${card.survivalBonus}",
                style = KdmType.labelCaps,
                color = MaterialTheme.colorScheme.secondary,
            )
        }

        Text(
            text = card.effect,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        card.consequenceNote?.let { note ->
            Text(
                text = note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }

        when (phaseState) {
            PhaseState.OFFER ->
                KdmActionButton(
                    text = "SELECCIONAR",
                    onClick = { onSelect(card.name) },
                    icon = "check_circle",
                    variant = KdmActionVariant.GOLD,
                    modifier = Modifier.fillMaxWidth(),
                )
            PhaseState.UNAFFORDABLE ->
                KdmActionButton(
                    text = "REQUIERE COSTE",
                    onClick = {},
                    icon = "lock",
                    variant = KdmActionVariant.BLOCKED,
                    modifier = Modifier.fillMaxWidth(),
                )
            else ->
                KdmActionButton(
                    text = "INERTE",
                    onClick = {},
                    enabled = false,
                    variant = KdmActionVariant.GOLD,
                    modifier = Modifier.fillMaxWidth(),
                )
        }
    }
}

/** The reference's card title: `font-headline-sm text-[13px] font-bold` — Playfair, not Inter. */
private val cardTitle =
    TextStyle(
        fontFamily = KdmFontFamilies.playfair,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Bold,
    )

/** The card's category chip: `font-label-caps text-[9px]` on a 20% gold wash. */
private val microLabel =
    TextStyle(
        fontFamily = KdmFontFamilies.inter,
        fontSize = 9.sp,
        lineHeight = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.1f.em,
    )
