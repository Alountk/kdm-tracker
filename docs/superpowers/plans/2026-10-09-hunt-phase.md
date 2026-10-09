# Hunt Phase Implementation Plan (Slice 1)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the Hunt Phase a faithful tracker: choose a quarry/level, set up and resolve the 13-space board, track the position and Overwhelming Darkness, apply event damage, and record the outcome — with a table-play fast path.

**Architecture:** The hunt model and every rule live in `:core` (pure, additive); hunt events are ingested from the `AF Caza` decks into a committed pack. `:app-android` renders a stateless 13-space board and reports intent through `LanternYearController`; the outcome drives the existing `LanternYearFlow`.

**Tech Stack:** Kotlin Multiplatform (`:core`), Compose Multiplatform (Android), kotlinx.serialization, Python stdlib `unittest` + `pdftotext` (poppler).

**Spec:** `docs/superpowers/specs/2026-10-09-hunt-phase-design.md`

## Global Constraints

- Rules stay in `:core`; the board is a tracker (records; does not resolve effects).
- No Compose UI tests (repo convention); behavior is pinned in `:core` and the controller.
- UI copy is **English**; content text is the source Spanish.
- Conventional Commits in English; **no** `Co-Authored-By` / AI attribution.
- Pre-commit hook runs `spotlessKotlinCheck`; run `./gradlew spotlessApply` before committing.
- The hunt board has **13 spaces** (`START` at 0, `MONSTER` at 12 by default); the layout is player-editable (the per-monster diagram is an image).
- Full suite must stay green: `./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck`.
- Python tests: `python3 -m unittest discover -s tools/content-ingest/tests -v`.

## Review Focus

Failure modes a person will hit that the tasks must pin:

1. **The board is 13 spaces** and the default has Start at 0 / Monster at 12; edits never change the count (Task 3, Task 4).
2. **The outcome drives the flow** — `QUARRY_FOUND`/`AMBUSH` advance to the Showdown; `FAILED` restarts the settlement (Task 5, Task 6).
3. **Player assignment is bounded** to `1..players` and the party is the Step-8 departers (Task 5).
4. **Ingest integrity** — the 12 duplicate "Evento Aleatorio" cards collapse to one and the monster decks tag their events with the right quarry (Task 2).
5. **Legacy transfer** — a v7 document (no `hunt`) still decodes with a default `HuntState` (Task 3).

---

## Task 1: Hunt-event content type and catalog

**Files:**
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/domain/HuntEvent.kt`
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/content/ContentPack.kt`
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/content/ContentCatalog.kt`
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/content/HuntEventsCatalogTest.kt`

**Interfaces:**
- Consumes: nothing new.
- Produces: `HuntEventKind { BASIC, SPECIAL, MONSTER }`; `HuntEvent(name, text, kind, monster = null)`; `ContentBundle.huntEvents`; `ContentCatalog.huntEventDefinitions` + `huntEventsFor(quarry: String): List<HuntEvent>`.

- [ ] **Step 1: Write the failing test**

`HuntEventsCatalogTest`:
- `huntEventsMergeAcrossPacksByKindAndName` — two packs merge, deduped by name.
- `huntEventsForIncludesBasicSpecialAndTheQuarry` — a catalog with a basic, a special, a "White Lion" monster event and a "Phoenix" one returns all but only the matching monster event for `"White Lion"` (basic + special + White Lion) (**Review Focus 4**).

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :core:jvmTest --tests "io.kdmtracker.core.content.HuntEventsCatalogTest"`
Expected: FAIL — unresolved `HuntEvent` / `huntEventsFor`.

- [ ] **Step 3: Implement**

`HuntEvent.kt`: `enum class HuntEventKind { BASIC, SPECIAL, MONSTER }` and `@Serializable data class HuntEvent(val name: String, val text: String, val kind: HuntEventKind, val monster: String? = null)`. Add `val huntEvents: List<HuntEvent> = emptyList()` to `ContentBundle`. In `ContentCatalog`: `huntEventDefinitions` (merge by name) and

```kotlin
fun huntEventsFor(quarry: String): List<HuntEvent> =
    huntEventDefinitions.filter { it.kind != HuntEventKind.MONSTER || it.monster == quarry }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :core:jvmTest --tests "io.kdmtracker.core.content.HuntEventsCatalogTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add core/src/commonMain/kotlin/io/kdmtracker/core/domain/HuntEvent.kt core/src/commonMain/kotlin/io/kdmtracker/core/content/ContentPack.kt core/src/commonMain/kotlin/io/kdmtracker/core/content/ContentCatalog.kt core/src/commonTest/kotlin/io/kdmtracker/core/content/HuntEventsCatalogTest.kt
