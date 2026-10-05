# Walking Skeleton (`:app-android`) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship a runnable Compose Multiplatform Android app (`:app-android`) that renders the full lantern-year loop (Settlement steps 1–11 → Hunt → Showdown → back to Settlement) driven by `:core`, with in-memory sample state and "Pendiente" stubs for the steps that later plans implement — producing a debug APK.

**Architecture:** `:core` gains an `androidTarget()` so the Android app can consume it. `:app-android` is an Android application module using Compose Multiplatform + Material 3, with a plain (non-Android) `LanternYearController` holding `StateFlow<UiState>` so the presentation logic is JVM-unit-testable. Navigation is a sealed `Screen` state in the controller, not a navigation library (YAGNI). Screens are stateless composables receiving `UiState` + lambdas. No persistence, no sync — that is the next plan (v0.3.0).

**Tech Stack:** Kotlin 2.3.21, Gradle 9.7.1 (Kotlin DSL), Android Gradle Plugin 9.4.1, Compose Multiplatform 1.12.1, compileSdk 36, build-tools 36.0.0, minSdk 24, JDK toolchain 21. Android SDK at `/opt/homebrew/share/android-commandlinetools` (via `local.properties`).

**Spec:** `docs/superpowers/specs/2026-10-05-kdm-tracker-offline-first-kmp-design.md`

## Global Constraints

- Kotlin `2.3.21`; Gradle wrapper `9.7.1`; JDK toolchain `21`.
- Android: AGP `9.4.1`, Compose Multiplatform `1.12.1`, `compileSdk = 36`, `buildToolsVersion = 36.0.0`, `minSdk = 24`, `targetSdk = 36`. Versions live in `gradle/libs.versions.toml` — no hardcoded versions in module files.
- `local.properties` (git-ignored) must contain `sdk.dir=/opt/homebrew/share/android-commandlinetools`; CI-less local builds rely on it.
- `:core` stays pure: no Spring/JPA. It may gain the Android target and the Android library plugin; no Android APIs in `commonMain`.
- UI is basic Material 3 — **no new prototypes, no design investment**. Screens stay stateless so a redesign does not touch logic.
- Business rules live in `:core`; the app never duplicates them.
- Conventional Commits; **no** `Co-Authored-By`/AI attribution.
- Spotless (ktlint via `.editorconfig`) must pass; `./gradlew :core:jvmTest :server:test spotlessCheck` stays green throughout.

## Review Focus

1. **The Android toolchain actually works**: `:app-android:assembleDebug` produces an APK, and `:core:jvmTest`/`:server:test` stay green after `:core` gains `androidTarget()`. A version mismatch is the most likely failure.
2. **Navigation follows the state machine**: the loop Settlement → Hunt → Showdown → Settlement is driven by `:core` `LanternYearFlow`/`AdvanceStep`, not by ad-hoc UI branching; a nemesis year skips Hunt.
3. **No business logic in the UI**: the controller/composables contain no rules; all transitions come from `:core`.
4. **The app survives process death / rotation minimally**: sample settlement state lives in the controller (held with `remember`), not reconstructed inconsistently per recomposition.
5. **Unimplemented steps are honest**: a step that later plans implement shows a clear "Pendiente" state, never a broken or empty screen.

---

## File Structure

```text
kdm-tracker/
├── gradle/libs.versions.toml                 # + AGP, CMP, compose, androidx-activity, android sdk levels
├── settings.gradle.kts                       # + include(":app-android")
├── core/
│   ├── build.gradle.kts                      # + com.android.library, androidTarget(), android {} block
│   └── src/androidMain/...                   # (only if needed; keep code in commonMain)
└── app-android/
    ├── build.gradle.kts                      # AGP application + kotlin.android + compose plugins
    ├── src/main/AndroidManifest.xml
    ├── src/main/kotlin/io/kdmtracker/app/
    │   ├── MainActivity.kt                   # ComponentActivity -> setContent { KdmTrackerApp() }
    │   ├── KdmTrackerApp.kt                  # root composable, holds the controller
    │   ├── LanternYearController.kt          # StateFlow<UiState>, calls :core AdvanceStep
    │   ├── UiState.kt                        # UiState + sealed Screen
    │   ├── SettlementRepository.kt           # in-memory sample Settlement/Campaign
    │   └── ui/
    │       ├── SettlementScreen.kt           # step rail 1..11 + step detail stub
    │       ├── HuntScreen.kt                 # stub + advance action
    │       └── ShowdownScreen.kt             # stub + advance action
    └── src/test/kotlin/io/kdmtracker/app/
        └── LanternYearControllerTest.kt      # JVM unit tests of the transitions
```

