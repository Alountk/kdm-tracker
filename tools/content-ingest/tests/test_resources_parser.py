"""Fixture-based tests for the resource parser.

Covers the pure tag helpers, the deliberate Alimaña -> SPECIAL judgement, the
`???` literal card, the page recognizers (with the PDF wrappers monkeypatched), the identity CSV,
and the material derivation that decides a card's settlement-summary bucket.
"""

from __future__ import annotations

import sys
import tempfile
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


def identity_row(material: str = "", decided: bool = False) -> resources.IdentityRow:
    return resources.IdentityRow(
        resource_id="x",
        english_name="",
        material=material or None,
        material_decided=decided,
    )


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
    def test_parses_a_card_with_its_deck_category_and_its_tags(self) -> None:
        text = deck("-Recursos Básicos-", "Hueso de Monstruo\nórgano, hueso, piel")

        cards = run_parse_deck(text, 2, "BASIC")

        self.assertEqual(
            [
                resources.Resource(
                    name="Hueso de Monstruo",
                    category="BASIC",
                    tags=("órgano", "hueso", "piel"),
                )
            ],
            cards,
        )

    def test_literal_question_mark_card_is_kept_verbatim(self) -> None:
        text = deck("-Recursos Básicos-", "???\nórgano, hueso, piel, consumible")

        cards = run_parse_deck(text, 2, "BASIC")

        self.assertEqual("???", cards[0].name)

    def test_unrecognized_type_line_aborts(self) -> None:
        text = deck("-Recursos Básicos-", "Foo\nnot a tag line")

        with self.assertRaisesRegex(SystemExit, "unrecognized type line"):
            run_parse_deck(text, 2)

    def test_page_count_mismatch_aborts(self) -> None:
        text = deck("-Recursos Básicos-", "Hueso de Monstruo\nórgano")

        with self.assertRaisesRegex(SystemExit, "page split mismatch"):
            run_parse_deck(text, 99)


class MaterialForTests(unittest.TestCase):
    def test_one_primary_tag_decides_the_bucket(self) -> None:
        self.assertEqual("BONE", resources.material_for("Garra", ("hueso", "arma"), identity_row()))

    def test_no_primary_tag_means_the_card_feeds_no_bucket(self) -> None:
        self.assertIsNone(resources.material_for("Acanto", ("hierba",), identity_row()))

    def test_an_undecided_ambiguous_card_aborts(self) -> None:
        with self.assertRaisesRegex(SystemExit, "must set material or 'none'"):
            resources.material_for("Segundo Corazón", ("hueso", "órgano"), identity_row())

    def test_a_decided_ambiguous_card_takes_the_csv_material(self) -> None:
        row = identity_row(material="ORGAN", decided=True)

        self.assertEqual("ORGAN", resources.material_for("Segundo Corazón", ("hueso", "órgano"), row))

    def test_an_explicit_none_means_the_ambiguous_card_feeds_no_bucket(self) -> None:
        row = identity_row(decided=True)

        self.assertIsNone(resources.material_for("Liquen", ("hueso", "órgano", "piel"), row))


class LoadIdentityTests(unittest.TestCase):
    def csv(self, body: str) -> Path:
        handle = tempfile.NamedTemporaryFile("w", suffix=".csv", delete=False, encoding="utf-8")
        handle.write("id,spanishName,englishName,material\n")
        handle.write(body)
        handle.close()
        self.addCleanup(Path(handle.name).unlink)
        return Path(handle.name)

    def test_reads_ids_english_names_and_materials(self) -> None:
        rows = resources.load_identity(self.csv("bone.monster,Hueso de Monstruo,Monster Bone,BONE\n"))

        row = rows["Hueso de Monstruo"]
        self.assertEqual("bone.monster", row.resource_id)
        self.assertEqual("Monster Bone", row.english_name)
        self.assertEqual("BONE", row.material)
        self.assertTrue(row.material_decided)

    def test_none_is_a_deliberate_no_bucket_not_an_empty_cell(self) -> None:
        rows = resources.load_identity(self.csv("herb.black_lichen,Liquen Negro,,none\n"))

        self.assertIsNone(rows["Liquen Negro"].material)
        self.assertTrue(rows["Liquen Negro"].material_decided)

    def test_an_empty_material_cell_is_undecided(self) -> None:
        rows = resources.load_identity(self.csv("iron,Hierro,,\n"))

        self.assertFalse(rows["Hierro"].material_decided)

    def test_an_unknown_material_aborts(self) -> None:
        with self.assertRaisesRegex(SystemExit, "unknown material"):
            resources.load_identity(self.csv("iron,Hierro,,PLUTONIUM\n"))

    def test_a_repeated_spanish_name_aborts(self) -> None:
        with self.assertRaisesRegex(SystemExit, "repeats"):
            resources.load_identity(self.csv("a,Hierro,,\nb,Hierro,,\n"))


class EmitTests(unittest.TestCase):
    def test_emits_the_definition_with_its_id_names_and_material(self) -> None:
        resource = resources.Resource(name="Pelaje Blanco", category="MONSTER", tags=("piel",))
        identity = {
            "Pelaje Blanco": resources.IdentityRow(
                resource_id="hide.white_lion",
                english_name="White Lion Fur",
                material=None,
                material_decided=False,
            )
        }

        output = resources.emit_kotlin([resource], identity)

        self.assertIn('id = ResourceId("hide.white_lion")', output)
        self.assertIn('names = LocalizedText(es = "Pelaje Blanco", en = "White Lion Fur")', output)
        self.assertIn("category = ResourceCategory.MONSTER", output)
        self.assertIn("material = ResourceMaterial.HIDE", output)

    def test_emits_a_null_material_for_a_card_that_feeds_no_bucket(self) -> None:
        resource = resources.Resource(name="Hierro", category="SPECIAL", tags=("hierro",))
        identity = {"Hierro": resources.IdentityRow("iron", "", None, False)}

        output = resources.emit_kotlin([resource], identity)

        self.assertIn("material = null", output)

    def test_marks_the_question_mark_card(self) -> None:
        resource = resources.Resource(name="???", category="BASIC", tags=("hueso", "órgano", "piel"))
        identity = {"???": resources.IdentityRow("unknown.monster_part", "", None, True)}

        output = resources.emit_kotlin([resource], identity)

        self.assertIn("not a parse placeholder", output)
        self.assertIn('names = LocalizedText(es = "???")', output)

    def test_omits_the_english_name_when_the_card_has_none(self) -> None:
        resource = resources.Resource(name="Hierro", category="SPECIAL", tags=("hierro",))
        identity = {"Hierro": resources.IdentityRow("iron", "", None, False)}

        output = resources.emit_kotlin([resource], identity)

        self.assertIn('names = LocalizedText(es = "Hierro")', output)
        self.assertNotIn("en = ", output)


if __name__ == "__main__":
    unittest.main()
