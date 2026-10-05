# Server Sync Hub (`:server`) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a server-side sync hub — a generic change log, user/campaign membership, JWT auth, and pull/push endpoints — that a client can sync against, plus a one-shot migration of the current data into the change log. The existing Spring app keeps working; retiring its Thymeleaf UI is a later plan.

**Architecture:** The wire contract (`ChangeRecord`, `ChangeSet`, `PullResponse`) is defined in `:core` (pure, serializable) so client and server share it. `:server` gains generic `change_records` storage keyed by `(campaignId, entityType, entityId)` with a server-assigned monotonic `rev`, alongside `users`, `campaigns`, and `campaign_members`. Endpoints live under `/api/v1/**` and are guarded by a bearer token; the legacy Thymeleaf routes are untouched.

**Tech Stack:** Kotlin 2.3.21, Spring Boot 4.1.1 (webmvc, data-jpa, validation), H2 file DB, `kotlinx-serialization` + Jackson, `java-jwt`/`jjwt` for HS256 tokens (choose one; add via the catalog). `:core` unchanged except for the shared wire types.

**Spec:** `docs/superpowers/specs/2026-10-05-kdm-tracker-offline-first-kmp-design.md`

## Global Constraints

- Kotlin `2.3.21`; JDK toolchain `21`; Gradle `9.7.1`. Versions live in `gradle/libs.versions.toml`.
- The wire contract is defined **once** in `:core` (`io.kdmtracker.core.sync`) and reused by the server; the server must not fork it.
- The change log is keyed by `(campaignId, entityType, entityId)`; `rev` is **server-assigned** and monotonic per campaign. Push is idempotent by `(campaignId, entityType, entityId)`.
- `/api/v1/**` requires a valid bearer token and campaign membership; the legacy routes stay open as they are today.
- `:core` stays pure (no Spring). The server may depend on `:core`.
- Conventional Commits; **no** `Co-Authored-By`/AI attribution.
- `./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck` stays green.

## Review Focus

1. **Auth actually protects the sync API**: an unauthenticated or non-member request to `/api/v1/**` is rejected (401/403), and the legacy routes are unaffected.
2. **`rev` is monotonic per campaign** and server-assigned; a client cannot forge or rewind it.
3. **Push is idempotent**: replaying the same change batch does not duplicate rows or bump `rev` for identical payloads.
4. **Pull is a correct delta**: `since=<rev>` returns exactly the records with a greater `rev`, in order, and an empty body yields no records.
5. **The migration is faithful**: every current settlement/survivor/resource/innovation/log row becomes a change record with the same identity, and running it twice does not duplicate.

---

## File Structure

```text
kdm-tracker/
├── gradle/libs.versions.toml                 # + jwt library
├── core/src/commonMain/kotlin/io/kdmtracker/core/sync/
│   └── ChangeSet.kt                          # ChangeRecord, ChangeSet, PullResponse
└── server/src/main/kotlin/io/kdmtracker/
    ├── sync/
    │   ├── ChangeRecordEntity.kt             # @Entity change_records
    │   ├── ChangeRecordRepository.kt
    │   ├── ChangeRecordService.kt            # assign rev, apply push, produce delta
    │   └── SyncController.kt                 # GET/POST /api/v1/campaigns/{id}/changes
    ├── auth/
    │   ├── UserEntity.kt, CampaignMemberEntity.kt, CampaignEntity.kt
    │   ├── UserRepository.kt, CampaignMemberRepository.kt, CampaignRepository.kt
    │   ├── JwtService.kt
    │   ├── AuthController.kt                 # register/login
    │   └── BearerTokenFilter.kt              # guards /api/v1/**
    └── migration/LegacyToChangeLogMigration.kt
└── server/src/test/kotlin/io/kdmtracker/sync/...
```

---

### Task 1: Shared wire contract in `:core`

**Files:**
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/sync/ChangeSet.kt`
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/sync/ChangeSetTest.kt`

**Interfaces:**
- Produces:
  - `enum class ChangeOp { UPSERT, DELETE }`
  - `@Serializable data class ChangeRecord(val campaignId: String, val entityType: String, val entityId: String, val op: ChangeOp, val baseRev: Long?, val payload: String, val updatedAtMillis: Long, val updatedAtCounter: Int, val rev: Long = 0)`
  - `@Serializable data class ChangeSet(val records: List<ChangeRecord>)`
  - `@Serializable data class PullResponse(val latestRev: Long, val records: List<ChangeRecord>)`

