# Plan — Hunt Phase screen (V2), rebuilt 1:1

Spec: `docs/superpowers/specs/2026-10-18-hunt-screen-design.md`.
Design: `.superpowers/design/caza/caza.html` (Stitch `4632e56b…`). Branch: `feat/hunt-screen`.

Order matters: extract shared primitives first (V2 and V3 both need them), then the board model,
then the screen, then the gate.

## Tasks

| ID | Spec | What | Route | Commit |
|---|---|---|---|---|
| T1 | S5 | Extract shared sheet primitives (`SheetPanel`, `CounterRow`, `PipTrack`, `StoneButton`, `ChipRow`, `AttributeStat`, `Symbol`) from `SurvivorSheet.kt` into `ui/components/`; `SurvivorSheet` imports them, behaviour unchanged. Pin with the existing suite. | inline | `refactor(app): extract the shared sheet primitives` |
| T2 | S3, S6 | `MoveMonster` usecase (TDD RED→GREEN): move the monster's space along the 13-space board; reaching the party's space resolves as **ambush** per `06_…` §2. | inline (TDD) | `feat(core): move the hunt monster and resolve ambush` |
| T3 | S6 | Views/model test pinning the 13-space board frame (index→icon/label by `HuntSpaceKind`, position markers). RED first. | inline (TDD) | `feat(app): model the 13-space hunt board frame` |
| T4 | S1, S2, S4 | Rebuild `HuntScreen` 1:1: seven regions in order, board = **13** spaces (`Casillas 0 - 12`), quarry plinth, instinct rule, party chips, event card (verbatim ranges), action buttons (`Avanzar`, `Mover Bestia`, `Enfrentamiento`). Reuse existing callbacks + wire `onMoveMonster`. | inline | `feat(app): render the hunt phase screen 1:1` |
| T5 | S7 | Visual gate: emulator captures (board w/ 13 spaces, plinth, chips, event card, buttons) → **maintainer approval before any push/PR**. | inline + maintainer | *(gate first)* |
| T6 | S6 | Full suite + both Python suites; PR with `--body-file`. | inline | `chore(test): pin the hunt phase screen` |

## Notes

- `HuntScreen` is already wired at `KdmTrackerApp.kt:186-194`; add `onMoveMonster` alongside the
  existing seven callbacks. The ViewModel gains `moveMonster()`.
- The board already exists (`SetUpHunt.BOARD_SPACES = 13`, `HuntState.spaces`); T4 is render only —
  no board-model change. T3 pins that render.
- `Tirar Dado` renders in the design's `Sin tirar aún` state; the d100 resolution is the **backlog**
  slice (S4), so the button is present but its roll path is not wired in V2.
- `Enfrentamiento` routes to `Screen.Showdown` (the tab already exists); it does not pull combat
  into this screen.
