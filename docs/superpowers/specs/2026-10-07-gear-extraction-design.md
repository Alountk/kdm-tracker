# Gear Content Extraction (bbox pipeline) — Design

**Status:** Draft (awaiting review)
**Slice:** Replace the hand-curated gear seed with an **automatic extractor** that reads the 1.5 gear cards and settlement-location cards, using a coordinate-aware text layer.
**Depends on:** PR #11 (Develop Craft — model + curated seed + UI) and the PR #8 content-ingest pipeline.

## Goal

Extract the full 1.5 **gear definitions** (name, keywords, rules) and the **crafting recipes** (gear → resource cost + prerequisite) from the card PDFs automatically, deterministically, and pinned by fixtures — so `Craft` has real content instead of one curated location.

Success is:
1. `ingest_gear.py` reads the cards with `pdftotext -bbox-layout` and emits committed Kotlin.
2. Gear definitions and recipes come from the source cards **verbatim**, or are recorded as `[gap]` — never guessed.
3. The curated `Core15GearSeed` is replaced by the generated pack; `Craft` works over the extracted content.
4. The pipeline stays **stdlib-only** (no new dependency) and its output is a no-op on re-run.

## Scope

### In
- A new extractor `tools/content-ingest/ingest_gear.py` (stdlib) built on `pdftotext -bbox-layout`.
- A small model addition: `GearRecipe.requiresText: String? = null` (display-only prerequisite text).
- Generated `Core15Gear.kt` (gear definitions + recipe-bearing locations), wired into `Legacy15Content`; the curated seed removed.
- Fixture tests pinning representative gear, recipes, prerequisites, and determinism.

### Out (deferred, explicitly)
- **Gear `modifiers` / stats** — the card bonuses are conditional rule text; still `[gap]` (a vision/LLM pass is the follow-up).
- **Image-only cards** and 1.6 content.
- **Executable effects** — rules text stays display-only.

## Method (grounded by a spike)

`pdftotext` in `-raw` strips spaces and in `-layout` splits columns, which is why the first extraction attempt failed. `pdftotext -bbox-layout` instead emits an XHTML tree of `<block>` → `<line>` → `<word>` with `xMin/yMin/xMax/yMax`, so text is recoverable **with spaces** and **columns are separable by x**. Verified on the skill cards:
- Gear card (`Yelmo de Calavera`): `Yelmo de Calavera` / `armadura, hueso, frágil` / rules prose as distinct blocks.
- Location card (`Armería`): gear names at `x≈80/219` with their cost block at `x≈81/219`, `y+≈20`; `Requiere Calor` at `x≈286` near its gear.

Parsing model:
- **Gear card page** → `Gear`: the top/largest block is the **name**; a block that is a comma-separated lowercase list is the **keywords**; the remaining blocks joined are the **rules**. Cards repeat across pages → dedup by name (first wins).
- **Location card page** → the top block is the **location name**; a recipe is a name block + the cost block directly below in the same column; a nearby `Requiere X` block is the **prerequisite**.
- **Prerequisite mapping (Ruling):** if `X` matches an innovation name in the pack → `requires = [X]` (real gating); otherwise → `requiresText = X` and `requires = []` (e.g. `Requiere Calor`: "Calor" is a state granted by the `Horno Linterna` innovation, not an innovation name).
- Deterministic: sorted, no timestamps/paths; re-run is byte-identical.

## Architecture

### 1. Model (`:core`, additive)
- `GearRecipe.requiresText: String? = null` — the card's prerequisite label when it is not an innovation name.

### 2. Extractor (`tools/content-ingest/ingest_gear.py`)
- A thin `bbox_text(pdf)` wrapper over `pdftotext -bbox-layout` (extend `pdf_text.py` with a `bbox=True` mode, or a local helper).
- Pure functions `parse_gear_cards(xml) -> List[Gear]` and `parse_location_recipes(xml, innovationNames) -> List[SettlementLocation]`, unit-tested with small bbox fixtures.
- Emits `Core15Gear.kt` with `gear` + `locations`.

### 3. Content + wiring
- `Legacy15Content` includes the generated gear + recipe locations; the curated `Core15GearSeed` is deleted.
- The location merge (field-combining) already combines the recipe locations with the generated build locations by name.

### 4. App
- No app change: `Craft`/the Develop screen already consume `catalog.gearDefinitions`/recipes.

## Testing

- **Extractor** — stdlib `unittest` fixtures pin: one gear definition (name/keywords/rules), one recipe (gear + cost), a mapped prerequisite (`requires`) and an unmapped one (`requiresText`), and byte-identical re-emit.
- **`:core`** — a content test asserts every extracted recipe's `resourceCost` names resolve against the 1.5 resources, and every `requires` entry resolves to an innovation (the `requiresText` ones do not gate).
- Full suite green: `./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck` + the Python suite.

## Risks

- **Block classification** — name vs keywords vs rules by geometry can misfire on unusual cards; pin with fixtures and `[gap]` the ambiguous ones.
- **Recipe column pairing** — the location cards wrap costs and place `Requiere` offsets; pair by column + nearest-y with a tolerance, and pin the tricky cases.
- **Content volume** — the generated pack is large; it is committed (like the other generated packs) and never read from a PDF at runtime.

## References

- `external-assets/Juego Completo/Formato Cuadrado 51x51 mm/AF Equipo/*.pdf` (gear)
- `external-assets/Juego Completo/Formato XXL 102x204 mm/AF Asentamientos Loc.pdf` (recipes)
- `tools/content-ingest/README.md` (existing pipeline)
- `docs/superpowers/specs/2026-10-07-develop-craft-design.md` (the model this extends)
