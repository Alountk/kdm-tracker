# Feature: Hunt board correctness + per-quarry setup + hunt-event draw (V2 polish)

Fix the Hunt Phase defects the maintainer reported on the rebuilt `Caza` screen: the board must
be configured to the monster, fixed spaces (Overwhelming Darkness / Starvation) must render with
their own marks instead of the party marker / `Guarida`, the space marks must match the Stitch
design (gold special `E`, beast paw + name below, filled star `Grupo`), and advancing must draw a
random hunt event card with its resolved history. Source of truth is the Stitch HTML
`.superpowers/design/caza/caza.html` (never screenshots) and the manual `external-assets/kdm_parte1.md`.

Design: Stitch `4632e56b39c5470188dbe0b087122767`, cached `.superpowers/design/caza/caza.html`.

## Specs

- **S1 — Board configured to the monster.** `SetUpHunt` seeds each of the base-game quarries
  (White Lion, Screaming Antelope, Phoenix) with its own layout: the fixed **Overwhelming
  Darkness** space, the fixed **Starvation** space, the quarry's special-`E` positions, the Start
  space at index 0 and the monster lair at index 12 — instead of 13 generic spaces the player has
  to mark by hand. Layout is hardcoded in code (the per-monster diagrams are images). Manual base
  layout `external-assets/kdm_parte1.md` §9.1: `0=Inicio`, `6=Oscuridad Abrumadora`,
  `12=Hambruna`. `MarkSpaceKind` still works for manual adjustment.
- **S2 — Fixed-space labels are never painted over.** `HuntBoardFrame.cellLabel` /
  `materialIcon` evaluate the fixed space kinds (`OVERWHELMING_DARKNESS`, `STARVATION`, `MONSTER`,
  `START`) **before** the party-position branch, so a space never shows `Grupo` when it is a
  fixed space. Verbatim labels: `Asent.`, `Oscuridad Abrumadora`, `Hambruna`, `Guarida`,
  `Evento`, `Grupo`.
- **S3 — Fixed-space icons are bundled.** `OVERWHELMING_DARKNESS` and `STARVATION` map to
  Material Symbols that exist in `KdmSymbols.CODEPOINTS`, so `glyph()` never throws when those
  spaces are rendered. `KdmSymbolsTest` pins the new codepoints (`glyphCount` 88 → the new total).
