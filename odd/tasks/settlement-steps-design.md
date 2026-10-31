# Feature: the remaining settlement steps' design (1:1)

The eleven-step settlement's step **bodies** (steps 1-6 and 8-11; step 7 already done) are rebuilt
1:1 from the four new Stitch screens. The frame (`StepFrame.kt`), the summary
(`SettlementSummary.kt`) and the carousel already render on every step and are **not** restyled —
only the content below the frame changes. Design authority is the **Stitch HTML, never screenshots**.

## Specs

**S1 — Sources and scope rule.** Build from the cached HTML only:
`.superpowers/design/asentamiento/{pasos_1_2,pasos_3_6,paso_8,pasos_9_11}.html` (Stitch project
`4909212909494507782`, screens `dcd74c1b…`, `f373f44e…`, `280c3d69…`, `727eb088…`). Each design
screen = page chrome the app already owns (top bar, carousel, **bottom CTA + everything below it =
`StepFrameFooter`**, hidden toast) + section(s) with a header row (numbered box, `Paso N`, title,
right-side chips/stats) + content. **Mapping rule:** the section header row's *title/badge/icon* is
the frame's job (the frame already renders `Paso N. <título>` + the catalog summary); the header
row's *right-side chips and stats* become the body's first row ("meta row"); the design's
*section-opening paragraph/quote* is the frame's text slot and must NOT be duplicated in the body —
instead the **catalog summary of steps 3, 8 and 10 is replaced with the design's verbatim copy**:

- step 3: `Cada superviviente que sobrevive y regresa de la cacería genera 1 esfuerzo (*). Se extinguen al concluir esta fase si no son consumidos.`
- step 8: `Selecciona una presa de la Lista de Presas y cuatro supervivientes con el valor para adentrarse en la oscuridad infinita.`
- step 10: `Guarda cualquier recurso no invertido y equipo sin asignar en el almacén del asentamiento para futuras innovaciones o cacerías.`

