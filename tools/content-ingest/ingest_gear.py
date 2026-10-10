#!/usr/bin/env python3
"""Parse the 1.5 gear + settlement-location cards into committed Kotlin.

Reads the cards through ``pdftotext -bbox-layout`` (see ``bbox.py``) so the text
keeps its spaces and columns stay separable by x — the reason ``-raw`` (which strips
spaces) and ``-layout`` (which flattens columns) were not enough.

The parsers here are pure: they take positioned ``Block``\\s and return plain data, so
they are exercised by small fixtures without the PDFs.
"""

from __future__ import annotations

import argparse
import re
import sys
import unicodedata
from dataclasses import dataclass, field
from pathlib import Path
from typing import Callable, Dict, List, Optional, Sequence, Set

sys.path.insert(0, str(Path(__file__).resolve().parent))

from bbox import Block, parse_blocks  # noqa: E402
from identity import load_identity, require_row  # noqa: E402
from ingest_innovations import DEFAULT_IDENTITY_RELATIVE as DEFAULT_INNOVATIONS_IDENTITY  # noqa: E402
from ingest_locations import (  # noqa: E402
    DEFAULT_LOCATIONS_IDENTITY,
    DEFAULT_RESOURCES_IDENTITY,
    _localized_lines,
    _map_lines,
    resolve_resource,
)
from kotlin_emit import indent, kotlin_escape, normalize_ws  # noqa: E402
from pdf_text import bbox_text  # noqa: E402

COST_RE = re.compile(r"^\d+\s*x\s*.+$", re.IGNORECASE)
REQUIRE_RE = re.compile(r"^requiere(?:\s+(.+))?$", re.IGNORECASE)
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
        if card and len(card.name) >= 3 and card.name not in by_name:
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


def _require_token(require_block: Block, blocks: Sequence[Block]) -> str:
    """The prerequisite label: the text after `Requiere`, or the block directly below it."""
    match = REQUIRE_RE.match(_text(require_block))
    if match and match.group(1):
        return match.group(1).strip()
    below = [
        b for b in blocks if abs(b.x0 - require_block.x0) <= COLUMN_TOLERANCE and 0 < b.y0 - require_block.y0 <= 15
    ]
    return _text(min(below, key=lambda b: b.y0)) if below else ""


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
            token = _require_token(require_block, blocks)
            if token in innovation_names:
                requires_list = [token]
            else:
                requires_text = token or None
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


GENERATED_HEADER = "// Generated by tools/content-ingest/ingest_gear.py. Do not edit by hand."

DEFAULT_GEAR_DIR = Path("external-assets/Juego Completo/Formato Cuadrado 51x51 mm/AF Equipo")
DEFAULT_LOCATIONS_PDF = Path("external-assets/Juego Completo/Formato XXL 102x204 mm/AF Asentamientos Loc.pdf")
DEFAULT_GEAR_IDENTITY = Path("tools/content-ingest/identity/gear.csv")
DEFAULT_OUT = Path("core/src/commonMain/kotlin/io/kdmtracker/core/content/generated/Core15Gear.kt")


def _repo_root() -> Path:
    return Path(__file__).resolve().parents[2]


def _relative(path: Path) -> Path:
    return path if path.is_absolute() else _repo_root() / path


def _string_list(items: Sequence[str]) -> str:
    return "listOf(" + ", ".join(f'"{kotlin_escape(item)}"' for item in items) + ")"


def _map_entries(mapping: Dict[str, int]) -> str:
    entries = ", ".join(f'"{kotlin_escape(name)}" to {count}' for name, count in sorted(mapping.items()))
    return f"mapOf({entries})"


MAX_LINE = 120


def _chunk(text: str, budget: int) -> List[str]:
    words = text.split(" ")
    chunks: List[List[str]] = []
    current: List[str] = []
    length = 0
    for word in words:
        extra = len(word) + (1 if current else 0)
        if current and length + extra > budget:
            chunks.append(current)
            current = [word]
            length = len(word)
        else:
            current.append(word)
            length += extra
    if current:
        chunks.append(current)
    joined = [" ".join(chunk) for chunk in chunks]
    return [chunk + " " for chunk in joined[:-1]] + [joined[-1]]


def _key_string_lines(key: str, base: int, text: str) -> List[str]:
    escaped = kotlin_escape(normalize_ws(text))
    inline = f'{indent(base)}{key} = "{escaped}",'
    if len(inline) <= MAX_LINE:
        return [inline]
    chunks = _chunk(escaped, MAX_LINE - base - 16)
    lines = [f"{indent(base)}{key} ="]
    for index, chunk in enumerate(chunks):
        width = base + 4 if index == 0 else base + 8
        suffix = " +" if index < len(chunks) - 1 else ","
        lines.append(f'{indent(width)}"{chunk}"{suffix}')
    return lines


