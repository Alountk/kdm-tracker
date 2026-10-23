# Feature: hunt steps 1:1 + per-step 2d10 roll

The hunt board's per-space **steps** are rebuilt 1:1 from the Stitch `estados de casilla` screen
(`<HEADLESS_STATE>0de8e1e38fc34375a2a1270b0961252b</HEADLESS_STATE>` sibling `422a5591bbe840dcb9db37b7f4f65531`, file
`.superpowers/design` cached `/tmp/stitch_hunt/estados.html`), and the caza phase gains a per-step
**2d10 roll** that is stored on the step so it stays visible when the party moves back.

## Specs

**S1 — Step top row: number flush top-left + per-type top-right mark (touching card edges).**
The top row sits **flush against the card's top edge** (design `p-2` + `w-full justify-between`,
no extra gap) with the **space number at the top-left** (zero-padded `00`…`12`, shown on **every**
card per `estados.html`). At the **top-right**, a per-type mark — and the design has **no stray
space** above the number/icon (the whole row touches the card border):
- `flag` — Inicio/Asentamiento (00)
- **`animate-ping` dot** — the **party's active step ONLY** (6px dot, `bg` of the party colour,
animated small→large then gone, looping; it stays **inside the step**, never overflowing — its
max size barely exceeds the other icons)
- `star` — a **quarry special event (E.…) step** (top-right of the *board step*, gold `star` 11px)
- `swords` — the beast / confrontation step (badge follows the beast even when it sits on a fixed
  space; the beast position wins over the fixed kind)
- `nights_stay` — Overwhelming Darkness (06)
- `warning` — Starvation (08)
- `skull` — the final / Guarida step (12)
- **no top-right mark** — an ordinary event step (the design's plain `E` steps have only the
  number; `GEN` appears only where the design draws it)
  (the `star` on the beast-event **card** top-right, per L4, is a separate surface and stays.)

**S2 — All cards one uniform size; center medallion round and centered.**
Every step card is the **same fixed size** (design `w-[84px] h-[134px]`; the few `86`/`88` variants
are only the wider beast/OD slots in the design and must not produce uneven-looking cards — the app
equals them all to one uniform size). Every center mark is a **round circle with the icon centered
in its middle**: `foundation` (Inicio), italic serif `E` (generic event), **`visibility` (the
quarry-special "eye", in the middle — the eye is correct there and is NOT the top-right badge)**,
`cruelty_free` (beast), `bedtime` (Overwhelming Darkness), `no_meals` (Starvation), `skull` (final).

**S3 — Party ping dot replaces the badge; party and beast halos; party tone green.**
The **party's active step** shows, at the **top-right in place of its type badge**, the **small
circle that switches on/off and grows small→large then disappears, forever** (design
`animate-ping`), sized so it **does not leave the step**. The party's active step and the beast's
step each also render a **halo/aura** of the step's own colour (`pulse-lantern` / `pulse-beast`).
Beast tone **red**; **party tone green (emerald)** (never collides with gold event cards).

**S4 — No lozenge; footer shows the event type.**
The rotated **rombo/lozenge marker is removed**. At the **bottom of every card** sits the **event
type**:
- `Evento` — normal event steps
- `Hambruna` — starvation step
- `E.<Animal>` — a quarry special event step (`E. León`, `E. Fénix`, `E. Antílope`…)
- `Oscuridad Abrumadora` — overwhelming darkness step
- `León Blanco` / `Fénix` / `Antílope Bramador` — the beast step (the quarry name)

**S5 — Caza phase at the start step.**
When the party is on the **start step**, the caza-phase panel shows only **"Inicia la cacería"**
in place of the current hunt-event card, plus the **`AVANZAR SUPERVIVIENTES`** action. No hunt
event is drawn on the start step.

**S6 — A 2d10 roll per step, app-rolled and player-editable.**
On each advance that lands the party on a step, the app **rolls 2d10** and **stores the result on
that step**. The player can **edit** the stored roll (if they rolled at the table instead). The
stored roll is **per-step and persists**, so moving **back** to a step shows **that step's own
roll**, never the same one. (`roll_edit` — app roll by default, editable.)

**S7 — Source of truth is the Stitch HTML, never screenshots.**
All of the above is taken from the Stitch HTML (`/tmp/stitch_hunt/estados.html` and
`.superpowers/design/caza/caza.html`), never from screenshots.

## Tasks

| ID | Spec | Route | Commit |
|----|------|-------|--------|
| T1 | S1–S4 | app-ui render + frame model (badge/number/medallion/footer/halo, no lozenge, green party, beast eye) | (uncommitted) |
| T2 | S5 | core + app-ui (start-step "Inicia la cacería", no draw on start) | (uncommitted) |
| T3 | S6 | core domain + usecase + data (per-step 2d10 roll, persisted, editable) + app-ui | (uncommitted) |
| T4 | S1–S6 | tests (frame pins, model pins, roll persistence/edit) RED→GREEN + suite gate | (uncommitted) |
| T5 | S1–S6 | emulator capture per region (REGLA DUDA: maintainer approval) | — (approved, see L6) |
| T6 | S7 / L5a | event-space config from md section 2 (0-12 `E` sets + beast homes) | (uncommitted) |
| T7 | L5b–f | visual fixes: party ping replaces badge, flush top row, uniform card size, no badge on normal steps, star on E steps, eye only in centre | (uncommitted) |

