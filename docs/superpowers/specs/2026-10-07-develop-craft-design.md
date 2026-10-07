# Develop Craft (Gear) — Design

**Status:** Draft (awaiting review)
**Slice:** Crafting in the Settlement Phase **Develop** step — model gear (+ its modifiers) and recipes in `:core`, extract the base 1.5 gear content, and let the Develop screen craft at built locations.
**Depends on:** PR #7 (settlement domain + content packs), PR #8 (1.5 content ingestion), PR #9 (settlement record persistence), PR #10 (Steps 1 & 7 UI + rules layer).

## Goal

Complete the Develop (Step 7) step with its headline missing activity: **Craft**. A player picks a **built** location, picks a gear the location can craft, pays the **recipe's resources**, and gains the **gear** at the settlement — with the gear's bonuses modeled as **structured data**.

Success is:
1. `:core` models `Gear` (with a structured `modifiers` list) and its recipes, and a `Craft` use-case enforces built-location + storage + prerequisite rules with a typed `StepResult`.
2. Recipe + gear content is **extracted from the source cards** and pinned by fixture tests — never invented; icon-only values are recorded as `[gap]`, not guessed.
3. The Develop screen offers a Craft affordance (built location → recipes → craft) that **persists** the settlement.
4. Rules live in `:core`; the UI decides nothing.

## Scope

### In
- `:core`: `Gear`, `GearRecipe`, `Modifier` (+ enums), `SettlementLocation.recipes`, `SettlementState.gear`, catalog gear views, and the `Craft` use-case. Settlement-scope modifiers are folded into the derived values (survival limit); survivor-scope modifiers are stored as queryable data.
- Content extraction: recipes from the **location cards** and gear definitions (`name`, `keywords`, `rules`, text-visible modifiers) from the **gear cards** (`AF Equipo/*.pdf`) → a generated pack, fixture-pinned.
- The app: a **Craft** section in `DevelopStepScreen` + `LanternYearController.craft(...)`; gear bonuses displayed.

### Out (deferred, explicitly)
- **Applying survivor-scope modifiers** — needs the deferred `Survivor` model; this slice stores them as data and displays them.
- **Icon-only stats** (armor points, several attribute bonuses) — recorded as `[gap]`; see “Extraction reality”.
- **Migrating other cards to `Modifier`** — `Innovation.survivalLimitBonus` stays as-is this slice; the dedicated “card bonuses/modifiers” slice will fold innovations (and properties/events) onto the shared `Modifier` model.
- **Survivor assignment / gear grids**; **effect execution** (rules text is displayed); **1.6 content**.

## Rules model (grounded)

| Rule | Value | Source |
| --- | --- | --- |
| Craft | Spend resources at a **built** location to create gear; the recipe's resource cost is printed per location | 1.5 rulebook §7 (Develop); `AF Asentamientos Loc.pdf` |
| Storage | The resources must exist at craft time; spent resources are removed | 1.5 rulebook p.83 |
| Prerequisite | Some gear requires an innovation (e.g. `Requiere Calor`, `Requiere Amoníaco`) | Gear/location cards |
| Gear | A gear card carries its **name**, **keywords** (e.g. `arma`, `armadura`, `objeto`, `hueso`), **rules text**, and its **stats** | `AF Equipo/*.pdf` |
| Stats | Weapon stats (speed/accuracy/strength) appear as **text numbers**; armor points and several attribute bonuses are **icons** | `AF Equipo/*.pdf` (verified) |

**Fidelity boundary:** we enforce **built location + recipe + storage + innovation prerequisite**. Modifiers are structured data; settlement-scope ones feed derived values; survivor-scope ones are not applied until the `Survivor` model exists. Effects are displayed as text.

## Architecture

### 1. Content model (`:core`, pure, additive)

New types (all `@Serializable`, defaults so existing content compiles):
- `enum class ModifierTarget { STRENGTH, SPEED, ACCURACY, EVASION, LUCK, MOVEMENT, SURVIVAL, ARMOR, INSANITY, SURVIVAL_LIMIT, STORAGE, ENDEAVORS }`
- `enum class ModifierScope { SURVIVOR, SETTLEMENT }`
- `Modifier(target: ModifierTarget, value: Int, scope: ModifierScope = ModifierScope.SURVIVOR, condition: String? = null)` — data-only; `condition` is display text.
- `Gear(name: String, keywords: List<String> = emptyList(), rules: String = "", modifiers: List<Modifier> = emptyList())`
- `GearRecipe(gearName: String, resourceCost: Map<String, Int> = emptyMap(), requires: List<String> = emptyList())`

