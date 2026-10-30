# Roadmap — the remaining Stitch design work

One page: what the Stitch designs still owe the app, in the order to build it. Updated as
slices land. `docs/backlog.md` keeps the small deferred items; this file keeps the program.

## Where we are (2026-10-30)

The design program is "apply the Stitch design to the screens", in two parts plus a screen
program. All of Part A is merged.

| Program | Scope | State |
|---|---|---|
| **Part A** — the Develop step's three flows | Fabricar (#34), Innovar + catálogo (#35), Añadir recursos (#36) | ✅ done |
| **Part B** — content identity (4 slices) | S1 resources (#31), S2 locations/gear/innovations (#32) | ✅ S1–S2 done; **S3, S4 open** |
| **Screens 1:1** — rebuild each designed screen | Asentamiento (#33 + Part A, step bodies #58–#59), Supervivientes (#37), Caza (#38–#41, #57), Enfrentamiento (#61–#62) | ✅ all four screens done |
| Foundation | Atramentum theme (slice 4), app shell (slice 5) | ✅ done |

Design sources (Stitch project `4909212909494507782`):

| Screen | Stitch id | Cached render |
|---|---|---|
| Ficha de Superviviente | `310b1d1ff8994d619e67fda164f35f36` | reskin spec, slice 4 |
| Fase de Asentamiento | `59fdd89d6100424eb9b5bbf1a6e74ffa` | `.superpowers/design/innovar-flow/` |
| Fase de Caza | `4632e56b39c5470188dbe0b087122767` | — |
| Enfrentamiento: León Blanco | `cd089a6938264afea5bd710ff04e3716` | — |

## Track 1 — the screens rebuilt 1:1 ✅ done

The app-shell spec (slice 5) declared the order: Asentamiento → Supervivientes → Caza →
Enfrentamiento. All four are done.

**Scope of "Asentamiento done":** the settlement-screen spec states the design covers the
summary, the step frame and **one step of eleven (7 · Desarrollo)** — that is exactly what is
rebuilt 1:1 (`2026-10-10-settlement-screen-design.md`, "The design covers the summary and one
step of eleven"). Steps 1–6 and 8–11 originally had **no Stitch design at all**: they are
functional implementations of our own text, kept as they are. The three Part A flows (Fabricar,
Innovar, Añadir recursos) are actions inside step 7.

**The remaining settlement steps (1–6, 8–11) are done** (#58–#59): their bodies were rebuilt from
the Stitch designs the maintainer later supplied; what the designs draw without a model behind it
is in `docs/backlog.md`. Rules that carried through the whole track: build from the **Stitch HTML**,
never screenshots; visual gate (the maintainer approves rendered emulator output before any
commit/push/PR); ~400-line PR budget; full suite gate.

### V1 — Supervivientes: the paged survivor sheet tab ✅ done (#37)

Merged as squash `01b4274`. The tab now routes `Screen.Survivors` to a paged `SurvivorSheet`:
the pager bar (one cycling prev button `(idx + 1) % len`, name + gender chip `H/M/O`, `Equipo`)
over the sheet with its header suppressed, the empty state, and a local `SurvivorEditorDialog`
host. The last `PendientePlaceholder()` in the shell's four tabs is gone. Gate approved on the
emulator (seed + newly created settlement) before merge; full suite green.

- The sheet's **9 recorded gaps** (4 regions: Edad 1–4/Retiro, Ruleta de Muerte, Armaduras
  rotas, Acto del Superviviente; 5 elements: narrative subtitle, `+0` modifiers, "Derribado",
  gear affinities, Hunt-XP milestone glyphs) need domain modelling — **follow-up slices, not V1**.
- Deferred to `docs/backlog.md`: the role subtitle (`Prólogo • Superviviente Fundador`, no
  domain field) and the on-device empty-state capture (0-survivor roster unreachable without a
  domain change; pinned by JVM tests).

### V2 — Caza: rebuild 1:1 ✅ *(done — PRs #38–#41 merged)*

`HuntScreen` (199 lines) is a functional tracker whose layout predates the design: it keeps
the theme but not the design's structure. Rebuild against Stitch `4632e56b…`: the hunt board,
its spaces and events, the party with per-player assignment, Overwhelming Darkness.

- Prep: extract the survivor-sheet primitives (`SheetPanel`, `StoneButton`, `CounterRow`,
  `PipTrack`, `ChipRow`, …) from `SurvivorSheet.kt` into a shared `ui/components/` layer first —
  V2/V3 need the same primitives plus new ones, and today they live inline.

**Follow-up merged 2026-10-23 — per-space steps 1:1 + the 2d10 roll** (PRs #42–#44):
rebuilt the board's per-space steps against the Stitch `estados de casilla` HTML (flush top row,
per-type mark, uniform cards, round centre medallion, event-type footer, party ping dot, `star`
on quarry-special steps, eye only in the centre), the start step's "Inicia la cacería", and a
per-step 2d10 roll (`roll_edit`, rolled on land, persisted, editable). Also re-seeded the
event-space config from the renumbered md (0-12: WL {1,2,5,8,9}, PHX {1,4,7,9},
ANT {1,2,7,9}). Feature doc: `odd/tasks/hunt-steps-and-roll.md` (T1–T7, S1–S7, L1–L6).

### V3 — Enfrentamiento: rebuild 1:1 ✅ done (#61–#62)

`ShowdownScreen` is rebuilt against Stitch `cd089a69…`: the encounter banner with the AI deck as
the monster's health, the drawn AI card with its Move & Attack numbers, the hit locations (a
miss, a wound or a critical wound resolves one out of the list) and the outcome. `HealMonster`
backs the `−` of the wound track. What the stored encounter cannot back (monster stats, round and
turn, controller, hit-location deck state, instinct and terrain) is listed in `docs/backlog.md`.

## Track 2 — content identity, remaining slices

From `docs/superpowers/specs/2026-10-10-content-identity-design.md`:

- **B3 — the real resource catalog** ✅ done. All 56 generated resources already carry an id and
  their materials; the summary-grid debt is paid: the summary and step 10 resolve each cell
  through `ResourceMaterial` (`basicId`, label, colour) instead of a hardcoded Spanish name map.
  **Still open:** counting a cell by material (a Garra de León adding to Hueso) needs a
  «spend by material» rule first; see `docs/backlog.md`.
- **B4 — the language selector**: `Language{ES,EN}` runtime selector (Spanish default,
  incremental English), per the i18n decision. No selector exists in the UI today.

## Track 3 — backlog and quality

From `docs/backlog.md` — no screen work, but they close review debt:

1. **Innovar colours** — decide board fidelity vs `KdmPalette` folding (pinned by `KdmThemeTest`).
2. **Visual regression tests** for the Innovaciones styles (only validated by hand at gates today).
3. **R3 findings** — `FilterChipLabelMatch` (match on enum, not display label) + a UI test for
   the catalogue filter state.
4. **Two visual minors** (Innovar surface, each with its own emulator gate): `costChip` mixes
   effort with resource initials; the steal row's alignment drifts from the board.

## Rules that carry across every slice

- **Visual gate**: no visual change is committed, pushed or PR'd until the maintainer approves
  the rendered emulator output.
- **PR budget**: 400 lines; over it, the squash title carries `— size:exception`.
- **Suite gate**: `./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest
  :server:test spotlessCheck` + `python3 -m unittest discover -s tools/content-ingest/tests`
  + `python3 -m unittest discover -s tools/fonts/tests`.
- Worktrees under `/Volumes/Mac_Nvme/Dev/10_26_kdm-tracker-worktrees/`; specs and plans live in
  `docs/superpowers/{specs,plans}/`.
