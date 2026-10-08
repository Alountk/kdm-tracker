# Settlement Bookkeeping Steps (4, 5, 6, 10, 11) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn Settlement Phase steps 4 (Update Timeline), 5 (Death Count), 6 (Check Milestones), 10 (Record & Archive Resources) and 11 (End Settlement Phase) into real, usable bodies, ingest the Settlement Event deck as content, and fix the missing lantern-year increment.

**Architecture:** All rules live in `:core` as pure use cases and additive record/state fields; the `:app-android` screens are stateless composables driven by `UiState` and reporting intents through `LanternYearController`. Settlement events are ingested from the 1.5 PDF into a committed generated Kotlin pack, mirroring `ingest_fighting_arts.py`.

**Tech Stack:** Kotlin Multiplatform (`:core` pure), Compose Multiplatform (Android), kotlinx.serialization, SQLDelight (unchanged), Python stdlib `unittest` + `pdftotext`/`pdfinfo` (poppler).

**Spec:** `docs/superpowers/specs/2026-10-08-settlement-bookkeeping-steps-design.md`

## Global Constraints

- Rules stay in `:core`; composables render state and report intent only. No game rule in `:app-android`.
- Conventional Commits in English; **no** `Co-Authored-By` / AI attribution.
- TDD: write the failing test, see it fail, implement, see it pass, commit.
- Content is generated-and-committed Kotlin; the app **never** reads the PDFs at runtime.
- The settlement record is persisted as JSON `stateJson` — **no** SQLDelight schema migration.
- Full suite must stay green: `./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck`.
- Python tests: `python3 -m unittest discover -s tools/content-ingest/tests -v`.
- `spotlessCheck` runs in the Gradle suite; new Kotlin must satisfy it (ktlint/spotless formatting).

## Review Focus

Failure modes a person will hit that the tasks must pin with tests:

1. **Deaths exceed population** — recording more deaths than living survivors must still count every death and floor population at 0, never negative (Step 5).
2. **Revisiting Step 4** — walking 4 → 5 → 4 → 5 must increment the lantern year exactly once per phase (marker).
3. **Next year's advance** — after the year boundary resets the phase markers, the following Settlement Phase's Step 4 must increment again.
4. **Bad principle / milestone input** — choosing a label not in the ruleset, or resolving an unreached milestone, must be refused with the record untouched.
5. **Pre-v6 transfer document** — a v5 document (no `principles` / `resolvedMilestones`) must still decode with defaults.

---

## Task 1: SettlementEvent content type, bundle and catalog

**Files:**
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/domain/SettlementEvent.kt`
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/content/ContentPack.kt`
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/content/ContentCatalog.kt`
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/content/ContentCatalogMergeTest.kt` (add cases)

**Interfaces:**
- Consumes: nothing.
- Produces: `SettlementEvent(name: String, text: String)`; `ContentBundle.settlementEvents: List<SettlementEvent>`; `ContentCatalog.settlementEventDefinitions: List<SettlementEvent>` and `ContentCatalog.settlementEvents: List<String>`.

- [ ] **Step 1: Write the failing test**

Add to `ContentCatalogMergeTest` a case that composes two packs each contributing a settlement event and asserts the merged definitions are deduped by name and sorted, and that `settlementEvents` is the sorted name list:

```kotlin
@Test
fun settlementEventsMergeAcrossPacksSortedAndDeduped() {
    val a = packWithSettlementEvents(SettlementEvent("Plaga", "plaga text"))
    val b = packWithSettlementEvents(
        SettlementEvent("Rivalidad", "rivalidad text"),
        SettlementEvent("Plaga", "plaga text"),
    )
    val catalog = (ContentCatalogComposer.compose(listOf(a, b)) as CatalogResult.Ok).catalog

    assertEquals(listOf("Plaga", "Rivalidad"), catalog.settlementEvents)
    assertEquals(2, catalog.settlementEventDefinitions.size)
}
```

Add a `helper` `packWithSettlementEvents(vararg events: SettlementEvent): ContentPack` (a `ContentPack(id=..., version=..., name=..., bundle=ContentBundle(settlementEvents = events.toList()))`).

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :core:jvmTest --tests "io.kdmtracker.core.content.ContentCatalogMergeTest"`
Expected: FAIL — does not compile (`SettlementEvent` / `settlementEvents` unresolved).

- [ ] **Step 3: Implement**

Create `SettlementEvent.kt`:

```kotlin
package io.kdmtracker.core.domain

import kotlinx.serialization.Serializable