Extended (defaulted):
- `SettlementLocation.recipes: List<GearRecipe> = emptyList()` — recipes travel on the location (same pattern as `actions`), because the location cards print them there.
- `SettlementState.gear: List<String> = emptyList()` — the settlement's crafted gear, by name.

Derived: settlement-scope modifiers with `target = SURVIVAL_LIMIT` contribute to `SettlementRecord.survivalLimit()` (alongside the ruleset base + innovation bonus). Other settlement targets are surfaced for display.

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
- **Recipes** from the location card PDF (`AF Asentamientos Loc.pdf`): each craftable block under a location is `<gear name>` + `<N x resource>` cost tokens → `SettlementLocation.recipes`.
- **Gear definitions** from the gear card PDFs (`AF Equipo/*.pdf`): each page = a card → `Gear(name, keywords, rules, modifiers)`. **Keywords and rules are text-reliable**; **weapon stat numbers are text** (positional); **icon-only values (armor points, attribute-icon bonuses) are emitted as `[gap]`** and documented — never guessed.
- Emit a generated Kotlin pack (`Core15Gear.kt`) wired into `Legacy15Content`; fixture tests pin representative recipes, one gear definition, and one weapon's text stats; re-run is byte-identical.

### 4. App (`:app-android`)

- `DevelopStepScreen` gains a **Craft** section: choose a built location → list its recipes (gear + resource cost + prerequisites + the gear's displayed bonuses) → craft. Rules/effect text is shown.
- `LanternYearController.craft(locationName, gearName)` calls the `:core` use-case, persists `Ok` via `SettlementRepository.update(record)`, surfaces `Blocked(reason)`.
- `LanternYearViewModel` exposes `craft`; `KdmTrackerApp` wires it (as with the other Develop actions).

## Data flow

`Campaign.enabledPacks` → `ContentCatalog` (with `gearDefinitions` + location `recipes`).
`SettlementRecord` ↔ `Craft` use-case ↔ controller ↔ `UiState` → Develop screen.
Every craft persists through `SettlementRepository.update(record)` (local store + outbox), like the other Develop actions.

## Error handling

Rules are **total**: invalid crafts return `StepResult.Blocked(reason)` (location not built, no such recipe, prerequisite missing, insufficient storage) — never an exception. The UI shows the reason inline and leaves the record unchanged.

## Testing

- **`:core`** — strict TDD for `Craft`: not-built location, no recipe, missing prerequisite, insufficient storage → `Blocked`; a valid craft spends the cost and adds the gear; re-crafting dedups. A test pins that a settlement-scope `SURVIVAL_LIMIT` modifier changes `survivalLimit()`.
- **Extraction** — fixture tests pin representative recipes + one gear definition + one weapon's text stats; determinism (byte-identical re-emit); `[gap]` behavior for icon-only values.
- **`:app-android`** — a controller test over the real store: craft at a built location persists (fresh repository reads it); an invalid craft surfaces `Blocked`.
- Full suite green: `./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck` and the Python suite.

## Risks

- **Recipe extraction accuracy** — the location cards mix gear blocks and other bars; pin with fixtures and `[gap]` anything ambiguous.
- **Gear name matching** — recipes (location cards) and definitions (gear cards) join by name; a mismatch leaves a recipe with no definition (cost still craftable) — document the join rule.
- **Icon-only stats** — armor/attribute bonuses that are icons stay `[gap]`; the `Modifier` model is ready but not fully populated until an OCR/vision pass. This is a known, documented limitation, not a silent one.
- **Scope creep into the rules engine** — survivor-scope modifiers are data only; do not attempt to apply them here.

## References

- `external-assets/KDM 1.5 EdESP Reglamento - Digital.pdf` (§7 Develop, p.83)
- `external-assets/Juego Completo/Formato XXL 102x204 mm/AF Asentamientos Loc.pdf` (recipes)
- `external-assets/Juego Completo/Formato Cuadrado 51x51 mm/AF Equipo/*.pdf` (gear definitions + stats)
- `docs/superpowers/specs/2026-10-07-settlement-steps-1-7-design.md` (the Develop rules model this extends)
- `server/src/main/resources/static/prototypes/research/lantern-year-steps.md` §7
