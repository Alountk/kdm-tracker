# Innovar Flow (Part A, Phase 2) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement "draw N, keep 1" as the reference's five states, over a hand derived from campaign state, with a single surface for innovating.

**Architecture:** A pure `InnovationDraw` in `:core` turns the settled state into the offer; an extended `InnovationsModel` classifies that into one of four `PhaseState`s and a `CostView`; `InnovationsPanel` renders them. `Innovate.run`, the controller and the transfer document are untouched. The app's second innovating surface (`InnovarTab`) and the location card's action button retire.

**Tech Stack:** Kotlin Multiplatform `:core`, Compose Android `:app-android`, SQLDelight `:data`, kotlinx.serialization, kotlin.test (JVM unit tests), Gradle/spotless.

**Spec:** `docs/superpowers/specs/2026-10-12-innovar-flow-design.md`

**Precondition:** PR #34 merged (Phase 1). Execute in a fresh worktree off `main` (superpowers:using-git-worktrees), with `local.properties` copied from `dev-seed/` and `external-assets` symlinked.

## Global Constraints

- One language per render: `names.es` / `LocalizedText.es` today; never two languages or parenthetical English (spec decision 7).
- Copy is Spanish and matches the reference: `Innovar: Elige 1 de 2 Cartas`, `SELECCIONAR`, `INERTE`, `REQUIERE COSTE`, `YA UTILIZADO (AÑO 1)`, `Acción de Innovar Agotada`, `INNOVAR NO DISPONIBLE`, `No Quedan Innovaciones en el Mazo`, `Todas` / `Adquiridas` / `No adquiridas`. Identifiers, comments and commits are English.
- No new dependencies; style uses the existing Atramentum tokens (`KdmType`, `KdmSymbols`, `KdmActionButton`, theme colours).
- `TransferDocument.schemaVersion` stays **12** and `InnovationSerializer` keeps its exact key set: `category` is content, never state (spec decision 5 + Testing).
- TDD: every model/rule behaviour is a failing test first; the final suite per task is `./gradlew spotlessApply && ./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck`, plus `python3 -m unittest discover -s tools/content-ingest/tests` (104 today).
- Commits: conventional, no `Co-Authored-By`/AI attribution. **Visual gate (hard rule):** Tasks 4–5 stay uncommitted until the maintainer approves the measured render (Task 6); nothing is pushed or PR'd before that, and the PR carries `— size:exception` (budget 400 lines — confirm at push time).

## Review Focus

Inputs no ordinary test below exercises — each is pinned by a test in the task that owns the code:

1. **A campaign at step 7 with nothing unlocked** (`SettlementState.innovations` is empty, so `InnovationDeck.of` is empty) → the section must render `NO_CARDS`, not an empty grid or an exception — `InnovationsModelTest.aCampaignWithNothingUnlockedIsTheEmptyState`.
2. **Fewer available cards than `innovationDraw`** (one card available, draw 2) → the hand is one card and nothing indexes past it — `InnovationDrawTest.theHandIsNeverLongerThanWhatIsAvailable`.
3. **A house rule with `innovationDraw = 4` and `innovationKeep = 1`** → the offer holds four, the model still reports keep 1, and `drawLabel` still reads `Robar 4, elegir 1` — `InnovationsModelTest.theOfferHonoursTheRulesetDrawAndKeep`.
4. **A cost resource the ledger has never held** → the checklist shows `have = 0` for it, `ok = false`, and names it in the reason, without a lookup miss — `InnovationsModelTest.aMissingLedgerLineReadsAsZeroNotAsAnError`.
5. **A v12 document written before `category` existed** → it decodes, the emitted JSON carries no `category` key, and `schemaVersion` is still 12 — `InnovationTest.categoryIsNeverSerialized`.

---

### Task 1: Innovation.category, end to end (content)