git commit -m "feat(core): add the hunt-event content type and catalog"
```

---

## Task 2: Hunt-events ingest pipeline + generated pack

**Files:**
- Create: `tools/content-ingest/ingest_hunt_events.py`
- Create: `tools/content-ingest/tests/test_hunt_events_parser.py`
- Create (generated): `core/src/commonMain/kotlin/io/kdmtracker/core/content/generated/Core15HuntEvents.kt`
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/content/Legacy15Content.kt`
- Modify: `tools/content-ingest/README.md`
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/content/HuntEventsContentTest.kt`

**Interfaces:**
- Consumes: `HuntEvent`, `ContentBundle.huntEvents` (Task 1).
- Produces: `Core15HuntEvents.bundle: ContentBundle`; `Legacy15Content.pack.bundle.huntEvents`.

- [ ] **Step 1: Write the failing Python test**

`test_hunt_events_parser.py` (monkeypatch `pdf_text`/`pdf_info` like `test_settlement_events_parser.py`):
- `parsesTitleKindAndTextFromACard` — a basic card `Name / - evento de caza básico - / body` → `HuntEventKind.BASIC`.
- `recognizesTheSpecialKind` — `- evento de caza especial -` → `SPECIAL`.
- `recognizesAMonsterCardAndTagsItsDeckMonster` — sub-line `- evento de caza de león blanco -` with the deck mapped to `"White Lion"` → `MONSTER`/`"White Lion"`.
- `duplicateRandomEventCardsCollapseToOne` — many identical `Evento Aleatorio` cards dedupe to one.
- `pageCountMismatchAborts` / `unknownSublineAborts`.

- [ ] **Step 2: Run test to verify it fails**

Run: `python3 -m unittest discover -s tools/content-ingest/tests -p "test_hunt_events_parser.py" -v`
Expected: FAIL — module not found.

- [ ] **Step 3: Implement `ingest_hunt_events.py`**

Mirror `ingest_settlement_events.py`: one card per page; `name = lines[0]`; the **sub-line `lines[1]`** recognizes the kind (`- evento de caza básico -` → BASIC, `- evento de caza especial -` → SPECIAL, `- evento de caza de <monstruo> -` → MONSTER with the tag from the deck map); any other sub-line aborts. `text = normalize_ws(join(lines[2:]))`. Dedupe by name (identical text); assert `count == pages - 1`. Deck→quarry map (curated): `AF Caza León.pdf → "White Lion"`, `AF Caza Antílope.pdf → "Screaming Antelope"`, `AF Caza Fénix.pdf → "Phoenix"`; `AF Caza Básicos.pdf` contributes BASIC/SPECIAL (no monster). Emit `Core15HuntEvents.kt` (`ContentBundle(huntEvents = listOf(HuntEvent(...)))`). Deterministic (sorted, no paths).

- [ ] **Step 4: Run the Python tests and generate the Kotlin**

Run: `python3 -m unittest discover -s tools/content-ingest/tests -v` → all PASS.
Run: `python3 tools/content-ingest/ingest_hunt_events.py` (defaults point at `external-assets/.../AF Caza/`).
Expected: prints the parsed count → `Core15HuntEvents.kt`. Reconcile if a page count is unexpected.

- [ ] **Step 5: Write the failing Kotlin integrity test**

`HuntEventsContentTest`: `huntEventsAreNotEmpty`; `basicSpecialAndMonsterArePresent` (at least one of each kind, and a `monster == "White Lion"` entry); `packCarriesTheGeneratedHuntEvents`. 

- [ ] **Step 6: Wire the pack**

In `Legacy15Content`, add `huntEvents = Core15HuntEvents.bundle.huntEvents,`.

- [ ] **Step 7: Run the tests**

Run: `./gradlew :core:jvmTest --tests "io.kdmtracker.core.content.HuntEventsContentTest"` → PASS.

- [ ] **Step 8: Document the pipeline**

Add a "Hunt events" section to `tools/content-ingest/README.md` (source `AF Caza/*.pdf`, the command, the committed output, the deck→quarry map).

- [ ] **Step 9: Commit**

```bash
git add tools/content-ingest/ingest_hunt_events.py tools/content-ingest/tests/test_hunt_events_parser.py tools/content-ingest/README.md core/src/commonMain/kotlin/io/kdmtracker/core/content/generated/Core15HuntEvents.kt core/src/commonMain/kotlin/io/kdmtracker/core/content/Legacy15Content.kt core/src/commonTest/kotlin/io/kdmtracker/core/content/HuntEventsContentTest.kt
git commit -m "feat(content): ingest the 1.5 hunt events decks"
```

---

## Task 3: Hunt model + settlement state + transfer v8

**Files:**
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/domain/HuntState.kt`
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/domain/SettlementState.kt`
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/transfer/TransferDocument.kt` (v8)
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/domain/HuntStateTest.kt`
- Modify: `core/src/commonTest/kotlin/io/kdmtracker/core/transfer/SettlementStateTransferTest.kt` (v8)

**Interfaces:**
- Consumes: `HuntStatus`, `Ulid`.
- Produces: `HuntSpaceKind { START, NORMAL, MONSTER_E, OVERWHELMING_DARKNESS, STARVATION, MONSTER }`; `HuntSpace(kind, events, resolved)`; `HuntState(quarry, level, players, assignments, spaces, position, status)`; `SettlementState.hunt`; `TransferDocument.schemaVersion = 8`.

- [ ] **Step 1: Write the failing test**

`HuntStateTest`:
- `defaultHuntStateRoundTripsThroughJson`.
- `populatedHuntStateRoundTrips` (quarry, level, players, an assignment, spaces with events/resolved, position, status).
- `jsonMissingHuntFieldDecodesWithDefaults`.
- `stateWithHuntRoundTrips`.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :core:jvmTest --tests "io.kdmtracker.core.domain.HuntStateTest"`
Expected: FAIL — unresolved `HuntState`.

- [ ] **Step 3: Implement**

`HuntState.kt`:

```kotlin
@Serializable
enum class HuntSpaceKind { START, NORMAL, MONSTER_E, OVERWHELMING_DARKNESS, STARVATION, MONSTER }

@Serializable
data class HuntSpace(
    val kind: HuntSpaceKind = HuntSpaceKind.NORMAL,
    val events: List<String> = emptyList(),
    val resolved: List<String> = emptyList(),
)

@Serializable
data class HuntState(
    val quarry: String? = null,
    val level: Int = 1,
    val players: Int = 1,
    val assignments: Map<Ulid, Int> = emptyMap(),
    val spaces: List<HuntSpace> = emptyList(),
    val position: Int = 0,
    val status: HuntStatus = HuntStatus.ACTIVE,
)
```

Add `val hunt: HuntState = HuntState(),` to `SettlementState` (+ KDoc). Bump `TransferDocument` to v8 (`SUPPORTED_VERSIONS` includes 8) and update `SettlementStateTransferTest`'s default-version assertion.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :core:jvmTest --tests "io.kdmtracker.core.domain.HuntStateTest" --tests "io.kdmtracker.core.transfer.SettlementStateTransferTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add core/src/commonMain/kotlin/io/kdmtracker/core/domain/HuntState.kt core/src/commonMain/kotlin/io/kdmtracker/core/domain/SettlementState.kt core/src/commonMain/kotlin/io/kdmtracker/core/transfer/TransferDocument.kt core/src/commonTest/kotlin/io/kdmtracker/core/domain/HuntStateTest.kt core/src/commonTest/kotlin/io/kdmtracker/core/transfer/SettlementStateTransferTest.kt
git commit -m "feat(core): add the hunt model and state (transfer v8)"
```

---

## Task 4: Board use cases (setup, space kind, advance)

**Files:**
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/usecase/SetUpHunt.kt`
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/usecase/MarkSpaceKind.kt`
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/usecase/AdvanceHunt.kt`
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/usecase/HuntBoardUseCaseTest.kt`

**Interfaces:**
- Consumes: `HuntState`, `HuntSpace`, `HuntSpaceKind`.
- Produces: `SetUpHunt.BOARD_SPACES = 13`; `SetUpHunt.run(record, quarry, level, players): StepResult<SettlementRecord>`; `MarkSpaceKind.run(record, index, kind): StepResult<SettlementRecord>`; `AdvanceHunt.run(record): StepResult<SettlementRecord>`.

- [ ] **Step 1: Write the failing test**

`HuntBoardUseCaseTest`:
- `setUpSeedsThirteenSpacesWithStartAndMonster` — Start at 0, Monster at 12, the rest normal (**Review Focus 1**).
- `setUpStoresQuarryLevelPlayersAndResetsStatus` — `status == ACTIVE`, position 0.
- `setUpRejectsABlankQuarry` — a blank quarry string is `Blocked` (the catalog validation is the UI's job; the use case only requires a non-blank quarry).
- `markSpaceKindEditsAKindWithoutChangingTheCount`.
- `advanceMovesOneSpaceAndCapsAtTheEnd`.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :core:jvmTest --tests "io.kdmtracker.core.usecase.HuntBoardUseCaseTest"`
Expected: FAIL — unresolved.

- [ ] **Step 3: Implement**

- `SetUpHunt.run`: `spaces = List(BOARD_SPACES) { index -> HuntSpace(if (index == 0) START else if (index == BOARD_SPACES - 1) MONSTER else NORMAL) }`; `hunt = HuntState(quarry, level, players, assignments = emptyMap(), spaces, position = 0, status = ACTIVE)`. Block on `level !in 1..3` or `players !in 1..4`.
- `MarkSpaceKind.run`: block on an out-of-range index; else copy the space with the new kind (count unchanged).
- `AdvanceHunt.run`: block when `status != ACTIVE`; else `position = min(position + 1, spaces.lastIndex)`.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :core:jvmTest --tests "io.kdmtracker.core.usecase.HuntBoardUseCaseTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add core/src/commonMain/kotlin/io/kdmtracker/core/usecase/{SetUpHunt,MarkSpaceKind,AdvanceHunt}.kt core/src/commonTest/kotlin/io/kdmtracker/core/usecase/HuntBoardUseCaseTest.kt
git commit -m "feat(core): set up and advance the hunt board"
```

---

## Task 5: Party, events and outcome use cases

**Files:**
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/usecase/AssignPlayer.kt`
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/usecase/AssignHuntEvent.kt`
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/usecase/ResolveHuntEvent.kt`
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/usecase/SetHuntOutcome.kt`
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/usecase/HuntPartyOutcomeUseCaseTest.kt`

**Interfaces:**
- Consumes: `HuntState`, `HuntStatus`, `Ulid`.
- Produces: `AssignPlayer.run(record, survivorId, player): StepResult<SettlementRecord>`; `AssignHuntEvent.run(record, index, eventName)`; `ResolveHuntEvent.run(record, index, eventName)`; `SetHuntOutcome.run(record, status)`.

- [ ] **Step 1: Write the failing test**

`HuntPartyOutcomeUseCaseTest`:
- `assignPlayerIsBoundedToThePlayerCount` — `player` outside `1..players` is `Blocked` (**Review Focus 3**).
- `assignPlayerRejectsAnUnknownSurvivor`.
- `assignHuntEventAppendsToASpace` / `resolveHuntEventMarksItResolved`.
- `setHuntOutcomeAdvancesTheStatus` — a terminal status (`QUARRY_FOUND`, `AMBUSH`, `FAILED`, `RUN_AWAY`) is stored; `ACTIVE` is refused.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :core:jvmTest --tests "io.kdmtracker.core.usecase.HuntPartyOutcomeUseCaseTest"`
Expected: FAIL — unresolved.

- [ ] **Step 3: Implement**

Each returns `StepResult<SettlementRecord>`, editing `record.state.hunt`. `AssignPlayer` blocks on an unknown survivor id or `player !in 1..hunt.players`. `AssignHuntEvent`/`ResolveHuntEvent` block on an out-of-range index; `assign` dedupes, `resolve` requires the event to be assigned. `SetHuntOutcome` blocks when `status == ACTIVE` (it must be a terminal outcome).

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :core:jvmTest --tests "io.kdmtracker.core.usecase.HuntPartyOutcomeUseCaseTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add core/src/commonMain/kotlin/io/kdmtracker/core/usecase/{AssignPlayer,AssignHuntEvent,ResolveHuntEvent,SetHuntOutcome}.kt core/src/commonTest/kotlin/io/kdmtracker/core/usecase/HuntPartyOutcomeUseCaseTest.kt
git commit -m "feat(core): assign the hunt party, events and outcome"
```

---

## Task 6: App hunt views, controller and outcome routing

**Files:**
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/UiState.kt` (add `hunt`)
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/LanternYearController.kt`
- Test: `app-android/src/test/kotlin/io/kdmtracker/app/HuntControllerTest.kt`

**Interfaces:**
- Consumes: all `:core` hunt use cases/types.
- Produces: `UiState.hunt: HuntState?`; `LanternYearController.{setUpHunt, markSpaceKind, advanceHunt, assignPlayer, assignHuntEvent, resolveHuntEvent, finishHunt}`.

- [ ] **Step 1: Write the failing test**

`HuntControllerTest` (mirror `SettlementBookkeepingControllerTest`; a store seeded at the Hunt phase):
- `setUpHuntExposesThirteenSpaces` — `state.value.hunt?.spaces?.size == 13`.
- `markSpaceKindPersists`.
- `finishingWithQuarryFoundMovesToTheShowdown` — `finishHunt(QUARRY_FOUND)` → `state.value.phase == SHOWDOWN` (**Review Focus 2**).
- `finishingWithFailedRestartsTheSettlement` — `finishHunt(FAILED)` → `phase == SETTLEMENT` and `currentStep == 1`.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app-android:testDebugUnitTest --tests "io.kdmtracker.app.HuntControllerTest"`
Expected: FAIL — unresolved.

- [ ] **Step 3: Implement**

- `UiState.hunt: HuntState? = null`; populate in `uiState` from `record.state.hunt` when `settlement.phase == HUNT`.
- Controller methods delegating through `applyResult` (`setUpHunt`, `markSpaceKind`, `advanceHunt`, `assignPlayer`, `assignHuntEvent`, `resolveHuntEvent`).
- `finishHunt(outcome)`: set the hunt status via `SetHuntOutcome`, then transition — `FAILED` → `applyStep(returnees = 0, context = FlowContext(ruleset, huntFailed = true))` (Settlement restart); otherwise `advance()` (Hunt → Showdown). Route the record update and the phase move in the right order so the stored hunt status survives.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app-android:testDebugUnitTest --tests "io.kdmtracker.app.HuntControllerTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app-android/src/main/kotlin/io/kdmtracker/app/UiState.kt app-android/src/main/kotlin/io/kdmtracker/app/LanternYearController.kt app-android/src/test/kotlin/io/kdmtracker/app/HuntControllerTest.kt
git commit -m "feat(app): expose the hunt state and actions"
```

---

## Task 7: Hunt board UI

**Files:**
- Create: `app-android/src/main/kotlin/io/kdmtracker/app/ui/HuntBoard.kt`
- Create: `app-android/src/main/kotlin/io/kdmtracker/app/ui/HuntEventPanel.kt`
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/ui/HuntScreen.kt`
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/KdmTrackerApp.kt` (pass the hunt callbacks)

**Interfaces:**
- Consumes: `UiState.hunt`, the Task 6 controller methods.
- Produces: `HuntBoard(hunt, onSelectSpace, onSetOutcome)`, `HuntEventPanel(...)`, the rewritten `HuntScreen`.

> No unit test (UI-only); the board model and routing are pinned by Tasks 3–6. Verification is compilation, `spotlessCheck`, the full suite, and the emulator.

- [ ] **Step 1: Implement the board (option A)**

`HuntBoard`: a horizontal, scrollable row of the 13 spaces; each space shows its kind (Start / Ξ E / ◎ Overwhelming Darkness / ☠ Starvation / monster) and its state (done/current); the current space is highlighted; tapping selects it. A header shows the quarry + level + players and the party (the Step-8 departing survivors) with its player assignment.

- [ ] **Step 2: Implement the events panel**

`HuntEventPanel`: for the selected space, list its events (expand to show the text, `Mark resolved`), an `Assign event` control drawing from `catalog.huntEventsFor(quarry)`, and a `Mark as Overwhelming Darkness / Starvation / E` control. The panel's `Advance` calls `onAdvance`.

- [ ] **Step 3: Rewrite `HuntScreen`**

Compose the header + `HuntBoard` + `HuntEventPanel` + the outcome row (`Find quarry`, `Ambush`, `Starvation`, `Run away`, `Hunt fails`) routing to `finishHunt`. When `state.hunt` is null (not set up), show a `Set up the hunt` action (quarry picker from the catalog's quarries + level + players).

- [ ] **Step 4: Wire the callbacks**

Add the ViewModel delegates and pass them from `KdmTrackerApp`'s `HuntScreen(...)` call.

- [ ] **Step 5: Run the full suite**

Run: `./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Manual emulator pass**

Run: `./gradlew :app-android:installDebug`, drive a settlement to the Hunt phase: set up (quarry + level + players), see 13 spaces, assign/resolve an event, advance, and record the outcome → the Showdown (or the settlement restart). (**Review Focus 1, 2**)

- [ ] **Step 7: Commit**

```bash
git add app-android/src/main/kotlin/io/kdmtracker/app/ui/HuntBoard.kt app-android/src/main/kotlin/io/kdmtracker/app/ui/HuntEventPanel.kt app-android/src/main/kotlin/io/kdmtracker/app/ui/HuntScreen.kt app-android/src/main/kotlin/io/kdmtracker/app/LanternYearViewModel.kt app-android/src/main/kotlin/io/kdmtracker/app/KdmTrackerApp.kt
git commit -m "feat(app): add the hunt board UI"
```

---

## Self-Review

- **Spec coverage:** hunt-event type + catalog (Task 1) · ingest + pack (Task 2) · model + v8 (Task 3) · board use cases (Task 4) · party/events/outcome (Task 5) · app state + routing (Task 6) · board UI (Task 7).
- **Review Focus tests:** (1) `setUpSeedsThirteenSpacesWithStartAndMonster`; (2) `finishingWithQuarryFoundMovesToTheShowdown` / `finishingWithFailedRestartsTheSettlement`; (3) `assignPlayerIsBoundedToThePlayerCount`; (4) `duplicateRandomEventCardsCollapseToOne` + `huntEventsForIncludesBasicSpecialAndTheQuarry`; (5) the v7 decode test in `SettlementStateTransferTest`.
- **Type consistency:** `HuntEvent`, `HuntEventKind`, `HuntState`, `HuntSpace`, `HuntSpaceKind`, `huntEventsFor`, `SetUpHunt.BOARD_SPACES` are used with the same shapes across tasks.