- **S4 — Space marks match the design.** Normal event space = grey letter `E` (`Evento`); quarry
  special event space (`MONSTER_E`) = **gold** medallion `E`/glyph (`Evento`); the beast's space
  (`MONSTER`) = **paw** (`pets`) in a blood-red circle with the **quarry name below it**, wrapping
  to two centered lines for long names without breaking the tile; the party's space (`index ==
  position`) = **filled** gold circle with a star (`flare`, FILL 1) labelled `Grupo`.
- **S5 — Advancing draws a hunt event.** On `AdvanceHunt`, one random card is drawn from the
  real per-quarry deck (`ContentCatalog.huntEventsFor(quarry)`): shown as the active event card;
  the player resolves it by hand (consequences applied manually) and marks it resolved. Already
  resolved events remain visible when navigating backward through the board (they are recorded
  in `HuntSpace.resolved`). Rolling is optional — no automatic d10. The drawn card lives in the
  app's transient `UiState`, **not** in the serialized `HuntState` (no sync/transfer schema
  change, no migration). *(Transferred to `HuntState.drawnEvent` by L8 — the maintainer chose
  "Persistente (HuntState)"; serialization stays backward-compatible via a defaulted field.)*
- **S6 — Per-quarry board layout from `25_tableros_de_caza_de_presa`.** `SetUpHunt` seeds the
  thirteen spaces from the canonical hunt-track table (`external-assets/25_tableros_de_caza_de_presas_le_n_blanco_f_nix_y_ant_lope_bramador.md`), which numbers spaces **1-13**.
  **Index conversion (critical):** the code array is **0-12**, so `codeIndex = mdSpaceNumber − 1`;
  every `E`/monster/lair index below is already converted to the 0-based code index:
  index `0` = `START` (Inicio, md 1), index `6` = `OVERWHELMING_DARKNESS` (md 7), index `12` =
  `STARVATION` (md 13); every other index's role and the beast's starting index depend on quarry
  **and level**:
  - *Monster event (`E`) spaces* — `León Blanco`: md 3,4,6,9,10 → index 2,3,5,8,9;
    `Phoenix`: md 3,5,8,10 → index 2,4,7,9; `Screaming Antelope`: md 3,4,8,10 → index 2,3,7,9
    → `HuntSpaceKind.MONSTER_E`.
  - *Monster starting space by level* (`monsterPosition`) — `White Lion` L1/L2/L3 = md 5/8/12 →
    index **4/7/11**; `Phoenix` L1/L2/L3 = md 6/9/12 → index **5/8/11**; `Screaming Antelope`
    L1/L2/L3 = md 5/9/11 → index **4/8/10**.
  Remaining intermediate indices are ordinary event spaces (`NORMAL`). `MarkSpaceKind` still
  works for manual adjustment. Layout is code-hardcoded (the per-monster diagrams are images).
- **S7 — The hunt cannot advance over an unresolved event.** `AdvanceHunt` is blocked while the
  active drawn card (`HuntState.drawnEvent`) is not yet in the party's current space's
  `resolved` list; the UI disables the `Avanzar Supervivientes (+1 Casilla)` button (with an
  inline hint) until the player marks it resolved with `Marcar resuelta`. Navigating backward
  through the board stays always allowed (it only reads history, never modifies it).
- **S8 — The beast can be moved one space back.** A manual `Mover Bestia atrás` action moves
  `HuntState.monsterPosition` one space *away* from the party (never past index 0 / the Start
  space), for the hunt events that say the monster retreats (e.g. `Asustadizo`: "el Antílope
  Bramador se mueve 1 espacio alejándose de los supervivientes"). Distinct from `MoveMonster`,
  which only steps the beast *toward* the party.
- **S9 — The hunt-event card renders like the design's "Flujo de Casillas" card.** `HuntEventCard`
  follows the canonical Stitch card (`.superpowers/design/caza/caza.html` event card + the
  `Tablero de Caza — Flujo de Casillas (KDM)` screen): header (`Evento de Caza #NN` + a
  `Tirada d10`/`1d10` chip), the card **title** in the headline font, the rules text rendered
  **in italic** (the design quotes it), and — when the text carries a d10 result table —
  **separate result blocks per range** (`1-3`, `4-7`, `8-10`, …), each with the design's colors:
  danger/red for damage lines, gold/secondary for advantage lines, primary otherwise. Ranges are
  parsed from the card's `text` (the decks store plain rules text, not structured ranges).

## Tasks

- **T1 (S2, S3, S4)** — Fix `HuntBoardFrame` ordering + bundled OD/Starvation icons + design marks
  (gold special `E`, paw + name, filled star `Grupo`). TDD RED→GREEN in `HuntBoardFrameTest` /
  `KdmSymbolsTest`. Route: parent inline (small path). Commit: `fix(hunt): …`
- **T2 (S1)** — Per-quarry `SetUpHunt` layout (hardcoded base layouts) + tests. Route: parent
  inline. Commit: `feat(hunt): …`
- **T3 (S5)** — Draw a random hunt event on `AdvanceHunt`, render card + resolved history, transient
  draw in `UiState`. TDD RED→GREEN. Route: parent inline. Commit: `feat(hunt): …`
- **T4 (S6)** — Re-seed `SetUpHunt` from `25_tableros_de_caza_de_presa` (index = md − 1): per-quarry
  `MONSTER_E` spaces + per-quarry-per-level `monsterPosition`. TDD in `HuntBoardUseCaseTest`.
  Route: parent inline. Commit: `feat(hunt): …`
- **T5 (S7)** — Gate `AdvanceHunt` on the unresolved drawn card (domain `StepResult.Blocked`) +
  disable/hint the advance button in `HuntScreen`. TDD RED→GREEN. Route: parent inline.
  Commit: `fix(hunt): …`
- **T6 (S8)** — Add a manual beast-retreat usecase (`monsterPosition` away from party, floored at 0)
  + a `Mover Bestia atrás` control. TDD RED→GREEN. Route: parent inline. Commit: `feat(hunt): …`