- [ ] **Step 1:** Write the failing test (JSON round-trip of a `PullResponse` with a `ChangeOp.DELETE` record; `latestRev` preserved).
- [ ] **Step 2:** Run `./gradlew :core:jvmTest` — expected FAIL.
- [ ] **Step 3:** Implement the three types.
- [ ] **Step 4:** Run — expected PASS. Commit `feat(core): add sync wire contract`.

---

### Task 2: Change-log persistence

**Files:**
- Create: `server/src/main/kotlin/io/kdmtracker/sync/ChangeRecordEntity.kt`, `ChangeRecordRepository.kt`
- Test: `server/src/test/kotlin/io/kdmtracker/sync/ChangeRecordRepositoryTest.kt`

**Interfaces:**
- Consumes: `:core` `ChangeRecord`/`ChangeOp`.
- Produces: a JPA `@Entity` (table `change_records`, columns `id`, `campaignId`, `entityType`, `entityId`, `op`, `payload`, `updatedAtMillis`, `updatedAtCounter`, `rev`, unique `(campaignId, entityType, entityId)`) and a repository with `findByCampaignIdAndRevGreaterThan(campaignId, rev)`, `maxRev(campaignId)`, `findByCampaignIdAndEntityTypeAndEntityId(...)`.

- [ ] **Step 1:** Write the failing repository test (`@DataJpaTest`): insert, read back, `maxRev` works, unique constraint on the triple.
- [ ] **Step 2:** Run `./gradlew :server:test` — expected FAIL.
- [ ] **Step 3:** Implement the entity + repository.
- [ ] **Step 4:** Run — expected PASS. Commit `feat(server): add change-log persistence`.

---

### Task 3: Users, campaigns, membership

**Files:**
- Create: `server/src/main/kotlin/io/kdmtracker/auth/{UserEntity,CampaignEntity,CampaignMemberEntity}.kt` + the three repositories
- Test: `server/src/test/kotlin/io/kdmtracker/auth/MembershipRepositoryTest.kt`

**Interfaces:**
- Produces: `UserEntity(username unique, passwordHash)`, `CampaignEntity(name, inviteCode unique)`, `CampaignMemberEntity(campaignId, userId, role)`, plus repositories including `findByUsername`, `findByInviteCode`, `findByCampaignIdAndUserId`, `existsByCampaignIdAndUserId`.

- [ ] **Step 1:** Write the failing test: create a user + campaign, add membership, `existsByCampaignIdAndUserId` true; invite-code lookup works.
- [ ] **Step 2:** Run — expected FAIL.
- [ ] **Step 3:** Implement the entities + repositories.
- [ ] **Step 4:** Run — expected PASS. Commit `feat(server): add users campaigns and membership`.

---

### Task 4: JWT auth + bearer filter

**Files:**
- Create: `server/src/main/kotlin/io/kdmtracker/auth/JwtService.kt`, `AuthController.kt`, `BearerTokenFilter.kt`
- Test: `server/src/test/kotlin/io/kdmtracker/auth/AuthFlowTest.kt`

**Interfaces:**
- Consumes: `UserRepository`, a JWT library from the catalog.
- Produces: `POST /api/v1/auth/register` and `POST /api/v1/auth/login` returning `{ "token": "..." }`; a `BearerTokenFilter` that authenticates `/api/v1/**` (except `/api/v1/auth/**`) and stores the user id in the request context; missing/invalid token → 401.

- [ ] **Step 1:** Write the failing MockMvc test: register → login returns a token; a protected endpoint without a token → 401; with a valid token → not 401.
- [ ] **Step 2:** Run — expected FAIL.
- [ ] **Step 3:** Implement `JwtService` (HS256, secret from config with a dev default), `AuthController`, `BearerTokenFilter`.
- [ ] **Step 4:** Run — expected PASS. Commit `feat(server): add jwt auth and bearer filter`.

---

### Task 5: Pull endpoint

**Files:**
- Create: `server/src/main/kotlin/io/kdmtracker/sync/ChangeRecordService.kt`, `SyncController.kt`
- Test: `server/src/test/kotlin/io/kdmtracker/sync/PullChangesTest.kt`

