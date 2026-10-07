# Settlement Record Persistence (Schema v4) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Persist the `SettlementState` record (timeline, innovations, locations, resource ledger, deaths, births) alongside the settlement scalars — through the transfer DTO and the local SQLDelight store — and make the app hold a `SettlementRecord` (settlement + state) instead of a bare `Settlement`.

**Architecture:** `:core`'s record types become `@Serializable`; `SettlementDto` (transfer) carries the state (schema v5); `:data` gains a `stateJson` column on `settlement` (schema v4 via a `.sqm` migration); the app's `SettlementRepository` holds a `SettlementRecord`. Strict TDD throughout. No UI step bodies yet.

**Tech Stack:** Kotlin 2.3.21, KMP, kotlinx-serialization, SQLDelight 2.4.0, Compose Multiplatform (app). `kotlin.test`.

**Spec:** `docs/superpowers/specs/2026-10-05-kdm-tracker-offline-first-kmp-design.md`
**Depends on:** the settlement-domain slice (PR #7) and the content-ingestion slice (PR #8).

## Global Constraints

- Kotlin `2.3.21`; `:core` stays pure (no Spring/Android/SQLDelight). No new dependencies.
- Strict TDD: failing test first, observe RED, implement, observe GREEN.
- **Additive/back-compatible**: existing DTO/rows without the new field must keep working (defaults).
- The store round-trips the full `SettlementState` losslessly, including the resource ledger and the consequence graph.
- Conventional Commits; **no** `Co-Authored-By`/AI attribution.
- `./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck` stays green.

## Review Focus

1. **Lossless round-trip**: a `SettlementState` (non-empty timeline, innovations with consequences, locations, ledger with gains/spends, deaths/births) survives `toDto → toDomain` and a store write→read unchanged.
2. **Backward compatibility**: a settlement row / DTO without `state` decodes to an **empty** `SettlementState` (defaults), not a crash.
3. **The migration is real**: a v3 `:data` database gains the `stateJson` column; the schema version bumps to 4; a fresh install works.
4. **The ledger is preserved as entries** (not pre-projected storage): the round-trip keeps `resourceEntries`, so storage/survival limit stay derivable.
5. **The app holds a record**: the repository exposes the state; the existing loop/UI behavior is otherwise unchanged.

---

## File Structure

```text
core/src/commonMain/kotlin/io/kdmtracker/core/
├── domain/{Timeline,Innovation,SettlementLocation,Storage,SettlementState}.kt   # + @Serializable
└── transfer/{TransferDocument,TransferMapper}.kt                                 # + state, schema v5
data/src/commonMain/sqldelight/io/kdmtracker/data/db/
├── Settlement.sq                              # + stateJson column
└── 3.sqm                                      # v3 -> v4 migration
data/src/commonMain/kotlin/io/kdmtracker/data/SqlDelightCampaignStore.kt  # map state
app-android/src/main/kotlin/io/kdmtracker/app/SettlementRepository.kt     # hold a SettlementRecord
```

---

### Task 1: Make the record types serializable

**Files:**
- Modify: `core/.../domain/Timeline.kt`, `Innovation.kt`, `SettlementLocation.kt`, `Storage.kt`, `SettlementState.kt`
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/domain/SettlementStateSerializationTest.kt`

**Interfaces:**
- Produces: `@Serializable` on `Timeline`, `TimelineEntry`, `Innovation`, `InnovationCollection`, `SettlementLocation`, `LocationSet`, `ResourceEntry`, `SettlementState` (and any nested type). Enums (`ResourceCategory`) are already serializable.
- Test: JSON round-trip of a populated `SettlementState` (timeline entries, innovations with consequences, locations, ledger) equals the original; an empty state round-trips; a state missing a field (older shape) decodes with defaults.

- [ ] **Step 1:** Write the failing `SettlementStateSerializationTest`; run `./gradlew :core:jvmTest` — expected FAIL (no serializer).
- [ ] **Step 2:** Add `@Serializable` to the types; add default values where a field must be optional for back-compat.
- [ ] **Step 3:** Run — expected GREEN. Commit `feat(core): serialize the settlement record types`.

---

### Task 2: Carry the state in the transfer DTO (schema v5)

**Files:**
- Modify: `core/.../transfer/TransferDocument.kt` (`SettlementDto` gains `state: SettlementState = SettlementState()`; `schemaVersion` → 5, `SUPPORTED_VERSIONS` → `[1,2,3,4,5]`)
- Modify: `core/.../transfer/TransferMapper.kt` (map state both ways)
- Test: `core/.../transfer/SettlementStateTransferTest.kt`

**Interfaces:**
- Produces: `SettlementDto(..., state: SettlementState = SettlementState())`; the mapper round-trips it; a v4-shaped DTO (no `state`) decodes to an empty state.
- Test: `toDomain(toDto(record)) == record` including a populated state; a JSON without `state` decodes with an empty `SettlementState`; `schemaVersion` reads 5 for new, 4 for old.

- [ ] **Step 1:** Write the failing test; run — expected FAIL.
- [ ] **Step 2:** Add the field + mapping + version bump.
- [ ] **Step 3:** Run `./gradlew :core:jvmTest spotlessCheck` — expected GREEN. Commit `feat(core): carry the settlement state in the transfer schema`.

---

### Task 3: `:data` schema v4 (persist the state)

**Files:**
- Modify: `data/src/commonMain/sqldelight/io/kdmtracker/data/db/Settlement.sq` (`stateJson TEXT NOT NULL DEFAULT '{}'`; update `upsertSettlement` with the new column)
- Create: `data/src/commonMain/sqldelight/io/kdmtracker/data/db/3.sqm` (v3 → v4: `ALTER TABLE settlement ADD COLUMN stateJson TEXT NOT NULL DEFAULT '{}'`)
- Modify: `data/.../SqlDelightCampaignStore.kt` (serialize/deserialize the state)
- Test: `data/src/jvmTest/kotlin/io/kdmtracker/data/SettlementStateStoreTest.kt`

**Interfaces:**
- Produces: the settlement row carries `stateJson`; `upsertSettlement(record)` and the read path map the full `SettlementState`; the schema version bumps to 4.
- Test (temp-file JDBC): write a record with a populated state → read back equal; a row written before the column (v3) migrates and decodes to an empty state; the migration bumps the version.

- [ ] **Step 1:** Write the failing `SettlementStateStoreTest`; run `./gradlew :data:jvmTest` — expected FAIL.
- [ ] **Step 2:** Add the column + `.sqm` + store mapping (encode/decode with the existing `Json`).
- [ ] **Step 3:** Run `./gradlew :data:jvmTest :data:assembleDebug spotlessCheck` — expected GREEN. Commit `feat(data): persist the settlement state`.

---

### Task 4: App holds a `SettlementRecord`

**Files:**
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/SettlementRepository.kt` (hold a `SettlementRecord` instead of a bare `Settlement`; `update` writes the record; `reload` reads it)
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/LanternYearViewModel.kt` (adapt to the record)
- Test: `app-android/src/test/kotlin/io/kdmtracker/app/SettlementRecordRepositoryTest.kt`

**Interfaces:**
- Produces: `SettlementRepository.record: SettlementRecord`; `update(record)`; `listCards()`/`select()` unchanged in behavior; the store round-trips the state through the app.
- Test (JVM fakes + the real store): create/select/update a record with a state; the state survives; the existing loop tests still pass.

- [ ] **Step 1:** Write the failing `SettlementRecordRepositoryTest`.
- [ ] **Step 2:** Run `./gradlew :app-android:testDebugUnitTest` — expected FAIL.
- [ ] **Step 3:** Refactor the repository/ViewModel to the record; keep behavior identical.
- [ ] **Step 4:** Run `./gradlew :app-android:testDebugUnitTest :app-android:assembleDebug spotlessCheck` — expected GREEN. Commit `feat(app): hold the settlement record in the repository`.

---

### Task 5: Verification sweep + README

**Files:**
- Modify: `README.md`

- [ ] **Step 1:** Run the full suite `./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck`; record per-module counts.
- [ ] **Step 2:** Document the settlement record persistence (the state fields, schema v4, the transfer schema v5) in the README.
- [ ] **Step 3:** Commit `docs: document the settlement record persistence`.

---

## Self-Review

**1. Spec coverage**

| Spec item | Covered by |
| --- | --- |
| D7 sync columns + local mirror | Task 3 |
| §8 local persistence of the record | Tasks 1–4 |
| The record types used by the steps | Tasks 1–4 |
| Step bodies (the 11 steps) | **Deferred** — the next slice |
| Reading the state in the cockpit UI | **Deferred** — after the record is available |
| Content ingestion follow-ups (secret arts, OCR, material tags) | **Deferred** |

**2. Step scan:** each step is RED → implement → GREEN → commit; the DTO/column shapes are exact.

**3. Type consistency:** `SettlementState` (Task 1) is carried by `SettlementDto` (Task 2) and persisted by `:data` (Task 3), and held by `SettlementRecord` in the app (Task 4); `SettlementRecord` already exists from PR #7.

**4. Review Focus:** each of the five lines maps to a task with tests (2/3, 2/3, 3, 3, 4).

**5. Proportion:** task-level; strict-TDD steps; no code transcripts.
