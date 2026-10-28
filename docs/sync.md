# Persistence, sync and sharing

How the app stores campaigns on the device, syncs them with the hub and shares them between users. The hub side is in [server.md](server.md). Back to the [README](../README.md).

## Persistence

The Android app is **offline-first**: campaigns and settlements are stored locally on the device
through SQLDelight (SQLite), behind the `:core` `CampaignStore` port and implemented by
`SqlDelightCampaignStore` in `:data`. `:core` stays pure — it declares the port and the domain and
owns no database dependency. The on-device database is `kdm-tracker.db`, created in the app's
private storage (`/data/data/io.kdmtracker.app/databases/`) and seeded with a sample campaign on
first launch.

Every table carries sync columns (`updatedAtMillis`, `updatedAtCounter`, `rev`, `deletedAtMillis`,
`deletedAtCounter`) so records can be reconciled. The **outbox**, **sync cursor**, and the
pull/merge/push engine are implemented (see [Sync (Android client)](#sync-android-client)); this
section covers only the local store.

### The settlement record

A settlement's scalar row is not the whole story. `:core` models the extra **settlement record** the
phase steps read in `SettlementState` — the timeline, the unlocked innovations, the built locations,
the append-only resource ledger and the death/birth counters — which composes with the `Settlement`
whose scalars (`name`, `lanternYear`, `population`, `phase`, `currentStep`, `endeavors`, `sync`) it
never duplicates. `SettlementRecord` pairs the two, and every record type is `@Serializable`.

Locally, `:data` stores the state in the `settlement` table's `stateJson` column (added by the
`3.sqm` migration — schema v4:
`ALTER TABLE settlement ADD COLUMN stateJson TEXT NOT NULL DEFAULT '{}'`).
`SqlDelightCampaignStore.upsertSettlement(SettlementRecord)` writes the scalars and the encoded state
as one row — the outbox payload is the versioned DTO, so a later push cannot drop it — and
`loadRecords(campaignId)` rehydrates `List<SettlementRecord>`. The legacy scalar
`upsertSettlement(Settlement)` is additive: it re-reads the stored `stateJson` and preserves it, so
an existing caller cannot clobber the record.

On the wire the transfer schema is **v5**: `SettlementDto` carries the record as
`state: SettlementState` (defaulting to an empty state, so a pre-v5 document decodes unchanged) and
`TransferDocument.SUPPORTED_VERSIONS = [1, 2, 3, 4, 5]`. Within `:core`'s transfer layer,
`TransferMapper` round-trips `SettlementRecord` both ways, so an export/import built on it is
lossless; the server's separate schema-v2 export/import endpoint is not, because it carries only the
settlement scalars and not the `SettlementState`.

## Sync (Android client)

The app is **offline-first and syncs opportunistically**: every local mutation is written to
SQLDelight and to an **outbox** table in the same database, and a sync pass runs after each mutation
and on demand (the manual sync button). The engine lives in `:core` (`io.kdmtracker.core.sync`)
behind two ports — `SyncApi` (transport) and `SyncStore` (local persistence) — so pull/merge/push
orchestration stays pure Kotlin and JVM-testable. `:app-android` supplies the HTTP transport
(`HttpSyncApi`), `:data` supplies the SQLDelight store (`SqlDelightSyncStore`), and `SyncCoordinator`
runs the pass off the UI thread.

**One pass — outbox → pull → per-record LWW merge → push** (`SyncEngine.sync`):

1. **Pull** from the stored cursor: `GET /api/v1/campaigns/{id}/changes?since=<cursor>`.
2. **Merge** each pulled record against the **canonical local record** (falling back to the outbox)
   with per-record last-write-wins (`MergePolicy`). A remote record that wins is applied **and** any
   pending outbox entry for the same `(entityType, entityId)` is dropped, so a superseded local edit
   is never re-pushed. A local record that wins is kept; if it was already acknowledged (canonical
   only, outbox cleared) it is **re-enqueued** so the hub eventually converges.
3. **Advance the cursor** to the response's `latestRev` only after the whole batch merges.
4. **Push** whatever is still pending in the outbox (`POST .../changes`), then clear exactly the
   acknowledged records.

A pull failure aborts before the cursor moves, so no change is skipped. A push failure propagates and
leaves the outbox intact for the next attempt, while the cursor stays advanced because the merged
batch is already durable. An atomic in-flight guard makes overlapping **sync passes** a no-op; it does
not lock the store against a concurrent local mutation (both use the same SQLDelight connection, which
serializes access itself). After a successful merge the repository is reloaded from the store
(`SettlementRepository.reload()`) so applied remote changes reach the UI before the next local edit.

**Conflict model — client-side LWW.** Records for the same entity are ordered by a hybrid logical
clock over (`updatedAtMillis`, `updatedAtCounter`), and **the client resolves conflicts**, not the
server. A `DELETE` is a tombstone ordered by the same clock, so a newer delete supersedes an older
upsert and vice versa. A remote record wins only when its clock is **strictly newer** than the local
one; when the clocks are equal **the local record wins** and is re-pushed. The wire `baseRev` is
carried and persisted verbatim but is **reserved** — the merge does not consult it yet. For the server
path, see the "conflict model (Ruling W)" note above: the hub still stores the most recent push per
entity as-is and never merges; the client now does the merging that was previously deferred.

> **Resurrection caveat (Ruling AB).** Because equal clocks keep local, a remote `DELETE` and a local
> `UPSERT` that share the exact same clock resolve in favour of the local upsert — the entity is
> re-pushed and **resurrected**. The tradeoff is deliberate: a local edit is never silently dropped on
> a tie, and real edits carry strictly-newer clocks. Since the hub itself is last-push-wins, an
> equal-clock delete can also be undone by another client's later push, so deletes are best-effort
> under clock collisions. Full conflict-history retention and UI are backlog.

**Configuration.** The transport needs two things:

- **Server base URL** — the `baseUrl` handed to `HttpSyncApi`. The app currently uses the development
  default `DEFAULT_SYNC_BASE_URL = "http://10.0.2.2:8080"` (the Android emulator's alias for the host
  machine), injected in `LanternYearViewModel`; a real deployment injects a configured URL.
- **Bearer token** — read fresh per request through a `TokenStore`. `SyncCoordinator.syncNow()` gates on
  it: with no token it sets `SyncState.NotConfigured` and **does not attempt** the doomed request,
  instead of asking the hub for a `401`. The token is only ever placed in the request header and is
  never logged.

**Sign-in (token acquisition).** The token is obtained through `POST /api/v1/auth/login`
(`HttpSyncAuthApi`), wrapped by `SyncSignIn` which persists the returned token via the `TokenStore`.
When the coordinator is `NotConfigured` the app shows a minimal username/password form; submitting it
runs `LanternYearViewModel.signIn`, stores the token, and then attempts a sync.

**Cleartext (debug only).** The development hub is plain HTTP and `targetSdk 36` blocks cleartext by
default, so `http://10.0.2.2:8080` would be unreachable. A **debug-only** manifest overlay
(`app-android/src/debug/AndroidManifest.xml`) sets `android:usesCleartextTraffic="true"`; the release
manifest is untouched and stays HTTPS-only. Never ship a release build that depends on cleartext.

**Token storage.** `AndroidKeystoreTokenStore` encrypts the bearer token with AES-GCM under a
non-extractable key held in the **Android Keystore**, and stores only the ciphertext plus IV in a
private `SharedPreferences` file. The choice avoids a dependency: `EncryptedSharedPreferences` would
pull in `androidx.security:security-crypto` (not in the version catalog) and `DataStore` alone stores
plaintext, whereas a hardware-backed key means app-data slurping alone cannot recover the token. A
rotated or deprecated key simply makes a read return `null`, leaving the coordinator `NotConfigured`
until the user signs in again.

## Home and sharing

The Home is the app's entry screen and its campaign picker. It lists **every campaign (settlement)
the signed-in user owns or has shared with them**, one tappable card each. A card shows
`name · Lantern Year N · Population P` plus a sharing **badge** so the three relationships are
distinguishable at a glance:

| Badge | Meaning | Rule |
| ----- | ------- | ---- |
| **Tuyo** | The caller owns the campaign and nobody else is a member. | `role == "OWNER"` and `sharedWith` is empty |
| **Compartido con: …** | The caller owns the campaign and has shared it with the listed users. | `role == "OWNER"` and `sharedWith` is non-empty |
| **Compartido con vos por …** | Someone else owns the campaign and shared it with the caller. | any non-owner role |

The badge rule lives in `:core` (`io.kdmtracker.core.sync.CampaignBadges`) and is pure/testable;
the screen only renders it. The `CampaignSummary` returned by the hub carries `id`, `name`, the
caller's `role`, the `ownerUsername` and the usernames in `sharedWith` (the caller is excluded), and
the app caches each membership for offline badge rendering.

**Sign-in syncs all campaigns.** Signing in (or pressing Sync) runs `SyncCoordinator.syncAll`: it
calls `GET /api/v1/campaigns` to list the caller's campaigns, caches their memberships, then runs a
full pull → merge → push pass for **every** campaign id — each with its own cursor, so one
campaign's failure does not abort the others. A locally created campaign not yet on the hub is
included through a supplemental id provider. Selecting a card re-scopes the cockpit to that campaign
and rebinds the sync target; the cockpit header then shows the settlement **name** (with the
generic "Settlement" label beneath it).

**Sharing model (campaign-level).** Membership is per campaign, not per settlement or record. A
campaign has one owner and any number of members; the role vocabulary is `OWNER`, `EDITOR` and
`VIEWER` (roles are currently displayed and badge the list — fine-grained role enforcement beyond
membership is backlog). Sharing happens through the **invite code** every campaign carries: a user
who wants in calls `POST /api/v1/campaigns/join` with `{"inviteCode"}` and becomes an `EDITOR`
(joining is idempotent and never downgrades an existing member). Creating a campaign makes the
caller its `OWNER` (see the endpoint table below).
