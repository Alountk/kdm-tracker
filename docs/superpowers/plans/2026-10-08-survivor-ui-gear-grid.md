# Survivor UI Redesign + Gear Grid Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign the survivor UI into a reusable component set (expandable card, filter chips, status dot, full-screen editor, 3×3 gear grid) that speaks the cockpit's visual language.

**Architecture:** All rules live in `:core` (the gear cap); the `:app-android` screens are stateless composables driven by `UiState.survivors`, with pure `filterSurvivors`/`survivorTone` seams that are testable without Compose.

**Tech Stack:** Kotlin, Jetpack Compose (Material3), kotlin.test.

**Spec:** `docs/superpowers/specs/2026-10-08-survivor-ui-gear-grid-design.md`

## Global Constraints

- Rules in `:core`; screens render state and report intent only.
- No Compose UI tests (repo convention); behavior is pinned through `:core` and pure `:app` seams + the emulator.
- UI copy is **English**, consistent with the existing chrome.
- Conventional Commits in English; **no** `Co-Authored-By` / AI attribution.
- Pre-commit hook runs `spotlessKotlinCheck`; run `./gradlew spotlessApply` before committing or the commit is rejected.
- Full suite must stay green: `./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck`.

## Review Focus

Failure modes a person will hit that the tasks must pin:

1. **The 9-item cap must be enforced in `:core`**, not just hidden in the grid UI (Task 1).
2. **Filter predicates must match the status dots** — UNNAMED, DEAD and INJURED must classify exactly as the dot colours say (Task 2).
3. **The departing cap** (`min(4, living)`) still holds and an over-selection surfaces an error (Task 4).
4. **Long gear names must not distort the grid** — the 9 cells stay uniform (Task 4, emulator).
5. **Dead survivors must not be departable** and are shown greyed (Task 2 predicate + Task 4 UI).

---

## Task 1: Cap the gear grid at 9 items

**Files:**
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/usecase/AssignGear.kt`
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/usecase/AssignGearCapTest.kt`

**Interfaces:**
- Consumes: `Survivor`, `SettlementRecord`.
- Produces: `AssignGear.MAX_GEAR_SLOTS = 9`; `AssignGear.run` refuses a 10th distinct item.

- [ ] **Step 1: Write the failing test**

`AssignGearCapTest`:
- `assigningUpToNineItemsSucceeds` — a survivor with 9 distinct names assigned keeps `Ok`.
- `assigningATenthDistinctItemIsBlocked` — the 10th distinct name is `Blocked` with `"gear grid is full"` and the record is untouched (**Review Focus 1**).
- `reassigningAnAlreadyCarriedNameIsANoOp` — assigning a name already in the 9 is `Ok` (idempotent), not blocked.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :core:jvmTest --tests "io.kdmtracker.core.usecase.AssignGearCapTest"`
Expected: FAIL — the 10th item is accepted.

- [ ] **Step 3: Implement**

In `AssignGear` add `const val MAX_GEAR_SLOTS = 9`; before adding a **new** name, block when `survivor.gear.size >= MAX_GEAR_SLOTS` with `StepResult.Blocked("gear grid is full")`. Re-assigning an existing name stays a no-op success (the existing early return).

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :core:jvmTest --tests "io.kdmtracker.core.usecase.AssignGearCapTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add core/src/commonMain/kotlin/io/kdmtracker/core/usecase/AssignGear.kt core/src/commonTest/kotlin/io/kdmtracker/core/usecase/AssignGearCapTest.kt
git commit -m "feat(core): cap the survivor gear grid at nine items"
```

---

## Task 2: Filter and status-tone seams

**Files:**
- Create: `app-android/src/main/kotlin/io/kdmtracker/app/ui/SurvivorRoster.kt`
- Test: `app-android/src/test/kotlin/io/kdmtracker/app/SurvivorRosterTest.kt`

**Interfaces:**
- Consumes: `Survivor`, `SurvivorStatus`, `HitLocation`.
- Produces: `SurvivorFilter { ALL, INJURED, DEPARTING, UNNAMED, DEAD }`; `filterSurvivors(survivors: List<Survivor>, filter: SurvivorFilter): List<Survivor>`; `SurvivorTone { ALIVE, DEPARTING, DEAD, INJURED, UNNAMED }`; `survivorTone(survivor: Survivor): SurvivorTone`.

