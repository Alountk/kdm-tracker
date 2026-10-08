# Survivor Roster + Steps 2 & 8 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the Survivor domain aggregate (the full record sheet as data), a Survivor Editor, and the Settlement Phase bodies for Step 2 (Survivors Return) and Step 8 (Prepare Departing Survivors), with population derived from the roster.

**Architecture:** The `Survivor` aggregate and every rule live in `:core` (pure, additive); `SettlementState` carries the roster; population projects from it. `:app-android` renders stateless Step 2 / Step 8 bodies and a Survivor Editor, reporting intents through `LanternYearController`.

**Tech Stack:** Kotlin Multiplatform (`:core`), Compose Multiplatform (Android), kotlinx.serialization, SQLDelight (unchanged), kotlin.test.

**Spec:** `docs/superpowers/specs/2026-10-08-survivor-roster-steps-2-8-design.md`

## Global Constraints

- Rules stay in `:core`; screens render state and report intent only.
- No Compose UI tests (repo convention); behavior is pinned through `:core` and the controller.
- UI copy is **English**, consistent with the existing chrome.
- Conventional Commits in English; **no** `Co-Authored-By` / AI attribution.
- Pre-commit hook runs `spotlessKotlinCheck`; run `./gradlew spotlessApply` before committing or the commit is rejected.
- Survivor ids are `Ulid`; a new survivor's id comes from an injected `() -> Ulid` so `:core` stays pure/deterministic in tests.
- Full suite must stay green: `./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck`.

## Review Focus

Failure modes a person will hit that the tasks must pin:

1. **Population drift** — killing/adding a survivor must keep the derived population and the milestone snapshot correct; a legacy record with a scalar population and no roster must seed the roster (Task 2, Task 3, Task 5).
2. **Step 2 does not wipe progression** — healing resets only light/heavy wounds and tokens; attributes, disorders, fighting arts, XP, courage/understanding must survive (Task 4).
3. **Departing cap** — never more than `min(4, living)` departing survivors; the rule lives in `:core` (Task 4).
4. **Editor validation** — disorders/fighting arts over 3, negative ints, or an unknown survivor id are refused with the record untouched (Task 4).
5. **Legacy transfer** — a v6 document (no roster) still decodes and seeds the roster from its stored population (Task 5).

---

## Task 1: Survivor model + roster on the settlement state

**Files:**
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/domain/Survivor.kt`
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/domain/SettlementState.kt` (add `survivors`)
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/domain/SurvivorSerializationTest.kt`

**Interfaces:**
- Consumes: `Ulid`, `Sex`.
- Produces: `SurvivorStatus`; `HitLocation`; `WoundState`; `Attributes`; `WeaponProficiency`; `Survivor`; `SettlementState.survivors: List<Survivor>`.

- [ ] **Step 1: Write the failing test**

`SurvivorSerializationTest`:
- `populatedSurvivorRoundTripsThroughJson` — a survivor with every field set round-trips equal.
- `defaultSurvivorRoundTripsThroughJson` — `Survivor(id = Ulid.generate(1L) { 0 })` round-trips equal.
- `jsonMissingFieldsDecodesWithDefaults` — `{"id":"…"}` decodes with `status == ALIVE`, empty `wounds`/`disorders`/`gear`.
- `stateWithSurvivorsRoundTrips` — a `SettlementState(survivors = listOf(…))` round-trips.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :core:jvmTest --tests "io.kdmtracker.core.domain.SurvivorSerializationTest"`
Expected: FAIL — unresolved `Survivor`.

- [ ] **Step 3: Implement the model**