**Files:**
- Modify: `tools/content-ingest/identity.py` (`IdentityRow`, `INNOVATIONS_HEADER`, `load_innovations_identity`)
- Modify: `tools/content-ingest/identity/innovations.csv` (new `category` column)
- Modify: `tools/content-ingest/ingest_innovations.py:58,198,260` (load with the new header, emit the field)
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/domain/Innovation.kt` (new field; serializer untouched)
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/content/ContentCatalog.kt:173` (`combineInnovations`)
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/content/Core16Content.kt` (the seed `language` innovation)
- Regenerate: `core/src/commonMain/kotlin/io/kdmtracker/core/content/generated/Core15Innovations.kt`
- Test: `tools/content-ingest/tests/test_innovations_parser.py`, `core/src/commonTest/kotlin/io/kdmtracker/core/domain/InnovationTest.kt`, `core/src/commonTest/kotlin/io/kdmtracker/core/content/ContentCatalogMergeTest.kt`

**Interfaces:**
- Consumes: `load_identity(csv_path, header=…)` and `IdentityRow` as Task 1 defines them; `description_lines`/`indent` from `kotlin_emit`.
- Produces: `Innovation.category: String` (default `""`), `INNOVATIONS_HEADER`, `load_innovations_identity(path) -> Dict[str, IdentityRow]` — consumed by Tasks 3–4 for the card badge and the category chips.

**The taxonomy (spec decision 5).** Category = the domain of the **first-level branch** the innovation descends from, read off `consequences`; the root gets its own. The reference guide confirms five of the seven; the last two are this plan's call and are flagged for review. The whole assignment is mechanical:

| branch (child of the root) | category |
|---|---|
| `Lenguaje` (the root itself) | `Sociedad` |
| `Choza` | `Hogar` |
| `Linterna Interior` | `Creencia` |
| `Simposio` | `Educación` |
| `Pintura` | `Arte` |
| `Tambores` | `Arte` *(flagged)* |
| `Amoníaco` | `Ciencia` |

- [ ] **Step 1: Write the failing Python test**

In `test_innovations_parser.py`, a new class:

```python
class InnovationCategoryTests(unittest.TestCase):
    def test_every_identity_row_carries_a_category(self) -> None:
        rows = load_innovations_identity(
            Path(__file__).resolve().parents[1] / "identity" / "innovations.csv"
        )
        missing = sorted({r.resource_id for r in rows.values() if not r.category})
        self.assertEqual([], missing, "every innovation needs a category for the deck chips")

    def test_the_emitted_card_carries_its_category(self) -> None:
        out = innovations.emit_kotlin([self._card()], load_innovations_identity(self.csv_path))
        self.assertIn('category = "Arte"', out)
```

(_`self._card()` and `self.csv_path` follow the existing helpers in that file; the second test may instead extend `test_emission_carries_the_identity_csv`'s fixture if one already builds a temp CSV._)

- [ ] **Step 2: Run it to verify it fails**

Run: `python3 -m unittest discover -s tools/content-ingest/tests`
Expected: FAIL — `load_innovations_identity` is not defined (and the CSV has no `category` column).

- [ ] **Step 3: Add the column and the loader**

In `identity.py`: `INNOVATIONS_HEADER = ("id", "spanishName", "englishName", "category")`, `IdentityRow.category: str = ""` read from `raw.get("category")`, and `load_innovations_identity(csv_path)` mirroring `load_locations_identity`. In `ingest_locations.py`'s sibling `ingest_innovations.py`, load through it.

- [ ] **Step 4: Fill the 42 rows**

Write `category` for every row using the table above (walk `consequences` to the root's child). Reuse `normalize_ws`'s output conventions; the CSV is quoted by `csv.writer` as in Phase 1.

- [ ] **Step 5: Run the Python tests**

Run: `python3 -m unittest discover -s tools/content-ingest/tests`
Expected: PASS (104 + the new ones).

- [ ] **Step 6: Write the failing Kotlin test**

In `InnovationTest.kt`:

```kotlin
@Test fun categoryIsNeverSerialized() {
    val innovation = Innovation(InnovationId("pintura"), LocalizedText(es = "Pintura"), "…", category = "Arte")
    val json = Json.encodeToString(Innovation.serializer(), innovation)
    assertFalse(json.contains("category"))
    assertEquals("Arte", Json.decodeFromString<Innovation>(json).category)
}
```

In `ContentCatalogMergeTest.kt`: `aCategoriesFillsInFromWhicheverPackCarriesOne` — pack A with `category = ""`, pack B with `category = "Arte"`, both orders → `"Arte"`.

- [ ] **Step 7: Run them to verify they fail**

Run: `./gradlew :core:jvmTest --tests "*InnovationTest*" --tests "*ContentCatalogMergeTest*"`
Expected: FAIL — `No parameter with name 'category'`.

- [ ] **Step 8: Implement**

Add `val category: String = ""` to `Innovation` **without** touching `InnovationSerializer` (content, not state — this is what keeps the schema at v12). Extend `combineInnovations` with `category = current.category.ifBlank { candidate.category }`, add `category = "Sociedad"` to the seed's `language`, and emit `category = "…"` from `ingest_innovations.py`.

- [ ] **Step 9: Regenerate and run the Kotlin tests**

Run: `python3 tools/content-ingest/ingest_innovations.py && ./gradlew :core:jvmTest`
Expected: PASS; the regenerated file is spotless-clean.

- [ ] **Step 10: Run the full suite**

Run: `./gradlew spotlessApply && ./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 11: Commit**

