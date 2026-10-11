package io.kdmtracker.app.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import io.kdmtracker.app.R

/**
 * The Material Symbols Outlined glyphs the design's screens use.
 *
 * The design renders icons with the Material Symbols web font, so the app does the same: a
 * **subset** of the font is bundled (the glyphs the design's four screens use, ~0.14 MB instead of
 * the full 10.23 MB) and glyphs are drawn as text by codepoint. Every codepoint was taken from
 * Google's official `MaterialSymbolsOutlined[...].codepoints`, and [KdmSymbolsTest] pins them all —
 * a wrong value draws a different icon. `drive_file_stream` is absent from that official file and is
 * therefore not bundled.
 */
object KdmSymbols {
    /** The icon font. Registered at its default instance (FILL 0, weight 400). */
    val family: FontFamily = FontFamily(Font(R.font.material_symbols_outlined, weight = FontWeight.Normal))

    /** How many glyphs the bundle maps — the designs' whole icon vocabulary. */
    val glyphCount: Int get() = CODEPOINTS.size

    /** The single-glyph string for [name], or an [IllegalArgumentException] when unmapped. */
    fun glyph(name: String): String =
        CODEPOINTS[name]?.toChar()?.toString()
            ?: throw IllegalArgumentException("unmapped Material Symbol: $name")

    // Every codepoint is inside the Basic Multilingual Plane, so a Char is enough.
    private val CODEPOINTS: Map<String, Int> =
        mapOf(
            "accessibility_new" to 0xE92C,
            "account_balance" to 0xE84F,
            "add" to 0xE145,
            "airline_seat_legroom_extra" to 0xE633,
            "airline_stops" to 0xE7D0,
            "arrow_drop_down" to 0xE5C5,
            "arrow_drop_up" to 0xE5C7,
            "arrow_forward" to 0xE5C8,
            "auto_stories" to 0xE666,
            "bolt" to 0xEA0B,
            "casino" to 0xEB40,
            "check_box_outline_blank" to 0xE835,
            "check_circle" to 0xF0BE,
            "chevron_left" to 0xE5CB,
            "colorize" to 0xE3B8,
            "crisis_alert" to 0xEBE9,
            "crop_square" to 0xE3C6,
            "directions_run" to 0xE566,
            "directions_walk" to 0xE536,
            "domain" to 0xE7EE,
            "edit_note" to 0xE745,
            "emergency" to 0xE1EB,
            "emergency_heat" to 0xF15D,
            "explore" to 0xE87A,
            "face" to 0xF008,
            "favorite" to 0xE87E,
            "fitness_center" to 0xEB43,
            "format_h4" to 0xF860,
            "flare" to 0xE3E4,
            "fort" to 0xEAAD,
            "front_hand" to 0xE769,
            "grass" to 0xF205,
            "grid_view" to 0xE9B0,
            "grid_4x4" to 0xF016,
            "group" to 0xEA21,
            "group_add" to 0xE7F0,
            "groups" to 0xF233,
            "handyman" to 0xF10B,
            "hardware" to 0xEA59,
            "home" to 0xE9B2,
            "hourglass_bottom" to 0xEA5C,
            "hourglass_empty" to 0xE88B,
            "info" to 0xE88E,
            "landscape" to 0xE564,
            "layers" to 0xE53B,
            "lightbulb" to 0xE0F0,
            "light_mode" to 0xE518,
            "local_fire_department" to 0xEF55,
            "lock" to 0xE897,
            "person" to 0xF0D3,
            "pets" to 0xE91D,
            "psychology" to 0xEA4A,
            "psychology_alt" to 0xF8EA,
            "publish" to 0xE255,
            "remove" to 0xE15B,
            "replay" to 0xE042,
            "restart_alt" to 0xF053,
            "roller_shades" to 0xEC12,
            "route" to 0xEACD,
            "security" to 0xE32A,
            "shield" to 0xE9E0,
            "shield_person" to 0xF650,
            "skull" to 0xF89A,
            "sports_mma" to 0xEA2C,
            "style" to 0xE41D,
            "swords" to 0xF889,
            "target" to 0xE719,
            "verified" to 0xEF76,
            "visibility" to 0xE8F4,
            "vital_signs" to 0xE650,
            "warning" to 0xF083,
        )
}