---

### Task 1: Android target for `:core` (build spike)

**Files:**
- Modify: `gradle/libs.versions.toml` (add AGP, `android-compileSdk`, `android-minSdk`, `android-targetSdk`)
- Modify: `settings.gradle.kts` (add `pluginManagement`/`dependencyResolutionManagement` google() repo if missing)
- Modify: `core/build.gradle.kts` (apply `com.android.library`, add `androidTarget()`, `android { }` block)
- Create: `local.properties` (git-ignored) with `sdk.dir=/opt/homebrew/share/android-commandlinetools`
- Modify: `.gitignore` if `local.properties` is not already ignored

**Interfaces:**
- Consumes: the existing KMP `:core`.
- Produces: `:core` compiling for `jvm()` **and** `androidTarget()`, still green on `:core:jvmTest`.

- [ ] **Step 1:** Add catalog entries: `agp = "9.4.1"`, `android-compileSdk = "36"`, `android-minSdk = "24"`, `android-targetSdk = "36"`; and the plugin alias `android-library = { id = "com.android.library", version.ref = "agp" }`.
- [ ] **Step 2:** Ensure `settings.gradle.kts` can resolve AGP from `google()` (add `pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }` and `dependencyResolutionManagement { repositories { google(); mavenCentral() } }` if absent). Remove per-module `repositories { mavenCentral() }` only if that is safe — otherwise leave them.
- [ ] **Step 3:** In `core/build.gradle.kts`, apply `alias(libs.plugins.android.library)`, add `androidTarget { compilerOptions { jvmTarget.set(JvmTarget.JVM_21) } }`, and an `android { namespace = "io.kdmtracker.core"; compileSdk = libs.versions.android.compileSdk.get().toInt(); defaultConfig { minSdk = ... } }` block. Use a namespace that does not collide with the app.
- [ ] **Step 4:** Verify `./gradlew :core:jvmTest` still passes and `./gradlew :core:assemble` (or `:core:assembleDebug`) succeeds. If AGP/Gradle/JDK versions are incompatible, report `BLOCKED` with the exact Gradle error — do **not** silently downgrade versions.
- [ ] **Step 5:** Commit `build(core): add android target`.

---

### Task 2: `:app-android` module + minimal APK

**Files:**
- Modify: `settings.gradle.kts` (`include(":app-android")`)
- Create: `app-android/build.gradle.kts`
- Create: `app-android/src/main/AndroidManifest.xml`
- Create: `app-android/src/main/kotlin/io/kdmtracker/app/MainActivity.kt`
- Create: `app-android/src/main/kotlin/io/kdmtracker/app/KdmTrackerApp.kt`

**Interfaces:**
- Consumes: `:core`.
- Produces: a launchable Android app whose root composable renders a Material 3 `Text("KDM Tracker")`; `:app-android:assembleDebug` produces an APK.

- [ ] **Step 1:** Catalog: add `androidx-activity-compose`, `android-application` plugin alias, and the CMP/compose plugin aliases (`org.jetbrains.compose`, `org.jetbrains.kotlin.plugin.compose`, `org.jetbrains.kotlin.android`).
- [ ] **Step 2:** `app-android/build.gradle.kts`: apply `com.android.application`, `org.jetbrains.kotlin.android`, `org.jetbrains.compose`, `org.jetbrains.kotlin.plugin.compose`; set `namespace`, `compileSdk`/`defaultConfig`/`buildTypes`; dependencies `implementation(project(":core"))`, `compose.runtime`, `compose.foundation`, `compose.material3`, `compose.ui`, `androidx.activity:activity-compose`.
- [ ] **Step 3:** `AndroidManifest.xml` with a `MainActivity` declared and a launcher intent filter; no special permissions.
- [ ] **Step 4:** `MainActivity` calls `setContent { KdmTrackerApp() }`; `KdmTrackerApp` renders the Material 3 text.
- [ ] **Step 5:** Verify `./gradlew :app-android:assembleDebug` produces `app-android/build/outputs/apk/debug/app-debug.apk`. Commit `feat(app): add android application shell`.
- [ ] **Step 6 (Review Focus 1):** Run `./gradlew :core:jvmTest :server:test spotlessCheck` and confirm still green.