```bash
git add tools/content-ingest core/src
git commit -m "feat(content): categorize every innovation by its tree branch"
```

---

### Task 2: InnovationDraw (core)

**Files:**
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/rules/InnovationDraw.kt`
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/rules/InnovationDrawTest.kt`

**Interfaces:**
- Consumes: `InnovationView` + `InnovationStatus` (already produced per frame by `InnovationAccess.view` and carried on `UiState.innovationViews`), and `UiState.record.settlement.campaignId`.
- Produces: `fun of(campaignId: String?, views: List<InnovationView>, draw: Int): List<Innovation>` — Task 3 calls it as `InnovationDraw.of(state.record?.settlement?.campaignId, state.innovationViews, state.innovationDraw)`.

Three deliberate departures from the spec's signature, each forced by a boundary the spec did not check: `UiState` carries **no `ContentCatalog`** (only the resolved definitions), so the draw takes the already-classified `innovationViews` rather than re-classifying; `draw: Int` rather than `ruleset: Ruleset`, because the number the panel displays comes from `UiState.innovationDraw`, which is what honours house-rule overrides and no `Ruleset` reaches the model; and `campaignId` rather than the record, so a null campaign can express "no offer" without an exception. *(Amend the spec's `Design` block in Task 7.)*

- [ ] **Step 1: Write the failing tests**

```kotlin
class InnovationDrawTest {
    @Test fun theHandIsExactlyTheRequestedSize()
    @Test fun theHandIsNeverLongerThanWhatIsAvailable()      // 1 available, draw 2 → 1
    @Test fun theHandOnlyOffersAvailableCards()              // no UNLOCKED, no NO_ACCESS
    @Test fun theHandIsEmptyWhenNothingIsUnlocked()          // no AVAILABLE view → []
    @Test fun theHandIsEmptyWithoutACampaign()               // campaignId == null → []
    @Test fun theHandIsStableForTheSameCampaign()           // twice → equal lists
    @Test fun theHandDiffersBetweenCampaigns()              // same views, other campaignId → different order
}
```

Fixtures: an `InnovationView` per card — a root, two children and one grandchild — with `status = AVAILABLE` on the two children and `UNLOCKED` on the root.

- [ ] **Step 2: Run to verify they fail**

Run: `./gradlew :core:jvmTest --tests "*InnovationDrawTest*"`
Expected: FAIL — unresolved reference `InnovationDraw`.

- [ ] **Step 3: Implement `InnovationDraw.of`**

`if (campaignId == null) return emptyList()`. Otherwise keep the views whose `status == AVAILABLE`, map to `.innovation`, sort ascending by `hashOf(campaignId, innovation.id.value)`, and `take(draw)`. No RNG, no stored state, no ordering that depends on the list's size.

- [ ] **Step 4: Run to verify they pass**

Run: `./gradlew :core:jvmTest --tests "*InnovationDrawTest*"`
Expected: PASS (6 tests).

- [ ] **Step 5: Run the full suite and commit**