- [ ] **Step 1: Write the failing test**

`SurvivorRosterTest` (pure, no Compose):
- `filterAllReturnsEverySurvivor`.
- `filterDepartingKeepsOnlyDeparting` / `filterDeadKeepsOnlyDead`.
- `filterUnnamedKeepsBlankNames`.
- `filterInjuredKeepsAnyLightOrHeavyWound` — a survivor with a light HEAD wound and one with a heavy TORSO wound are kept; a clean one is not.
- `tonePrecedence` — DEAD → `DEAD`; DEPARTING → `DEPARTING`; a wounded ALIVE → `INJURED`; a blank-named healthy ALIVE → `UNNAMED`; a named healthy ALIVE → `ALIVE` (**Review Focus 2**).

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app-android:testDebugUnitTest --tests "io.kdmtracker.app.SurvivorRosterTest"`
Expected: FAIL — unresolved `filterSurvivors` / `survivorTone`.

- [ ] **Step 3: Implement**

In `SurvivorRoster.kt` (package `io.kdmtracker.app.ui`):
- `enum class SurvivorFilter { ALL, INJURED, DEPARTING, UNNAMED, DEAD }`.
- `fun filterSurvivors(survivors, filter): List<Survivor>` — `ALL` returns all; `DEPARTING`/`DEAD` filter by `status`; `UNNAMED` by `name.isBlank()`; `INJURED` by any location whose `WoundState.light || WoundState.heavy`.
- `enum class SurvivorTone { ALIVE, DEPARTING, DEAD, INJURED, UNNAMED }`.
- `fun survivorTone(survivor): SurvivorTone` with the precedence DEAD → DEPARTING → INJURED → UNNAMED → ALIVE.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app-android:testDebugUnitTest --tests "io.kdmtracker.app.SurvivorRosterTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app-android/src/main/kotlin/io/kdmtracker/app/ui/SurvivorRoster.kt app-android/src/test/kotlin/io/kdmtracker/app/SurvivorRosterTest.kt
git commit -m "feat(app): add the survivor filter and status-tone seams"
```

---

## Task 3: Shared composables + Step 2 rewrite

**Files:**
- Create: `app-android/src/main/kotlin/io/kdmtracker/app/ui/SurvivorComponents.kt`
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/ui/SurvivorsReturnStepScreen.kt`
- Keep: `app-android/src/main/kotlin/io/kdmtracker/app/ui/SurvivorEditorScreen.kt` (the full-screen editor)

**Interfaces:**
- Consumes: `filterSurvivors`, `survivorTone`, `ChoiceChip`, `UiState.survivors`.
- Produces: `StatusDot(tone)`; `SurvivorCard(survivor, survivalLimit, expanded, onToggle, onEdit, trailing)`; `SurvivorSheet(survivor, survivalLimit)`; `RosterFilterChips(selected, onSelect)`.

> No unit test: the project has no Compose UI test harness; the filter/tone logic is pinned by `SurvivorRosterTest` (Task 2). Verification is compilation, `spotlessCheck`, the full suite, and the emulator.

- [ ] **Step 1: Implement the shared composables**

`SurvivorComponents.kt`:
- `StatusDot(tone: SurvivorTone)` — an 8 dp circle coloured by tone (reuse the cockpit's palette: green/blue/grey/amber).
- `SurvivorCard(survivor, survivalLimit, expanded, onToggle, onEdit, trailing: @Composable () -> Unit = {})` — collapsed line `StatusDot · name · sex · STATUS` + a one-line summary (`Survival x · key stats · wound/token markers`); tapping calls `onToggle`; when `expanded`, renders `SurvivorSheet(survivor, survivalLimit)` + an `Editar ficha` button (`onEdit`); `trailing` carries the step control.
- `SurvivorSheet(survivor, survivalLimit)` — the read-only expanded content (survival/limit, insanity, six attributes, per-location armor + light/heavy, tokens, gear, disorders/fighting arts, progression, notes).
- `RosterFilterChips(selected: SurvivorFilter, onSelect: (SurvivorFilter) -> Unit)` — a horizontal `ChoiceChip` row: All / Injured / Departing / Unnamed / Dead.

- [ ] **Step 2: Rewrite `SurvivorsReturnStepScreen` on the set**

State: `var filter by remember { mutableStateOf(SurvivorFilter.ALL) }`, `var expandedId by remember { mutableStateOf<Ulid?>(null) }`, plus the existing `editing`/`pending` editor state. Render `RosterFilterChips`, `Heal & clear tokens`, then `filterSurvivors(state.survivors, filter).forEach { SurvivorCard(survivor = it, survivalLimit = state.survivalLimit, expanded = it.id == expandedId, onToggle = { expandedId = if (expandedId == it.id) null else it.id }, onEdit = { editing = it }) }`. Keep the `actionError` strip and the editor wiring.

- [ ] **Step 3: Run the full suite**

Run: `./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Manual emulator pass (Step 2)**

