# kdm-tracker

Campaign/settlement tracker for Kingdom Death: Monster.

**v1 scope**: settlement manager — survivors, resource inventory, innovations, and the campaign log.
The in-game session companion is planned for a later milestone (v2).

## Stack

| Component   | Version                                                 |
| ----------- | ------------------------------------------------------- |
| Spring Boot | 4.1.1                                                   |
| Kotlin      | 2.3.21                                                  |
| Gradle      | 9.7.1 (wrapper, Kotlin DSL)                             |
| JDK         | Target 21 LTS (via `jvmToolchain(21)`); local daemon runs on JDK 25 |
| Persistence (server) | Spring Data JPA + H2 file database (in `./data/`) |
| Persistence (app)    | SQLDelight (SQLite) in `:data` — typed on-device mirror of `:core` |
| Views       | Thymeleaf (server-side rendering)                       |
| Android app | Compose Multiplatform 1.12.1 + Material 3 (AGP 9.4.1)   |
| Lint        | ktlint via Spotless (`com.diffplug.spotless`) 7.0.4     |

## Requirements

- JDK 21+ (the build targets 21; any newer JDK works — the local daemon here runs on JDK 25)
- Android SDK (command-line tools) — **required for every Gradle invocation**, not only the app
- Docker (optional, for the container workflow)

No global Gradle install is required — use the committed wrapper (`./gradlew`).

### Android SDK (repo-wide build prerequisite)

`:core` applies the Android Gradle Plugin to publish its `androidTarget`, so **the Android SDK is a
prerequisite for the whole repository**. Without it, even the JVM-only tasks fail at configuration:
`./gradlew :core:jvmTest`, `./gradlew :server:test` and `./gradlew build` all abort before running a
single test. Set the SDK up first.

Required SDK packages (install with `sdkmanager`):

- `platform-tools` (adb)
- `platforms;android-37.0` (compileSdk 37)
- `build-tools;36.0.0`

Point Gradle at the SDK in either of two ways:

1. **`ANDROID_HOME`** environment variable (portable; recommended for CI):

   ```bash
   export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
   ```

2. **`local.properties`** at the repository root (git-ignored — never commit it), for a
   machine-local path:

   ```properties
   sdk.dir=/opt/homebrew/share/android-commandlinetools
   ```

`ANDROID_SDK_ROOT` is accepted as a legacy alias, but `ANDROID_HOME` is preferred. This workspace
uses SDK root `/opt/homebrew/share/android-commandlinetools`.

The app module uses the same JDK 21 toolchain and Gradle 9.7.1 wrapper as the rest of the repo.

## Quickstart (local)

```bash
# Enable the plain pre-commit hook (runs `./gradlew -q spotlessCheck`):
git config core.hooksPath .githooks

# Local dev: the server refuses to boot on the committed default JWT secret, so opt into the
# `dev` profile (or export a real SYNC_JWT_SECRET).
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

> Commit messages follow **Conventional Commits by convention** — this project intentionally ships
> no commitlint/husky, so the message style is a team convention, not an automated check.

Then verify:

```bash
curl http://localhost:8080/
# Expected: the settlement dashboard (HTTP 200)

curl http://localhost:8080/health
# Expected: {"status":"ok"}
```

On first run the app seeds a single settlement row (name "New Settlement", lantern year 1) and the
basic resource inventory (Bone, Hide, Sinew, Amber, Cloth, Iron, Pigment, Scrap Iron).

## Quickstart (Docker)

The build context is the repository **root** (the image copies both `core/` and `server/`),
while the Dockerfile itself lives under `server/`:

```bash
docker build -t bp-kdm-tracker -f server/Dockerfile .
# The container refuses to boot on the default JWT secret — pass a real one:
docker run -d --name bp-kdm-tracker -p 8703:8080 -v kdm-data:/app/data \
  -e SYNC_JWT_SECRET="change-me-to-a-long-random-secret" bp-kdm-tracker

curl -fsS -o /dev/null -w "%{http_code}" http://localhost:8703/
# Expected: 200

curl http://localhost:8703/health
# Expected: {"status":"ok"}