## Log

- **L1** — User (verbatim): "el tablero se ve mucho mejor, pero en las steps de los asentamientos
  aun queda un poco, en las cards de inicio y del final no tiene sentido que tengan numero. Las
  cards del diseño tienen el numero arriba a la izquierda y arriba a la izquierda hay un GEN cuando
  el evento es generico, una estrella cuando es de un evento de criatura, unas espadas cuando es un
  enfrentamiento, una exclamacion cuando es hambruna y cuando es la activa que es cuando es el
  grupo sale un punto con una animacion de mas pequeño a grande y luego desaparece. El activo
  además sale como con una aureola y el de la bestia tambien, del color de la card rojo para la
  card de la bestia y para el grupo en el diseño es de color dorado pero para que no se mezcle con
  las cards de evento de bestias podemos cambiarlo a tonalidades de verde. los iconos del centro de
  los steps son todos redondos con los iconos centrados en su centro, y en los eventos de bestia
  hay un ojo y no una E. Y el rombo de las cards ya no son necesarias, luego en todos los cards
  abajo del todo tienen que poner el tipo de evento. Evento para los eventos normales, Hambruna,
  E.{Animal que sea}, Oscuridad Abrumadora, Leon Blanco / Fenix / Antilope bramador. En la parte de
  evento de caza es perfecto pero falla el funcionamiento: cuando estas en el step de inicio en la
  parte de Evento de Caza solo debería algo como Inicia la caceria y la parte de avanzar
  supervivientes, y en cada step una vez cae se debe de hacer una tirada de dos dados de 10,
  tenemos dos opciones o la tirada es manual o la hacemos desde aqui. Si la hace el jugador tendra
  que apuntarla para que salga en pantalla la asociada a la step porque lo que queremos es que si
  vuelve hacia atras pueda verla. y asi se va asociando y no se ve la misma que es lo que pasa
  ahora."
