# Core Foundations (`:core`) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extract a pure, framework-free, fully tested Kotlin Multiplatform `:core` module holding the
domain model, the versioned ruleset seam, and the phase/step state machine, with the existing Spring
app moved to `:server` and consuming `:core`.

**Architecture:** `:core` is a Kotlin Multiplatform library with a single `jvm()` target for now (an
`androidTarget` is added in the walking-skeleton plan). It has no Spring, no JPA, no Android, and no
platform APIs beyond `kotlinx-serialization`, `kotlinx-datetime`, and `kotlinx-coroutines`. The
existing Spring Boot app becomes `:server` and depends on `:core`; it keeps working unchanged. Rules
are data-driven: the stable mechanism lives in `core/rules`, the content lives in `core/ruleset`.

**Tech Stack:** Kotlin 2.3.21, Gradle 9.7.1 (Kotlin DSL), Kotlin Multiplatform, kotlinx-serialization,
kotlinx-datetime, kotlinx-coroutines, kotlin-test. Spring Boot 4.1.1 (server, unchanged). ktlint via
Spotless.

**Spec:** `docs/superpowers/specs/2026-10-05-kdm-tracker-offline-first-kmp-design.md`

## Global Constraints

- Kotlin `2.3.21`; JDK toolchain `21`. Gradle wrapper stays `9.7.1`.
- `:core` dependencies are **only** `kotlinx-serialization`, `kotlinx-datetime`, `kotlinx-coroutines`. Nothing else.
- `:core` must not import `org.springframework.*`, `jakarta.persistence.*`, `android.*`, or `java.time.*`.
- IDs are **ULID**, client-generated. Every mutable record carries `SyncMeta` (`updatedAt` HLC, `deletedAt`, `rev`).
- **Zero magic numbers in `core/rules`** — rules read their values from a `Ruleset`.
- Versioning: SemVer `0.1.0` baseline; project `version = "0.1.0"`.
- Conventional Commits; **no** `Co-Authored-By` or AI attribution in commit messages.
- Style is enforced by `./gradlew spotlessCheck` (ktlint via `.editorconfig`: utf-8, indent 4, max line 120).

## Review Focus

These input classes are the most likely to bite a user and are pinned by a test in the owning task:

1. **Editing an earlier step after later steps** — derived values (endeavors, storage, survival limit) must recompute from primitives, never stay stale. *(Task 10)*
2. **Special Showdown year** — the flow must loop from step 9 back to step 5, not advance to step 10. *(Task 9)*
3. **Nemesis year** — the Hunt Phase must be skipped; the flow goes settlement-end → showdown. *(Task 9)*
4. **Hunt failure / TPK** — the next Settlement Phase starts with zero returning survivors, so base endeavors are 0. *(Task 9)*
5. **Step 11 with unspent endeavors** — ending the phase must report the discarded endeavor count and require an explicit confirmation, never silently zero it. *(Task 9)*

---

## File Structure

```text
kdm-tracker/
├── settings.gradle.kts                         # include(":core"); include(":server")
├── build.gradle.kts                            # root: minimal shared config only
├── gradle/libs.versions.toml                   # version catalog (single source of versions)
├── core/
│   ├── build.gradle.kts                        # KMP · jvm() target · kotlinx deps
│   └── src/
│       ├── commonMain/kotlin/io/kdmtracker/core/
│       │   ├── domain/      # enums, Ulid, HybridLogicalClock, SyncMeta, Campaign, Settlement
│       │   ├── rules/       # PhaseStep, FlowTransition, LanternYearFlow, derived-value recompute
│       │   ├── ruleset/     # Ruleset, Core16Ruleset, RulesetOverrides
│       │   ├── transfer/    # TransferDocument v3 DTOs (@Serializable) + mappers
│       │   └── usecase/     # AdvanceStep (thin orchestration over rules)
│       └── commonTest/kotlin/io/kdmtracker/core/
│           ├── domain/ rules/ ruleset/ transfer/
└── server/                                     # existing Spring app moved here
    ├── build.gradle.kts                        # was root build.gradle.kts + dependsOn(project(":core"))
    ├── src/ Dockerfile .dockerignore .env.example
```

---

### Task 1: Gradle multi-module restructure (existing app → `:server`)

