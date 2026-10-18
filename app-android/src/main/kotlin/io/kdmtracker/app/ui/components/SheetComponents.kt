package io.kdmtracker.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.kdmtracker.app.ui.theme.KdmSymbols
import io.kdmtracker.app.ui.theme.KdmType

/**
 * Shared sheet primitives on the Atramentum theme, extracted from `SurvivorSheet` so the hunt (V2)
 * and showdown (V3) screens reuse them.
 *
 * These keep the **exact signature and render** they had as `private fun`s inside `SurvivorSheet`,
 * so extracting them is a pure refactor with no visual change. In particular [Symbol] renders with
 * the `bodyLarge` font size SurvivorSheet used; the `settlement/` screens that pass a `size` and a
 * pinned `lineHeight` keep their own local copies (those render differently and are deliberately
 * not consolidated here).
 *
 * [SheetPanel] is a tonal panel with an optional leading icon and a trailing slot, the sheet's
 * section frame.
 */
@Composable
fun SheetPanel(
    title: String,
    icon: String? = null,
    trailing: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest, MaterialTheme.shapes.small)
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Symbol(icon, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = title.uppercase(),
                style = KdmType.labelCaps,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            trailing()
        }
        content()
    }
}

/**
 * The Hunt XP track: one cell per point, scrolled horizontally. Cells past [filled] stay unlit.
 */
@Composable
fun PipTrack(
    filled: Int,
    total: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(total) { index ->
            Box(
                modifier =
                    Modifier
                        .size(16.dp)
                        .background(
                            if (index < filled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                        ),
                contentAlignment = Alignment.Center,
            ) {
                if (index < filled) {
                    Text(
                        text = (index + 1).toString(),
                        style = KdmType.labelCaps,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}

/** One attribute stat: an icon over the value over its label, centred. */
@Composable
fun AttributeStat(
    label: String,
    value: Int,
    icon: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Symbol(icon, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value.toString(), style = KdmType.statDisplay)
        Text(
            text = label,
            style = KdmType.labelCaps,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The colour of a stone box's label. Checked must stay legible on the panel background —
 * `onPrimary` (the obvious choice for a filled button) is nearly invisible there, so the checked
 * state uses the panel-safe [androidx.compose.material3.ColorScheme.secondary].
 */
internal fun woundBoxColor(
    checked: Boolean,
    scheme: androidx.compose.material3.ColorScheme,
): androidx.compose.ui.graphics.Color = if (checked) scheme.secondary else scheme.onSurfaceVariant

/**
 * A compact 28dp stone square — the design's "minimalist stone square with a centered numeral".
 *
 * It has a fixed size on purpose: a row of Material buttons squeezed the neighbouring label down to
 * one character per line, so every control in a crowded row is explicitly sized.
 */
@Composable
fun StoneButton(
    label: String,
    onClick: () -> Unit,
    checked: Boolean = false,
    enabled: Boolean = true,
) {
    Box(
        modifier =
            Modifier
                .size(28.dp)
                .border(
                    1.dp,
                    if (checked) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                    MaterialTheme.shapes.small,
                ).background(
                    if (checked) MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f) else Color.Transparent,
                ).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = KdmType.labelCaps,
            fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal,
            color =
                if (enabled) {
                    woundBoxColor(checked, MaterialTheme.colorScheme)
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
        )
    }
}

/** A Material Symbols glyph at the sheet's `bodyLarge` size (SurvivorSheet's original render). */
@Composable
fun Symbol(
    name: String,
    color: androidx.compose.ui.graphics.Color,
) {
    Text(
        text = KdmSymbols.glyph(name),
        fontFamily = KdmSymbols.family,
        fontSize = MaterialTheme.typography.bodyLarge.fontSize,
        color = color,
    )
}
