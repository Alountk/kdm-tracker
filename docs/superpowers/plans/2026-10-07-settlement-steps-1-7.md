# Settlement Steps 1 & 7 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the Compose app's Settlement Step 1 (Set Up) and Step 7 (Develop) real, backed by a faithful, content-driven rules layer in `:core`.

**Architecture:** Extend the settlement content model (`SettlementLocation` gains build cost + actions), add pure `:core` use-cases that enforce the real KDM costs/limits with a typed `StepResult`, extract the base location cards into generated content, then wire the catalog + record into two Compose screens. Rules live in `:core`; the UI decides nothing.

**Tech Stack:** Kotlin 2.3.21 (KMP), kotlinx.serialization, Compose Multiplatform, Python 3 + poppler (`pdftotext`) for the content pipeline, `kotlin.test`.

**Spec:** `docs/superpowers/specs/2026-10-07-settlement-steps-1-7-design.md`

## Global Constraints

- Kotlin `2.3.21`; `:core` stays pure (no Spring/Android/SQLDelight). No new dependencies.
- Rules are **total**: invalid actions return `StepResult.Blocked(reason)`, never throw.
- Strict TDD: failing test first, observe RED, implement, observe GREEN.
- Costs/limits come from the sources verbatim; never invent a number. An unverifiable value is a `[gap]`, not a guess.
- Conventional Commits; **no** `Co-Authored-By`/AI attribution. Technical artifacts in English.
- The full suite stays green: `./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck`.
- Do not commit `external-assets/`, `local.properties`, `build/`, `.gradle/`.

## Review Focus

The spec's rules imply these failure modes; each is pinned by the task named:

1. **Insufficient storage on a spend** — crafting/building with resources the settlement does not hold must `Blocked`, never go negative. ← Task 4/6.
2. **Innovate once per phase** — a second Innovate in the same Settlement Phase must `Blocked`; a new year must clear the limit. ← Task 5/10.
3. **Prerequisite gating** — building a location whose `requires` are unmet must `Blocked`. ← Task 6.
4. **Content overlap determinism** — when two packs define the same location, the definition with modeled cost/actions must win. ← Task 2.
5. **Backward compatibility** — old `SettlementState` JSON (no phase markers) and existing content packs (locations without cost/actions) must decode and compose unchanged. ← Task 1/3.

---

## File Structure

```text
core/src/commonMain/kotlin/io/kdmtracker/core/
├── domain/SettlementAction.kt                 # NEW: SettlementAction + ActionKind
├── domain/SettlementLocation.kt               # + buildEndeavors/buildCost/actions
├── domain/SettlementState.kt                  # + phaseActionsUsed
├── rules/StepResult.kt                        # NEW: Ok/Blocked
├── usecase/GainEndeavors.kt                   # NEW
├── usecase/AddResourceEntry.kt                # NEW
├── usecase/Innovate.kt                        # NEW
├── usecase/BuildLocation.kt                   # NEW
├── usecase/SpendEndeavor.kt                   # NEW
├── content/ContentCatalog.kt                  # location merge prefers modeled data
└── content/generated/Core15Locations.kt       # GENERATED (Task 8)
core/src/commonTest/kotlin/io/kdmtracker/core/{domain,usecase,content}/…   # tests
tools/content-ingest/{ingest_locations.py,tests/test_locations_parser.py}  # NEW
core/.../content/Legacy15Content.kt            # wire extracted locations
app-android/src/main/kotlin/io/kdmtracker/app/
├── SettlementRepository.kt                    # compose catalog; expose record+content
├── LanternYearController.kt                   # step actions
├── LanternYearViewModel.kt                    # action methods
├── UiState.kt                                 # carry record + content view
└── ui/{SettlementScreen,SetUpStepScreen,DevelopStepScreen}.kt
```

---

### Task 1: Add `SettlementAction` + extend `SettlementLocation`

