# Develop Step — the Fabricar Flow (Part A, Phase 1)

**Status:** Draft (awaiting review)
**Depends on:** PR #33 (`feat/settlement-screen`) merged — this work edits the slice-6 screen files it introduces.
**Shape:** Phase 1 of 3 of "Part A" (the Develop step's three flows from the Stitch design). Phase 2 = Innovar (draw-2-keep-1), Phase 3 = Añadir recursos; the content/identity program (S1/S2) is already merged (#31, #32).

## Goal

Rebuild the step-7 **crafting flow** as the section the Stitch design specifies — one "Localizaciones de Asentamiento" section whose built-location cards contain their crafting catalog, their action, and their inline build upgrade — retiring the current `Fabricar` tab and the duplicated build list. Pure presentation work over existing rules: `:core` does not change.

## Source of truth

- **Design:** Stitch project `KDM Mobile Companion App`, screen `59fdd89d6100424eb9b5bbf1a6e74ffa` ("Fase Asentamiento", 780×6128). The relevant region is the *Localizaciones de Asentamiento* area: header counts, the built card with `CATÁLOGO DE FABRICACIÓN (N CARTAS)` expandable (per-recipe `FABRICAR`/`BLOQUEADO` rows, `REQUIERE <innovation>` badges, cost chips), the inline `Construir X (MEJORA)` → `ERIGIR` row, and the `POR ERIGIR` cards with `CONSTRUIR`.
- **Content authority:** the identity CSVs and generated packs (spec `2026-10-10-content-identity-design.md` §7), not the mock's copy.

## Decisions (validated with the user)

1. **Phased migration (approach A):** each Part-A phase rebuilds one slice-6 section to design fidelity and retires its tab; the screen converges to the design's continuous page without a big-bang rewrite.
2. **Fabricar first** — the most-used flow, whose behaviour already exists entirely in `:core` (`Craft`, `Craftability`, `BuildLocation`), so fidelity needs no rule changes.
3. **One language at a time, no parenthetical English.** The mock's `Daga de Hueso (Bone Dagger)` is mock styling (content-identity Decision 1). The UI shows `names.es` today; `LocalizedText`'s `en → es` fallback makes English appear by itself once S3/S4 land. Nothing renders two languages.
4. **Show only what exists.** The mock's location prose is not modelled; the section renders endeavors, `Requiere:` and costs. Gear icons from the mock are not in the 66-glyph font subset; rows keep the generic craft icon. Cost chips read `N <Spanish name>` in full (the mock's `1H`/`1Ó` abbreviations are not adopted).
5. **Content over mock copy:** the founding location's action renders as `Innovar` (the card's name from the emitter), not the mock's `Augurio`.
6. **No duplicate advance CTA:** the slice-6 `StepFrame`'s Next already advances; the mock's `AVANZAR AL PASO 8` button is not added.

## Design

### Model

A pure `FabricarModel` in `app-android/.../ui/settlement/`, computed from `UiState` (the slice-6 model pattern; unit-testable on the JVM):

```kotlin
data class FabricarModel(
    val builtCount: Int,            // header: "N CONSTRUIDAS · M PENDIENTES"
    val pendingCount: Int,
    val built: List<BuiltLocationCard>,
    val buildable: List<BuildableLocationCard>,
)

data class BuiltLocationCard(
    val name: String,               // names.es
    val actionLabel: String?,       // the location's action, e.g. "Innovar" — null ⇒ no button
    val recipes: List<RecipeRow>,
    val upgrades: List<UpgradeRow>,
    val catalogSize: Int,           // "CATÁLOGO DE FABRICACIÓN (N CARTAS)"
)

data class RecipeRow(
    val gearName: String,
    val costChips: List<String>,    // ResourceId → "N <es name>", resolved via the catalog
    val status: RecipeStatus,       // CRAFTABLE | OUT_OF_STOCK | MISSING_INNOVATION
    val missingInnovation: String?, // ES name for the "REQUIERE AMONÍACO" badge
    val requiresText: String?,      // the card's non-innovation prerequisite label ("Calor")
)

data class UpgradeRow(
    val locationName: String,       // the improvement target, e.g. "Armería"
    val costChips: List<String>,
    val endeavors: Int,
)

data class BuildableLocationCard(   // the "POR ERIGIR" cards
    val name: String,
    val requires: List<String>,     // ids resolved to ES names
    val endeavors: Int,
    val costChips: List<String>,
)
```

Status derivation is pure and lives in the model (never in a composable):