Run: `./gradlew spotlessApply && ./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck`
Expected: BUILD SUCCESSFUL.

```bash
git add core/src/commonMain/kotlin/io/kdmtracker/core/rules/InnovationDraw.kt core/src/commonTest
git commit -m "feat(core): derive the innovation hand from campaign state"
```

---

### Task 3: InnovationsModel — hand, phase state, cost

**Files:**
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/ui/settlement/InnovationsModel.kt`
- Test: `app-android/src/test/kotlin/io/kdmtracker/app/DevelopPanelsTest.kt`

**Interfaces:**
- Consumes: `InnovationDraw.of`, `state.innovationViews` (each view carries `innovation`, `status`, `endeavorCost`, `resourceCost`, `used`), `state.innovationDefinitions`, `state.resourceDefinitions`, `state.storageLines`, `state.endeavors`, `state.chosenPrinciples`, `state.innovationDeck`, `state.innovationDraw`/`innovationKeep`, `state.record`.
- Produces (Task 4 renders these):
  - `fun InnovationsModel.of(state: UiState): InnovationsModel` (replaces the 4-argument `of`)
  - `enum class PhaseState { OFFER, USED_THIS_PHASE, NO_CARDS, UNAFFORDABLE }`
  - `data class ResourceNeed(name: String, have: Int, need: Int)`
  - `data class CostView(endeavors: ResourceNeed, resources: List<ResourceNeed>, ok: Boolean, reason: String)`
  - `data class InnovationCard(id: InnovationId, name: String, category: String, subtitle: String, effect: String, survivalBonus: Int, consequenceNote: String?)`
  - `InnovationsModel(deckSize, principleSlots, emptySlots, drawLabel, hand: List<InnovationCard>, phaseState: PhaseState, cost: CostView, categoryChips: List<Pair<String, Int>>, acquisitionChips: List<Pair<String, Int>>)` — `keep` is not a field: it already reaches the screen through `drawLabel`.

**`PhaseState` priority (spec):** `USED_THIS_PHASE` when `used`; else `NO_CARDS` when `hand.isEmpty()`; else `UNAFFORDABLE` when `!cost.ok`; else `OFFER`. `record == null` ⇒ empty hand + `NO_CARDS`.

- [ ] **Step 1: Write the failing tests**

Rewrite the three existing `InnovationsModel.of(deck, principles, draw, keep)` assertions against a `state()` fixture (copy `FabricarModelTest`'s builder), then add:

```kotlin
@Test fun aCampaignWithNothingUnlockedIsTheEmptyState()
@Test fun theOfferHonoursTheRulesetDrawAndKeep()      // draw=4, keep=1
@Test fun aMissingLedgerLineReadsAsZeroNotAsAnError() // cost needs hide, storage has none
@Test fun usedThisPhaseBeatsEveryOtherState()
@Test fun unaffordableBeatsTheOffer()
@Test fun theSubtitleIsTheUnlockParent()              // "consecuencia de Lenguaje" | "innovación inicial"
@Test fun theFootnoteFollowsTheConsequences()
@Test fun acquisitionChipsComeBeforeCategoryChips()
```

- [ ] **Step 2: Run to verify they fail**

Run: `./gradlew :app-android:testDebugUnitTest --tests "*DevelopPanelsTest*"`
Expected: FAIL — `No value passed for parameter 'hand'` / unresolved `of(state)`.

- [ ] **Step 3: Implement**

`of(state: UiState)`:

- Hand: `InnovationDraw.of(state.record?.settlement?.campaignId, state.innovationViews, state.innovationDraw)`. A null `record` therefore yields an empty hand and, by the priority, `NO_CARDS` — no `!!`, no branch.
- `InnovationCard`: `names.es`, `category` from Task 1, `description`, `survivalLimitBonus`; `subtitle` = `"innovación inicial"` when no catalog entry lists this card's name among its `consequences`, else `"consecuencia de " + <the parent's names.es>` (walk `state.innovationDefinitions`, resolving consequence names with `ContentIdentity.innovationIdFor`); `consequenceNote` = `"Añade consecuencias de " + name + " al mazo"` and **only** when `consequences` is not empty.
- `CostView`: `endeavorCost`/`resourceCost` from the innovating action on any view, against `state.endeavors` and `state.storageLines` (a resource absent from the ledger reads `have = 0`); `ok` is all of them satisfied; `reason` is the reference's sentence naming the ones that fail.
- `phaseState` in the fixed order; `categoryChips` = distinct categories with counts sorted; `acquisitionChips` = `Todas` / `Adquiridas` / `No adquiridas` with their counts.