docker rm -f bp-kdm-tracker
```

The named volume `kdm-data` is mounted at `/app/data`, where the container writes the H2 file
database — campaign data survives container replacement.

## Quickstart (Android app)

The `:app-android` module is a Compose Multiplatform Android app that consumes `:core`. It opens on
a **Home** that lists every campaign (settlement) the signed-in user can reach, then enters a
navigable lantern-year loop (Settlement → Hunt → Showdown) driven by `:core` rules. Settlement
Steps 1 (Set Up) and 7 (Develop) are implemented; the remaining step bodies show a "Pendiente"
stub and arrive in later plans (see [Settlement Steps 1 & 7](#settlement-steps-1--7)). Entering
Step 3 funds the endeavor pool; hunt rewards are not modeled yet, so resources are recorded
manually on the Develop screen. State
persists locally through `:data` (see [Persistence](#persistence)) and syncs to the hub through the
client sync engine (see [Sync (Android client)](#sync-android-client)). The Home and its sharing
model are described in [Home and sharing](#home-and-sharing).

Build the debug APK:

```bash
./gradlew :app-android:assembleDebug
# Output: app-android/build/outputs/apk/debug/app-debug.apk
```

Install it on a connected device or emulator:

```bash
adb install -r app-android/build/outputs/apk/debug/app-debug.apk
```

## Settlement Steps 1 & 7

Two Settlement Phase steps are real in the Compose app; the rest keep their `Pendiente` body. Both
are backed by a content-driven rules layer in `:core` — the screens render state and report intent,
and every cost and limit comes from the composed `ContentCatalog`, never a literal in the UI.

- **Step 1 — Set Up the Settlement** (`SetUpStepScreen`): read-only. Shows the principles in play,
  the derived **Innovation Deck** (the consequences the unlocked innovations add), the locations
  already built, and this lantern year's timeline/event, then a single *Confirm Setup* that advances.
- **Step 7 — Develop** (`DevelopStepScreen`): rendered inside the cockpit's chrome, so it is never
  a navigation dead-end. Three tabs, all drawn from `:core`-derived views:
  - **Recursos** — resources grouped by **source** (Básicos / León Blanco / Fénix / …), each with a
    `+1` (the manual funding bridge until hunt rewards are modeled).
  - **Innovar** — the **unlock tree** by access state: filter chips *Todas / Disponibles /
    Desbloqueadas / Sin acceso*, coloured **green** (unlocked) / **blue** (available — its parent is
    unlocked, i.e. in the `InnovationDeck`) / **grey** (no access; names the parent it still needs).
  - **Fabricar** — the locations you can still build, plus gear grouped by **localización** or by
    **tipo**; a recipe is **green with an enabled *Crear*** iff `Craft` would succeed, and otherwise
    dimmed with what is missing.
  Rules text (effects) is shown verbatim; the group resolves it.

`SettlementScreen` renders a **thin top step bar** (the 11 steps as numbered chips, the current one
highlighted and forward-skips disabled) with the body spanning the full width — the old
half-screen left rail is gone. Step 1 renders Set Up, step 7 supplies the Develop body, and every
other step shows the "Pendiente" placeholder — all inside the same chrome, so no step is
unreachable. Each action calls a pure `:core` use-case through the
controller and persists the mutated `SettlementRecord` via `SettlementRepository.update` (local
store + outbox), so it syncs like any other change. Rules are **total**: an invalid action returns
`StepResult.Blocked(reason)` instead of throwing, and the screen shows the reason inline with the
record unchanged.

| Rule | Enforcement |
| --- | --- |
| **Endeavor income** | Entering Settlement Step 3 runs `GainEndeavors`, adding the Step 3 breakdown (base **+1 per returning survivor**, funded from the population in this slice) to the pool once per phase. |
| **Innovate** | `Innovate`: only at a **built** location granting it (the Lantern Hoard is built in the seed) — spends the action's endeavors **and** resources (1 Bone + 1 Organ + 1 Hide in 1.5), is **once per Settlement Phase** (a phase marker reset at the year boundary), and **draws 2, keeps 1**. |
| **Build** | `BuildLocation`: the location's `requires` must already be built, storage must cover its resource cost, and the pool must cover its `buildEndeavors` (prereq tree). |
| **Craft** | `Craft`: only at a **built** location, using one of its `GearRecipe`s — the recipe's innovation `requires` must be unlocked and storage must cover its resource cost; the gear is then added to the settlement. |
| **Generic endeavor** | `SpendEndeavor`: a rule carrying the `[endeavor]` symbol costs −1; the player resolves the effect. |
| **Storage** | `AddResourceEntry` appends a gain, but blocks a spend that would take a resource below 0. |
| **Step 11** | Unspent endeavors are discarded at the year boundary (`AdvanceStep`), not carried over. |

**Craft (Step 7).** `Gear` carries a `keywords`/`rules`/`modifiers` model, and `GearRecipe` a
resource cost plus innovation prerequisites; recipes travel on the built `SettlementLocation`.
`Craft` enforces built location + recipe + prerequisites + storage, then adds the gear to the
settlement. The **gear `Modifier` model** (`target`/`value`/`scope`/`condition`) is ready for
curated data: settlement-scope modifiers feed derived values, survivor-scope ones are stored and
applied once a `Survivor` model exists. The gear definitions and recipes are **auto-extracted** from
the 1.5 cards by `tools/content-ingest/ingest_gear.py` (see *Content ingestion pipeline*):
`pdftotext -raw`/`-layout` were not enough (they strip spaces or flatten columns), so it uses
`pdftotext -bbox-layout` (word boxes) to recover spaced text and separate columns. A prerequisite
that is not an innovation name (e.g. `Requiere Calor`) is kept as display-only `requiresText` and
does not gate crafting. The card bonuses remain `[gap]` (they are conditional rule text), never
guessed.

**Known limitation (Ruling CB).** The extracted `buildCost`/`resourceCost` are keyed by a **specific
resource card name** that approximates the card's printed material *type* (`3 x hueso` →
`Hueso de Monstruo`), because the model has no resource-type key yet. A settlement holding a
different resource of the same type would be wrongly `Blocked`; resolving costs by type is a
follow-up work unit.

**Known limitation (upgrade path).** The shipped seed enables `core-1.6` and `core-1.5`, but a
campaign stored before that change keeps its original `enabledPacks` (often empty) and therefore
composes an empty catalog with no in-app path to fix it. The app is pre-release with no persisted
production campaigns, so this is deferred: an existing local campaign must be **re-created** (or the
store re-seeded) to pick up the packs.

**Deferred:** gear `modifiers` content (the card bonuses are conditional rule text, recorded as
`[gap]`), applying survivor-scope modifiers (needs the `Survivor` model), executable effects (shown
as text, not resolved), a full per-survivor `Survivor` model (returnees and deaths are
settlement-level counters), and 1.6-specific rules (the local rulebook is 1.5 and the 1.6 sheet is
image-only).

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

## Content packs

Game content is **data, not code**. `:core` ships a modular content-catalog seam
(`io.kdmtracker.core.content`) so the rules engine stays generic while the cards,
locations, quarries, nemeses and resources a campaign can use live in selectable
**content packs**. Nothing about the settlement record is hardcoded in `rules/`.

A `ContentPack` is identified by a stable string id and carries the typed `ContentBundle`
the domain reads:

```kotlin
@Serializable
data class ContentPack(
    val id: String,                 // e.g. "core-1.6", "gorm"
    val version: String,
    val name: String,
    val requires: List<String> = emptyList(),   // pack ids that must also be enabled
    val bundle: ContentBundle = ContentBundle(),
)

