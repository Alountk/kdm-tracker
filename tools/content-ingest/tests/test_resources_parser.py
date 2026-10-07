"""Fixture-based tests for the resource parser.

Covers the pure tag helpers, the deliberate Alimaña -> SPECIAL judgement, the
`???` literal card, and the page recognizers (with the PDF wrappers monkeypatched).
"""

from __future__ import annotations

import sys
import unittest
from pathlib import Path
from unittest import mock

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

import ingest_resources as resources  # noqa: E402

FORM_FEED = "\f"


def deck(*pages: str) -> str:
    return FORM_FEED.join(pages)


def run_parse_deck(text: str, npages: int, category: str = "BASIC"):
    with mock.patch.object(resources, "pdf_text", return_value=text), mock.patch.object(
        resources, "pdf_info", return_value={"Pages": str(npages)}
    ):
        return resources.parse_deck(Path("deck.pdf"), category)


class TagHelperTests(unittest.TestCase):
    def test_looks_like_tags_accepts_known_vocabulary(self) -> None:
        self.assertTrue(resources.looks_like_tags("órgano, hueso, piel, consumible"))
        self.assertFalse(resources.looks_like_tags("not, a, tag"))

    def test_looks_like_tags_rejects_an_empty_line(self) -> None:
        self.assertFalse(resources.looks_like_tags(""))

    def test_deck_title_page_detects_the_section_header(self) -> None:
        self.assertTrue(resources.deck_title_page(["-Recursos Básicos-"]))
        self.assertFalse(resources.deck_title_page(["Hueso", "hueso"]))


class AlimanaJudgementTests(unittest.TestCase):
    def test_alimana_deck_is_deliberately_mapped_to_special(self) -> None:
        # Alimañas (vermin) have no VERMIN enum member; their deck ships the "strange"
        # consumables, so SPECIAL is the deliberate choice. Pinned so it stays deliberate.
        self.assertEqual("SPECIAL", resources.DECK_CATEGORIES["AF Rec Alimaña.pdf"])


class ParseDeckTests(unittest.TestCase):
    def test_parses_a_card_with_its_deck_category(self) -> None:
        text = deck("-Recursos Básicos-", "Hueso de Monstruo\nórgano, hueso, piel")

        cards = run_parse_deck(text, 2, "BASIC")

        self.assertEqual([("Hueso de Monstruo", "BASIC")], cards)

    def test_literal_question_mark_card_is_kept_verbatim(self) -> None:
        text = deck("-Recursos Básicos-", "???\nórgano, hueso, piel, consumible")

        cards = run_parse_deck(text, 2, "BASIC")

        self.assertEqual("???", cards[0][0])

    def test_unrecognized_type_line_aborts(self) -> None:
        text = deck("-Recursos Básicos-", "Foo\nnot a tag line")

        with self.assertRaisesRegex(SystemExit, "unrecognized type line"):
            run_parse_deck(text, 2)

    def test_page_count_mismatch_aborts(self) -> None:
        text = deck("-Recursos Básicos-", "Hueso de Monstruo\nórgano")

        with self.assertRaisesRegex(SystemExit, "page split mismatch"):
            run_parse_deck(text, 99)

    def test_emit_marks_the_question_mark_card(self) -> None:
        output = resources.emit_kotlin([("???", "BASIC")])

        self.assertIn("not a parse placeholder", output)
        self.assertIn('name = "???"', output)


if __name__ == "__main__":
    unittest.main()
