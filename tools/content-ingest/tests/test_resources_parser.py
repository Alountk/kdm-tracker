"""Fixture-based tests for the resource parser.

Covers the pure tag helpers, the deliberate Alimaña -> SPECIAL judgement, the
`???` literal card, the page recognizers (with the PDF wrappers monkeypatched), the identity CSV,
and the material derivation that decides a card's settlement-summary buckets.
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


class MaterialsForTests(unittest.TestCase):
    def test_one_primary_tag_decides_the_bucket(self) -> None:
        self.assertEqual(frozenset({"BONE"}), resources.materials_for(("hueso", "arma")))

    def test_no_primary_tag_means_the_card_feeds_no_bucket(self) -> None:
        self.assertEqual(frozenset(), resources.materials_for(("hierba",)))
        self.assertEqual(frozenset(), resources.materials_for(("alimaña", "consumible")))

    def test_several_primary_tags_make_the_card_a_wildcard(self) -> None:
        # The `???` card: it pays a bone, a hide or an organ.
        self.assertEqual(
            frozenset({"BONE", "HIDE", "ORGAN"}),
            resources.materials_for(("órgano", "hueso", "piel", "consumible")),
        )

    def test_a_non_primary_tag_alongside_a_primary_one_does_not_widen_the_set(self) -> None:
        self.assertEqual(frozenset({"SCRAP"}), resources.materials_for(("restos", "otro")))


class LoadIdentityTests(unittest.TestCase):
    def csv(self, body: str) -> Path:
        handle = tempfile.NamedTemporaryFile("w", suffix=".csv", delete=False, encoding="utf-8")
        handle.write("id,spanishName,englishName\n")
        handle.write(body)
        handle.close()
        self.addCleanup(Path(handle.name).unlink)
        return Path(handle.name)

    def test_reads_ids_and_optional_english_names(self) -> None:
        rows = resources.load_identity(self.csv("bone.monster,Hueso de Monstruo,Monster Bone\n"))

        row = rows["Hueso de Monstruo"]
        self.assertEqual("bone.monster", row.resource_id)
        self.assertEqual("Monster Bone", row.english_name)

    def test_an_empty_english_name_is_allowed(self) -> None:
        rows = resources.load_identity(self.csv("iron,Hierro,\n"))

        self.assertEqual("", rows["Hierro"].english_name)

    def test_a_repeated_spanish_name_aborts(self) -> None:
        with self.assertRaisesRegex(SystemExit, "repeats"):
            resources.load_identity(self.csv("a,Hierro,\nb,Hierro,\n"))

    def test_a_wrong_header_aborts(self) -> None:
        handle = tempfile.NamedTemporaryFile("w", suffix=".csv", delete=False, encoding="utf-8")
        handle.write("id,name\n")
        handle.close()
        self.addCleanup(Path(handle.name).unlink)

        with self.assertRaisesRegex(SystemExit, "header"):
            resources.load_identity(Path(handle.name))


class EmitTests(unittest.TestCase):
    def identity(self, name: str, resource_id: str, english: str = "") -> dict:
        return {
            name: resources.IdentityRow(
                resource_id=resource_id,
                english_name=english,
            )
        }

    def test_emits_the_definition_with_its_id_names_and_materials(self) -> None:
        resource = resources.Resource(name="Pelaje Blanco", category="MONSTER", tags=("piel",))
        identity = self.identity("Pelaje Blanco", "hide.white_lion", "White Lion Fur")

        output = resources.emit_kotlin([resource], identity)

        self.assertIn('id = ResourceId("hide.white_lion")', output)
        self.assertIn('names = LocalizedText(es = "Pelaje Blanco", en = "White Lion Fur")', output)
        self.assertIn("category = ResourceCategory.MONSTER", output)
        self.assertIn("materials = setOf(ResourceMaterial.HIDE),", output)

    def test_emits_an_empty_set_for_a_card_that_feeds_no_bucket(self) -> None:
        resource = resources.Resource(name="Hierro", category="SPECIAL", tags=("hierro",))

        output = resources.emit_kotlin([resource], self.identity("Hierro", "iron"))

        self.assertIn("materials = emptySet(),", output)

    def test_emits_every_material_of_a_wildcard(self) -> None:
        resource = resources.Resource(name="???", category="BASIC", tags=("órgano", "hueso", "piel"))

        output = resources.emit_kotlin([resource], self.identity("???", "unknown.monster_part"))

        self.assertIn(
            "materials = setOf(ResourceMaterial.BONE, ResourceMaterial.HIDE, ResourceMaterial.ORGAN),",
            output,
        )
        self.assertIn("not a parse placeholder", output)
        self.assertIn('names = LocalizedText(es = "???")', output)

    def test_omits_the_english_name_when_the_card_has_none(self) -> None:
        resource = resources.Resource(name="Hierro", category="SPECIAL", tags=("hierro",))

        output = resources.emit_kotlin([resource], self.identity("Hierro", "iron"))

        self.assertIn('names = LocalizedText(es = "Hierro")', output)
        self.assertNotIn("en = ", output)


if __name__ == "__main__":
    unittest.main()