/** A Settlement Event card: a stable display [name] and its rules [text]. */
@Serializable
data class SettlementEvent(val name: String, val text: String)
```

Add `val settlementEvents: List<SettlementEvent> = emptyList(),` to `ContentBundle`. In `ContentCatalog` add:

```kotlin
val settlementEventDefinitions: List<SettlementEvent> =
    mergeDefinition(packs.flatMap { it.bundle.settlementEvents }) { it.name }
val settlementEvents: List<String> = settlementEventDefinitions.map { it.name }.sorted()
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :core:jvmTest --tests "io.kdmtracker.core.content.ContentCatalogMergeTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add core/src/commonMain/kotlin/io/kdmtracker/core/domain/SettlementEvent.kt \
        core/src/commonMain/kotlin/io/kdmtracker/core/content/ContentPack.kt \
        core/src/commonMain/kotlin/io/kdmtracker/core/content/ContentCatalog.kt \
        core/src/commonTest/kotlin/io/kdmtracker/core/content/ContentCatalogMergeTest.kt
git commit -m "feat(core): add the settlement-event content type and catalog"
```

---

## Task 2: Settlement events ingest pipeline + generated pack

**Files:**
- Create: `tools/content-ingest/ingest_settlement_events.py`
- Create: `tools/content-ingest/tests/test_settlement_events_parser.py`
- Create (generated by running the script): `core/src/commonMain/kotlin/io/kdmtracker/core/content/generated/Core15SettlementEvents.kt`
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/content/Legacy15Content.kt`
- Modify: `tools/content-ingest/README.md`
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/content/SettlementEventsTest.kt`

**Interfaces:**
- Consumes: `SettlementEvent`, `ContentBundle.settlementEvents` (Task 1).
- Produces: `Core15SettlementEvents.bundle: ContentBundle` (generated); `Legacy15Content.pack.bundle.settlementEvents` populated.

- [ ] **Step 1: Write the failing Python test**

Create `test_settlement_events_parser.py` following `test_fighting_arts_parser.py`'s monkeypatch pattern (`mock.patch.object(mod, "pdf_text", ...)`, `mock.patch.object(mod, "pdf_info", return_value={"Pages": ...})`). Cases:
- `test_parses_name_and_text_from_each_card_page` — a 3-page deck (cover + 2 cards) yields 2 entries; first entry name is the page's first line, `text` is the whitespace-normalized remaining lines.
- `test_title_page_is_skipped` — the cover page never becomes a card.
- `test_page_count_mismatch_aborts` — when `pdfinfo` Pages disagrees with the form-feed split, `SystemExit` is raised.
- `test_empty_card_aborts` — a card page with a blank name or blank text raises `SystemExit`.
- `test_duplicate_names_abort` — two pages with the same name but different text raise `SystemExit`.

- [ ] **Step 2: Run test to verify it fails**

Run: `python3 -m unittest tools.content-ingest.tests.test_settlement_events_parser -v` (or `python3 -m unittest discover -s tools/content-ingest/tests -p "test_settlement_events_parser.py" -v`)
Expected: FAIL — module `ingest_settlement_events` not found.

- [ ] **Step 3: Implement `ingest_settlement_events.py`**

Mirror `ingest_fighting_arts.py`: reuse `pdf_text`, `pdf_info`, and `kotlin_emit` (`normalize_ws`, `indent`, `kotlin_escape`, `description_lines`).

- `_pages(pdf_path)` — split on `\f`, cross-check against `pdfinfo` Pages.
- `parse_events(pdf_path) -> List[SettlementEvent]` — skip page 1 (cover, `TITLE_PAGE = 1`); for every later page: `name = lines[0]`, `text = normalize_ws(" ".join(lines[1:]))`; a page with fewer than 3 non-blank lines, a blank name, or blank text aborts with `SystemExit`; assert `len(cards) == len(pages) - 1`.
- `dedupe_sorted` — reject same-name/different-text, else sort by name (reuse the fighting-arts approach).
- `emit_kotlin(events)` — write `object Core15SettlementEvents { val bundle: ContentBundle = ContentBundle(settlementEvents = listOf( SettlementEvent(name = "…", text = description_lines-wrapped "…") , …)) }` with header `// Generated by tools/content-ingest/ingest_settlement_events.py. Do not edit by hand.`
- `DEFAULT_PDF_RELATIVE = Path("external-assets/Juego Completo/Formato XL 86x146 mm/AF Eventos.pdf")`; `DEFAULT_OUT_RELATIVE = Path("core/src/commonMain/kotlin/io/kdmtracker/core/content/generated/Core15SettlementEvents.kt")`.
- `main(argv)` — argparse (`--pdf`, `--out`), abort if the PDF is missing, parse, emit, print the count to stderr, return 0.