**Files:**
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/domain/SettlementAction.kt`
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/domain/SettlementLocation.kt`
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/domain/SettlementActionTest.kt`

**Interfaces:**
- Produces:
  - `enum class ActionKind { ENDEAVOR, INNOVATE }`
  - `@Serializable data class SettlementAction(name: String, kind: ActionKind = ActionKind.ENDEAVOR, endeavorCost: Int = 1, resourceCost: Map<String, Int> = emptyMap(), oncePerPhase: Boolean = false, draw: Int = 0, keep: Int = 0, effect: String = "")`
  - `SettlementLocation(name: String, requires: List<String> = emptyList(), buildEndeavors: Int = 0, buildCost: Map<String, Int> = emptyMap(), actions: List<SettlementAction> = emptyList())` (all new params defaulted)

- [ ] **Step 1: Write the failing test** — a defaulted `SettlementLocation("X")` has empty `buildCost`/`actions` and `buildEndeavors == 0`; `SettlementAction("Innovate", kind = INNOVATE, resourceCost = mapOf("Bone" to 1), oncePerPhase = true, draw = 2, keep = 1)` round-trips through JSON (the types are `@Serializable`); a legacy JSON `{"name":"Lantern Hoard","requires":[]}` decodes with the new fields defaulted.
- [ ] **Step 2: Run** `./gradlew :core:jvmTest` — expected FAIL (no such types).
- [ ] **Step 3: Implement** the new type + the three defaulted fields; add `@Serializable` and KDoc.
- [ ] **Step 4: Run** `./gradlew :core:jvmTest spotlessCheck` — expected GREEN.
- [ ] **Step 5: Commit** `feat(core): model settlement actions and build costs`.

---

### Task 2: Location content overlap prefers modeled data

**Files:**
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/content/ContentCatalog.kt`
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/content/ContentCatalogMergeTest.kt`

**Interfaces:**
- Consumes: `SettlementLocation.buildCost`/`actions` (Task 1).
- Produces: `ContentCatalog.locationDefinitions` keeps, per name, the definition with modeled data (`buildCost` or `actions` non-empty) over a bare seed, deterministically.

- [ ] **Step 1: Write the failing test** — compose two packs that both define `SettlementLocation("Armería")`, one bare and one with `buildCost = mapOf("Bone" to 3)`: `catalog.locationDefinitions.single { it.name == "Armería" }.buildCost` is non-empty. Also assert an all-bare overlap still yields the first deterministically (stable by pack order).
- [ ] **Step 2: Run** `./gradlew :core:jvmTest` — expected FAIL.
- [ ] **Step 3: Implement** a location-specific merge: group by name, pick with `maxOf`/`reduce` preferring `hasModeledData` (non-empty `buildCost` or `actions`), tie-break by first occurrence; keep other types on the existing `distinctBy`.
- [ ] **Step 4: Run** `./gradlew :core:jvmTest spotlessCheck` — expected GREEN.
- [ ] **Step 5: Commit** `feat(core): prefer modeled location content on overlap`.

---

### Task 3: `StepResult` + per-phase markers in `SettlementState`

**Files:**
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/rules/StepResult.kt`
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/domain/SettlementState.kt`
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/domain/SettlementStatePhaseMarkersTest.kt`

**Interfaces:**
- Produces:
  - `sealed interface StepResult<out T> { data class Ok<T>(val value: T); data class Blocked(val reason: String) }`
  - `SettlementState.phaseActionsUsed: Set<String> = emptySet()` — keys are action identifiers (Task 5 uses `location.name` for Innovate).
  - `SettlementState.usedInPhase(key: String): Boolean` and `withPhaseAction(key: String): SettlementState` helpers.

- [ ] **Step 1: Write the failing test** — a state with a marker reports `usedInPhase(key)` true; `withPhaseAction` is idempotent; a legacy `SettlementState` JSON without `phaseActionsUsed` decodes with an empty set (back-compat).
- [ ] **Step 2: Run** `./gradlew :core:jvmTest` — expected FAIL.
- [ ] **Step 3: Implement** `StepResult` and the field + helpers.
- [ ] **Step 4: Run** `./gradlew :core:jvmTest spotlessCheck` — expected GREEN.
- [ ] **Step 5: Commit** `feat(core): add StepResult and per-phase markers`.

---

### Task 4: `GainEndeavors` + `AddResourceEntry`

