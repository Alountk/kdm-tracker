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

## Resources

`ingest_resources.py` reads every deck in
`external-assets/Juego Completo/Formato Americano 57x88 mm/AF Recursos/` and writes
`core/src/commonMain/kotlin/io/kdmtracker/core/content/generated/Core15Resources.kt`.

```sh
python3 tools/content-ingest/ingest_resources.py \
  --pdf-dir "path/to/external-assets/Juego Completo/Formato Americano 57x88 mm/AF Recursos"
```

`--pdf-dir` overrides the deck directory; `--out` overrides the generated file.

### Category mapping

A resource's typed `ResourceCategory` is a property of the **deck** it ships in, not
of its material tags: `hueso`/`órgano`/`piel`/`restos` appear both on basic cards
("Hueso de Monstruo") and on quarry cards ("Garra de León"), so the tag alone cannot
tell BASIC from MONSTER. The deck is the section and the section decides:

| Deck (file) | Section title | Category |
| --- | --- | --- |
| `AF Rec Básicos.pdf` | `-Recursos Básicos-` | `BASIC` |
| `AF Rec Extraños.pdf` | `-Recursos Extraños-` | `SPECIAL` |
| `AF Rec Alimaña.pdf` | `-Alimañas-` | `SPECIAL` |
| `AF RecPre Antílope.pdf` | `Antílope Bramador` | `MONSTER` |
| `AF RecPre Fénix.pdf` | `Fénix` | `MONSTER` |
| `AF RecPre León.pdf` | `León Blanco` | `MONSTER` |

`Alimañas` (vermin) are neither basic materials nor quarry/nemesis monster cards; the
enum has no `VERMIN` member, and vermin are the deck's strange consumables, so
`SPECIAL` is the closest existing category. The mapping is explicit: every deck must be
listed, and an unrecognized deck aborts the run rather than inventing a category.

### Determinism and integrity

Entries are deduped and sorted by name; the output embeds no timestamps and no paths,
so a re-run is byte-identical. The card parser cross-checks `pdfinfo`'s page count
against the form-feed split, validates every card's type line against the known tag
vocabulary, and aborts on any page that does not parse as a card or on a resource name
that appears in two decks with different categories.

## Fighting arts and disorders

`ingest_fighting_arts.py` reads the fighting-arts deck
`AF Artes de Combate/AF Artes de Combate.pdf` and the disorders deck
`AF Trastornos.pdf`, and writes
`core/src/commonMain/kotlin/io/kdmtracker/core/content/generated/Core15FightingArts.kt`
as a `ContentBundle` with `fightingArts = List<ContentEntry>` and
`disorders = List<ContentEntry>` (the minimal name + description card type).

```sh
python3 tools/content-ingest/ingest_fighting_arts.py \
  --combate "path/to/external-assets/Juego Completo/Formato Americano 57x88 mm/AF Artes de Combate/AF Artes de Combate.pdf" \
  --trastornos "path/to/external-assets/Juego Completo/Formato Americano 57x88 mm/AF Trastornos.pdf"
```

`--combate` / `--trastornos` override the source PDFs and `--out` the generated file.

### Parsing model

Both decks render one card per PDF page with page 1 as the deck title; each deck has
its own card recognizer:

- A **fighting-art** card's second line is its type line, either `- arte de combate -`
  or `- arte de combate, cepa -`. It is validated against exactly that vocabulary, so
  a shifted page aborts the run instead of being silently mis-read.
- A **disorder** card has no type line; its last line is the `Anota y retira` reminder
  that closes every card. A page that does not end with it aborts the run.

The effect text is everything between the name (and type line, for arts) and the
footer, whitespace-normalized. The `- arte de combate, cepa -` strain variant is
recognized as a fighting art, not split into a separate list.

### Determinism and integrity

Entries are deduped and sorted by name; the output embeds no timestamps and no paths,
so a re-run is byte-identical. Per deck, `pdfinfo`'s page count is cross-checked
against the form-feed split, the parsed card count is asserted against `pages - 1`
(one title page), and any page that does not parse as a card aborts the run. A card
appearing twice with different descriptions also aborts.

### Known parse gaps

- The PDF's icon glyphs are dropped by `pdftotext`, leaving a dangling empty grant in
  `Doble Esprint` and `Berserker` (e.g. `… puedes gastar para ganar .`). The tokens
  were never in the extractable layer.
- The `Apatía` card's placeholder art text (`Lorem ipsum`) is part of the extracted
  body and is kept verbatim.