- [ ] **Step 4: Run the Python tests and generate the Kotlin**

Run: `python3 -m unittest discover -s tools/content-ingest/tests -v`
Expected: all PASS.
Run: `python3 tools/content-ingest/ingest_settlement_events.py`
Expected: prints `parsed 23 settlement events -> …/Core15SettlementEvents.kt`. If the count is not 23, stop and reconcile (the deck is 23 cards, p1 cover).

- [ ] **Step 5: Write the failing Kotlin integrity test**

Create `SettlementEventsTest` (mirror `FightingArtsTest`): `settlementEventsAreNotEmpty`, `settlementEventsHaveTheExpectedCardCount` (`assertEquals(23, …)`), `knownEventIsPresentWithNonBlankText` (`"Rivalidad"` present and text non-blank), `packCarriesTheGeneratedSettlementEvents` (`assertEquals(Core15SettlementEvents.bundle.settlementEvents, Legacy15Content.pack.bundle.settlementEvents)`), `namesAreSorted`.

- [ ] **Step 6: Wire the pack**

In `Legacy15Content`, add `settlementEvents = Core15SettlementEvents.bundle.settlementEvents,` to the `ContentBundle(...)`, with the import.

- [ ] **Step 7: Run the tests**

Run: `./gradlew :core:jvmTest --tests "io.kdmtracker.core.content.SettlementEventsTest"`
Expected: PASS.

- [ ] **Step 8: Document the pipeline**

Add a "Settlement events" section to `tools/content-ingest/README.md` matching the fighting-arts section: source PDF path, `python3 tools/content-ingest/ingest_settlement_events.py`, and the committed output path.

- [ ] **Step 9: Commit**

```bash
git add tools/content-ingest/ingest_settlement_events.py \
        tools/content-ingest/tests/test_settlement_events_parser.py \
        tools/content-ingest/README.md \
        core/src/commonMain/kotlin/io/kdmtracker/core/content/generated/Core15SettlementEvents.kt \
        core/src/commonMain/kotlin/io/kdmtracker/core/content/Legacy15Content.kt \
        core/src/commonTest/kotlin/io/kdmtracker/core/content/SettlementEventsTest.kt
git commit -m "feat(content): ingest the 1.5 settlement events deck"
```

---

## Task 3: SettlementState principles + resolved milestones, transfer v6

**Files:**
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/domain/SettlementState.kt`
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/transfer/TransferDocument.kt`
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/domain/SettlementStateSerializationTest.kt` (add cases)
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/transfer/SettlementStateTransferTest.kt` (add cases)

**Interfaces:**
- Consumes: nothing.
- Produces: `SettlementState.principles: Map<PrincipleCategory, String>`, `SettlementState.resolvedMilestones: Set<MilestoneId>`; `TransferDocument.schemaVersion = 6`, `SUPPORTED_VERSIONS` includes 6.

- [ ] **Step 1: Write the failing tests**

In `SettlementStateSerializationTest`:
- `principlesAndResolvedMilestonesRoundTrip` — a `SettlementState` with `principles = mapOf(PrincipleCategory.DEATH to "The First Grave → Graves")` and `resolvedMilestones = setOf(Core16Milestones.PRINCIPLE_DEATH)` encodes and decodes equal.
- `jsonMissingNewFieldsDecodesWithDefaults` — a JSON object without `principles`/`resolvedMilestones` decodes with `emptyMap()` / `emptySet()`.

