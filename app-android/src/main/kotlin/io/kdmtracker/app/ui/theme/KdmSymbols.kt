package io.kdmtracker.app.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import io.kdmtracker.app.R

/**
 * The Material Symbols Outlined glyphs the design's screens use.
 *
 * The design renders icons with the Material Symbols web font, so the sheet does the same: the font
 * is bundled and glyphs are drawn as text by codepoint. Every codepoint here was taken from Google's
 * official `MaterialSymbolsOutlined[...].codepoints`, and [KdmSymbolsTest] pins them — a wrong value
 * draws a different icon.
 */
object KdmSymbols {
    /** The icon font. Registered at its default instance (FILL 0, weight 400). */
    val family: FontFamily = FontFamily(Font(R.font.material_symbols_outlined, weight = FontWeight.Normal))

    /** The single-glyph string for [name], or an [IllegalArgumentException] when unmapped. */
    fun glyph(name: String): String =
        CODEPOINTS[name]?.toChar()?.toString()
            ?: throw IllegalArgumentException("unmapped Material Symbol: $name")

    // Every codepoint is inside the Basic Multilingual Plane, so a Char is enough.
    private val CODEPOINTS: Map<String, Int> =
        mapOf(
            "flare" to 0xE3E4,
            "swords" to 0xF889,
            "light_mode" to 0xE518,
            "group" to 0xEA21,
            "skull" to 0xF89A,
            "person" to 0xF0D3,
            "chevron_left" to 0xE5CB,
            "grid_view" to 0xE9B0,
            "psychology" to 0xEA4A,
            "directions_walk" to 0xE536,
            "target" to 0xE719,
            "fitness_center" to 0xEB43,
            "airline_stops" to 0xE7D0,
            "casino" to 0xEB40,
            "bolt" to 0xEA0B,
            "security" to 0xE32A,
            "face" to 0xF008,
            "front_hand" to 0xE769,
            "accessibility_new" to 0xE92C,
            "roller_shades" to 0xEC12,
            "airline_seat_legroom_extra" to 0xE633,
            "shield" to 0xE9E0,
            "handyman" to 0xF10B,
            "sports_mma" to 0xEA2C,
            "directions_run" to 0xE566,
            "emergency_heat" to 0xF15D,
            "fort" to 0xEAAD,
            "shield_person" to 0xF650,
            "explore" to 0xE87A,
        )
}
