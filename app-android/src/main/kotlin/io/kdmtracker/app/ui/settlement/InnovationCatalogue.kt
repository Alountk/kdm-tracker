package io.kdmtracker.app.ui.settlement

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kdmtracker.app.ui.settlement.common.Symbol
import io.kdmtracker.app.ui.settlement.common.microLabel
import io.kdmtracker.app.ui.theme.InnovarPalette
import io.kdmtracker.app.ui.theme.KdmFontFamilies
import io.kdmtracker.app.ui.theme.KdmPalette

/** The design's entrance row (estado A): one row that opens the catalogue as a separate view. */
@Composable
fun CatalogueEntrance(
    deckSize: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.small
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, shape)
                    .border(1.dp, KdmPalette.outlineVariant.copy(alpha = 0.6f), shape)
                    .clickable(onClick = onClick)
                    .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(32.dp)
                            .background(InnovarPalette.goldPillBg, MaterialTheme.shapes.small)
                            .border(1.dp, InnovarPalette.gold.copy(alpha = 0.4f), MaterialTheme.shapes.small),
                    contentAlignment = Alignment.Center,
                ) {
                    Symbol("manage_search", InnovarPalette.gold, 19.sp)
                }
                Column {
                    Text(
                        text = "Mazo de Innovación",
                        style = entranceTitle,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "($deckSize Descubribles)",
                        style = entranceSubtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Catálogo",
                    style = microLabel,
                    color = InnovarPalette.gold,
                    modifier =
                        Modifier
                            .background(InnovarPalette.goldPillBg, RoundedCornerShape(4.dp))
                            .border(1.dp, InnovarPalette.gold.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                )
                Symbol("unfold_more", MaterialTheme.colorScheme.onSurfaceVariant, 18.sp)
            }
        }
        Text(
            text = "Abre el catálogo completo para ver lo que ya posees y lo que puedes descubrir.",
            style =
                TextStyle(
                    fontFamily = KdmFontFamilies.inter,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    fontStyle = FontStyle.Italic,
                ),
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        )
    }
}

/** The entrance row's title: 14px medium Inter. */
internal val entranceTitle =
    TextStyle(
        fontFamily = KdmFontFamilies.inter,
        fontSize = 14.sp,
        lineHeight = 17.sp,
        fontWeight = FontWeight.Medium,
    )

/** The entrance row's subtitle / deck-row subtitle: 11px Inter. */
internal val entranceSubtitle =
    TextStyle(
        fontFamily = KdmFontFamilies.inter,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.SemiBold,
    )
