# Fabricar Flow (Part A, Phase 1) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the step-7 crafting flow as the Stitch design's single "Localizaciones de Asentamiento" section — built-location cards with their inline crafting catalog, action and build upgrade, plus "Por erigir" cards — and retire the `Fabricar` tab and its duplicate build list.

**Architecture:** A pure `FabricarModel` derived from `UiState` (slice-6 model pattern) feeds four small composables in a new `LocationsSection`. All taps route through the callbacks that already exist (`onBuild`/`onCraft`/`onAction`/`onSpendEndeavor`) and S2's controller-side name→id resolution; `:core` does not change. The section absorbs `LocationsPanel` and `FabricarTab`; the Develop body keeps the Recursos and Innovar tabs until Phases 3 and 2.

**Tech Stack:** Kotlin Multiplatform `:core`, Compose Android `:app-android`, kotlinx.serialization, kotlin.test (JVM unit tests), Gradle/spotless.

**Spec:** `docs/superpowers/specs/2026-10-11-fabricar-flow-design.md`

**Precondition:** PR #33 (`feat/settlement-screen`) merged; execute in a fresh worktree off `main` (superpowers:using-git-worktrees). This plan was written before that merge, so execution starts in a later session.

## Global Constraints

- One language per render: labels use `names.es` / `LocalizedText.es` today; never render two languages or parenthetical English (spec decision 3).
- Copy is Spanish and matches the design's words (`CONSTRUIDO`, `NO CONSTRUIDO`, `POR ERIGIR`, `FABRICAR`, `ERIGIR`, `CONSTRUIR`, `REQUIERE <nombre>`); identifiers, comments and commits are English.
- No new dependencies; style uses the existing Atramentum tokens (`KdmType`, `KdmSymbols`, theme colours).
- TDD: every model behaviour is a failing test first; the final suite per task is `./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck` after `./gradlew spotlessApply`.
- Commits: conventional, no `Co-Authored-By`/AI attribution. **Visual gate (hard rule):** Tasks 2–3 stay uncommitted until the maintainer approves the measured render (Task 4); nothing is pushed or PR'd before that, and the PR carries `— size:exception` once approved (budget 400 lines).

## Review Focus

Inputs no ordinary test below exercises — each is pinned by a test in Task 1, the task that owns the model:

