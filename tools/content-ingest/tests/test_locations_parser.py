"""Fixture-based tests for the settlement-locations parser.

Everything here is pure string processing: ``parse_cost``, ``parse_card``,
``parse_builds`` and ``parse_locations`` take card text (never a PDF), so the
build costs, the prerequisites they imply, the Lantern Hoard's Innovate action,
and the stable emission are provable without the untracked ``AF Asentamientos
Loc.pdf``.

Two text layers are fed in for the build blocks, because the source PDF carries
an overlapping-text artifact on the Peletería card (see the README): the default
reading order (``pdftotext``) gives clean targets but a polluted cost box there,
while the ``-raw`` order binds the correct cost to the target name.
"""

from __future__ import annotations

import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

import ingest_locations as locations  # noqa: E402
from identity import load_identity, load_locations_identity  # noqa: E402

HIDE = "Piel de Monstruo"
BONE = "Hueso de Monstruo"
ORGAN = "Órgano de Monstruo"

# --- Taller de Huesos: a clean card that builds Armería. ---------------------------

TALLER_DEFAULT = "\n".join(
    [
        "Taller de Huesos",
        "Daga de Hueso",
        "1 x hueso",
        "Cuchilla de Hueso",
        "1 x hueso",
        "Construir",
        "Armería",
        "Localización de Asentamiento",
        "3 x hueso",
        "1 x piel",
    ]
)
TALLER_RAW = "3xhueso\n1xpiel Construir Armería\nLocalizacióndeAsentamiento\nTaller de Huesos\n"

# --- Peletería: the overlapping-text card (the stray Armería build bar). -----------

PELE_DEFAULT = "\n".join(
    [
        "Peletería",
        "Vendas",
        "1 x piel",
        "Construir",
        "Curtiduría",
        "Armería",
        "Localización de Asentamiento",
        "Tambores",
        "Amoníaco",
        "3 x piel",
        "hueso",
        "1 x piel",
        "órgano",
    ]
)
# Default order pollutes the cost box with the overlaid Armería cost; -raw binds
# the visible cost to "Curtiduría".
PELE_RAW = (
    "3xhueso\n1xpiel Construir Armería\nLocalizacióndeAsentamiento\n"
    "Peletería\n3xpiel\n1xórgano Construir Curtiduría\nLocalizacióndeAsentamiento\n"
)

# --- Molino de Órganos: the build bar sits after an action table. ------------------

MOLINO_DEFAULT = "\n".join(
    [
        "Molino de Órganos",
        "Ungüento Fecal",
        "1 x órgano",
        "Construir",
        "Círculo de Piedra",
        "1-3",
        "Requiere",
        "Calor",
        "1 x restos",
        "1 x hueso",
        "Pierdes un recurso del asentamiento.",
        "4 - 7 Ganas +1 a Supervivencia.",
        "8+ Intimar.",
        "Localización de Asentamiento",
        "3 x órgano",
        "1 x piel",
    ]
)
# "Círculo de Piedra" is not adjacent to Construir in -raw, so the parser falls
# back to the default cost box.
MOLINO_RAW = "Construir 3xórgano\n1xpiel Construir\nMolino de Órganos\nCírculo de Piedra\n"

# --- Lantern Hoard: the Innovate action. -------------------------------------------

LANTERN_DEFAULT = "\n".join(
    [
        "Cúmulo de Linternas",
        "INNOVAR",
        "Esfuerzo",
        "Hueso",
        "Órgano",
        "Piel",
        "Una vez por fase de asentamiento,",
        "podéis gastar los recursos listados para",
        "robar 2 cartas de innovación. Quedaos",
        "con 1 y devolved la otra al mazo.",
        "Construir Taller de Huesos",
        "Localización de Asentamiento",
        "Construir Peletería",
        "Localización de Asentamiento",
        "Construir Molino de Órganos Localización de Asentamiento",
    ]
)
LANTERN_RAW = (
    "Requiere\nLenguaje\n"
    "Taller de Huesos Construir LocalizacióndeAsentamiento\n"
    "Piel Órgano Hueso Esfuerzo\n"
    "Peletería Construir LocalizacióndeAsentamiento\n"
    "Molino de Órganos Construir LocalizacióndeAsentamiento\n"
    "Cúmulo de Linternas\n"
)

INNOVATE_EFFECT = (
    "Una vez por fase de asentamiento, podéis gastar los recursos listados para "
    "robar 2 cartas de innovación. Quedaos con 1 y devolved la otra al mazo."
)


def deck(*pages: str) -> str:
    return "\f".join(pages)


