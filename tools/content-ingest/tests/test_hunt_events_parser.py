"""Fixture-based tests for the hunt-events parser.

The pure page recognizer runs on text; the PDF subprocess wrappers are monkeypatched.
"""

from __future__ import annotations

import sys
import unittest
from pathlib import Path
from unittest import mock

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

import ingest_hunt_events as he  # noqa: E402

FORM_FEED = "\f"
BASIC = "- evento de caza básico -"
SPECIAL = "- evento de caza especial -"
LION = "- evento de caza de león blanco -"


def deck(*pages: str) -> str:
    return FORM_FEED.join(pages)


def card(name: str, subline: str, *body: str) -> str:
    return "\n".join([name, subline, *body])


def run(func, text: str, npages: int):
    with mock.patch.object(he, "pdf_text", return_value=text), mock.patch.object(
        he, "pdf_info", return_value={"Pages": str(npages)}
    ):
        return func(Path("deck.pdf"))


class ParseDeckTests(unittest.TestCase):
    def test_parses_a_basic_card(self) -> None:
        text = deck("C", card("Guerrero Muerto", BASIC, "Body line."))

        entries = run(lambda p: he.parse_deck(p, monster=None, expected_subline=None), text, 2)

        self.assertEqual(1, len(entries))
        self.assertEqual("Guerrero Muerto", entries[0].name)
        self.assertEqual("BASIC", entries[0].kind)
        self.assertEqual(None, entries[0].monster)
        self.assertEqual("Body line.", entries[0].description)

    def test_recognizes_the_special_kind(self) -> None:
        text = deck("C", card("Recolección de Hierbas", SPECIAL, "Ganáis +1 a supervivencia."))

        entries = run(lambda p: he.parse_deck(p, monster=None, expected_subline=None), text, 2)

        self.assertEqual("SPECIAL", entries[0].kind)

    def test_recognizes_a_monster_card_and_tags_its_deck_monster(self) -> None:
        text = deck("C", card("Mar de Hierba Dorada", LION, "Body."))

        entries = run(lambda p: he.parse_deck(p, monster="White Lion", expected_subline=LION), text, 2)

        self.assertEqual("MONSTER", entries[0].kind)
        self.assertEqual("White Lion", entries[0].monster)

    def test_page_count_mismatch_aborts(self) -> None:
        text = deck("C", card("X", BASIC, "b"))

        with self.assertRaises(SystemExit):
            run(lambda p: he.parse_deck(p, monster=None, expected_subline=None), text, 3)

    def test_unknown_subline_aborts(self) -> None:
        text = deck("C", card("X", "- not a known type -", "b"))

        with self.assertRaises(SystemExit):
            run(lambda p: he.parse_deck(p, monster=None, expected_subline=None), text, 2)


class DedupeTests(unittest.TestCase):
    def test_duplicate_random_event_cards_collapse_to_one(self) -> None:
        entries = [he.Entry("Evento Aleatorio", "Tira un d100.", "BASIC", None)] * 12 + [
            he.Entry("Guerrero Muerto", "b", "BASIC", None)
        ]

        result = he.dedupe_sorted(entries, "hunt events")

        self.assertEqual(["Evento Aleatorio", "Guerrero Muerto"], [e.name for e in result])

    def test_same_name_with_different_text_aborts(self) -> None:
        entries = [he.Entry("X", "a", "BASIC", None), he.Entry("X", "b", "BASIC", None)]

        with self.assertRaises(SystemExit):
            he.dedupe_sorted(entries, "hunt events")


if __name__ == "__main__":
    unittest.main()
