"""Thin wrappers around the poppler command-line tools `pdftotext` and `pdfinfo`.

Only the two read operations the ingest pipeline needs are exposed. Keeping the
subprocess plumbing in one place lets the parsers stay pure string processing and
makes the external dependency (poppler) explicit and easy to test around.
"""

from __future__ import annotations

import shutil
import subprocess
from pathlib import Path
from typing import Dict, Union

PathLike = Union[str, Path]


def _require(tool: str) -> str:
    """Return the absolute path to `tool`, or fail with an actionable message."""
    resolved = shutil.which(tool)
    if resolved is None:
        raise RuntimeError(
            f"required tool '{tool}' was not found on PATH. "
            f"Install poppler (macOS: `brew install poppler`)."
        )
    return resolved


def pdf_text(pdf_path: PathLike, *, layout: bool = True) -> str:
    """Return the text of `pdf_path`.

    With `layout=True` (the default) columns are preserved via `pdftotext -layout`,
    which is what the card parser relies on. Output is decoded as UTF-8 so accented
    Spanish text round-trips exactly.
    """
    args = [_require("pdftotext")]
    if layout:
        args.append("-layout")
    args += [str(pdf_path), "-"]

    completed = subprocess.run(args, check=True, capture_output=True, text=True, encoding="utf-8")
    return completed.stdout


def pdf_info(pdf_path: PathLike) -> Dict[str, str]:
    """Return `pdfinfo` output as a `{key: value}` mapping."""
    completed = subprocess.run(
        [_require("pdfinfo"), str(pdf_path)],
        check=True,
        capture_output=True,
        text=True,
        encoding="utf-8",
    )

    info: Dict[str, str] = {}
    for line in completed.stdout.splitlines():
        if ":" not in line:
            continue
        key, value = line.split(":", 1)
        info[key.strip()] = value.strip()
    return info
