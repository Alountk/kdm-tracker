"""Tests for the reproducible Material Symbols subset script.

The subset was once regenerated from a throwaway venv with no checked-in script, so nobody could
repeat it after the glyph map changed. `subset_material_symbols.py` owns that recipe; these tests
pin its stdlib half (parse the Kotlin map, read the TTF cmap, diff the two) against the real files
in this repo, which is exactly what the script's `--check` mode runs.
"""

from __future__ import annotations

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

import subset_material_symbols as script  # noqa: E402

REPO = Path(__file__).resolve().parents[3]
KOTLIN_MAP = REPO / "app-android/src/main/kotlin/io/kdmtracker/app/ui/theme/KdmSymbols.kt"
BUNDLED_TTF = REPO / "app-android/src/main/res/font/material_symbols_outlined.ttf"


class ParseKotlinMapTests(unittest.TestCase):
    def test_parses_every_entry_of_the_real_map(self) -> None:
        entries = script.parse_kotlin_map(KOTLIN_MAP.read_text(encoding="utf-8"))
        self.assertEqual(88, len(entries))

    def test_reads_codepoints_as_integers(self) -> None:
        entries = script.parse_kotlin_map(
            'private val CODEPOINTS: Map<String, Int> =\n    mapOf(\n        "check" to 0xE5CA,\n    )\n'
        )
        self.assertEqual({"check": 0xE5CA}, entries)

    def test_rejects_a_map_it_cannot_find(self) -> None:
        with self.assertRaises(ValueError):
            script.parse_kotlin_map("nothing to see here")


class CmapTests(unittest.TestCase):
    def test_reads_the_bundled_subset_cmap(self) -> None:
        codepoints = script.cmap_codepoints(BUNDLED_TTF.read_bytes())
        self.assertEqual(88, len(codepoints))

    def test_rejects_a_file_without_cmap(self) -> None:
        with self.assertRaises(ValueError):
            script.cmap_codepoints(b"not a font")


class CheckTests(unittest.TestCase):
    def test_bundled_font_and_kotlin_map_agree(self) -> None:
        entries = script.parse_kotlin_map(KOTLIN_MAP.read_text(encoding="utf-8"))
        codepoints = script.cmap_codepoints(BUNDLED_TTF.read_bytes())
        missing = sorted(set(entries.values()) - codepoints)
        extra = sorted(codepoints - set(entries.values()))
        self.assertEqual([], missing)
        self.assertEqual([], extra)

    def test_diff_names_each_divergent_codepoint(self) -> None:
        diff = script.diff(entries={"a": 0xE000}, codepoints={0xE110})
        self.assertEqual("missing from the TTF: U+E000", diff[0])
        self.assertEqual("unmapped in the TTF: U+E110", diff[1])


if __name__ == "__main__":
    unittest.main()
