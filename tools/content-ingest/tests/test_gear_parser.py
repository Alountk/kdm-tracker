import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from bbox import Block  # noqa: E402
from ingest_gear import parse_cost, parse_gear_cards, parse_location  # noqa: E402


def block(x0, y0, x1, y1, text):
    return Block(x0, y0, x1, y1, text)


GEAR_PAGE = [
    block(47.6, 42.7, 55.6, 58.7, "3"),
    block(71.4, 40.9, 144.2, 55.6, "Yelmo de Calavera"),
    block(75.9, 55.3, 138.9, 62.6, "armadura, hueso, frágil"),
    block(43.8, 148.9, 166.7, 162.9, "Si sufres una lesión grave en la cabeza, el Yelmo de Calavera es destruido."),
]

# Two recipe columns (left x≈80, right x≈219) with a `Requiere Calor` to the right.
LOCATION_PAGE = [
    block(149.9, 38.3, 205.2, 52.0, "Armería"),
    block(80.6, 259.2, 168.1, 272.9, "Hacha de Contrapeso"),
    block(81.0, 279.6, 157.2, 293.3, "2 x hueso, 1 x piel, 1 x órgano"),
    block(218.5, 300.3, 276.9, 314.0, "Espada de Restos"),
    block(219.9, 320.7, 272.6, 334.4, "2 x hueso, 1 x restos"),
    block(286.5, 315.0, 303.0, 328.7, "Requiere Calor"),
]


class GearParserTest(unittest.TestCase):
    def test_parse_cost_splits_tokens(self):
        self.assertEqual({"hueso": 2, "piel": 1, "órgano": 1}, parse_cost("2 x hueso, 1 x piel, 1 x órgano"))
        self.assertEqual({"dedo de fénix": 1, "hueso": 4}, parse_cost("1x dedo de fénix, 4 x hueso"))

    def test_parse_gear_card_splits_name_keywords_and_rules(self):
        gears = parse_gear_cards([GEAR_PAGE])

        self.assertEqual(1, len(gears))
        gear = gears[0]
        self.assertEqual("Yelmo de Calavera", gear.name)
        self.assertEqual(["armadura", "hueso", "frágil"], gear.keywords)
        self.assertIn("lesión grave", gear.rules)
        self.assertEqual([], gear.modifiers)

    def test_duplicate_cards_dedup_by_name(self):
        self.assertEqual(1, len(parse_gear_cards([GEAR_PAGE, GEAR_PAGE])))

    def test_parse_location_pairs_names_with_costs_and_maps_prerequisites(self):
        location = parse_location(LOCATION_PAGE, innovation_names=set(), resolve=lambda token: token)

        self.assertEqual("Armería", location.name)
        by_name = {recipe.gear_name: recipe for recipe in location.recipes}
        self.assertEqual({"hueso": 2, "piel": 1, "órgano": 1}, by_name["Hacha de Contrapeso"].resource_cost)
        # "Calor" is not an innovation name here, so it is display-only text, not gating.
        self.assertEqual("Calor", by_name["Espada de Restos"].requires_text)
        self.assertEqual([], by_name["Espada de Restos"].requires)

    def test_a_recipe_prerequisite_that_is_an_innovation_gates(self):
        page = LOCATION_PAGE + [block(286.5, 259.0, 320.0, 272.0, "Requiere Horno Linterna")]

        location = parse_location(page, innovation_names={"Horno Linterna"}, resolve=lambda token: token)

        by_name = {recipe.gear_name: recipe for recipe in location.recipes}
        self.assertEqual(["Horno Linterna"], by_name["Hacha de Contrapeso"].requires)
        self.assertIsNone(by_name["Hacha de Contrapeso"].requires_text)


if __name__ == "__main__":
    unittest.main()
