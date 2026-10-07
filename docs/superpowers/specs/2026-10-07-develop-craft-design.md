# Develop Craft (Gear) — Design

**Status:** Draft (awaiting review)
**Slice:** Crafting in the Settlement Phase **Develop** step — model gear + recipes in `:core`, extract the base 1.5 gear content, and let the Develop screen craft at built locations.
**Depends on:** PR #7 (settlement domain + content packs), PR #8 (1.5 content ingestion), PR #9 (settlement record persistence), PR #10 (Steps 1 & 7 UI + rules layer).

## Goal

Complete the Develop (Step 7) step by adding its headline missing activity: **Craft**. A player picks a **built** location, picks a gear the location can craft, pays the **recipe's resources**, and gains the **gear** at the settlement.

Success is:
1. `:core` models `Gear` and its recipes, and a `Craft` use-case enforces built-location + storage + prerequisite rules, returning a typed `StepResult`.
2. The recipe + gear content is **extracted from the source cards** and pinned by fixture tests — never invented.
3. The Develop screen offers a Craft affordance (built location → recipes → craft) that **persists** the settlement.
4. Rules live in `:core`; the UI decides nothing.

## Scope

### In
- `:core`: `Gear`, `GearRecipe`, `SettlementLocation.recipes`, `SettlementState.gear`, catalog gear views, and the `Craft` use-case.
- Content extraction: recipes from the **location cards** (`Construir`-adjacent gear blocks) and gear definitions from the **gear cards** (`AF Equipo/*.pdf`) → a generated pack, fixture-pinned.
- The app: a **Craft** section in `DevelopStepScreen` + `LanternYearController.craft(...)`.

### Out (deferred, explicitly)
- **Gear stats** (armor points, attribute modifiers) — icon-heavy on the cards; not needed for crafting.
- **Survivor assignment / gear grids** — needs the deferred `Survivor` model.
- **Crafting prerequisites beyond innovations** — a gear's `Requiere <innovation>` is modeled; other gates are display-only.
- **Effect execution** — gear rules text is displayed; the group resolves it.
- **1.6-specific content** — local sources are 1.5.

## Rules model (grounded)

| Rule | Value | Source |
| --- | --- | --- |
| Craft | Spend resources at a **built** location to create gear; the recipe's resource cost is printed per location | 1.5 rulebook §7 (Develop); `AF Asentamientos Loc.pdf` |
| Storage | The resources must exist at craft time; spent resources are removed | 1.5 rulebook p.83 |
| Prerequisite | Some gear requires an innovation (e.g. `Requiere Calor`, `Requiere Amoníaco`) | Gear/location cards |
| Gear | A gear card carries its **name**, **keywords** (e.g. `arma`, `armadura`, `objeto`), and **rules text** | `AF Equipo/*.pdf` |

**Fidelity boundary:** we enforce **built location + recipe + storage + innovation prerequisite**. Gear stats are not modeled; effects are displayed as text.

## Architecture

### 1. Content model (`:core`, pure, additive)

New types (all `@Serializable`, defaults so existing content compiles):
- `Gear(name: String, keywords: List<String> = emptyList(), rules: String = "")`
- `GearRecipe(gearName: String, resourceCost: Map<String, Int> = emptyMap(), requires: List<String> = emptyList())`

Extended (defaulted):
- `SettlementLocation.recipes: List<GearRecipe> = emptyList()` — recipes travel on the location (same pattern as `actions`), because the location cards print them there.
- `SettlementState.gear: List<String> = emptyList()` — the settlement's crafted gear, by name.

`ContentCatalog` gains `gearDefinitions: List<Gear>` (merged/deduped by name) and exposes recipes via `locationDefinitions`.

### 2. Use-case (`:core`, pure, TDD)

```kotlin
object Craft {
    fun run(record: SettlementRecord, locationName: String, gearName: String, catalog: ContentCatalog): StepResult<SettlementRecord>
}
```
Validates, in order (all `Blocked`, never throw): the location exists with a matching recipe; the location is **built**; the recipe's `requires` are all unlocked innovations; storage holds `resourceCost`. On success: spend the recipe's resources (via the existing ledger guard) and add `gearName` to `state.gear` (deduped).

### 3. Content extraction (`tools/content-ingest/`)

`ingest_gear.py` (Python 3 stdlib, deterministic, `--pdf`/`--out`):
- **Recipes** from the location card PDF (`AF Asentamientos Loc.pdf`): each craftable block under a location is `<gear name>` + `<N x resource>` cost tokens; attach as `SettlementLocation.recipes`.
- **Gear definitions** from the gear card PDFs (`AF Equipo/*.pdf`, one card file per location set): page = card → `Gear(name, keywords, rules)`.
- Emit a generated Kotlin pack (`Core15Gear.kt`) wired into `Legacy15Content`; fixture tests pin representative recipes + a gear definition; re-run is byte-identical.

### 4. App (`:app-android`)

- `DevelopStepScreen` gains a **Craft** section: choose a built location → list its recipes (gear + resource cost + prerequisites) → craft. Effects (gear rules) shown as text.
- `LanternYearController.craft(locationName, gearName)` calls the `:core` use-case, persists `Ok` via `SettlementRepository.update(record)`, surfaces `Blocked(reason)`.
- `LanternYearViewModel` exposes `craft`; `KdmTrackerApp` wires it (as with the other Develop actions).

## Data flow

`Campaign.enabledPacks` → `ContentCatalog` (with `gearDefinitions` + location `recipes`).
`SettlementRecord` ↔ `Craft` use-case ↔ controller ↔ `UiState` → Develop screen.
Every craft persists through `SettlementRepository.update(record)` (local store + outbox), like the other Develop actions.

## Error handling

Rules are **total**: invalid crafts return `StepResult.Blocked(reason)` (location not built, no such recipe, prerequisite missing, insufficient storage) — never an exception. The UI shows the reason inline and leaves the record unchanged.

## Testing

- **`:core`** — strict TDD for `Craft`: not-built location, no recipe, missing prerequisite, insufficient storage → `Blocked`; a valid craft spends the cost and adds the gear; re-crafting the same gear is handled (deduped).
- **Extraction** — fixture tests pin representative recipes + one gear definition; determinism (byte-identical re-emit).
- **`:app-android`** — a controller test over the real store: craft at a built location persists (fresh repository reads it); an invalid craft surfaces `Blocked`.
- Full suite green: `./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck` and the Python suite.

## Risks

- **Recipe extraction accuracy** — the location cards mix gear blocks and other bars; pin with fixtures and `[gap]` anything ambiguous.
- **Gear name matching** — recipes (location cards) and definitions (gear cards) must join by name; a mismatch leaves a recipe with no definition (display-only cost, still craftable) — document the join rule.
- **Scope creep into stats** — armor/attribute icons are explicitly out; do not attempt them here.

## References

- `external-assets/KDM 1.5 EdESP Reglamento - Digital.pdf` (§7 Develop, p.83)
- `external-assets/Juego Completo/Formato XXL 102x204 mm/AF Asentamientos Loc.pdf` (recipes)
- `external-assets/Juego Completo/Formato Cuadrado 51x51 mm/AF Equipo/*.pdf` (gear definitions)
- `docs/superpowers/specs/2026-10-07-settlement-steps-1-7-design.md` (the Develop rules model this extends)
- `server/src/main/resources/static/prototypes/research/lantern-year-steps.md` §7
