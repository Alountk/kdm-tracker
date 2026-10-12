"""Fixture-based tests for the innovation parser.

Everything here is pure string processing: ``parse_cards`` and
``resolve_consequences`` take text (never a PDF), so the consequence graph,
the survival-limit cross-check, the anchored deck-prose, and every abort path
are provable without the untracked PDFs.
"""

from __future__ import annotations

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

import ingest_innovations as innovations  # noqa: E402
from identity import load_innovations_identity  # noqa: E402

FORM_FEED = "\f"


def deck(*pages: str) -> str:
    return FORM_FEED.join(pages)


TITLE = "I\n–Innovaciones–"


def card(name: str, subline: str, *body: str) -> str:
    return "\n".join([name, subline, *body])


class ParseCardsTests(unittest.TestCase):
    def test_builds_the_consequence_graph_from_sublines(self) -> None:
        text = deck(
            TITLE,
            card(
                "Lenguaje",
                "Innovación inicial",
                "Effect.",
                "Añade las consecuencias del lenguaje al mazo de",
                "innovaciones.",
            ),
            card("Linterna Interior", "fe, consecuencia del lenguaje", "Effect 2."),
            card("Pintura", "arte, consecuencia del lenguaje", "Effect 3."),
        )

        cards = innovations.resolve_consequences(innovations.parse_cards(text))
        by_name = {card.name: card for card in cards}

        self.assertEqual(["Linterna Interior", "Pintura"], by_name["Lenguaje"].consequences)
        self.assertEqual([], by_name["Linterna Interior"].consequences)
        self.assertEqual("lenguaje", by_name["Linterna Interior"].parent)

    def test_survival_limit_becomes_a_bonus_and_leaves_the_description(self) -> None:
        text = deck(
            TITLE,
            card("Lenguaje", "Innovación inicial", "Límite de Supervivencia +2", "Effect."),
        )

        cards = innovations.parse_cards(text)

        self.assertEqual(2, cards[0].survival_limit_bonus)
        self.assertEqual("Effect.", cards[0].description)

    def test_anchored_footer_does_not_truncate_an_añade_effect_line(self) -> None:
        # Regression for I1: a body line starting with "Añade" that is NOT the footer
        # must not cut the description. Only "Añade … consecuencias" is the footer.
        text = deck(
            TITLE,
            card(
                "Forja",
                "ciencia",
                "Calienta el metal.",
                "Añade 1 Hierro al almacén.",
                "Sigue trabajando.",
                "Añade las consecuencias de forja al mazo de",
                "innovaciones.",
            ),
        )

        cards = innovations.parse_cards(text)

        self.assertTrue(cards[0].has_deck_prose)
        self.assertIn("Añade 1 Hierro al almacén.", cards[0].description)
        self.assertTrue(cards[0].description.endswith("Sigue trabajando."))

    def test_footer_variant_without_las_is_still_recognized(self) -> None:
        # The Amoníaco card prints "Añade consecuencias …" (no "las").
        text = deck(
            TITLE,
            card("Amoníaco", "ciencia", "Effect.", "Añade consecuencias de amoníaco al mazo de"),
        )

        cards = innovations.parse_cards(text)

        self.assertTrue(cards[0].has_deck_prose)
        self.assertNotIn("consecuencias", cards[0].description)

    def test_unrecognized_page_aborts(self) -> None:
        text = deck(TITLE, "not a card\njust two lines")

        with self.assertRaisesRegex(SystemExit, "neither the deck title nor a recognized card"):
            innovations.parse_cards(text)

    def test_card_count_mismatch_aborts(self) -> None:
        # Page 1 is a card rather than the deck title, so parsed != pages - 1.
        text = deck(card("Uno", "ciencia", "Effect."), card("Dos", "ciencia", "Effect."))

        with self.assertRaisesRegex(SystemExit, "a card was dropped or a non-card page was parsed"):
            innovations.parse_cards(text)

    def test_unknown_parent_aborts(self) -> None:
        text = deck(
            TITLE,
            card("Linterna Interior", "fe, consecuencia de nadie", "Effect."),
        )

        cards = innovations.parse_cards(text)
        with self.assertRaisesRegex(SystemExit, "references unknown parent"):
            innovations.resolve_consequences(cards)

    def test_prose_without_children_aborts(self) -> None:
        text = deck(
            TITLE,
            card("Lenguaje", "Innovación inicial", "Effect.", "Añade las consecuencias del lenguaje al mazo de"),
        )

        cards = innovations.parse_cards(text)
        with self.assertRaisesRegex(SystemExit, "graph/prose mismatch"):
            innovations.resolve_consequences(cards)

    def test_survival_limit_count_mismatch_aborts(self) -> None:
        # Two matched limit lines on one card is impossible to satisfy with one bonus.
        text = deck(
            TITLE,
            card(
                "Foo",
                "ciencia",
                "Límite de Supervivencia +1",
                "Effect.",
                "Límite de Supervivencia +1",
                "Añade las consecuencias de foo al mazo de",
            ),
        )

        with self.assertRaisesRegex(SystemExit, "survival-limit mismatch"):
            innovations.parse_cards(text)

    def test_survival_limit_text_leaking_into_description_aborts(self) -> None:
        text = deck(
            TITLE,
            card("Foo", "ciencia", "Límite de Supervivencia", "Effect."),
        )

        with self.assertRaisesRegex(SystemExit, "leaked into the description"):
            innovations.parse_cards(text)

    def test_empty_description_aborts(self) -> None:
        text = deck(
            TITLE,
            card("Foo", "ciencia", "Añade las consecuencias de foo al mazo de"),
        )

        with self.assertRaisesRegex(SystemExit, "empty name or description"):
            innovations.parse_cards(text)