**Files:**
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/usecase/GainEndeavors.kt`
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/usecase/AddResourceEntry.kt`
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/usecase/EndeavorAndLedgerUseCaseTest.kt`

**Interfaces:**
- Consumes: `SettlementRecord`, `DerivedValues.endeavorsFor`, `Storage`, `ResourceEntry`, `StepResult`.
- Produces:
  - `object GainEndeavors { fun run(record: SettlementRecord, returnedSurvivors: Int, ruleset: Ruleset): SettlementRecord }` — adds the Step 3 breakdown total to the pool (`endeavors + total`).
  - `object AddResourceEntry { fun run(record: SettlementRecord, entry: ResourceEntry): StepResult<SettlementRecord> }` — a gain (`delta >= 0`) always appends; a spend appends only when the resource's projected total would stay `>= 0`, else `Blocked("insufficient <name>")`.

- [ ] **Step 1: Write the failing tests** — `GainEndeavors.run(record, 4, Core16Ruleset).settlement.endeavors == 4` (base 1/returnee) and `0` returnees adds `0`; `AddResourceEntry` with `delta = -2` on an empty ledger is `Blocked`, with `+3` then `-2` is `Ok` and projects `1`.
- [ ] **Step 2: Run** `./gradlew :core:jvmTest` — expected FAIL.
- [ ] **Step 3: Implement** both. `AddResourceEntry` validates against `Storage.project(entries)` net for `entry.name`.
- [ ] **Step 4: Run** `./gradlew :core:jvmTest spotlessCheck` — expected GREEN.
- [ ] **Step 5: Commit** `feat(core): gain endeavors and guard the resource ledger`.

---

### Task 5: `Innovate` use-case

**Files:**
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/usecase/Innovate.kt`
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/usecase/InnovateUseCaseTest.kt`

**Interfaces:**
- Consumes: `SettlementRecord`, `ContentCatalog`, `SettlementAction` (`kind = INNOVATE`), `SettlementState.usedInPhase`/`withPhaseAction`, `AddResourceEntry`, `StepResult`.
- Produces: `object Innovate { fun run(record: SettlementRecord, locationName: String, innovationName: String, catalog: ContentCatalog): StepResult<SettlementRecord> }` — finds the location's `INNOVATE` action; requires not `usedInPhase(locationName)`; requires the innovation exists in `catalog.innovationDefinitions`; spends 1 endeavor + the action's `resourceCost` (via the ledger guard); adds the innovation to `state.innovations`; marks the phase action.

- [ ] **Step 1: Write the failing tests** — a valid Innovate spends exactly `1 Bone, 1 Organ, 1 Hide` and 1 endeavor, adds the innovation, and marks the phase; a second Innovate (`usedInPhase`) is `Blocked`; insufficient storage is `Blocked`; a location with no `INNOVATE` action is `Blocked`.
- [ ] **Step 2: Run** `./gradlew :core:jvmTest` — expected FAIL.
- [ ] **Step 3: Implement** — resolve the resource categories from `catalog.resourceDefinitions` when writing ledger entries; return `StepResult.Blocked` on every failure.
- [ ] **Step 4: Run** `./gradlew :core:jvmTest spotlessCheck` — expected GREEN.
- [ ] **Step 5: Commit** `feat(core): innovate at the Lantern Hoard once per phase`.

---

### Task 6: `BuildLocation` use-case

**Files:**
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/usecase/BuildLocation.kt`
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/usecase/BuildLocationUseCaseTest.kt`

**Interfaces:**
- Consumes: `SettlementRecord`, `ContentCatalog.locationDefinitions`, `LocationSet.canBuild/with`, `AddResourceEntry`, `StepResult`.
- Produces: `object BuildLocation { fun run(record: SettlementRecord, locationName: String, catalog: ContentCatalog): StepResult<SettlementRecord> }` — requires the location to exist, not already built (`canBuild` / already-present), `requires` satisfied, storage holds `buildCost`, and `endeavors >= buildEndeavors`; spends the cost + endeavors, adds to `state.locations`.

- [ ] **Step 1: Write the failing tests** — a satisfied build spends the cost + endeavors and adds the location; unmet `requires` is `Blocked`; insufficient storage is `Blocked`; not enough endeavors is `Blocked`; already-built is `Blocked`.
- [ ] **Step 2: Run** `./gradlew :core:jvmTest` — expected FAIL.
- [ ] **Step 3: Implement** using the Task 4 ledger guard; decrement `endeavors` by `buildEndeavors`.
- [ ] **Step 4: Run** `./gradlew :core:jvmTest spotlessCheck` — expected GREEN.
- [ ] **Step 5: Commit** `feat(core): build settlement locations with their costs`.

---

### Task 7: `SpendEndeavor` use-case

**Files:**
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/usecase/SpendEndeavor.kt`
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/usecase/SpendEndeavorUseCaseTest.kt`

**Interfaces:**
- Produces: `object SpendEndeavor { fun run(record: SettlementRecord, count: Int = 1): StepResult<SettlementRecord> }` — `Blocked("no endeavors")` when `endeavors < count`, else decrements.

- [ ] **Step 1: Write the failing test** — spending 1 with 0 endeavors is `Blocked`; with 2 it is `Ok` and leaves 1.
- [ ] **Step 2: Run** `./gradlew :core:jvmTest` — expected FAIL.
- [ ] **Step 3: Implement** the guard + decrement.
- [ ] **Step 4: Run** `./gradlew :core:jvmTest spotlessCheck` — expected GREEN.
- [ ] **Step 5: Commit** `feat(core): spend a settlement endeavor`.

---

### Task 8: Extract the base settlement locations

**Files:**
- Create: `tools/content-ingest/ingest_locations.py`
- Create: `tools/content-ingest/tests/test_locations_parser.py`
- Create (generated): `core/src/commonMain/kotlin/io/kdmtracker/core/content/generated/Core15Locations.kt`
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/content/Legacy15Content.kt`
- Modify: `tools/content-ingest/README.md`

