# Showdown Phase Implementation Plan (Slice 2)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the Showdown Phase a faithful tracker: set up a monster encounter, track the AI deck as the monster's health (draw/wound), reference hit locations, and record the outcome plus the aftermath.

**Architecture:** The showdown model and rules live in `:core` (pure, additive); monster cards are ingested from the `AF Enfrentamientos` decks. `:app-android` renders a stateless Showdown body driven by `UiState.showdown`.

**Tech Stack:** Kotlin Multiplatform (`:core`), Compose Multiplatform (Android), kotlinx.serialization, Python stdlib `unittest` + `pdftotext`.

**Spec:** `docs/superpowers/specs/2026-10-09-showdown-phase-design.md`

## Global Constraints

- Rules stay in `:core`; the app tracks, it does not resolve combat.
- No Compose UI tests; behavior is pinned in `:core` and the controller.
- UI copy is **English**; content text is the source Spanish.
- Conventional Commits in English; **no** `Co-Authored-By` / AI attribution.
- Pre-commit hook runs `spotlessKotlinCheck`; run `./gradlew spotlessApply` before committing.
- **RED before implementing** each test (run it and watch it fail).
- Full suite green: `./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck`; Python: `python3 -m unittest discover -s tools/content-ingest/tests -v`.

## Review Focus

1. **The AI deck is the monster's health** — draw cycles to `discard`; wound removes to `wounds`; the deck reshuffles when empty; victory is when every AI card is wounded (Task 4).
2. **A wound with no cards left is refused** (nothing to wound) and the outcome is terminal-only / non-re-entrant (Task 5).
3. **`RecordHuntXp` grants +1 only to listed survivors** and leaves others untouched (Task 5).
4. **Ingest tagging** — each card is tagged with the right monster and kind; empty cards abort (Task 2).
5. **Legacy transfer** — a v8 document (no `showdown`) still decodes with a default `ShowdownState` (Task 3).

---

## Task 1: Monster-card content type and catalog

**Files:**
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/domain/MonsterCard.kt`
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/content/ContentPack.kt`
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/content/ContentCatalog.kt`
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/content/MonsterCardsCatalogTest.kt`

**Interfaces:**
- Produces: `MonsterCardKind { AI, BASIC_ACTION, HIT_LOCATION }`; `MonsterCard(name, text, kind, monster, rank = null)`; `ContentBundle.monsterCards`; `ContentCatalog.monsterCardDefinitions` + `monsterCardsFor(monster: String): List<MonsterCard>`.

- [ ] **Step 1: Write the failing test** — `monsterCardsFor` returns only the named monster's cards across kinds; the merge dedupes by (monster, kind, name).

- [ ] **Step 2: Run it, watch it fail** (`./gradlew :core:jvmTest --tests "io.kdmtracker.core.content.MonsterCardsCatalogTest"`).

- [ ] **Step 3: Implement** `MonsterCard.kt` + `ContentBundle.monsterCards` + the catalog accessors (`monsterCardsFor` filters by `monster`; the merge key is `"$monster|$kind|$name"`).

- [ ] **Step 4: Run it green.** — **Step 5: Commit** `feat(core): add the monster-card content type and catalog`.

---

## Task 2: Monsters ingest pipeline + generated pack

**Files:**
- Create: `tools/content-ingest/ingest_monsters.py`
- Create: `tools/content-ingest/tests/test_monsters_parser.py`
- Create (generated): `core/src/commonMain/kotlin/io/kdmtracker/core/content/generated/Core15Monsters.kt`
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/content/Legacy15Content.kt`, `tools/content-ingest/README.md`
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/content/MonstersContentTest.kt`

**Interfaces:** consumes `MonsterCard` (Task 1); produces `Core15Monsters.bundle`; `Legacy15Content.pack.bundle.monsterCards`.

- [ ] **Step 1: Python test** (monkeypatch like `test_hunt_events_parser.py`): `AI` card recognition (name + rank letter + text), `HIT_LOCATION` recognition, `BASIC_ACTION` recognition, deck→monster tagging, empty-card abort.

- [ ] **Step 2: Run it, watch it fail.**

- [ ] **Step 3: Implement `ingest_monsters.py`** — for each folder under `AF Enfrentamientos/`, parse `AF AI <monster>.pdf` (kind AI; rank from the corner letter `B`/`A`/`L` when the second line matches), `AF AI <monster> - Acción Básica.pdf` (kind BASIC_ACTION), `AF HL <monster>.pdf` (kind HIT_LOCATION). The folder→catalog-monster map: `León Blanco → White Lion`, `Antílope Bramador → Screaming Antelope`, `Fénix → Phoenix`, `Carnicero → The Butcher`, `Hombre del Rey → King's Man`, `La Mano → The Hand`, `Vigilante → The Watcher`, `Humo Dorado → Golden Smoke Knight`. Dedupe by (monster, kind, name); abort on empty. Emit `Core15Monsters.kt`.

- [ ] **Step 4:** run the Python suite, generate the Kotlin, **Step 5:** Kotlin integrity test (`monsterCardsFor("White Lion")` has AI + HL + the basic action), **Step 6:** wire the pack, **Step 7:** README.

- [ ] **Step 8: Commit** `feat(content): ingest the 1.5 monster showdown decks`.

---

## Task 3: Showdown model + settlement state + transfer v9