- **T7 (S9)** — Rebuild `HuntEventCard` to the design's "Flujo de Casillas" card: italic rules text,
  parsed d10 result blocks with the design's colors. Route: parent inline. Commit: `feat(hunt): …`

## Log

- **L1** — Verbatim (maintainer, hunt-phase report): (1) "el tablero de caza debe estar configurado
  a nivel de monstruo. ya que tienes todas las confrontaciones en el md que te adjunte"; (2) "No
  esta marcado Oscuridad Abrumadora y la de Hambruna aparece como si fuera Guarida"; (3) design: "cuando
  el grupo pasa adelante el icono central pasa a ser un circulo con una estrella, luego pasa a poner
  Grupo, en lugar de Evento"; (4) "los eventos normales con E y los del leon con una E dorada";
  (5) "la casilla de la bestia con una zarpa en el centro y el nombre debajo"; (6) "Puedes hablar con
  Stitch para que te cree el flow de esta parte?"; (7) on advance "roba una carta de evento de caza
  al azar", "mostrar los eventos ya resueltos al retroceder", "todo su efecto... sera por mano",
  "la tirada o no es indistinto". "Recuerda siempre utilizar el html de stitch y nunca screenshots."
- **L2** — Maintainer design decisions (ask_user_question): event model = **show card + history,
  resolve by hand, no automatic d10**; board setup = **per-quarry layout hardcoded in code** for
  the three base-game quarries.
- **L3** — Stitch consultation attempted (`generate_screen_from_text` ×3, `generate_variants` ×1);
  unavailable: the Stitch MCP server hard-times-out at 60s and generation exceeds it, so no screen
  is persisted. Reported unavailable; the cached Stitch HTML remains the canonical source.
- **L4** — Maintainer approved the proposed scope/order (PR-1 marks, PR-2 setup, PR-3 draw): “dale”.
- **L5** — T1 delivered on `feat/hunt-board-marks` (commit `43fd1cd`): fixed the paint order
  (`HuntBoardFrame` resolves fixed kinds before the party branch), bundled `wb_twilight`/`restaurant`
  (regenerated the TTF subset 88→90 with `tools/fonts/subset_material_symbols.py --build`), added a
  FILL-1 `Symbol` for the solid party star / beast paw, and accented the beast (blood) / special
  event (gold). Full gradle gate + both Python suites green; emulator captures confirm `Asent.`,
  `Grupo` (filled star), beast `pets` + `White Lion`, grey `E`. Risk: item 3 (shared icon
  vocabulary) — verified by `KdmSymbolsFontTest` + `fonts --check`.
- **L6** — Maintainer decision (ask_user_question, T2): **"Layout completo verificable + base"** —
  hardcode the layout that has a source (`Inicio@0`, normal, `Oscuridad Abrumadora@6`,
  `Hambruna@12` from manual `kdm_parte1.md` §9.1 / reglamento p.61) plus the White Lion's
  special-`E` positions `3` and `6` (from the canonical Stitch `caza.html`); leave the Screaming
  Antelope's and Phoenix's special-`E` positions as a TODO (they exist only in the rulebook's
  per-monster *images*, no extractable text) — do **not** invent them.
- **L7** — T2 delivered on `feat/hunt-board-marks` (commit `604a49a`): `SetUpHunt` now seeds the
  base layout (`Inicio@0`, `Oscuridad Abrumadora@6`, `Hambruna@12`, rest normal) per manual §9.1;
  the beast's home is `monsterPosition` (no fixed `MONSTER` space) and `HuntBoardFrame` resolves
  mark-by-position (beast paw / party star) before fixed kind so a fixed space is never `Grupo`.
  **Correction vs L6**: the canonical Stitch `caza.html` track is a *9-space illustration*
  (`Casillas 0 - 8`), so its "casilla 3 / 6" special-E notes are NOT indices on the real 13-space
  board — no per-quarry special-E positions were hardcoded for any quarry (all three keep
  `MarkSpaceKind` for those); the base OD/Starvation layout applies to all three. Full gradle gate
  + both Python suites green; emulator shows `Casillas 0 - 12`, `Hambruna` at 12 with `restaurant`,
  `Monstruo (Casilla 12)`. Caveat: adb cannot horizontally scroll the track (Compose
  `horizontalScroll` over clickable cells), so the `Oscuridad Abrumadora` cell at index 6 was
  verified by test, not by capture — capture partial.
