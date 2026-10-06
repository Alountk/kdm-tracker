# Client Sync Engine (`:core`/`:data`/`:app-android`) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the Android client actually sync: record local mutations in an outbox, pull remote changes, merge them with per-record last-write-wins, and push the outbox — over the server sync hub. Plus the deferred hardening: move DB I/O off the main thread.

**Architecture:** The merge policy is a **pure function in `:core`** so it is JVM-testable and has no I/O. A `SyncApi` port (pull/push) lives in `:core`; the HTTP implementation lives in the app. `:data` gains an `outbox` and a `sync_cursor` table (via a SQLDelight `.sqm` migration) and records every local mutation there atomically. A `SyncEngine` in `:core` orchestrates pull → merge → push over the `SyncApi` port.

**Tech Stack:** Kotlin 2.3.21, Kotlin Multiplatform, SQLDelight 2.4.0 (`:data`), kotlinx-serialization/coroutines, Compose Multiplatform app. HTTP via `java.net.HttpURLConnection` on Android/JVM (no new HTTP framework).

**Spec:** `docs/superpowers/specs/2026-10-05-kdm-tracker-offline-first-kmp-design.md`
**Depends on:** the merged server sync hub (`/api/v1/**`) and the local persistence plan.

## Global Constraints

- Kotlin `2.3.21`; JDK toolchain `21`; versions via the catalog.
- `:core` stays pure (no Android/HTTP libs); the `SyncApi` port is an interface.
- The merge policy is **per-record LWW by HLC `updatedAt`** (`millis`, then `counter`); append-only entities merge by union (idempotent by id); tombstones (`deletedAt`) win over older writes.
- The server is authoritative for `rev`; the client stores the pulled `latestRev` as its cursor.
- Conservative conflict rule: when local and remote `updatedAt` are equal, prefer the **local** value but keep it in the outbox for the next push (no silent loss).
- Conventional Commits; **no** `Co-Authored-By`/AI attribution.
- `./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck` stays green.

## Review Focus

1. **No data loss on merge:** a remote change newer than local overwrites it; a local change newer than remote is preserved and pushed; an equal-timestamp tie is deterministic and never drops the local edit.
2. **The outbox is complete and idempotent:** every mutation records exactly one outbox entry; pushing does not clear an entry until the server acknowledges it; replaying a push after a crash does not duplicate.
3. **The cursor only advances on successful pull** and matches the server's `latestRev`.
4. **The app survives sync failure:** a failed or offline sync leaves the local state usable and the outbox intact.
5. **DB I/O no longer blocks the UI thread:** loads and writes run off the main dispatcher.

---

## File Structure

```text
core/src/commonMain/kotlin/io/kdmtracker/core/sync/
├── SyncApi.kt              # port: pull(campaignId, since) -> PullResponse; push(campaignId, ChangeSet) -> Long
├── MergePolicy.kt          # pure per-record LWW merge
└── SyncEngine.kt           # pull -> merge -> push orchestration
data/src/commonMain/sqldelight/io/kdmtracker/data/db/
├── Outbox.sq               # outbox table
├── SyncCursor.sq           # sync_cursor table
└── 1.sqm                   # migration adding both tables
data/src/commonMain/kotlin/io/kdmtracker/data/
├── SqlDelightOutbox.kt     # outbox + cursor operations
└── SqlDelightCampaignStore.kt   # records outbox entries on write (transactional)
app-android/src/main/kotlin/io/kdmtracker/app/sync/
├── HttpSyncApi.kt          # SyncApi over HttpURLConnection
├── TokenStore.kt           # bearer token storage
└── SyncCoordinator.kt      # runs the engine on IO, exposes sync state
```

---

### Task 1: Harden the server JWT secret guard (known residual)

**Files:**
- Modify: `server/src/main/kotlin/io/kdmtracker/auth/JwtSecretGuard.kt`
- Test: `server/src/test/kotlin/io/kdmtracker/auth/JwtSecretGuardTest.kt`
- Modify: `server/.env.example`

**Interfaces:**
- Produces: the guard rejects a secret that is blank, shorter than 32 chars, or equal to the committed default (unless profile `dev`/`test`). `.env.example` documents a placeholder like `SYNC_JWT_SECRET=<32+ random chars>`.

- [ ] **Step 1:** Write the failing test: blank and short secrets throw outside dev/test; a >=32-char non-default secret passes; the default passes only under `test`.
- [ ] **Step 2:** Run `./gradlew :server:test` — expected FAIL.
- [ ] **Step 3:** Implement the tighter guard; update `.env.example`.
- [ ] **Step 4:** Run — expected PASS. Commit `fix(server): reject blank or short jwt secrets`.