**Files:**
- Create: `gradle/libs.versions.toml`
- Modify: `settings.gradle.kts`
- Create: `server/build.gradle.kts` (from root `build.gradle.kts`)
- Modify: `build.gradle.kts` (root → minimal)
- Move: `src/` → `server/src/`; `Dockerfile`, `.dockerignore`, `.env.example` → `server/`
- Modify: `Dockerfile` (build paths after the move)

**Interfaces:**
- Consumes: nothing.
- Produces: a working `:server` Gradle project that still runs `./gradlew :server:test` green, and a catalog exposing `kotlin = "2.3.21"`, `springBoot = "4.1.1"`, `kotlinxSerialization`, `kotlinxDatetime`, `kotlinxCoroutines`, `kotlinTest`.

- [ ] **Step 1: Create the version catalog** with library coordinates above, then run `./gradlew help` and expect a successful configuration.
- [ ] **Step 2: Move the app sources** with `git mv src server/src` and `git mv Dockerfile .dockerignore .env.example server/`.
- [ ] **Step 3: Set `settings.gradle.kts`** to `rootProject.name = "kdm-tracker"` + `include(":server")`, and move the current plugin/dependency config into `server/build.gradle.kts` with `version = "0.1.0"` and coordinates from the catalog.
- [ ] **Step 4: Make the root `build.gradle.kts` minimal** — Spotless config applying to all subprojects only.
- [ ] **Step 5: Fix the Dockerfile paths** so the multi-stage build copies `server/` and runs the `:server:bootJar` task.
- [ ] **Step 6: Verify** `./gradlew :server:test :server:spotlessCheck` passes (same tests as before the move).
- [ ] **Step 7: Commit** `refactor: move spring app to :server module with version catalog`.

---

### Task 2: Create the `:core` Kotlin Multiplatform module

**Files:**
- Create: `core/build.gradle.kts`
- Modify: `settings.gradle.kts` (add `include(":core")`)
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/CoreModule.kt`
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/CoreModuleTest.kt`

**Interfaces:**
- Consumes: the version catalog from Task 1.
- Produces: a compiling `:core` module with `jvm()` target and the three kotlinx dependencies. `CoreModule.VERSION: String = "0.1.0"` as a smoke-testable symbol.

- [ ] **Step 1: Write the failing test** asserting `CoreModule.VERSION == "0.1.0"`.
- [ ] **Step 2: Run** `./gradlew :core:jvmTest` — expected FAIL (`:core` does not exist).
- [ ] **Step 3: Create `core/build.gradle.kts`** with `kotlin("multiplatform")`, `jvm()`, `commonMain` deps (`libs.kotlinx.serialization.json`, `libs.kotlinx.datetime`, `libs.kotlinx.coroutines.core`), `commonTest` dep (`libs.kotlin.test`), and `compilerOptions` matching the server (`-Xjsr305=strict`, `-Xannotation-default-target=param-property`).
- [ ] **Step 4: Implement `CoreModule`** and run `./gradlew :core:jvmTest` — expected PASS.
- [ ] **Step 5: Commit** `build: add pure kotlin multiplatform :core module`.

---

### Task 3: Domain enums

**Files:**
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/domain/SettlementPhase.kt`
- Create: `.../domain/StepType.kt`
- Create: `.../domain/HuntStatus.kt`
- Create: `.../domain/Sex.kt`
- Create: `.../domain/ResourceCategory.kt`
- Create: `.../domain/PrincipleCategory.kt`
- Create: `.../domain/MilestoneKey.kt`
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/domain/DomainEnumsTest.kt`

**Interfaces:**
- Produces (exact names; must match the existing server enums so the server can consume them):
  - `enum class SettlementPhase { SETTLEMENT, HUNT, SHOWDOWN }`
  - `enum class StepType { BOOKKEEPING, DECISION, BRANCHING }`
  - `enum class HuntStatus { ACTIVE, QUARRY_FOUND, AMBUSH, FAILED, RUN_AWAY }`
  - `enum class Sex { MALE, FEMALE, OTHER }`
  - `enum class ResourceCategory { BASIC, SPECIAL, MONSTER }`
  - `enum class PrincipleCategory { DEATH, NEW_LIFE, SOCIETY }`
  - `enum class MilestoneKey { PRINCIPLE_DEATH, PRINCIPLE_NEW_LIFE, PRINCIPLE_SOCIETY, HOODED_KNIGHT, GAME_OVER }`

