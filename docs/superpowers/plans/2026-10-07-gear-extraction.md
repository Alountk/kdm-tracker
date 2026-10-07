# Gear Content Extraction (bbox pipeline) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the hand-curated gear seed with an automatic, stdlib-only extractor that reads the 1.5 gear and settlement-location cards via `pdftotext -bbox-layout` and emits committed Kotlin.

**Architecture:** A new `tools/content-ingest/ingest_gear.py` parses the `-bbox-layout` XHTML tree (blocks/lines/words with coordinates) with pure functions; it emits `Core15Gear.kt` (gear definitions + recipe-bearing locations), replaces `Core15GearSeed`, and is wired into `Legacy15Content`.

**Tech Stack:** Python 3 stdlib + poppler (`pdftotext`), Kotlin 2.3.21, `kotlin.test`.

**Spec:** `docs/superpowers/specs/2026-10-07-gear-extraction-design.md`

## Global Constraints

- Python standard library only; deterministic output (sorted; no timestamps/paths); re-run is byte-identical.
- The PDFs under `external-assets/` are git-ignored and never committed; the generated Kotlin is committed.
- `:core` stays pure; no new dependencies.
- Extracted values come from the cards verbatim; anything ambiguous is a `[gap]`, never a guess.
- Conventional Commits; **no** `Co-Authored-By`/AI attribution. Technical artifacts in English.
- Green: `python3 -m unittest discover -s tools/content-ingest/tests -v` and `./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck`.

## Review Focus

1. **Determinism** — re-running the generator leaves `git diff --exit-code` clean. ← Task 4.
2. **Prerequisite mapping** — a `Requiere X` that matches an innovation name goes to `requires` (gates `Craft`); an unmapped one goes to `requiresText` and does **not** gate. ← Task 3/5.
3. **Block classification** — name / keywords / rules correctly split by geometry, cards deduped by name. ← Task 3.
4. **Recipe column pairing** — name↔cost paired by column + nearest y; wrapped/offset costs handled. ← Task 3.
5. **Content integrity** — every emitted recipe's resources and `requires` resolve against the shipped pack. ← Task 5.

---

## File Structure

```text
core/src/commonMain/kotlin/io/kdmtracker/core/domain/GearRecipe.kt   # + requiresText
tools/content-ingest/pdf_text.py                                     # + bbox_text
tools/content-ingest/ingest_gear.py                                  # NEW: parsers + emitter
tools/content-ingest/tests/test_gear_parser.py                       # NEW
tools/content-ingest/README.md                                       # + gear section
core/.../content/generated/Core15Gear.kt                             # GENERATED
core/.../content/Legacy15Content.kt                                  # wire; drop the seed
core/.../content/Core15GearSeed.kt                                   # DELETE
core/src/commonTest/kotlin/io/kdmtracker/core/content/Core15GearContentTest.kt   # integrity
```

---

### Task 1: `GearRecipe.requiresText`

**Files:**
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/domain/GearRecipe.kt`
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/domain/GearModelTest.kt`

**Interfaces:**
- Produces: `GearRecipe(gearName, resourceCost = emptyMap(), requires = emptyList(), requiresText: String? = null)` (new param defaulted); a display-only prerequisite label that does not gate `Craft`.

- [ ] **Step 1: Write the failing test** — `GearRecipe("X")` has `requiresText == null`; a `GearRecipe("X", requiresText = "Calor")` round-trips through `Json`; a legacy `{"gearName":"X"}` JSON decodes with `requiresText == null`.
- [ ] **Step 2: Run** `./gradlew :core:jvmTest` — expected FAIL.
- [ ] **Step 3: Implement** the defaulted param + KDoc.
- [ ] **Step 4: Run** `./gradlew :core:jvmTest spotlessCheck` — expected GREEN.
- [ ] **Step 5: Commit** `feat(core): add a display-only recipe prerequisite`.

---

### Task 2: `-bbox-layout` text access + block model

**Files:**
- Modify: `tools/content-ingest/pdf_text.py`
- Create: `tools/content-ingest/bbox.py`
- Test: `tools/content-ingest/tests/test_bbox.py`

**Interfaces:**
- Produces:
  - `pdf_text.bbox_text(pdf_path, first=None, last=None) -> str` — runs `pdftotext -bbox-layout` (optionally a page range) and returns the XHTML.
  - `bbox.Block(x0, y0, x1, y1, text)` and `bbox.parse_blocks(xml) -> List[List[Block]]` (one list per `<page>`), plus `bbox.blocks_of(text)` helper if useful.
  - `parse_blocks` reads `<block>` → `<line>` → `<word>` and joins a block's words with single spaces (the bbox layer preserves word boundaries).

- [ ] **Step 1: Write the failing test** — feed a small hand-written `-bbox-layout` XHTML snippet to `parse_blocks`; assert one page with the expected blocks (coords + `text`, e.g. `Yelmo de Calavera`, `armadura, hueso, frágil`); assert an empty `<page>` yields no blocks.
- [ ] **Step 2: Run** `python3 -m unittest discover -s tools/content-ingest/tests -v` — expected FAIL.
- [ ] **Step 3: Implement** `bbox_text` (subprocess, like `pdf_text`) + `bbox.py` (`xml.etree.ElementTree`).
- [ ] **Step 4: Run** the Python suite — expected GREEN.
- [ ] **Step 5: Commit** `feat(content): read pdftotext bbox-layout blocks`.

---

### Task 3: Gear + recipe parsers

**Files:**
- Create: `tools/content-ingest/ingest_gear.py`
- Test: `tools/content-ingest/tests/test_gear_parser.py`