**Interfaces:**
- Consumes: `ChangeRecordRepository`, `CampaignMemberRepository`, the authenticated user id.
- Produces: `GET /api/v1/campaigns/{id}/changes?since={rev}` → `PullResponse(latestRev, records)` containing only records with `rev > since`, ordered by `rev`. A non-member → 403.

- [ ] **Step 1:** Write the failing test: seed two records; `since=0` returns both ordered; `since=<firstRev>` returns only the second; a non-member gets 403; no token gets 401.
- [ ] **Step 2:** Run — expected FAIL.
- [ ] **Step 3:** Implement `ChangeRecordService.pull(...)` + the controller.
- [ ] **Step 4:** Run — expected PASS. Commit `feat(server): add change pull endpoint`.

---

### Task 6: Push endpoint

**Files:**
- Modify: `server/src/main/kotlin/io/kdmtracker/sync/ChangeRecordService.kt`, `SyncController.kt`
- Test: `server/src/test/kotlin/io/kdmtracker/sync/PushChangesTest.kt`

**Interfaces:**
- Produces: `POST /api/v1/campaigns/{id}/changes` accepting a `ChangeSet`, assigning each record the next per-campaign `rev`, upserting by `(campaignId, entityType, entityId)`, and returning the new `latestRev`. Idempotent: replaying an identical batch does not create duplicate rows (Review Focus 3).

- [ ] **Step 1:** Write the failing test: push two records → `rev` assigned 1,2; a delta pull sees them; replay the **same** batch → no duplicates and `latestRev` unchanged; a non-member → 403.
- [ ] **Step 2:** Run — expected FAIL.
- [ ] **Step 3:** Implement `ChangeRecordService.push(...)` (transactional, server-assigned `rev`) + the controller.
- [ ] **Step 4:** Run — expected PASS. Commit `feat(server): add change push endpoint`.

---

### Task 7: Legacy → change-log migration + verification

**Files:**
- Create: `server/src/main/kotlin/io/kdmtracker/migration/LegacyToChangeLogMigration.kt`
- Modify: `README.md`
- Test: `server/src/test/kotlin/io/kdmtracker/migration/LegacyMigrationTest.kt`

**Interfaces:**
- Consumes: the existing settlement/survivor/resource/innovation/log repositories and `ChangeRecordService`.
- Produces: an idempotent one-shot migration that emits one `ChangeRecord` per legacy row (entityType per table, entityId the legacy id, payload the JSON of the row), stamping `updatedAt` from the row and letting the service assign `rev`. Running it twice does not duplicate (Review Focus 5).

- [ ] **Step 1:** Write the failing test: seed legacy rows, run the migration, assert one change record per row and a second run is a no-op.
- [ ] **Step 2:** Run — expected FAIL.
- [ ] **Step 3:** Implement the migration; document the sync API + migration in `README.md`.
- [ ] **Step 4:** Run `./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck` — expected PASS. Commit `feat(server): migrate legacy rows into the change log`.

---

## Self-Review

**1. Spec coverage**

| Spec item | Covered by |
| --- | --- |
| D11 server storage becomes a change log | Tasks 2, 5, 6 |
| §6 sync protocol (pull `since`, push batch, `rev`) | Tasks 1, 5, 6 |
| D2 accounts + `Campaign`/`CampaignMember` + roles + invite | Tasks 3, 4 |
| §9 migration (existing data → new model) | Task 7 |
| Auth JWT | Task 4 |
| Client sync engine (outbox/cursor, merge, background sync) | **Deferred** to plan 3c |
| Retiring the Thymeleaf UI and the legacy relational tables | **Deferred** to a later plan |
| Moving DB I/O off the main thread | **Deferred** to plan 3c |

**2. Step scan:** each step names one action with a checkable result; the wire types and entity columns are exact.

**3. Type consistency:** `ChangeRecord`/`ChangeSet`/`PullResponse` (Task 1) are referenced identically by Tasks 2, 5, 6, 7; `rev` is always `Long`; `campaignId`/`entityId` are `String` (ULID text).

**4. Review Focus:** each of the five lines is assigned to a task with a test (Tasks 4/5, 6, 6, 5, 7).

**5. Proportion:** task-level; no code transcripts beyond the wire-type and column shapes.