- [ ] **Step 1: Write the failing test** asserting each enum's exact `entries` (names + order) and that `MilestoneKey` has 5 values.
- [ ] **Step 2: Run** `./gradlew :core:jvmTest --tests '*DomainEnumsTest'` — expected FAIL.
- [ ] **Step 3: Implement the enums** (declarations only; no logic).
- [ ] **Step 4: Run** the test — expected PASS.
- [ ] **Step 5: Commit** `feat(core): add domain enums`.

---

### Task 4: Sync metadata types (`Ulid`, `HybridLogicalClock`, `SyncMeta`)

**Files:**
- Create: `.../domain/Ulid.kt`
- Create: `.../domain/HybridLogicalClock.kt`
- Create: `.../domain/SyncMeta.kt`
- Test: `.../domain/UlidTest.kt`, `.../domain/HybridLogicalClockTest.kt`

**Interfaces:**
- Produces:
  - `@JvmInline value class Ulid(val value: String)` with `companion object { fun generate(clockMillis: Long, random: () -> Int): Ulid; fun parse(value: String): Ulid }`; `parse` throws `IllegalArgumentException` on malformed input.
  - `data class HybridLogicalClock(val millis: Long, val counter: Int) : Comparable<HybridLogicalClock>` with `companion object { fun tick(previous: HybridLogicalClock?, wallMillis: Long): HybridLogicalClock }`.
  - `data class SyncMeta(val updatedAt: HybridLogicalClock, val rev: Long = 0, val deletedAt: HybridLogicalClock? = null)`.

- [ ] **Step 1: Write the failing tests** — ULID: 26-char Crockford base32 output, 10-char time prefix from `clockMillis`; monotonic sort by time; `parse` rejects wrong length/chars. HLC: `tick` returns greater than `previous` when the wall clock does not advance and increments `counter`; resets `counter` to 0 when the wall clock jumps forward.
- [ ] **Step 2: Run** tests — expected FAIL.
- [ ] **Step 3: Implement** `Ulid` (Crockford alphabet, deterministic given `clockMillis` + `random`), `HybridLogicalClock.tick`, and `SyncMeta`.
- [ ] **Step 4: Run** tests — expected PASS.
- [ ] **Step 5: Commit** `feat(core): add ulid and hybrid logical clock metadata`.

---

### Task 5: `Campaign` and `Settlement` aggregates

**Files:**
- Create: `.../domain/Campaign.kt`
- Create: `.../domain/Settlement.kt`
- Test: `.../domain/SettlementTest.kt`

**Interfaces:**
- Consumes: `Ulid`, `SyncMeta`, `SettlementPhase`, `RulesetOverrides` (Task 7).
- Produces:
  - `data class Campaign(val id: Ulid, val name: String, val rulesetId: String, val rulesetOverrides: RulesetOverrides, val settlementIds: List<Ulid>, val sync: SyncMeta)`
  - `data class Settlement(val id: Ulid, val campaignId: Ulid, val name: String, val lanternYear: Int, val phase: SettlementPhase, val currentStep: Int, val endeavors: Int, val population: Int, val settlementEvent: String?, val sync: SyncMeta)` with `init` requiring `lanternYear >= 1`, `currentStep in 1..SettlementStepCatalog.TOTAL`, `endeavors >= 0`, `population >= 0`.

- [ ] **Step 1: Write the failing test** — constructing a `Settlement` with `currentStep = 12` throws `IllegalArgumentException`; a valid one keeps its values; defaults come from a companion factory `Settlement.new(id, campaignId, name)` with year 1, SETTLEMENT phase, step 1, 0 endeavors, population 4.
- [ ] **Step 2: Run** test — expected FAIL.
- [ ] **Step 3: Implement** both data classes with the `init` guards and factory.
- [ ] **Step 4: Run** test — expected PASS.
- [ ] **Step 5: Commit** `feat(core): add campaign and settlement aggregates`.

---

### Task 6: Settlement step catalog

**Files:**
- Create: `.../rules/SettlementStep.kt`
- Create: `.../rules/SettlementStepCatalog.kt`
- Test: `.../rules/SettlementStepCatalogTest.kt`