- **L8** — T3 delivered on `feat/hunt-board-marks` (commit `c410e82`): `AdvanceHunt.run(record,
  deck, random)` deals a random card from `content.huntEventsFor(quarry)` into the new
  `HuntState.drawnEvent: HuntEvent? = null` (added with a default → persisted `SettlementState`
  JSON decodes old blobs unchanged, no migration); `HuntEventCard` renders the drawn card's real
  name+rules text, a `Marcar resuelta` button (assigns + resolves on the selected space), and that
  space's resolved history (visible when navigating backward); the static mock + dead
  `ResultRange` removed. Consequences applied by hand, no auto d10 (per L2). Full gradle gate +
  both Python suites green; emulator: `Avanza el grupo para robar...` -> advance draws
  `Evento Aleatorio` -> `Marcar resuelta` -> `Resuelta` + `HISTORIAL DE LA CASILLA 2`.
- **L9** — **Slice boundary**: the feature ran 3 work-unit commits on `feat/hunt-board-marks`
  (`43fd1cd` marks / `604a49a` layout / `c410e82` draw), totalling 607 changed lines — past the
  ~400 line guide. Each commit is one coherent reviewable unit (fix / setup / draw); delivery
  strategy: chain them as separate PRs or ship behind one PR with `— size:exception`. Decision
  pending with the maintainer.
- **L10** — Emulator re-verification after stack delivery: S1-S5 all green (layout `Casillas 0 - 12`,
  marks `Asent.`/`Grupo`/beast `White Lion`, draw `Evento Aleatorio` -> `Suelos Arañados` ->
  `Cachorro de León Blanco`, history per space). Found + fixed: "Marcar resuelta" wrote to the
  *selected* (navigable) space, so the same card could land on multiple spaces' histories; anchored
  it to the party's current space (`partySpace`/`hunt.position`) while history/header keep following
  the viewed space (commit `03854d0`, test `resolvedEventStaysOnItsOwnSpaceAcrossAdvance`).
- **L11** — Maintainer re-review findings (verbatim intent): (a) special Lion `E` spaces aren't
  marked correctly; (b) must not let the party advance while the hunt event isn't resolved — only
  let them go **back** to view what happened, never to modify; (c) verify space placement against
  the pasted md per encounter/monster level; (d) the `Evento de Caza #NN` card must match the
  design's "Flujo de Casillas": title, description in *italic*, and colored blocks for d10 rolls;
  (e) asks how a monster that must step **back** one space is controlled. Decisions
  (ask_user_question): placement = **new md `25_tableros_de_caza_de_presa`** (canonical; Inicio@0,
  OD@6, Hambruna@12); advance = **block until resolved**; monster = **add manual retreat**; card =
  **full redesign with result blocks** (Stitch generation still unavailable, so reuse the cached
  `caza.html` + `Flujo de Casillas` design as the source).
- **L12** — New canonical source `external-assets/25_tableros_de_caza_de_presas_le_n_blanco_f_nix_y_ant_lope_bramador.md`
  (untracked until this feature lands) gives the per-quarry per-level board: the md numbers spaces
  **1-13**; the app array is **0-12**, so **codeIndex = md − 1**. S6 stores the converted indices.
  Also confirms a hunt card that makes the beast retreat exists (`Asustadizo`), motivating S8.
- **L13** — T4-T7 delivered on `feat/hunt-event-draw-pr3` (suite green: gradle + 106 content-ingest +
  7 fonts + 91 codepoints): **T4/S6** `SetUpHunt` re-seeded from md25 (`MONSTER_E` spaces + beast
  home per quarry/level, index = md − 1; `MoveMonsterTest` updated to the new beast home); **T5/S7**
  `AdvanceHunt` blocks while the drawn card is unresolved on the party's space and `HuntScreen`
  disables the advance button with "Resuelve el evento para avanzar"; **T6/S8** new `RetreatMonster`
  usecase (steps away from the party, floored at 0) + a "Mover Bestia Atrás" control — this needed a
  new bundled `arrow_back` glyph (0xE5C4): the first emulator run crashed with
  `unmapped Material Symbol: arrow_back`, so the TTF subset was rebuilt (90 → **91** codepoints,
  `KdmSymbolsTest`/`KdmSymbolsFontTest`/`test_subset…` pins updated) — a gap the JVM symbol tests
  didn't catch because they only pin declared glyphs, not every `Symbol(...)` call site; **T7/S9**
  new `HuntEventCardModel.parse` splits the plain rules text into an italic intro + one toned block
  per d10 range (danger/advantage/neutral), rendered by `HuntEventCard`.
