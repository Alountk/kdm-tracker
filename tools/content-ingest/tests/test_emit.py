"""Tests for the shared Kotlin emit helpers and stable emission."""

from __future__ import annotations

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

import ingest_innovations as innovations  # noqa: E402
import kotlin_emit  # noqa: E402

FORM_FEED = "\f"
TITLE = "I\n–Innovaciones–"


def card(name: str, subline: str, *body: str) -> str:
    return "\n".join([name, subline, *body])


class KotlinEmitTests(unittest.TestCase):
    def test_normalize_ws_collapses_all_whitespace(self) -> None:
        self.assertEqual("a b c", kotlin_emit.normalize_ws("  a\n\tb   c "))

    def test_kotlin_escape_covers_every_special_character(self) -> None:
        self.assertEqual('a\\"b\\\\c\\$d\\ne\\re\\tf', kotlin_emit.kotlin_escape('a"b\\c$d\ne\re\tf'))

    def test_short_description_is_inlined(self) -> None:
        lines = kotlin_emit.description_lines("hi")

        self.assertEqual(1, len(lines))
        self.assertTrue(lines[0].endswith('description = "hi",'))

    def test_long_description_wraps_with_plus_continuations(self) -> None:
        lines = kotlin_emit.description_lines("palabra " * 40)

        self.assertTrue(lines[0].endswith("description ="))
        self.assertTrue(lines[-1].endswith(","))
        self.assertTrue(any(line.endswith(" +") for line in lines[1:-1]))


class StableEmitTests(unittest.TestCase):
    def _cards(self):
        text = FORM_FEED.join(
            [
                TITLE,
                card(
                    "Alfa",
                    "Innovación inicial",
                    "Effect.",
                    "Añade las consecuencias de alfa al mazo de",
                ),
                card("Zeta", "ciencia, consecuencia de alfa", "Effect."),
            ]
        )
        return innovations.resolve_consequences(innovations.parse_cards(text))

    def test_emit_is_stable_and_entries_are_sorted(self) -> None:
        cards = self._cards()

        first = innovations.emit_kotlin(cards)
        second = innovations.emit_kotlin(cards)

        self.assertEqual(first, second)
        self.assertTrue(first.startswith(innovations.GENERATED_HEADER))
        self.assertLess(first.index('name = "Alfa"'), first.index('name = "Zeta"'))
        self.assertNotIn("timestamp", first.lower())


if __name__ == "__main__":
    unittest.main()