class ParseCostTests(unittest.TestCase):
    def test_parses_a_comma_separated_cost(self) -> None:
        self.assertEqual({BONE: 3, HIDE: 1}, locations.parse_cost("3 x hueso, 1 x piel"))

    def test_parses_the_raw_pack_cost_form(self) -> None:
        self.assertEqual({HIDE: 3, ORGAN: 1}, locations.parse_cost("3xpiel\n1xórgano"))

    def test_unknown_token_is_kept_verbatim(self) -> None:
        # "restos" (scrap) has no card in Core15Resources.kt; kept as-is so the gap is visible.
        self.assertEqual({"restos": 2}, locations.parse_cost("2 x restos"))


class ParseCardTests(unittest.TestCase):
    def test_first_line_is_the_name(self) -> None:
        self.assertEqual("Taller de Huesos", locations.parse_card(TALLER_DEFAULT).name)

    def test_lantern_hoard_innovate_action(self) -> None:
        card = locations.parse_card(LANTERN_DEFAULT)

        self.assertEqual("Cúmulo de Linternas", card.name)
        action = card.actions[0]
        self.assertEqual("Innovar", action.name)
        self.assertEqual("INNOVATE", action.kind)
        self.assertEqual(1, action.endeavor_cost)
        self.assertEqual({BONE: 1, ORGAN: 1, HIDE: 1}, action.resource_cost)
        self.assertTrue(action.once_per_phase)
        self.assertEqual(2, action.draw)
        self.assertEqual(1, action.keep)
        self.assertEqual(INNOVATE_EFFECT, action.effect)

    def test_a_card_without_an_esfuerzo_block_has_no_actions(self) -> None:
        self.assertEqual((), locations.parse_card(TALLER_DEFAULT).actions)


class ParseBuildsTests(unittest.TestCase):
    def _offers(self, default: str, raw: str, source: str):
        return locations.parse_builds(locations.body_lines(default), raw, source)

    def test_armeria_build_cost(self) -> None:
        offers = self._offers(TALLER_DEFAULT, TALLER_RAW, "Taller de Huesos")

        self.assertEqual(1, len(offers))
        self.assertEqual("Armería", offers[0].target)
        self.assertEqual({BONE: 3, HIDE: 1}, offers[0].cost)

    def test_circulo_de_piedra_build_cost_falls_back_to_default_order(self) -> None:
        offers = self._offers(MOLINO_DEFAULT, MOLINO_RAW, "Molino de Órganos")

        self.assertEqual(1, len(offers))
        self.assertEqual("Círculo de Piedra", offers[0].target)
        self.assertEqual({ORGAN: 3, HIDE: 1}, offers[0].cost)

    def test_peleteria_overlap_binds_the_visible_cost_to_curtiduria(self) -> None:
        offers = self._offers(PELE_DEFAULT, PELE_RAW, "Peletería")

        self.assertEqual(1, len(offers))
        self.assertEqual("Curtiduría", offers[0].target)
        self.assertEqual({HIDE: 3, ORGAN: 1}, offers[0].cost)

    def test_lantern_hoard_builds_the_tier_one_trio_without_a_cost(self) -> None:
        offers = self._offers(LANTERN_DEFAULT, LANTERN_RAW, "Cúmulo de Linternas")

        self.assertEqual(
            [("Taller de Huesos", {}), ("Peletería", {}), ("Molino de Órganos", {})],
            [(offer.target, offer.cost) for offer in offers],
        )


class ParseLocationsTests(unittest.TestCase):
    def test_assembles_build_cost_prerequisite_and_endeavor_from_the_building_card(self) -> None:
        text = deck(TALLER_DEFAULT, PELE_DEFAULT, MOLINO_DEFAULT, LANTERN_DEFAULT)
        raw = deck(TALLER_RAW, PELE_RAW, MOLINO_RAW, LANTERN_RAW)

        by_name = {location.name: location for location in locations.parse_locations(text, raw)}

        armeria = by_name["Armería"]
        self.assertEqual({BONE: 3, HIDE: 1}, armeria.build_cost)
        self.assertEqual(("Taller de Huesos",), armeria.requires)
        self.assertEqual(1, armeria.build_endeavors)

        circulo = by_name["Círculo de Piedra"]
        self.assertEqual({ORGAN: 3, HIDE: 1}, circulo.build_cost)
        self.assertEqual(("Molino de Órganos",), circulo.requires)

        taller = by_name["Taller de Huesos"]
        self.assertEqual({}, taller.build_cost)
        self.assertEqual(("Cúmulo de Linternas",), taller.requires)

        self.assertEqual(
            ["Armería", "Curtiduría", "Círculo de Piedra", "Cúmulo de Linternas",
             "Molino de Órganos", "Peletería", "Taller de Huesos"],
            [location.name for location in locations.parse_locations(text, raw)],
        )

    def test_emitting_twice_is_byte_identical(self) -> None:
        text = deck(TALLER_DEFAULT, PELE_DEFAULT, MOLINO_DEFAULT, LANTERN_DEFAULT)
        raw = deck(TALLER_RAW, PELE_RAW, MOLINO_RAW, LANTERN_RAW)

        first = locations.emit_kotlin(locations.parse_locations(text, raw), *_identities())
        second = locations.emit_kotlin(locations.parse_locations(text, raw), *_identities())

        self.assertEqual(first, second)
        self.assertIn("object Core15Locations {", first)
        self.assertIn("ActionKind.INNOVATE", first)


