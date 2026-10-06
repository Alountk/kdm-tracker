# Home, Multi-Campaign Sync & Sharing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give the app a **Home** that lists every campaign (settlement) the signed-in user owns or has shared with them, each with a **sharing badge**, lets them **select** one or **create** a new one, and **syncs all of them** on sign-in. Also show the settlement **name** in the cockpit.

**Architecture:** Sharing is at the **campaign** level (campaign = settlement). The server exposes the caller's campaigns with their role and the other members; `:core` carries the serializable summary and a pure badge rule; `:data` stores each campaign's membership (role/owner/sharedWith) for offline badges (schema v3); the app discovers and syncs every campaign on sign-in and drives a Home screen.

**Tech Stack:** Kotlin 2.3.21, Spring Boot 4.1.1 (`:server`), Kotlin Multiplatform (`:core`), SQLDelight 2.4.0 (`:data`), Compose Multiplatform (`:app-android`).

**Spec:** `docs/superpowers/specs/2026-10-05-kdm-tracker-offline-first-kmp-design.md`
**Depends on:** the merged server sync hub, local persistence, and client sync engine.

## Global Constraints

- Kotlin `2.3.21`; JDK toolchain `21`; versions via the catalog. `:core` stays pure (no Spring/Android/SQLDelight).
- Sharing unit = **campaign**. A campaign has exactly one settlement (`Settlement` id used as the campaign's settlement).
- The server is authoritative for role/membership; the client caches it for offline display.
- Wire additions are serializable in `:core` and reused by the server — never forked.
- Conventional Commits; **no** `Co-Authored-By`/AI attribution.
- `./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck` stays green.

## Review Focus

1. **Sign-in syncs everything**: after sign-in the app lists and syncs **every** campaign the user owns or that is shared with them — not just one.
2. **The badge is correct**: `OWNER` with no others → "Tuyo"; `OWNER` with others → "Compartido con …"; non-owner → "Compartido con vos por …" (owner username). It never mislabels an owner as a sharee or vice-versa.
3. **Selecting a campaign is isolated**: opening one campaign's settlement never reads or writes another campaign's rows/cursor.
4. **Creating a campaign is online-only for now**: the hub owns the campaign id (`POST /api/v1/campaigns`), so `create` requires a reachable, authenticated hub; it persists the server-issued campaign and its settlement locally and pushes them, and the new campaign appears in the list. A failure surfaces inline and leaves no half-created state. **Offline creation is deferred** — the client never mints an id it intends to sync. The Home card list refresh is **independent of sync success**: one campaign failing to sync must not hide the campaigns that did sync.
5. **The cockpit shows the settlement name** so a remote rename is visible.

---

## File Structure

```text
server/src/main/kotlin/io/kdmtracker/auth/
└── CampaignController.kt          # + GET /api/v1/campaigns (my campaigns with role + sharedWith)
core/src/commonMain/kotlin/io/kdmtracker/core/sync/
├── CampaignSummary.kt             # serializable summary + pure banner rule
└── SyncApi.kt                     # + listCampaigns()
data/src/commonMain/sqldelight/io/kdmtracker/data/db/
├── Membership.sq                  # campaign_membership table
└── 2.sqm                          # v2 -> v3 migration
data/src/commonMain/kotlin/io/kdmtracker/data/SqlDelightSyncStore.kt   # + membership persistence
app-android/src/main/kotlin/io/kdmtracker/app/
├── SettlementRepository.kt        # + listCards / select / create, sync-all
├── UiState.kt                     # + Home screen + settlementName
├── LanternYearViewModel.kt        # + home state, select, create, sync-all
└── ui/HomeScreen.kt               # new
```

---

### Task 1: Server — list my campaigns

**Files:**
- Modify: `server/src/main/kotlin/io/kdmtracker/auth/CampaignController.kt`
- Test: `server/src/test/kotlin/io/kdmtracker/auth/CampaignListTest.kt`

**Interfaces:**
- Produces `GET /api/v1/campaigns` (behind the bearer filter) → `List<CampaignSummaryResponse>`:
  - `CampaignSummaryResponse(id: String, name: String, role: String, ownerUsername: String, sharedWith: List<String>)`
  - `role` is the caller's role; `ownerUsername` is the `OWNER`'s username; `sharedWith` is the usernames of **other** members (excluding the caller), ordered.
- Uses `members.findByUserId(userId)` to find the caller's campaigns; for each, load the campaign and its members (`findByCampaignId` — add to `CampaignMemberRepository` if missing) and resolve usernames via `UserRepository`.
- Test (MockMvc): a user with two campaigns (one owned alone → `sharedWith` empty; one shared with another user → `sharedWith` contains them); a user who joined someone else's campaign → their `role` is `EDITOR` and `ownerUsername` is the owner; no token → 401.

- [ ] **Step 1:** Write the failing `CampaignListTest`.
- [ ] **Step 2:** Run `./gradlew :server:test` — expected FAIL.
- [ ] **Step 3:** Implement the endpoint (+ any repository query needed).
- [ ] **Step 4:** Run — expected PASS. Commit `feat(server): list the caller's campaigns with sharing`.

---

### Task 2: `:core` — campaign summary + badge rule

**Files:**
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/sync/CampaignSummary.kt`
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/sync/SyncApi.kt` (add `listCampaigns`)
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/sync/CampaignSummaryTest.kt`

**Interfaces:**
- Produces:
  - `@Serializable data class CampaignSummary(val id: String, val name: String, val role: String, val ownerUsername: String, val sharedWith: List<String>)`
  - `enum class CampaignBadge { OWNED, SHARED_BY_ME, SHARED_WITH_ME }`
  - `data class CampaignBanner(val badge: CampaignBadge, val label: String)`
  - `object CampaignBadges { fun of(summary: CampaignSummary): CampaignBanner }` — `OWNER` & empty `sharedWith` → `OWNED` with label `"Tuyo"`; `OWNER` & non-empty → `SHARED_BY_ME` with `"Compartido con: <names, comma-joined>"`; otherwise → `SHARED_WITH_ME` with `"Compartido con vos por <ownerUsername>"`.
- Extend `SyncApi` with `fun listCampaigns(): List<CampaignSummary>`.
- Test covers all three badge cases (owner alone, owner with others, editor/viewer) and label formatting.

- [ ] **Step 1:** Write the failing `CampaignSummaryTest`.
- [ ] **Step 2:** Run `./gradlew :core:jvmTest` — expected FAIL.
- [ ] **Step 3:** Implement the type, enum, banner rule, and the port method.
- [ ] **Step 4:** Run — expected PASS. Commit `feat(core): add campaign summary and badge rule`.

---

### Task 3: `:data` — membership persistence (schema v3)

**Files:**
- Create: `data/src/commonMain/sqldelight/io/kdmtracker/data/db/Membership.sq`
- Create: `data/src/commonMain/sqldelight/io/kdmtracker/data/db/2.sqm`
- Modify: `data/src/commonMain/kotlin/io/kdmtracker/data/SqlDelightSyncStore.kt`
- Test: `data/src/jvmTest/kotlin/io/kdmtracker/data/MembershipStoreTest.kt`

**Interfaces:**
- Produces a `campaign_membership(campaignId TEXT PRIMARY KEY, name TEXT, role TEXT, ownerUsername TEXT, sharedWithJson TEXT)` table and store methods `upsertMembership(summary)`, `memberships(): List<CampaignSummary>`, `membership(campaignId): CampaignSummary?`.
- The `2.sqm` migration adds the table to a v2 database; verify the generated schema version bumps to 3 and both fresh-create and upgrade work.
- Test: round-trip a `CampaignSummary`; replay updates rather than duplicates; migration adds the table.

- [ ] **Step 1:** Write the failing `MembershipStoreTest`.
- [ ] **Step 2:** Run `./gradlew :data:jvmTest` — expected FAIL.
- [ ] **Step 3:** Add the `.sq` + `.sqm` and the store methods.
- [ ] **Step 4:** Run `./gradlew :data:jvmTest :data:assembleDebug` — expected PASS. Commit `feat(data): persist campaign membership`.

---

### Task 4: App — multi-campaign repository + sync-all

**Files:**
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/SettlementRepository.kt`
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/sync/HttpSyncApi.kt` (`listCampaigns`)
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/sync/SyncCoordinator.kt` (sync **all**)
- Test: `app-android/src/test/kotlin/io/kdmtracker/app/MultiCampaignRepositoryTest.kt`

**Interfaces:**
- Produces:
  - `data class CampaignCard(val campaignId: String, val name: String, val lanternYear: Int, val population: Int, val banner: CampaignBanner)`
  - `SettlementRepository.listCards(): List<CampaignCard>` — one card per stored campaign (settlement) joined with its `membership` banner.
  - `SettlementRepository.select(campaignId: String): Boolean` and `SettlementRepository.create(name: String): CampaignCard` (round-trips `POST /api/v1/campaigns` for the server-issued id, then persists that campaign + a `Settlement.new` + the OWNER membership; online-only, no client-minted id).
  - `SyncCoordinator.syncAll()` — `api.listCampaigns()`, upsert each membership, then `engine.sync(campaignId)` for each hub-listed or hub-created campaign; the local first-run seed is never submitted. Failures per campaign do not abort the rest (Review Focus 1/3).
- `HttpSyncApi.listCampaigns()` GETs `/api/v1/campaigns` and decodes `List<CampaignSummary>`.
- Test (JVM, fakes): `listCards` returns one card per campaign with the right banner; `select` scopes the settlement; `syncAll` syncs every listed campaign (assert each cursor advanced); a failure in one campaign does not stop the others.

- [ ] **Step 1:** Write the failing `MultiCampaignRepositoryTest`.
- [ ] **Step 2:** Run `./gradlew :app-android:testDebugUnitTest` — expected FAIL.
- [ ] **Step 3:** Implement `listCards`/`select`/`create`, `listCampaigns`, and `syncAll`.
- [ ] **Step 4:** Run `./gradlew :app-android:testDebugUnitTest :app-android:assembleDebug` — expected PASS. Commit `feat(app): sync and list all campaigns`.

---

### Task 5: App — Home screen with badges

**Files:**
- Create: `app-android/src/main/kotlin/io/kdmtracker/app/ui/HomeScreen.kt`
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/UiState.kt` (`Screen.Home` + `settlementName`)
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/LanternYearViewModel.kt` (home state, `select`, `create`)
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/KdmTrackerApp.kt` (render Home first; nav)
- Test: `app-android/src/test/kotlin/io/kdmtracker/app/HomeSelectionTest.kt`

**Interfaces:**
- Produces:
  - `Screen.Home`; the app starts on Home (lists `listCards()`), and `select(id)` moves to the cockpit for that campaign.
  - `HomeScreen(cards, syncState, onSelect, onCreate, onSync, onSignIn, username, password)` — a card per campaign showing `name · Lantern Year N · Population P` and the `banner.label`; a "New settlement" name field + button; a "Sync"/sign-in control; **no** settlement name hardcoded.
  - `UiState` gains `settlementName` (the selected settlement's name).
- Test: selecting a card switches to the Settlement screen with that campaign's data; creating adds a card and opens it; the badge label matches `CampaignBadges`.

- [ ] **Step 1:** Write the failing `HomeSelectionTest`.
- [ ] **Step 2:** Run — expected FAIL.
- [ ] **Step 3:** Implement `Screen.Home`, the Home screen, and the ViewModel wiring.
- [ ] **Step 4 (Review Focus 2):** Confirm each badge renders the correct label for owner-alone / owner-with-others / shared.
- [ ] **Step 5:** Run `./gradlew :app-android:testDebugUnitTest :app-android:assembleDebug` — expected PASS. Commit `feat(app): add the settlement home with sharing badges`.

---

### Task 6: App — show the settlement name in the cockpit

**Files:**
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/ui/SettlementScreen.kt`, `.../HuntScreen.kt`, `.../ShowdownScreen.kt`

**Interfaces:**
- Produces: the settlement name rendered as a heading in all three cockpit screens (Review Focus 5), so a remote rename is visible after sync.

- [ ] **Step 1:** Render `state.settlementName` in the three screens; add the name to the header.
- [ ] **Step 2:** Verify `./gradlew :app-android:assembleDebug`.
- [ ] **Step 3:** Commit `feat(app): show the settlement name in the cockpit`.

---

### Task 7: Verification sweep + README

**Files:**
- Modify: `README.md`

- [ ] **Step 1:** Run `./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck`; record per-module counts.
- [ ] **Step 2:** `./gradlew :app-android:assembleDebug`; confirm the APK.
- [ ] **Step 3:** README: the Home, multi-campaign sync on sign-in, the sharing model (campaign-level, roles, invite code) and the badge meanings, plus the new `GET /api/v1/campaigns` endpoint.
- [ ] **Step 4:** Commit `docs: document the home and sharing`.

---

## Self-Review

**1. Spec coverage**

| Spec item | Covered by |
| --- | --- |
| D2 accounts + campaigns + membership + roles + invite | Tasks 1, 2, 4 |
| §6 client sync | Task 4 (sync-all) |
| Multi-settlement home (original web feature) | Tasks 4, 5 |
| Sharing visibility (owned / shared by me / shared with me) | Tasks 1, 2, 5 |
| D8 stateless screens | Tasks 5, 6 |
| Full role-based authorization (EDITOR can't edit a VIEWER-only resource) | **Deferred** — roles are displayed, not yet enforced beyond membership |
| Retiring Thymeleaf/legacy tables | **Deferred** |

**2. Step scan:** each step names one action with a checkable result; the wire shapes and the badge rule are exact.

**3. Type consistency:** `CampaignSummary`/`CampaignBadge`/`CampaignBanner` defined in Task 2 are used by Tasks 3, 4, 5; `CampaignCard` in Task 4 is used by Task 5.

**4. Review Focus:** each of the five lines maps to a task with tests (4, 2/5, 4, 4, 6).

**5. Proportion:** task-level; no code transcripts beyond the wire/table shapes.