@Serializable
data class ContentBundle(
    val innovations: List<Innovation> = emptyList(),        // names + consequences
    val locations: List<SettlementLocation> = emptyList(),  // names + prerequisites
    val resources: List<ResourceDefinition> = emptyList(),  // names + typed category
    val fightingArts: List<ContentEntry> = emptyList(),     // names + effect text
    val disorders: List<ContentEntry> = emptyList(),        // names + effect text
    val timeline: List<TimelineEntry> = emptyList(),
    val quarries: List<String> = emptyList(),
    val nemeses: List<String> = emptyList(),
)
```

Two pack kinds share one model:

- **Edition packs** — a base game edition (`core-1.6`), with no `requires`.
- **Expansion packs** — additions (e.g. `gorm`, `dragon-king`), declaring the packs
  they build on via `requires`.

`ContentCatalogComposer.compose(packs)` validates that every `requires` id is present
(returning `CatalogResult.MissingRequirement` otherwise) and, on success, merges the
packs into a `ContentCatalog`. The catalog exposes the **typed definitions**
(`innovationDefinitions`/`locationDefinitions`/`resourceDefinitions`/`timeline`) and
derives the display-name lists (`innovations`/`locations`/`resources`/`quarries`/
`nemeses`) as the **deduped, sorted union** across packs — so composition is
order-independent and deterministic.

```kotlin
val result = ContentCatalogComposer.compose(listOf(Core16Content.pack, gorm.pack))
// CatalogResult.Ok(catalog) — or MissingRequirement(packId, missing)
```

A campaign **selects** the packs it plays with through its `enabledPacks` list
(alongside `rulesetId` and `rulesetOverrides`), so the same settlement record model
serves any edition + expansion combination. `Core16Content.pack` is the shipped
`core-1.6` **seed** — the location tree, the initial innovation `Language`, the base
resources, and the core quarries and nemeses. It is deliberately not exhaustive: the
full 1.5/1.6 card sets arrive later through the content-ingestion pipeline, split
across edition and expansion packs.

### Content ingestion pipeline

The `core-1.5` edition pack (`Legacy15Content.pack`, id `core-1.5`) is built from the
Spanish 1.5 card PDFs by the scripts in `tools/content-ingest/`. The source PDFs live under
`external-assets/`, which is **git-ignored and never committed**; each `ingest_*.py` script
parses its PDFs with `pdftotext` and writes **committed** Kotlin sources under
`core/src/commonMain/kotlin/io/kdmtracker/core/content/generated/`. The generator is the
source of truth — the generated files are never edited by hand — and **the app never reads
a PDF at runtime**; it only reads the committed Kotlin.

| Generated file | Script | Content |
| --- | --- | --- |
| `Core15Innovations.kt` | `ingest_innovations.py` | 42 innovations + consequence graph |
| `Core15Resources.kt` | `ingest_resources.py` | 56 resources + typed category |
| `Core15FightingArts.kt` | `ingest_fighting_arts.py` | 24 fighting arts + 23 disorders |
| `Core15Locations.kt` | `ingest_locations.py` | settlement locations: build cost + actions |
| `Core15Gear.kt` | `ingest_gear.py` | gear definitions + crafting recipes (via `pdftotext -bbox-layout`) |

Output is **deterministic**: entries and every nested list are sorted, and no timestamps or
paths are embedded, so re-running a script over unchanged inputs is **byte-identical**.
See [`tools/content-ingest/README.md`](tools/content-ingest/README.md) for the exact
re-run commands, the parsing model, and the known gaps (image-only card decks, the
secret-arts deck, dropped cost-icon glyphs).

## Sync hub (server)

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
[Sync (Android client)](#sync-android-client)); `baseRev`-based detection remains client-side and is
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

## Project structure

```text
kdm-tracker/
├── core/                              # platform-agnostic domain + rules (Kotlin multiplatform: jvm + android)
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/kotlin/io/kdmtracker/core/
│       │   ├── CoreModule.kt          # version marker for the pure core
│       │   ├── domain/                # Settlement, Campaign + value objects (Ulid, HLC, enums)
│       │   ├── content/               # ContentPack, ContentCatalog, Core16Content (seed pack)
│       │   ├── rules/                 # LanternYearFlow, SettlementStepCatalog, DerivedValues
│       │   ├── ruleset/               # Ruleset, Core16Ruleset, RulesetOverrides
│       │   ├── sync/                  # SyncApi/SyncStore ports, MergePolicy (LWW), SyncEngine
│       │   ├── transfer/              # TransferDocument/TransferMapper (schema v5)
│       │   └── usecase/               # AdvanceStep (lantern-year transition use case)
│       └── commonTest/kotlin/io/kdmtracker/core/  # unit tests for the above
├── data/                              # SQLDelight local persistence (Kotlin multiplatform: jvm + android)
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/
│       │   ├── kotlin/io/kdmtracker/data/
│       │   │   ├── SqlDelightCampaignStore.kt  # :core CampaignStore port over the generated DB
│       │   │   └── SqlDelightSyncStore.kt      # :core SyncStore port (outbox, cursor, applyMerged)
│       │   └── sqldelight/io/kdmtracker/data/db/
│       │       ├── Campaign.sq                 # campaign table + sync columns
│       │       ├── Settlement.sq               # settlement table + sync columns
│       │       ├── Outbox.sq                   # pending local changes awaiting push
│       │       └── SyncCursor.sq               # per-campaign pull high-water mark
│       ├── androidMain/kotlin/io/kdmtracker/data/
│       │   └── KdmDatabaseFactory.kt           # opens the on-device DB; driver owns migrations
│       └── jvmTest/kotlin/io/kdmtracker/data/  # schema, store, contract and durability tests
├── testing/shared-kotlin/             # shared CampaignStore contract + in-memory fake (test-only)
├── server/                            # Spring Boot app (web, persistence, seeding)
│   ├── build.gradle.kts
│   ├── Dockerfile                     # Multi-stage: build+test+lint gate, non-root runtime with /app/data
│   ├── .env.example
│   └── src/
│       ├── main/kotlin/io/kdmtracker/
│       │   ├── KdmTrackerApplication.kt   # @SpringBootApplication entry point
│       │   ├── auth/                      # users, campaigns, membership, JWT auth
│       │   ├── domain/                    # Settlement, Survivor, Resource, Innovation, LogEntry + enums
│       │   ├── migration/                 # LegacyToChangeLogMigration (one-shot legacy → change-log)
│       │   ├── repository/                # Spring Data JPA repositories
│       │   ├── seed/DatabaseSeeder.kt     # First-run seed: settlement + basic resources
│       │   ├── sync/                      # change-log persistence + pull/push endpoints
│       │   └── web/                       # Controllers (dashboard, survivors, resources, innovations, log, health)
│       │       └── form/                  # Validated form-backing objects
│       ├── main/resources/
│       │   ├── application.yaml           # H2 file DB (./data/kdm-tracker), ddl-auto: update, ${PORT:8080}
│       │   ├── static/css/style.css       # Hand-written stylesheet (no CDN)
│       │   └── templates/                 # Thymeleaf views (+ shared fragments)
│       ├── test/kotlin/io/kdmtracker/     # Smoke test + one flow test per entity (MockMvc)
│       └── test/resources/application.yaml # In-memory H2 for fast tests
├── app-android/                       # Compose Multiplatform Android app (walking skeleton)
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   └── kotlin/io/kdmtracker/app/
│       │       ├── MainActivity.kt          # ComponentActivity entry point
│       │       ├── KdmTrackerApp.kt         # root composable; renders by state.screen
│       │       ├── LanternYearController.kt # plain-Kotlin loop state machine over :core
│       │       ├── LanternYearViewModel.kt  # retains the loop; opens the on-device SQLDelight DB
│       │       ├── SettlementRepository.kt  # settlement/campaign holder backed by CampaignStore
│       │       ├── UiState.kt               # UiState + Screen sealed interface
│       │       ├── sync/                    # HttpSyncApi, TokenStore, SyncCoordinator
│       │       └── ui/                      # stateless Compose screens (Home/Settlement/Hunt/Showdown)
│       └── test/kotlin/io/kdmtracker/app/   # controller, loop, repository and sync tests
├── .githooks/
│   └── pre-commit                     # ./gradlew -q spotlessCheck (enable via git config)
├── .dockerignore                      # build-context root; ignores .git, build, data, .superpowers, ...
├── .editorconfig                      # drives ktlint (utf-8, indent 4, max line 120)
├── .gitignore                         # ignores local H2 file artifacts, local.properties and build output
├── LICENSE
├── README.md
├── build.gradle.kts                   # root: applies Spotless to every subproject
├── gradlew / gradlew.bat              # Gradle wrapper (pinned 9.7.1)
├── gradle/
│   ├── libs.versions.toml             # version catalog (single source of versions)
│   └── wrapper/                       # wrapper jar + properties
└── settings.gradle.kts                # rootProjectName = kdm-tracker; includes :core, :data, :server and :app-android
```

## Scripts

| Script                    | Command              | Description                                    |
| ------------------------- | -------------------- | ---------------------------------------------- |
| `./gradlew :server:bootRun` | Start the dev server | Runs the app on `PORT` (default 8080)        |
| `./gradlew build`         | Compile + test + jar | Runs tests, lint, and assembles every module   |
| `./gradlew :core:jvmTest` | Core tests           | Pure domain/rules/use-case unit tests          |
| `./gradlew :data:jvmTest` | Data tests           | SQLDelight schema + campaign-store tests       |
| `./gradlew :server:test`  | Server tests         | MockMvc smoke + entity flow tests              |
| `./gradlew :app-android:assembleDebug` | Build debug APK | Compiles the app into `app-debug.apk`      |
| `./gradlew :app-android:testDebugUnitTest` | App tests | Controller, loop, repository and sync tests |
| `./gradlew spotlessCheck` | Lint                 | ktlint style check (driven by `.editorconfig`) |
| `./gradlew spotlessApply` | Format               | Auto-fix ktlint violations                     |

## Environment variables

| Variable | Default | Description            |
| -------- | ------- | ---------------------- |
| `PORT`   | `8080`  | HTTP listen port       |
| `SYNC_JWT_SECRET` | dev default (non-production) | HS256 signing secret for bearer tokens. Required outside dev/test: boot fails fast if the committed default is still in use and no `dev`/`test` profile is active. |
| `SYNC_JWT_EXPIRY_SECONDS` | `86400` | Bearer-token lifetime in seconds. |
| `SYNC_MIGRATION_RUN_ON_STARTUP` | `false` | When `true`, runs the one-shot legacy migration on boot (see the migration runbook). Undefined/`false` for normal boots. |

## Tests

```bash
./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test
```

The `:core` suite is pure Kotlin unit tests (domain invariants, the lantern-year flow, the ruleset
seam, the transfer mapper, and the sync engine — per-record LWW merge, tombstone ordering and the
pull/merge/push pass). The `:data` suite runs the shared `CampaignStore` contract against
`SqlDelightCampaignStore` over a temp-file database, plus schema, soft-delete, JSON round-trip,
reopen-durability, outbox and sync-store tests. The `:app-android` suite covers the lantern-year
controller, the end-to-end loop, the store-backed settlement repository, the multi-campaign Home
(selection, cards and sharing badges) and the sync coordinator (in-flight guard, off-thread
dispatch, non-fatal errors, sync-all). The `:server` suite boots the app with
`@SpringBootTest` + MockMvc against an in-memory H2 database and runs without sleeps:

- Smoke: `GET /` renders the dashboard (200), `GET /health` returns `{"status":"ok"}`.
- Survivor flow: create via POST → appears in the list with KDM defaults (age 12, MOV 5, SPD 4,
  ACC 5, STR 1, DEX 8, EVA 0, ARM 0); blank name rejected; edit persists.
- Resource flow: seeded basics present; `+1` increases the quantity, repeated `−1` never drops
  below 0; custom monster resource can be created.
- Innovation flow: create → appears in the list with the unlock year.
- Log flow: entries render sorted by lantern year desc, then `createdAt` desc; blank title rejected.
- Sync hub: the pull/push endpoints enforce auth + campaign membership, assign monotonic per-campaign
  `rev`s, and replay idempotently; the legacy migration emits one record per row and is a no-op on a
  second run.

Tests run automatically as part of `./gradlew build`.

## License

MIT — see [LICENSE](LICENSE).
