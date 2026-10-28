# Sync hub (server)

The Spring Boot server: JSON sync API, conflict model and legacy-data migration. Back to the [README](../README.md).

The server exposes a small JSON sync API under `/api/v1`. Every route except `/api/v1/auth/*`
requires an `Authorization: Bearer <token>` header; a missing or invalid token is rejected with
`401`.

Authentication:

| Route | Method | Body | Response |
| ----- | ------ | ---- | -------- |
| `/api/v1/auth/register` | POST | `{"username","password"}` | `{"token":"<jwt>"}` (`409` if the name is taken) |
| `/api/v1/auth/login` | POST | `{"username","password"}` | `{"token":"<jwt>"}` (`401` on bad credentials) |

Identity — `GET /api/v1/me` → `{"userId":<id>}` (protected).

Campaigns — a valid token is required. `GET /api/v1/campaigns` lists **every campaign the caller
belongs to** (owned or joined) as summaries carrying the caller's role, the owner's username and the
other members' usernames, ordered by name then id. Creating a campaign makes the caller its `OWNER`;
joining by invite code makes the caller an `EDITOR`. Without these, a freshly registered user has no
membership and every change-log call is `403`, so the hub is unusable end to end:

| Route | Method | Body | Response |
| ----- | ------ | ---- | -------- |
| `/api/v1/campaigns` | GET | — | `[{"id","name","role","ownerUsername","sharedWith"}]` (all the caller's campaigns) |
| `/api/v1/campaigns` | POST | `{"name"}` | `{"id","name","inviteCode","role"}` (`role="OWNER"`; `400` on a blank name) |
| `/api/v1/campaigns/join` | POST | `{"inviteCode"}` | `{"id","name","inviteCode","role"}` (`role="EDITOR"`; `404` on an unknown code) |

Campaign ids are server-generated 26-character Crockford ULIDs; invite codes are unique random
strings. **The client must use the id the server returns from `POST /api/v1/campaigns`** when it
creates a campaign — the change-log endpoints only accept server-issued ids, so the client never
mints one it intends to sync. Joining is idempotent and never downgrades an existing member's role.

Change log, per campaign. The caller must be a member of the campaign or the request is `403`:

| Route | Method | Body / query | Response |
| ----- | ------ | ------------ | -------- |
| `/api/v1/campaigns/{id}/changes?since=<rev>` | GET | `since` (default `0`) | `{"latestRev":<n>,"records":[…]}` |
| `/api/v1/campaigns/{id}/changes` | POST | `{"records":[…]}` | `{"latestRev":<n>}` |

The wire contract lives in `:core` (`io.kdmtracker.core.sync`). Each record carries `campaignId`,
`entityType`, `entityId`, `op` (`UPSERT`/`DELETE`), `baseRev`, `payload`, `updatedAtMillis`,
`updatedAtCounter` and `rev`. On push the server assigns a monotonic per-campaign `rev`
(`maxRev + 1`); replaying an identical `(campaignId, entityType, entityId)` with the same payload is a
no-op — no duplicate row and no revision bump.

Pull reads the campaign high-water mark **before** the records and clamps the returned records to
`rev <= latestRev`. A push that commits between the two reads therefore cannot make `latestRev`
outrun the returned records, so a client cursor never skips a change.

**Conflict model (Ruling W — recorded decision).** The server does **not** implement
last-write-wins or any merge. It stores the most recent push for each
`(campaignId, entityType, entityId)` triple as-is. `baseRev` is carried on the wire and persisted
verbatim but never interpreted server-side; it stays reserved for the client's conflict detection.
The client now performs the merging (per-record LWW by HLC — see
[Sync (Android client)](sync.md#sync-android-client)); `baseRev`-based detection remains client-side and is
still reserved.

### Migrating existing data

Legacy relational rows (settlements, survivors, resources, innovations, log entries, step notes,
milestones, principles — the full schema-v2 set) predate campaigns and accounts.
`io.kdmtracker.migration.LegacyToChangeLogMigration.migrate()` reads every legacy row, folds it into
a single deterministic campaign (`LegacyToChangeLogMigration.CAMPAIGN_ID`, a fixed Crockford-valid
ULID), and pushes one `UPSERT` per row so existing data becomes visible to sync clients. Each table
gets a stable `entityType` (`settlement`, `survivor`, `resource`, `innovation`, `log`, `step_note`,
`milestone`, `principle`). The campaign is owned by a synthetic `legacy-migration` account, and that
username is reserved so no real user can register it.

`migrate()` is a **one-shot, explicitly invoked** entry point — not an always-on startup hook — and is
idempotent: because it routes through the push path, a second run over unchanged legacy data writes
nothing and does not bump `rev`. Timestamps derived from legacy rows use a fixed UTC zone, so a replay
on a host in another time zone is still a no-op. It is exposed as a Spring bean
(`LegacyToChangeLogMigration`) returning a `MigrationReport` with per-entity counts, `changesApplied`
and the campaign's `latestRev`.

**Production runbook (invocation path).** `LegacyMigrationRunner` calls `migrate()` on startup **only**
when `sync.migration.run-on-startup` is true (default `false`), so ordinary boots never migrate
silently. To run the one-shot in production:

```bash
SYNC_JWT_SECRET="<strong-secret>" SYNC_MIGRATION_RUN_ON_STARTUP=true java -jar app.jar
# watch the logs for the migration summary, then restart WITHOUT SYNC_MIGRATION_RUN_ON_STARTUP
```

Because the migration is idempotent, a second boot with the flag still set is safe but unnecessary;
unset it after the run. (A first re-run after new legacy rows is added will emit only the new rows.)
