# Develop Craft (Gear) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the Settlement Phase Develop step craft gear: model `Gear` (+ structured `Modifier`s) and recipes in `:core`, extract the base 1.5 gear content, and add a Craft affordance to the Develop screen.

**Architecture:** `:core` gains `Gear`/`GearRecipe`/`Modifier` and a pure `Craft` use-case; a Python extractor turns the untracked location + gear card PDFs into committed Kotlin; the app exposes gear views on `UiState` and a `craft` action. Rules live in `:core`; the UI decides nothing; effects stay display-text.

**Tech Stack:** Kotlin 2.3.21 (KMP), kotlinx.serialization, Compose Multiplatform, Python 3 + poppler (`pdftotext`), `kotlin.test`.

**Spec:** `docs/superpowers/specs/2026-10-07-develop-craft-design.md`

## Global Constraints

- Kotlin `2.3.21`; `:core` stays pure (no Spring/Android/SQLDelight). No new dependencies.
- Rules are **total**: invalid crafts return `StepResult.Blocked(reason)`, never throw.
- Strict TDD: failing test first, observe RED, implement, observe GREEN.
- Costs/limits come from the sources verbatim; an unverifiable (icon-only) value is a `[gap]`, not a guess.
- Nothing new is ever assigned to a survivor; modifiers are data (applied when the `Survivor` model lands).
- Conventional Commits; **no** `Co-Authored-By`/AI attribution. Technical artifacts in English.
- Full suite green: `./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck` + `python3 -m unittest discover -s tools/content-ingest/tests -v`.
- Do not commit `external-assets/`, `local.properties`, `build/`, `.gradle/`.

## Review Focus

1. **Craft requires the location built** — crafting at a location not in `state.locations.built` must `Blocked`. ← Task 3.
2. **Insufficient storage on craft** — must `Blocked`, spending nothing. ← Task 3.
3. **Missing recipe prerequisite** — a recipe whose `requires` innovation is locked must `Blocked`. ← Task 3.
4. **Back-compat** — old `SettlementState`/`SettlementLocation` JSON and existing packs (no `gear`/`recipes`) must decode and compose unchanged. ← Task 1/2.
5. **Modifier scope** — a settlement-scope `SURVIVAL_LIMIT` modifier counts toward the settlement total; a survivor-scope one does not. ← Task 2.

---

## File Structure

```text
core/src/commonMain/kotlin/io/kdmtracker/core/
├── domain/Modifier.kt            # NEW: Modifier + ModifierTarget + ModifierScope
├── domain/Gear.kt                # NEW: Gear
├── domain/GearRecipe.kt          # NEW: GearRecipe
├── domain/SettlementLocation.kt  # + recipes
├── domain/SettlementState.kt     # + gear
├── content/ContentCatalog.kt     # + gearDefinitions + gearModifiersFor
├── rules/DerivedValues.kt        # + modifierTotal
├── usecase/Craft.kt              # NEW
├── content/generated/Core15Gear.kt   # GENERATED (Task 4)
└── content/Legacy15Content.kt        # wire gear
tools/content-ingest/{ingest_gear.py,tests/test_gear_parser.py}
app-android/src/main/kotlin/io/kdmtracker/app/{UiState,LanternYearController,LanternYearViewModel,KdmTrackerApp,ui/DevelopStepScreen}.kt
README.md
```

---

### Task 1: `Modifier`, `Gear`, `GearRecipe`

**Files:**
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/domain/Modifier.kt`
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/domain/Gear.kt`
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/domain/GearRecipe.kt`
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/domain/GearModelTest.kt`

**Interfaces:**
- Produces:
  - `enum class ModifierScope { SURVIVOR, SETTLEMENT }`
  - `enum class ModifierTarget { STRENGTH, SPEED, ACCURACY, EVASION, LUCK, MOVEMENT, SURVIVAL, ARMOR, INSANITY, SURVIVAL_LIMIT, STORAGE, ENDEAVORS }`
  - `@Serializable data class Modifier(val target: ModifierTarget, val value: Int, val scope: ModifierScope = ModifierScope.SURVIVOR, val condition: String? = null)`
  - `@Serializable data class Gear(val name: String, val keywords: List<String> = emptyList(), val rules: String = "", val modifiers: List<Modifier> = emptyList())`
  - `@Serializable data class GearRecipe(val gearName: String, val resourceCost: Map<String, Int> = emptyMap(), val requires: List<String> = emptyList())`

- [ ] **Step 1: Write the failing test** — `Gear("Daga", keywords = listOf("arma"), rules = "…", modifiers = listOf(Modifier(ModifierTarget.STRENGTH, 1, ModifierScope.SURVIVOR)))` round-trips through `Json`; defaults (`Gear("X")` → empty keywords/modifiers, `Modifier(TARGET, 1)` → `scope = SURVIVOR`, `condition = null`); a legacy `Gear` JSON without `modifiers` decodes empty.
- [ ] **Step 2: Run** `./gradlew :core:jvmTest` — expected FAIL (types missing).
- [ ] **Step 3: Implement** the three files with `@Serializable` + KDoc.
- [ ] **Step 4: Run** `./gradlew :core:jvmTest spotlessCheck` — expected GREEN.
- [ ] **Step 5: Commit** `feat(core): model gear and its modifiers`.