| Status | Rule |
|---|---|
| `MISSING_INNOVATION` | some `recipe.requires` id ∉ unlocked innovation ids → disabled + red `REQUIERE <es name>` badge |
| `OUT_OF_STOCK` | innovation prerequisites met but the ledger cannot cover `resourceCost` → disabled, no badge |
| `CRAFTABLE` | otherwise (same acceptance as `Craftability.check`) |

### Upgrade placement (each buildable location appears exactly once)

`upgrades` on a built location `L` = buildable candidates whose `requires` contains `L` **and** whose full `requires` set is already built (ready to erect now), listed only in the **first** such `L` (sorted by id) so no candidate is duplicated. A buildable location with no built requiring-location — or with unmet prerequisites — appears as a `POR ERIGIR` card. This keeps the S2 "no duplicated entity on screen" property inside the section too.

### Components

Four small composables replacing `LocationsPanel`'s current rows and all of `FabricarTab`:

1. `LocationSectionHeader` — title + `N CONSTRUIDAS · M PENDIENTES` (replaces `N Opciones`).
2. `BuiltLocationCard` — name, `CONSTRUIDO` badge, action button (`onAction` → opens the Innovar tab, as wired in `cdbc699`), expandable catalog (open by default, `remember` local state), recipe rows, inline upgrade rows.
3. `UpgradeRow` / `RecipeRow` — chips + status-driven button.
4. `BuildableLocationCard` — `POR ERIGIR` / `NO CONSTRUIDO`, `Requiere:`, endeavors, `CONSTRUIR`.

Style: existing Atramentum tokens (`KdmType`, `KdmSymbols`, theme colours). No new dependencies.

### Data flow and behaviour (no rule changes)

| UI action | Callback (existing) | Path |
|---|---|---|
| `FABRICAR` | `onCraft(locationName, gearName)` | controller resolves name → ids → `Craft` |
| `CONSTRUIR` / `ERIGIR` | `onBuild(locationName)` | `BuildLocation` (same transaction, same blockers) |
| location's action (`Innovar`) | `onAction` → `developTab = INNOVAR` | unchanged from `cdbc699` |

### Retired in this phase

`FabricarTab` (whole file), the duplicate build list it carried, and the `Fabricar` entry of `DevelopStepScreen`'s tab row (tabs remaining: Recursos, Innovar — until Phases 3 and 2). The tab's `Gastar endeavor (−1)` control is **not** dropped with it: the design is silent on free endeavor spending, so the button moves to the section's footer (called out at the visual gate; remove it only on an explicit design decision).

## Out of scope

- Phase 2 (Innovar: the draw-2-keep-1 flow — touches `:core`), Phase 3 (Añadir recursos: batch + loot-origin selector).
- Location prose, per-gear icons, English names/parentheticals, abbreviated cost chips.
- Any `:core` or controller change (S2 already provides name→id resolution at the controller boundary).

## Testing

- **`FabricarModelTest` (new, JVM):** cost chips resolve to ES names; craftable → `CRAFTABLE`; insufficient ledger → `OUT_OF_STOCK` without badge; missing innovation → `MISSING_INNOVATION` with `REQUIERE <es name>`; `upgrades` placement rule (ready upgrade inside its first requiring built location, never duplicated, never in `Por erigir`); partially-ready candidates stay in `Por erigir`; header counts partition the catalog; `actionLabel` null ⇒ no action button.
- Port every existing test that references the retired tab/panel; controller-level tests (`DevelopTabsTest`, `DevelopCraftSectionTest`, `CraftControllerTest`) must stay green untouched.
- Gate: `./gradlew spotlessApply` then `:core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck`, plus `python3 -m unittest discover -s tools/content-ingest/tests`.

## Visual gate (hard rule)

Install on the emulator (AVD `kdm`, 1080×2400), sign in, open step 7, measure against the design: single build list (no duplication), `N CONSTRUIDAS · M PENDIENTES` counts, `FABRICAR` enabled/disabled states, `REQUIERE <innovation>` badge, inline `ERIGIR` with cost chips, `POR ERIGIR` cards with resolved `Requiere:` names. Capture, present, **stop** — no commit, push or PR until the maintainer approves the measured render.

## Risks

- **Tab retirement changes the step-7 surface** users know; mitigated by the section being a strict superset of what the tab showed (recipes, build list) plus the design's states.
- **Upgrade placement rule** is this spec's invention (the mock implies it); covered by model tests and re-pinned at the visual gate. Cost if wrong: a location shows twice or an `ERIGIR` sits in the wrong card — a model-rule fix, not a data fix.
- **PR #33 not merged** blocks implementation start; spec/plan work proceeds now, code waits for the merge.
