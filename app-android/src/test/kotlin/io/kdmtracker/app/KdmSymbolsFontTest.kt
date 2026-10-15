package io.kdmtracker.app

import io.kdmtracker.app.ui.theme.KdmSymbols
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * Holds the bundled icon subset against the pinned glyph vocabulary.
 *
 * [KdmSymbolsTest] pins the *map*: it proves `glyph(name)` returns the codepoint the design wants,
 * but nothing connects that map to the TTF actually shipped in `res/font`. Sixteen glyphs once
 * shipped missing while every map assertion stayed green — the count said 86 and the font
 * disagreed. This test reads the font's own `cmap` and requires the two to be the same set:
 * a codepoint the font lacks renders tofu (and `glyph` will happily return it), and a codepoint the
 * font carries that nobody maps is dead weight in the subset.
 */
class KdmSymbolsFontTest {
    @Test
    fun theBundledSubsetCarriesExactlyThePinnedCodepoints() {
        val inFont = codepointsInBundledFont()
        val pinned = designedGlyphs.values.toSet()

        val missing = (pinned - inFont).sorted()
        val unmapped = (inFont - pinned).sorted()
        if (missing.isNotEmpty()) {
            fail(
                "codepoints the bundled TTF does not contain: ${missing.joinToString { hex(it) }}",
            )
        }
        if (unmapped.isNotEmpty()) {
            fail(
                "codepoints in the bundled TTF that no glyph maps: ${unmapped.joinToString { hex(it) }}",
            )
        }
    }

    @Test
    fun thePinnedVocabularyCoversEveryMappedGlyph() {
        assertEquals(
            KdmSymbols.glyphCount,
            designedGlyphs.size,
            "designedGlyphs pins a different number of glyphs than KdmSymbols maps",
        )
    }

    private fun hex(code: Int) = "U+%04X".format(code)

    /** The `cmap` codepoints of `res/font/material_symbols_outlined.ttf`, read straight from disk. */
    private fun codepointsInBundledFont(): Set<Int> = cmapCodepoints(bundledFont().readBytes())

    /**
     * Locates the bundled TTF from the test's working directory (the `app-android` module dir under
     * Gradle, the repo root when run by hand).
     */
    private fun bundledFont(): File {
        val rel = "src/main/res/font/material_symbols_outlined.ttf"
        var dir: File? = File(System.getProperty("user.dir") ?: ".").absoluteFile
        repeat(4) {
            if (dir != null) {
                val candidate = File(dir, rel)
                if (candidate.isFile) return candidate
                val fromRoot = File(dir, "app-android/$rel")
                if (fromRoot.isFile) return fromRoot
                dir = dir.parentFile
            }
        }
        fail("bundled icon font not found above ${System.getProperty("user.dir")}: checked $rel")
    }

    /**
     * Minimal TrueType `cmap` reader: every codepoint that resolves to a non-zero glyph in any
     * subtable (formats 4 and 12, the only ones a subsetted font carries). No dependencies — the
     * whole point is to hold the shipped bytes against the map without trusting a tool that could
     * be missing from the machine running the suite.
     */
    private fun cmapCodepoints(data: ByteArray): Set<Int> {
        fun u16(offset: Int): Int = ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)

        fun u32(offset: Int): Int = (u16(offset) shl 16) or u16(offset + 2)

        val tableCount = u16(4)
        var cmap = -1
        for (i in 0 until tableCount) {
            val record = 12 + i * 16
            if (String(data, record, 4) == "cmap") cmap = u32(record + 8)
        }
        if (cmap < 0) fail("the bundled TTF has no cmap table")

        val subtables = u16(cmap + 2)
        val codepoints = mutableSetOf<Int>()
        for (i in 0 until subtables) {
            val record = cmap + 4 + i * 8
            val subtable = cmap + u32(record + 4)
            when (u16(subtable)) {
                4 -> codepoints += format4(data, subtable)
                12 -> codepoints += format12(data, subtable)
            }
        }
        if (codepoints.isEmpty()) fail("no readable cmap subtable (expected format 4 or 12)")
        return codepoints
    }

    private fun format4(
        data: ByteArray,
        at: Int,
    ): Set<Int> {
        fun u16(offset: Int): Int = ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)

        fun i16(offset: Int): Int = (data[offset].toInt() shl 8) or (data[offset + 1].toInt() and 0xFF)

        val segCount = u16(at + 6) / 2
        val ends = at + 14
        val starts = ends + segCount * 2 + 2
        val deltas = starts + segCount * 2
        val rangeOffsets = deltas + segCount * 2
        val found = mutableSetOf<Int>()
        for (segment in 0 until segCount) {
            val start = u16(starts + segment * 2)
            val end = u16(ends + segment * 2)
            if (start > end || end == 0xFFFF) continue
            val delta = i16(deltas + segment * 2)
            val rangeOffset = u16(rangeOffsets + segment * 2)
            for (codepoint in start..end) {
                val glyph =
                    if (rangeOffset == 0) {
                        (codepoint + delta) and 0xFFFF
                    } else {
                        val index = rangeOffsets + segment * 2 + rangeOffset + (codepoint - start) * 2
                        val raw = u16(index)
                        if (raw == 0) 0 else (raw + delta) and 0xFFFF
                    }
                if (glyph != 0) found += codepoint
            }
        }
        return found
    }

    private fun format12(
        data: ByteArray,
        at: Int,
    ): Set<Int> {
        fun u32(offset: Int): Int {
            fun u16(o: Int): Int = ((data[o].toInt() and 0xFF) shl 8) or (data[o + 1].toInt() and 0xFF)
            return (u16(offset) shl 16) or u16(offset + 2)
        }

        val groups = u32(at + 12)
        val found = mutableSetOf<Int>()
        for (group in 0 until groups) {
            val record = at + 16 + group * 12
            val start = u32(record)
            val end = u32(record + 4)
            val firstGlyph = u32(record + 8)
            if (firstGlyph != 0) found += (start..end)
        }
        return found
    }
}
