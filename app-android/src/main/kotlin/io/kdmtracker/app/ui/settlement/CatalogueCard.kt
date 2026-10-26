package io.kdmtracker.app.ui.settlement

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kdmtracker.app.ui.settlement.common.Symbol
import io.kdmtracker.app.ui.settlement.common.microLabel
import io.kdmtracker.app.ui.settlement.common.topHairline
import io.kdmtracker.app.ui.theme.InnovarPalette
import io.kdmtracker.app.ui.theme.KdmFontFamilies
import io.kdmtracker.app.ui.theme.KdmPalette
import io.kdmtracker.core.rules.InnovationStatus

/**
 * One catalogue card preview (the design's fixed-height card): category chip, name, two-line
 * effect, and the status footer — gold `ADQUIRIDA`, green `DISPONIBLE`, or grey `SIN ACCESO` with
 * the parent it still requires.
 */
@Composable
internal fun CatalogueCardView(
    card: CatalogCard,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.small
    val accent =
        when (card.status) {
            InnovationStatus.UNLOCKED -> InnovarPalette.gold
            InnovationStatus.AVAILABLE -> InnovarPalette.green
            InnovationStatus.NO_ACCESS -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    val container =
        if (card.status == InnovationStatus.NO_ACCESS) {
            MaterialTheme.colorScheme.surfaceContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        }

    Column(
        modifier =
            modifier
                .height(142.dp)
                .clip(shape)
                .background(container, shape)
                .border(
                    1.dp,
                    when (card.status) {
                        InnovationStatus.UNLOCKED -> InnovarPalette.gold.copy(alpha = 0.6f)
                        InnovationStatus.AVAILABLE -> InnovarPalette.green.copy(alpha = 0.5f)
                        InnovationStatus.NO_ACCESS -> KdmPalette.outlineVariant
                    },
                    shape,
                ).padding(10.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = card.category.uppercase(),
                style = microLabel,
                color =
                    if (card.status == InnovationStatus.NO_ACCESS) {
                        InnovarPalette.gold.copy(alpha = 0.7f)
                    } else {
                        InnovarPalette.gold
                    },
                modifier =
                    Modifier
                        .background(
                            if (card.status == InnovationStatus.NO_ACCESS) {
                                InnovarPalette.goldPillBg.copy(alpha = 0.5f)
                            } else {
                                InnovarPalette.goldPillBg
                            },
                            MaterialTheme.shapes.small,
                        ).padding(horizontal = 6.dp, vertical = 2.dp),
            )
            Text(
                text = card.name,
                style = cardTitle,
                color =
                    if (card.status == InnovationStatus.NO_ACCESS) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = card.effect,
                style =
                    TextStyle(
                        fontFamily = KdmFontFamilies.inter,
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                    ),
                color =
                    if (card.status == InnovationStatus.NO_ACCESS) {
                        MaterialTheme.colorScheme.outline
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Column(modifier = Modifier.topHairline(KdmPalette.outlineVariant.copy(alpha = 0.5f)).padding(top = 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = card.footer, style = microLabel, color = accent)
                Symbol(footerIcon(card.status), accent, 12.sp)
            }
            if (card.requires != null) {
                Text(
                    text = "Requiere: ${card.requires}",
                    style =
                        TextStyle(
                            fontFamily = KdmFontFamilies.inter,
                            fontSize = 8.sp,
                            lineHeight = 11.sp,
                        ),
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun footerIcon(status: InnovationStatus): String =
    when (status) {
        InnovationStatus.UNLOCKED -> "check_circle"
        InnovationStatus.AVAILABLE -> "lock_open"
        InnovationStatus.NO_ACCESS -> "lock"
    }

/** The catalogue card's title: `font-headline-sm text-[13px] font-bold` — Playfair, not Inter. */
private val cardTitle =
    TextStyle(
        fontFamily = KdmFontFamilies.playfair,
        fontSize = 13.sp,
        lineHeight = 17.sp,
        fontWeight = FontWeight.Bold,
    )
