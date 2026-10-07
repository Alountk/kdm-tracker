#!/usr/bin/env python3
"""Parse the 1.5 gear + settlement-location cards into committed Kotlin.

Reads the cards through ``pdftotext -bbox-layout`` (see ``bbox.py``) so the text
keeps its spaces and columns stay separable by x — the reason ``-raw`` (which strips
spaces) and ``-layout`` (which flattens columns) were not enough.

The parsers here are pure: they take positioned ``Block``\\s and return plain data, so
they are exercised by small fixtures without the PDFs.
"""

from __future__ import annotations

import re
from dataclasses import dataclass, field
from typing import Callable, Dict, List, Optional, Sequence, Set

from bbox import Block

COST_RE = re.compile(r"^\d+\s*x\s*.+$", re.IGNORECASE)
REQUIRE_RE = re.compile(r"^requiere\s+(.+)$", re.IGNORECASE)
NUMBER_RE = re.compile(r"^\d+[+]?$")
KEYWORDS_RE = re.compile(r"^[a-záéíóúüñ][a-záéíóúüñ,\s]*$")

COLUMN_TOLERANCE = 20.0
ROW_TOLERANCE = 30.0


@dataclass
class Gear:
    name: str
    keywords: List[str] = field(default_factory=list)
    rules: str = ""
    modifiers: List[object] = field(default_factory=list)


@dataclass
class Recipe:
    gear_name: str
    resource_cost: Dict[str, int]
    requires: List[str] = field(default_factory=list)
    requires_text: Optional[str] = None


@dataclass
class Location:
    name: str
    recipes: List[Recipe] = field(default_factory=list)


def _text(block: Block) -> str:
    return " ".join(block.text.split())


def _is_number(text: str) -> bool:
    return bool(NUMBER_RE.match(text))


def _is_cost(text: str) -> bool:
    return bool(COST_RE.match(text))


def _is_require(text: str) -> bool:
    return bool(REQUIRE_RE.match(text))


def _is_keywords(text: str) -> bool:
    return "," in text and bool(KEYWORDS_RE.match(text))


def parse_cost(text: str) -> Dict[str, int]:
    """Parse a card cost line like ``2 x hueso, 1 x piel`` into ``{token: count}``."""
    costs: Dict[str, int] = {}
    for chunk in text.split(","):
        match = re.match(r"\s*(\d+)\s*x\s*(.+?)\s*$", chunk, re.IGNORECASE)
        if not match:
            continue
        token = match.group(2).strip()
        if token:
            count = int(match.group(1))
            costs[token] = costs.get(token, 0) + count
    return costs


def _parse_gear_card(blocks: Sequence[Block]) -> Optional[Gear]:
    ordered = sorted((b for b in blocks if _text(b)), key=lambda b: (b.y0, b.x0))
    name = next((_text(b) for b in ordered if not _is_number(_text(b))), None)
    if name is None:
        return None
    rest = [_text(b) for b in ordered if _text(b) != name and not _is_number(_text(b))]
    keywords: List[str] = []
    rules: List[str] = []
    for text in rest:
        if not keywords and _is_keywords(text):
            keywords = [part.strip() for part in text.split(",") if part.strip()]
        else:
            rules.append(text)
    return Gear(name=name, keywords=keywords, rules=" ".join(rules).strip())


def parse_gear_cards(pages: Sequence[Sequence[Block]]) -> List[Gear]:
    """Parse one ``Gear`` per card page, deduped by name (first wins), sorted by name."""
    by_name: Dict[str, Gear] = {}
    for page in pages:
        card = _parse_gear_card(page)
        if card and card.name and card.name not in by_name:
            by_name[card.name] = card
    return [by_name[name] for name in sorted(by_name)]


def _nearest_below(
    block: Block,
    candidates: Sequence[Block],
    tolerance: float,
) -> Optional[Block]:
    in_column = [
        c for c in candidates if abs(c.x0 - block.x0) <= COLUMN_TOLERANCE and 0 <= c.y0 - block.y0 <= tolerance
    ]
    return min(in_column, key=lambda c: c.y0) if in_column else None


def _nearest_require(block: Block, requires: Sequence[Block]) -> Optional[Block]:
    near = [r for r in requires if abs(r.y0 - block.y0) <= ROW_TOLERANCE and r.x0 >= block.x0]
    return min(near, key=lambda r: abs(r.y0 - block.y0)) if near else None


def parse_location(
    blocks: Sequence[Block],
    innovation_names: Set[str],
    resolve: Callable[[str], str] = lambda token: token,
) -> Location:
    """Parse one location card page: its name and the recipes its columns print.

    A recipe is a name block paired with the cost block directly below it in the same
    column. A nearby ``Requiere X`` gates the recipe (``requires``) when ``X`` is an
    innovation name, and is display-only text (``requires_text``) otherwise.
    """
    texts = [_text(b) for b in blocks if _text(b)]
    name = next((t for t in texts if not _is_number(t)), "")
    costs = [b for b in blocks if _text(b) and _is_cost(_text(b))]
    requires = [b for b in blocks if _text(b) and _is_require(_text(b))]

    recipes: List[Recipe] = []
    seen: Set[str] = set()
    for block in sorted(blocks, key=lambda b: (b.y0, b.x0)):
        text = _text(block)
        if not text or _is_number(text) or _is_cost(text) or _is_require(text) or text == name or text in seen:
            continue
        cost_block = _nearest_below(block, costs, ROW_TOLERANCE)
        if cost_block is None:
            continue
        cost = {resolve(token): count for token, count in parse_cost(_text(cost_block)).items()}
        require_block = _nearest_require(block, requires)
        requires_list: List[str] = []
        requires_text: Optional[str] = None
        if require_block is not None:
            token = REQUIRE_RE.match(_text(require_block)).group(1).strip()
            if token in innovation_names:
                requires_list = [token]
            else:
                requires_text = token
        seen.add(text)
        recipes.append(
            Recipe(
                gear_name=text,
                resource_cost=cost,
                requires=requires_list,
                requires_text=requires_text,
            ),
        )
    return Location(name=name, recipes=recipes)
