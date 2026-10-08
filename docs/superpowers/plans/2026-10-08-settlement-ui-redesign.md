# Settlement UI Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the Settlement cockpit's half-screen step rail with a thin top step bar, and rebuild the Develop step as three grouped/colour-coded tabs (Recursos / Innovar / Fabricar).

**Architecture:** `:core` gains pure derived view helpers (resource groups, innovation access state, craftability) + a resource `source`; the app recomposes the cockpit chrome and tabs, all rendering state from `UiState`. Rules stay in `:core`; the screens decide nothing.

**Tech Stack:** Kotlin 2.3.21 (KMP), Compose Multiplatform, `kotlin.test`.

**Spec:** `docs/superpowers/specs/2026-10-08-settlement-ui-redesign-design.md`

## Global Constraints

- Kotlin `2.3.21`; `:core` stays pure. No new dependencies.
- Strict TDD: failing test first, observe RED, implement, observe GREEN.
- Derived views are pure and deterministic; they must not duplicate `Craft`/`Innovate`/`AddResourceEntry` rules — they call them (or mirror their predicates exactly) and only add grouping/presentation.
- Additive/back-compatible: new fields default; existing tests stay green.
- Conventional Commits; no `Co-Authored-By`/AI attribution. Technical artifacts in English.
- Green: `./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck`.

## Review Focus

1. **Innovation access is read the right way** — `Disponibles` = a consequence of something unlocked (in the deck); `Sin acceso` = otherwise and names the missing parent. ← Task 2.
2. **Craftability mirrors `Craft` exactly** — a recipe is green iff `Craft.run` returns `Ok`; no drift between the badge and the action. ← Task 3.
3. **Resource grouping is faithful** — every resource lands in its source group; unmapped names fall back to `Otros` (never dropped). ← Task 1.
4. **Full-width content** — the step bar is thin and the body spans the width; no step is unreachable. ← Task 5.
5. **Back-compat** — defaulted fields; the existing Home/loop behaviour is unchanged. ← Task 4.

---

## File Structure

```text
core/.../domain/Storage.kt                              # ResourceDefinition + source
core/.../content/ResourceSources.kt                     # NEW: curated name -> source map
core/.../content/ContentCatalog.kt                      # + resourceGroups()
core/.../rules/InnovationAccess.kt                      # NEW
core/.../rules/Craftability.kt                          # NEW
app-android/.../ui/SettlementScreen.kt                  # top step bar (replace the rail)
app-android/.../ui/DevelopStepScreen.kt                 # tabs
app-android/.../ui/{RecursosTab,InnovarTab,FabricarTab}.kt   # NEW
app-android/.../UiState.kt · LanternYearController.kt   # derived views
```

---

### Task 1: Resource `source` + `resourceGroups`

