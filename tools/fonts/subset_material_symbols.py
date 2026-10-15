#!/usr/bin/env python3
"""Regenerate and verify the bundled Material Symbols subset.

The subset in `app-android/src/main/res/font/material_symbols_outlined.ttf` must contain exactly
the codepoints `KdmSymbols.CODEPOINTS` maps: a mapped glyph the font lacks renders tofu (and
`KdmSymbols.glyph` happily returns the codepoint, so nothing fails until the screen is drawn), and
a font glyph nobody maps is dead weight. This script owns both halves of that contract:

    # verify the shipped bytes against the Kotlin map (stdlib only, safe in CI):
    python3 tools/fonts/subset_material_symbols.py --check

    # regenerate the subset after the map changes (requires fontTools and the upstream font):
    python3 tools/fonts/subset_material_symbols.py --build \
        --source path/to/MaterialSymbolsOutlined[FILL,GRAD,opsz,wght].ttf

`--build` reads the full variable font from Google's `google/material-design-icons` repository
(`variablefont/MaterialSymbolsOutlined[FILL,GRAD,opsz,wght].ttf`), resolves the map's names through
its cmap, and runs `pyftsubset` with `--layout-features=* --no-hinting --desubroutinize`. The
upstream font is never committed here (it is ~10 MB of Google's assets); pass its path or download
it yourself. `--check` then proves the result.
"""

from __future__ import annotations

import argparse
import re
import struct
import subprocess
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
KOTLIN_MAP = REPO / "app-android/src/main/kotlin/io/kdmtracker/app/ui/theme/KdmSymbols.kt"
BUNDLED_TTF = REPO / "app-android/src/main/res/font/material_symbols_outlined.ttf"

_ENTRY = re.compile(r'"(\w+)" to 0x([0-9A-Fa-f]+)')


def parse_kotlin_map(source: str) -> dict[str, int]:
    """Every `name` to `0x…` entry of `KdmSymbols.CODEPOINTS`, in file order."""
    if "CODEPOINTS" not in source:
        raise ValueError("no CODEPOINTS map found in the Kotlin source")
    entries = {name: int(code, 16) for name, code in _ENTRY.findall(source)}
    if not entries:
        raise ValueError("CODEPOINTS map is empty or unreadable")
    return entries


def cmap_codepoints(data: bytes) -> set[int]:
    """Every codepoint the font's `cmap` resolves to a non-zero glyph (formats 4 and 12)."""
    if len(data) < 12 or data[:4] not in (b"\x00\x01\x00\x00", b"OTTO", b"true"):
        raise ValueError("not a TrueType/OpenType font")

    def u16(offset: int) -> int:
        return struct.unpack_from(">H", data, offset)[0]

    def u32(offset: int) -> int:
        return struct.unpack_from(">I", data, offset)[0]

    cmap = -1
    for i in range(u16(4)):
        record = 12 + i * 16
        if data[record : record + 4] == b"cmap":
            cmap = u32(record + 8)
    if cmap < 0:
        raise ValueError("the font has no cmap table")

    found: set[int] = set()
    for i in range(u16(cmap + 2)):
        subtable = cmap + u32(cmap + 4 + i * 8 + 4)
        fmt = u16(subtable)
        if fmt == 4:
            found |= _format4(data, subtable)
        elif fmt == 12:
            found |= _format12(data, subtable)
    if not found:
        raise ValueError("no readable cmap subtable (expected format 4 or 12)")
    return found


