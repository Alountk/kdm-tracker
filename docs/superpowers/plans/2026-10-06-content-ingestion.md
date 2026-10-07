# Content Ingestion Pipeline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the KDM 1.5 card PDFs (local, untracked) into **committed, generated Kotlin content packs** (innovations, resources, fighting arts/disorders) that `:core` composes, so the settlement steps have real content.

**Architecture:** The PDFs live in `external-assets/` (local input, git-ignored — the rulebook alone is 135 MB). A small Python pipeline in `tools/content-ingest/` parses the text-extractable PDFs with `pdftotext` and emits **generated Kotlin source** into `core/.../content/generated/`. The generated source is committed; the app never reads a PDF. `:core` tests validate the generated content (counts, spot checks, the consequence graph).

**Tech Stack:** Python 3 + `pdftotext`/`pdfinfo` (poppler, already installed) for the pipeline; Kotlin `:core` for the generated packs + tests. Strict TDD applies to the `:core` side (the generated content is validated by tests).

**Spec:** `docs/superpowers/specs/2026-10-05-kdm-tracker-offline-first-kmp-design.md`
**Inputs:** `external-assets/Juego Completo/Formato Americano 57x88 mm/**` and `external-assets/expansiones/gorm/**` (1.5, Spanish).

## Global Constraints

- `external-assets/` must be **git-ignored** (never commit the PDFs, especially the 135 MB rulebook).
- Generated Kotlin is **committed** and must compile + pass `spotlessCheck`.
- Pipelines are **reproducible**: re-running the script over the same PDFs yields byte-identical output (sorted, no timestamps).
- Content stays **data**: the generated packs provide the typed definitions the domain reads (`Innovation` with `consequences`/`survivalLimitBonus`, `ResourceDefinition` with a typed category, …).
- No new Kotlin dependencies. Conventional Commits; **no** `Co-Authored-By`/AI attribution.
- `./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck` stays green.

## Review Focus

1. **The pipeline is reproducible**: re-running it produces identical output (deterministic ordering, no timestamps/paths).
2. **The generated content is faithful**: a spot check matches the card (e.g. `Lenguaje` has `survivalLimitBonus == 1`; `Linterna Interior` is listed as a **consequence** of `Lenguaje`).
3. **The consequence graph is correct**: an innovation's `consequences` = the cards tagged `consecuencia de <parent>`, not fabricated from the "Añade las consecuencias…" prose alone.
4. **Nothing untracked leaks in**: `external-assets/` is ignored; no PDF is committed.
5. **The generated file is valid Kotlin** and compiles in `:core`.

---

## File Structure

```text
.gitignore                                  # + external-assets/
tools/content-ingest/
├── README.md                              # how to run the pipeline
├── pdf_text.py                            # pdftotext/pdfinfo helpers
├── ingest_innovations.py                  # AF Innovaciones.pdf -> generated Kotlin
├── ingest_resources.py                    # AF Recursos/** -> generated Kotlin
└── ingest_fighting_arts.py                # AF Artes de Combate / Trastornos -> generated Kotlin
core/src/commonMain/kotlin/io/kdmtracker/core/content/
├── generated/Core15Innovations.kt         # GENERATED (committed)
├── generated/Core15Resources.kt           # GENERATED (committed)
├── generated/Core15FightingArts.kt        # GENERATED (committed)
└── Legacy15Content.kt                     # assembles the packs
core/src/commonTest/kotlin/io/kdmtracker/core/content/
└── GeneratedContentTest.kt
```

---

### Task 1: Ignore `external-assets` + the innovations pipeline

