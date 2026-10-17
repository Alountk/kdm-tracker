# Plan: implement the paged survivor sheet in five tasks

**Spec:** `docs/superpowers/specs/2026-10-16-supervivientes-paged-sheet-design.md`
**Date:** 2026-10-16

Ordered tasks. Each names its spec subsections, its route (inline / worker), its tests, and its
commit. TDD where a pure model exists: observe RED, then GREEN.

## Tasks

### T1 — The pager model (pure) — spec "Behaviour" — route: inline (small)
New `app-android/src/main/kotlin/io/kdmtracker/app/ui/SurvivorPagerModel.kt`:
- `advance(index: Int, size: Int): Int` = `(index + 1) % size` when `size > 0`, else `0` (the
  design's cycle; guard the divide-by-zero).
- `genderLabel(sex: Sex): String` → `H` / `M` / `O` (design's chip, `Sex.OTHER` → `O`).
- `isEmpty(survivors: List<Survivor>)` (or take the size) → the empty-state predicate.
- `clamp(index, size)` for a shrinking roster.

RED first: `app-android/src/test/.../SurvivorPagerModelTest.kt` (cycle boundaries, gender labels,
empty, clamp). Then implement → GREEN.

**Commit:** `feat(app): model the survivor pager`

### T2 — The SurvivorsScreen composable — spec "Scope"/"Behaviour" — route: inline
New `app-android/src/main/kotlin/io/kdmtracker/app/ui/SurvivorsScreen.kt`:
- `rememberSaveable` pager index (pure UI state).
- Empty roster → centred `Aún no hay supervivientes.`, no pager.
- Non-empty → the pager bar (design's sub-header: prev `chevron_left` button with
  `Anterior superviviente` contentDescription, name + gender chip column, `Equipo` right) wrapping
  `SurvivorSheet`. Prev advances via `SurvivorPagerModel.advance`.
- Threads the sheet's existing callbacks.

A views-style test pinning empty vs non-empty frame resolution (existing derived-view pattern).

**Commit:** `feat(app): render the paged survivor sheet`

### T3 — Route the tab in KdmTrackerApp — spec "Scope" — route: inline
`Screen.Survivors -> SurvivorsScreen(...)` replacing `PendientePlaceholder()`; pass `state` and the
survivor callbacks already threaded in the `SettlementScreen` branch. Keep `PendientePlaceholder`
(it is still used by `SettlementScreen.kt:141`).

**Commit:** `feat(app): route the supervivientes tab to the paged sheet`

### T4 — Visual gate (manual) — spec "Testing.3" — route: inline + maintainer
Build, launch the emulator (`kdm` / `emulator-5554`), screenshot: first survivor with pager, paging
to the next, empty state. **Stop and get maintainer approval before any push/PR.**

### T5 — Full suite + backlog note + PR — spec "Gate" — route: inline
Run the full suite + both Python suites; `spotlessApply` before judging format. Note the deferred
role-subtitle gap in `docs/backlog.md` if not already there. Push, open PR with `— size:exception`
if over 400 lines (use `--body-file`).

**Commit:** `chore(test): pin the paged survivor sheet` (+ the PR)

## Order and gates

T1 → T2 → T3 are code (RED/GREEN on T1 and T2). **T4 gate before push.** T5 after approval.
The visual rule is hard: nothing visual leaves the machine until the maintainer approves the
rendered output.
