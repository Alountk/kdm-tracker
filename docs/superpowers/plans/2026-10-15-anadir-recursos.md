# Add-Resources Flow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Land the summary's four-state resource adjust — tap to edit, inline stepper, minimum guard, confirmed strip — consumed 1:1 from the cached `recursos_flujo.html`, with the flow's state moved out of the composable into a pure model built on the existing `ResourceEdit`.

**Architecture:** `app-android` only. One new pure state holder (`ResourceAdjustFlow`) in `ResourceAdjustModel.kt` owns selection, pending count and the last confirmed result; `SettlementSummary` becomes a renderer over it; `ResourceColour.icon` follows the newest design for two cells. `:core`'s `AddResourceEntry` keeps enforcing the non-negative ledger; the controller keeps publishing refusals.

**Tech Stack:** Kotlin Multiplatform (`core`, `data`), Compose Multiplatform / Android (`app-android`), SQLDelight (`data`), Spring Boot (`server`), Gradle, JUnit.

**Spec:** `docs/superpowers/specs/2026-10-15-anadir-recursos-design.md`

## Global Constraints

- Spanish UI copy; English code, identifiers, comments, commit messages. Conventional commits. **No AI attribution anywhere.**
- `./gradlew spotlessApply` before every commit. Suite gate: `./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck` + `python3 -m unittest discover -s tools/content-ingest/tests` + `python3 -m unittest discover -s tools/fonts/tests`.
- `TransferDocument.schemaVersion` stays **12**. No `:core`, `:data`, `:server` or font change in this phase.
- Design is consumed from the cached `.superpowers/design/innovar-flow/recursos_flujo.html` 1:1. 1 CSS px ≈ 1 dp. Material Symbols: pin `lineHeight = fontSize`.
- **Visual changes are never committed, pushed or PR'd until the maintainer approves the measured render.**
- Palette: surface `#131315`, surface-container `#201F21`, surface-container-low `#1B1B1D`, surface-container-high `#2A2A2C`, secondary gold, error, outline-variant, `kdm-green`/`kdm-red` accents (as in `InnovarPalette`).
- PR budget 400 lines → `— size:exception` (pre-authorised, confirm at push time).

## Flow authority

The maintainer's decisions (spec, 2026-10-15) override the sheet where they disagree:

