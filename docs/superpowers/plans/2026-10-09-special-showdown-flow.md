# Special Showdown + Nemesis Flow Implementation Plan (Slice 3)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the app trigger the two settlement-flow branches the domain already models: a nemesis year skips the Hunt straight to the Showdown, and a player-declared Special Showdown at step 9 loops back to step 5 — reusing the encounter tracker.

**Architecture:** All rules stay in `:core` (`SettlementState.specialShowdown`, `SetSpecialShowdown`, a `nemesisOf` helper) and flow through `LanternYearFlow` via the `FlowContext` the controller builds. `:app-android` extracts a shared `EncounterPanel` and adds the Step 9 body.

**Tech Stack:** Kotlin Multiplatform (`:core`), Compose Multiplatform (Android), kotlinx.serialization, kotlin.test.

**Spec:** `docs/superpowers/specs/2026-10-09-special-showdown-flow-design.md`

## Global Constraints

- Rules in `:core`; screens stateless. No Compose UI tests.
- UI copy English; Conventional Commits; **no** `Co-Authored-By`.
- Pre-commit runs `spotlessKotlinCheck`; run `./gradlew spotlessApply` before committing.
- **RED before implementing** each test.
- Full suite green: `./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck`.

## Review Focus

1. **Nemesis routing** — a nemesis year's Step 11 advance reaches the Showdown; a non-nemesis year reaches the Hunt (Task 3).
2. **Special Showdown loop** — Step 9 advancing with the flag loops back to Step 5; without it, Step 9 advances to 10 (Task 3).
3. **`nemesisOf` parsing** — `"Nemesis: The Butcher Lvl 2"` → `"The Butcher"`; a plain year → `null` (Task 1).
4. **Legacy transfer** — a v9 document (no `specialShowdown`) still decodes (Task 1).
5. **The flag is per-year state**, persisted and cleared appropriately — declared at step 9, it must not leak into the next year (Task 2).

---

## Task 1: Special-showdown state + nemesis helper + transfer v10

**Files:** modify `core/src/commonMain/kotlin/io/kdmtracker/core/domain/SettlementState.kt`, `SettlementRecord.kt`, `transfer/TransferDocument.kt` (v10); test `SpecialShowdownStateTest.kt`; modify `SettlementStateTransferTest.kt`.

**Interfaces:** `SettlementState.specialShowdown: Boolean = false`; `SettlementRecord.nemesisOf(year: Int): String?`; `TransferDocument.schemaVersion = 10`.

- [ ] **Step 1: Write the failing test** — `nemesisOf` returns `"The Butcher"` for a year whose timeline has `"Nemesis: The Butcher Lvl 2"`, and `null` for a plain year; the state round-trips; a v9 document decodes with `specialShowdown == false`.
- [ ] **Step 2: Run it RED** (`./gradlew :core:jvmTest --tests "io.kdmtracker.core.domain.SpecialShowdownStateTest"`).
- [ ] **Step 3: Implement** the field + the helper (parse: strip a `Nemesis:` prefix, then a trailing ` Lvl N`) + transfer v10 (+ the default-version assertions in `SettlementStateTransferTest`/`TransferDocumentTest`/`SurvivorMigrationTest`).
- [ ] **Step 4: Run it GREEN.**
- [ ] **Step 5: Commit** `feat(core): add the special-showdown flag and nemesis helper (transfer v10)`.

---

## Task 2: `SetSpecialShowdown` use case

**Files:** create `core/src/commonMain/kotlin/io/kdmtracker/core/usecase/SetSpecialShowdown.kt`; test `SpecialShowdownUseCaseTest.kt`.

**Interfaces:** `SetSpecialShowdown.run(record, enabled: Boolean): StepResult<SettlementRecord>`.

- [ ] **Step 1: Write the failing test** — setting true then false toggles `state.specialShowdown` (**Review Focus 5**).
- [ ] **Step 2: Run it RED.**
- [ ] **Step 3: Implement** the use case.
- [ ] **Step 4: Run it GREEN.**
- [ ] **Step 5: Commit** `feat(core): declare a special showdown`.

---

## Task 3: Controller nemesis/special routing

**Files:** modify `app-android/src/main/kotlin/io/kdmtracker/app/UiState.kt`, `LanternYearController.kt`; test `SettlementFlowBranchControllerTest.kt`.

**Interfaces:** `UiState.{specialShowdown, nemesis}`; `LanternYearController.{declareSpecialShowdown, nemesisFor}`; `advance` builds `FlowContext(isNemesisYear, hasSpecialShowdown, …)`.

- [ ] **Step 1: Write the failing test** — with a record at Step 11 on a **nemesis year** (a timeline entry for the year), `advance()` → `phase == SHOWDOWN`; on a **plain** year → `phase == HUNT`; with `specialShowdown = true` at Step 9, `advance()` → `currentStep == 5` (the LoopBack); `declareSpecialShowdown(true)` persists. (**Review Focus 1, 2**)
- [ ] **Step 2: Run it RED.**
- [ ] **Step 3: Implement** — `nemesisFor(record)` via `nemesisOf`; `advance()` builds the context; `declareSpecialShowdown`; `UiState.specialShowdown`/`nemesis`; the ViewModel delegate.
- [ ] **Step 4: Run it GREEN.**
- [ ] **Step 5: Commit** `feat(app): route the nemesis year and the special showdown`.

---

## Task 4: Shared encounter panel + Step 9 body

**Files:** create `app-android/src/main/kotlin/io/kdmtracker/app/ui/EncounterPanel.kt`, `ui/SpecialShowdownStepScreen.kt`; modify `ui/ShowdownScreen.kt`, `ui/SettlementScreen.kt`, `LanternYearViewModel.kt`, `KdmTrackerApp.kt`. No unit test; suite + emulator.

- [ ] **Step 1: Extract `EncounterPanel`** — move the AI-deck tracker + the last-drawn card + the hit-locations reference + the outcome + the aftermath (deaths/XP) out of `ShowdownScreen` into `EncounterPanel(state, onDraw, onWound, onGrantHuntXp, onRecordDeaths, onFinish, concludeLabel)`.
- [ ] **Step 2: Use it in `ShowdownScreen`** and pre-select `state.nemesis` in the setup when set.
- [ ] **Step 3: Implement `SpecialShowdownStepScreen`** — a toggle bound to `state.specialShowdown` (→ `onDeclare`), and, when declared, the `EncounterPanel` with `concludeLabel = "Conclude · back to Step 5"` calling `viewModel::advance`.
- [ ] **Step 4: Dispatch Step 9** in `SettlementScreen`'s `when`; wire the ViewModel/KdmTrackerApp callbacks.
- [ ] **Step 5: Full suite** + **manual emulator pass** (advance to a nemesis year → Showdown; step 9 with the flag → back to step 5).
- [ ] **Step 6: Commit** `feat(app): add the special-showdown step and share the encounter panel`.

---

## Self-Review

- **Spec coverage:** state + helper + v10 (T1) · declare use case (T2) · routing (T3) · shared panel + step 9 UI (T4).
- **Review Focus tests:** (1) the nemesis-vs-plain Step 11 routing (T3); (2) the Step 9 loop (T3); (3) `nemesisOf` (T1); (4) the v9 decode (T1); (5) the toggle + per-year persistence (T2/T3).
- **Type consistency:** `specialShowdown`, `nemesisOf`, `SetSpecialShowdown`, `declareSpecialShowdown`, `nemesisFor`, `EncounterPanel` are used with the same shapes across tasks.