**Files:** create `core/src/commonMain/kotlin/io/kdmtracker/core/domain/ShowdownState.kt`; modify `SettlementState.kt`, `TransferDocument.kt` (v9); test `ShowdownStateTest.kt`; modify `SettlementStateTransferTest.kt` (v9).

**Interfaces:**
- Produces: `ShowdownStatus { ACTIVE, VICTORY, DEFEAT }`; `ShowdownState(monster, level, aiDeck, discard, wounds, lastDrawn, status)`; `SettlementState.showdown`; `TransferDocument.schemaVersion = 9`.

- [ ] Tests: default + populated round-trip; missing-field defaults; `SettlementState` with showdown. Bump the transfer default + the existing default-version assertions (`SettlementStateTransferTest`, `TransferDocumentTest`, `SurvivorMigrationTest`), run RED then GREEN.

- [ ] **Commit** `feat(core): add the showdown model (transfer v9)`.

---

## Task 4: AI-deck use cases (setup, draw, wound)

**Files:** create `usecase/{SetUpShowdown,DrawShowdownCard,WoundMonster}.kt`; test `ShowdownDeckUseCaseTest.kt`.

**Interfaces:** `SetUpShowdown.run(record, monster, level, deck: List<String>): StepResult<SettlementRecord>`; `DrawShowdownCard.run(record): StepResult<SettlementRecord>`; `WoundMonster.run(record): StepResult<SettlementRecord>`.

- [ ] Tests: `setUpStoresTheEncounterAndShuffledDeck` (level 1..3, non-empty deck); `drawMovesTheTopCardToTheDiscard`; `drawReshufflesWhenTheDeckEmpties`; `woundMovesTheTopCardToTheWounds`; `woundingTheLastCardLeavesTheDeckAndDiscardEmpty` (victory-eligible); `woundWithNoCardsIsBlocked` (**Review Focus 1, 2**). Run RED then GREEN.

- [ ] **Commit** `feat(core): set up and track the monster's ai deck`.

---

## Task 5: Outcome and hunt-XP use cases

**Files:** create `usecase/{SetShowdownOutcome,RecordHuntXp}.kt`; test `ShowdownOutcomeUseCaseTest.kt`.

**Interfaces:** `SetShowdownOutcome.run(record, status): StepResult<SettlementRecord>`; `RecordHuntXp.run(record, survivorIds: List<Ulid>): StepResult<SettlementRecord>`.

- [ ] Tests: `setShowdownOutcomeStoresTerminalStatusesOnly`; `setShowdownOutcomeRejectsAReEntry` (**Review Focus 2**); `recordHuntXpGrantsOneToEachListedSurvivor` (others untouched) (**Review Focus 3**); `recordHuntXpRejectsUnknownIds`. Run RED then GREEN.

- [ ] **Commit** `feat(core): record the showdown outcome and hunt xp`.

---

## Task 6: App showdown views, controller and routing

**Files:** modify `UiState.kt`, `LanternYearController.kt`; test `ShowdownControllerTest.kt`.

**Interfaces:** `UiState.{showdown, monsterCards}`; `LanternYearController.{setUpShowdown, drawShowdownCard, woundMonster, finishShowdown, grantHuntXp}`.

- [ ] Tests: `setUpShowdownExposesTheEncounter`; `drawingAndWoundingUpdateTheDeck`; `finishingWithVictoryReturnsToTheSettlement` (Showdown → Settlement step 1 via `advance()`); `finishingWithDefeatRecordsDeathsAndReturns`. Run RED then GREEN.

- [ ] **Commit** `feat(app): expose the showdown state and actions`.

---

## Task 7: Showdown body UI

**Files:** modify `ui/ShowdownScreen.kt`; modify `LanternYearViewModel.kt`, `KdmTrackerApp.kt`. No unit test; suite + emulator.

- [ ] Header (monster + level); the AI tracker (`Draw` / `Wound` with the remaining/wounded counts + the last drawn card's text); the hit-locations reference (collapsible); the outcome actions (`Victory` / `Defeat`); the aftermath panel (`Add reward`, `Grant hunt XP`, `Record deaths`). Setup form (monster picker from the catalog's quarries + nemeses, level stepper, deck assembly from the content's AI cards). Wire the callbacks.
- [ ] Run the full suite; manual emulator pass (set up → draw/wound → the aftermath → back to the settlement). **Commit** `feat(app): add the showdown body`.

---

## Self-Review

- **Spec coverage:** content type (T1) · ingest (T2) · model+v9 (T3) · deck use cases (T4) · outcome/XP (T5) · app state+routing (T6) · body UI (T7).
- **Review Focus tests:** (1) the deck/shuffle/victory tests (T4); (2) `woundWithNoCardsIsBlocked` + `setShowdownOutcomeRejectsAReEntry` (T4/T5); (3) `recordHuntXpGrantsOneToEachListedSurvivor` (T5); (4) `deck→monster tagging` (T2); (5) the v8 decode test (T3).
- **Type consistency:** `MonsterCard`, `MonsterCardKind`, `ShowdownState`, `ShowdownStatus`, `monsterCardsFor`, `SetUpShowdown`/`DrawShowdownCard`/`WoundMonster`/`SetShowdownOutcome`/`RecordHuntXp` are used with the same shapes across tasks.