def _identity(name: str):
    base = Path(__file__).resolve().parents[1] / "identity"
    if name == "locations.csv":
        return load_locations_identity(base / name)
    return load_identity(base / name)


class AuthoredProseTests(unittest.TestCase):
    """The locations CSV is also the authored prose for every row the pipeline can emit."""

    def test_every_locations_row_with_a_spanish_name_carries_prose(self) -> None:
        rows = load_locations_identity(Path(__file__).resolve().parents[1] / "identity" / "locations.csv")

        missing = sorted(
            {row.resource_id for row in rows.values() if row.spanish_name and not row.description}
        )

        self.assertEqual([], missing, "every emitted location needs its description in the CSV")


def _identities():
    return (_identity("locations.csv"), _identity("resources.csv"))


class EmitIdentityTests(unittest.TestCase):
    """The emitter resolves every name through the identity CSVs before writing Kotlin."""

    def test_emission_carries_the_authored_description(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "locations.csv"
            path.write_text(
                "id,spanishName,englishName,description\n"
                "bone_smith,Taller de Huesos,Bone Smith,Talla armas de hueso.\n"
                "weapon_crafter,Armería,Weapon Crafter,\n",
                encoding="utf-8",
            )
            identity = load_locations_identity(path)

            out = locations.emit_kotlin(
                locations.parse_locations(TALLER_DEFAULT, TALLER_RAW),
                identity,
                _identity("resources.csv"),
            )

        self.assertIn('description = LocalizedText(es = "Talla armas de hueso."),', out)
        # A row with no authored prose emits no description at all — never a blank one.
        self.assertEqual(1, out.count("description ="))


    def test_emission_carries_ids_and_localized_names(self) -> None:
        text = deck(TALLER_DEFAULT, PELE_DEFAULT, MOLINO_DEFAULT, LANTERN_DEFAULT)
        raw = deck(TALLER_RAW, PELE_RAW, MOLINO_RAW, LANTERN_RAW)

        out = locations.emit_kotlin(locations.parse_locations(text, raw), *_identities())

        # Each location carries its stable id and both language names.
        self.assertIn('id = LocationId("bone_smith")', out)
        self.assertIn('names = LocalizedText(es = "Taller de Huesos", en = "Bone Smith")', out)
        self.assertIn('id = LocationId("lantern_hoard")', out)
        self.assertIn('names = LocalizedText(es = "Cúmulo de Linternas", en = "Lantern Hoard")', out)

        # Prerequisites reference locations by id, never by display name.
        self.assertIn(
            'requires = listOf(LocationId("bone_smith"))',
            out,
        )
        self.assertIn(
            'requires = listOf(LocationId("skinnery"))',
            out,
        )

        # Build costs and the action's resource cost reference resources by id.
        self.assertIn(
            'buildCost = mapOf(ResourceId("bone.monster") to 3, ResourceId("hide.monster") to 1)',
            out,
        )
        self.assertIn(
            'buildCost = mapOf(ResourceId("organ.monster") to 3, ResourceId("hide.monster") to 1)',
            out,
        )
        self.assertIn('ResourceId("bone.monster") to 1', out)
        self.assertNotIn('buildCost = mapOf("', out)

        # The action carries both language names so the panel stops rendering one language only.
        self.assertIn('names = LocalizedText(es = "Innovar", en = "Innovate")', out)
        self.assertNotIn('name = "Innovar"', out)
        self.assertNotIn('SettlementLocation("', out)

    def test_a_location_the_identity_table_does_not_cover_aborts_the_emit(self) -> None:
        parsed = locations.parse_locations("Sitio Fantasma\n", "Sitio Fantasma\n")

        with self.assertRaises(SystemExit) as ctx:
            locations.emit_kotlin(parsed, *_identities())

        self.assertIn("Sitio Fantasma", str(ctx.exception))

    def test_a_cost_resource_the_identity_table_does_not_cover_aborts_the_emit(self) -> None:
        default = "\n".join(
            [
                "Taller de Huesos",
                "Construir",
                "Armería",
                "Localización de Asentamiento",
                "1 x restos",
            ]
        )
        # A raw order with no ``Construir <target>`` adjacency, so the parser falls back to the
        # default reading order and reads the cost box below the caption.
        parsed = locations.parse_locations(default, "Armería\n")

        with self.assertRaises(SystemExit) as ctx:
            locations.emit_kotlin(parsed, *_identities())

        self.assertIn("restos", str(ctx.exception))


if __name__ == "__main__":
    unittest.main()