---

### Task 3: In-memory settlement holder

**Files:**
- Create: `app-android/src/main/kotlin/io/kdmtracker/app/SettlementRepository.kt`
- Test: `app-android/src/test/kotlin/io/kdmtracker/app/SettlementRepositoryTest.kt`

**Interfaces:**
- Consumes: `io.kdmtracker.core.domain.{Campaign, Settlement, Ulid, SyncMeta, HybridLogicalClock}`, `io.kdmtracker.core.ruleset.Core16Ruleset`.
- Produces:
  - `class SettlementRepository { val campaign: Campaign; var settlement: Settlement; fun update(next: Settlement) }`
  - A sample: one campaign (`rulesetId = Core16Ruleset.id`) with one settlement (`Settlement.new(Ulid.generate(0, { 0 }), campaignId, "New Settlement")`).

- [ ] **Step 1:** Write a failing test: the repository exposes a settlement with `lanternYear == 1`, phase `SETTLEMENT`, `currentStep == 1`, `population == 4`; `update` replaces it.
- [ ] **Step 2:** Run `./gradlew :app-android:testDebugUnitTest` — expected FAIL.
- [ ] **Step 3:** Implement `SettlementRepository`.
- [ ] **Step 4:** Run the test — expected PASS.
- [ ] **Step 5:** Commit `feat(app): add in-memory settlement holder`.

---

### Task 4: `UiState` + `LanternYearController`

**Files:**
- Create: `app-android/src/main/kotlin/io/kdmtracker/app/UiState.kt`
- Create: `app-android/src/main/kotlin/io/kdmtracker/app/LanternYearController.kt`
- Test: `app-android/src/test/kotlin/io/kdmtracker/app/LanternYearControllerTest.kt`

**Interfaces:**
- Consumes: `SettlementRepository`, `io.kdmtracker.core.usecase.AdvanceStep`, `io.kdmtracker.core.rules.{LanternYearFlow, FlowContext, FlowTransition, PhaseStep}`, `io.kdmtracker.core.rules.SettlementStepCatalog`, `io.kdmtracker.core.domain.SettlementPhase`.
- Produces:
  - `sealed interface Screen { data object Settlement : Screen; data object Hunt : Screen; data object Showdown : Screen }`
  - `data class UiState(val screen: Screen, val lanternYear: Int, val phase: SettlementPhase, val currentStep: Int, val stepTitle: String, val population: Int, val endeavors: Int, val steps: List<SettlementStep>, val lastTransition: String?)`
  - `class LanternYearController(repository: SettlementRepository, ruleset: Ruleset = Core16Ruleset) { val state: StateFlow<UiState>; fun advance(); fun previous(); fun selectStep(number: Int) }`
  - `advance()` calls `AdvanceStep.run(settlement, returnedSurvivors, FlowContext(ruleset, ...))` and maps the resulting phase to a `Screen` (SETTLEMENT→Settlement, HUNT→Hunt, SHOWDOWN→Showdown). `previous()` moves the step back on the settlement screen without crossing phases.

- [ ] **Step 1:** Write failing tests: initial state is Screen.Settlement, step 1, year 1, population 4; `advance()` from step 1 → step 7 yields step 8 and the step title from the catalog; the state's `steps` has 11 entries in order; a transition to HUNT yields `Screen.Hunt`.
- [ ] **Step 2:** Run `./gradlew :app-android:testDebugUnitTest` — expected FAIL.
- [ ] **Step 3:** Implement `UiState` and `LanternYearController` (business transitions delegated to `:core`).
- [ ] **Step 4:** Run the test — expected PASS. Commit `feat(app): add lantern-year controller and ui state`.

---

### Task 5: Settlement / Hunt / Showdown screens

