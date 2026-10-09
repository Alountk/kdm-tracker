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
    fun glyphMapsTheDesignedSymbols() {
        assertEquals("\uE3E4", KdmSymbols.glyph("flare"))
        assertEquals("\uF89A", KdmSymbols.glyph("skull"))
        assertEquals("\uEA4A", KdmSymbols.glyph("psychology"))
        assertEquals("\uEA2C", KdmSymbols.glyph("sports_mma"))
        assertEquals("\uE87A", KdmSymbols.glyph("explore"))
    }

    @Test
    fun anUnmappedSymbolFailsLoudly() {
        assertFailsWith<IllegalArgumentException> { KdmSymbols.glyph("not_a_real_symbol") }
    }
}