**S2 — Step 1 enters the cockpit (1:1).** Step 1 stops rendering outside the cockpit
(`SetUpStepScreen` early-return in `SettlementScreen.kt`); it renders like the other ten steps:
summary + frame (`Paso 1. Preparación`) + body + footer (the frame's advance replaces `Confirm
Setup`). The old read-only sections (`Principles` / `Innovation Deck` / `Built Locations` / `This
Year`) disappear — their content is shown by steps 4, 6 and 7. Body = meta row (right chip
`REQUERIDO`, `bg-surface-container-high`) + the design's codex card (`menu_book` + `Registro del
Códice` label + the italic quote `"Tras abatir al León Blanco, los supervivientes deambulan por la
oscuridad perpetua, atraídos por un suave resplandor que brilla cálido en el horizonte."` — year-1
narrative, rendered inert) + the instruction card (`Sitúa el **tablero de asentamiento** en el área
de juego, junto a las cuadrículas de equipo y las hojas de los 4 supervivientes que vencieron en el
prólogo.`) + the checkbox row, drawn **checked and disabled** exactly as designed:
`Hoja de Asentamiento inicializada` / `Bautizado formalmente como {state.settlementName}` (the
settlement name derives from state, not the design's `El Cúmulo`).

**S3 — Step 2 body.** Meta row (right chip `EN CURSO`). Then, verbatim design copy:
(1) *Bono de Regreso* module: `add_circle` + label, `Cada superviviente que regresa gana **+1 Supervivencia**.`,
right stat `Límite Actual` = `state.survivalLimit`, italic note `Aviso reglamentario: No se puede
superar el Límite de Supervivencia actual de la hoja de asentamiento ({survivalLimit}).`, and the
button `Aplicar +1 Supervivencia a los que Regresan` drawn **disabled** (unmodeled → backlog).
(2) *Curación Automática* module: `healing` + label, `Todas las heridas leves y graves se curan de
forma inmediata. Se borran todas las marcas en su registro de impacto.`, the error box
`Regla Inflexible` / `Las **lesiones permanentes NO se curan** jamás en este paso. Se mantienen
registradas.`, and the button `Confirmar Curación de Heridas` **enabled → `onHeal`** (the modeled
`ReturnSurvivors` action; replaces the English `Heal & clear tokens`).
(3) The survivor stack: header `Supervivientes Vencedores ({living count})` / `Listos para el
Asentamiento`, then one design card per **living** survivor: initial-letter avatar, name, a status
line **derived from wounds** (`Sin lesiones permanentes` + `check_circle` when no light/heavy wound
is set; otherwise `Heridas leves {n} · graves {m}` — no invented flavor copy), `Supervivencia
{survival}/{state.survivalLimit}`, and the heart button drawn **disabled** (no wound-tag model →
backlog). The current filter chips and expandable `SurvivorCard` list are removed (survivor editing
remains reachable in the Supervivientes tab); `state.actionError` is already surfaced by the
cockpit and is not repeated here.

**S4 — Step 3 body (new; today it renders `PendientePlaceholder`).** Meta row: `Auto-calculado`
chip + right stat `Reserva Activa {state.endeavors} / {state.endeavorBreakdown.total}`. Then the
token tray card: header `Supervivientes que regresan ({n})` + `+{n} ESFUERZOS` where **n = the
living-survivor count** (the same basis `GainEndeavors` funds); a token per living survivor
(`flare` filled + first name), all tokens drawn **disabled**; hint `Toca una ficha para alternar su
consumo`; the `remove`/`add` endeavor buttons drawn **disabled** (unmodeled manual adjustment →
backlog). Everything else in this design screen's step-3 section is header/paragraph (frame) or
footer-zone (CTA) per S1.

**S5 — Step 4 body.** Meta row: pill `Año {state.lanternYear}`. Then the timeline card: year row
(`{lanternYear}` badge + `Año Linterna {lanternYear}` + `Marcado` with a filled `check_box` icon);
the story-event block (`menu_book` + `Evento de Historia Activo`, title `Primer Día (First Day)` +
`Los supervivientes contemplan el brillo y descubren la palabra hablada.`, right chip
`Obligatorio`, and the three checklist rows `Añadir innovación: **Lenguaje**`, `Añadir localización:
**Cúmulo de Linternas**`, `Crear mazo de Innovaciones del Asentamiento` — each drawn **checked and
disabled**, year-1 inert copy, the checks are not toggleable); the next-year row
(`{lanternYear+1}` badge + `Próximo año: Aparición de Carroñeros` + `lock`). Only the two year
numbers derive from state; the story copy is the design's inert year-1 copy (same treatment slice 6
gave the year narrative). **Removed per L2:** the `Draw a settlement event` button, the
`SettlementEventCard`, the settlement-event deck list and the `This Year` section (their usecases
stay in `:core`, unreachable from this screen → backlog).

