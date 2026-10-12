"""The identity layer shared by every ingest script.

Each content family has a committed CSV under ``identity/`` giving every card a stable, permanent
id (a human-chosen slug, never derived from a display name) and its names per language. The
locations CSV additionally carries each entity's authored prose (see ``LOCATIONS_HEADER``).
The scripts read the PDFs for the *content* and the CSV for the *identity*, and a card the CSV
does not cover aborts the run rather than entering the catalog with an id that could drift.

A row may name the same entity in more than one language — the 1.5 packs are Spanish and the 1.6
seed is English — so a row is indexed under **every** name it carries. That is what lets
``Lantern Hoard`` and ``Cúmulo de Linternas`` collapse into one location instead of two.
"""

from __future__ import annotations

import csv
import sys
import unicodedata
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, FrozenSet, List, Optional, Sequence, Tuple

# The material tags that may appear on a resource card's type line.
KNOWN_TAGS = {
    "hueso",
    "órgano",
    "piel",
    "restos",
    "consumible",
    "otro",
    "alimaña",
    "hierba",
    "hierro",
    "calavera",
}

# The four tags that name a settlement-summary bucket. The rest decide nothing: a card carrying
# none of these four feeds no bucket, which is an answer rather than a gap.
PRIMARY_MATERIAL_BY_TAG = {
    "hueso": "BONE",
    "órgano": "ORGAN",
    "piel": "HIDE",
    "restos": "SCRAP",
}

MATERIALS = frozenset(PRIMARY_MATERIAL_BY_TAG.values())

IDENTITY_HEADER = ("id", "spanishName", "englishName")

# The locations CSV carries one authored column more than the rest: the card prose the
# settlement-location cards never print and no PDF transcribes. It rides here because the row is
# the entity's single content record, and it is optional — a row with no Spanish name (an entity
# only the 1.6 seed defines) has no authored prose either.
LOCATIONS_HEADER = ("id", "spanishName", "englishName", "description")

# The innovations CSV carries one more authored column than the rest: the domain of the
# tree branch the innovation descends from, which the deck's category chips and each
# card's badge read. Like the location prose it is optional in the type and required by
# this family's loader, so a row that forgets it fails the run rather than shipping a
# chip-less card.
INNOVATIONS_HEADER = ("id", "spanishName", "englishName", "category")


def normalize_ws(text: str) -> str:
    """Collapse runs of whitespace and strip; PDF text arrives with ragged spacing."""
    return " ".join(text.split())


def nfc(text: str) -> str:
    """macOS stores filenames in NFD; compare against NFC literals."""
    return unicodedata.normalize("NFC", text)


@dataclass(frozen=True)
class IdentityRow:
    """One row of an identity CSV: an entity's stable id and its names per language."""

    resource_id: str
    spanish_name: str
    english_name: str
    #: The authored card prose, when this CSV carries a [LOCATIONS_HEADER]; empty otherwise.
    description: str = ""
    #: The authored tree-branch domain, when this CSV carries an [INNOVATIONS_HEADER].
    category: str = ""

    def names(self) -> Tuple[str, ...]:
        """Every non-empty name this row is reachable by."""
        return tuple(name for name in (self.spanish_name, self.english_name) if name)

    def name_in(self, language: str) -> str:
        return self.english_name if language == "en" else self.spanish_name


def load_identity(
    csv_path: Path,
    *,
    header: Sequence[str] = IDENTITY_HEADER,
) -> Dict[str, IdentityRow]:
    """Read an identity CSV, keyed by **every** name each row carries."""
    if not csv_path.is_file():
        raise SystemExit(f"identity CSV not found: {csv_path}")

    by_name: Dict[str, IdentityRow] = {}
    with csv_path.open(encoding="utf-8", newline="") as handle:
        reader = csv.DictReader(handle)
        got = tuple(reader.fieldnames or ())
        if got != tuple(header):
            raise SystemExit(f"identity CSV {csv_path.name} header must be {tuple(header)}, got {got}")
        for raw in reader:
            row = IdentityRow(
                resource_id=normalize_ws(raw["id"]),
                spanish_name=normalize_ws(raw.get("spanishName") or ""),
                english_name=normalize_ws(raw.get("englishName") or ""),
                description=normalize_ws(raw.get("description") or ""),
                category=normalize_ws(raw.get("category") or ""),
            )
            if not row.resource_id:
                raise SystemExit(f"identity CSV {csv_path.name}: a row has no id: {raw!r}")
            if not row.names():
                raise SystemExit(f"identity CSV {csv_path.name}: row {row.resource_id!r} carries no name")
            for name in dict.fromkeys(row.names()):
                if name in by_name:
                    raise SystemExit(f"identity CSV {csv_path.name} repeats the name {name!r}")
                by_name[name] = row
    return by_name


def load_locations_identity(csv_path: Path) -> Dict[str, IdentityRow]:
    """Read the locations identity CSV — the one family whose rows carry authored prose."""
    return load_identity(csv_path, header=LOCATIONS_HEADER)


def load_innovations_identity(csv_path: Path) -> Dict[str, IdentityRow]:
    """Read the innovations identity CSV — the one family whose rows carry a category."""
    return load_identity(csv_path, header=INNOVATIONS_HEADER)


def require_row(by_name: Dict[str, IdentityRow], name: str, *, source: str) -> IdentityRow:
    """The row for [name], aborting when it is missing — an unmapped card must not be emitted."""
    row = by_name.get(name)
    if row is None:
        raise SystemExit(
            f"{source}: {name!r} has no identity row; every card needs a stable id before it can "
            "be emitted"
        )
    return row


def unmapped_cards(
    by_name: Dict[str, IdentityRow],
    cards: Sequence[str],
) -> List[str]:
    """The card names with no identity row."""
    return sorted(name for name in cards if name not in by_name)


def unused_rows(by_name: Dict[str, IdentityRow], cards: Sequence[str], *, stream=sys.stderr) -> None:
    """
    Note the rows that cover no card in this deck set.

    Not an error: a family's CSV is the identity table for **every** pack, and the hand-written 1.6
    seed names entities the 1.5 decks do not carry. A mistyped card name cannot slip through this
    way — the card it meant to cover would be unmapped, and the caller aborts on that.
    """
    covered = set(cards)
    seen = {row.resource_id for name in covered if (row := by_name.get(name)) is not None}
    orphans = sorted({row.resource_id for row in by_name.values()} - seen)
    if orphans:
        print(
            f"note: {len(orphans)} identity row(s) cover no card in this deck set "
            f"(other packs' entities): {orphans}",
            file=stream,
        )