def _key_list_lines(key: str, base: int, items: Sequence[str]) -> List[str]:
    entries = [f'"{kotlin_escape(item)}"' for item in items]
    inline = f"{indent(base)}{key} = listOf(" + ", ".join(entries) + "),"
    if len(inline) <= MAX_LINE:
        return [inline]
    lines = [f"{indent(base)}{key} =", f"{indent(base + 4)}listOf("]
    lines.extend(f"{indent(base + 8)}{entry}," for entry in entries)
    lines.append(f"{indent(base + 4)}),")
    return lines


def _key_map_lines(key: str, base: int, mapping: Dict[str, int]) -> List[str]:
    entries = [f'"{kotlin_escape(name)}" to {count}' for name, count in sorted(mapping.items())]
    inline = f"{indent(base)}{key} = mapOf(" + ", ".join(entries) + "),"
    if len(inline) <= MAX_LINE:
        return [inline]
    lines = [f"{indent(base)}{key} =", f"{indent(base + 4)}mapOf("]
    lines.extend(f"{indent(base + 8)}{entry}," for entry in entries)
    lines.append(f"{indent(base + 4)}),")
    return lines


def _gear_lines(gear: Gear, gear_identity: Dict[str, object]) -> List[str]:
    row = require_row(gear_identity, gear.name, source="ingest_gear")
    lines = [
        "                    Gear(",
        f'                        id = GearId("{kotlin_escape(row.resource_id)}"),',
    ]
    lines.extend(_localized_lines("names", 24, row.spanish_name or row.english_name, row.english_name))
    if gear.keywords:
        lines.extend(_key_list_lines("keywords", 24, gear.keywords))
    if gear.rules:
        lines.extend(_key_string_lines("rules", 24, gear.rules))
    lines.append("                    ),")
    return lines


def _requires_lines(requires: Sequence[str], innovation_identity: Dict[str, object], source: str) -> List[str]:
    ids = []
    for name in requires:
        row = require_row(innovation_identity, name, source=f"{source} requires")
        ids.append(f'InnovationId("{kotlin_escape(row.resource_id)}")')
    inline = f"{indent(36)}requires = listOf(" + ", ".join(ids) + "),"
    if len(inline) <= 120:
        return [inline]
    lines = [f"{indent(36)}requires =", f"{indent(40)}listOf("]
    lines.extend(f"{indent(44)}{entry}," for entry in ids)
    lines.append(f"{indent(40)}),")
    return lines


def _recipe_lines(
    recipe: Recipe,
    gear_identity: Dict[str, object],
    resource_identity: Dict[str, object],
    innovation_identity: Dict[str, object],
) -> List[str]:
    gear_row = require_row(gear_identity, recipe.gear_name, source="ingest_gear (recipe)")
    lines = [
        "                                GearRecipe(",
        f'                                    gearId = GearId("{kotlin_escape(gear_row.resource_id)}"),',
    ]
    if recipe.resource_cost:
        resolved: Dict[str, int] = {}
        for name, count in sorted(recipe.resource_cost.items()):
            row = require_row(resource_identity, name, source=f"ingest_gear ({recipe.gear_name} cost)")
            resolved[row.resource_id] = count
        lines.extend(_map_lines("resourceCost", 36, resolved, wrapper="ResourceId"))
    if recipe.requires:
        lines.extend(_requires_lines(recipe.requires, innovation_identity, f"ingest_gear ({recipe.gear_name})"))
    if recipe.requires_text:
        lines.append(f'                                    requiresText = "{kotlin_escape(recipe.requires_text)}",')
    lines.append("                                ),")
    return lines


def _location_lines(
    location: Location,
    location_identity: Dict[str, object],
    gear_identity: Dict[str, object],
    resource_identity: Dict[str, object],
    innovation_identity: Dict[str, object],
) -> List[str]:
    row = require_row(location_identity, location.name, source="ingest_gear")
    lines = [
        "                    SettlementLocation(",
        f'                        id = LocationId("{kotlin_escape(row.resource_id)}"),',
    ]
    lines.extend(_localized_lines("names", 24, row.spanish_name or row.english_name, row.english_name))
    if not location.recipes:
        lines.append("                    ),")
        return lines
    lines += ["                        recipes =", "                            listOf("]
    for recipe in location.recipes:
        lines.extend(_recipe_lines(recipe, gear_identity, resource_identity, innovation_identity))
    lines += ["                            ),", "                    ),"]
    return lines


def emit_kotlin(
    gears: Sequence[Gear],
    locations: Sequence[Location],
    gear_identity: Dict[str, object],
    location_identity: Dict[str, object],
    resource_identity: Dict[str, object],
    innovation_identity: Dict[str, object],
) -> str:
    lines = [
        GENERATED_HEADER,
        "package io.kdmtracker.core.content.generated",
        "",
        "import io.kdmtracker.core.content.ContentBundle",
        "import io.kdmtracker.core.domain.Gear",
        "import io.kdmtracker.core.domain.GearId",
        "import io.kdmtracker.core.domain.GearRecipe",
        "import io.kdmtracker.core.domain.InnovationId",
        "import io.kdmtracker.core.domain.LocalizedText",
        "import io.kdmtracker.core.domain.LocationId",
        "import io.kdmtracker.core.domain.ResourceId",
        "import io.kdmtracker.core.domain.SettlementLocation",
        "",
        "object Core15Gear {",
        "    val bundle: ContentBundle =",
        "        ContentBundle(",
        "            gear =",
        "                listOf(",
    ]
    for gear in gears:
        lines.extend(_gear_lines(gear, gear_identity))
    lines += ["                ),", "            locations =", "                listOf("]
    for location in locations:
        lines.extend(_location_lines(location, location_identity, gear_identity, resource_identity, innovation_identity))
    lines += ["                ),", "        )", "}", ""]
    return "\n".join(lines)


