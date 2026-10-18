package io.kdmtracker.app.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import io.kdmtracker.app.ui.theme.KdmSymbols

/**
 * Draws one Material Symbols glyph by name with the bundled icon font — the same web font the
 * Stitch design renders its icons with. Shared by the hunt screen's regions.
 */
@Composable
internal fun Symbol(
    name: String,
    color: Color,
    size: Int,
) {
    Text(text = KdmSymbols.glyph(name), color = color, fontSize = size.sp, fontFamily = KdmSymbols.family)
}
