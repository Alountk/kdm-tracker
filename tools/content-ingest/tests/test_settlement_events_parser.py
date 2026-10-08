"""Fixture-based tests for the settlement-events parser.

The pure page recognizer runs on text; the PDF subprocess wrappers are
monkeypatched so no PDF is needed.
"""

from __future__ import annotations

import sys
import unittest
from pathlib import Path
from unittest import mock

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

import ingest_settlement_events as se  # noqa: E402

FORM_FEED = "\f"
MARKER = "- evento de asentamiento -"
COVER = "A\n–Eventos de Asentamiento–"


def deck(*pages: str) -> str:
    return FORM_FEED.join(pages)


def card(name: str, *body: str) -> str:
    return "\n".join([name, MARKER, *body])


def run(func, text: str, npages: int):
    with mock.patch.object(se, "pdf_text", return_value=text), mock.patch.object(
        se, "pdf_info", return_value={"Pages": str(npages)}
    ):
        return func(Path("deck.pdf"))


class ParseEventsTests(unittest.TestCase):
    def test_parses_name_and_text_from_each_card_page(self) -> None:
        text = deck(
            COVER,
            card("Rivalidad", "Línea uno.", "Línea dos."),
            card("Plaga", "La enfermedad."),
        )

        events = run(se.parse_events, text, 3)

        self.assertEqual(2, len(events))
        self.assertEqual("Rivalidad", events[0].name)
        self.assertEqual("Línea uno. Línea dos.", events[0].description)

    def test_title_page_is_skipped(self) -> None:
        text = deck(COVER, card("Rivalidad", "x."))

        events = run(se.parse_events, text, 2)

        self.assertEqual(["Rivalidad"], [event.name for event in events])

    def test_page_count_mismatch_aborts(self) -> None:
        text = deck(COVER, card("Rivalidad", "x."))

        with self.assertRaises(SystemExit):
            run(se.parse_events, text, 3)

    def test_missing_marker_aborts(self) -> None:
        text = deck(COVER, "Rivalidad\nno marker here\nbody")

        with self.assertRaises(SystemExit):
            run(se.parse_events, text, 2)

    def test_cover_page_that_looks_like_a_card_aborts(self) -> None:
        text = deck(card("Rivalidad", "x."), card("Plaga", "y."))

        with self.assertRaises(SystemExit):
            run(se.parse_events, text, 2)


class DedupeTests(unittest.TestCase):
    def test_duplicate_names_with_different_text_abort(self) -> None:
        events = [se.Entry("Plaga", "a"), se.Entry("Plaga", "b")]

        with self.assertRaises(SystemExit):
            se.dedupe_sorted(events, "settlement events")

    def test_duplicate_names_with_same_text_dedup_and_sort(self) -> None:
        events = [se.Entry("Plaga", "a"), se.Entry("Plaga", "a"), se.Entry("Rivalidad", "b")]

        result = se.dedupe_sorted(events, "settlement events")

        self.assertEqual(["Plaga", "Rivalidad"], [event.name for event in result])


if __name__ == "__main__":
    unittest.main()