def _dedup_gears(gears: Sequence[Gear]) -> List[Gear]:
    by_name: Dict[str, Gear] = {}
    for gear in gears:
        by_name.setdefault(gear.name, gear)
    return [by_name[name] for name in sorted(by_name)]


def _innovation_names() -> Set[str]:
    path = _repo_root() / "core/src/commonMain/kotlin/io/kdmtracker/core/content/generated/Core15Innovations.kt"
    if not path.exists():
        return set()
    # Same trap as `_resource_names`: the pack emits ``names = LocalizedText(es = "…")`` once
    # innovations re-key to ids; matching the old shape drops every recipe's `requires` gate.
    return set(re.findall(r'names = LocalizedText\(es = "([^"]+)"', path.read_text(encoding="utf-8")))


def _resource_names() -> Set[str]:
    path = _repo_root() / "core/src/commonMain/kotlin/io/kdmtracker/core/content/generated/Core15Resources.kt"
    if not path.exists():
        return set()
    # S1 re-keyed the pack: each resource emits ``names = LocalizedText(es = "…")`` rather than
    # ``name = "…"``; matching the old shape silently resolved every recipe filter to nothing.
    return set(re.findall(r'names = LocalizedText\(es = "([^"]+)"', path.read_text(encoding="utf-8")))


def _normalize(token: str) -> str:
    decomposed = unicodedata.normalize("NFKD", token)
    stripped = "".join(char for char in decomposed if not unicodedata.combining(char))
    return " ".join(stripped.split()).lower()


def _normalized_variants(token: str) -> List[str]:
    """Normalized spellings to try: the token, then its first word singularized.

    The cards print some resources plural where the card name is singular (e.g. the cost
    `excrementos arcoíris` vs the resource `Excremento Arcoiris`), so matching a singularized
    first word recovers those recipes.
    """
    normalized = _normalize(token)
    variants = [normalized]
    words = normalized.split(" ")
    if words and words[0].endswith("s"):
        variants.append(" ".join([words[0][:-1]] + words[1:]))
    return variants


def _make_resolver(resource_names: Set[str]) -> Callable[[str], str]:
    by_normalized = {_normalize(name): name for name in resource_names}

    def resolve(token: str) -> str:
        mapped = resolve_resource(token)
        if mapped in resource_names:
            return mapped
        for variant in _normalized_variants(token):
            if variant in by_normalized:
                return by_normalized[variant]
        return token

    return resolve


def main(argv: Optional[Sequence[str]] = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--gear-dir", default=str(DEFAULT_GEAR_DIR))
    parser.add_argument("--locations-pdf", default=str(DEFAULT_LOCATIONS_PDF))
    parser.add_argument("--out", default=str(DEFAULT_OUT))
    args = parser.parse_args(argv)

    gears: List[Gear] = []
    for pdf in sorted(_relative(Path(args.gear_dir)).glob("*.pdf")):
        # Page 1 of each deck is the location's title card, not a gear card.
        gears.extend(parse_gear_cards(parse_blocks(bbox_text(pdf))[1:]))
    gears = _dedup_gears(gears)

    innovations = _innovation_names()
    resource_names = _resource_names()
    resolve = _make_resolver(resource_names)
    locations: List[Location] = []
    for page in parse_blocks(bbox_text(_relative(Path(args.locations_pdf)))):
        location = parse_location(page, innovations, resolve)
        # A recipe whose resources do not resolve cannot be crafted; drop it (a [gap]).
        location.recipes = [
            recipe
            for recipe in location.recipes
            if recipe.resource_cost and all(name in resource_names for name in recipe.resource_cost)
        ]
        if location.name and location.recipes:
            locations.append(location)
    locations.sort(key=lambda loc: loc.name)

    out = _relative(Path(args.out))
    out.parent.mkdir(parents=True, exist_ok=True)
    gear_identity = load_identity(_relative(DEFAULT_GEAR_IDENTITY))
    location_identity = load_identity(_relative(DEFAULT_LOCATIONS_IDENTITY))
    resource_identity = load_identity(_relative(DEFAULT_RESOURCES_IDENTITY))
    innovation_identity = load_identity(_relative(DEFAULT_INNOVATIONS_IDENTITY))
    out.write_text(
        emit_kotlin(gears, locations, gear_identity, location_identity, resource_identity, innovation_identity),
        encoding="utf-8",
    )
    print(f"wrote {out}: {len(gears)} gear, {len(locations)} locations")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