- **L14** — Emulator verification of T5 (S7) green: advancing drew `EVENTO DE CAZA #5` and the button
  flipped to the disabled `RESUELVE EL EVENTO PARA AVANZAR`; resolving re-enables it. S9's italic
  description verified on real cards ("León en Celo", "León Merodeador"). Still to capture for the
  REGLA DUDA gate: the S6 board on a **fresh** setup (the persisted `EmptyNew` hunt predates T4, so
  it shows the old layout / `Monstruo (Casilla 12)`), a card **with** a d10 table to show the colored
  blocks, and the S8 retreat control. `external-assets/` is gitignored (md25 stays a local reference).
- **L15** — T8 complete: full suite gate green (gradle + 106 content-ingest + 7 fonts + 91
  codepoints) and all emulator captures for the S6-S9 expansion done on a **fresh** setup
  (`pm clear` + new settlement + `Set up the hunt`): **S6** board shows `Inicio@0` (gold), gold
  `E` spaces at indices 2 and 3 (White Lion md 3,4 → 2,3), the beast paw on `Monstruo (Casilla 4)`
  (White Lion L1 home, md casilla 5 → 4), `Supervivientes (Casilla 0)`, `DISTANCIA 4`, and the
  stock `AVANZAR` enabled with no card; **S7** `RESUELVE EL EVENTO PARA AVANZAR` while a card is
  pending → enabled `AVANZAR SUPERVIVIENTES` after `Marcar resuelta`; **S8** `MOVER BESTIA ATRÁS`
  button present; **S9** italic rules text on real cards plus a colored range block (`1 - 2` chip
  with its outcome) on `El Bebé y la Espada`. A `Monstruo (Casilla 11)` reading on an earlier
  hunt was stale pre-T4 state, disproven by the fresh L1 → 4 capture.
- **L16** — Maintainer: "los colores no coinciden y los iconos tampoco. coge los diseños de stitch
  1:1 recuerda usar siempre el html de stitch en vez de screenshots." Rebuilt board + card from
  the Stitch HTML (`/tmp/stitch_hunt/{estados,flujo}.html` + `caza.html`), commit `bb14216`:
  - **Glyphs 1:1** — `foundation` (Inicio), `groups` (Grupo), `cruelty_free` (bestia),
    `bedtime` (Oscuridad), `no_meals` (Hambruna), `visibility` (evento especial); 4 new bundled
    codepoints → **95**. Codepoints from Google's official `.codepoints` (the TTF cmap has no
    name→cp map).
  - **Space caption + tone** — new `SpaceTone` on `HuntSpaceView` + `markSubLabel`/`markTone`:
    `Inicio/Tablero/Ojo/Activo/En Acecho/Divisoria/Desgaste` captions and per-tone cell border
    (violet Darkness, amber Starvation, gold special/party, red beast, outline normal) + tone
    medallion fill; `HuntSpaceCell` consumes tone for border/medallion/label/lozenge.
  - **Card 1:1** — intro renders as a quoted italic block with a left rule; each d10 range block
    gets a tone-filled chip (`bloodContainer`/`goldContainer`/`surfaceHighest`) + 3px left accent
    (`RangeTone.style()` → `RangeStyle`). New palette: violet/amber families.
  - Verified: gradle + 106 content-ingest + 7 fonts + `--check` (95) green; emulator captures
    `/tmp/s9_board_full.png` (00 foundation/Inicio … 04 cruelty_free/En Acecho) and
    `/tmp/s9_card_range.png` ("Suelos Arañados" italic quote + red/neutral/gold range chips).