**Files:**
- Modify: `.gitignore` (add `external-assets/`)
- Create: `tools/content-ingest/README.md`, `tools/content-ingest/pdf_text.py`, `tools/content-ingest/ingest_innovations.py`
- Create (generated): `core/src/commonMain/kotlin/io/kdmtracker/core/content/generated/Core15Innovations.kt`
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/content/GeneratedInnovationsTest.kt`

**Interfaces:**
- Produces: `object Core15Innovations { val bundle: ContentBundle }` (a `ContentBundle` whose `innovations` are the parsed cards) — generated, committed.
- Parser contract: for each card, extract `name`, `tags`, `parent` (from `consecuencia de <parent>`), `survivalLimitBonus` (from `Límite de Supervivencia +N`, default 0), and `description` (the effect text). Build `consequences` = the names of cards whose `parent` is this card. Emit `Innovation(name, description, consequences, survivalLimitBonus)` entries **sorted by name**.
- Determinism: no timestamps, no absolute paths, stable ordering.

- [ ] **Step 1:** Add `external-assets/` to `.gitignore`; verify `git status` no longer shows it (and the 135 MB PDF is not staged).
- [ ] **Step 2:** Write `tools/content-ingest/pdf_text.py` (wrap `pdftotext -layout`) and `ingest_innovations.py` (parse + emit Kotlin). Run it; open the generated file.
- [ ] **Step 3:** Write the failing `GeneratedInnovationsTest`: the bundle is non-empty; `Lenguaje` exists with `survivalLimitBonus == 1`; `Linterna Interior` is in `Lenguaje.consequences`; a known card's description is non-blank; the names are sorted.
- [ ] **Step 4:** Run `./gradlew :core:jvmTest` — expected FAIL (generated file missing / empty).
- [ ] **Step 5:** Iterate the parser until the test passes; run `./gradlew :core:jvmTest spotlessCheck`.
- [ ] **Step 6:** Re-run the parser and assert the output is byte-identical (`git diff --exit-code` on the generated file) — determinism. Commit `feat(content): ingest the 1.5 innovations`.

---

### Task 2: `Legacy15Content` pack + resources pipeline

**Files:**
- Create: `tools/content-ingest/ingest_resources.py`
- Create (generated): `core/.../content/generated/Core15Resources.kt`
- Create: `core/.../content/Legacy15Content.kt`
- Test: `core/.../content/Legacy15ContentTest.kt`

**Interfaces:**
- Produces: `object Legacy15Content { val pack: ContentPack }` — a `ContentPack(id = "core-1.5", version = "1.5", name = "Kingdom Death 1.5 (ES)", bundle = ContentBundle(innovations = Core15Innovations.bundle.innovations, resources = Core15Resources.bundle.resources))`; `ContentCatalogComposer.compose(listOf(Legacy15Content.pack))` succeeds.
- `Core15Resources`: the base resources with a **typed** category (`ResourceCategory`), parsed from `AF Recursos/**`.
- Test: the pack composes; the resources are non-empty and typed; a spot check (e.g. `Bone` is a `MONSTER` or `BASIC` resource per the cards — state which and assert it).

- [ ] **Step 1:** Write the failing `Legacy15ContentTest` (compose succeeds; innovations + resources present; typed categories).
- [ ] **Step 2:** Run `./gradlew :core:jvmTest` — expected FAIL.
- [ ] **Step 3:** Write `ingest_resources.py`, generate `Core15Resources.kt`, implement `Legacy15Content`.
- [ ] **Step 4:** Run `./gradlew :core:jvmTest spotlessCheck` — expected PASS; confirm determinism. Commit `feat(content): add the 1.5 content pack with resources`.

---

### Task 3: Fighting arts + disorders pipeline

**Files:**
- Create: `tools/content-ingest/ingest_fighting_arts.py`
- Create (generated): `core/.../content/generated/Core15FightingArts.kt`
- Modify: `core/.../content/Legacy15Content.kt`
- Test: `core/.../content/FightingArtsTest.kt`

**Interfaces:**
- Extend `ContentBundle` (if needed) with `fightingArts: List<...>` and `disorders: List<...>` (minimal typed definitions: `name`, `description`), or reuse an existing generic definition type — keep it minimal and typed.
- Test: the fighting arts / disorders are non-empty; a known card is present; the pack composes.

- [ ] **Step 1:** Write the failing `FightingArtsTest`.
- [ ] **Step 2:** Run — expected FAIL.
- [ ] **Step 3:** Write the parser, generate the Kotlin, extend the bundle/pack.
- [ ] **Step 4:** Run — expected PASS. Commit `feat(content): ingest fighting arts and disorders`.

---

### Task 4: Verification sweep + README

**Files:**
- Modify: `README.md`, `tools/content-ingest/README.md`

- [ ] **Step 1:** Run the full suite `./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck`; record per-module counts.
- [ ] **Step 2:** Document the pipeline: the inputs (untracked PDFs), how to re-run each script, the determinism guarantee, and that the generated Kotlin is committed. Note the **image-only** PDFs (Gorm cards, monster AI) are out of scope here (OCR/vision later).
- [ ] **Step 3:** Commit `docs: document the content-ingestion pipeline`.

---

## Self-Review

**1. Spec coverage**

| Spec item | Covered by |
| --- | --- |
| Content as data / modular packs | Tasks 1–3 |
| Real 1.5 content (innovations, resources, fighting arts) | Tasks 1–3 |
| Reproducible pipeline | Tasks 1–4 |
| The `gorm` expansion pack | **Deferred** — its card PDF is image-only (OCR/vision) |
| Monster AI / hunt / terrain / gear cards | **Deferred** — later ingestion waves |
| Steps 1–11 UI wiring | **Deferred** — the next slice |

**2. Step scan:** each step is one action with a checkable result; the parser contract and the generated shapes are exact.

**3. Type consistency:** `Core15Innovations.bundle` (Task 1) is consumed by `Legacy15Content` (Task 2); the generated files reference `:core`'s `Innovation`/`ResourceDefinition`/`ContentBundle`.

**4. Review Focus:** each of the five lines maps to a task with checks (1, 2, 1, 1, 1).

**5. Proportion:** task-level; no code transcripts beyond the parser contract.