---

### Task 2: Extend the settlement content model + modifier totals

**Files:**
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/domain/SettlementLocation.kt`
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/domain/SettlementState.kt`
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/content/ContentCatalog.kt`
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/rules/DerivedValues.kt`
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/content/GearCatalogTest.kt`

**Interfaces:**
- Consumes: `Gear`/`GearRecipe`/`Modifier` (Task 1).
- Produces:
  - `SettlementLocation.recipes: List<GearRecipe> = emptyList()` (additive, defaulted).
  - `SettlementState.gear: List<String> = emptyList()` (additive, defaulted) + `fun withGear(name: String): SettlementState` (deduped).
  - `ContentCatalog.gearDefinitions: List<Gear>` (merged/deduped by name) + `ContentCatalog.gearModifiersFor(gearNames: Collection<String>): List<Modifier>` (resolves names → modifiers; unknown names ignored).
  - `DerivedValues.modifierTotal(modifiers: List<Modifier>, target: ModifierTarget, scope: ModifierScope): Int`.

- [ ] **Step 1: Write the failing test** — a `SettlementState`/`SettlementLocation` legacy JSON (no `gear`/`recipes`) decodes with empty defaults; a catalog over two packs exposing the same `Gear` name dedups (first wins); `gearModifiersFor(listOf("A"))` returns A's modifiers; `modifierTotal` sums only `target == SURVIVAL_LIMIT && scope == SETTLEMENT` entries and ignores a `SURVIVOR` one.
- [ ] **Step 2: Run** `./gradlew :core:jvmTest` — expected FAIL.
- [ ] **Step 3: Implement** the defaulted fields + `withGear` + the catalog gear views (reuse the location-merge helper where applicable) + `modifierTotal`.
- [ ] **Step 4: Run** `./gradlew :core:jvmTest spotlessCheck` — expected GREEN.
- [ ] **Step 5: Commit** `feat(core): carry gear and recipes in the content model`.

---

### Task 3: `Craft` use-case

**Files:**
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/usecase/Craft.kt`
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/usecase/CraftUseCaseTest.kt`

**Interfaces:**
- Consumes: `SettlementRecord`, `ContentCatalog.locationDefinitions`/`gearDefinitions`, `SettlementLocation.recipes`, `LocationSet.built`, `InnovationCollection`, `AddResourceEntry`, `StepResult`.
- Produces: `object Craft { fun run(record: SettlementRecord, locationName: String, gearName: String, catalog: ContentCatalog): StepResult<SettlementRecord> }`.

- [ ] **Step 1: Write the failing tests** — a valid craft spends the recipe's `resourceCost`, adds `gearName` to `state.gear`, and leaves the rest unchanged; each of → `Blocked`: unknown location/no recipe, location not built, unmet `requires` (innovation locked), insufficient storage (record unchanged). Re-crafting the same gear does not duplicate it.
- [ ] **Step 2: Run** `./gradlew :core:jvmTest` — expected FAIL.
- [ ] **Step 3: Implement** validate-all-then-build-one-copy; spends via the ledger guard (atomic, no partial state); dedup on the gear list.
- [ ] **Step 4: Run** `./gradlew :core:jvmTest spotlessCheck` — expected GREEN.
- [ ] **Step 5: Commit** `feat(core): craft gear at built locations`.

---

### Task 4: Extract the 1.5 gear (recipes + definitions)

**Files:**
- Create: `tools/content-ingest/ingest_gear.py`
- Create: `tools/content-ingest/tests/test_gear_parser.py`
- Create (generated): `core/src/commonMain/kotlin/io/kdmtracker/core/content/generated/Core15Gear.kt`
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/content/Legacy15Content.kt`
- Modify: `tools/content-ingest/README.md`

**Interfaces:**
- Consumes: the Task 1/2 shapes via the emitted Kotlin.
- Produces: `object Core15Gear { val bundle: ContentBundle }` with `gear: List<Gear>` and location `recipes`; `Legacy15Content.pack` includes them.

Source PDFs (untracked; pass `--pdf` explicitly):
- Location card / recipes: `/Volumes/Mac_Nvme/Dev/10_26_kdm-tracker/external-assets/Juego Completo/Formato XXL 102x204 mm/AF Asentamientos Loc.pdf`
- Gear definitions: `/Volumes/Mac_Nvme/Dev/10_26_kdm-tracker/external-assets/Juego Completo/Formato Cuadrado 51x51 mm/AF Equipo/*.pdf`

Parsing model (verified): a gear card page = `name`, then a `keywords` line (comma-separated lowercase words), then rules text; weapon cards print **text** stat numbers (e.g. speed/accuracy/strength). Icon-only values (armor points, several attribute bonuses) are **not** text — emit them as `[gap]` (do not guess). Location cards list craftable gear + `N x <resource>` cost blocks → `SettlementLocation.recipes`. Emit sorted; no timestamps/paths.