1. **Tap to edit**, hint copy = `Toca una celda para ajustarla.` (the sheet's own UX verdict beats its literal long-press hint).
2. **Icons:** Hueso `nutrition`, Órgano `psychology` (newer design wins over `format_h4`/`vital_signs`); Pellejo `layers`, Chatarra `hardware` unchanged. Both glyphs already pinned — no font work.
3. **`delta == 0` saves nothing:** no `onAdjust`, no success strip (closes "guardar sin cambio grita éxito").
4. **Minimum keeps the save button:** the red caption box renders *above* `Guardar ajuste` so `12 → 0` stays confirmable (the sheet drops the button; deliberate delta).

## Review Focus

- `save()` with `delta == 0` must clear the edit and produce **no** confirmed result — the caller must not call `onAdjust`. *(Task 2)*
- `cellState` matches by `ResourceId`, never by label: a confirmed cell must re-green after the model rebuilds from state. *(Task 2)*
- Non-selected cells dim **only while editing**; at rest and confirmed they render normally. *(Task 3)*
- At `pending == 0` the minus is inert, the cell shows `agotado` + red accents, and the save button still renders. *(Task 3)*
- The hint row shows only when there is nothing open **and** nothing confirmed — never alongside the success strip. *(Task 3)*

---

### Task 1: Read the design and pin the deltas

**Files:**
- Read: `.superpowers/design/innovar-flow/recursos_flujo.html`
- Read: `app-android/src/main/kotlin/io/kdmtracker/app/ui/settlement/{SettlementSummary.kt,ResourceAdjustModel.kt,SettlementSummaryModel.kt}`
- Read: `docs/superpowers/specs/2026-10-15-anadir-recursos-design.md`

**Interfaces:**
- Produces: nothing but understanding — every later task's markup comes from this pass.

- [ ] **Step 1:** Extract the four states' structure and copy from the cached HTML (cell stack order, `previo:`/`agotado` captions, footer rows, exact Spanish strings).
- [ ] **Step 2:** Diff against today's `SettlementSummary.kt` and list what changes (stack order, `opacity-60`, red/green accents, footer precedence, hint copy, two icons).
- [ ] **Step 3:** Confirm the spec's two deliberate deltas (save button at minimum; hint copy) are the only deviations you will ship.
- [ ] **Step 4:** Stop and present the delta list with the spec link.

### Task 2: The flow model (RED → GREEN)

**Files:**
- Modify: `app-android/src/test/kotlin/io/kdmtracker/app/ResourceAdjustModelTest.kt`
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/ui/settlement/ResourceAdjustModel.kt`

**Interfaces:**
- Produces: `ResourceAdjustFlow(select, bump, save, delta, cellState, isEditing, isIdle)` + `AdjustedCell(id, result)`; `CellState { REST, EDITING, MINIMUM, CONFIRMED }`.

- [ ] **Step 1 (RED):** Write the failing tests from the spec's Testing section: select opens/clears; bump floors at 0 and yields `MINIMUM`; `cellState` by `ResourceId`; `save` with `delta == 0` emits no confirmed result; `save` with `delta != 0` records `AdjustedCell` with `Hueso: 12 → 14` and clears the edit; `isIdle` iff nothing open and nothing confirmed. Run `./gradlew :app-android:testDebugUnitTest --tests "io.kdmtracker.app.ResourceAdjustModelTest"` — expect the new tests red, the five existing ones still green.
- [ ] **Step 2 (GREEN):** Implement `ResourceAdjustFlow` in `ResourceAdjustModel.kt` on top of `ResourceEdit`/`AdjustResult` — no other file changes. Re-run the test class until green.
- [ ] **Step 3:** Run the suite gate; commit `feat(app): model the resource adjust flow` (tests + model only).

### Task 3: Render the four states

**Files:**
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/ui/settlement/SettlementSummary.kt`
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/ui/settlement/SettlementSummaryModel.kt`
- Modify: `app-android/src/test/kotlin/io/kdmtracker/app/SettlementSummaryTest.kt`

**Interfaces:**
- Consumes: `ResourceAdjustFlow` (Task 2).
- Produces: the sheet's four states in `SettlementSummary`; `ResourceColour.BONE("nutrition")`, `ResourceColour.CORAL("psychology")`.

- [ ] **Step 1 (RED):** Add the icon pins to `SettlementSummaryTest` (`nutrition`/`psychology`/`layers`/`hardware`) — red until Step 3.
- [ ] **Step 2:** Replace the composable's `editing`/`pending`/`saved` locals with one `ResourceAdjustFlow` in `remember`. Footer precedence: editing/minimum ⇒ red caption (only at 0) + full-width `Guardar ajuste`; confirmed ⇒ green strip; else ⇒ hint. `onAdjust` fires only when `flow.delta != 0`, then `save()`.
- [ ] **Step 3:** Cell render per spec: rest stack `icon → label → value` at 74dp; editing cell gold border with `previo: N` inside; non-selected `opacity-60` while editing; minimum cell red with disabled minus, `agotado` caption and red error box above the button; confirmed cell green border + dot; `ResourceColour.icon` update for BONE/CORAL; hint copy `Toca una celda para ajustarla.`.
- [ ] **Step 4:** `./gradlew spotlessApply` + full suite gate; commit `feat(app): render the four resource adjust states` — **not pushed**.

### Task 4: Visual gate (emulator)

**Files:** none committed in this task.

**Interfaces:**
- Consumes: Task 3's uncommitted render.
- Produces: the maintainer's approval (or correction notes) for the four states.

- [ ] **Step 1:** `./gradlew :app-android:installDebug` on emulator `kdm` (adb `/opt/homebrew/share/android-commandlinetools/platform-tools/adb`, 1080×2400, app `io.kdmtracker.app`).
- [ ] **Step 2:** Capture and present: **rest** (hint row), **editing** (dimmed cells, `previo:`), **minimum** (red cell, `agotado`, caption above save), **confirmed** (green cell + strip), and a **save-with-no-change** tap (nothing should happen).
- [ ] **Step 3:** Stop. Nothing is committed until the maintainer approves the renders; corrections come back as notes on these four states.

### Task 5: Suite, commits and PR

**Files:**
- Modify (only if the gate asked for corrections): the Task 3 files.

**Interfaces:**
- Produces: green suite, the phase's commits on `feat/anadir-recursos`, PR to `main`.

- [ ] **Step 1:** Full suite gate (Gradle five targets + both Python suites) green after any gate corrections.
- [ ] **Step 2:** Create/verify the branch `feat/anadir-recursos` from `main` with the approved commits; push and open the PR (title `feat(app): the add-resources flow — the summary's four adjust states`, `— size:exception` if over 400 lines).
- [ ] **Step 3:** Record the phase as done in `docs/backlog.md` (the two deferred minors closed: hint copy, save-without-change) and update the roadmap note.
