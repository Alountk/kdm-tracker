# Project structure

Repository layout. Layer rules and the code-comment convention live in [ARCHITECTURE.md](ARCHITECTURE.md). Back to the [README](../README.md).

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
