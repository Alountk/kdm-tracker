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
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

import ingest_locations as locations  # noqa: E402

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

    def test_parses_the_raw_packt_cost_form(self) -> None:
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

        first = locations.emit_kotlin(locations.parse_locations(text, raw))
        second = locations.emit_kotlin(locations.parse_locations(text, raw))

        self.assertEqual(first, second)
        self.assertIn("object Core15Locations {", first)
        self.assertIn("ActionKind.INNOVATE", first)


if __name__ == "__main__":
    unittest.main()