def _format4(data: bytes, at: int) -> set[int]:
    def u16(offset: int) -> int:
        return struct.unpack_from(">H", data, offset)[0]

    def i16(offset: int) -> int:
        return struct.unpack_from(">h", data, offset)[0]

    seg_count = u16(at + 6) // 2
    ends = at + 14
    starts = ends + seg_count * 2 + 2
    deltas = starts + seg_count * 2
    range_offsets = deltas + seg_count * 2
    found: set[int] = set()
    for segment in range(seg_count):
        start = u16(starts + segment * 2)
        end = u16(ends + segment * 2)
        if start > end or end == 0xFFFF:
            continue
        delta = i16(deltas + segment * 2)
        range_offset = u16(range_offsets + segment * 2)
        for codepoint in range(start, end + 1):
            if range_offset == 0:
                glyph = (codepoint + delta) & 0xFFFF
            else:
                index = range_offsets + segment * 2 + range_offset + (codepoint - start) * 2
                raw = u16(index)
                glyph = (raw + delta) & 0xFFFF if raw else 0
            if glyph:
                found.add(codepoint)
    return found


def _format12(data: bytes, at: int) -> set[int]:
    groups = struct.unpack_from(">I", data, at + 12)[0]
    found: set[int] = set()
    for group in range(groups):
        record = at + 16 + group * 12
        start, end, first_glyph = struct.unpack_from(">III", data, record)
        if first_glyph:
            found.update(range(start, end + 1))
    return found


def diff(entries: dict[str, int], codepoints: set[int]) -> list[str]:
    """Human-readable divergence between the Kotlin map and the font, empty when identical."""
    mapped = set(entries.values())
    missing = sorted(mapped - codepoints)
    extra = sorted(codepoints - mapped)
    lines = []
    if missing:
        lines.append("missing from the TTF: " + ", ".join(f"U+{c:04X}" for c in missing))
    if extra:
        lines.append("unmapped in the TTF: " + ", ".join(f"U+{c:04X}" for c in extra))
    return lines


def check() -> int:
    entries = parse_kotlin_map(KOTLIN_MAP.read_text(encoding="utf-8"))
    font = BUNDLED_TTF.read_bytes()
    lines = diff(entries, cmap_codepoints(font))
    if lines:
        print(f"KdmSymbols.CODEPOINTS and {BUNDLED_TTF.name} disagree:")
        for line in lines:
            print(f"  {line}")
        print("regenerate with --build, or fix the map")
        return 1
    print(f"OK: {len(entries)} codepoints, TTF and CODEPOINTS agree")
    return 0


def build(source: Path, output: Path) -> int:
    try:
        import fontTools  # noqa: F401
    except ImportError:
        print("fontTools is required to build: python3 -m pip install fonttools", file=sys.stderr)
        return 2
    if not source.is_file():
        print(f"source font not found: {source}", file=sys.stderr)
        return 2

    entries = parse_kotlin_map(KOTLIN_MAP.read_text(encoding="utf-8"))
    available = cmap_codepoints(source.read_bytes())
    unresolved = sorted(set(entries.values()) - available)
    if unresolved:
        names = [n for n, c in entries.items() if c in unresolved]
        print(f"upstream font lacks {len(unresolved)} mapped glyphs: {', '.join(names)}", file=sys.stderr)
        return 1

    unicodes = ",".join(f"U+{c:04X}" for c in sorted(set(entries.values())))
    subprocess.run(
        [
            sys.executable,
            "-m",
            "fontTools.subset",
            str(source),
            f"--unicodes={unicodes}",
            "--layout-features=*",
            "--no-hinting",
            "--desubroutinize",
            f"--output-file={output}",
        ],
        check=True,
    )
    print(f"wrote {output} ({output.stat().st_size} bytes, {len(set(entries.values()))} codepoints)")
    return 0


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("--check", action="store_true", help="verify the bundled TTF against CODEPOINTS")
    group.add_argument("--build", action="store_true", help="regenerate the bundled TTF from a full font")
    parser.add_argument("--source", type=Path, help="full Material Symbols TTF to subset from (--build)")
    parser.add_argument("--output", type=Path, default=BUNDLED_TTF, help="destination of --build")
    args = parser.parse_args(argv)

    if args.check:
        return check()
    if not args.source:
        parser.error("--build requires --source")
    return build(args.source, args.output)


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
