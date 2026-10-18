# Spec — Hunt Phase screen (V2), rebuilt 1:1 from the Stitch design

Design: Stitch `4632e56b39c5470188dbe0b087122767` "Fase de Caza" (780×2650, dark, es), cached
`.superpowers/design/caza/caza.html`.

## Context and the two corrections the maintainer made

The Hunt Phase is **not** the Showdown. Per the manual (`external-assets/06_caza_…`, pág. 61–63)
the hunt advances a **linear 13-space board**: fixed Inicio / Oscuridad Abrumadora / Hambruna, the
monster on the last space, `E` spaces holding Monster Event cards and free spaces holding Basic
Hunt Events; the party moves toward the monster and reveals the top card. Basic events roll on the
**d100 hunt-event table** (pág. 90–105). The Showdown (AI deck, combat, `05_…`, pág. 65–75) is a
**separate phase** and is out of scope here.

Two corrections to the Stitch render, decided by the maintainer 2026-10-18:

1. **13 spaces, not 9.** The design draws `Casillas 0 - 8` (nine). The game/model are thirteen
   (`SetUpHunt.BOARD_SPACES = 13`). **Show the real 13**; this is a fix to that part of the design.
2. **Caza ≠ Enfrentamiento.** Keep the hunt's own mechanics (advance, reveal, event resolution,
   monster move/ambush, starvation) inside this screen; do not pull Showdown combat in.

## Scope of V2 (user's decision, `ask_user_choice`)

- **In**: rebuild `HuntScreen` 1:1 against the design over the **existing** `HuntState` domain, and
  **model `MoveMonster`** (move the monster along the board → ambush), which has no usecase today.
- **Out → backlog** (`docs/backlog.md`): the **d100 hunt-event table** (the 100 events,
  `external-assets/09..11`) is **not** modelled in V2. The design's "Tirada **d10** / 3 ranges"
  event is an illustrative example; the real roll is **d100**. The `Tirar Dado` control renders but
  its d100 resolution is the deferred slice.

## Regions (design's own HTML comments, in order) and their strings kept verbatim

1. **Shell header** (already the shared shell) — `Año Linterna 1`, `Caza`, `El Cúmulo`, stat cluster
   `group 12` / `skull 0` / `flare 4` / `person`.
2. **Monstruo Objetivo & Atributos Plinth** — `Presa Activa`, `Nivel 1`, `León Blanco`,
   `Variante Primera Historia`, `Distancia` `3` `casillas`; stat matrix `Mov 6`, `Res 8`, `Vel -`,
   `Daño -`.
3. **Regla de Instinto Activo** — `Instinto: Olfateo` + `El León Blanco olfatea el aire y finaliza
   su turno. Todos los supervivientes pasan a ser considerados amenazas activas.`
4. **Partida de Caza (Expedición de Supervivientes)** — `Cazadores en Marcha`,
   `4 Linternas encendidas`, four survivor chips: `AL Albar` `psychology 3` `favorite 4`,
   `ER Erlinda` `psychology 5` `favorite 3`, `MA Marko` `psychology 1` `favorite 5`,
   `SA Sarai` `psychology 2` `favorite 4`.
5. **Tablero Lineal de la Ruta de Caza** — header `route` `Tablero de Caza`, `Casillas 0 - 12`
   (**corrected from the design's `0 - 8`**). Thirteen spaces from `HuntState.spaces`, each showing
   its index, an icon and a label by `HuntSpaceKind`:
   `START`→`home Asent.`, party space→`flare Grupo`, `MONSTER_E`→`E Evento`,
   `OVERWHELMING_DARKNESS`→ its kind's label, `MONSTER`→`skull Guarida`, `NORMAL`→`E Evento`/free.
   Two position markers: `Supervivientes (Casilla <position>)` and `Monstruo (Casilla <last>)`.
6. **Tarjeta de Evento de Caza Activo** — header `auto_stories` `Evento de Caza #<n>`,
   `Tirada d10` (design example; see correction 2 above), title `Bruma Olorosa y Viento Fétido`,
   description `El hedor metálico a carne descompuesta empapa las túnicas. En la penumbra, una
   densa neblina borra las huellas de la bestia.`; **Tabla de Resultados** with three ranges kept
   verbatim: `1 - 3` `Pánico en la Ceguera` `1 de daño al Cerebro`, `4 - 7` `Huellas Frescas`
   `+1 de Supervivencia`, `8 - 10` `Viento a Favor (Ventaja)` `+1 PE de Caza`; **Control de Tirada**
   `Resultado d10` / `Sin tirar aún` + `casino` `Tirar Dado`.
7. **Botones de Acción de la Fase de Caza** — `arrow_forward`
   `Avanzar Supervivientes (+1 Casilla)`, `directions_walk` `Mover Bestia`, `swords`
   `Enfrentamiento` (routes to the Showdown tab).

## Specs

- **S1** — `HuntScreen` renders the design's seven regions in order, over `state.hunt`
  (`HuntState`), reusing the existing callbacks (`onSetUpHunt`, `onAssignPlayer`,
  `onMarkSpaceKind`, `onAssignHuntEvent`, `onResolveHuntEvent`, `onAdvanceHunt`,
  `onFinishHunt`). Setup (no `spaces`) still offers the quarry/level/players form first.
- **S2** — The board renders **13** spaces from `HuntState.spaces` (header `Casillas 0 - 12`),
  with the index/icon/label per `HuntSpaceKind`, plus the `Supervivientes`/`Monstruo` position
  markers. The design's 9-space strip is corrected to 13 (correction 1).
- **S3** — **`MoveMonster`** is modelled: a usecase moves the monster's space toward/along the
  board (`directions_walk Mover Bestia`), and reaching the party's space resolves as **ambush**
  per the manual (`06_…` §2). TDD RED→GREEN; no usecase existed before.
- **S4** — The hunt-event card renders the event's title/description/ranges verbatim; `Tirar Dado`
  is present but its **d100** table resolution is deferred (out of scope, backlog) — the control
  renders in the design's disabled/`Sin tirar aún` state until that slice lands.
- **S5** — Prep: extract the shared sheet primitives (`SheetPanel`, `CounterRow`, `PipTrack`,
  `StoneButton`, `ChipRow`, `AttributeStat`, `Symbol`) from `SurvivorSheet.kt` into
  `ui/components/` so V2/V3 reuse them (V2 uses `ChipRow`/`StoneButton`/`SheetPanel`).
- **S6** — Tests: `MoveMonster` usecase test (move + ambush edge cases) RED→GREEN; a views/model
  test pinning the 13-space board frame; full suite + both Python suites green.
- **S7** — Visual gate: emulator captures of the rebuilt screen (board with 13 spaces, quarry
  plinth, party chips, event card, action buttons) **approved by the maintainer before any
  push/PR** (standing rule).

## Non-goals (recorded gaps, follow-up slices)

- d100 hunt-event table (100 events) — **backlog**.
- Showdown/Enfrentamiento mechanics — **V3**.
- The design's example `Tirada d10` literal — rendered as the design shows it, but the resolution
  path is the deferred d100 slice (S4).
- Per-monster board diagrams (`E` placement) stay manual (`MarkSpaceKind`) — the diagram is an
  image, not machine-readable (already the case in `SetUpHunt`).