In `SettlementStateTransferTest`:
- `v5DocumentWithoutPrinciplesDecodes` — a `TransferDocument` JSON with `schemaVersion: 5` and a settlement state lacking the new fields decodes; the round-trip keeps `principles` empty.
- `v6RoundTripCarriesPrinciplesAndMilestones` — a populated state survives `toDto`/`toDomain` and `encodeToString`/`decodeFromString` with `schemaVersion = 6`.
- `supportedVersionsIncludesSix` — `assertTrue(6 in TransferDocument.SUPPORTED_VERSIONS)`.

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :core:jvmTest --tests "io.kdmtracker.core.domain.SettlementStateSerializationTest" --tests "io.kdmtracker.core.transfer.SettlementStateTransferTest"`
Expected: FAIL — `principles` / `resolvedMilestones` unresolved.

- [ ] **Step 3: Implement**

Add to `SettlementState` (after `gear`):

```kotlin
val principles: Map<PrincipleCategory, String> = emptyMap(),
val resolvedMilestones: Set<MilestoneId> = emptySet(),
```

(No import needed: same package.) In `TransferDocument`: `schemaVersion: Int = 6` and `SUPPORTED_VERSIONS = listOf(1, 2, 3, 4, 5, 6)`; update the KDoc to describe v6.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :core:jvmTest --tests "io.kdmtracker.core.domain.SettlementStateSerializationTest" --tests "io.kdmtracker.core.transfer.SettlementStateTransferTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add core/src/commonMain/kotlin/io/kdmtracker/core/domain/SettlementState.kt \
        core/src/commonMain/kotlin/io/kdmtracker/core/transfer/TransferDocument.kt \
        core/src/commonTest/kotlin/io/kdmtracker/core/domain/SettlementStateSerializationTest.kt \
        core/src/commonTest/kotlin/io/kdmtracker/core/transfer/SettlementStateTransferTest.kt
git commit -m "feat(core): persist chosen principles and resolved milestones (transfer v6)"
```

---

## Task 4: Counter and timeline use cases (RecordDeaths, RecordBirths, AdvanceTimeline)

**Files:**
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/usecase/RecordDeaths.kt`
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/usecase/RecordBirths.kt`
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/usecase/AdvanceTimeline.kt`
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/usecase/CountersAndTimelineUseCaseTest.kt`

**Interfaces:**
- Consumes: `SettlementRecord`, `StepResult`.
- Produces:
  - `RecordDeaths.run(record: SettlementRecord, count: Int): StepResult<SettlementRecord>`
  - `RecordBirths.run(record: SettlementRecord, count: Int): StepResult<SettlementRecord>`
  - `AdvanceTimeline.run(record: SettlementRecord): SettlementRecord` (idempotent, phase-marker "Update Timeline")

- [ ] **Step 1: Write the failing tests**

`CountersAndTimelineUseCaseTest` (build a `SettlementRecord` over a helper `Settlement(...)` as the other use-case tests do):
- `recordDeathsAddsToCounterAndLowersPopulation` — pop 4, `RecordDeaths.run(rec, 2)` → `deaths == 2`, `population == 2`.
- `recordDeathsFloorsPopulationAtZero` — pop 1, `RecordDeaths.run(rec, 3)` → `deaths == 3`, `population == 0` (**Review Focus 1**).
- `recordDeathsRejectsNonPositive` — `RecordDeaths.run(rec, 0)` is `Blocked` and the record is unchanged.
- `recordBirthsAddsToCounterAndRaisesPopulation` — `RecordBirths.run(rec, 3)` → `births == 3`, `population == 7`.
- `advanceTimelineIncrementsOnceThenIsIdempotent` — first `AdvanceTimeline.run(rec).settlement.lanternYear == 2`; a second call leaves it at 2 (**Review Focus 2**).
- `advanceTimelineRunsAgainAfterMarkersReset` — after clearing `phaseActionsUsed`, `AdvanceTimeline.run` increments again (**Review Focus 3**).

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :core:jvmTest --tests "io.kdmtracker.core.usecase.CountersAndTimelineUseCaseTest"`
Expected: FAIL — unresolved symbols.

- [ ] **Step 3: Implement**

- `RecordDeaths.run`: `if (count < 1) return StepResult.Blocked("invalid death count")`; else `Ok(record.copy(settlement = settlement.copy(population = (population - count).coerceAtLeast(0)), state = state.copy(deaths = state.deaths + count)))`.
- `RecordBirths.run`: `if (count < 1) return StepResult.Blocked("invalid birth count")`; else `Ok(record.copy(settlement = settlement.copy(population = population + count), state = state.copy(births = state.births + count)))`.
- `AdvanceTimeline`: marker `private const val MARKER = "Update Timeline"`.
  ```kotlin
  fun run(record: SettlementRecord): SettlementRecord {
      if (record.state.usedInPhase(MARKER)) return record
      return record.copy(
          settlement = record.settlement.copy(lanternYear = record.settlement.lanternYear + 1),
          state = record.state.withPhaseAction(MARKER),
      )
  }
  ```
  KDoc: the increment is idempotent per phase because `phaseActionsUsed` resets at the year boundary.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :core:jvmTest --tests "io.kdmtracker.core.usecase.CountersAndTimelineUseCaseTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add core/src/commonMain/kotlin/io/kdmtracker/core/usecase/RecordDeaths.kt \
        core/src/commonMain/kotlin/io/kdmtracker/core/usecase/RecordBirths.kt \
        core/src/commonMain/kotlin/io/kdmtracker/core/usecase/AdvanceTimeline.kt \
        core/src/commonTest/kotlin/io/kdmtracker/core/usecase/CountersAndTimelineUseCaseTest.kt
git commit -m "feat(core): record deaths/births and advance the lantern year"
```