**S6 — Step 5 body.** Meta row: pill `Total: {state.deaths}`. Then the deaths row card: title
`Bajas en la Primera Historia`, subtitle `Ningún superviviente pereció ante el León Blanco.`
**rendered only while `deaths == 0`** (no designed copy exists for a nonzero state — omit rather
than invent), and the `remove` / `add` buttons: `add` **enabled → `onRecordDeaths(1)`** (the usecase
refuses `count < 1`, so `remove` is drawn **disabled** → backlog as "undo a recorded death");
then the italic note `Marca una casilla vacía por cada caído en combate o por eventos de historia
durante la partida.` **Removed per L2:** the births counter (`RecordBirths` unreachable → backlog;
its New-Life milestone can then never trigger) and the step-5 `PrincipleCard` (the same choice is
offered by step 6's milestone cards).

**S7 — Step 6 body.** Meta row: chip `{active} Activos` where **active = reached && !resolved**.
Then one card per `state.milestoneViews`: icon (`bookmark_border`, except `family_restroom` for
the Society/New-Life milestone when its id matches the design's two named milestones), title =
`milestone.description` **except the two milestones the design names, which use the design's own
Spanish titles** (amended by L5: `PRINCIPLE_DEATH` → `Principio: Muerte`, `PRINCIPLE_SOCIETY` →
`Principio: Sociedad`; every other milestone keeps its existing English description — undesigned
content keeps its text; language selection is Track 2/B4), a status chip **derived from state** (`Pendiente` / `Alcanzada` /
`Resuelta` — the design's per-milestone condition chips like `Pendiente (0 Bajas)` /
`Población ≥ 15` have no model behind them, so state-derived status replaces them; recorded as a
deliberate derivation), the design's flavor paragraph **only for the two milestones the design
names** (Death: `Se activa automáticamente cuando se registre la primera muerte en el asentamiento.
El grupo deberá elegir una filosofía funeraria.`; Society: `Se activará cuando el primer niño nazca
en el asentamiento.` — omitted for milestones the design does not cover), and the existing
interactive content **inside the card when the milestone is reached and unresolved**: the
`PrincipleChoice` buttons + `Resolve` (the design mock only shows the unreached state; these
functions are not in L2's removal list and must stay reachable). The design's closing codex quote
(`"Después de derrotar al León Blanco…"` + `Códice de Supervivencia • Prólogo`) renders at the end
of this body (it sits between the last section and the CTA on its screen).

**S8 — Step 8 body.** No meta chips (the design's title row has only the frame's icon slot).
Content: (1) `Presa Asignada` label + the `swap_horiz` button `Canteras ({n})` **disabled**
(quarry re-selection is not modeled at this step → backlog) + the quarry card: **packaged photo**
(asset from L4) with the `star` `Nivel 1` chip (inert), kicker `Bestia Blanca`, name
`León Blanco`, subtitle `Prólogo / Presa Estándar` — **all three inert design constants**
(amended by L5: the assigned quarry is chosen at hunt setup and is not modeled before it;
`state.quarries.first()` is arbitrary — it rendered `Fénix` under the `Bestia Blanca` kicker — so
the card no longer derives its name) — and the stat boxes
`Vida / IA 8` / `Dureza 8` (**inert design values** — no monster-stat model exists → backlog).
The `Canteras ({n})` button count stays derived from `state.quarries.size`.
(2) `Grupo de Caza ({limit} Supervivientes)` + `{departing}/{limit} Asignados` where
**limit = `population.coerceAtMost(4)`** (the current rule), then one card per departing survivor:
`person` avatar, name, `Supervivencia: {s}` · `Locura: {i}`, the `sync_alt` button **enabled →
opens a swap picker over living non-departing (non-dead) survivors and reports the new list
through `onDepartingChange`** (this is the design's own selection control — it replaces the
removed roster), the `grid_view` button **enabled → expands the 3×3 gear grid inline wired to
`onAssignGear`/`onUnassignGear`**, and the gear strip (`Equipo Portado:` + `Cuadrícula
{gear.size}/9` + one chip per assigned gear item, uniform `secondary` dot — per-item dot colors have
no model). An empty-slot card (`Sin asignar` + enabled `sync_alt`) is a **derived empty state** so
a fresh settlement can reach the design's `4/4` state. The role chips (`Fundadora`/`Fundador`) are
**omitted** — role has no domain field (existing backlog item from V1). (3) The armor callout
(`shield` + `Ajuste de Armaduras y Equipo` + the paragraph + the two `check_box` chips `Cintura +1
(Tela)` / `Puntos de Supervivencia` — inert design copy). (4) The campaign-rule error box
(`Regla Oficial de Campaña` + `"Sáltate el paso 9 (Enfrentamiento Especial) en la primera fase de
asentamiento."` + `Reglamento KDM pág. 35` — inert). **Removed per L2:** `RosterFilterChips`, the
full `SurvivorCard` list with `Depart`/`Departing` buttons and its editor host (gear assignment
survives via `grid_view`; survivor editing survives in the Supervivientes tab).

**S9 — Step 9 body.** Meta row: chip `Nemesis / Evento` (`error`-toned). The body renders the
design's year-1 exemption card **only during the first settlement phase, implemented as
`state.lanternYear <= 2`** (amended by L5: `AdvanceTimeline` increments the year when leaving
step 4 and `LanternYearFlow.canEnterStep` forbids forward skips, so at step 9 the first
settlement phase always runs under year 2 and the original `lanternYear == 1` gate was
unreachable): left `error` bar,
`verified_user` + `Regla de Año Linterna 1`, headline `Sáltate el Paso 9`, chip `Automático`, the
paragraph `Durante la primera fase de asentamiento, los supervivientes aún no conocen las bestias
némesis. Este paso queda ignorado por orden del códice.`, and the inner block (`menu_book` +
`Efecto en Años Posteriores` + `Tras resolver un combate especial, regresa de inmediato al Paso 5.
Cura a estos combatientes y retira fichas temporales. No computan como supervivientes que
regresan.`) — all inert (slice 6 already declared this year-rule unbacked). For `lanternYear > 2`
the body renders only the meta row: the designs give no later-year content and the showdown UI was
removed per L2. **Removed per L2:** the `Special Showdown this year` switch and the whole
`EncounterSetup`/`EncounterPanel` branch (`SetSpecialShowdown` & friends unreachable from this
screen → backlog); the now-unused step-9 panel composables are deleted if nothing else references
them.

**S10 — Step 10 body.** Meta row: chip `Cúmulo`. Then the resource card (its design paragraph is
the catalog summary per S1, so the card starts at the grid): the **2-column grid of the four basic
resources** — icon box (`radiology` Hueso, `vital_signs` Órgano, `texture` Pellejo, `hardware`
Chatarra), uppercase label, sub-label `Básico` (Chatarra: `Raro` — design copy verbatim), count
from `state.storageLines`, and the `remove`/`add` buttons **enabled → the existing resource adjust
callback (`ResourceEntry(id, ∓1)`, same semantics as the summary's adjust, clamped at 0 by the
existing model)**. Then `Equipo Almacenado`: header (`shield` + label) + the `add` button
`Añadir Ítem` **disabled** (no item-add model → backlog) + one chip per distinct item in
`state.gear` with `×{count}` (uniform neutral style; per-item icons have no mapping → omitted).
**Removed per L2:** the read-only `Storage` list (its `resourceName` helper is kept if the grid
reuses it). **Accepted consequence:** storage lines beyond the four basics have no designed surface
here → not rendered (record in the backlog alongside the real-catalog slice B3).

**S11 — Step 11 body.** Meta row: chip `Cierre`. Then the closing card: the **packaged banner
photo** (asset from L4) with the overlay `Fin de la Fase de Asentamiento` (italic) + `hotel_class`;
the three checklist rows **checked and disabled** exactly as designed — `Limpieza de mesa
completada` / `Cartas de innovación recogidas y recursos no gastados asegurados.`, `Hojas de
registro actualizadas` / `Equipo adquirido registrado en las fichas de los supervivientes que
parten.`, `Grupo de Caza Designado` / `4 supervivientes listos con sus cuadrículas de equipo
ajustadas.`; and the campaign note (`bookmark` + `Los jugadores han concluido la **Primera
Historia**. La campaña avanza hacia las tinieblas de la estepa de rostros.` — inert design copy).
**Removed per L2:** the unspent-endeavors discard warning and the `Summary` card (the count stays
visible in the footer's `Restaurar Esfuerzos (N)`; population/deaths live in the top bar).

**S12 — Icon subset + fidelity rules.** Every glyph a body renders must exist in
`KdmSymbols.CODEPOINTS` (a missing name crashes at runtime). The four designs use ~27 names absent
today (`menu_book`, `healing`, `cleaning_services`, `radiology`, `texture`, `family_restroom`,
`bookmark_border`, `verified_user`, `swap_horiz`, `sensor_door`, `dry_cleaning`, `lens`,
`hotel_class`, …). Derive the final set from the implemented `Symbol(...)` calls, look each name up
in the source TTF cmap (`/tmp/MaterialSymbolsOutlined_FILL_GRAD_opsz_wght.ttf` via the
`/tmp/fsub` venv), **substitute and record** any name the font lacks (precedent:
`drive_file_stream` → `handyman`), then rebuild the subset and bump **all four count assertions**
(`KdmSymbolsTest` ×2, `tools/fonts/tests` ×2) and pass `--check`. Where the design sets
`FILL 1` on a glyph, render it filled (existing `FontVariation.Setting("FILL", 1f)` precedent).
Design copy in `font-rule-quote` (Playfair italic) renders italic on the theme's body style — the
Playfair family is **not** bundled (recorded deviation). Koch rule: **the Stitch HTML is the only
source; screenshots are never a source.**

**S13 — Backlog entries (L3).** Every control drawn disabled and every function removed by L2
gets a `docs/backlog.md` entry: `+1 Supervivencia` core rule; manual endeavor toggle/±; undo a
recorded death; step-11 checklists; `Añadir Ítem`; quarry swap at step 8; monster Vida/Dureza stats;
settlement-event draw/record; births counter (and its New-Life milestone); special-showdown
declaration + encounter UI; non-basic storage surface; heart wound-tag toggle; hero backdrop photos
(out of scope per S1 — packaged URLs recorded there, if the maintainer wants them later).

**S14 — Gates.** Full suite
(`./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck`
+ both Python suites + `subset_material_symbols.py --check`) before any capture. **REGLA DUDA:**
nothing is committed, pushed or PR'd until the maintainer approves per-step emulator captures.
Work stays uncommitted on `feat/settlement-steps-design` until approval, then lands as ~400-line
sliced commits/PRs (chained, retargeted in order).

## Tasks

| ID | Spec | Route | Commit |
|----|------|-------|--------|
| T1 | S1, S12 | prereqs: catalog summaries 3/8/10 + icon subset rebuild + package 2 photos | — |
| T2 | S2 | app-ui: step 1 into cockpit, new body, remove early-return | — |
| T3 | S3 | app-ui: step 2 body + model (status line) + tests | — |
| T4 | S4 | app-ui: step 3 body (new file) + model + tests | — |
| T5 | S5 | app-ui: step 4 body + model + tests (event UI removed) | — |
| T6 | S6 | app-ui: step 5 body + model + tests (births/principle removed) | — |
| T7 | S7 | app-ui: step 6 body + model + tests | — |
| T8 | S8 | app-ui: step 8 body + model (slots/swap/gear) + tests (roster removed) | — |
| T9 | S9 | app-ui: step 9 body + model (year gate) + tests (showdown UI removed) | — |
| T10 | S10 | app-ui: step 10 body + model (grid/chips) + tests (adjust wired) | — |
| T11 | S11 | app-ui: step 11 body + tests | — |
| T12 | S1–S11 | integration: prune now-unused callbacks/params, full suite gate | — |
| T13 | S14 | emulator captures per step → REGLA DUDA approval | — |
| T14 | S13 | `docs/backlog.md` entries | — |
| T15 | S14 | sliced commits + chained PRs after approval; roadmap update | — |

## Status (work log, pre-approval)

**T1–T12 DONE, uncommitted** on `feat/settlement-steps-design` (worktree).
- T1: summaries 3/8/10 verbatim (test-first); subset 98 → **111** codepoints (13 glyphs, no substitutions,
  `bookmark_border` aliased via legacy cp E867); photos in `res/drawable-nodpi/` — parent cropped both
  (baked caption text + editor UI rails removed): quarry 500×229, banner 460×251.
- T2–T11: all ten bodies rebuilt 1:1 (batches A–D); models `SurvivorsReturnModel`, `StepBodies3To6Model`,
  `DepartingSlotsModel`, `StepBodies9To11Model` all RED→GREEN; `SettlementStepBodiesTest` 16 copy pins.
- T12: pruned 9 unused `SettlementScreen` params (+`KdmTrackerApp` call site); dead roster code deleted by
  parent (`SurvivorComponents.kt`, `SurvivorRoster.kt`, `SurvivorRosterTest.kt` — orphaned by S8/S3);
  `PendientePlaceholder` kept (else-branch). Full gate GREEN: core 485 / data 53 / app 353 / server 126 /
  spotless clean / content-ingest 106 / fonts 7 / subset 111.
- Noted dead-but-alive elsewhere: `EncounterSetup`/`EncounterPanel` stay (used by `ShowdownScreen`);
  ViewModel methods `recordSettlementEvent`/`drawSettlementEvent`/`recordBirths`/`declareSpecialShowdown`
  now unwired (kept — controller tests exercise them).

**Next: T13 emulator captures (REGLA DUDA) → T14 backlog → T15 sliced commits/PRs after approval.**

**T13 captures done + fixes round 1 (L5) applied.** Full pass over steps 1–11 (`/tmp/t13/*.png`);
four defects found and fixed test-first (S9 gate, S8 quarry constants, S10 truncation, S7 titles);
re-captured the four affected steps. Full suite re-run GREEN after fixes (gradle 4s incremental +
content-ingest OK + fonts OK + subset 111). **Awaiting maintainer approval of the captures.**

## Log

- **L1** — User (verbatim): "El contexto está en docs/roadmap.md (Track 1 "Next session target") y
  odd/tasks/hunt-steps-and-roll.md (Status)."
- **L2** — User decisions (batch questionnaire): (a) step 1 → **"Dentro del cockpit (1:1)"**
  (entered after a clarification: the old screen sits outside summary/frame with its own
  `Confirm Setup`; the design shows it like every other step — answer: *"Dentro del cockpit (1:1)"*),
  read-only sections disappear; (b) functions the new drawings do not show (step-4 settlement-event
  draw/record, step-5 births counter, step-8 roster selection, step-9 special-showdown switch) →
  **"1:1 estricto, quitarlas"** — removed even where that leaves actions unreachable; (c) drawn
  actions with no model (`+1 Supervivencia`, endeavor tokens/±, step-11 checklists, `Añadir Ítem`)
  → **"Dibujadas, deshabilitadas, pero agregadas al backlog para implementarlas"**; (d) imagery →
  **"Empaquetar las fotos"**.
- **L3** — Evidence recorded during exploration: `RecordDeaths` refuses `count < 1` (so the design's
  `−` death button cannot be modeled without a new usecase → disabled + backlog); `MarkDeparting`
  is the only path to `DEPARTING` and `AssignPlayer` requires it (so step 8's `sync_alt` picker —
  the design's own control — must be wired, not disabled, or the whole hunt loop breaks);
  `MonsterCard` carries no Vida/Dureza fields (step-8 stat boxes are inert design values);
  `ReturnSurvivors` exists and is already wired to `onHeal` (step 2's confirm button is enabled,
  not backlog); step 3 renders `PendientePlaceholder` today; `WoundState` has only
  `armor/light/heavy` (step-2 status line derives from light/heavy only).
- **L4** — Imagery scope note: the two hero backdrop photos (`pasos_1_2`, `pasos_3_6` headers) sit
  **above the carousel = page chrome, outside this feature's "bodies below the frame" scope** per
  the roadmap handoff; only the photos the bodies render are packaged (step-8 quarry card, step-11
  banner). Hero URLs stay recorded in S13's backlog entry for a follow-up slice if requested.
- **L5** — T13 capture findings (first full emulator pass over steps 1-11) → four amendments: (i)
  **S9 gate was unreachable**: `AdvanceTimeline` increments the year at step 4→5 (captures show
  year 2 from step 5 on) and `LanternYearFlow.canEnterStep` forbids forward skips, so
  `lanternYear == 1` can never hold at step 9 → first settlement phase is now `lanternYear <= 2`;
  (ii) **S8 quarry card**: `state.quarries.first()` is arbitrary (rendered `Fénix` under the fixed
  `Bestia Blanca` kicker + `Prólogo / Presa Estándar`) and no assigned-quarry model exists before
  hunt setup → kicker/name/subtitle became inert design constants; only `Canteras ({n})` derives;
  (iii) **S10 grid truncated labels** (`HUE…`, `Bási…`) at 1080 px → cells compacted so the four
  labels + `Básico`/`Raro` never ellipsize; (iv) **S7 titles**: the design's two named milestones
  showed English core descriptions → those two cards now use the design's Spanish titles.
