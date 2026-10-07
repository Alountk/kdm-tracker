import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from bbox import parse_blocks  # noqa: E402

SAMPLE = """<?xml version="1.0"?>
<html xmlns="http://www.w3.org/1999/xhtml"><body><doc>
  <page width="210" height="210">
    <flow>
      <block xMin="47.6" yMin="42.7" xMax="144.2" yMax="55.6">
        <line xMin="47.6" yMin="42.7" xMax="144.2" yMax="55.6">
          <word xMin="47.6" yMin="42.7" xMax="96.6" yMax="55.6">Yelmo</word>
          <word xMin="98.8" yMin="42.7" xMax="108.6" yMax="55.6">de</word>
          <word xMin="110.9" yMin="42.7" xMax="144.2" yMax="55.6">Calavera</word>
        </line>
      </block>
    </flow>
  </page>
  <page width="210" height="210"></page>
</doc></body></html>
"""


class BboxTest(unittest.TestCase):
    def test_parse_blocks_returns_pages_of_positioned_blocks(self):
        pages = parse_blocks(SAMPLE)

        self.assertEqual(2, len(pages))
        block = pages[0][0]
        self.assertEqual("Yelmo de Calavera", block.text)
        self.assertAlmostEqual(47.6, block.x0)
        self.assertAlmostEqual(144.2, block.x1)
        self.assertEqual([], pages[1])

    def test_empty_document_yields_no_pages(self):
        self.assertEqual([], parse_blocks("<html><body><doc></doc></body></html>"))


if __name__ == "__main__":
    unittest.main()
