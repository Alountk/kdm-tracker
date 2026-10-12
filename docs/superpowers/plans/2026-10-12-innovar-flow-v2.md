# Innovar Flow (v2) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the Innovaciones section as a gated Innovar flow — access, resources, milestone grid, draw, choose one of two, confirm — with the design consumed 1:1 from Stitch's HTML.

**Architecture:** `core` gains one piece of state (`Innovation.unlockedInYear`) that `Innovate` stamps when it unlocks. `InnovationsModel` derives a gate plus the five visual states from one `UiState` frame. `app-android` renders them as stacked sections, outer gate first, each section landing behind its own visual gate.

**Tech Stack:** Kotlin Multiplatform (`core`, `data`), Compose Multiplatform / Android (`app-android`), SQLDelight (`data`), Spring Boot (`server`), Gradle, JUnit.

**Spec:** `docs/superpowers/specs/2026-10-12-innovar-flow-design.md`, amended by the maintainer's flow (this plan's "Flow authority" below).

## Global Constraints

- Spanish UI copy; English code, identifiers, comments, commit messages. Conventional commits. **No AI attribution anywhere.**
- `./gradlew spotlessApply` before every commit. Suite gate: `./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck` + `python3 -m unittest discover -s tools/content-ingest/tests`.
- `TransferDocument.schemaVersion` stays **12**. `InnovationSerializer` must NOT carry `category` (content); it **must** carry `unlockedInYear` (state) as optional with default `0`.
- Design is consumed from Stitch `htmlCode` 1:1. 1 CSS px ≈ 1 dp. Material Symbols: pin `lineHeight = fontSize`.
- **Visual changes are never committed, pushed or PR'd until the maintainer approves the measured render.**
- Palette: surface `#131315`, surface-container `#201F21`, surface-container-low `#1B1B1D`, surface-container-high `#2A2A2C`, secondary gold, error, outline-variant. Fonts: Inter + Playfair Display.
- PR budget 400 lines → `— size:exception` (pre-authorised, confirm at push time).

## Flow authority

The maintainer's five points override the reference screens wherever they disagree:

1. **Access** — if no built location offers `ActionKind.INNOVATE`, only the no-access block.
2. **Resources** — if short, **only** the state-5 top block (error panel + 4 resource boxes + `Motivo:`). No cards, no draw button, no grid below it.
3. **Milestones** — the 2×2 grid is the passive display of what the other steps already granted: gold = already owned, green = unlocked this lantern year, dashed grey = empty.
4. **Draw** — an explicit action ("spend N to draw 2") consumes the cost and reveals the two cards. Selecting one reveals `ACEPTAR` below the cards.
5. **Confirm** — state 2 (consummated), then back to the catalogue with its filters.

Three deltas against the reference screens, deliberate: state 5 shows **no** cards (the reference shows them as inert previews); the draw button and `ACEPTAR` are **new** (the reference has neither); the cost is **minified** (`1` + `emergency` icon in gold + `1H 1Ó 1P`).

## Review Focus

- A document written before this change has no `unlockedInYear` → must decode to `0` and render **gold**, never green, never crash. *(Task 2)*
- `Innovate` short on one resource must leave the record **byte-identical** — no partial spend of the endeavor or of an earlier resource. *(Task 2)*
- State 5 must render **only** the error block: no grid, no cards, no draw button below it. *(Task 3)*
- Two cards with wildly different description lengths must render **equal height**. *(Task 6)*
- Select → `ACEPTAR` → confirm must unlock exactly one innovation and return the other to the deck; a second confirm in the same phase must be blocked. *(Task 7)*

---

### Task 1: The design, consumed from Stitch

**Files:**
- Create: `.superpowers/design/innovar-flow/*.html` (cached `htmlCode` per state)
- Create: `.superpowers/design/innovar-flow/DELTA.md` (what the HTML says vs. what we already have)

**Interfaces:**
- Produces: the authoritative HTML per state, referenced by every later task's implementation step.

- [ ] **Step 1:** Ask Stitch for the flow guide screen with the prompt already agreed (see below), project `projects/4909212909494507782`.
- [ ] **Step 2:** Download each state's `htmlCode` with the Python `urllib` path (curl cannot resolve these hostnames), cache it under `.superpowers/design/innovar-flow/`.
- [ ] **Step 3:** Write `DELTA.md`: every place the HTML differs from today's `DevelopPanels.kt`/`InnovationCardView.kt`, so the implementer knows what to change. Do not implement anything here.
- [ ] **Step 4:** Stop and present `DELTA.md` for approval.

The Stitch prompt (agreed with the maintainer):

