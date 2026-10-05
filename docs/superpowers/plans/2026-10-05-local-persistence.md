# Local Persistence (`:data`) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the app's in-memory `SettlementRepository` with a real offline-first local store backed by SQLDelight, sitting behind a `:core` port, so campaign/settlement state survives app restarts on the device.

**Architecture:** A new `:data` Kotlin Multiplatform module owns SQLDelight (schema + generated queries + drivers). `:core` declares a pure `CampaignStore` port and DTO-ish snapshot type; `:core` never imports SQLDelight. `:app-android` depends on `:data` and consumes the port. Sync columns (`updatedAt`, `rev`, `deletedAt`) exist in the schema from day one, but the outbox, cursor, and network sync are the **next** plan (3b) and are not implemented here.

**Tech Stack:** Kotlin 2.3.21, Gradle 9.7.1, Kotlin Multiplatform (`jvm()` + `androidTarget()`), AGP 9.4.1, SQLDelight 2.4.0 (Gradle plugin + `runtime`, `android-driver`, `sqlite-driver`), JDK toolchain 21, Compose Multiplatform 1.12.1 (app).

**Spec:** `docs/superpowers/specs/2026-10-05-kdm-tracker-offline-first-kmp-design.md`

## Global Constraints

- Kotlin `2.3.21`; Gradle `9.7.1`; JDK toolchain `21`. Android: AGP `9.4.1`, compileSdk `37`, targetSdk `36`, minSdk `24`.
- **SQLDelight `2.4.0`**; versions live in `gradle/libs.versions.toml` — no hardcoded versions in module files.
- `:core` stays pure: **no SQLDelight, no Android APIs** in `commonMain`. Persistence lives behind a port.
- The Android SDK is a repo-wide build prerequisite (AGP is applied to `:core` and `:data`); `local.properties` (git-ignored) holds `sdk.dir`, and root `gradle.properties` carries `android.builtInKotlin=false` / `android.newDsl=false`.
- Every persisted table carries the sync columns `id` (ULID text), `updatedAtMillis`, `updatedAtCounter`, `rev`, and nullable `deletedAtMillis`/`deletedAtCounter`.
- Spotless (ktlint) passes; `./gradlew :core:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck` stays green.
- Conventional Commits; **no** `Co-Authored-By`/AI attribution.

## Review Focus

1. **Real persistence, not a shim**: a settlement written then read back from a fresh store instance round-trips the full aggregate (including sync metadata); process restart equivalent is covered by a JVM test using a new store over the same database.
2. **`:core` stays pure**: the port is a plain interface; no SQLDelight/Android type leaks into `:core`.
3. **Schema completeness**: the sync columns exist for every table, so plan 3b does not need a schema migration.
4. **The app still runs the loop**: after wiring, `:app-android:assembleDebug` builds and the lantern-year loop still advances against the persisted settlement.
5. **No data loss on upsert**: saving a settlement twice updates the same row (keyed by ULID), never duplicates it.

---

## File Structure

```text
kdm-tracker/
├── gradle/libs.versions.toml            # + sqldelight plugin + libraries
├── settings.gradle.kts                  # + include(":data")
├── core/src/commonMain/kotlin/io/kdmtracker/core/
│   ├── persistence/CampaignStore.kt     # port + CampaignSnapshot (pure)
│   └── ruleset/RulesetOverrides.kt      # + @Serializable (nested types too)
├── data/
│   ├── build.gradle.kts                 # KMP + android library + sqldelight
│   └── src/
│       ├── commonMain/sqldelight/io/kdmtracker/data/db/
│       │   ├── Campaign.sq
│       │   └── Settlement.sq
│       ├── commonMain/kotlin/io/kdmtracker/data/
│       │   ├── KdmDatabaseFactory.kt
│       │   └── SqlDelightCampaignStore.kt
│       └── commonTest/kotlin/io/kdmtracker/data/
│           └── SqlDelightCampaignStoreTest.kt
└── app-android/
    ├── build.gradle.kts                 # + implementation(project(":data"))
    └── src/main/kotlin/io/kdmtracker/app/
        └── SettlementRepository.kt      # backed by CampaignStore
```

---

### Task 1: `:data` module + SQLDelight (build spike)

**Files:**
- Modify: `gradle/libs.versions.toml` (sqldelight plugin + libraries)
- Modify: `settings.gradle.kts` (`include(":data")`)
- Create: `data/build.gradle.kts`
- Create: `data/src/commonMain/sqldelight/io/kdmtracker/data/db/Campaign.sq` (minimal table + one query)

**Interfaces:**
- Produces: a `:data` module that generates the `KdmDatabase` type and runs a JVM test.

