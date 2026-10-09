package io.kdmtracker.app

import io.kdmtracker.app.ui.theme.KdmSymbols
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Pins the Material Symbols codepoints the sheet renders. A wrong codepoint draws a different icon
 * or tofu, so each mapping is asserted here.
 */
class KdmSymbolsTest {
    @Test
    fun glyphMapsEveryDesignedSymbol() {
        val expected =
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

        assertEquals(29, expected.size)
        expected.forEach { (name, code) -> assertEquals(code.toChar().toString(), KdmSymbols.glyph(name), name) }
    }

    @Test
    fun anUnmappedSymbolFailsLoudly() {
        assertFailsWith<IllegalArgumentException> { KdmSymbols.glyph("not_a_real_symbol") }
    }
}