**Files:**
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/domain/Storage.kt`
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/content/ResourceSources.kt`
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/content/ContentCatalog.kt`
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/content/Legacy15Content.kt`
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/content/ResourceGroupsTest.kt`

**Interfaces:**
- Produces:
  - `ResourceDefinition.source: String = ""` (additive, defaulted).
  - `object ResourceSources { fun sourceOf(name: String): String }` — a curated `name -> source` map (Básicos, León Blanco, Fénix, Antílope Bramador, …), falling back to `"Otros"`.
  - `ContentCatalog.resourceGroups(): Map<String, List<ResourceDefinition>>` — resources grouped by `source`, sorted by source then name, deterministic.

- [ ] **Step 1: Write the failing test** — `ResourceSources.sourceOf("Garra de León") == "León Blanco"`; an unknown name → `"Otros"`; `resourceGroups()` over a catalog containing a mapped and an unmapped resource returns two groups and keeps every resource (no drops); a legacy `ResourceDefinition` JSON without `source` decodes with `""`.
- [ ] **Step 2: Run** `./gradlew :core:jvmTest` — expected FAIL.
- [ ] **Step 3: Implement** the field, the curated map (from `external-assets/` decks/names), `sourceOf`, `resourceGroups`, and set `source` on the `Legacy15Content` resources.
- [ ] **Step 4: Run** `./gradlew :core:jvmTest spotlessCheck` — expected GREEN.
- [ ] **Step 5: Commit** `feat(core): group resources by source`.

---

### Task 2: `InnovationAccess`

**Files:**
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/rules/InnovationAccess.kt`
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/rules/InnovationAccessTest.kt`

**Interfaces:**
- Consumes: `SettlementState.innovations`, `InnovationDeck.of`, `ContentCatalog.innovationDefinitions`.
- Produces:
  - `enum class InnovationStatus { UNLOCKED, AVAILABLE, NO_ACCESS }`
  - `data class InnovationView(val innovation: Innovation, val status: InnovationStatus, val missingParent: String?)`
  - `object InnovationAccess { fun view(state: SettlementState, catalog: ContentCatalog): List<InnovationView> }` — `UNLOCKED` if in `state.innovations`; else `AVAILABLE` if in the deck (`InnovationDeck.of(state.innovations).cards`); else `NO_ACCESS` with the catalog innovation whose `consequences` contains its name (`missingParent`).

- [ ] **Step 1: Write the failing tests** — with only `Lenguaje` unlocked: `Lenguaje` is `UNLOCKED`; `Choza`/`Pintura` (its consequences) are `AVAILABLE`; `Cama` (a consequence of `Choza`, not yet unlocked) is `NO_ACCESS` with `missingParent == "Choza"`. Validate the shape against `external-assets/arbol_de_innovaciones_kdm_1_5.md`.
- [ ] **Step 2: Run** `./gradlew :core:jvmTest` — expected FAIL.
- [ ] **Step 3: Implement** the classification (deterministic; sorted by name).
- [ ] **Step 4: Run** `./gradlew :core:jvmTest spotlessCheck` — expected GREEN.
- [ ] **Step 5: Commit** `feat(core): classify innovation access state`.

---

### Task 3: `Craftability`

**Files:**
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/rules/Craftability.kt`
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/rules/CraftabilityTest.kt`

**Interfaces:**
- Consumes: `Craft`, `SettlementRecord`, `SettlementLocation`, `GearRecipe`, `ContentCatalog`, `Storage`.
- Produces:
  - `data class CraftCheck(val craftable: Boolean, val missing: String?)`
  - `object Craftability { fun check(record: SettlementRecord, locationName: String, recipe: GearRecipe, catalog: ContentCatalog): CraftCheck }` — `craftable` is exactly "`Craft.run(...)` returns `Ok`" (delegate to it, or mirror its predicates); when it would be `Blocked`, `missing` is the human reason (e.g. `"faltan 2 Piel de Monstruo"`).

- [ ] **Step 1: Write the failing tests** — affordable → `craftable == true, missing == null`; unbuilt location → `false` + reason; insufficient storage → `false` + `"faltan N <resource>"`; missing innovation prerequisite → `false` + the prerequisite. A test asserts `craftable` matches `Craft.run(...) is Ok` across the cases (no drift).
- [ ] **Step 2: Run** `./gradlew :core:jvmTest` — expected FAIL.
- [ ] **Step 3: Implement** — call `Craft` (wrap the result), derive `missing` from the `Blocked` reason.
- [ ] **Step 4: Run** `./gradlew :core:jvmTest spotlessCheck` — expected GREEN.
- [ ] **Step 5: Commit** `feat(core): derive craftability for a recipe`.

---

### Task 4: `UiState` derived views

**Files:**
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/UiState.kt`
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/LanternYearController.kt`
- Test: `app-android/src/test/kotlin/io/kdmtracker/app/DevelopViewsTest.kt`

**Interfaces:**
- Consumes: `resourceGroups`, `InnovationAccess`, `Craftability` (Tasks 1–3), `ContentCatalog.locationDefinitions`.
- Produces on `UiState` (additive, defaulted): `resourceGroups: Map<String, List<ResourceDefinition>>`, `innovations: List<InnovationView>`, and per-built-location `recipes: List<Pair<SettlementLocation, List<Pair<GearRecipe, CraftCheck>>>>` (or an equivalent typed view); the controller populates them in `uiState(...)`.

- [ ] **Step 1: Write the failing test** — over a seeded repository: `state.resourceGroups` groups the resources (Básicos present); with only the root unlocked, `state.innovations` marks it `UNLOCKED` and its consequences `AVAILABLE`; a built location's recipes carry a `CraftCheck` whose `craftable` matches storage.
- [ ] **Step 2: Run** `./gradlew :app-android:testDebugUnitTest` — expected FAIL.
- [ ] **Step 3: Implement** the `UiState` fields + the controller population.
- [ ] **Step 4: Run** `./gradlew :app-android:testDebugUnitTest :app-android:assembleDebug spotlessCheck` — expected GREEN.
- [ ] **Step 5: Commit** `feat(app): expose grouped develop views`.

---

### Task 5: Top step bar (replace the rail)

**Files:**
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/ui/SettlementScreen.kt`
- Test: `app-android/src/test/kotlin/io/kdmtracker/app/StepBarControllerTest.kt`

