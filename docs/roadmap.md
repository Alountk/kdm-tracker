# Roadmap — the remaining Stitch design work

One page: what the Stitch designs still owe the app, in the order to build it. Updated as
slices land. `docs/backlog.md` keeps the small deferred items; this file keeps the program.

## Where we are (2026-10-16)

The design program is "apply the Stitch design to the screens", in two parts plus a screen
program. All of Part A is merged.

| Program | Scope | State |
|---|---|---|
| **Part A** — the Develop step's three flows | Fabricar (#34), Innovar + catálogo (#35), Añadir recursos (#36) | ✅ done |
| **Part B** — content identity (4 slices) | S1 resources (#31), S2 locations/gear/innovations (#32) | ✅ S1–S2 done; **S3, S4 open** |
| **Screens 1:1** — rebuild each designed screen | Asentamiento (#33 + Part A), Supervivientes (#37), Caza (#38–#41), Enfrentamiento | ✅ Asentamiento's designed part, Supervivientes & Caza done; **1 open** |
| Foundation | Atramentum theme (slice 4), app shell (slice 5) | ✅ done |

Design sources (Stitch project `4909212909494507782`):

| Screen | Stitch id | Cached render |
|---|---|---|
| Ficha de Superviviente | `310b1d1ff8994d619e67fda164f35f36` | reskin spec, slice 4 |
| Fase de Asentamiento | `59fdd89d6100424eb9b5bbf1a6e74ffa` | `.superpowers/design/innovar-flow/` |
| Fase de Caza | `4632e56b39c5470188dbe0b087122767` | — |
| Enfrentamiento: León Blanco | `cd089a6938264afea5bd710ff04e3716` | — |

## Track 1 — the three screens still to rebuild 1:1

The app-shell spec (slice 5) declared the order: Asentamiento → Supervivientes → Caza →
Enfrentamiento. Asentamiento's designed part is done; the rest follow.

**Scope of "Asentamiento done":** the settlement-screen spec states the design covers the
summary, the step frame and **one step of eleven (7 · Desarrollo)** — that is exactly what is
rebuilt 1:1 (`2026-10-10-settlement-screen-design.md`, "The design covers the summary and one
step of eleven"). Steps 1–6 and 8–11 have **no Stitch design at all**: they are functional
implementations of our own text, kept as they are, and are therefore not pending design work.
The three Part A flows (Fabricar, Innovar, Añadir recursos) are actions inside step 7.

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

### V3 — Enfrentamiento: rebuild 1:1

`ShowdownScreen` (93 lines) is the thinnest screen in the app. Rebuild against Stitch
`cd089a69…`: AI deck as the monster's health, the drawn AI card, hit locations with
Fallo/Herida/Crítico, reactions, terrain, the wound tracker.

## Track 2 — content identity, remaining slices

From `docs/superpowers/specs/2026-10-10-content-identity-design.md`:

- **B3 — the real resource catalog**: all 56 resources with ids and materials through the CSV,
  plus variants. Also pays the **summary-grid debt**: `SettlementSummaryModel` still resolves
  the four basic resources through a hardcoded Spanish `DESIGN` map
  (`SettlementSummaryModel.kt:45`) — delete it and resolve through content identity +
  `ResourceMaterial` instead (this was T5 of S1, never done).
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
