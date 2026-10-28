# Development guide

Everything needed to build, run, test and release the project. Back to the [README](../README.md).

## Stack versions

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
stub and arrive in later plans (see [Settlement Steps 1 & 7](settlement-steps.md)). Entering
Step 3 funds the endeavor pool; hunt rewards are not modeled yet, so resources are recorded
manually on the Develop screen. State
persists locally through `:data` (see [Persistence](sync.md#persistence)) and syncs to the hub through the
client sync engine (see [Sync (Android client)](sync.md#sync-android-client)). The Home and its sharing
model are described in [Home and sharing](sync.md#home-and-sharing).

Build the debug APK:

```bash
./gradlew :app-android:assembleDebug
# Output: app-android/build/outputs/apk/debug/app-debug.apk
```

Install it on a connected device or emulator:

```bash
adb install -r app-android/build/outputs/apk/debug/app-debug.apk
```

### Releasing

Pushing a `v*` tag builds a signed APK and publishes it as a GitHub Release
(`.github/workflows/release.yml`). The tag sets the version and the run number the version code:

```bash
git tag v0.3.0 && git push origin v0.3.0
```

The signing key is a repository secret set (`KDM_KEYSTORE_BASE64`, `KDM_KEYSTORE_PASSWORD`,
`KDM_KEY_ALIAS`, `KDM_KEY_PASSWORD`); the keystore itself stays out of the repository. Losing it
means installed copies cannot be updated, so keep a backup. To build a signed APK locally, export
`KDM_KEYSTORE_FILE`, `KDM_KEYSTORE_PASSWORD`, `KDM_KEY_ALIAS` and `KDM_KEY_PASSWORD`, then run
`./gradlew :app-android:assembleRelease`.

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
