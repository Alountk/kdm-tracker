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
| Persistence | Spring Data JPA + H2 file database (in `./data/`)       |
| Views       | Thymeleaf (server-side rendering)                       |
| Android app | Compose Multiplatform 1.12.1 + Material 3 (AGP 9.4.1)   |
| Lint        | ktlint via Spotless (`com.diffplug.spotless`) 7.0.4     |

## Requirements

- JDK 21+ (the build targets 21; any newer JDK works — the local daemon here runs on JDK 25)
- Docker (optional, for the container workflow)

No global Gradle install is required — use the committed wrapper (`./gradlew`).

### Android (`:app-android`)

Building the app requires the Android SDK (command-line tools). This workspace uses SDK root
`/opt/homebrew/share/android-commandlinetools` with:

- `platform-tools`
- `platforms;android-37.0`
- `build-tools;36.0.0`

Point Gradle at it with a repository-root `local.properties` (git-ignored — never commit it):

```properties
sdk.dir=/opt/homebrew/share/android-commandlinetools
```

The module uses the same JDK 21 toolchain and Gradle 9.7.1 wrapper as the rest of the repo.

## Quickstart (local)

```bash
# Enable the plain pre-commit hook (runs `./gradlew -q spotlessCheck`):
git config core.hooksPath .githooks

./gradlew bootRun
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
docker run -d --name bp-kdm-tracker -p 8703:8080 -v kdm-data:/app/data bp-kdm-tracker

curl -fsS -o /dev/null -w "%{http_code}" http://localhost:8703/
# Expected: 200

curl http://localhost:8703/health
# Expected: {"status":"ok"}

docker rm -f bp-kdm-tracker
```

The named volume `kdm-data` is mounted at `/app/data`, where the container writes the H2 file
database — campaign data survives container replacement.

## Quickstart (Android app)

The `:app-android` module is a Compose Multiplatform Android app that consumes `:core`. It is
currently a **walking skeleton**: a navigable lantern-year loop (Settlement → Hunt → Showdown)
driven by `:core` rules, with "Pendiente" stubs for the step bodies — offline persistence and the
real step UIs arrive in later plans.

Build the debug APK:

```bash
./gradlew :app-android:assembleDebug
# Output: app-android/build/outputs/apk/debug/app-debug.apk
```

Install it on a connected device or emulator:

```bash
adb install -r app-android/build/outputs/apk/debug/app-debug.apk
```

## Project structure

```text
kdm-tracker/
├── core/                              # platform-agnostic domain + rules (Kotlin multiplatform: jvm + android)
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/kotlin/io/kdmtracker/core/
│       │   ├── CoreModule.kt          # version marker for the pure core
│       │   ├── domain/                # Settlement, Campaign + value objects (Ulid, HLC, enums)
│       │   ├── rules/                 # LanternYearFlow, SettlementStepCatalog, DerivedValues
│       │   ├── ruleset/               # Ruleset, Core16Ruleset, RulesetOverrides
│       │   ├── transfer/              # TransferDocument/TransferMapper (schema v3)
│       │   └── usecase/               # AdvanceStep (lantern-year transition use case)
│       └── commonTest/kotlin/io/kdmtracker/core/  # unit tests for the above
├── server/                            # Spring Boot app (web, persistence, seeding)
│   ├── build.gradle.kts
│   ├── Dockerfile                     # Multi-stage: build+test+lint gate, non-root runtime with /app/data
│   ├── .env.example
│   └── src/
│       ├── main/kotlin/io/kdmtracker/
│       │   ├── KdmTrackerApplication.kt   # @SpringBootApplication entry point
│       │   ├── domain/                    # Settlement, Survivor, Resource, Innovation, LogEntry + enums
│       │   ├── repository/                # Spring Data JPA repositories
│       │   ├── seed/DatabaseSeeder.kt     # First-run seed: settlement + basic resources
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
│       │       ├── SettlementRepository.kt  # in-memory settlement holder
│       │       ├── UiState.kt               # UiState + Screen sealed interface
│       │       └── ui/                      # stateless Compose screens (Settlement/Hunt/Showdown)
│       └── test/kotlin/io/kdmtracker/app/   # controller, loop and repository unit tests
├── .githooks/
│   └── pre-commit                     # ./gradlew -q spotlessCheck (enable via git config)
├── .dockerignore                      # build-context root; ignores .git, build, data, .superpowers, ...
├── .editorconfig                      # drives ktlint (utf-8, indent 4, max line 120)
├── .gitignore                         # ignores ./data (local H2 files), local.properties and build output
├── LICENSE
├── README.md
├── build.gradle.kts                   # root: applies Spotless to every subproject
├── gradlew / gradlew.bat              # Gradle wrapper (pinned 9.7.1)
├── gradle/
│   ├── libs.versions.toml             # version catalog (single source of versions)
│   └── wrapper/                       # wrapper jar + properties
└── settings.gradle.kts                # rootProjectName = kdm-tracker; includes :core, :server and :app-android
```

## Scripts

| Script                    | Command              | Description                                    |
| ------------------------- | -------------------- | ---------------------------------------------- |
| `./gradlew :server:bootRun` | Start the dev server | Runs the app on `PORT` (default 8080)        |
| `./gradlew build`         | Compile + test + jar | Runs tests, lint, and assembles every module   |
| `./gradlew :core:jvmTest` | Core tests           | Pure domain/rules/use-case unit tests          |
| `./gradlew :server:test`  | Server tests         | MockMvc smoke + entity flow tests              |
| `./gradlew :app-android:assembleDebug` | Build debug APK | Compiles the app into `app-debug.apk`      |
| `./gradlew :app-android:testDebugUnitTest` | App tests | Controller, loop and repository tests    |
| `./gradlew spotlessCheck` | Lint                 | ktlint style check (driven by `.editorconfig`) |
| `./gradlew spotlessApply` | Format               | Auto-fix ktlint violations                     |

## Environment variables

| Variable | Default | Description            |
| -------- | ------- | ---------------------- |
| `PORT`   | `8080`  | HTTP listen port       |

## Tests

```bash
./gradlew :core:jvmTest :app-android:testDebugUnitTest :server:test
```

The `:core` suite is pure Kotlin unit tests (domain invariants, the lantern-year flow, the ruleset
seam and the transfer mapper). The `:app-android` suite covers the lantern-year controller, the
end-to-end loop and the in-memory settlement repository. The `:server` suite boots the app with
`@SpringBootTest` + MockMvc against an in-memory H2 database and runs without sleeps:

- Smoke: `GET /` renders the dashboard (200), `GET /health` returns `{"status":"ok"}`.
- Survivor flow: create via POST → appears in the list with KDM defaults (age 12, MOV 5, SPD 4,
  ACC 5, STR 1, DEX 8, EVA 0, ARM 0); blank name rejected; edit persists.
- Resource flow: seeded basics present; `+1` increases the quantity, repeated `−1` never drops
  below 0; custom monster resource can be created.
- Innovation flow: create → appears in the list with the unlock year.
- Log flow: entries render sorted by lantern year desc, then `createdAt` desc; blank title rejected.

Tests run automatically as part of `./gradlew build`.

## License

MIT — see [LICENSE](LICENSE).