> Design a mobile (390px, dark) **flow guide** screen in the same project and design system as the existing "Fase de Asentamiento" step-7 screen — reuse its exact tokens (surface #131315, surface-container #201F21, surface-container-high #2A2A2C, secondary/gold, error, outline-variant), Inter + Playfair Display, Material Symbols Outlined, and the existing Innovaciones section styling. Title: **"Innovaciones: Flujo de Innovar"**.
>
> Stack these six states as one vertical board, each in its own section labelled `ESTADO N`, separated by hairlines. Keep the Innovaciones header exactly as today: gold `auto_stories` 18px, `Innovaciones` in Playfair white, and a pill on the right with `MAZO: 14 CARTAS` (gold border, dark-amber fill #342800, gold text).
>
> **Common block — the milestone grid (2×2, `grid-cols-2 gap-2`, min-h 110px):**
> - `PRINCIPIO` — gold label, `verified` icon, `Nueva Vida`, `Acepta la oscuridad`, gold `ACTIVO PERMANENTE`. Gold border.
> - `RANURA 2` — green label + green border, `Pintura`, `Acción Esprintar activa`, green chip `NUEVA`, green button `DESBLOQUEADO` with `check`, open-lock icon.
> - `RANURA 3` — dashed outline, `hourglass_bottom`, `FUTURO`, `Vacío`.
> - `MAZO` — gold label, `flare`, `Cúmulo Linternas`, `Robar 2, elegir 1`, `13 Innovaciones`.
>
> **ESTADO 1 — SIN ACCESO:** only a compact block: lock icon, `Acción de Innovar no disponible`, muted reason line, and a disabled full-width button `INNOVAR NO DISPONIBLE`. No grid, no cards.
>
> **ESTADO 5 — SIN RECURSOS:** ONLY the top block, nothing below it. Red-bordered dark-red panel: `Coste de Innovación Insuficiente` + right pill `FALTAN RECURSOS`; then a 4-column row of small boxes `Esfuerzo 0/1`, `Hueso 1/1`, `Órgano 0/1`, `Pellejo 0/1`, each with `FALTA` (red) or `OK` (green) underneath; then `Motivo: No es posible seleccionar una innovación hasta tener disponibles en el asentamiento: 1 Esfuerzo + 1 Hueso + 1 Órgano + 1 Pellejo.`
>
> **ESTADO 3 — LISTO (antes del robo):** the gold/green grid, then a **draw action** row: label `Coste` on top; below it a rounded wrapper with a darker fill and a border in the grey that is currently the background, containing `1` and the `emergency` icon in gold, plus `1H 1Ó 1P`; and a full-width gold button `Gastar 1 Esfuerzo + 3 Recursos para robar 2 cartas` with `style` icon. Below that the deck row `Mazo de Innovación (14 Descubribles)` / `Catálogo` / `unfold_more`, and the wrapped filter chips `Supervivencia (4)`, `Creencia (3)`, `Arte (4)`, `Sociedad (3)`.
>
> **ESTADO 4 — ELIGIENDO:** the grid, then the offer header `Innovar: Elige 1 de 2 Cartas` in **white** with a deck icon (`style`) in gold, and the rule line `Regla oficial KDM: Roba 2 innovaciones, quédate 1 y devuelve la otra al mazo.` Then two cards side by side **of identical height (matched to the taller)**: category chip in a small rounded wrapper (gold text on a 20% gold wash) + a small icon on the right, then below the chip a **wrapper** holding the card text — title in Playfair 13px bold white, description in Inter **10px** muted — and a full-width `SELECCIONAR` button. One card is shown selected (gold button), one unselected (grey). **Below both cards, a full-width `ACEPTAR` button.**
>
> **ESTADO 2 — CONSUMADA:** the grid with `RANURA 2` green/`NUEVA`; then a resolution block `RESOLUCIÓN DEL ROBO DE CARTAS` with green `COMPLETADO`, the kept card `Lenguaje`, `Devuelta al fondo del mazo de innovación`, `1 CARTA DEVUELTA`; then a gold-bordered block `Innovación Realizada en este Año Linterna` with `El esfuerzo gastado y los 3 recursos (1 Hueso, 1 Órgano, 1 Pellejo) ya fueron deducidos del almacén.`
>
> Keep every card's corners, borders and spacing identical to the current Innovaciones section. Text is Spanish. Do not introduce new colours.

### Task 2: `unlockedInYear` on the innovation record

**Files:**
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/domain/Innovation.kt:38` (`Innovation`, `InnovationSerializer`)
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/usecase/Innovate.kt:88`
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/SettlementRepository.kt:347` (`STARTING_INNOVATIONS`)
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/domain/SettlementStateSerializationTest.kt`, `core/src/commonTest/kotlin/io/kdmtracker/core/usecase/InnovateUseCaseTest.kt`

**Interfaces:**
- Produces: `Innovation.unlockedInYear: Int = 0` — the lantern year the innovation entered the collection; `0` means "recorded before this field existed".
- Consumes: `SettlementRecord.settlement.lanternYear`.

- [ ] **Step 1: Write the failing test** — `SettlementStateSerializationTest`: an innovation round-trips `unlockedInYear = 3`; a document with **no** `unlockedInYear` key decodes to `0`.
- [ ] **Step 2: Run it** — `./gradlew :core:jvmTest --tests '*SettlementStateSerializationTest*'`. Expect FAIL (unknown property / missing field).
- [ ] **Step 3: Add `val unlockedInYear: Int = 0` to `Innovation` and emit/read it in `InnovationSerializer`, defaulting to 0 when the key is absent.** Keep `category` out of the serializer. `schemaVersion` stays 12.
- [ ] **Step 4: Write the failing test** — `InnovateUseCaseTest.aValidInnovateStampsTheUnlockedYear`: after a successful Innovate in lantern year 3, the added innovation has `unlockedInYear == 3`.
- [ ] **Step 5: Implement** — in `Innovate.run`, `innovations.with(innovation.copy(unlockedInYear = record.settlement.lanternYear))`. Give `STARTING_INNOVATIONS` year `1`.
- [ ] **Step 6: Run the suite** — full gate command above. Expect green.
- [ ] **Step 7: Commit** — `feat(core): record the lantern year an innovation was unlocked`

### Task 3 (Sección A — outside): the gate and the state-5 block

**Files:**
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/ui/settlement/InnovationsModel.kt`
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/ui/settlement/DevelopPanels.kt`
- Test: `app-android/src/test/kotlin/io/kdmtracker/app/DevelopPanelsTest.kt`

**Interfaces:**
- Produces: `enum class InnovarGate { NO_ACCESS, SHORT_ON_RESOURCES, READY }` and `InnovationsModel.gate: InnovarGate`, `InnovationsModel.missing: List<ResourceNeed>`, `InnovationsModel.motivo: String`.
- Consumes: `CostView`, `InnovationView.status`.

- [ ] **Step 1: Write the failing test** — `aSettlementShortOnResourcesShowsOnlyTheCostBlock` (gate `SHORT_ON_RESOURCES`, `hand` empty) and `aSettlementWithoutABuiltInnovateLocationShowsTheNoAccessBlock` (gate `NO_ACCESS`).
- [ ] **Step 2: Run it** — `./gradlew :app-android:testDebugUnitTest --tests '*DevelopPanelsTest*'`. Expect FAIL.
- [ ] **Step 3: Derive `gate` in `InnovationsModel.of`** — `NO_ACCESS` when no built location offers `ActionKind.INNOVATE`; else `SHORT_ON_RESOURCES` when `!cost.ok`; else `READY`.
- [ ] **Step 4: Render the state-5 top block only** — red-bordered panel `Coste de Innovación Insuficiente` + pill `FALTAN RECURSOS`, the four resource boxes (`Esfuerzo/Hueso/Órgano/Pellejo`) with `FALTA`/`OK`, and the `Motivo:` sentence. When `SHORT_ON_RESOURCES`, `InnovationsPanel` renders **this block and nothing else**.
- [ ] **Step 5: Run the suite.**
- [ ] **Step 6: Visual gate** — install, measure, present. **Do not commit until approved.**

### Task 4 (Sección B): the milestone grid, gold and green

**Files:**
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/ui/settlement/DevelopPanels.kt` (`SlotCard`, `slotCards`)
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/ui/settlement/InnovationsModel.kt`
- Test: `app-android/src/test/kotlin/io/kdmtracker/app/DevelopPanelsTest.kt`

**Interfaces:**
- Produces: `enum class SlotState { OWNED, NEW_THIS_YEAR, EMPTY }` and `SlotCard(state: SlotState, …)`.
- Consumes: `Innovation.unlockedInYear`, `InnovationsModel.lanternYear`.

- [ ] **Step 1: Write the failing test** — `anInnovationUnlockedThisYearIsNew` (`unlockedInYear == lanternYear` → `NEW_THIS_YEAR`) and `anOlderInnovationIsOwned` (`unlockedInYear < lanternYear` → `OWNED`).
- [ ] **Step 2: Run it** — expect FAIL.
- [ ] **Step 3: Derive `SlotState`** and colour the grid from the cached HTML: gold label/border/`ACTIVO PERMANENTE` for `OWNED`, green label/border/`NUEVA` + `DESBLOQUEADO` for `NEW_THIS_YEAR`, dashed grey for `EMPTY`.
- [ ] **Step 4: Run the suite.**
- [ ] **Step 5: Visual gate** — present, wait.

### Task 5 (Sección C): the draw action and the minified cost

**Files:**
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/ui/settlement/InnovationsModel.kt` (`CostView` → `CostChip`)
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/ui/settlement/DevelopPanels.kt`
- Test: `app-android/src/test/kotlin/io/kdmtracker/app/DevelopPanelsTest.kt`

**Interfaces:**
- Produces: `data class CostChip(val endeavors: Int, val resources: List<String>)` — the abbreviated form, e.g. `1` + `1H 1Ó 1P`; and `InnovationsModel.costChip: CostChip`, `InnovationsModel.drawLabel: String` (the button text).
- Consumes: `InnovationView.endeavorCost`, `InnovationView.resourceCost`.

- [ ] **Step 1: Write the failing test** — `theCostChipAbbreviatesResourcesToInitials` (Hueso→`H`, Órgano→`Ó`, Pellejo→`P`) and `theDrawLabelStatesTheTotalCost`.
- [ ] **Step 2: Run it** — expect FAIL.
- [ ] **Step 3: Implement** — `Coste` label on top; below it the wrapper (fill `surface-container-low`, border `surface-container-high`) with the gold `1` + `emergency` icon and the `1H 1Ó 1P` chips; then the full-width gold draw button.
- [ ] **Step 4: Run the suite.**
- [ ] **Step 5: Visual gate** — present, wait.

### Task 6 (Sección D): the two cards and `ACEPTAR`

**Files:**
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/ui/settlement/InnovationCardView.kt`
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/ui/settlement/DevelopPanels.kt`
- Test: `app-android/src/test/kotlin/io/kdmtracker/app/DevelopPanelsTest.kt`

**Interfaces:**
- Produces: `InnovationsModel.selection: InnovationId?` and `InnovationCardView(card, selected: Boolean, enabled: Boolean, onSelect: () -> Unit)`.
- Consumes: `InnovationsModel.hand`, the Task 1 card HTML.

- [ ] **Step 1: Write the failing test** — `theAcceptButtonIsAbsentBeforeASelection` and `selectingACardRevealsTheAcceptButton`.
- [ ] **Step 2: Run it** — expect FAIL.
- [ ] **Step 3: Rebuild the card from the cached HTML 1:1** — category chip in its rounded wrapper + icon right; below the chip a wrapper holding the text (Playfair 13sp bold title, Inter **10sp** muted description); full-width `SELECCIONAR`. **Equal height across the row, matched to the taller card.**
- [ ] **Step 4: Run the suite.**
- [ ] **Step 5: Visual gate** — present, wait.

### Task 7 (Sección E — inside): confirmation and the return

**Files:**
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/ui/settlement/DevelopPanels.kt`
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/usecase/Innovate.kt` (only if the confirm path is not already wired)
- Test: `app-android/src/test/kotlin/io/kdmtracker/app/DevelopPanelsTest.kt`, `core/.../InnovateUseCaseTest.kt`

**Interfaces:**
- Produces: `PhaseState.USED_THIS_PHASE` rendering — `RESOLUCIÓN DEL ROBO DE CARTAS` (`COMPLETADO`, the kept card, `1 CARTA DEVUELTA`) plus the gold block `Innovación Realizada en este Año Linterna`.
- Consumes: `InnovationsModel.phaseState`, `usedInPhase`.

- [ ] **Step 1: Write the failing test** — `confirmingUnlocksTheCardAndMarksThePhaseUsed` and `aSecondInnovateInTheSamePhaseIsBlocked`.
- [ ] **Step 2: Run it** — expect FAIL.
- [ ] **Step 3: Implement** — on `ACEPTAR`, run `Innovate`, then render state 2 and fall back to the catalogue with its filters.
- [ ] **Step 4: Run the suite.**
- [ ] **Step 5: Visual gate** — present, wait.

### Task 8: Land it

- [ ] **Step 1:** After the maintainer approves every visual gate: `spotlessApply`, full suite, then the commits.
- [ ] **Step 2:** Push and open the PR with `— size:exception`, confirming the exception at push time.

## Self-review

- **Spec coverage:** the maintainer's five flow points each map to a task (1→Task 3, 2→Task 3, 3→Task 4, 4→Task 5, 5→Task 7). The three UI corrections map to Tasks 5 and 6. `unlockedInYear` (choice **a**) is Task 2. Covered.
- **Type consistency:** `Innovation.unlockedInYear` (Task 2) is consumed by `SlotState` (Task 4) and stamped by `Innovate` (Task 2). `CostChip` (Task 5) is consumed by the draw row. Consistent.
- **Proportion:** 8 tasks, no function bodies beyond the two that are genuinely algorithmic.