---

## Task 5: Decision use cases (ChoosePrinciple, ResolveMilestone, RecordSettlementEvent)

**Files:**
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/usecase/ChoosePrinciple.kt`
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/usecase/ResolveMilestone.kt`
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/usecase/RecordSettlementEvent.kt`
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/domain/MilestoneId.kt`
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/usecase/DecisionUseCaseTest.kt`

**Interfaces:**
- Consumes: `SettlementRecord`, `StepResult`, `Ruleset`, `ContentCatalog`, `Core16Milestones`.
- Produces:
  - `Core16Milestones.principleCategoryOf(id: MilestoneId): PrincipleCategory?`
  - `ChoosePrinciple.run(record, category: PrincipleCategory, label: String, ruleset: Ruleset): StepResult<SettlementRecord>`
  - `ResolveMilestone.run(record, id: MilestoneId, ruleset: Ruleset): StepResult<SettlementRecord>`
  - `RecordSettlementEvent.run(record, name: String, catalog: ContentCatalog): StepResult<SettlementRecord>`

- [ ] **Step 1: Write the failing tests**

`DecisionUseCaseTest`:
- `choosePrincipleStoresTheChoice` — choosing `("The First Grave → Graves")` for `DEATH` (from `Core16Ruleset`) persists `principles[DEATH]`.
- `choosePrincipleRejectsUnknownLabel` — an unknown label is `Blocked` and the state is unchanged (**Review Focus 4**).
- `resolveMilestoneTicksAReachedMilestone` — DEATHS = 1 then `ResolveMilestone.run(rec, PRINCIPLE_DEATH, Core16Ruleset)` adds it to `resolvedMilestones`.
- `resolveMilestoneRejectsAnUnreachedMilestone` — `PRINCIPLE_DEATH` with DEATHS = 0 is `Blocked` and unchanged (**Review Focus 4**).
- `recordSettlementEventStoresTheName` — `RecordSettlementEvent.run(rec, "Rivalidad", catalog)` sets `settlementEvent`.
- `recordSettlementEventRejectsUnknownName` — an unknown name is `Blocked` and unchanged.
- `principleCategoryOfMapsTheThreePrincipleMilestones` — DEATH/NEW_LIFE/SOCIETY map; HOODED_KNIGHT/GAME_OVER are `null`.

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :core:jvmTest --tests "io.kdmtracker.core.usecase.DecisionUseCaseTest"`
Expected: FAIL — unresolved symbols.

- [ ] **Step 3: Implement**

- `Core16Milestones.principleCategoryOf` in `MilestoneId.kt` (needs `import io.kdmtracker.core.domain.PrincipleCategory` — same package, no import):
  ```kotlin
  fun principleCategoryOf(id: MilestoneId): PrincipleCategory? =
      when (id) {
          PRINCIPLE_DEATH -> PrincipleCategory.DEATH
          PRINCIPLE_NEW_LIFE -> PrincipleCategory.NEW_LIFE
          PRINCIPLE_SOCIETY -> PrincipleCategory.SOCIETY
          else -> null
      }
  ```
- `ChoosePrinciple.run`: block when `ruleset.principles[category].orEmpty().none { it.label == label }` with `"unknown principle choice"`; else `Ok(record.copy(state = state.copy(principles = state.principles + (category to label))))`.
- `ResolveMilestone.run`: block when `ruleset.milestones.none { it.id == id }` (`"unknown milestone"`); block when `id !in record.reachedMilestones(ruleset)` (`"milestone not reached"`); else `Ok(record.copy(state = state.copy(resolvedMilestones = state.resolvedMilestones + id)))`.
- `RecordSettlementEvent.run`: block when `name !in catalog.settlementEvents` (`"unknown settlement event $name"`); else `Ok(record.copy(settlement = settlement.copy(settlementEvent = name)))`.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :core:jvmTest --tests "io.kdmtracker.core.usecase.DecisionUseCaseTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add core/src/commonMain/kotlin/io/kdmtracker/core/usecase/ChoosePrinciple.kt \
        core/src/commonMain/kotlin/io/kdmtracker/core/usecase/ResolveMilestone.kt \
        core/src/commonMain/kotlin/io/kdmtracker/core/usecase/RecordSettlementEvent.kt \
        core/src/commonMain/kotlin/io/kdmtracker/core/domain/MilestoneId.kt \
        core/src/commonTest/kotlin/io/kdmtracker/core/usecase/DecisionUseCaseTest.kt