- [ ] **Step 4: Run to verify they pass**

Run: `./gradlew :app-android:testDebugUnitTest --tests "*DevelopPanelsTest*"`
Expected: PASS.

- [ ] **Step 5: Fix the caller and run the full suite**

`SettlementScreen` step 7 calls `InnovationsModel.of(state = state)`; `InnovationsPanel`'s existing call site compiles unchanged otherwise.

Run: `./gradlew spotlessApply && ./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app-android/src/main/kotlin/io/kdmtracker/app/ui/settlement/InnovationsModel.kt \
        app-android/src/test/kotlin/io/kdmtracker/app/DevelopPanelsTest.kt \
        app-android/src/main/kotlin/io/kdmtracker/app/ui/SettlementScreen.kt
git commit -m "feat(app): model the innovar offer, its four states and its cost"
```

---

### Task 4: InnovationsPanel — the reference's five states (visual, no commit)

**Files:**
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/ui/settlement/DevelopPanels.kt` (`InnovationsPanel`)
- Create: `app-android/src/main/kotlin/io/kdmtracker/app/ui/settlement/InnovationCardView.kt`
- Test: none beyond Task 3's — this task is render only.

**Interfaces:**
- Consumes: everything Task 3 produces; `KdmActionButton`, `KdmType`, `KdmSymbols`.
- Produces: `@Composable fun InnovationsPanel(model: InnovationsModel, onSelect: (String) -> Unit)` — `SettlementScreen` passes `onSelect = onInnovate`.

- [ ] **Step 1: Render the panel per the reference**

Slots (including the fourth, `MAZO ACTIVO`) → deck row with `Mazo de Innovación (N Descubribles)` + its toggle → **`acquisitionChips` first, then `categoryChips`** → offer header `Innovar: Elige 1 de 2 Cartas` + `COSTE …` + the official rule line → the hand as a **2-column grid** of `InnovationCardView`.

Wire the caller in the same step: `SettlementScreen` step 7 renders `InnovationsPanel(model = InnovationsModel.of(state = state), onSelect = onInnovate)`.

Each card: category badge, subtitle, name, bonus chip when `survivalBonus != 0`, `effect`, footnote, and the action — `SELECCIONAR` (gold `KdmActionButton`) when `OFFER`, `INERTE` when `USED_THIS_PHASE`, `REQUIERE COSTE` with a lock when `UNAFFORDABLE`. `onSelect(card.name)` is the only thing that fires.

Per state: the `Acción de Innovar Agotada` / `1 DE 1 USADO` / `YA UTILIZADO (AÑO 1)` banner (3); `No Quedan Innovaciones en el Mazo` + the three help bullets (4); the `FALTA`/`OK` checklist and `Motivo:` from `CostView` (5). Keep the existing `drawLabel`/`deckSize` renderings that already match.

- [ ] **Step 2: Compile and run the suite (render-only)**

Run: `./gradlew spotlessApply && ./gradlew :app-android:compileDebugKotlin && ./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: NO COMMIT** — visual work stays uncommitted until Task 6 approval.

---

### Task 5: One surface — retire the tab and the card's button (visual, no commit)