**Files:**
- Create: `app-android/src/main/kotlin/io/kdmtracker/app/ui/SettlementScreen.kt`
- Create: `app-android/src/main/kotlin/io/kdmtracker/app/ui/HuntScreen.kt`
- Create: `app-android/src/main/kotlin/io/kdmtracker/app/ui/ShowdownScreen.kt`
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/KdmTrackerApp.kt` (render by `state.screen`)

**Interfaces:**
- Consumes: `UiState`, `SettlementStep`.
- Produces: stateless composables — `SettlementScreen(state, onAdvance, onPrevious, onSelectStep)`, `HuntScreen(state, onAdvance)`, `ShowdownScreen(state, onAdvance)`. `SettlementScreen` shows the lantern year, population, endeavors, and a vertical rail of the 11 steps with the current step highlighted; the current step's body is a "Pendiente" stub (the Set Up / Develop screens arrive in later plans). `HuntScreen`/`ShowdownScreen` are stubs with a prominent "Pendiente" label and an advance button.

- [ ] **Step 1:** Implement the three stateless composables (Material 3 only).
- [ ] **Step 2:** `KdmTrackerApp` holds the controller (`remember { LanternYearController(...) }`), collects `state` via `collectAsState`, and renders `when (state.screen)`.
- [ ] **Step 3:** Verify `./gradlew :app-android:assembleDebug` succeeds.
- [ ] **Step 4 (Review Focus 5):** Confirm every unimplemented step renders a clear "Pendiente" state, not an empty screen.
- [ ] **Step 5:** Commit `feat(app): add settlement hunt and showdown skeleton screens`.

---

### Task 6: Full-loop navigation driven by `:core`

**Files:**
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/LanternYearController.kt`
- Test: `app-android/src/test/kotlin/io/kdmtracker/app/LanternYearLoopTest.kt`

**Interfaces:**
- Consumes: `AdvanceStep`, `LanternYearFlow`, `Core16Ruleset`.
- Produces: the controller exposing `advance()` that, from Settlement step 11, moves to Hunt; from Hunt (success) to Showdown; from Showdown back to Settlement step 1; and a way to mark a hunt failed so the loop restarts the settlement with 0 returning survivors.

- [ ] **Step 1:** Write failing tests: advancing from Settlement step 11 lands on `Screen.Hunt`; `advance()` on the Hunt screen (success) lands on `Screen.Showdown`; advancing on Showdown lands on `Screen.Settlement` step 1 with a new lantern year; `failHunt()` from Hunt lands on `Screen.Settlement` step 1.
- [ ] **Step 2:** Run — expected FAIL.
- [ ] **Step 3:** Implement the loop in the controller, delegating every transition to `:core` `LanternYearFlow`/`AdvanceStep` (no ad-hoc branching).
- [ ] **Step 4 (Review Focus 2):** Confirm the loop is driven by `:core` and a nemesis year skips Hunt.
- [ ] **Step 5:** Run the tests — expected PASS. Commit `feat(app): drive the lantern-year loop from core`.

---

### Task 7: Verification sweep + README

**Files:**
- Modify: `README.md` (document `:app-android`, the Android prerequisites, and the APK build command)
- Test: whichever integration test pins the end-to-end loop

**Interfaces:**
- Consumes: everything above.
- Produces: a documented, reproducible build.

- [ ] **Step 1:** Run `./gradlew :core:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck` — expected PASS.
- [ ] **Step 2:** Run `./gradlew :app-android:assembleDebug` and confirm the APK exists.
- [ ] **Step 3:** Update `README.md`: modules (`core`, `server`, `app-android`), the Android SDK prerequisite + `local.properties`, and how to build/install the debug APK.
- [ ] **Step 4:** Commit `docs: document android app build`.

---

## Self-Review

**1. Spec coverage**

| Spec item | Covered by |
| --- | --- |
| §4 module `app-android` (CMP) | Tasks 2, 5 |
| D8 Compose Multiplatform + Material 3, MVVM, stateless composables | Tasks 4, 5 |
| D9 walking skeleton (navigable loop with stubs) | Tasks 3–6 |
| §7 phase/step loop, gates, branches | Task 6 (via `:core`) |
| Anti-stopper: rules in `:core`, UI replaceable | Tasks 4, 5 (controller is plain Kotlin; screens stateless) |
| Offline persistence (SQLDelight) | **Deferred** — v0.3.0 (next plan) |
| Sync client / auth | **Deferred** — v0.3.0 |

**2. Step scan:** each step names one action with a checkable result; bodies are left to the implementer except the state/interface shapes that must be consistent across tasks.

**3. Type consistency:** `UiState`, `Screen`, `LanternYearController`, and the screen composable signatures are defined before first use and referenced identically in later tasks.

**4. Review Focus:** each of the five lines is assigned to a task with a check (Tasks 1, 6, 4/5, 4, 5).

**5. Proportion:** the plan is task-level; no code transcripts.

**Known scope boundary:** persistence, sync, and the real Settlement steps 1–11 / Hunt / Showdown / Aftermath models are intentionally out of scope.