- **L2** — User decisions: roll = `roll_edit` (app rolls 2d10 on landing, editable, persisted per
  step so it's visible on back-move); party tone = **green (emerald)**; number = **none on Inicio
  and the final step**.
- **L3** — User correction (verbatim): "rectifica el 3- el diseño bueno es el de estados de casilla
  y en el diseño final no hay ninguna star. las casillas de enfrentamiento contra la bestia sale
  la card con el numero de casilla arriba a la izquierda en el otro lado unas espadas en medio un
  circulo con un conejito en el centro y debajo el nombre del animal al cazar." → beast step =
  number top-left, `swords` top-right, round center with `cruelty_free` (rabbit/eye), quarry name
  below.
- **L4** — User clarification (verbatim): "tienes razon creia que te referias a otra star. LA star
  solo aparece en las cartas de evento de bestia arriaba a la derecha" → the `star` mark is **only
  on the beast-event card, top-right**; board steps use their own badges (`GEN`/`swords`/`flag`…),
  so the `estados de casilla` HTML (which shows `star` on quarry-special steps) is not contradicted
  by L3 — the two refer to different surfaces.
- **T1 status** — DONE (uncommitted): S1–S4 implemented in `HuntBoardFrame.kt` + `HuntBoard.kt`;
  +3 codepoints (flag/nights_stay/star → 98, subset rebuilt); frame + symbols tests GREEN. Party
  tone GREEN, `E. <Animal>`/`Ojo <Animal>` footers, round medallion, ping halos, lozenge removed.
- **T2 status** — DONE (uncommitted): S5 in `HuntEventCard` — `atStart = hunt.position == 0` renders
  "Inicia la cacería" and no event card on the start step. Core already drew nothing on start
  (SetUpHunt leaves `drawnEvent = null`, covered by an existing GREEN test).
- **T3 status** — DONE (uncommitted): S6 `roll_edit` — `HuntSpace.roll: Int?`; `AdvanceHunt` rolls 2d10
  on the landed step **only if empty** (maintainer decision: conserve, never re-roll a visited step);
  new `EditHuntRoll` usecase (range 2..20); JSON round-trip persistence test GREEN; UI `StepRoll`
  (shows the step's roll + editable AlertDialog); cable Controller→ViewModel→KdmTrackerApp→HuntScreen.
- **T4 status** — DONE: full gate GREEN — gradle (spotlessApply + core/data/app/server tests +
  spotlessCheck) BUILD SUCCESSFUL; content-ingest 106 OK; fonts 7 OK; subset `--check` 98 codepoints.
  Counts bumped 95→98 in `KdmSymbolsTest` (2 asserts) and `tools/fonts/tests` (2 asserts).
- **L5** — User correction (verbatim): "la configuracion de los eventos de los animales esta mal,
  como comente la parte de la configuracion comparativa de las 13 casillas del archivo
  25_tableros...md ponia de 1 al 13 y nosotros los controlamos de 0 a 12 por estan mal. ahora ya
  los he modificado en en el md, cambialos. en las cards las redondas que parpaddean solo debe
  salir en la card del paso en la que estan el grupo y sustituye el icono de arriba a la derecha,
  ahora hay como un espacio raro arriba de todas las cards cuando el numero y el icono tiene que
  estar tocando los bordes, aparte hay cards de diferente tamaño y todas tienen que ser del mismo
  tamaño. las cards de los eventos normales no tienen icono arriba a la derecha y la de eventos de
  los animales tienen una estrella y no un ojo como ahora, el ojo solo deberia estar en medio (eso
  si que es correcto de la card). En la step donde esta el grupo el icono de arriba a la derecha es
  el circulo pequeño que se apaga y se enciende (realmente se apaga y se enciende y se hace de
  pequeño a grande y desaparece y asi todo el rato, y el tamaño maximo no supera mucho de los
  iconos, no debe salir de la step)." → (a) fix the per-animal event-space config against the
  maintainer-updated `25_tableros...md` (they renumbered to 0-12); (b) party ping dot replaces the
  badge on the party step only; (c) top row flush to the card edges (no stray top gap); (d) all
cards one uniform size; (e) ordinary event steps have **no** top-right mark, quarry-special steps
  show **`star`** (not the eye — the eye stays only in the center); (f) the ping dot grows
  small→large and vanishes in a loop, bounded inside the step. → (a) **VERIFIED, no code change**:
  the maintainer-chosen source of truth is the **section-2 comparative table (base 0-12)**; the app's
  `SetUpHunt.MONSTER_EVENT_INDICES` + `BEAST_HOME_BY_LEVEL` and their mirrors in
  `HuntBoardUseCaseTest` **already match it exactly** (WL E={2,3,5,8,9}/4·7·11, Phoenix E={2,4,7,9}/
  5·8·11, Antelope E={2,3,7,9}/4·8·10). The md's section-3 text is still 1-13 (not renumbered),
  which is the source of the apparent mismatch — the code was already 0-12 and correct.
- **T6 update (md section 2 + 3 renumbered to 0-12)** — the maintainer edited `25_tableros` so
  sections 1, 2 and 3 are all 0-based. Re-extracting the `E` sets from section 2 (table) gives a
  **REAL change**: the app's `MONSTER_EVENT_INDICES` were written against the old 1-13 numbering
  (offset −1). Corrected `SetUpHunt.MONSTER_EVENT_INDICES` to the md's 0-based `E` sets:
  White Lion **{1,2,5,8,9}** (was {2,3,5,8,9}), Phoenix **{1,4,7,9}** (was {2,4,7,9}),
  Screaming Antelope **{1,2,7,9}** (was {2,3,7,9}). Beast homes (4/7/11, 5/8/11, 4/8/10) were
  already correct. Updated the two doc-comments that still said "1-13 → 0-12", the board-seed
  test (`setUpSeedsFixedSpacesAndTheQuarrysMonsterEventSpaces` eSpaces map + doc), and the
  `spaces[1] == NORMAL` assertion (index 1 is now `MONSTER_E` for White Lion). RED→GREEN; full
  gate + Python suites GREEN.
- **T7 status** — DONE (uncommitted): L5(b-f) visual fixes in `HuntBoardFrame.kt` + `HuntBoard.kt`:
  (b) `partyPing` field + `PartyPingDot` composable — the party's step swaps its type badge for the
  animated ping dot (grows small→large then fades, bounded inside the step); only the party's step
  has it. (c) top row flush to the card's top edge (tight `vertical=4` pad, `TopCenter`, row as the
  first child). (d) all cards one uniform size (`width 72.dp` + `height 132.dp`, never
  content-driven). (e) ordinary event steps have **no** top-right mark (`markBadge` → `""`, was
  `GEN`); quarry-special steps show **`star`** (was `visibility`). (f) the eye (`visibility`) stays
  only in the medallion centre. Removed the outer `pingDrawBehind` rings that overflowed the step.
  Frame tests RED→GREEN (12/12, incl. `onlyThePartysStepCarriesThePingDot`). Emulator captures
  verified: 00 flag / 01 no-badge / 02·03·05·08 star+centre-eye / 04 swords / 06 party ping+green /
  07 no-badge / uniform cards / flush top row. Full gate GREEN (gradle + content-ingest 106 +
  fonts 7 + subset 98).
- **L6** — T5 approval (maintainer): "dale" — REGLA DUDA approval of the T7 post-fix emulator
  captures (board regions 00-08, party ping dot animation, roll/edit panel). Approved for commit
  + PR. Build `hunt-steps-and-roll`: T1-T7 complete, full suite gate GREEN.

## Delivery (sliced commits on feat/hunt-event-draw-pr3)

- Slice boundary recorded after commit 8 (8ca8014): the branch carries 8 commits vs origin/main
  past the ~400-line single-slice guide; the work is delivered as three reviewable units:
  (1) core roll S6 `8ca8014`, (2) event-space config T6, (3) app-ui render + roll wiring T1/T2/T7.
  Each unit keeps tests/docs with its behavior; the feature doc lands on `main` with the final unit.