**Files:**
- Delete: `app-android/src/main/kotlin/io/kdmtracker/app/ui/InnovarTab.kt`
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/ui/DevelopStepScreen.kt` (drop `DevelopTab`, the chip row, `selectedTab`/`onTabSelected`/`onInnovate`; keep `RecursosTab`)
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/ui/SettlementScreen.kt` (drop `developTab` state and the `onAction` wiring)
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/ui/settlement/LocationsSection.kt` (drop `KdmActionButton` from the action row; keep `Costo: N Esfuerzo`; drop the `onAction` parameter)
- Modify: `app-android/src/test/kotlin/io/kdmtracker/app/ui/settlement/FabricarModelTest.kt` (one rename)

**Interfaces:**
- Consumes: `LocationsSection` as Task 4's Phase 1 left it; `DevelopStepScreen`'s current signature.
- Produces: `LocationsSection(model, onBuild, onCraft, onSpendEndeavor)` (no `onAction`); `DevelopStepScreen(state, onAddResource)`.

- [ ] **Step 1: Fix the one name the removal makes stale**

In `FabricarModelTest`, rename `aBuiltLocationWithoutAnActionHasNoActionButton` → `aBuiltLocationWithoutAnActionHasNoCostRow`. The assertion is unchanged (`card.actionLabel == null` is still what decides whether the row draws); only the name stops promising a button. Nothing else in the suite names a button — `DevelopTabsTest` is about resource→craftability and references neither `DevelopTab` nor `InnovarTab`.

- [ ] **Step 2: Run that test to confirm it still passes**

Run: `./gradlew :app-android:testDebugUnitTest --tests "*FabricarModelTest*"`
Expected: PASS — the rename alone must not change behaviour.

- [ ] **Step 3: Retire**

Delete `InnovarTab.kt`; delete the `DevelopTab` enum and the chip row from `DevelopStepScreen.kt` (its signature becomes `state` + `onAddResource`, and its KDoc stops naming a tab switcher); drop `developTab` and the `onAction` lambda from step 7 in `SettlementScreen.kt`; in `LocationsSection.kt` drop the `KdmActionButton` and the `onAction` parameter — the `Costo: N Esfuerzo` line and its `border-t` stay.

- [ ] **Step 4: Run the full suite**

Run: `./gradlew spotlessApply && ./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck`
Expected: BUILD SUCCESSFUL. A compile error naming `DevelopTab`, `InnovarTab` or `onAction` is the list of call sites left to fix, not a surprise.

- [ ] **Step 5: NO COMMIT** — visual work stays uncommitted until Task 6 approval.

---

### Task 6: Visual gate (STOP for approval)

**Files:** none (install + measurement only).

- [ ] **Step 1: Install and measure**

Run `./gradlew :app-android:installDebug`, open the campaign → step 7 on AVD `kdm` (1080×2400), and measure against the reference in **all five states**: the 2-column hand with `SELECCIONAR`; `INERTE` under `YA UTILIZADO (AÑO 1)`; the empty state; `REQUIERE COSTE` with the `FALTA`/`OK` checklist; the chip order (`Todas/Adquiridas/No adquiridas` before the category chips); no `Innovar` button anywhere; the location card keeping `Costo: N Esfuerzo` with no button; no `Innovar` tab.

- [ ] **Step 2: Present and STOP**

Present measurements + screenshots; wait for approval. No commit of Tasks 4–5, no push, no PR before it.

---

### Task 7: After approval — spec amendment, commits, push, PR

- [ ] **Step 1: Amend the spec's `InnovationDraw` signature** to `of(record, catalog, draw: Int)` (the ruling made in Task 2) and commit it with `docs(specs):`.

- [ ] **Step 2: Work-unit commits**

```bash
git add app-android/src/main/kotlin/io/kdmtracker/app/ui/settlement/InnovationsPanel.kt \
        app-android/src/main/kotlin/io/kdmtracker/app/ui/settlement/InnovationCardView.kt
git commit -m "feat(app): render the innovar offer and its five states"
git add -A
git commit -m "refactor(app): retire the innovar tab and the location card's action button"
```

- [ ] **Step 3: Final suite**

Run: `./gradlew spotlessApply && ./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck` + Python `104+`.
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Push and open the PR**

Push the branch; open the PR against `main` with a conventional title + `— size:exception` (confirm at push time), rich body, squash intended. Keep the worktree for feedback.

- [ ] **Step 5: Record in the ledger**

Append the phase's rulings (the derived hand, the one-surface decision, the `Tambores`/`Linterna Interior` category flags, the gate results) to `.superpowers/sdd/…/progress.md`.
