# Feature: Caza — the hunt phase screen (V2)

Rebuild the shell's `Caza` tab 1:1 from the Stitch design: the quarry plinth, active-instinct
rule, party chips, the **13-space** hunt board, the active hunt-event card, and the phase action
buttons — plus model `MoveMonster` (which does not exist yet). Keeps the Hunt Phase separate from
the Showdown.
Design: Stitch `4632e56b39c5470188dbe0b087122767`, cached `.superpowers/design/caza/caza.html`.

## Specs

- **S1** — `HuntScreen` renders the design's seven regions in order over `state.hunt`, reusing the
  existing callbacks; setup still offers quarry/level/players first when `spaces` is empty.
  Verbatim strings: `Presa Activa`, `Nivel 1`, `León Blanco`, `Variante Primera Historia`,
  `Distancia`/`casillas`, `Instinto: Olfateo`, `El León Blanco olfatea el aire y finaliza su turno.
  Todos los supervivientes pasan a ser considerados amenazas activas.`, `Cazadores en Marcha`,
  `4 Linternas encendidas`, `Avanzar Supervivientes (+1 Casilla)`, `Mover Bestia`, `Enfrentamiento`.
- **S2** — The board renders **13** spaces from `HuntState.spaces` with header `Casillas 0 - 12`
  (**corrected from the design's `Casillas 0 - 8`** — maintainer: the model/rules are 13), an
  index/icon/label per `HuntSpaceKind` (`START`→`home Asent.`, party→`flare Grupo`,
  `MONSTER_E`→`E Evento`, `MONSTER`→`skull Guarida`), plus markers
  `Supervivientes (Casilla <position>)` / `Monstruo (Casilla <last>)`.
- **S3** — **`MoveMonster`** is modelled: a usecase moves the monster's space along the board
  (`directions_walk Mover Bestia`); reaching the party's space resolves as **ambush** per the
  manual (`external-assets/06_…` §2). TDD RED→GREEN; no usecase existed.
- **S4** — The hunt-event card renders the example event verbatim (`Evento de Caza #34`,
  `Tirada d10`, `Bruma Olorosa y Viento Fétido`, ranges `1 - 3`/`4 - 7`/`8 - 10` with their effects,
  `Resultado d10`, `Sin tirar aún`, `Tirar Dado`). The **d100** table resolution is **deferred to
  backlog** — the control renders in the design's `Sin tirar aún` state.
- **S5** — Prep: extract shared sheet primitives (`SheetPanel`, `PipTrack`, `AttributeStat`,
  `StoneButton`, `Symbol`) from `SurvivorSheet.kt` into `ui/components/`, **conservatively** — each
  keeps its **exact current signature and render** (e.g. `Symbol(name, color)` keeps SurvivorSheet's
  `bodyLarge` render, no `lineHeight` change), and only `SurvivorSheet` imports them in V2. **Not**
  in scope: consolidating the 6 divergent `Symbol` copies across `settlement/*.kt` (5 of them pass
  `size` + `lineHeight`, a different render), the `CounterRow` in `DeathCountStepScreen` (a
  different component: field+record, not −/value/+), or `GearGrid` (already its own file) — those
  stay as-is so V2 introduces **zero visual change** outside the hunt screen.
- **S6** — Tests: `MoveMonster` usecase test (move + ambush edges) and a 13-space board-frame
  views/model test, both RED→GREEN; full suite + both Python suites green.
- **S7** — Visual gate: emulator captures of the rebuilt screen **approved by the maintainer
  before any push/PR** (standing rule).

## Tasks

| ID | Spec | Route | Commit |
|---|---|---|---|
| T1 | S5 | inline (refactor, suite-pinned) | `refactor(app): extract the shared sheet primitives` → **`9cdde77` (PR #38, slice 1)** |
| T2 | S3, S6 | inline (TDD) | `feat(core): move the hunt monster and resolve ambush` → **`7f95404` (PR #38, slice 1)** |
| T3 | S6 | inline (TDD) | `feat(app): model the 13-space hunt board frame` → **`cae7db9` (slice 2)** |
| T4 | S1, S2, S4 | inline | `feat(app): render the hunt phase screen 1:1` → **`4265bf4` (slice 2)** |
| T5 | S7 (visual gate) | inline + maintainer | **APPROVED as-is** 2026-10-18 (chips require real DEPARTING data; Distancia derived) |
| T6 | S6 (suite + PR) | inline | `chore(test): pin the hunt phase screen` |

## Log

- **L5** (2026-10-18): T1 inspection found the plan's premise wrong — `Symbol` is duplicated ×6
  with **divergent** signatures (`settlement/*.kt` pass `size`+`lineHeight`, SurvivorSheet does
  not), `CounterRow` in `DeathCountStepScreen` is a **different** component (field+record), and
  `GearGrid` is already its own file. Consolidating would change unrelated renders. Maintainer
  chose **conservative extraction** (`ask_user_choice`): `ui/components/` with exact current
  signatures, only `SurvivorSheet` imports them, zero visual change; the global `Symbol` cleanup is
  deferred.
- **L6** (2026-10-18): **slice boundary** — after T1+T2 the branch is at 532 authored lines (past
  the ~400-line budget). Delivery strategy: **T1+T2 ship as slice 1** (prep + `MoveMonster`, both
  domain/refactor, no visual change → green suite is the only gate, no emulator gate needed for
  this slice). Open PR #1 `refactor+feat: shared sheet primitives and the hunt monster move` now.
  **Slice 2 = T3–T6** (board-frame test, screen 1:1 rebuild, emulator gate, PR) carries the visual
  change and its gate.

## Tasks

- **L1** (2026-10-18): maintainer chose V2 = Caza next and approved starting it ("dale").
- **L2** (2026-10-18): maintainer corrected a **Caza/Enfrentamiento conflation** — the Hunt Phase
  advances a linear **13-space** board (manual pág. 61–63); `E` spaces hold Monster Event cards and
  free spaces roll **d100** on the hunt-event table (pág. 90–105); Showdown (AI deck, combat) is a
  separate phase. He attached 25 KDM manual `.md` files under `external-assets/` with
  `00_ndice_maestro_y_router_de_documentos.md` as the router.
- **L3** (2026-10-18): maintainer confirmed the Stitch design's `Casillas 0 - 8` (9 spaces) is a
  **design error** — show the real **13**; "habría que hacer ese cambio en esa parte del diseño".
- **L4** (2026-10-18): maintainer scope decision (`ask_user_choice`) — **V2 = render 1:1 +
  `MoveMonster`; the d100 hunt-event table → backlog** (a contradictory "todo en V2" first answer
  was resolved to this by a follow-up choice).
- **L5** (2026-10-18): T1 inspection found the plan's premise wrong — `Symbol` is duplicated ×6
  with **divergent** signatures (`settlement/*.kt` pass `size`+`lineHeight`, SurvivorSheet does
  not), `CounterRow` in `DeathCountStepScreen` is a **different** component (field+record), and
  `GearGrid` is already its own file. Consolidating would change unrelated renders. Maintainer
  chose **conservative extraction** (`ask_user_choice`): `ui/components/` with exact current
  signatures, only `SurvivorSheet` imports them, zero visual change; the global `Symbol` cleanup is
  deferred.
