# Design: Supervivientes — the paged survivor sheet (part A, screen program V1)

**Date:** 2026-10-16
**Status:** Draft (awaiting review)
**Slice:** Route the shell's `Supervivientes` tab to the design's paged survivor sheet — a single
cycling pager over the roster plus the already-built `SurvivorSheet`, with an empty state. Replaces
the `PendientePlaceholder()`.
**Depends on:** the Atramentum theme (slice 4) and the app shell (slice 5), merged; the survivor
sheet reskin (`2026-10-09-survivor-sheet-reskin-design.md`), merged. Track 1 / V1 of
`docs/roadmap.md`.

## Goal

Today `Screen.Survivors -> PendientePlaceholder()` (`KdmTrackerApp.kt:179`). It is the last of the
shell's four tabs that leads nowhere, even though `SurvivorSheet` is already reskinned and works
inside the settlement flow's expanded `SurvivorCard`.

The design (Stitch `projects/4909212909494507782/screens/310b1d1ff8994d619e67fda164f35f36`,
"Ficha de Superviviente", cached at `.superpowers/design/ficha/ficha.html`) has **no roster list**:
the tab is one survivor's full sheet, with a pager in the sub-header to step through the roster.

## Source of truth

Taken from the cached Stitch markup (not the brainstorm sketches, which are discarded):

**Sub-header / survivor selector** — a bar `bg-surface-container-low p-2 rounded-lg`, laid out
`justify-between`:

- **Left group** (`flex items-center gap-space-sm`):
  - a **single prev button** — `w-8 h-8 rounded bg-surface-container-high`, `aria-label="Anterior
    superviviente"`, `chevron_left` at 18px. Its JS **cycles**: `survivorIdx = (survivorIdx + 1) %
    survivors.length`, updating the name and gender chip in place. It is *one* button, not a
    prev/next pair; there is no counter and no disabled state in the markup.
  - a column with:
    - name (`id="survivor-name"`, `font-headline-sm text-headline-sm text-primary`) + gender chip
      (`id="survivor-gender"`, `H`/`M`, `font-label-caps … bg-surface-container text-secondary`);
    - a role subtitle (`Prólogo • Superviviente Fundador`, `font-body-sm text-on-surface-variant`).
- **Right group**: the `Equipo` button (`grid_view` icon + `Equipo` label, `bg-surface-container-high
  text-secondary`) — the sheet's existing gear entry.

**Sections below** are the sheet already rebuilt in slice 4 (PE de Caza, Supervivencia, Insanity,
atributos, localizaciones, cuadrícula de equipo, arma).

### Verbatim strings the design shows (and this slice keeps or defers)

| Design copy | This slice |
|---|---|
| `Anterior superviviente` (aria-label) | **keep** — the pager button's accessibility label |
| `H` / `M` gender chip beside the name | **keep** — from `Survivor.sex` (already modelled) |
| `Equipo` (right button) | **keep** — already the sheet's gear button |
| `Prólogo • Superviviente Fundador` role subtitle | **defer** — the recorded "Header narrative
  subtitle" gap (`not modelled`); same class as the 9 gaps in the reskin spec |

The sheet's own header today shows an `initial`-box + name + `Equipo`. The design shows
name + gender chip + role subtitle + `Equipo`, with the pager wrapping the whole bar. V1 replaces
the header's `initial`-box with the design's name + gender chip and folds the pager around it;
the role subtitle stays out (deferred gap).

## Behaviour

**Pager over the roster.** The tab renders survivor `[index]` of `state.survivors` (the order the
controller already exposes, `LanternYearController.kt:690`). The single prev button advances
`(index + 1) % size` — the design's cycle. Rationale: the design ships one button with modulo
arithmetic, not a pair; adding a second button would be inventing beyond the markup.

**Empty state (maintainer decision, 2026-10-16).** When `state.survivors` is empty, show a simple
centred message — the same tone as other empty states — and hide the pager. The design defines no
empty state (the mock always has 2 survivors), so copy is ours: **`Aún no hay supervivientes.`**
No action button: survivor creation lives in settlement steps 2/8, not here.

**Navigation to a survivor is index-based and resets.** Selecting the tab always starts at index 0
(first survivor). Paging is pure UI state (no rules); edits route through the sheet's existing
`onSave`/`onAssignGear`/`onUnassignGear` callbacks already wired in `KdmTrackerApp`.

## Scope

- `KdmTrackerApp.kt` — `Screen.Survivors ->` the new `SurvivorsScreen(...)` instead of
  `PendientePlaceholder()`; pass `state`, the survivor callbacks already threaded to the sheet
  (`onSaveSurvivor`, `onAssignGear`, `onUnassignGear`), and `onOpenEditor` for `Equipo`.
- **New** `ui/SurvivorsScreen.kt` — stateless: empty state or the pager + `SurvivorSheet`.
  The pager index is a `rememberSaveable` (pure UI state; survives rotation).
- `SurvivorSheet` — its header gains a pager affordance slot (or the pager bar is built in
  `SurvivorsScreen` and the sheet keeps only the name-column content). Decision in the plan: the
  **pager bar lives in `SurvivorsScreen`**, wrapping the sheet, so the sheet's internal header
  stays untouched where it is also used by the expanded `SurvivorCard`.
- **New** a small pure model (e.g. `SurvivorPagerModel`) if any logic is derived (index cycling,
  gender label from `Sex`, empty check) — kept in a testable pure file, following the
  `ResourceAdjustModel` pattern from Fase 3.

**Out of scope (recorded, not this slice):**
- The 9 recorded gaps of the reskin spec (4 regions + 5 elements), including the role subtitle.
- Rebuilding the roster list (the design has none; the in-step rosters in steps 2/8 are untouched).
- Extracting shared sheet primitives to `ui/components/` (roadmap prep for V2/V3, not V1).

## Testing

No Compose UI harness exists (JVM `kotlin.test` pinning derived views, as `DevelopViewsTest`).

1. **`SurvivorPagerModelTest`** (pure) — cycling `(i+1) % n` at the boundary (last→first,
   first→second); gender chip label from `Sex` (`H`/`M`/`O` for `OTHER`); the empty predicate
   (empty roster ⇒ empty, non-empty ⇒ not); the index is clamped if the roster shrinks.
2. **A views-style test** pinning that a `UiState` with survivors resolves to a non-empty sheet
   frame and an empty `UiState.survivors` resolves to the empty frame — following the existing
   derived-view pattern.
3. **Visual gate (manual, maintainer):** render on the emulator — the sheet with a pager (first
   survivor), paging to the next, and the empty state — and get approval **before** push/PR, per
   the repo's hard rule.

## Gate

The full suite (`./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test
spotlessCheck` + both Python suites) green; maintainer approves the rendered emulator output; PR
against `main`; squash-merge with `— size:exception` if over 400 lines.