**Interfaces:**
- Consumes: the parsed card text (Task 1 types via the emitted Kotlin).
- Produces: `object Core15Locations { val bundle: ContentBundle }` with `locations: List<SettlementLocation>` carrying `buildEndeavors`/`buildCost`/`actions`; `Legacy15Content.pack` includes them.

**Parsing model (verified against `AF Asentamientos Loc.pdf`, 14 pages):** one page per location; the first non-blank line is the name. A build block is a `Construir <name>` line followed by `N x <resource>` tokens (possibly wrapped across lines; the tier-1 trio appears as three `Construir` lines on the Lantern Hoard card). An action block is `Esfuerzo <cost tokens> <effect prose>`; `Esfuerzo Hueso Órgano Piel … robar 2 cartas de innovación … Quedaos con 1` is Lantern Hoard's `INNOVATE` action (`endeavorCost = 1`, `resourceCost = {Bone:1, Organ:1, Hide:1}`, `oncePerPhase = true`, `draw = 2`, `keep = 1`). Emit sorted by name; no timestamps/paths.

- [ ] **Step 1: Write the failing fixture tests** — pure functions `parse_card(text) -> SettlementLocation` and `parse_cost("3 x hueso, 1 x piel") -> Map<String,Int>`; fixtures assert Armería's build cost, Círculo de Piedra's cost, and Lantern Hoard's Innovate action; a determinism test re-emits byte-identically.
- [ ] **Step 2: Run** `python3 -m unittest discover -s tools/content-ingest/tests -v` — expected FAIL.
- [ ] **Step 3: Implement** the parser + emitter (stdlib only, `kotlin_emit`/`pdf_text` helpers), run it against the PDF with `--pdf`, and wire `Legacy15Content`. Where the text is ambiguous, emit only what is verifiable and note the rest in the README as `[gap]`.
- [ ] **Step 4: Run** the Python suite + `./gradlew :core:jvmTest spotlessCheck` — expected GREEN; re-run the generator → `git diff --exit-code` on the generated file.
- [ ] **Step 5: Commit** `feat(content): ingest the 1.5 settlement locations`.

---

### Task 9: Compose the catalog and expose it to the app

**Files:**
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/SettlementRepository.kt`
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/UiState.kt`
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/LanternYearController.kt`
- Test: `app-android/src/test/kotlin/io/kdmtracker/app/SettlementCatalogTest.kt`

**Interfaces:**
- Consumes: `ContentCatalogComposer`, `ContentPack`, `Core16Content`/`Legacy15Content`, `SettlementRecord`.
- Produces: the repository composes `ContentCatalog` from `Campaign.enabledPacks` (unknown pack id → `Blocked`/empty catalog surfaced as a load error); `UiState` gains `record: SettlementRecord` and the catalog view (`buildableLocations`, `innovationDefinitions`, `storageLines`, `endeavorBreakdown`).

- [ ] **Step 1: Write the failing test** — a campaign with `enabledPacks = listOf("core-1.6")` yields a non-empty catalog with the base locations; a repository over an empty campaign exposes an empty catalog without crashing.
- [ ] **Step 2: Run** `./gradlew :app-android:testDebugUnitTest` — expected FAIL.
- [ ] **Step 3: Implement** the composition in the repository and the `UiState` extension; keep `listCards()`/`select()` behavior unchanged.
- [ ] **Step 4: Run** `./gradlew :app-android:testDebugUnitTest :app-android:assembleDebug spotlessCheck` — expected GREEN.
- [ ] **Step 5: Commit** `feat(app): compose the campaign catalog for the step screens`.

---

### Task 10: Step 1 (Set Up) screen + step dispatch

**Files:**
- Create: `app-android/src/main/kotlin/io/kdmtracker/app/ui/SetUpStepScreen.kt`
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/ui/SettlementScreen.kt`
- Test: `app-android/src/test/kotlin/io/kdmtracker/app/SetUpStepControllerTest.kt`