**Interfaces:**
- Consumes: `StepType`.
- Produces:
  - `data class SettlementStep(val number: Int, val title: String, val summary: String, val type: StepType)`
  - `object SettlementStepCatalog { const val TOTAL = 11; val ALL: List<SettlementStep>; fun byNumber(number: Int): SettlementStep }` — content copied verbatim from the existing `io.kdmtracker.domain.SettlementSteps` (11 official steps, same titles/summaries/types).

- [ ] **Step 1: Write the failing test** — `ALL` has 11 entries numbered 1..11 in order; `byNumber(9).type == BRANCHING`; `byNumber(7).title == "Develop"`; `byNumber(12)` throws `NoSuchElementException`.
- [ ] **Step 2: Run** test — expected FAIL.
- [ ] **Step 3: Implement** the catalog with the 11 steps.
- [ ] **Step 4: Run** test — expected PASS.
- [ ] **Step 5: Commit** `feat(core): add settlement step catalog`.

---

### Task 7: Ruleset seam (`Ruleset`, `Core16Ruleset`, `RulesetOverrides`)

**Files:**
- Create: `.../ruleset/Ruleset.kt`
- Create: `.../ruleset/Core16Ruleset.kt`
- Create: `.../ruleset/RulesetOverrides.kt`
- Test: `.../ruleset/RulesetTest.kt`

**Interfaces:**
- Consumes: `SettlementStep`, `PrincipleCategory`, `MilestoneKey`.
- Produces:
  - `data class MilestoneDefinition(val key: MilestoneKey, val condition: String)`
  - `data class PrincipleChoice(val category: PrincipleCategory, val label: String, val effect: String)`
  - `interface Ruleset { val id: String; val version: String; val settlementSteps: List<SettlementStep>; val survivalLimit: Int; val milestones: List<MilestoneDefinition>; val principles: Map<PrincipleCategory, List<PrincipleChoice>> }`
  - `object Core16Ruleset : Ruleset` with `id = "core-1.6"`, the five official milestones from the research artifact, and the six principle choices.
  - `data class RulesetOverrides(val survivalLimit: Int? = null, val extraMilestones: List<MilestoneDefinition> = emptyList(), val principleOverrides: Map<PrincipleCategory, List<PrincipleChoice>> = emptyMap())`
  - `fun Ruleset.applyOverrides(overrides: RulesetOverrides): Ruleset` — returns a copy with non-null overrides applied; base unchanged.

- [ ] **Step 1: Write the failing test** — `Core16Ruleset.survivalLimit` equals the 1.6 value; `applyOverrides(RulesetOverrides(survivalLimit = 12)).survivalLimit == 12` while `Core16Ruleset.survivalLimit` is unchanged; `applyOverrides(RulesetOverrides())` deep-equals `Core16Ruleset`.
- [ ] **Step 2: Run** test — expected FAIL.
- [ ] **Step 3: Implement** the interface, the `Core16Ruleset` data (milestones from `lantern-year-steps.md` §2 step 6; principle choices from steps 5–6), and `applyOverrides`.
- [ ] **Step 4: Run** test — expected PASS.
- [ ] **Step 5: Commit** `feat(core): add ruleset seam with core 1.6 default and overrides`.

---

### Task 8: Phase/step state machine — forward navigation

**Files:**
- Create: `.../rules/PhaseStep.kt`
- Create: `.../rules/FlowTransition.kt`
- Create: `.../rules/LanternYearFlow.kt`
- Test: `.../rules/LanternYearFlowTest.kt`

**Interfaces:**
- Consumes: `SettlementPhase`, `SettlementStepCatalog`, `Ruleset`.
- Produces:
  - `data class PhaseStep(val phase: SettlementPhase, val step: Int)`
  - `sealed interface FlowTransition { data class Advance(val to: PhaseStep) : FlowTransition; data class LoopBack(val to: PhaseStep) : FlowTransition; data class Commit(val to: PhaseStep, val discardedEndeavors: Int) : FlowTransition; data class Blocked(val reason: String) : FlowTransition }`
  - `data class FlowContext(val ruleset: Ruleset, val isNemesisYear: Boolean = false, val returnedSurvivors: Int = 0)`
  - `object LanternYearFlow { fun next(current: PhaseStep, context: FlowContext): FlowTransition; fun canEnterStep(step: Int, from: PhaseStep, context: FlowContext): Boolean }`