git commit -m "feat(core): choose principles, resolve milestones and record settlement events"
```

---

## Task 6: App derived views, controller methods and the year advance

**Files:**
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/UiState.kt`
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/LanternYearController.kt`
- Test: `app-android/src/test/kotlin/io/kdmtracker/app/SettlementBookkeepingControllerTest.kt`

**Interfaces:**
- Consumes: all `:core` use cases/types from Tasks 1–5.
- Produces: `MilestoneView`; `UiState.{settlementEventDeck, settlementEvent, deaths, births, milestoneViews, chosenPrinciples}`; `LanternYearController.{recordDeaths, recordBirths, choosePrinciple, resolveMilestone, recordSettlementEvent}`.

- [ ] **Step 1: Write the failing tests**

`SettlementBookkeepingControllerTest` (construct `LanternYearController(SettlementRepository(InMemoryCampaignStore()))`; the seeded campaign composes core content, so events/milestones are reachable):
- `exposesTheSettlementEventDeck` — `state.value.settlementEventDeck` is non-empty and contains `"Rivalidad"`.
- `recordingADeathUpdatesCountersAndView` — `controller.recordDeaths(1)` then `state.value.deaths == 1`, `population` dropped, and `state.value.milestoneViews.single { it.id == PRINCIPLE_DEATH }.reached` is true.
- `recordingABirthMarksNewLifeReached` — `controller.recordBirths(1)` → the `PRINCIPLE_NEW_LIFE` milestone view is `reached`.
- `choosingAPrincipleIsPersistedAndExposed` — `controller.choosePrinciple(DEATH, "The First Grave → Graves")` → `state.value.chosenPrinciples[DEATH]` set and `repository.record.state.principles[DEATH]` set.
- `resolvingAReachedMilestoneMarksItResolved` — death first, then `resolveMilestone(PRINCIPLE_DEATH)` → view `resolved`.
- `recordingASettlementEventStoresIt` — `controller.recordSettlementEvent("Rivalidad")` → `state.value.settlementEvent == "Rivalidad"`.
- `advancingPastStepFourIncrementsTheYearOnce` — put the record at `currentStep = 4`, call `advance()`, assert `lanternYear == 2`; drive back to step 4 (`previous`), `advance()` again, still `2` (**Review Focus 2**).
- `aBadPrincipleChoiceSurfacesAnErrorAndChangesNothing` — unknown label → `state.value.actionError != null`, record unchanged (**Review Focus 4**).
- `stepFlowDeathPrincipleResolveFlipsEachView` — `recordDeaths(1)` → the DEATH view `reached`, `choosePrinciple(DEATH, label)` → the view's `choices` match and the choice is persisted, `resolveMilestone(PRINCIPLE_DEATH)` → the view `resolved`.
- `stepTenExposesStorage` — after `addResource`, put the record at `currentStep = 10` and assert `state.value.storageLines` is non-empty.
- `stepElevenReportsTheUnspentEndeavors` — at `currentStep = 11` with `endeavors = 3`, `state.value.endeavors == 3`.

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :app-android:testDebugUnitTest --tests "io.kdmtracker.app.SettlementBookkeepingControllerTest"`
Expected: FAIL — unresolved symbols.

- [ ] **Step 3: Implement**

In `UiState.kt`: add the six fields (defaulted) plus:

```kotlin
data class MilestoneView(
    val id: MilestoneId,
    val description: String,
    val reached: Boolean,
    val resolved: Boolean,
    val choices: List<PrincipleChoice>,
)
```

In `LanternYearController`:
- Add methods delegating through `applyResult` (mirror `spendEndeavor`):
  ```kotlin
  fun recordDeaths(count: Int) = applyResult(RecordDeaths.run(repository.record, count))
  fun recordBirths(count: Int) = applyResult(RecordBirths.run(repository.record, count))
  fun choosePrinciple(category: PrincipleCategory, label: String) =
      applyResult(ChoosePrinciple.run(repository.record, category, label, ruleset))
  fun resolveMilestone(id: MilestoneId) =
      applyResult(ResolveMilestone.run(repository.record, id, ruleset))
  fun recordSettlementEvent(name: String) =
      applyResult(RecordSettlementEvent.run(repository.record, name, content))
  ```
