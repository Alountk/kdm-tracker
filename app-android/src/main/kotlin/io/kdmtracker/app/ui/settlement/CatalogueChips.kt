package io.kdmtracker.app.ui.settlement

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kdmtracker.app.ui.theme.InnovarPalette
import io.kdmtracker.app.ui.theme.KdmFontFamilies
import io.kdmtracker.app.ui.theme.KdmPalette

/** The acquisition chip: solid gold while selected (the design's estado B/C active state). */
@Composable
internal fun AcquisitionChip(
    chip: CatalogChip,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Text(
        text = "${chip.label} (${chip.count})",
        style =
            TextStyle(
                fontFamily = KdmFontFamilies.inter,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontWeight = if (chip.selected) FontWeight.Bold else FontWeight.Medium,
            ),
        color =
            if (chip.selected) {
                KdmPalette.canvas
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        modifier =
            Modifier
                .clip(shape)
                .background(
                    if (chip.selected) {
                        InnovarPalette.gold
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                    shape,
                ).border(
                    1.dp,
                    if (chip.selected) {
                        InnovarPalette.gold
                    } else {
                        KdmPalette.outlineVariant.copy(alpha = 0.6f)
                    },
                    shape,
                ).clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** The category chip: gold wash while selected, smaller than the acquisition chips. */
@Composable
internal fun CategoryChip(
    chip: CatalogChip,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(4.dp)
    Text(
        text = "${chip.label} (${chip.count})",
        style =
            TextStyle(
                fontFamily = KdmFontFamilies.inter,
                fontSize = 10.sp,
                lineHeight = 13.sp,
                fontWeight = if (chip.selected) FontWeight.SemiBold else FontWeight.Medium,
            ),
        color =
            if (chip.selected) {
                InnovarPalette.gold
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        modifier =
            Modifier
                .clip(shape)
                .background(
                    if (chip.selected) {
                        InnovarPalette.goldPillBg
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                    shape,
                ).border(
                    1.dp,
                    if (chip.selected) {
                        InnovarPalette.gold.copy(alpha = 0.4f)
                    } else {
                        KdmPalette.outlineVariant.copy(alpha = 0.6f)
                    },
                    shape,
                ).clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}