- [ ] **Step 1:** Catalog: `sqldelight = "2.4.0"`, plugin `sqldelight = { id = "app.cash.sqldelight", version.ref = "sqldelight" }`, libraries `sqldelight-runtime`, `sqldelight-android-driver`, `sqldelight-sqlite-driver`.
- [ ] **Step 2:** `data/build.gradle.kts`: apply `kotlin("multiplatform")`, `alias(libs.plugins.android.library)`, `alias(libs.plugins.sqldelight)`; `androidTarget()` + `jvm()` with JVM 21; `android { namespace = "io.kdmtracker.data"; compileSdk = 37; defaultConfig { minSdk = 24 } }`; `sqldelight { databases { create("KdmDatabase") { packageName.set("io.kdmtracker.data.db") } } }`; deps `commonMain` → `libs.sqldelight.runtime`, `commonTest` → `libs.kotlin.test`, `jvmMain` → `libs.sqldelight.sqlite.driver`, `androidMain` → `libs.sqldelight.android.driver`. Add `google()`/`mavenCentral()`.
- [ ] **Step 3:** Minimal `Campaign.sq` with a `campaign` table (id PK, name) and `selectAll`.
- [ ] **Step 4:** Verify `./gradlew :data:generateCommonMainKdmDatabaseInterface :data:jvmTest` succeeds (codegen runs). If the SQLDelight plugin is incompatible with Kotlin 2.3.21 / Gradle 9.7.1, report `BLOCKED` with the exact error — do **not** downgrade silently.
- [ ] **Step 5:** Commit `build(data): add sqldelight persistence module`.

---

### Task 2: `CampaignStore` port in `:core` + serializable overrides

**Files:**
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/persistence/CampaignStore.kt`
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/ruleset/RulesetOverrides.kt` (add `@Serializable` to it and to `MilestoneDefinition`/`PrincipleChoice` if they are not already)
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/persistence/CampaignStoreContractTest.kt`

**Interfaces:**
- Consumes: `Campaign`, `Settlement`, `Ulid`.
- Produces:
  - `data class CampaignSnapshot(val campaign: Campaign, val settlements: List<Settlement>)`
  - `interface CampaignStore { fun loadSnapshots(): List<CampaignSnapshot>; fun upsertCampaign(campaign: Campaign); fun upsertSettlement(settlement: Settlement) }` — synchronous for now (SQLDelight sync queries); a `Flow`-based variant is a later concern.
- The test defines a trivial in-memory `FakeCampaignStore` implementing the port and asserts the contract semantics (upsert twice → one entry; load returns what was written). This is the contract the SQLDelight store must satisfy.

- [ ] **Step 1:** Write the failing contract test with a `FakeCampaignStore`.
- [ ] **Step 2:** Run `./gradlew :core:jvmTest` — expected FAIL.
- [ ] **Step 3:** Implement the port + `CampaignSnapshot`; annotate `RulesetOverrides` (and nested types) `@Serializable` so `:data` can persist them.
- [ ] **Step 4:** Run — expected PASS. Commit `feat(core): add campaign store port`.

---

### Task 3: SQLDelight schema + queries

**Files:**
- Replace: `data/src/commonMain/sqldelight/io/kdmtracker/data/db/Campaign.sq`
- Create: `data/src/commonMain/sqldelight/io/kdmtracker/data/db/Settlement.sq`

**Interfaces:**
- Produces generated types `Campaign`/`Settlement` rows. Tables:

```sql
-- Campaign.sq
CREATE TABLE campaign (
  id TEXT NOT NULL PRIMARY KEY,
  name TEXT NOT NULL,
  rulesetId TEXT NOT NULL,
  rulesetOverridesJson TEXT NOT NULL,
  updatedAtMillis INTEGER NOT NULL,
  updatedAtCounter INTEGER NOT NULL,
  rev INTEGER NOT NULL,
  deletedAtMillis INTEGER,
  deletedAtCounter INTEGER
);
upsertCampaign: INSERT OR REPLACE INTO campaign VALUES (?,?,?,?,?,?,?,?,?);
selectAllCampaigns: SELECT * FROM campaign WHERE deletedAtMillis IS NULL;
selectCampaignById: SELECT * FROM campaign WHERE id = ?;