- Year advance: add a private helper and call it from `applyStep` and `moveToStep`.
  ```kotlin
  private fun advanceTimelineIfNeeded(fromStep: Int, record: SettlementRecord): SettlementRecord =
      if (fromStep == UPDATE_TIMELINE_STEP && record.settlement.currentStep > UPDATE_TIMELINE_STEP) {
          AdvanceTimeline.run(record)
      } else {
          record
      }
  ```
  with `private const val UPDATE_TIMELINE_STEP = 4`. In `applyStep`, capture `val fromStep = settlement.currentStep` before the move and wrap the persisted `moved` with `advanceTimelineIfNeeded(fromStep, moved)`. In `moveToStep(step)`, apply the same wrapper using the pre-move `currentStep`.
- In `uiState(...)`: add
  ```kotlin
  settlementEventDeck = content.settlementEventDefinitions,
  settlementEvent = settlement.settlementEvent,
  deaths = record.state.deaths,
  births = record.state.births,
  chosenPrinciples = record.state.principles,
  milestoneViews =
      ruleset.milestones.map { definition ->
          MilestoneView(
              id = definition.id,
              description = definition.description,
              reached = definition.id in record.reachedMilestones(ruleset),
              resolved = definition.id in record.state.resolvedMilestones,
              choices = Core16Milestones.principleCategoryOf(definition.id)
                  ?.let { ruleset.principles[it] }.orEmpty(),
          )
      },
  ```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :app-android:testDebugUnitTest --tests "io.kdmtracker.app.SettlementBookkeepingControllerTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app-android/src/main/kotlin/io/kdmtracker/app/UiState.kt \
        app-android/src/main/kotlin/io/kdmtracker/app/LanternYearController.kt \
        app-android/src/test/kotlin/io/kdmtracker/app/SettlementBookkeepingControllerTest.kt
git commit -m "feat(app): expose settlement bookkeeping views and actions"
```

---

## Task 7: Step bodies for 4, 5, 6, 10, 11 and cockpit wiring

**Files:**
- Create: `app-android/src/main/kotlin/io/kdmtracker/app/ui/TimelineStepScreen.kt`
- Create: `app-android/src/main/kotlin/io/kdmtracker/app/ui/DeathCountStepScreen.kt`
- Create: `app-android/src/main/kotlin/io/kdmtracker/app/ui/MilestonesStepScreen.kt`
- Create: `app-android/src/main/kotlin/io/kdmtracker/app/ui/ArchiveStepScreen.kt`
- Create: `app-android/src/main/kotlin/io/kdmtracker/app/ui/EndPhaseStepScreen.kt`
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/ui/SettlementScreen.kt`
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/LanternYearViewModel.kt`
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/KdmTrackerApp.kt`

**Interfaces:**
- Consumes: `UiState` views and controller methods (Task 6).
- Produces: five composables; `SettlementScreen` dispatch; ViewModel methods `recordDeaths`, `recordBirths`, `choosePrinciple`, `resolveMilestone`, `recordSettlementEvent`, `drawSettlementEvent`.

> This task adds no unit test: the project deliberately has no Compose UI test harness, and the controller behaviour behind every screen is already pinned by `SettlementBookkeepingControllerTest` (Task 6). Verification here is compilation, `spotlessCheck`, the full suite, and the manual emulator walk-through.

- [ ] **Step 1: Implement the composables**

Each is stateless, `fillMaxWidth`, Material3, and reports intents through callbacks (mirror `SetUpStepScreen`/`DevelopStepScreen`):
- `TimelineStepScreen(state, onRecordEvent: (String) -> Unit, onDrawEvent: () -> Unit)` — shows `state.lanternYear`, a button `Draw` calling `onDrawEvent`, a chip/list of `state.settlementEventDeck.map { it.name }` calling `onRecordEvent(name)`, the selected `state.settlementEvent` with its full text, and `state.yearEvents`.
- `DeathCountStepScreen(state, onRecordDeaths: (Int) -> Unit, onRecordBirths: (Int) -> Unit, onChoosePrinciple: (PrincipleCategory, String) -> Unit)` — a number input + "Record deaths" and "Record births" buttons; shows `deaths`/`births`/`population`; when `deaths >= 1`, lists the `DEATH` principle choices (from `state.milestoneViews` where `id == Core16Milestones.PRINCIPLE_DEATH`, using `.choices`) as buttons calling `onChoosePrinciple(DEATH, it.label)`; shows the current `chosenPrinciples` choice.
- `MilestonesStepScreen(state, onChoosePrinciple, onResolveMilestone)` — lists `state.milestoneViews`: id/description, a reached/resolved label, the choice buttons when `choices` is non-empty, and a "Resolve" button calling `onResolveMilestone(view.id)`.
- `ArchiveStepScreen(state)` — read-only `state.storageLines` (`name × quantity`).
- `EndPhaseStepScreen(state)` — a warning `"${state.endeavors} unspent endeavors will be discarded"` plus a storage/population summary.