**Interfaces:**
- Consumes: `UiState.steps`/`currentStep`/`reachableSteps`.
- Produces: a thin top step bar (11 numbered chips, current highlighted, disabled when unreachable); the body spans full width; Prev/Next stay. `SettlementScreen` keeps its signature and the `Pendiente` body for non-implemented steps.

- [ ] **Step 1: Write the failing test** — selecting a reachable step still navigates; a forward-skip stays blocked; the loop tests are unchanged (controller-level).
- [ ] **Step 2: Run** `./gradlew :app-android:testDebugUnitTest` — expected FAIL.
- [ ] **Step 3: Implement** the top bar, remove the left rail, let the body fill the width.
- [ ] **Step 4: Run** `./gradlew :app-android:testDebugUnitTest :app-android:assembleDebug spotlessCheck` — expected GREEN.
- [ ] **Step 5: Commit** `feat(app): compact top step bar`.

---

### Task 6: Develop tabs (Recursos / Innovar / Fabricar)

**Files:**
- Create: `app-android/src/main/kotlin/io/kdmtracker/app/ui/{RecursosTab,InnovarTab,FabricarTab}.kt`
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/ui/DevelopStepScreen.kt`
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/ui/SettlementScreen.kt`
- Test: `app-android/src/test/kotlin/io/kdmtracker/app/DevelopTabsTest.kt`

**Interfaces:**
- Consumes: the `UiState` views (Task 4) + the existing callbacks (`onAddResource`, `onInnovate`, `onBuild`, `onSpendEndeavor`, `onCraft`).
- Produces: `DevelopStepScreen` with a 3-tab switcher. **Recursos**: groups by source + filter chips; each row `+1`. **Innovar**: filter chips Todas/Disponibles/Desbloqueadas/Sin acceso; green/blue/grey rows; `Innovar` on AVAILABLE. **Fabricar**: a Por localización / Por tipo toggle; green `Crear` / grey with the missing reason. `Build`/`Endeavor`/`Record` stay reachable (fold into the tabs or a small header).

- [ ] **Step 1: Write the failing test** — the controller/state exposes what each tab renders (source groups, access states, craftability) and the actions still persist (craft/innovate/addResource through the controller).
- [ ] **Step 2: Run** `./gradlew :app-android:testDebugUnitTest` — expected FAIL.
- [ ] **Step 3: Implement** the three tab composables + the switcher; keep everything stateless.
- [ ] **Step 4: Run** `./gradlew :app-android:testDebugUnitTest :app-android:assembleDebug spotlessCheck` — expected GREEN.
- [ ] **Step 5: Commit** `feat(app): develop tabs for resources, innovation and crafting`.

---

### Task 7: Verification sweep + README

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Run** `./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck --rerun-tasks`; record per-module counts.
- [ ] **Step 2:** Document the redesigned cockpit (top step bar + the three Develop tabs, the grouping/colours, the resource `source`).
- [ ] **Step 3: Commit** `docs: document the settlement UI redesign`.

---

## Self-Review

**1. Spec coverage:** resource groups + source (T1), innovation access (T2), craftability (T3), UiState views (T4), top bar (T5), tabs (T6), docs (T7). Deferred (other steps, Home, rules, survivor/modifiers/CB) not tasked — correct.

**2. Step scan:** each step is one action with a checkable result; tests carry exact values; no code transcripts.

**3. Type consistency:** `InnovationView`/`InnovationStatus` (T2) → UiState (T4) → InnovarTab (T6); `CraftCheck` (T3) → UiState (T4) → FabricarTab (T6); `resourceGroups` (T1) → UiState (T4) → RecursosTab (T6).

**4. Review Focus:** each of the five lines maps to the named task.

**5. Proportion:** task-level; strict-TDD steps; no transcripts. Delivery will exceed ~400 lines → plan for **chained PRs** (`work-unit-commits` + `chained-pr`) at execution time.
