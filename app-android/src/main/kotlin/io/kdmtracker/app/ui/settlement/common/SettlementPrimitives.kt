package io.kdmtracker.app.ui.settlement.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.kdmtracker.app.ui.theme.KdmFontFamilies
import io.kdmtracker.app.ui.theme.KdmSymbols
import io.kdmtracker.app.ui.theme.KdmType

/** A Material Symbol glyph rendered from the bundled subset font. */
@Composable
internal fun Symbol(
    name: String,
    color: Color,
    size: TextUnit,
) {
    Text(
        text = KdmSymbols.glyph(name),
        // Google's hosted Material Symbols CSS pins `line-height: 1`; without it the symbol
        // font's own ~1.7em line box pads every row that carries an icon.
        style = TextStyle(fontFamily = KdmSymbols.family, fontSize = size, lineHeight = size),
        color = color,
    )
}

/** Section title row: glyph + uppercase title, trailing value on the right. */
@Composable
internal fun PanelTitle(
    icon: String,
    title: String,
    trailing: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Symbol(icon, MaterialTheme.colorScheme.secondary, 18.sp)
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = trailing.uppercase(),
            style = KdmType.labelCaps,
            color = MaterialTheme.colorScheme.secondary,
            modifier =
                Modifier
                    .background(MaterialTheme.colorScheme.onSecondaryContainer, MaterialTheme.shapes.small)
                    .border(1.dp, MaterialTheme.colorScheme.secondary, MaterialTheme.shapes.small)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

/** A 1dp hairline across the top of the row — the mock's `border-t`. */
internal fun Modifier.topHairline(color: Color): Modifier =
    drawBehind {
        drawLine(
            color = color,
            start = Offset(0f, 0.5f),
            end = Offset(size.width, 0.5f),
            strokeWidth = 1.dp.toPx(),
        )
    }

/** `font-label-caps text-[9px]` with wide tracking: footers and chip labels. */
internal val microLabel =
    TextStyle(
        fontFamily = KdmFontFamilies.inter,
        fontSize = 9.sp,
        lineHeight = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.1f.em,
    )