**Interfaces:**
- Consumes: `UiState.record`, `InnovationDeck.of`, `SettlementState.locations`, `survivalLimit`.
- Produces: `SettlementScreen` renders `SetUpStepScreen` when `state.currentStep == 1` (else the existing header + `PendientePlaceholder`); the derived deck/built locations/principles/year-event are exposed on `UiState`.

- [ ] **Step 1: Write the failing test** — the controller exposes the derived deck (consequences of unlocked innovations) and the built locations for Step 1; advancing still behaves as before (loop test unchanged); and a transition that lands on a **new year** (a `Commit` out of the Settlement Phase, or a return to Settlement step 1) **clears `phaseActionsUsed`** so Innovate is available again next phase.
- [ ] **Step 2: Run** `./gradlew :app-android:testDebugUnitTest` — expected FAIL.
- [ ] **Step 3: Implement** the screen + the `when (currentStep)` dispatch in `SettlementScreen`.
- [ ] **Step 4: Run** `./gradlew :app-android:testDebugUnitTest :app-android:assembleDebug spotlessCheck` — expected GREEN.
- [ ] **Step 5: Commit** `feat(app): render the settlement set-up step`.

---

### Task 11: Step 7 (Develop) screen + actions

**Files:**
- Create: `app-android/src/main/kotlin/io/kdmtracker/app/ui/DevelopStepScreen.kt`
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/ui/SettlementScreen.kt`
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/LanternYearController.kt`
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/LanternYearViewModel.kt`
- Test: `app-android/src/test/kotlin/io/kdmtracker/app/DevelopStepControllerTest.kt`

**Interfaces:**
- Consumes: `Innovate`, `BuildLocation`, `SpendEndeavor`, `AddResourceEntry` (Tasks 4–7).
- Produces: controller methods `innovate(name)`, `build(name)`, `spendEndeavor()`, `addResource(entry)` that call the use-cases, persist via `SettlementRepository.update(record)`, and surface `Blocked(reason)`; `SettlementScreen` renders `DevelopStepScreen` at `currentStep == 7`.

- [ ] **Step 1: Write the failing test** — through the controller: Innovate at Lantern Hoard spends the cost and marks the phase; a second Innovate is blocked with a reason; Build respects prerequisites; a spend with insufficient storage is blocked; each `Ok` persists (a fresh repository reads the change).
- [ ] **Step 2: Run** `./gradlew :app-android:testDebugUnitTest` — expected FAIL.
- [ ] **Step 3: Implement** the controller/ViewModel actions and `DevelopStepScreen` (Innovate / Build list / Endeavor + storage & endeavor display; effects shown as text).
- [ ] **Step 4: Run** `./gradlew :app-android:testDebugUnitTest :app-android:assembleDebug spotlessCheck` — expected GREEN.
- [ ] **Step 5: Commit** `feat(app): develop — innovate, build and spend endeavors`.

---

### Task 12: Verification sweep + README

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Run** `./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck` and `python3 -m unittest discover -s tools/content-ingest/tests -v`; record per-module counts.
- [ ] **Step 2:** Document the Step 1/Step 7 screens and the Develop rules model (costs/limits, storage validation, per-phase Innovate) in the README.
- [ ] **Step 3: Commit** `docs: document the settlement steps 1 & 7`.

---

## Self-Review

**1. Spec coverage:** content model (T1/T2), rules use-cases (T4–T7), extraction (T8), app wiring (T9), Step 1 (T10), Step 7 (T11), docs (T12). Deferred items (Craft/gear, executable effects, Survivor model, 1.6) are not tasked — correct.

**2. Step scan:** each step is one action with a checkable result; tests carry the spec's exact values; no code transcripts.

**3. Type consistency:** `SettlementAction`/`ActionKind` (T1) → Innovate (T5); `phaseActionsUsed`/`StepResult` (T3) → T5; `AddResourceEntry` (T4) → T5/T6; catalog (T2/T9) → T5/T6/T11.

**4. Review Focus:** each of the five lines maps to the named task.

**5. Proportion:** task-level; strict-TDD steps; no transcripts. Delivery likely exceeds ~400 authored lines → plan for **chained PRs** (`work-unit-commits` + `chained-pr` skills) at execution time.
