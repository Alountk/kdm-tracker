# content-ingest

Deterministic pipeline that turns untracked source assets (card PDFs) into
committed Kotlin content for `:core`. The generator is the source of truth: the
generated file is never edited by hand, and re-running the generator must be a
no-op.

## Prerequisites

- Python 3 (standard library only).
- [poppler](https://poppler.freedesktop.org/) on `PATH`, providing `pdftotext`
  and `pdfinfo` (`brew install poppler` on macOS).

## Innovations

`ingest_innovations.py` reads `AF Innovaciones.pdf` and writes
`core/src/commonMain/kotlin/io/kdmtracker/core/content/generated/Core15Innovations.kt`.

The PDF lives under `external-assets/`, which is git-ignored and never committed.
A git worktree does not contain that directory, so pass the PDF explicitly when it
is not in the current checkout:

```sh
python3 tools/content-ingest/ingest_innovations.py \
  --pdf "/path/to/external-assets/Juego Completo/Formato Americano 57x88 mm/AF Innovaciones.pdf"
```

`--out` overrides the generated file path (defaults to the location above).

### Determinism

Entries and every `consequences` list are sorted by name; the output embeds no
timestamps and no paths. Re-running is byte-identical:

```sh
python3 tools/content-ingest/ingest_innovations.py --pdf "<pdf>"
git diff --exit-code core/src/commonMain/kotlin/io/kdmtracker/core/content/generated/Core15Innovations.kt
```

### Parsing model

- Each PDF page is one card. The page count is cross-checked against `pdfinfo`.
- Within a page, the first body line is the card **name**; the second is its
  **sub-line** (a tag list, `Innovación inicial`, or `principio de …`).
- A sub-line of the form `consecuencia[s] de[l] <parent>` builds the consequence
  graph: a card's `consequences` are the names of the cards that name it as
  parent. The graph is built from sub-lines **only**.
- The trailing `Añade las consecuencias …` prose is stripped from the description
  and used solely as a cross-check (every card that publishes consequences must
  have children in the graph, and vice versa).
- `Límite de Supervivencia +N` becomes `survivalLimitBonus` (default `0`).
- `description` is the remaining effect text, whitespace-normalized.

### Known parse gaps

- The PDF's icon glyphs are dropped by `pdftotext`; affected lines begin with a
  stray `:` (e.g. Cerámica, Escultura, Compañeros, Guía del Rey).
- Build-cost layout blocks render as trailing words in the description
  (Fundición de Restos ends with `… 6 x hueso Construir Herrería 3 x restos`).
  Tracked for a later refinement; the effect text is otherwise intact.
