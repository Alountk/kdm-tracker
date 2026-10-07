"""Parse the XHTML that ``pdftotext -bbox-layout`` emits into positioned blocks.

Unlike ``-raw`` (which strips spaces) or ``-layout`` (which splits columns by
whitespace), ``-bbox-layout`` gives every word a bounding box, so text is
recoverable with its spaces intact and columns can be separated by x. This module
turns that XHTML into a simple ``Block`` model the parsers reason over.
"""

from __future__ import annotations

import xml.etree.ElementTree as ET
from dataclasses import dataclass
from typing import List

_NS = "{http://www.w3.org/1999/xhtml}"


@dataclass(frozen=True)
class Block:
    """One text block with its page-space bounding box and joined word text."""

    x0: float
    y0: float
    x1: float
    y1: float
    text: str


def _coord(element: ET.Element, name: str) -> float:
    return float(element.get(name, "0") or 0)


def _words(element: ET.Element) -> List[str]:
    return [word.text or "" for word in element.iter(f"{_NS}word")]


def parse_blocks(xml: str) -> List[List[Block]]:
    """Return one list of :class:`Block` per ``<page>`` in the ``-bbox-layout`` XML."""
    if not xml.strip():
        return []
    root = ET.fromstring(xml)
    pages: List[List[Block]] = []
    for page in root.iter(f"{_NS}page"):
        blocks = [
            Block(
                x0=_coord(block, "xMin"),
                y0=_coord(block, "yMin"),
                x1=_coord(block, "xMax"),
                y1=_coord(block, "yMax"),
                text=" ".join(word for word in _words(block) if word).strip(),
            )
            for block in page.iter(f"{_NS}block")
        ]
        pages.append(blocks)
    return pages