Draw convenience: `TimelineStepScreen` computes the random pick UI-side (`state.settlementEventDeck.random().name` on click via `onRecordEvent`); no RNG in `:core` or the controller.

- [ ] **Step 2: Wire the dispatch and callbacks**

In `SettlementScreen`, add parameters `onRecordEvent`, `onDrawEvent`, `onRecordDeaths`, `onRecordBirths`, `onChoosePrinciple`, `onResolveMilestone` (defaulted to `{}`) and replace the `PendientePlaceholder` branch with a `when (state.currentStep)`:
```
4 -> TimelineStepScreen(...)
5 -> DeathCountStepScreen(...)
6 -> MilestonesStepScreen(...)
7 -> DevelopStepScreen(...)
10 -> ArchiveStepScreen(state)
11 -> EndPhaseStepScreen(state)
else -> PendientePlaceholder(...)
```
Add the matching `mutate { }` delegates in `LanternYearViewModel` (`fun recordDeaths(count: Int) = mutate { it.recordDeaths(count) }`, etc.; draw is `fun drawSettlementEvent()` that picks a random deck name from `_state.value` and calls `recordSettlementEvent`). Pass them from `KdmTrackerApp`'s `SettlementScreen(...)` call.

- [ ] **Step 3: Run the full suite**

Run: `./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Manual emulator check**

Run: `./gradlew :app-android:installDebug`, open a settlement, and walk steps 1 → 11: record a settlement event (Step 4), confirm the year becomes 2; record a death and pick `Principle: Death` (Step 5); resolve a milestone (Step 6); review storage (Step 10); see the discard warning (Step 11).

- [ ] **Step 5: Commit**

```bash
git add app-android/src/main/kotlin/io/kdmtracker/app/ui/TimelineStepScreen.kt \
        app-android/src/main/kotlin/io/kdmtracker/app/ui/DeathCountStepScreen.kt \
        app-android/src/main/kotlin/io/kdmtracker/app/ui/MilestonesStepScreen.kt \
        app-android/src/main/kotlin/io/kdmtracker/app/ui/ArchiveStepScreen.kt \
        app-android/src/main/kotlin/io/kdmtracker/app/ui/EndPhaseStepScreen.kt \
        app-android/src/main/kotlin/io/kdmtracker/app/ui/SettlementScreen.kt \
        app-android/src/main/kotlin/io/kdmtracker/app/LanternYearViewModel.kt \
        app-android/src/main/kotlin/io/kdmtracker/app/KdmTrackerApp.kt
git commit -m "feat(app): add the bookkeeping step bodies (4, 5, 6, 10, 11)"
```

---

## Self-Review

- **Spec coverage:** content type/bundle/catalog (Task 1) · ingest + generated pack + docs (Task 2) · `SettlementState` + transfer v6 (Task 3) · `RecordDeaths`/`RecordBirths`/`AdvanceTimeline` (Task 4) · `ChoosePrinciple`/`ResolveMilestone`/`RecordSettlementEvent` + `principleCategoryOf` (Task 5) · `UiState`/controller/year-advance (Task 6) · five bodies + dispatch/wiring (Task 7). Steps 2/8/9 and the deviation notes are declared out of scope in the spec.
- **Review Focus tests:** (1) `recordDeathsFloorsPopulationAtZero`; (2) `advanceTimelineIncrementsOnceThenIsIdempotent` + `advancingPastStepFourIncrementsTheYearOnce`; (3) `advanceTimelineRunsAgainAfterMarkersReset`; (4) `choosePrincipleRejectsUnknownLabel` / `resolveMilestoneRejectsAnUnreachedMilestone` / `aBadPrincipleChoiceSurfacesAnErrorAndChangesNothing`; (5) `v5DocumentWithoutPrinciplesDecodes`.
- **Type consistency:** `SettlementEvent(name, text)`, `settlementEventDefinitions`/`settlementEvents`, `AdvanceTimeline.run(record): SettlementRecord`, `ChoosePrinciple.run(record, category, label, ruleset)`, `MilestoneView(id, description, reached, resolved, choices)` are used with the same shapes across tasks.
