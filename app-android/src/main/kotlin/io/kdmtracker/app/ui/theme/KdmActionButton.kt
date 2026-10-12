package io.kdmtracker.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The design's action-button variants — one per container/label pair the screen actually renders. */
enum class KdmActionVariant {
    /** Amber primary action — the design's `FABRICAR`. */
    GOLD,

    /** Bone/light action — the design's `CONSTRUIR` and `ERIGIR`. */
    LIGHT,

    /** Surface action — a location's own action (`Innovar`/Augurio). */
    DARK,

    /** The design's unaffordable `FABRICAR`: raised surface, label still readable. */
    OUT_OF_STOCK,

    /** The design's `BLOQUEADO`: flat surface, outline-grey label, always inert. */
    BLOCKED,
}

private data class KdmActionColors(
    val container: Color,
    val content: Color,
    val icon: Color,
)

private fun actionColors(
    variant: KdmActionVariant,
    enabled: Boolean,
): KdmActionColors =
    when {
        variant == KdmActionVariant.OUT_OF_STOCK ->
            KdmActionColors(KdmPalette.surfaceHighest, KdmPalette.onCanvas, KdmPalette.onCanvas)
        variant == KdmActionVariant.BLOCKED ->
            KdmActionColors(KdmPalette.surfaceContainer, KdmPalette.outline, KdmPalette.outline)
        !enabled ->
            KdmActionColors(KdmPalette.surfaceHigh, KdmPalette.ash, KdmPalette.ash)
        variant == KdmActionVariant.GOLD ->
            KdmActionColors(KdmPalette.gold, KdmPalette.onGold, KdmPalette.onGold)
        variant == KdmActionVariant.LIGHT ->
            KdmActionColors(KdmPalette.bone, KdmPalette.onBone, KdmPalette.onBone)
        else ->
            KdmActionColors(KdmPalette.surfaceHigh, KdmPalette.onCanvas, KdmPalette.gold)
    }

/**
 * The reusable action button of the Atramentum design: `px-3 py-1.5` over label-caps text, a 4dp
 * corner and an optional Material Symbol on the left — the mock's metrics, not Material 3's
 * default 24/6 content padding and 40dp minimum height, which are what made the buttons huge.
 *
 * [enabled] is false only for the variants the design itself renders inert; the
 * [KdmActionVariant.OUT_OF_STOCK] and [KdmActionVariant.BLOCKED] colours *are* the design's
 * disabled states, so they carry their own container/label pair instead of a generic dim.
 */
@Composable
fun KdmActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: String? = null,
    variant: KdmActionVariant = KdmActionVariant.LIGHT,
    border: Color? = null,
) {
    val shape = MaterialTheme.shapes.small
    val colors = actionColors(variant, enabled)
    val interactive = enabled && variant != KdmActionVariant.OUT_OF_STOCK && variant != KdmActionVariant.BLOCKED
    Box(
        modifier =
            modifier
                .clip(shape)
                .background(colors.container)
                .clickable(enabled = interactive, onClick = onClick)
                .then(if (border != null) Modifier.border(1.dp, border, shape) else Modifier)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (icon != null) {
                Text(
                    text = KdmSymbols.glyph(icon),
                    style =
                        TextStyle(
                            fontFamily = KdmSymbols.family,
                            fontSize = 13.sp,
                            // The symbol font's own line box is ~1.7em; the mock's icon is inline
                            // with the label's 14px line, so pin it instead of letting it pad
                            // the button to nearly twice the design's height.
                            lineHeight = 14.sp,
                        ),
                    color = colors.icon,
                )
            }
            Text(text = text, style = KdmType.labelCaps, color = colors.content)
        }
    }
}