- [ ] **Step 1: Write the failing test** — step 1→2, 2→3, …, 8→9, and 10→11 return `Advance`; 11 returns `Commit` to `PhaseStep(SETTLEMENT, 1)` carrying the discarded endeavor count; step 8 is `Blocked` when reached directly from step 6 (Develop gate).
- [ ] **Step 2: Run** test — expected FAIL.
- [ ] **Step 3: Implement** forward navigation reading `TOTAL` from `context.ruleset.settlementSteps.size` (no magic numbers).
- [ ] **Step 4: Run** test — expected PASS.
- [ ] **Step 5: Commit** `feat(core): add forward phase/step state machine`.

---

### Task 9: State machine — branches, loops, and gates

**Files:**
- Modify: `.../rules/LanternYearFlow.kt`
- Test: `.../rules/LanternYearFlowBranchesTest.kt`

**Interfaces:**
- Consumes: `LanternYearFlow.next` from Task 8.
- Produces: same signature; behavior extended for the four Review Focus branches.

- [ ] **Step 1: Write the failing tests** for each branch:
  - step 9 → `LoopBack(PhaseStep(SETTLEMENT, 5))` (Special Showdown loop).
  - `isNemesisYear = true` at step 11 → `Advance(PhaseStep(SHOWDOWN, 1))` (Hunt skipped).
  - phase HUNT → `Advance(PhaseStep(SETTLEMENT, 1))` with `returnedSurvivors = 0` (hunt failure/TPK path) landing at step 2 with base endeavors computed from 0.
  - step 11 with `discardedEndeavors > 0` returns `Commit` carrying that exact count (never 0).
- [ ] **Step 2: Run** tests — expected FAIL.
- [ ] **Step 3: Implement** the branch logic: step 9 loops to 5; nemesis skips Hunt; hunt phase entry from step 11; zero-returnee endeavor base.
- [ ] **Step 4: Run** tests — expected PASS.
- [ ] **Step 5: Commit** `feat(core): add lantern-year branches and gates`.

---

### Task 10: Derived-value recompute

**Files:**
- Create: `.../rules/DerivedValues.kt`
- Test: `.../rules/DerivedValuesTest.kt`

**Interfaces:**
- Consumes: `Settlement`, `FlowContext`.
- Produces:
  - `data class EndeavorBreakdown(val base: Int, val bonuses: List<Pair<String, Int>>) { val total: Int }`
  - `object DerivedValues { fun endeavorsFor(returnedSurvivors: Int, ruleset: Ruleset): EndeavorBreakdown; fun survivalLimit(ruleset: Ruleset, innovationBonus: Int = 0): Int }`
  - `fun recompute(settlement: Settlement, returnedSurvivors: Int, ruleset: Ruleset): Settlement` — returns a copy whose derived scalars are recomputed from primitives (never hand-carried).

