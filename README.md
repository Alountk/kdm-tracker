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
| Lint        | ktlint via Spotless (`com.diffplug.spotless`) 7.0.4     |

## Requirements

- JDK 21+ (the build targets 21; any newer JDK works — the local daemon here runs on JDK 25)
- Docker (optional, for the container workflow)

No global Gradle install is required — use the committed wrapper (`./gradlew`).

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

```bash
docker build -t bp-kdm-tracker .
docker run -d --name bp-kdm-tracker -p 8703:8080 -v kdm-data:/app/data bp-kdm-tracker

curl -fsS -o /dev/null -w "%{http_code}" http://localhost:8703/
# Expected: 200

curl http://localhost:8703/health
# Expected: {"status":"ok"}

docker rm -f bp-kdm-tracker
```

The named volume `kdm-data` is mounted at `/app/data`, where the container writes the H2 file
database — campaign data survives container replacement.

## Project structure

```text
kdm-tracker/
├── src/
│   ├── main/kotlin/io/kdmtracker/
│   │   ├── KdmTrackerApplication.kt   # @SpringBootApplication entry point
│   │   ├── domain/                    # Settlement, Survivor, Resource, Innovation, LogEntry + enums
│   │   ├── repository/                # Spring Data JPA repositories
│   │   ├── seed/DatabaseSeeder.kt     # First-run seed: settlement + basic resources
│   │   └── web/                       # Controllers (dashboard, survivors, resources, innovations, log, health)
│   │       └── form/                  # Validated form-backing objects
│   ├── main/resources/
│   │   ├── application.yaml           # H2 file DB (./data/kdm-tracker), ddl-auto: update, ${PORT:8080}
│   │   ├── static/css/style.css       # Hand-written stylesheet (no CDN)
│   │   └── templates/                 # Thymeleaf views (+ shared fragments)
│   ├── test/kotlin/io/kdmtracker/     # Smoke test + one flow test per entity (MockMvc)
│   └── test/resources/application.yaml # In-memory H2 for fast tests
├── .githooks/
│   └── pre-commit                     # ./gradlew -q spotlessCheck (enable via git config)
├── .dockerignore
├── .editorconfig                      # drives ktlint (utf-8, indent 4, max line 120)
├── .env.example
├── .gitignore                         # ignores ./data (local H2 files)
├── Dockerfile                         # Multi-stage: build+test+lint gate, non-root runtime with /app/data
├── LICENSE
├── README.md
├── build.gradle.kts                   # plugins, dependencies, Spotless, bootJar name
├── gradlew / gradlew.bat              # Gradle wrapper (pinned 9.7.1)
├── gradle/wrapper/                    # wrapper jar + properties
└── settings.gradle.kts                # rootProjectName = kdm-tracker
```

## Scripts

| Script                   | Command                 | Description                                  |
| ------------------------ | ----------------------- | -------------------------------------------- |
| `./gradlew bootRun`      | Start the dev server    | Runs the app on `PORT` (default 8080)        |
| `./gradlew build`        | Compile + test + jar    | Runs tests, lint, and produces the boot jar  |
| `./gradlew test`         | Run tests               | MockMvc smoke + entity flow tests            |
| `./gradlew spotlessCheck`| Lint                    | ktlint style check (driven by `.editorconfig`) |
| `./gradlew spotlessApply`| Format                  | Auto-fix ktlint violations                   |

## Environment variables

| Variable | Default | Description            |
| -------- | ------- | ---------------------- |
| `PORT`   | `8080`  | HTTP listen port       |

## Tests

```bash
./gradlew test
```

The suite boots the app with `@SpringBootTest` + MockMvc against an in-memory H2 database and runs
without sleeps:

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
