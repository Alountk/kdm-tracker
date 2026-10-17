# Feature: Supervivientes — the paged survivor sheet (V1)

Route the shell's `Supervivientes` tab from `PendientePlaceholder()` to the design's paged
survivor sheet: a single cycling pager over the roster plus the already-built `SurvivorSheet`,
with an empty state.
Design: Stitch `310b1d1ff8994d619e67fda164f35f36`, cached `.superpowers/design/ficha/ficha.html`.

## Specs

- **S1** — The tab renders `SurvivorsScreen` instead of `PendientePlaceholder()`; the pager cycles
  with `(index + 1) % size` (the design's single prev button), a gender chip `H`/`M`/`O` beside the
  name, and `Equipo` on the right. Design strings kept verbatim: `Anterior superviviente`
  (contentDescription), `Equipo`.
- **S2** — When `state.survivors` is empty, show a centred message **`Aún no hay supervivientes.`**
  and hide the pager. No action button (survivor creation lives in settlement steps 2/8).
- **S3** — The design's role subtitle `Prólogo • Superviviente Fundador` is **deferred** (the
  recorded "Header narrative subtitle" gap, `not modelled`); not rendered in V1.
- **S4** — Pager index is pure UI state (`rememberSaveable`), resets to survivor 0 when the tab is
  selected; edits route through the sheet's existing `onSave`/`onAssignGear`/`onUnassignGear`.
- **S5** — Tests: `SurvivorPagerModelTest` (cycle boundaries, gender labels, empty predicate,
  clamp) RED→GREEN; a views-style test pinning empty vs non-empty frame; full suite + both Python
  suites green.

## Tasks

| ID | Spec | Route | Commit |
|---|---|---|---|
| T1 | S1, S5 | inline (small, TDD) | `feat(app): model the survivor pager` |
| T2 | S1, S4, S5 | inline | `feat(app): render the paged survivor sheet` |
| T3 | S1 | inline | `feat(app): route the supervivientes tab to the paged sheet` |
| T4 | S5 (visual gate) | inline + maintainer | *(gate before any push/PR)* |
| T5 | S5 (suite + PR) | inline | `chore(test): pin the paged survivor sheet` |

## Log

- **L1** (2026-10-16): maintainer approved starting V1 after the roadmap review — "dale" (V1 =
  Supervivientes, the paged sheet). Roadmap confirmed: only settlement step 7 is 1:1 with Stitch;
  the other tabs' screens are the remaining rebuilds.
- **L2** (2026-10-16): maintainer decision on the empty state — chose "Mensaje simple en el
  vacío" (`Aún no hay supervivientes.`, no button) over keeping the placeholder or a ghost sheet.