```kotlin
package io.kdmtracker.core.domain

import kotlinx.serialization.Serializable

enum class SurvivorStatus { ALIVE, DEPARTING, DEAD }

enum class HitLocation { HEAD, TORSO, ARMS, WAIST, LEGS }

@Serializable
data class WoundState(val armor: Int = 0, val light: Boolean = false, val heavy: Boolean = false)

@Serializable
data class Attributes(
    val movement: Int = 0,
    val accuracy: Int = 0,
    val strength: Int = 0,
    val evasion: Int = 0,
    val luck: Int = 0,
    val speed: Int = 0,
)

@Serializable
data class WeaponProficiency(
    val type: String = "",
    val xp: Int = 0,
    val specialist: Boolean = false,
    val master: Boolean = false,
)

@Serializable
data class Survivor(
    val id: Ulid,
    val name: String = "",
    val sex: Sex = Sex.OTHER,
    val nameBonusApplied: Boolean = false,
    val retired: Boolean = false,
    val survival: Int = 0,
    val insanity: Int = 0,
    val brainTrauma: Boolean = false,
    val attributes: Attributes = Attributes(),
    val wounds: Map<HitLocation, WoundState> = emptyMap(),
    val huntXp: Int = 0,
    val weapon: WeaponProficiency = WeaponProficiency(),
    val courage: Int = 0,
    val understanding: Int = 0,
    val disorders: List<String> = emptyList(),
    val fightingArts: List<String> = emptyList(),
    val notes: String = "",
    val tokens: Set<String> = emptySet(),
    val gear: List<String> = emptyList(),
    val status: SurvivorStatus = SurvivorStatus.ALIVE,
)
```

Add `val survivors: List<Survivor> = emptyList(),` to `SettlementState` and a `@property survivors` KDoc line.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :core:jvmTest --tests "io.kdmtracker.core.domain.SurvivorSerializationTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add core/src/commonMain/kotlin/io/kdmtracker/core/domain/Survivor.kt core/src/commonMain/kotlin/io/kdmtracker/core/domain/SettlementState.kt core/src/commonTest/kotlin/io/kdmtracker/core/domain/SurvivorSerializationTest.kt
git commit -m "feat(core): add the survivor aggregate and roster"
```

---

## Task 2: Derived population and effective attributes

**Files:**
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/domain/SettlementRecord.kt`
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/rules/SurvivorValues.kt`
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/rules/SurvivorValuesTest.kt`

**Interfaces:**
- Consumes: `Survivor`, `ModifierTarget`, `ModifierScope`, `ContentCatalog`, `DerivedValues`.
- Produces: `SettlementRecord.population: Int`; `SurvivorValues.effectiveAttribute(survivor, target, catalog): Int`.

- [ ] **Step 1: Write the failing test**

`SurvivorValuesTest`:
- `populationCountsLivingSurvivorsOnly` — a record with 2 `ALIVE`, 1 `DEPARTING`, 1 `DEAD` has `population == 3`. (**Review Focus 1**)
- `milestoneStateUsesTheRosterPopulation` — `record.milestoneState().population` equals the living count.
- `effectiveAttributeAddsSurvivorScopedGearModifiers` — a survivor with `attributes.strength = 2` and gear whose catalog modifiers include `Modifier(STRENGTH, +1, SURVIVOR)` has `effectiveAttribute(…, STRENGTH, catalog) == 3`.
- `effectiveAttributeIgnoresSettlementScopedModifiers` — a `Settlement`-scope `STRENGTH` modifier does not change the result.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :core:jvmTest --tests "io.kdmtracker.core.rules.SurvivorValuesTest"`
Expected: FAIL — unresolved `population` / `SurvivorValues`.

- [ ] **Step 3: Implement**

In `SettlementRecord`, add:

```kotlin
/** The living survivors: everyone in the roster whose status is not [SurvivorStatus.DEAD]. */
val population: Int get() = state.survivors.count { it.status != SurvivorStatus.DEAD }
```

and change `milestoneState()` to use `population = population` (the derived value) instead of `settlement.population`.

`SurvivorValues`:
```kotlin
object SurvivorValues {
    fun effectiveAttribute(survivor: Survivor, target: ModifierTarget, catalog: ContentCatalog): Int =
        base(survivor.attributes, target) +
            DerivedValues.modifierTotal(catalog.gearModifiersFor(survivor.gear), target, ModifierScope.SURVIVOR)