1. **A recipe whose cost the ledger cannot cover** (storage quantity < a cost count) → row disabled, status `OUT_OF_STOCK`, **no** `REQUIERE` badge — `FabricarModelTest.aRecipeTheLedgerCannotCoverIsOutOfStockWithoutABadge`.
2. **A recipe requiring an innovation the settlement has not unlocked** → disabled, badge `REQUIERE <nombre ES>` (the innovation's `names.es`, never the id, never English) — `FabricarModelTest.aRecipeMissingAnInnovationShowsTheRequiresBadgeInSpanish`.
3. **A buildable location whose `requires` are only partially built** → must NOT appear as an `ERIGIR` upgrade (a click would `Blocked`); it stays a `POR ERIGIR` card — `FabricarModelTest.aPartiallyReadyLocationStaysInBuildableAndNeverBecomesAnUpgrade`.
4. **A built location with no action** → `actionLabel == null` and the card renders no action button (never a bogus `CONSTRUIR`) — `FabricarModelTest.aBuiltLocationWithoutAnActionHasNoActionButton`.
5. **Counts and de-duplication over the real merged catalog** → `builtCount + pendingCount == 19`, every buildable location appears exactly once (upgrade XOR card) — `FabricarModelTest.theMergedCatalogPartitionsIntoBuiltAndPendingWithEachLocationExactlyOnce`.

---

### Task 1: FabricarModel (pure)

**Files:**
- Create: `app-android/src/main/kotlin/io/kdmtracker/app/ui/settlement/FabricarModel.kt`
- Test: `app-android/src/test/kotlin/io/kdmtracker/app/ui/settlement/FabricarModelTest.kt`

**Interfaces:**
- Consumes: `UiState` fields `builtLocations: List<String>`, `catalogLocations: List<SettlementLocation>`, `craftRecipes: List<CraftRecipeView>`, `storageLines: List<StorageLine>`, `resourceDefinitions: List<ResourceDefinition>`, `record: SettlementRecord?` (for unlocked innovation ids: `record.state.innovations.innovations.map { it.id }`); `CraftRecipeView(locationName, recipe, check, type, gearName)`; `GearRecipe(gearId, resourceCost: Map<ResourceId, Int>, requires: List<InnovationId>, requiresText: String?)`.
- Produces (consumed by Task 2):
  - `FabricarModel.of(state: UiState): FabricarModel`
  - `data class FabricarModel(builtCount: Int, pendingCount: Int, built: List<BuiltLocationCard>, buildable: List<BuildableLocationCard>)`
  - `data class BuiltLocationCard(name: String, actionLabel: String?, catalogSize: Int, recipes: List<RecipeRow>, upgrades: List<UpgradeRow>)`
  - `data class RecipeRow(gearName: String, costChips: List<String>, status: RecipeStatus, missingInnovation: String?, requiresText: String?)`
  - `enum class RecipeStatus { CRAFTABLE, OUT_OF_STOCK, MISSING_INNOVATION }`
  - `data class UpgradeRow(locationName: String, costChips: List<String>, endeavors: Int)`
  - `data class BuildableLocationCard(name: String, requires: List<String>, endeavors: Int, costChips: List<String>)`

- [ ] **Step 1: Write the failing tests**

Create `FabricarModelTest` with a small `UiState` fixture builder (locations via `SettlementLocation(LocationId, LocalizedText, …)`, one craftable/one out-of-stock/one innovation-gated recipe, a `StorageLine(ResourceId, qty)` set, and a record whose `state.innovations` holds the unlocked ids). Tests (names are the contract):

```kotlin
@Test fun aCraftableRecipeIsCraftable()
@Test fun aRecipeTheLedgerCannotCoverIsOutOfStockWithoutABadge()
//   assert status == OUT_OF_STOCK, missingInnovation == null
@Test fun aRecipeMissingAnInnovationShowsTheRequiresBadgeInSpanish()
//   assert status == MISSING_INNOVATION, missingInnovation == "Amoníaco"
@Test fun costChipsResolveResourceIdsToSpanishNames()
//   assert costChips == listOf("2 Hueso", "1 Órgano") for ResourceId map {bone.monster=2, organ.monster=1}
@Test fun aBuiltLocationWithoutAnActionHasNoActionButton()
//   assert card.actionLabel == null (fixture: built location with actions = emptyList())
@Test fun aBuiltLocationsActionLabelIsItsOwnAction()
//   assert actionLabel == "Innovar" (fixture action names = LocalizedText(es="Innovar"))
@Test fun aReadyUpgradeShowsInsideItsFirstRequiringBuiltLocationExactlyOnce()
//   built = [Taller, Peletería]; Armería requires [Taller] fully built → upgrade row inside
//   Taller's card, and Armería ∉ buildable
@Test fun aPartiallyReadyLocationStaysInBuildableAndNeverBecomesAnUpgrade()
//   Armería requires [Taller, Stone Circle] with only Taller built → no upgrade anywhere,
//   buildable contains Armería
@Test fun theMergedCatalogPartitionsIntoBuiltAndPendingWithEachLocationExactlyOnce()
//   catalog = ContentCatalog(Core16 + Legacy15).locationDefinitions; built = the two seed ids
//   assert builtCount == 2 && pendingCount == 17
//   assert builtCount + pendingCount == 19
//   assert every not-built location id appears exactly once across upgrades (as target name
//   resolved through the catalog) ∪ buildable cards
@Test fun theHeaderCountsExcludeNothingFromTheCatalog()   // partition == catalog size (fixtures)
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :app-android:testDebugUnitTest --tests "io.kdmtracker.app.ui.settlement.FabricarModelTest"`
Expected: compile FAIL — `FabricarModel`/`of` unresolved.

- [ ] **Step 3: Implement `FabricarModel.of(state: UiState)` in `FabricarModel.kt`**

Derivation rules (the spec pins these; the body is theirs):
- built set = `builtLocations` (projected ES names); `builtCount = built.size`, `pendingCount = catalog.size - builtCount`.
- For each built location (catalog lookup by `names.es`): `recipes` from `state.craftRecipes` where `locationName == name`; status from `check.craftable` plus `missingInnovation = recipe.requires.firstOrNull { it !in unlockedIds }?.let { id -> catalog.innovationDefinitions…names.es }` when the check is not craftable and an innovation is missing, else `OUT_OF_STOCK` when not craftable. `costChips` = `resourceCost` resolved `ResourceId → "N <names.es>"` (fallback `id.value`), ordered by id value.
- `upgrades` = not-built candidates with `this.id in candidate.requires && candidate.requires.all { it in builtIds }`, candidates ordered by id, each assigned to its first requiring built location; assigned candidates are removed from `buildable`.
- `actionLabel` = `actions.firstOrNull()?.names?.es`.
- `BuildableLocationCard.requires` = ids resolved to `names.es` (fallback `id.value`).

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :app-android:testDebugUnitTest --tests "io.kdmtracker.app.ui.settlement.FabricarModelTest"`
Expected: PASS (all tests above).

- [ ] **Step 5: Run the full suite**

Run: `./gradlew spotlessApply && ./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit** (model + tests are not visual; committing is allowed before the gate)

```bash
git add app-android/src/main/kotlin/io/kdmtracker/app/ui/settlement/FabricarModel.kt \
        app-android/src/test/kotlin/io/kdmtracker/app/ui/settlement/FabricarModelTest.kt
git commit -m "feat(app): derive the fabricar section's model from the ui state"
```

---

### Task 2: LocationsSection composables (visual — no commit yet)

**Files:**
- Create: `app-android/src/main/kotlin/io/kdmtracker/app/ui/settlement/LocationsSection.kt`
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/ui/settlement/DevelopPanels.kt` (delete `LocationsPanel` and `LocationRow`; keep `InnovationsPanel`, `PanelTitle`, `SlotRow` — the section reuses `PanelTitle`)

**Interfaces:**
- Consumes: everything Task 1 produces; `PanelTitle` from `DevelopPanels.kt` (same package); `KdmType`/`KdmSymbols`.
- Produces: `@Composable fun LocationsSection(model: FabricarModel, onBuild: (String) -> Unit, onCraft: (String, String) -> Unit, onAction: (String) -> Unit, onSpendEndeavor: () -> Unit)` — and the five private rows it renders (Header line, `BuiltLocationCard`, `RecipeRow`, `UpgradeRow`, `BuildableLocationCard`).

- [ ] **Step 1: Implement `LocationsSection.kt` per the spec's layout**

Structure (the spec's ASCII is the contract): section header `Localizaciones de Asentamiento` + `N CONSTRUIDAS · M PENDIENTES`; built cards with `CONSTRUIDO` badge, optional action button (`enabled`, `onAction(name)`), expandable catalog default-open (`remember { mutableStateOf(true) }`, title `CATÁLOGO DE FABRICACIÓN (N CARTAS)`), recipe rows (chips + status button: `CRAFTABLE`→enabled `FABRICAR`→`onCraft(locationName, gearName)`; `OUT_OF_STOCK`→disabled no badge; `MISSING_INNOVATION`→disabled + red `REQUIERE <missingInnovation>`; `requiresText` as plain text when present), upgrade rows (`Construir <nombre> (MEJORA)` + chips + `ERIGIR`→`onBuild(locationName)`); buildable cards (`POR ERIGIR`/`NO CONSTRUIDO`, `Requiere: …`, endeavors, chips, `CONSTRUIR`→`onBuild(name)`); footer row keeping the existing capability: `Gastar endeavor (−1)` button → `onSpendEndeavor` (the design is silent on it — it moves here from `FabricarTab` rather than disappearing; flagged at the visual gate).

- [ ] **Step 2: Delete `LocationsPanel` + `LocationRow` from `DevelopPanels.kt`**

Remove the composables and any now-unused imports. The file keeps `InnovationsPanel` + shared `PanelTitle`/`SlotRow`.

- [ ] **Step 3: Compile main to establish Task 3's entry condition**

Run: `./gradlew :app-android:compileDebugKotlin`
Expected: exactly one class of error — `SettlementScreen.kt` still referencing `LocationsPanel` (unresolved reference). Any error inside `LocationsSection.kt`/`DevelopPanels.kt` is a real defect: fix before proceeding. The full suite cannot run while `SettlementScreen` is broken; it runs at Task 3.

- [ ] **Step 4: NO COMMIT** — visual work stays uncommitted until Task 4 approval.

---

### Task 3: Wire the section and retire the Fabricar tab (visual — no commit yet)

**Files:**
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/ui/SettlementScreen.kt` (step-7 branch)
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/ui/DevelopStepScreen.kt` (remove `FABRICAR` from `DevelopTab`, its chip, its `when` branch, and the `onBuild`/`onCraft`/`onSpendEndeavor` parameters)
- Delete: `app-android/src/main/kotlin/io/kdmtracker/app/ui/FabricarTab.kt`, `app-android/src/main/kotlin/io/kdmtracker/app/ui/settlement/LocationsModel.kt`
- Modify: `app-android/src/test/kotlin/io/kdmtracker/app/DevelopPanelsTest.kt` (delete the three `LocationsModel` tests — their assertions now live in `FabricarModelTest`; keep the Innovations-model tests)
- Modify: any test still referencing the removed symbols (compile list is the source of truth; `DevelopTabsTest` is controller-level and keeps only a comment mentioning the Fabricar tab — update the comment)

**Interfaces:**
- Consumes: `LocationsSection(model, onBuild, onCraft, onAction, onSpendEndeavor)`, `FabricarModel.of(state)`.
- Produces: step-7 branch wiring: `LocationsSection(model = FabricarModel.of(state), onBuild = onBuild, onCraft = onCraft, onAction = { developTab = DevelopTab.INNOVAR }, onSpendEndeavor = onSpendEndeavor)`; `DevelopStepScreen` keeps `selectedTab`/`onTabSelected` (unchanged since `cdbc699`) with tabs = {RECURSOS, INNOVAR}.

- [ ] **Step 1: Rewire `SettlementScreen` step 7**

Replace the `LocationsPanel(...)` call with `LocationsSection(...)` as above; pass `onSpendEndeavor` to the section instead of `DevelopStepScreen`; `DevelopStepScreen` call loses `onBuild`/`onCraft`/`onSpendEndeavor`.

- [ ] **Step 2: Retire the tab**

`DevelopStepScreen`: drop `FABRICAR` from the enum, the `ChoiceChip("Fabricar"…)`, the `when (DevelopTab.FABRICAR) → FabricarTab(...)` branch, the three now-unused callback parameters, and unused imports. Delete `FabricarTab.kt` and `LocationsModel.kt`.

- [ ] **Step 3: Port the broken tests**

Delete the three `LocationsModel` tests from `DevelopPanelsTest` (assertions already pinned in `FabricarModelTest`: action-not-Construir, 19-count, built/buildable split); fix any other compile errors the deletions cause; update the `DevelopTabsTest` KDoc comment that names the Fabricar tab.

- [ ] **Step 4: Run the full suite**

Run: `./gradlew spotlessApply && ./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: NO COMMIT** — visual work stays uncommitted until Task 4 approval.

---

### Task 4: Visual gate (STOP for approval)

**Files:** none (install + measurement only).

- [ ] **Step 1: Install and measure**

Run: `./gradlew :app-android:installDebug`, then on AVD `kdm` (1080×2400): open the campaign → step 7. Capture screenshots (`pa1_*.png`) and measure against the spec's design list:
- one build list only (no duplicate Construir anywhere), header `N CONSTRUIDAS · M PENDIENTES` (expect `2 CONSTRUIDAS · 17 PENDIENTES` on the dev campaign);
- recipe rows: enabled `FABRICAR` when craftable, disabled-without-badge when out of stock, disabled + `REQUIERE <innovation>` when gated;
- inline `ERIGIR` upgrade with chips inside the unlocking card, not duplicated below;
- `POR ERIGIR` cards with resolved Spanish `Requiere:` names;
- the `Gastar endeavor (−1)` footer (design-silent addition — call it out).

- [ ] **Step 2: Present and STOP**

Present the measurements + screenshots; wait for approval. No commit of Tasks 2–3, no push, no PR before it.

---

### Task 5: After approval — commits, push, PR

- [ ] **Step 1: Work-unit commits**

```bash
git add app-android/src/main/kotlin/io/kdmtracker/app/ui/settlement/LocationsSection.kt \
        app-android/src/main/kotlin/io/kdmtracker/app/ui/settlement/DevelopPanels.kt
git commit -m "feat(app): render the fabricar section from the design"
git add -A
git commit -m "refactor(app): retire the fabricar tab and the duplicate build list"
```

- [ ] **Step 2: Final suite**

Run: `./gradlew spotlessApply && ./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck` + Python `102/102`.
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Push and open the PR**

Push the branch; open the PR against `main` with conventional title + `— size:exception` (line budget 400; authorisation was given for this work stream — confirm at push time), rich body, squash intended. Keep the worktree for feedback.

- [ ] **Step 4: Record in the ledger**

Append the phase's rulings (upgrade-placement rule, the endeavor-footer deviation, anything the gate changed) to the plan's workspace ledger (`.superpowers/sdd/…`).
