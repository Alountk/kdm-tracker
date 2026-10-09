"""Fixture-based tests for the monster showdown parser.

The pure page recognizers run on text; the PDF subprocess wrappers are monkeypatched.
"""

from __future__ import annotations

import sys
import unittest
from pathlib import Path
from unittest import mock

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

import ingest_monsters as mo  # noqa: E402

FORM_FEED = "\f"


def deck(*pages: str) -> str:
    return FORM_FEED.join(pages)


def run(func, text: str, npages: int):
    with mock.patch.object(mo, "pdf_text", return_value=text), mock.patch.object(
        mo, "pdf_info", return_value={"Pages": str(npages)}
    ):
        return func(Path("deck.pdf"))


class AiDeckTests(unittest.TestCase):
    def test_parses_an_ai_card_with_quantity_and_rank(self) -> None:
        text = deck("IA\nLeón Blanco", "x2\nB\nAtrapar\nSelección de Objetivo.")

        entries = run(lambda p: mo.parse_ai_deck(p, monster="White Lion"), text, 2)

        self.assertEqual(1, len(entries))
        self.assertEqual("Atrapar", entries[0].name)
        self.assertEqual("AI", entries[0].kind)
        self.assertEqual("B", entries[0].rank)
        self.assertEqual("White Lion", entries[0].monster)

    def test_parses_an_ai_card_without_quantity(self) -> None:
        text = deck("IA\nLeón Blanco", "A\nMutilar\nSelección de Objetivo.")

        entries = run(lambda p: mo.parse_ai_deck(p, monster="White Lion"), text, 2)

        self.assertEqual("Mutilar", entries[0].name)
        self.assertEqual("A", entries[0].rank)

    def test_unknown_rank_aborts(self) -> None:
        text = deck("IA\nLeón Blanco", "Z\nNope\nbody")

        with self.assertRaises(SystemExit):
            run(lambda p: mo.parse_ai_deck(p, monster="White Lion"), text, 2)

    def test_skips_the_monster_stat_sheet(self) -> None:
        text = deck(
            "IA\nCarnicero",
            "Carnicero\nNivel   B   A   L   Mov   Res",
            "B\nTajos Salvajes\nbody",
        )

        entries = run(lambda p: mo.parse_ai_deck(p, monster="The Butcher"), text, 3)

        self.assertEqual(["Tajos Salvajes"], [e.name for e in entries])

    def test_parses_the_basic_action_card(self) -> None:
        text = deck("IA\nCarnicero", "Acción Básica\nSelección de Objetivo\nsuperviviente más cercano.")

        entries = run(lambda p: mo.parse_ai_deck(p, monster="The Butcher"), text, 2)

        self.assertEqual("BASIC_ACTION", entries[0].kind)
        self.assertEqual("Acción Básica", entries[0].name)


class HitLocationTests(unittest.TestCase):
    def test_parses_a_hit_location(self) -> None:
        text = deck("LI\nLeón Blanco", "Bajo Vientre\nUn golpe debilitante.")

        entries = run(lambda p: mo.parse_hit_locations(p, monster="White Lion"), text, 2)

        self.assertEqual(1, len(entries))
        self.assertEqual("Bajo Vientre", entries[0].name)
        self.assertEqual("HIT_LOCATION", entries[0].kind)
        self.assertIsNone(entries[0].rank)

    def test_page_count_mismatch_aborts(self) -> None:
        text = deck("LI\nLeón Blanco", "Bajo Vientre\ntext")

        with self.assertRaises(SystemExit):
            run(lambda p: mo.parse_hit_locations(p, monster="White Lion"), text, 3)


class DedupeTests(unittest.TestCase):
    def test_duplicate_ai_cards_collapse_to_one(self) -> None:
        entries = [mo.Entry("Atrapar", "t", "AI", "White Lion", "B")] * 2 + [
            mo.Entry("Mutilar", "t", "AI", "White Lion", "A")
        ]

        result = mo.dedupe_sorted(entries, "monster cards")

        self.assertEqual(["Atrapar", "Mutilar"], [e.name for e in result])


if __name__ == "__main__":
    unittest.main()