    private fun base(attributes: Attributes, target: ModifierTarget): Int =
        when (target) {
            ModifierTarget.STRENGTH -> attributes.strength
            ModifierTarget.SPEED -> attributes.speed
            ModifierTarget.ACCURACY -> attributes.accuracy
            ModifierTarget.EVASION -> attributes.evasion
            ModifierTarget.LUCK -> attributes.luck
            ModifierTarget.MOVEMENT -> attributes.movement
            else -> 0
        }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :core:jvmTest --tests "io.kdmtracker.core.rules.SurvivorValuesTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add core/src/commonMain/kotlin/io/kdmtracker/core/domain/SettlementRecord.kt core/src/commonMain/kotlin/io/kdmtracker/core/rules/SurvivorValues.kt core/src/commonTest/kotlin/io/kdmtracker/core/rules/SurvivorValuesTest.kt
git commit -m "feat(core): derive population and effective attributes from the roster"
```

---

## Task 3: Roster-aware deaths and births

**Files:**
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/usecase/RecordDeaths.kt`
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/usecase/RecordBirths.kt`
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/usecase/RosterLifecycleUseCaseTest.kt`
- Modify: `core/src/commonTest/kotlin/io/kdmtracker/core/usecase/CountersAndTimelineUseCaseTest.kt` (update the births/deaths cases)

**Interfaces:**
- Consumes: `Survivor`, `SurvivorStatus`, `Ulid`.
- Produces: `RecordDeaths.run(record, count): StepResult<SettlementRecord>`; `RecordBirths.run(record, count, newId: () -> Ulid): StepResult<SettlementRecord>`.

- [ ] **Step 1: Write the failing test**

`RosterLifecycleUseCaseTest` (build a `SettlementRecord` over a helper with a roster):
- `recordDeathsMarksLivingSurvivorsDead` — 4 alive, `RecordDeaths.run(records, 2)` → 2 `DEAD`, `deaths == 2`, `population == 2`.
- `recordDeathsCapsAtTheLivingCount` — 2 alive, `RecordDeaths.run(records, 5)` → both `DEAD`, `deaths == 5`, `population == 0` (**Review Focus 1**).
- `recordBirthsAppendsUnnamedSurvivors` — `RecordBirths.run(records, 3) { Ulid.generate(1L) { 0 } }` → roster grew by 3, `births == 3`, `population == 7`.
- `recordDeathsRejectsNonPositive` / `recordBirthsRejectsNonPositive` — `Blocked` and unchanged.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :core:jvmTest --tests "io.kdmtracker.core.usecase.RosterLifecycleUseCaseTest"`
Expected: FAIL — the roster is not touched / signature mismatch.

- [ ] **Step 3: Implement**

`RecordDeaths.run` marks the first `count` survivors whose status is not `DEAD` as `DEAD` (order = list order), raising `deaths` by `count`; a non-positive count is `Blocked`. It no longer writes `settlement.population` (population is derived).

`RecordBirths.run(record, count, newId)` appends `count` `Survivor(id = newId())` entries, raising `births` by `count`; non-positive is `Blocked`.

Update `CountersAndTimelineUseCaseTest` to the new signatures/behaviour (its births case now passes an id supplier; its deaths case may keep asserting the counter).

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :core:jvmTest --tests "io.kdmtracker.core.usecase.RosterLifecycleUseCaseTest" --tests "io.kdmtracker.core.usecase.CountersAndTimelineUseCaseTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add core/src/commonMain/kotlin/io/kdmtracker/core/usecase/RecordDeaths.kt core/src/commonMain/kotlin/io/kdmtracker/core/usecase/RecordBirths.kt core/src/commonTest/kotlin/io/kdmtracker/core/usecase/RosterLifecycleUseCaseTest.kt core/src/commonTest/kotlin/io/kdmtracker/core/usecase/CountersAndTimelineUseCaseTest.kt
git commit -m "feat(core): record deaths and births on the survivor roster"
```

---

## Task 4: Roster mutation use cases (return, depart, gear, edit)

**Files:**
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/usecase/ReturnSurvivors.kt`
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/usecase/MarkDeparting.kt`
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/usecase/AssignGear.kt`
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/usecase/UpdateSurvivor.kt`
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/ruleset/Ruleset.kt` (+ `Core16Ruleset`) — add `departingLimit`
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/usecase/RosterMutationUseCaseTest.kt`

**Interfaces:**
- Consumes: `Survivor`, `HitLocation`, `WoundState`, `Ulid`, `Ruleset`.
- Produces: `ReturnSurvivors.run(record): SettlementRecord`; `MarkDeparting.run(record, ids: List<Ulid>, limit: Int): StepResult<SettlementRecord>`; `AssignGear.run(record, survivorId, gearName): StepResult<SettlementRecord>`; `UnassignGear.run(...)`; `UpdateSurvivor.run(record, survivor): StepResult<SettlementRecord>`; `Ruleset.departingLimit: Int`.

- [ ] **Step 1: Write the failing test**

`RosterMutationUseCaseTest`:
- `returnSurvivorsHealsWoundsAndClearsTokensButKeepsProgression` — a survivor with a `heavy` wound, a token, strength 2, a disorder and `huntXp = 3` comes back with light/heavy `false`, no tokens, and the progression intact (**Review Focus 2**).
- `markDepartingSetsTheChosenAndResetsTheRest` — marks the given ids `DEPARTING`, others `ALIVE`.
- `markDepartingRejectsMoreThanTheLimit` — 4 ids with `limit = 3` is `Blocked`, unchanged (**Review Focus 3**).
- `markDepartingCapsAtTheLivingCount` — 2 living, 3 ids requested → `Blocked` (or capped, pin the chosen behaviour).
- `assignGearAddsAndUnassignRemoves` — round-trip on a survivor's gear.
- `updateSurvivorRejectsTooManyDisorders` — 4 disorders is `Blocked`, unchanged (**Review Focus 4**).
- `updateSurvivorRejectsAnUnknownId` — `Blocked`.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :core:jvmTest --tests "io.kdmtracker.core.usecase.RosterMutationUseCaseTest"`
Expected: FAIL — unresolved use cases.

- [ ] **Step 3: Implement**

- `Ruleset.departingLimit: Int` (Core16Ruleset = 4).
- `ReturnSurvivors.run(record)`: map every non-`DEAD` survivor to `wounds.mapValues { it.value.copy(light = false, heavy = false) }` and `tokens = emptySet()`; everything else untouched.
- `MarkDeparting.run(record, ids, limit)`: `living = record.population`; block when `ids.size > min(limit, living)` or any id is not a living survivor (`"invalid departing selection"`); else set the chosen to `DEPARTING` and every other non-`DEAD` to `ALIVE`.
- `AssignGear.run` / `UnassignGear.run`: block on an unknown survivor id; add/remove the name (dedup) on that survivor's `gear`.
- `UpdateSurvivor.run(record, survivor)`: block on an unknown id, `disorders.size > 3`, `fightingArts.size > 3`, or any negative int field (`survival`, `insanity`, `huntXp`, `courage`, `understanding`, attribute values, armor, weapon xp); else replace by id.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :core:jvmTest --tests "io.kdmtracker.core.usecase.RosterMutationUseCaseTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add core/src/commonMain/kotlin/io/kdmtracker/core/usecase/ReturnSurvivors.kt core/src/commonMain/kotlin/io/kdmtracker/core/usecase/MarkDeparting.kt core/src/commonMain/kotlin/io/kdmtracker/core/usecase/AssignGear.kt core/src/commonMain/kotlin/io/kdmtracker/core/usecase/UpdateSurvivor.kt core/src/commonMain/kotlin/io/kdmtracker/core/ruleset/Ruleset.kt core/src/commonMain/kotlin/io/kdmtracker/core/ruleset/Core16Ruleset.kt core/src/commonTest/kotlin/io/kdmtracker/core/usecase/RosterMutationUseCaseTest.kt
git commit -m "feat(core): return, depart, gear and edit the survivor roster"
```

---

## Task 5: Transfer v7 and roster migration

**Files:**
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/transfer/TransferDocument.kt` (v7)
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/transfer/TransferMapper.kt` (seed on decode)
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/domain/SettlementRecord.kt` (`migrated()`)
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/SettlementRepository.kt` (`startingRecord` seeds the roster; `storedRecord` applies `migrated()` — the load seam is `storedRecord`)
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/transfer/SurvivorMigrationTest.kt`
- Modify: `core/src/commonTest/kotlin/io/kdmtracker/core/transfer/SettlementStateTransferTest.kt` (v7 default)
- Modify: `core/src/commonTest/kotlin/io/kdmtracker/core/transfer/TransferDocumentTest.kt` (supported versions)

**Interfaces:**
- Consumes: `Survivor`, `SettlementRecord.population`.
- Produces: `TransferDocument.schemaVersion = 7`; `SettlementRecord.migrated(): SettlementRecord`.

- [ ] **Step 1: Write the failing test**

`SurvivorMigrationTest`:
- `migratedSeedsTheRosterFromTheLegacyPopulation` — a record whose `state.survivors` is empty and `settlement.population = 4` migrates to 4 survivors, `population == 4` (**Review Focus 1**).
- `migratedLeavesANonEmptyRosterUntouched` — a record with survivors is unchanged.
- `v6DocumentWithoutRosterDecodesAndSeeds` — a v6 `TransferDocument` JSON (state with no `survivors`, settlement `population` 3) decodes and `toDomain` yields 3 survivors (**Review Focus 5**).
- `v7RoundTripCarriesTheRoster` — a populated roster survives `toDto`/`toDomain` and JSON with `schemaVersion = 7`.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :core:jvmTest --tests "io.kdmtracker.core.transfer.SurvivorMigrationTest"`
Expected: FAIL — unresolved `migrated`.

- [ ] **Step 3: Implement**

- `TransferDocument`: `schemaVersion = 7`, `SUPPORTED_VERSIONS += 7`, KDoc note.
- `SettlementRecord.migrated()`: when `state.survivors.isEmpty()` and `settlement.population > 0`, seed that many `Survivor(id = Ulid.generate(counter…))`; else return `this`.
- `TransferMapper.toDto` writes `population = record.population`; `toDomain` applies `migrated()`.
- `SettlementRepository.startingRecord` seeds 4 survivors (the founding survivors) so a new settlement has a roster; `storedRecord` applies `migrated()`.
- Update `SettlementStateTransferTest`'s default-version assertion and `TransferDocumentTest.supportedVersionsCoverV1ThroughV6` → `…V7`.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :core:jvmTest --tests "io.kdmtracker.core.transfer.SurvivorMigrationTest" --tests "io.kdmtracker.core.transfer.SettlementStateTransferTest" --tests "io.kdmtracker.core.transfer.TransferDocumentTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add core/src/commonMain/kotlin/io/kdmtracker/core/transfer/TransferDocument.kt core/src/commonMain/kotlin/io/kdmtracker/core/transfer/TransferMapper.kt core/src/commonMain/kotlin/io/kdmtracker/core/domain/SettlementRecord.kt app-android/src/main/kotlin/io/kdmtracker/app/SettlementRepository.kt core/src/commonTest/kotlin/io/kdmtracker/core/transfer/SurvivorMigrationTest.kt core/src/commonTest/kotlin/io/kdmtracker/core/transfer/SettlementStateTransferTest.kt core/src/commonTest/kotlin/io/kdmtracker/core/transfer/TransferDocumentTest.kt
git commit -m "feat(core): migrate the roster from the legacy population (transfer v7)"
```

---

## Task 6: App roster views and controller methods

**Files:**
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/UiState.kt` (add `survivors`)
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/LanternYearController.kt`
- Test: `app-android/src/test/kotlin/io/kdmtracker/app/SurvivorRosterControllerTest.kt`

**Interfaces:**
- Consumes: all `:core` roster use cases/types.
- Produces: `UiState.survivors: List<Survivor>`; a new constructor parameter `newSurvivorId: () -> Ulid`; `LanternYearController.{healSurvivors, markDeparting, assignGear, unassignGear, updateSurvivor, recordDeaths, recordBirths}`.

- [ ] **Step 1: Write the failing test**

`SurvivorRosterControllerTest` (mirror `SettlementBookkeepingControllerTest`; pass a deterministic `newSurvivorId = { Ulid.generate(1L) { 0 } }`):
- `exposesTheSeededRoster` — a fresh settlement exposes 4 survivors.
- `healingSurvivorsClearsWoundsAndTokens` — set a wounded survivor, `healSurvivors()`, assert cleared.
- `markingDepartingUpdatesWhoDeparts` — `markDeparting(ids)` sets those `DEPARTING`.
- `markingMoreThanFourIsBlocked` — 5 ids → `actionError`, unchanged.
- `assigningGearPersists` — `assignGear(id, "Armería…")` shows on the survivor.
- `recordingADeathShrinksTheRoster` — `recordDeaths(1)` → 3 living, `population == 3`.
- `recordingABirthGrowsTheRoster` — `recordBirths(1)` → 5 living.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app-android:testDebugUnitTest --tests "io.kdmtracker.app.SurvivorRosterControllerTest"`
Expected: FAIL — unresolved `survivors` / methods.

- [ ] **Step 3: Implement**

- `UiState.survivors: List<Survivor> = emptyList()`; populate in `LanternYearController.uiState` from `record.state.survivors`.
- Add `newSurvivorId: () -> Ulid = { Ulid.generate(System.currentTimeMillis()) { Random.nextInt() } }` to the `LanternYearController` constructor (after `catalog`).
- Controller methods delegating through `applyResult` / `StepResult`:
  `healSurvivors()` (ReturnSurvivors), `markDeparting(ids)` (MarkDeparting with `ruleset.departingLimit`), `assignGear(id, name)` / `unassignGear(id, name)`, `updateSurvivor(survivor)`, and `recordBirths(count)` → `RecordBirths.run(repository.record, count, newSurvivorId)`.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app-android:testDebugUnitTest --tests "io.kdmtracker.app.SurvivorRosterControllerTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app-android/src/main/kotlin/io/kdmtracker/app/UiState.kt app-android/src/main/kotlin/io/kdmtracker/app/LanternYearController.kt app-android/src/test/kotlin/io/kdmtracker/app/SurvivorRosterControllerTest.kt
git commit -m "feat(app): expose the survivor roster and roster actions"
```

---

## Task 7: Step 2, Step 8 and the Survivor Editor

**Files:**
- Create: `app-android/src/main/kotlin/io/kdmtracker/app/ui/SurvivorsReturnStepScreen.kt`
- Create: `app-android/src/main/kotlin/io/kdmtracker/app/ui/PrepareDepartingStepScreen.kt`
- Create: `app-android/src/main/kotlin/io/kdmtracker/app/ui/SurvivorEditorScreen.kt`
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/ui/SettlementScreen.kt` (dispatch 2 and 8)
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/LanternYearViewModel.kt` + `KdmTrackerApp.kt` (callbacks)

**Interfaces:**
- Consumes: `UiState.survivors`, Task 6 controller methods.
- Produces: the three composables; the cockpit dispatch; the ViewModel callbacks.

> This task adds no unit test: the project deliberately has no Compose UI test harness, and the controller behaviour behind every control is pinned by `SurvivorRosterControllerTest` (Task 6). Verification is compilation, `spotlessCheck`, the full suite, and the manual emulator pass.

- [ ] **Step 1: Implement `SurvivorsReturnStepScreen`**

```kotlin
@Composable
fun SurvivorsReturnStepScreen(
    state: UiState,
    onHeal: () -> Unit,
    onEdit: (Survivor) -> Unit,
    onAddSurvivor: () -> Unit = {},
)
```
A `Heal & clear tokens` button, the roster list (name · sex · survival/insanity · each location's light/heavy · tokens · key stats), and an `Edit` per survivor. Read-only beyond the callbacks.

- [ ] **Step 2: Implement `PrepareDepartingStepScreen`**

```kotlin
@Composable
fun PrepareDepartingStepScreen(
    state: UiState,
    onToggleDeparting: (Ulid) -> Unit,
    onAssignGear: (Ulid, String) -> Unit,
    onUnassignGear: (Ulid, String) -> Unit,
    onEdit: (Survivor) -> Unit,
)
```
The roster with a depart toggle (disabled past the cap), the settlement's crafted gear offered per departing survivor to assign/unassign, and an `Edit` per survivor.

- [ ] **Step 3: Implement `SurvivorEditorScreen`**

```kotlin
@Composable
fun SurvivorEditorScreen(
    survivor: Survivor,
    survivalLimit: Int,
    onSave: (Survivor) -> Unit,
    onCancel: () -> Unit,
)
```
Editable fields: name, sex, `nameBonusApplied`, `retired`, survival (shown `/ survivalLimit`), insanity, brain trauma, the six attributes, per-location armor + light/heavy, tokens, disorders (≤3), fighting arts (≤3), hunt XP, weapon type/XP/specialist/master, courage, understanding, notes. Owns only the draft state; `onSave` reports the edited survivor.

- [ ] **Step 4: Wire dispatch and callbacks**

In `SettlementScreen`'s `when (state.currentStep)` add `2 -> SurvivorsReturnStepScreen(...)` and `8 -> PrepareDepartingStepScreen(...)`, plus an editor route (a dialog/overlay keyed by the survivor being edited). Add the matching ViewModel delegates and pass them from `KdmTrackerApp`.

- [ ] **Step 5: Run the full suite**

Run: `./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Manual emulator pass**

Run: `./gradlew :app-android:installDebug`, then: open a settlement, jump to Step 2 → `Heal & clear tokens`, edit a survivor (stat, disorder, XP); jump to Step 8 → pick departing survivors and assign gear; confirm the roster and population reflect it. (**Review Focus 2, 3**)

- [ ] **Step 7: Commit**

```bash
git add app-android/src/main/kotlin/io/kdmtracker/app/ui/SurvivorsReturnStepScreen.kt app-android/src/main/kotlin/io/kdmtracker/app/ui/PrepareDepartingStepScreen.kt app-android/src/main/kotlin/io/kdmtracker/app/ui/SurvivorEditorScreen.kt app-android/src/main/kotlin/io/kdmtracker/app/ui/SettlementScreen.kt app-android/src/main/kotlin/io/kdmtracker/app/LanternYearViewModel.kt app-android/src/main/kotlin/io/kdmtracker/app/KdmTrackerApp.kt
git commit -m "feat(app): add the survivors-return, prepare-departing and editor screens"
```

---

## Self-Review

- **Spec coverage:** model (Task 1) · derived population + effective attribute (Task 2) · roster-aware deaths/births (Task 3) · ReturnSurvivors/MarkDeparting/AssignGear/UnassignGear/UpdateSurvivor + departing limit (Task 4) · transfer v7 + migration (Task 5) · app views + controller (Task 6) · Step 2 / Step 8 / Editor screens + wiring (Task 7).
- **Review Focus tests:** (1) `populationCountsLivingSurvivorsOnly`, `recordDeathsCapsAtTheLivingCount`, `migratedSeedsTheRosterFromTheLegacyPopulation`; (2) `returnSurvivorsHealsWoundsAndClearsTokensButKeepsProgression`; (3) `markDepartingRejectsMoreThanTheLimit`; (4) `updateSurvivorRejectsTooManyDisorders`; (5) `v6DocumentWithoutRosterDecodesAndSeeds`.
- **Type consistency:** `Survivor`, `SurvivorStatus`, `HitLocation`, `WoundState`, `Attributes`, `WeaponProficiency`, `SettlementRecord.population`, `SurvivorValues.effectiveAttribute`, `ReturnSurvivors/MarkDeparting/AssignGear/UnassignGear/UpdateSurvivor` are used with the same shapes across tasks.
