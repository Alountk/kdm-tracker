"""Shared Kotlin emit helpers for the content-ingest scripts.

The three ingest scripts parse different card shapes but emit the same Kotlin
vocabulary. Whitespace normalization, string escaping, indentation, and
description wrapping live here so the generated output stays identical across
scripts and the wrapping rules exist in exactly one place.
"""

from __future__ import annotations

import re
from typing import List

DESC_KEY_INDENT = 24
DESC_FIRST_INDENT = 28
DESC_CONT_INDENT = 32
# An inlined `description = "…",` occupies 24 + len('description = "') + "…" + '",'
# characters on the source line, so the text budget is the line width minus that prefix.
DESC_INLINE_MAX = 120 - 41
DESC_CHUNK_MAX = 80


def normalize_ws(text: str) -> str:
    return re.sub(r"\s+", " ", text).strip()


def kotlin_escape(text: str) -> str:
    out = []
    for char in text:
        if char == "\\":
            out.append("\\\\")
        elif char == '"':
            out.append('\\"')
        elif char == "$":
            out.append("\\$")
        elif char == "\n":
            out.append("\\n")
        elif char == "\r":
            out.append("\\r")
        elif char == "\t":
            out.append("\\t")
        else:
            out.append(char)
    return "".join(out)


def indent(width: int) -> str:
    return " " * width


def _chunk_for_wrapping(escaped: str, budget: int) -> List[str]:
    words = escaped.split(" ")
    chunks: List[List[str]] = []
    current: List[str] = []
    current_len = 0
    for word in words:
        extra = len(word) + (1 if current else 0)
        if current and current_len + extra > budget:
            chunks.append(current)
            current = [word]
            current_len = len(word)
        else:
            current.append(word)
            current_len += extra
    if current:
        chunks.append(current)
    joined = [" ".join(chunk) for chunk in chunks]
    return [chunk + " " for chunk in joined[:-1]] + [joined[-1]]


def description_lines(description: str) -> List[str]:
    """Render `description = …` as either one inline line or a wrapped `+` block."""
    escaped = kotlin_escape(description)
    if len(escaped) <= DESC_INLINE_MAX:
        return [f'{indent(DESC_KEY_INDENT)}description = "{escaped}",']

    chunks = _chunk_for_wrapping(escaped, DESC_CHUNK_MAX)
    lines = [f"{indent(DESC_KEY_INDENT)}description ="]
    for index, chunk in enumerate(chunks):
        width = DESC_FIRST_INDENT if index == 0 else DESC_CONT_INDENT
        suffix = " +" if index < len(chunks) - 1 else ","
        lines.append(f'{indent(width)}"{chunk}"{suffix}')
    return lines
