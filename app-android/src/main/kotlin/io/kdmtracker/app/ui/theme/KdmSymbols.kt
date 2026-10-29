package io.kdmtracker.app.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import io.kdmtracker.app.R

/**
 * The Material Symbols Outlined glyphs the design's screens use.
 *
 * The design renders icons with the Material Symbols web font, so the app does the same: a
 * **subset** of the font is bundled (only the glyphs the designs draw, ~0.18 MB instead of
 * the full ~10.7 MB) and glyphs are drawn as text by codepoint. Every codepoint was taken from
 * Google's official `MaterialSymbolsOutlined[...].codepoints`, and [KdmSymbolsTest] pins them all —
 * a wrong value draws a different icon. `drive_file_stream` is absent from that official file and is
 * therefore not bundled.
 */
object KdmSymbols {
    /** The icon font. Registered at its default instance (FILL 0, weight 400). */
    val family: FontFamily = FontFamily(Font(R.font.material_symbols_outlined, weight = FontWeight.Normal))

    /**
     * The icon font pinned at FILL 1 — the design's **filled** marks (the party's star, the
     * beast's paw). The same variable font, so the FILL axis selects the solid glyph on API 26+;
     * below it the font falls back to its default (outline) instance.
     */
    val filledFamily: FontFamily =
        FontFamily(
            Font(
                R.font.material_symbols_outlined,
                weight = FontWeight.Normal,
                variationSettings =
                    FontVariation.Settings(
                        FontVariation.weight(400),
                        FontVariation.Setting("FILL", 1f),
                    ),
            ),
        )

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
            "add_circle" to 0xE147,
            "airline_seat_legroom_extra" to 0xE633,
            "airline_stops" to 0xE7D0,
            "arrow_drop_down" to 0xE5C5,
            "arrow_drop_up" to 0xE5C7,
            "arrow_forward" to 0xE5C8,
            "arrow_back" to 0xE5C4, // beast retreat
            "auto_stories" to 0xE666,
            "balance" to 0xEAF6,
            "bedtime" to 0xF159, // hunt board: Overwhelming Darkness (Stitch)
            "bolt" to 0xEA0B,
            "bookmark" to 0xE866,
            "bookmark_border" to 0xE867, // legacy alias: Material Symbols draws it with the bookmark glyph
            "brush" to 0xE3AE,
            "casino" to 0xEB40,
            "check" to 0xE5CA,
            "check_box" to 0xE834,
            "check_box_outline_blank" to 0xE835,
            "check_circle" to 0xF0BE,
            "chevron_left" to 0xE5CB,
            "cleaning_services" to 0xF0FF,
            "colorize" to 0xE3B8,
            "crisis_alert" to 0xEBE9,
            "cruelty_free" to 0xE799, // hunt board: the beast's paw (Stitch)
            "crop_square" to 0xE3C6,
            "directions_run" to 0xE566,
            "directions_walk" to 0xE536,
            "domain" to 0xE7EE,
            "edit_note" to 0xE745,
            "emergency" to 0xE1EB,
            "emergency_heat" to 0xF15D,
            "error" to 0xE000,
            "expand_less" to 0xE5CE,
            "explore" to 0xE87A,
            "face" to 0xF008,
            "family_restroom" to 0xF1A2,
            "favorite" to 0xE87E,
            "fitness_center" to 0xEB43,
            "flare" to 0xE3E4,
            "format_h4" to 0xF860,
            "fort" to 0xEAAD,
            "foundation" to 0xF200, // hunt board: Inicio/Asentamiento (Stitch)
            "flag" to 0xF0C6, // hunt board: Inicio step badge (Stitch)
            "front_hand" to 0xE769,
            "grass" to 0xF205,
            "grid_4x4" to 0xF016,
            "grid_view" to 0xE9B0,
            "group" to 0xEA21,
            "group_add" to 0xE7F0,
            "groups" to 0xF233,
            "handyman" to 0xF10B,
            "hardware" to 0xEA59,
            "healing" to 0xE3F3,
            "home" to 0xE9B2,
            "hotel_class" to 0xE743,
            "hourglass_bottom" to 0xEA5C,
            "hourglass_empty" to 0xE88B,
            "info" to 0xE88E,
            "inventory_2" to 0xE1A1,
            "landscape" to 0xE564,
            "layers" to 0xE53B,
            "light_mode" to 0xE518,
            "lightbulb" to 0xE0F0,
            "local_fire_department" to 0xEF55,
            "lock" to 0xE897,
            "lock_open" to 0xE898,
            "logout" to 0xE9BA,
            "manage_search" to 0xF02F,
            "menu_book" to 0xEA19,
            "no_meals" to 0xF1D6, // hunt board: Starvation/Hambruna (Stitch)
            "nights_stay" to 0xF174, // hunt board: Overwhelming Darkness step badge (Stitch)
            "nutrition" to 0xE110,
            "palette" to 0xE3B7,
            "person" to 0xF0D3,
            "pets" to 0xE91D,
            "psychology" to 0xEA4A,
            "psychology_alt" to 0xF8EA,
            "publish" to 0xE255,
            "radiology" to 0xE125,
            "remove" to 0xE15B,
            "replay" to 0xE042,
            "restaurant" to 0xE56C,
            "restart_alt" to 0xF053,
            "roller_shades" to 0xEC12,
            "route" to 0xEACD,
            "security" to 0xE32A,
            "shield" to 0xE9E0,
            "shield_person" to 0xF650,
            "skull" to 0xF89A,
            "star" to 0xF09A, // beast-event card mark, top-right (Stitch)
            "sports_mma" to 0xEA2C,
            "style" to 0xE41D,
            "swap_horiz" to 0xE8D4,
            "swap_horizontal_circle" to 0xE933,
            "swap_vert" to 0xE8D5,
            "swords" to 0xF889,
            "sync_alt" to 0xEA18,
            "target" to 0xE719,
            "texture" to 0xE421,
            "touch_app" to 0xE913,
            "undo" to 0xE166,
            "unfold_more" to 0xE5D7,
            "verified" to 0xEF76,
            "verified_user" to 0xE8E8,
            "visibility" to 0xE8F4,
            "vital_signs" to 0xE650,
            "warning" to 0xF083,
            "wb_twilight" to 0xE1C6,
        )
}