Run: `./gradlew :app-android:installDebug`, open a settlement, jump to Step 2: filters change the list; expanding a card shows the sheet; `Editar ficha` opens the editor. (**Review Focus 2**)

- [ ] **Step 5: Commit**

```bash
git add app-android/src/main/kotlin/io/kdmtracker/app/ui/SurvivorComponents.kt app-android/src/main/kotlin/io/kdmtracker/app/ui/SurvivorsReturnStepScreen.kt
git commit -m "feat(app): add the survivor components and redesign step 2"
```

---

## Task 4: Step 8 rewrite with the gear grid

**Files:**
- Create: `app-android/src/main/kotlin/io/kdmtracker/app/ui/GearGrid.kt`
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/ui/PrepareDepartingStepScreen.kt`

**Interfaces:**
- Consumes: `SurvivorCard`, `RosterFilterChips`, `UiState.gear`, the controller's `onDepartingChange`/`onAssignGear`/`onUnassignGear`.
- Produces: `GearGrid(assigned: List<String>, settlementGear: List<String>, onAssign: (String) -> Unit, onUnassign: (String) -> Unit)`.

> No unit test (UI-only); the departing cap is pinned in `:core` (existing) and the emulator checks the rest.

- [ ] **Step 1: Implement `GearGrid`**

A 3-column grid of **9 uniform cells** (all the same height, sized to the tallest content via a fixed minimum that wraps long names). Each cell shows an assigned gear name + its `m` detail, or `empty`. An assigned cell offers `Remove`; the row below the grid lists the settlement's crafted gear not yet carried, each an `Assign` control. No armor, no positions.

- [ ] **Step 2: Rewrite `PrepareDepartingStepScreen`**

Chips (`default ALL`, choice 3a) + the roster; each **non-DEAD** card's `trailing` is a `Depart`/`Departing` toggle calling `onDepartingChange(current ± id)`; a card whose survivor is `DEPARTING` (expand) shows its `GearGrid` (bound to `onAssignGear`/`onUnassignGear` and `state.gear`); `DEAD` cards are shown greyed and get no toggle (**Review Focus 5**). Keep the `actionError` strip and the editor wiring.

- [ ] **Step 3: Run the full suite**

Run: `./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Manual emulator pass (Step 8)**

Run: `./gradlew :app-android:installDebug`, jump to Step 8: mark departing survivors (cap `min(4, living)`); expand a departing card → the 3×3 grid; assign/remove gear; a 10th distinct item is refused with an error. (**Review Focus 3, 4**)

- [ ] **Step 5: Commit**

```bash
git add app-android/src/main/kotlin/io/kdmtracker/app/ui/GearGrid.kt app-android/src/main/kotlin/io/kdmtracker/app/ui/PrepareDepartingStepScreen.kt
git commit -m "feat(app): add the gear grid and redesign step 8"
```

---

## Self-Review

- **Spec coverage:** components + Step 2 (Task 3) · gear grid + Step 8 (Task 4) · cap in `:core` (Task 1) · filter + tone seams (Task 2) · dead shown/not departable (Tasks 2, 4).
- **Review Focus tests:** (1) `assigningATenthDistinctItemIsBlocked`; (2) `tonePrecedence` + filter tests; (3) existing `MarkDeparting` tests + Task 4 emulator; (4) Task 4 emulator; (5) `filterDeadKeepsOnlyDead` + Task 4 UI.
- **Type consistency:** `SurvivorFilter`, `SurvivorTone`, `filterSurvivors`, `survivorTone`, `SurvivorCard`, `GearGrid`, `MAX_GEAR_SLOTS` are used with the same shapes across tasks.
