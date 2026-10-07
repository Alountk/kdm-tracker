"""Fixture-based tests for the fighting-arts and disorder parsers.

The pure page recognizers run on text; the PDF subprocess wrappers are monkeypatched
so no PDF is needed.
"""

from __future__ import annotations

import sys
import unittest
from pathlib import Path
from unittest import mock

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

import ingest_fighting_arts as fa  # noqa: E402

FORM_FEED = "\f"


def deck(*pages: str) -> str:
    return FORM_FEED.join(pages)


def run(func, text: str, npages: int):
    with mock.patch.object(fa, "pdf_text", return_value=text), mock.patch.object(
        fa, "pdf_info", return_value={"Pages": str(npages)}
    ):
        return func(Path("deck.pdf"))


class FightingArtsTests(unittest.TestCase):
    def test_parses_an_art_card_between_type_line_and_footer(self) -> None:
        text = deck(
            "Artes de Combate",
            "Duro\n- arte de combate -\nCura lesiones graves.\nAnota y retira",
        )

        entries = run(fa.parse_fighting_arts, text, 2)

        self.assertEqual(1, len(entries))
        self.assertEqual("Duro", entries[0].name)
        self.assertEqual("Cura lesiones graves.", entries[0].description)

    def test_cepa_variant_is_recognized(self) -> None:
        text = deck(
            "Artes de Combate",
            "Cepa Rara\n- arte de combate, cepa -\nEffect.\nAnota y retira",
        )

        entries = run(fa.parse_fighting_arts, text, 2)

        self.assertEqual("Cepa Rara", entries[0].name)

    def test_missing_footer_aborts(self) -> None:
        text = deck(
            "Artes de Combate",
            "Duro\n- arte de combate -\nCura lesiones graves.",
        )

        with self.assertRaisesRegex(SystemExit, "does not end with"):
            run(fa.parse_fighting_arts, text, 2)

    def test_unrecognized_type_line_aborts(self) -> None:
        text = deck(
            "Artes de Combate",
            "Duro\nnot a type line\nEffect.\nAnota y retira",
        )

        with self.assertRaisesRegex(SystemExit, "not a fighting-art card"):
            run(fa.parse_fighting_arts, text, 2)

    def test_page_count_mismatch_aborts(self) -> None:
        text = deck("Artes de Combate", "Duro\n- arte de combate -\nEffect.\nAnota y retira")

        with self.assertRaisesRegex(SystemExit, "page split mismatch"):
            run(fa.parse_fighting_arts, text, 99)


class DisorderTests(unittest.TestCase):
    def test_parses_a_disorder_card_up_to_the_footer(self) -> None:
        text = deck(
            "Trastornos",
            "Acaparador\nDebe acumular.\nAnota y retira",
        )

        entries = run(fa.parse_disorders, text, 2)

        self.assertEqual("Acaparador", entries[0].name)
        self.assertEqual("Debe acumular.", entries[0].description)

    def test_missing_footer_aborts(self) -> None:
        text = deck(
            "Trastornos",
            "Acaparador\nDebe acumular.",
        )

        with self.assertRaisesRegex(SystemExit, "not a disorder card"):
            run(fa.parse_disorders, text, 2)


class DedupeTests(unittest.TestCase):
    def test_empty_name_or_description_aborts(self) -> None:
        with self.assertRaisesRegex(SystemExit, "empty name or description"):
            fa.dedupe_sorted([fa.Entry(name="", description="x")], "arts")

    def test_duplicate_with_different_description_aborts(self) -> None:
        entries = [fa.Entry(name="Duro", description="a"), fa.Entry(name="Duro", description="b")]

        with self.assertRaisesRegex(SystemExit, "appears twice with different descriptions"):
            fa.dedupe_sorted(entries, "arts")

    def test_dedupe_sorted_orders_by_name(self) -> None:
        entries = [fa.Entry(name="Zeta", description="z"), fa.Entry(name="Alfa", description="a")]

        self.assertEqual(["Alfa", "Zeta"], [entry.name for entry in fa.dedupe_sorted(entries, "arts")])


if __name__ == "__main__":
    unittest.main()