-- Settlement.sq
CREATE TABLE settlement (
  id TEXT NOT NULL PRIMARY KEY,
  campaignId TEXT NOT NULL,
  name TEXT NOT NULL,
  lanternYear INTEGER NOT NULL,
  phase TEXT NOT NULL,
  currentStep INTEGER NOT NULL,
  endeavors INTEGER NOT NULL,
  population INTEGER NOT NULL,
  settlementEvent TEXT,
  updatedAtMillis INTEGER NOT NULL,
  updatedAtCounter INTEGER NOT NULL,
  rev INTEGER NOT NULL,
  deletedAtMillis INTEGER,
  deletedAtCounter INTEGER
);
upsertSettlement: INSERT OR REPLACE INTO settlement VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?);
selectSettlementsByCampaign: SELECT * FROM settlement WHERE campaignId = ? AND deletedAtMillis IS NULL;
selectSettlementById: SELECT * FROM settlement WHERE id = ?;
```

- [ ] **Step 1:** Write both `.sq` files.
- [ ] **Step 2:** Run `./gradlew :data:generateCommonMainKdmDatabaseInterface` — expected success.
- [ ] **Step 3:** Commit `feat(data): add campaign and settlement schema`.

---

### Task 4: `SqlDelightCampaignStore` + JVM tests

**Files:**
- Create: `data/src/commonMain/kotlin/io/kdmtracker/data/SqlDelightCampaignStore.kt`
- Create: `data/src/commonMain/kotlin/io/kdmtracker/data/KdmDatabaseFactory.kt`
- Test: `data/src/commonTest/kotlin/io/kdmtracker/data/SqlDelightCampaignStoreTest.kt`

**Interfaces:**
- Consumes: `KdmDatabase`, `CampaignStore`, `CampaignSnapshot`, the domain aggregates, `Json` (kotlinx-serialization) for `rulesetOverridesJson`.
- Produces: `class SqlDelightCampaignStore(private val database: KdmDatabase) : CampaignStore` mapping each row ⇄ domain. Maps `SettlementPhase` via `valueOf`/`name`. Reconstructs `SyncMeta` from the four columns.

- [ ] **Step 1:** Write the failing test using an in-memory SQLDelight database on JVM (`JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)` + `KdmDatabase.Schema.create(driver)`): upsert a campaign + settlement, then a **second store instance over the same database** loads them with full equality (Review Focus 1/5).
- [ ] **Step 2:** Run `./gradlew :data:jvmTest` — expected FAIL.
- [ ] **Step 3:** Implement `SqlDelightCampaignStore`.
- [ ] **Step 4:** Run — expected PASS. Commit `feat(data): implement sqldelight campaign store`.

---

### Task 5: Wire the app to the store

**Files:**
- Modify: `app-android/build.gradle.kts` (`implementation(project(":data"))`)
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/SettlementRepository.kt` (back it with `CampaignStore`; keep a constructor that seeds the sample on first run)
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/LanternYearViewModel.kt` (construct the real store)

**Interfaces:**
- Consumes: `CampaignStore`, `SqlDelightCampaignStore`, the Android SQLDelight driver.
- Produces: `SettlementRepository` reads/writes through the store; `viewModel()` creates a database-backed store.

- [ ] **Step 1:** Add the `:data` dependency; create the Android database (driver via `AndroidSqliteDriver`) in the app.
- [ ] **Step 2:** Back `SettlementRepository` with the store; on first load, if empty, seed the sample campaign/settlement through `upsertCampaign`/`upsertSettlement`.
- [ ] **Step 3:** Update the repository/viewmodel tests to pass a fake store (no Android driver in JVM tests).
- [ ] **Step 4:** Verify `./gradlew :app-android:testDebugUnitTest :app-android:assembleDebug` — expected PASS.
- [ ] **Step 5 (Review Focus 4):** Confirm the loop still advances against the persisted settlement.
- [ ] **Step 6:** Commit `feat(app): persist the settlement through the campaign store`.

---

### Task 6: Verification sweep + README

**Files:**
- Modify: `README.md` (document `:data`, SQLDelight, and where the on-device database lives)

**Interfaces:**
- Produces: a documented, reproducible build.

- [ ] **Step 1:** Run `./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck` — expected PASS.
- [ ] **Step 2:** Run `./gradlew :app-android:assembleDebug` and confirm the APK exists.
- [ ] **Step 3:** Update `README.md`: modules (`core`, `data`, `server`, `app-android`), the persistence note, and that sync (outbox/cursor/network) is the next plan.
- [ ] **Step 4:** Commit `docs: document local persistence`.

---

## Self-Review

**1. Spec coverage**

| Spec item | Covered by |
| --- | --- |
| §8 / D7 SQLDelight typed local mirror | Tasks 1, 3, 4 |
| Sync columns on every table | Task 3 |
| Persistence behind ports | Tasks 2, 4, 5 |
| `:core` stays pure | Task 2 (port only; no SQLDelight) |
| Offline-first source of truth on device | Task 5 |
| `pending_ops` outbox / `sync_cursor` | **Deferred** to plan 3b (sync engine) |
| Server change-log + auth + pull/push | **Deferred** to plan 3b |
| Migration of existing H2 data | **Deferred** to plan 3b |

**2. Step scan:** each step names one action with a checkable result; bodies are left to the implementer except the schema DDL and the port contract, which must be exact.

**3. Type consistency:** `CampaignStore`, `CampaignSnapshot`, `SqlDelightCampaignStore`, and the schema column names are defined before first use and referenced identically.

**4. Review Focus:** each of the five lines is assigned to a task with a check (Tasks 4, 2, 3, 5, 4).

**5. Proportion:** task-level; no code transcripts beyond the required DDL.

**Known scope boundary:** the outbox, sync cursor, network client, server change-log, auth, and data migration are intentionally out of scope — plan 3b.