if __name__ == "__main__":
    unittest.main()


def _identity():
    return load_innovations_identity(Path(__file__).resolve().parents[1] / "identity" / "innovations.csv")


class EmitIdentityTests(unittest.TestCase):
    """The emitter resolves every card through the identity CSV before writing Kotlin."""

    @staticmethod
    def _card(name: str = "Lenguaje", consequences=None):
        return innovations.Card(
            name=name,
            subline="innovación inicial",
            parent=None,
            description="Se pueden hablar y registrar lo aprendido.",
            consequences=list(consequences or []),
        )

    def test_emission_carries_ids_and_localized_names(self) -> None:
        out = innovations.emit_kotlin([self._card()], _identity())

        self.assertIn('id = InnovationId("language")', out)
        self.assertIn('names = LocalizedText(es = "Lenguaje", en = "Language")', out)
        self.assertIn("import io.kdmtracker.core.domain.InnovationId", out)
        self.assertNotIn('name = "', out)

    def test_a_card_the_identity_table_does_not_cover_aborts_the_emit(self) -> None:
        with self.assertRaises(SystemExit) as ctx:
            innovations.emit_kotlin([self._card(name="Carta Fantasma")], _identity())

        self.assertIn("Carta Fantasma", str(ctx.exception))

    def test_a_consequence_the_identity_table_does_not_cover_aborts_the_emit(self) -> None:
        with self.assertRaises(SystemExit) as ctx:
            innovations.emit_kotlin([self._card(consequences=["Inexistente"])], _identity())

        self.assertIn("Inexistente", str(ctx.exception))


class InnovationCategoryTests(unittest.TestCase):
    """Every innovation row carries the domain the tree branch it descends from belongs to."""

    @staticmethod
    def _csv_path() -> Path:
        return Path(__file__).resolve().parents[1] / "identity" / "innovations.csv"

    def test_every_identity_row_carries_a_category(self) -> None:
        rows = load_innovations_identity(self._csv_path())

        missing = sorted({row.resource_id for row in rows.values() if not row.category})

        self.assertEqual([], missing, "every innovation needs a category for the deck chips")

    def test_the_emitted_card_carries_its_category(self) -> None:
        identity = load_innovations_identity(self._csv_path())

        out = innovations.emit_kotlin([EmitIdentityTests._card(name="Pintura")], identity)

        self.assertIn('category = "Arte"', out)
