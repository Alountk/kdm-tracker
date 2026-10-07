# content-ingest

Deterministic pipeline that turns untracked source assets (card PDFs) into
committed Kotlin content for `:core`. The generator is the source of truth: the
generated file is never edited by hand, and re-running the generator must be a
no-op.

## Pipeline

The inputs are the **untracked** Spanish 1.5 card PDFs under `external-assets/**`,
which is git-ignored and never committed. Each `ingest_*.py` script parses its PDFs with
`pdftotext` (cross-checking page counts with `pdfinfo`) and writes a **committed** Kotlin
file under `core/src/commonMain/kotlin/io/kdmtracker/core/content/generated/`. The app
never reads a PDF at runtime — it only reads the committed Kotlin.

Output is **deterministic**: every list is sorted and no timestamps or paths are embedded,
so re-running a script over unchanged inputs is byte-identical:

```sh
git diff --exit-code core/src/commonMain/kotlin/io/kdmtracker/core/content/generated/
```

Each script accepts `--out` to override its generated file path; the deck-specific
sections below list the remaining arguments.

## Prerequisites

- Python 3 (standard library only).
- [poppler](https://poppler.freedesktop.org/) on `PATH`, providing `pdftotext`
  and `pdfinfo` (`brew install poppler` on macOS). **poppler >= 22.02.** The
  generated output was produced and is verified with **poppler 26.04.0**; the
  `-layout` text layer and the accent round-trip differ across older releases, so
  pin or record the version when regenerating on a new machine.

## Tests

The parsers are exercised without the PDFs by a stdlib `unittest` suite that feeds
small text fixtures to the pure functions (consequence graph, survival bonus, the
anchored deck-prose, every abort path, and stable emission):

```sh
python3 -m unittest discover -s tools/content-ingest/tests -v
```

No third-party packages are required — `unittest` only. The suite runs in CI on any
checkout, including worktrees that do not carry the untracked `external-assets/`.

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
  have children in the graph, and vice versa). The footer is matched by the
  anchored `^añade\s+(?:las\s+)?consecuencias\b`, so an effect line that merely
  starts with "Añade" cannot truncate a card (the Amoníaco card prints the footer
  without "las").
- `Límite de Supervivencia +N` becomes `survivalLimitBonus` (default `0`). The
  parser asserts the number of matched `Límite de Supervivencia +N` lines equals
  the number of cards with a non-zero bonus, and that no `Límite de Supervivencia`
  text leaked into a description.
- Every card is asserted to have a non-empty `name` and `description`.
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

One Básicos card is genuinely named `???` (an unidentified monster part), so the
generated file carries a comment marking it as a real card name, not a parse artifact.
It appears twice in the deck with identical text and is deduped to a single `BASIC` entry.

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

## Settlement locations

`ingest_locations.py` reads `AF Asentamientos Loc.pdf` and writes
`core/src/commonMain/kotlin/io/kdmtracker/core/content/generated/Core15Locations.kt`
as a `ContentBundle` with `locations = List<SettlementLocation>`, each carrying its
build prerequisites, build cost, and actions.

```sh
python3 tools/content-ingest/ingest_locations.py \
  --pdf "path/to/external-assets/Juego Completo/Formato XXL 102x204 mm/AF Asentamientos Loc.pdf"
```

`--pdf` overrides the source PDF (it defaults to that path) and `--out` the generated file.

### Parsing model

- The PDF is 14 pages: a blank cover, then one location card per page. The page count is
  cross-checked against `pdfinfo`, and the blank page is skipped.
- The first non-blank line of the default reading order is the location **name**.
- A **build block** is a `Construir <Target>` line. Building `<Target>` requires the card
  it is printed on, so `<Target>` gains `requires = [card name]` and `buildEndeavors = 1`
  (the rulebook charges one endeavor per location; the card prints only the resources).
  The three bars on the Lantern Hoard (Taller de Huesos / Peletería / Molino de Órganos)
  carry no resource cost.
- The Lantern Hoard's **Innovate** action is parsed from its `Esfuerzo` block:
  `kind = INNOVATE`, `endeavorCost = 1`, `resourceCost = {Hueso de Monstruo: 1, Órgano de
  Monstruo: 1, Piel de Monstruo: 1}`, `oncePerPhase = true`, `draw = 2`, `keep = 1`, and
  the printed prose as `effect`.
- Resource tokens resolve to the canonical names used by `Core15Resources.kt`
  (`hueso` → `Hueso de Monstruo`, …). A token with no card definition is kept verbatim,
  and nothing is invented: a location whose build cost is not on this PDF is emitted bare.

### Determinism

Locations are sorted by name; the output embeds no timestamps and no paths, so a re-run is
byte-identical:

```sh
python3 tools/content-ingest/ingest_locations.py --pdf "<pdf>"
git diff --exit-code core/src/commonMain/kotlin/io/kdmtracker/core/content/generated/Core15Locations.kt
```

### Known parse gaps

- **Overlapping build bar on Peletería.** The source PDF draws a stray copy of the Taller
  de Huesos → Armería bar on top of the visible Peletería → Curtiduría bar, so the default
  reading order merges both cost boxes. The generator reads the cost from the `-raw`
  content order (which binds the visible cost to `Curtiduría`) and falls back to the
  default order elsewhere; this is the only card affected.
- **`restos` (scrap) has no card.** Core15Resources.kt defines no `restos` card, so the
  token is kept verbatim should it reach a cost box. Only gear recipes (not ingested here)
  use it today.
- **Locations outside the build tree.** Plumería, Felinarium, Cirujano Barbero, Artesano
  de Máscaras, Herrería and Cúmulo de Linternas Extinto publish no `Construir` bar on any
  base location card; they are unlocked by innovations, so their build costs live on the
  innovation cards and are not extracted here. They are emitted as bare names.
- **Activities beyond Innovate.** The base cards print other activities (Búsqueda de
  Restos, Trepanación, Augurio, Investigar, Experiencia Compartida) as d10 tables/prose.
  Only the Lantern Hoard's `Esfuerzo` Innovate action is modeled here; the rest are
  deferred.

## Known gaps / not ingested

Only decks with an extractable text layer are covered today. The rest are deferred to
later ingestion waves:

- **Image-only PDFs (OCR/vision later).** Several decks are scanned art with no text
  layer — the **Gorm** expansion cards, and the **monster AI / terrain / gear** cards where
  scanned. `pdftotext` returns nothing usable for them, so they need OCR or a vision model
  before they can be parsed.
- **`AF Artes de Combate Secretas.pdf`.** The **13 secret fighting arts** are not yet
  ingested; `ingest_fighting_arts.py` reads only the public fighting-arts and disorders
  decks.
- **Cost-icon glyphs dropped.** `pdftotext` omits the PDF's icon glyphs, so a cost icon
  leaves a dangling empty grant in a few cards (e.g. `Doble Esprint`, `Berserker`), and a
  few innovation lines begin with a stray `:`.
- **Source placeholder kept verbatim.** The `Apatía` card's placeholder art text
  (`Lorem ipsum`) is part of the extracted body and is not cleaned up.