- [ ] **Step 1: Write the failing test** — `endeavorsFor(3)` base is 3; `recompute` on a settlement whose `endeavors` was stale recomputes from `returnedSurvivors`; editing an earlier input changes the recomputed output (the Review Focus #1 guarantee).
- [ ] **Step 2: Run** test — expected FAIL.
- [ ] **Step 3: Implement** `EndeavorBreakdown`, `DerivedValues`, and `recompute`.
- [ ] **Step 4: Run** test — expected PASS.
- [ ] **Step 5: Commit** `feat(core): recompute derived values from primitives`.

---

### Task 11: Transfer schema v3 DTOs

**Files:**
- Create: `.../transfer/TransferDocument.kt`
- Create: `.../transfer/TransferMapper.kt`
- Test: `.../transfer/TransferDocumentTest.kt`

**Interfaces:**
- Consumes: `Campaign`, `Settlement`, `RulesetOverrides`, all enums.
- Produces:
  - `@Serializable data class TransferDocument(val schemaVersion: Int = 3, val exportedAt: String, val campaigns: List<CampaignDto>)`; `TransferDocument.SUPPORTED_VERSIONS = listOf(1, 2, 3)`.
  - DTOs (`CampaignDto`, `SettlementDto`, `RulesetOverridesDto`) mirroring the aggregates with `@Serializable` and default values for forward compatibility.
  - `object TransferMapper { fun toDto(campaign: Campaign, settlements: List<Settlement>): CampaignDto; fun toDomain(dto: CampaignDto): Pair<Campaign, List<Settlement>> }`.
  - v1/v2 documents (no v3 fields) deserialize using the DTO defaults.

- [ ] **Step 1: Write the failing test** — round-trip a `Campaign` + `Settlement` through DTO ⇄ domain and assert equality; a v2-shaped JSON without `rulesetOverrides` deserializes with defaults and `schemaVersion` reads as 2.
- [ ] **Step 2: Run** test — expected FAIL.
- [ ] **Step 3: Implement** the DTOs and mapper.
- [ ] **Step 4: Run** test — expected PASS.
- [ ] **Step 5: Commit** `feat(core): add transfer schema v3 dtos and mapper`.

---

### Task 12: `AdvanceStep` use case + server consumes `:core`

**Files:**
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/usecase/AdvanceStep.kt`
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/usecase/AdvanceStepTest.kt`
- Modify: `server/build.gradle.kts` (add `implementation(project(":core"))`)
- Modify: `server/src/main/kotlin/io/kdmtracker/domain/SettlementSteps.kt` (delegate to `SettlementStepCatalog`, keep the server enum/mapping)

**Interfaces:**
- Consumes: `LanternYearFlow`, `FlowContext`, `DerivedValues`, `Settlement`.
- Produces: `object AdvanceStep { data class Result(val settlement: Settlement, val transition: FlowTransition); fun run(settlement: Settlement, returnedSurvivors: Int, context: FlowContext): Result }` — applies the transition to the settlement and recomputes derived values.

- [ ] **Step 1: Write the failing test** — running from step 7 with 3 returnees yields step 8 and an endeavor total of 3; running from step 11 returns the discarded count and phase SETTLEMENT step 1.
- [ ] **Step 2: Run** test — expected FAIL.
- [ ] **Step 3: Implement** `AdvanceStep`; then wire `:server` to `:core` and make `SettlementSteps.byNumber` delegate to `SettlementStepCatalog`.
- [ ] **Step 3b: Run the full suite** `./gradlew :core:jvmTest :server:test :server:spotlessCheck` — expected PASS (server behavior unchanged).
- [ ] **Step 4: Commit** `feat(core): add advance-step use case and consume core from server`.

---

## Self-Review

**1. Spec coverage**

| Spec section | Covered by |
| --- | --- |
| §4 module architecture (`:core` pure, `server` depends on core) | Tasks 1, 2, 12 |
| §5 domain model (aggregates, enums, ULID/HLC metadata) | Tasks 3, 4, 5 |
| §5 full `Survivor`/`Hunt`/`Showdown`/`Aftermath`/`Timeline` | **Deferred** to the Settlement / Hunt-Showdown plans (out of scope here) |
| §6 sync model (ChangeSet, conflict policy) | **Deferred** to the Offline+Sync plan; only `SyncMeta` foundation here (Task 4) |
| §7 phase/step state machine (loops, branches, gates, recompute) | Tasks 8, 9, 10 |
| D6 rules engine vs. ruleset data (1.6 default + overrides) | Tasks 6, 7 |
| §9 migration (v2 export → new model) | **Deferred** to the Offline+Sync plan |
| D10 SemVer baseline `v0.1.0` | Global Constraints + Task 1 |
| `rulesetId` + overrides carried by a campaign | Tasks 5, 11 |

**2. Step scan:** every step names a single action with a checkable result; code bodies are left to the implementer except where the spec fixes values (catalog content, milestone/principle data).

**3. Type consistency:** `SettlementPhase`, `StepType`, `SettlementStepCatalog.TOTAL`, `SyncMeta`, `RulesetOverrides`, and `PhaseStep` are defined before first use and referenced with identical names in later tasks.

**4. Review Focus:** all five lines have an owning task and a test (Tasks 9 and 10).

**5. Proportion:** the plan is task-level, not a code transcript; bodies appear only where the spec fixes content (Tasks 6 and 7).

**Known scope boundary:** full `Survivor`/`Resource`/`Hunt`/`Showdown`/`Aftermath` entities and the sync protocol are intentionally **not** in this plan — they belong to the Settlement and Offline+Sync plans, and their absence does not block a green `:core` build.