**Interfaces:**
- Consumes: `bbox.parse_blocks` / `Block` (Task 2); `kotlin_emit` helpers.
- Produces:
  - `parse_gear_card(page_blocks) -> Optional[Gear]` — the top/largest block is the name; a block that is a comma-separated lowercase keyword list is the keywords; the remaining blocks joined are the rules; rules-empty cards still emit.
  - `parse_gear_cards(pages) -> List[Gear]` — dedup by name (first wins), sorted by name.
  - `parse_recipes(page_blocks, innovation_names) -> SettlementLocation` — the top block is the location name; each recipe is a name block + its cost block directly below in the same column (nearest `y` within a tolerance); a nearby `Requiere X` maps to `requires = [X]` when `X in innovation_names`, else `requiresText = X`.
  - `parse_cost("2 x hueso, 1 x piel") -> Dict[str, int]`.

- [ ] **Step 1: Write the failing fixture tests** — a gear-card fixture emits `Gear("Yelmo de Calavera", keywords = ["armadura","hueso","frágil"], rules = "…")`; a location fixture emits a recipe (`Hacha de Contrapeso` → `{"hueso": 2, "piel": 1, "órgano": 1}`); a `Requiere Calor` fixture → `requiresText = "Calor"` (not gating) and a `Requiere <innovation>` fixture → `requires = [<innovation>]`; duplicate cards dedup.
- [ ] **Step 2: Run** `python3 -m unittest discover -s tools/content-ingest/tests -v` — expected FAIL.
- [ ] **Step 3: Implement** the pure parsers (geometry by column `x` and nearest `y`; `[gap]` anything ambiguous).
- [ ] **Step 4: Run** the Python suite — expected GREEN.
- [ ] **Step 5: Commit** `feat(content): parse gear cards and location recipes`.

---

### Task 4: Emit `Core15Gear.kt`, wire it, drop the curated seed

**Files:**
- Modify: `tools/content-ingest/ingest_gear.py` (emitter + `main`)
- Create (generated): `core/.../content/generated/Core15Gear.kt`
- Modify: `core/.../content/Legacy15Content.kt`
- Delete: `core/.../content/Core15GearSeed.kt`
- Modify: `core/src/commonTest/kotlin/io/kdmtracker/core/content/Core15GearSeedTest.kt` (rename/rewrite for the generated pack)
- Modify: `tools/content-ingest/README.md`

**Interfaces:**
- Consumes: the Task 3 parsers.
- Produces: `object Core15Gear { val bundle: ContentBundle }` with `gear: List<Gear>` and `locations: List<SettlementLocation>` (recipes + `requiresText`).

Run against the untracked PDFs (pass paths explicitly):
- `/Volumes/Mac_Nvme/Dev/10_26_kdm-tracker/external-assets/Juego Completo/Formato Cuadrado 51x51 mm/AF Equipo/*.pdf`
- `/Volumes/Mac_Nvme/Dev/10_26_kdm-tracker/external-assets/Juego Completo/Formato XXL 102x204 mm/AF Asentamientos Loc.pdf`

- [ ] **Step 1: Write the failing test** — a test asserts the committed `Core15Gear.bundle.gear` is non-empty and contains a known gear (e.g. `Yelmo de Calavera`) with its keywords, and that a known location exposes a recipe.
- [ ] **Step 2: Run** `./gradlew :core:jvmTest` — expected FAIL (no `Core15Gear`).
- [ ] **Step 3: Implement** the emitter; run it against the PDFs; wire `Legacy15Content` to `Core15Gear.bundle.gear` + `...locations`; delete `Core15GearSeed` and its use; document the pipeline + any `[gap]`s in the README.
- [ ] **Step 4: Run** the Python suite + `./gradlew :core:jvmTest spotlessCheck` — GREEN; re-run the generator → `git diff --exit-code` clean.
- [ ] **Step 5: Commit** `feat(content): generate and wire the 1.5 gear pack`.

---

### Task 5: Content integrity + verification sweep

**Files:**
- Create: `core/src/commonTest/kotlin/io/kdmtracker/core/content/Core15GearContentTest.kt`
- Modify: `README.md`

**Interfaces:**
- Consumes: `Core15Gear`, `Legacy15Content`.

- [ ] **Step 1: Write the failing test** — every `Core15Gear` recipe's `resourceCost` names resolve against `Legacy15Content` resources, and every `requires` entry resolves to a `Legacy15Content` innovation (a `requiresText` entry is allowed not to resolve); every gear name is non-blank.
- [ ] **Step 2: Run** `./gradlew :core:jvmTest` — expected FAIL if any content is inconsistent (fix the extractor or `[gap]` the entry).
- [ ] **Step 3:** Run the full suite `./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck` and the Python suite; record per-module counts.
- [ ] **Step 4:** Update the README: the gear pipeline is now automatic and stdlib-only; note the `requiresText` (display-only prerequisite) and the `modifiers` `[gap]`; correct the "curated seed" wording from the Craft slice.
- [ ] **Step 5: Commit** `docs: document the gear extraction pipeline`.

---

## Self-Review

**1. Spec coverage:** method/bbox (T2), gear parser (T3), recipe parser + prerequisite mapping (T3), model `requiresText` (T1), generated pack + wiring + seed removal (T4), integrity + docs (T5). Deferred (`modifiers`, image cards, 1.6) not tasked — correct.

**2. Step scan:** each step is one action with a checkable result; fixtures carry the exact values; no code transcripts.

**3. Type consistency:** `Block`/`parse_blocks` (T2) → parsers (T3) → emitter (T4) → `Core15Gear` (T4) → integrity (T5); `requiresText` (T1) flows through T3/T4/T5.

**4. Review Focus:** each of the five lines maps to the named task.

**5. Proportion:** task-level; strict-TDD steps; no transcripts.