---

### Task 2: Merge policy in `:core`

**Files:**
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/sync/MergePolicy.kt`
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/sync/MergePolicyTest.kt`

**Interfaces:**
- Produces:
  - `data class MergeResult(val record: ChangeRecord?, val localWins: Boolean)`
  - `object MergePolicy { fun merge(local: ChangeRecord?, remote: ChangeRecord): MergeResult }`
  - Rules: no local → take remote (`localWins=false`); remote `updatedAt` > local → take remote; local > remote → keep local (`localWins=true`); equal → keep local (`localWins=true`); a `deletedAt`-bearing record wins over an older non-deleted one.
  - Ordering compares `HybridLogicalClock(updatedAtMillis, updatedAtCounter)`.

- [ ] **Step 1:** Write the failing test covering each rule (`null local`, newer remote, newer local, equal-tie, tombstone).
- [ ] **Step 2:** Run `./gradlew :core:jvmTest` — expected FAIL.
- [ ] **Step 3:** Implement `MergePolicy`.
- [ ] **Step 4:** Run — expected PASS. Commit `feat(core): add per-record lww merge policy`.

---

### Task 3: `SyncApi` port + `SyncEngine`

**Files:**
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/sync/SyncApi.kt`
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/sync/SyncEngine.kt`
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/sync/SyncEngineTest.kt`

**Interfaces:**
- Produces:
  - `interface SyncApi { fun pull(campaignId: String, since: Long): PullResponse; fun push(campaignId: String, changeSet: ChangeSet): Long }`
  - `interface SyncStore { fun cursor(campaignId: String): Long; fun setCursor(campaignId: String, rev: Long); fun pending(campaignId: String): List<ChangeRecord>; fun applyMerged(campaignId: String, record: ChangeRecord); fun clearAcknowledged(campaignId: String, records: List<ChangeRecord>) }`
  - `class SyncEngine(private val api: SyncApi, private val store: SyncStore) { fun sync(campaignId: String): SyncOutcome }`
  - `sync`: pull from `store.cursor` → merge each remote into the local store via `MergePolicy` → set the cursor to `latestRev` → push `store.pending` → on ack, `clearAcknowledged`.
  - `data class SyncOutcome(val pulled: Int, val pushed: Int, val cursor: Long)`
- Test: a fake `SyncApi`/`SyncStore` (in-memory) proves cursor advance, remote-overwrites-local, local-preserved-and-pushed, and that a push failure leaves the outbox intact (Review Focus 1–4).

- [ ] **Step 1:** Write the failing `SyncEngineTest`.
- [ ] **Step 2:** Run — expected FAIL.
- [ ] **Step 3:** Implement the port + engine.
- [ ] **Step 4:** Run — expected PASS. Commit `feat(core): add sync api port and engine`.

---

### Task 4: `:data` outbox + cursor tables (migration)

**Files:**
- Create: `data/src/commonMain/sqldelight/io/kdmtracker/data/db/Outbox.sq`, `SyncCursor.sq`, `1.sqm`
- Modify: `data/src/commonMain/kotlin/io/kdmtracker/data/SqlDelightCampaignStore.kt`
- Test: `data/src/jvmTest/kotlin/io/kdmtracker/data/OutboxStoreTest.kt`

**Interfaces:**
- `outbox(campaignId, entityType, entityId, op, payload, updatedAtMillis, updatedAtCounter, PRIMARY KEY(campaignId, entityType, entityId))`; `sync_cursor(campaignId PRIMARY KEY, rev)`.
- Store gains `recordOutbox(...)`, `pending(campaignId)`, `cursor(campaignId)`, `setCursor(...)`, `clearAcknowledged(...)`.
- `upsertCampaign`/`upsertSettlement` write the row **and** its outbox entry in the same `transaction {}` (Review Focus 2).
- The `.sqm` migration creates both tables for an existing v1 database; verify codegen bumps the schema version and a fresh install + an upgraded install both work.

- [ ] **Step 1:** Write the failing test: schema creates the tables; an upsert records exactly one outbox row; replaying the upsert updates (not duplicates) the entry.
- [ ] **Step 2:** Run `./gradlew :data:jvmTest` — expected FAIL.
- [ ] **Step 3:** Add the `.sq` files, the `.sqm`, and the store methods; wire the outbox into the existing upserts transactionally.
- [ ] **Step 4:** Run `./gradlew :data:jvmTest :data:assembleDebug` — expected PASS. Commit `feat(data): add outbox and sync cursor`.

---

### Task 5: `SqlDelightSyncStore` implementing `SyncStore`

**Files:**
- Create: `data/src/commonMain/kotlin/io/kdmtracker/data/SqlDelightSyncStore.kt`
- Test: `data/src/jvmTest/kotlin/io/kdmtracker/data/SqlDelightSyncStoreTest.kt`

**Interfaces:**
- Consumes: the outbox/cursor tables and `:core` `SyncStore`.
- Produces: `class SqlDelightSyncStore(database) : SyncStore` mapping outbox rows ⇄ `:core` `ChangeRecord`; `applyMerged` upserts the record into the domain tables; `clearAcknowledged` deletes only acked outbox rows.

- [ ] **Step 1:** Write the failing test: pending round-trips; `applyMerged` upserts a settlement; `clearAcknowledged` removes exactly the acked entries.
- [ ] **Step 2:** Run — expected FAIL.
- [ ] **Step 3:** Implement.
- [ ] **Step 4:** Run — expected PASS. Commit `feat(data): implement sync store`.

---

### Task 6: App wiring — HTTP `SyncApi`, token store, off-main-thread I/O

**Files:**
- Create: `app-android/src/main/kotlin/io/kdmtracker/app/sync/HttpSyncApi.kt`, `TokenStore.kt`, `SyncCoordinator.kt`
- Modify: `app-android/build.gradle.kts` (needs `:data` already present), `LanternYearViewModel.kt`, `KdmTrackerApp.kt`
- Test: `app-android/src/test/kotlin/io/kdmtracker/app/sync/SyncCoordinatorTest.kt`

**Interfaces:**
- `HttpSyncApi(baseUrl, tokenProvider)` implementing `:core` `SyncApi` over `HttpURLConnection` (GET/POST the JSON wire types via kotlinx-serialization). `TokenStore` persists the bearer token (Android Keystore/DataStore is ideal; an encrypted SharedPreferences is acceptable if simpler — document the choice).
- `SyncCoordinator` runs `SyncEngine.sync` inside `viewModelScope.launch(Dispatchers.IO)` (Review Focus 5) and exposes a `SyncState` (`Idle`/`Syncing`/`Error`).
- `LanternYearViewModel` creates the coordinator and triggers a sync after each mutation; the UI shows a "sync" control + last-sync info.

- [ ] **Step 1:** Write the failing `SyncCoordinatorTest` (fake api + store): a successful sync updates state; a failing push keeps the outbox (Review Focus 4).
- [ ] **Step 2:** Run — expected FAIL.
- [ ] **Step 3:** Implement the HTTP api, token store, coordinator; move DB writes/loads onto `Dispatchers.IO`; wire the UI.
- [ ] **Step 4:** Run `./gradlew :app-android:testDebugUnitTest :app-android:assembleDebug` — expected PASS. Commit `feat(app): wire the sync engine`.

---

### Task 7: Verification sweep + README

**Files:**
- Modify: `README.md`

- [ ] **Step 1:** Run `./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck` — expected PASS; record per-module counts.
- [ ] **Step 2:** `./gradlew :app-android:assembleDebug`; confirm the APK.
- [ ] **Step 3:** README: how sync works (outbox → pull → merge LWW → push), the conflict model, and the token/base-url configuration.
- [ ] **Step 4:** Commit `docs: document client sync`.

---

## Self-Review

**1. Spec coverage**

| Spec item | Covered by |
| --- | --- |
| D7 outbox + cursor on the client | Task 4 |
| §6 per-record LWW by HLC; tombstones | Tasks 2, 3 |
| §6 pull/push client | Tasks 3, 5, 6 |
| §8 sync client + background + token storage | Task 6 |
| Ruling P (DB I/O off the main thread) | Task 6 |
| D4 conflict history retention | **Partial** — the equal-tie keeps the local value and re-pushes; full history UI is backlog |
| Retiring Thymeleaf/legacy tables; migration tool | **Deferred** to a later plan |

**2. Step scan:** each step has one checkable result; the merge rules and the port/store shapes are exact.

**3. Type consistency:** `ChangeRecord`/`PullResponse`/`ChangeSet` reused from `:core` (already merged); `SyncApi`/`SyncStore`/`MergePolicy`/`SyncEngine` defined before use.

**4. Review Focus:** each of the five lines maps to a task with tests (Tasks 2/3, 4, 3, 3/6, 6).

**5. Proportion:** task-level; no code transcripts.