- [ ] **Step 1: Write the failing fixture tests** — pure functions `parse_gear_card(text) -> Gear` and `parse_recipe_block(text) -> GearRecipe`; fixtures pin one gear definition (name/keywords/rules), one weapon's text stats as `Modifier`s, one recipe cost, and a `[gap]` case for an icon-only stat; a determinism test re-emits byte-identically.
- [ ] **Step 2: Run** `python3 -m unittest discover -s tools/content-ingest/tests -v` — expected FAIL.
- [ ] **Step 3: Implement** the parser + emitter (reuse `pdf_text`/`kotlin_emit`), run it against the PDFs, wire `Legacy15Content`; document `[gap]`s in the README.
- [ ] **Step 4: Run** the Python suite + `./gradlew :core:jvmTest spotlessCheck` — expected GREEN; generator re-run → `git diff --exit-code` clean.
- [ ] **Step 5: Commit** `feat(content): ingest the 1.5 gear and recipes`.

---

### Task 5: Expose gear to the app + `craft` action

**Files:**
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/UiState.kt`
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/LanternYearController.kt`
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/LanternYearViewModel.kt`
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/KdmTrackerApp.kt`
- Test: `app-android/src/test/kotlin/io/kdmtracker/app/CraftControllerTest.kt`

**Interfaces:**
- Consumes: `Craft` (Task 3), `ContentCatalog.gearDefinitions`/`gearModifiersFor` (Task 2).
- Produces: `UiState` gains (additive, defaulted) `gear: List<String>`, `craftableRecipes: List<GearRecipe>` (for the built locations) or an equivalent view, and `settlementModifierTotal`; `LanternYearController.craft(locationName: String, gearName: String)` + ViewModel `craft`; `KdmTrackerApp` wires `viewModel::craft` into `SettlementScreen`/`DevelopStepScreen`.

- [ ] **Step 1: Write the failing test** — over the real store: a seeded campaign with a built location + stocked storage crafts a recipe, the gear appears and persists (a fresh repository reads it); an invalid craft (unbuilt/insufficient) surfaces `Blocked` and changes nothing.
- [ ] **Step 2: Run** `./gradlew :app-android:testDebugUnitTest` — expected FAIL.
- [ ] **Step 3: Implement** the `UiState` views, the `craft` action, and the wiring.
- [ ] **Step 4: Run** `./gradlew :app-android:testDebugUnitTest :app-android:assembleDebug spotlessCheck` — expected GREEN.
- [ ] **Step 5: Commit** `feat(app): expose gear and the craft action`.

---

### Task 6: Craft section in the Develop screen

**Files:**
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/ui/DevelopStepScreen.kt`
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/ui/SettlementScreen.kt`
- Test: `app-android/src/test/kotlin/io/kdmtracker/app/DevelopCraftSectionTest.kt`

**Interfaces:**
- Consumes: `UiState.craftableRecipes`/`gear`, `onCraft`.
- Produces: a **Craft** section listing, per built location, its recipes (gear + resource cost + prerequisites + the gear's displayed bonuses); crafting calls `onCraft(locationName, gearName)`; a `Blocked` reason is shown inline. Existing sections unchanged.

- [ ] **Step 1: Write the failing test** — the Develop view exposes the built locations' recipes with their costs (controller-level), and a craft through it persists; the screen stays navigable (rail/Next unchanged).
- [ ] **Step 2: Run** `./gradlew :app-android:testDebugUnitTest` — expected FAIL.
- [ ] **Step 3: Implement** the Craft section + thread `onCraft` through `SettlementScreen` (defaulted no-op).
- [ ] **Step 4: Run** `./gradlew :app-android:testDebugUnitTest :app-android:assembleDebug spotlessCheck` — expected GREEN.
- [ ] **Step 5: Commit** `feat(app): craft gear from the develop step`.

---

### Task 7: Verification sweep + README

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Run** `./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck --rerun-tasks` and `python3 -m unittest discover -s tools/content-ingest/tests -v`; record per-module counts.
- [ ] **Step 2:** Document the Develop Craft step in the README (recipes + build/innovation prerequisites, storage validation; the gear modifier model + the icon-only `[gap]`; the deferred survivor application).
- [ ] **Step 3: Commit** `docs: document the develop craft step`.

---

## Self-Review

**1. Spec coverage:** model + modifiers (T1/T2), settlement-scope compute (T2), `Craft` (T3), extraction incl. `[gap]` (T4), UiState + action (T5), screen (T6), docs (T7). Deferred items (survivor application, icon stats, innovation migration, gear grids, effects) are not tasked — correct.

**2. Step scan:** each step is one action with a checkable result; tests carry the spec's exact values; no code transcripts.

**3. Type consistency:** `Modifier`/`Gear`/`GearRecipe` (T1) → catalog + `modifierTotal` (T2) → `Craft` (T3) → extraction (T4) → app (T5/T6).

**4. Review Focus:** each of the five lines maps to the named task.

**5. Proportion:** task-level; strict-TDD steps; no transcripts. Delivery likely under ~400 lines? If it exceeds, plan for **chained PRs** (`work-unit-commits` + `chained-pr`) at execution time.
