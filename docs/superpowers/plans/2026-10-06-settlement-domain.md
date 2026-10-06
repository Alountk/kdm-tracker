# Settlement Domain Backbone & Content Catalog Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Model the settlement record from the 1.6 sheet and introduce a **modular content-catalog seam** (`ContentPack` + `ContentCatalog`) so game content is data selected per campaign — all in pure `:core` with strict TDD. No UI, no persistence wiring yet.

**Architecture:** Mechanisms stay in `rules/`; content lives in `ruleset/` (the existing `Ruleset`) and in **content packs** that bundle game content (innovations, locations, quarries, nemeses, resources, timeline). A `ContentCatalog.compose(packs)` validates `requires` and merges packs; a campaign references `rulesetId + enabledPacks + overrides`. Everything added here is **additive** — the existing `Settlement`/wire types are untouched, so the whole suite stays green.

**Tech Stack:** Kotlin 2.3.21, Kotlin Multiplatform, kotlinx-serialization, `kotlin.test`. Strict TDD (RED → GREEN → refactor).

**Spec:** `docs/superpowers/specs/2026-10-05-kdm-tracker-offline-first-kmp-design.md`
**Reference:** settlement sheet `external-assets/asentamiento_v1.6.pdf` (converted to images during the design conversation).

## Global Constraints

- Kotlin `2.3.21`; `:core` stays pure (no Spring/Android/SQLDelight); `kotlin.test` only; no new dependencies.
- Strict TDD: every task writes the failing test first, observes RED, implements, observes GREEN.
- Content is **data**, not code: no settlement-record constant may be hardcoded in `rules/`.
- Packs are identified by a stable string id (`core-1.6`, `gorm`, …) and declare `requires`.
- Conventional Commits; **no** `Co-Authored-By`/AI attribution.
- `./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck` stays green.

## Review Focus

1. **Packs compose correctly**: a pack whose `requires` is unmet is rejected; composing the same packs in any order yields the same catalog.
2. **Milestone conditions are machine-checkable**, not prose: "first death", "first birth", "population 15", "5 innovations", "population 0 (Game Over)" each evaluate from the settlement state.
3. **No magic numbers in `rules/`**: survival limit, timeline length, endeavor rate, etc. come from the ruleset/packs.
4. **Prerequisites are enforced**: a settlement location/innovation cannot be added without its `requires`.
5. **Additive**: the existing `Settlement`, transfer DTOs, and sync wire types are unchanged and their tests still pass.

---

## File Structure

```text
core/src/commonMain/kotlin/io/kdmtracker/core/
├── content/                       # NEW — modular content packs
│   ├── ContentPack.kt             # ContentPack, ContentBundle, requires
│   ├── ContentCatalog.kt          # compose + validation
│   └── Core16Content.kt           # the seed pack for 1.6
├── domain/
│   ├── Timeline.kt                # Timeline, TimelineEra, TimelineEntry
│   ├── Innovation.kt              # Innovation + deck
│   ├── SettlementLocation.kt      # location + prerequisites
│   ├── Storage.kt                 # inventory projection by category
│   ├── Death.kt                   # deaths + first-death flag
│   └── SettlementState.kt         # the aggregate tying it together
└── rules/
    └── MilestoneConditions.kt     # machine-checkable milestone predicates
```

---

### Task 1: Content-catalog seam

**Files:**
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/content/ContentPack.kt`
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/content/ContentCatalog.kt`
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/content/ContentCatalogTest.kt`

**Interfaces:**
- Produces:
  - `@Serializable data class ContentPack(val id: String, val version: String, val name: String, val requires: List<String> = emptyList(), val provideInnovations: List<String> = emptyList(), val provideLocations: List<String> = emptyList(), val provideQuarries: List<String> = emptyList(), val provideNemeses: List<String> = emptyList(), val provideResources: List<String> = emptyList())`
  - `sealed interface CatalogResult { data class Ok(val catalog: ContentCatalog) : CatalogResult; data class MissingRequirement(val packId: String, val missing: String) : CatalogResult }`
  - `data class ContentCatalog(val packs: List<ContentPack>)` with `innovations/locations/quarries/nemeses/resources` as the **sorted union** across packs.
  - `object ContentCatalogComposer { fun compose(packs: List<ContentPack>): CatalogResult }` — validates every `requires` id is present; on success merges content (union, dedup, sorted for determinism).
- Tests: compose is order-independent; a missing requirement returns `MissingRequirement`; `requires` of a present pack passes; the union dedups.

- [ ] **Step 1:** Write the failing `ContentCatalogTest` (order-independence, missing requirement, dedup, sorted union).
- [ ] **Step 2:** Run `./gradlew :core:jvmTest` — expected FAIL.
- [ ] **Step 3:** Implement `ContentPack`, `ContentCatalog`, `ContentCatalogComposer`.
- [ ] **Step 4:** Run — expected PASS. Commit `feat(core): add the content-pack catalog seam`.

---

### Task 2: Timeline

**Files:**
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/domain/Timeline.kt`
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/domain/TimelineTest.kt`

**Interfaces:**
- Produces:
  - `data class TimelineEntry(val lanternYear: Int, val label: String)`
  - `data class Timeline(val entries: List<TimelineEntry> = emptyList(), val length: Int = 40)`
  - `fun Timeline.withEvent(year, label): Timeline` (idempotent per year)
  - `fun Timeline.entriesFor(year: Int): List<TimelineEntry>`
  - A helper `Timeline.core16(length: Int = 40): Timeline` seeded from the sheet's fixed entries (Returning Survivors at 1, the story/nemesis years) — **content**, so the labels come from the sheet, not the mechanism.
- Tests: default length 40; adding an event at a year is idempotent; `entriesFor` filters; a year out of `1..length` is rejected or ignored per the chosen rule (state it).

- [ ] **Step 1:** Write the failing `TimelineTest`.
- [ ] **Step 2:** Run — expected FAIL.
- [ ] **Step 3:** Implement `Timeline`.
- [ ] **Step 4:** Run — expected PASS. Commit `feat(core): add the settlement timeline`.

---

### Task 3: Machine-checkable milestone conditions

**Files:**
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/rules/MilestoneConditions.kt`
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/rules/MilestoneConditionsTest.kt`

**Interfaces:**
- Produces:
  - `data class MilestoneState(val deaths: Int, val births: Int, val population: Int, val innovations: Int)`
  - `fun interface MilestoneCondition { fun reached(state: MilestoneState): Boolean }`
  - `object MilestoneConditions { val death; val newLife; val society; val hoodedKnight; val gameOver; fun forKey(key: MilestoneKey): MilestoneCondition }` — `PRINCIPLE_DEATH` = `deaths >= 1`; `PRINCIPLE_NEW_LIFE` = `births >= 1`; `PRINCIPLE_SOCIETY` = `population >= 15`; `HOODED_KNIGHT` = `innovations >= 5`; `GAME_OVER` = `population <= 0`.
  - `fun reachedMilestones(state: MilestoneState, keys: Collection<MilestoneKey>): Set<MilestoneKey>`
- Tests: each predicate at its boundary (0 vs 1 death, 14 vs 15 population, 4 vs 5 innovations, population 0 → Game Over); `reachedMilestones` returns exactly the reached set.
- Note the 15/5/1 thresholds come from the `Ruleset` milestones where feasible; if kept as constants, add a test asserting they match `Core16Ruleset`'s declared conditions (no silent drift).

- [ ] **Step 1:** Write the failing `MilestoneConditionsTest`.
- [ ] **Step 2:** Run — expected FAIL.
- [ ] **Step 3:** Implement the predicates.
- [ ] **Step 4:** Run — expected PASS. Commit `feat(core): add machine-checkable milestone conditions`.

---

### Task 4: Innovations and the innovation deck

**Files:**
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/domain/Innovation.kt`
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/domain/InnovationTest.kt`

**Interfaces:**
- Produces:
  - `data class Innovation(val name: String, val description: String, val consequences: List<String> = emptyList(), val survivalLimitBonus: Int = 0)`
  - `data class InnovationCollection(val innovations: List<Innovation> = emptyList())` with `fun with(innovation): InnovationCollection` (dedup by name) and `fun of(names: Collection<String>, catalog: List<Innovation>): InnovationCollection` (resolve names against a catalog, erroring/ignoring unknowns per the chosen rule — state it).
  - `data class InnovationDeck(val cards: List<String>)` — the deck = unlocked innovations + their `consequences` (the sheet notes "must contain exactly the unlocked consequences").
- Tests: dedup by name; consequence resolution; `of` resolves known names and rejects/ignores unknown ones deterministically; `survivalLimitBonus` sums.

- [ ] **Step 1:** Write the failing `InnovationTest`.
- [ ] **Step 2:** Run — expected FAIL.
- [ ] **Step 3:** Implement `Innovation`.
- [ ] **Step 4:** Run — expected PASS. Commit `feat(core): add innovations and the innovation deck`.

---

### Task 5: Settlement locations with prerequisites

**Files:**
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/domain/SettlementLocation.kt`
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/domain/SettlementLocationTest.kt`

**Interfaces:**
- Produces:
  - `data class SettlementLocation(val name: String, val requires: List<String> = emptyList())`
  - `data class LocationSet(val built: List<String> = emptyList())` with `fun canBuild(location, catalog): Boolean` (all `requires` are in `built`) and `fun with(location): LocationSet` (adds only when buildable; idempotent).
- Tests: building `Bone Smith` requires `Lantern Hoard` (from the sheet's tree); cannot build without the prerequisite; building is idempotent; building an unknown location is rejected/ignored deterministically.

- [ ] **Step 1:** Write the failing `SettlementLocationTest`.
- [ ] **Step 2:** Run — expected FAIL.
- [ ] **Step 3:** Implement `SettlementLocation` + `LocationSet`.
- [ ] **Step 4:** Run — expected PASS. Commit `feat(core): add settlement locations with prerequisites`.

---

### Task 6: Storage (inventory projection)

**Files:**
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/domain/Storage.kt`
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/domain/StorageTest.kt`

**Interfaces:**
- Produces:
  - `data class StorageLine(val name: String, val category: String, val quantity: Int)`
  - `object Storage { fun project(entries: List<ResourceEntry>): List<StorageLine>; fun total(entries): Int; fun forCategory(entries, category): List<StorageLine> }` — projects the existing append-only resource ledger (`ResourceEntry` from the resource plan) into non-negative quantities grouped by category, sorted.
- Tests: gains/spends project to the net quantity; a category filter works; quantities never go negative (or the rule is stated); empty ledger → empty storage.

- [ ] **Step 1:** Write the failing `StorageTest`.
- [ ] **Step 2:** Run — expected FAIL. (If `ResourceEntry` does not exist yet, define the minimal ledger type here.)
- [ ] **Step 3:** Implement `Storage`.
- [ ] **Step 4:** Run — expected PASS. Commit `feat(core): add settlement storage projection`.

---

### Task 7: Settlement state aggregate

**Files:**
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/domain/SettlementState.kt`
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/domain/SettlementStateTest.kt`

**Interfaces:**
- Produces:
  - `data class SettlementState(val lanternYear: Int = 1, val survivalLimit: Int, val timeline: Timeline = Timeline(), val innovations: InnovationCollection = InnovationCollection(), val locations: LocationSet = LocationSet(), val storage: List<StorageLine> = emptyList(), val population: Int = 0, val deaths: Int = 0, val births: Int = 0, val enabledPacks: List<String> = emptyList())`
  - `fun SettlementState.milestoneState(): MilestoneState` (adapts to Task 3)
  - `fun SettlementState.reachedMilestones(): Set<MilestoneKey>`
- Tests: the adapter maps population/deaths/births/innovations correctly; `reachedMilestones` composes Task 3; a fresh state has none reached; 15 population reaches Society; 0 population reaches Game Over.

- [ ] **Step 1:** Write the failing `SettlementStateTest`.
- [ ] **Step 2:** Run — expected FAIL.
- [ ] **Step 3:** Implement `SettlementState` + the adapter.
- [ ] **Step 4:** Run — expected PASS. Commit `feat(core): add the settlement state aggregate`.

---

### Task 8: Seed pack + campaign config + verification

**Files:**
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/content/Core16Content.kt`
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/domain/Campaign.kt` (add `enabledPacks: List<String> = emptyList()` with a default so existing construction still compiles)
- Modify: `README.md`
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/content/Core16ContentTest.kt`

**Interfaces:**
- Produces: `object Core16Content { val pack: ContentPack }` — the `core-1.6` pack with the sheet's minimal content (the location tree, the initial innovations like `Language`, the base resources, the quarantine/nemesis names), and `ContentCatalogComposer.compose(listOf(Core16Content.pack))` succeeds.
- `Campaign.enabledPacks` defaults to `emptyList()`, so existing call sites and DTOs keep working (verify `:core:jvmTest` + `:data:jvmTest` + the transfer tests still pass — Review Focus 5).
- README: document the content-pack model (edition + expansion packs, campaign selection, `requires`).

- [ ] **Step 1:** Write the failing `Core16ContentTest` (compose succeeds; the pack provides the expected seed items).
- [ ] **Step 2:** Run — expected FAIL.
- [ ] **Step 3:** Implement `Core16Content` + the `Campaign.enabledPacks` field; update the README.
- [ ] **Step 4:** Run the FULL suite — `./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck` — expected PASS (additive change, no breakage).
- [ ] **Step 5:** Commit `feat(core): add the core-1.6 content pack and campaign selection`.

---

## Self-Review

**1. Spec coverage**

| Spec item | Covered by |
| --- | --- |
| D6 rules engine vs. ruleset data (modular) | Tasks 1, 8 |
| Settlement record model (1.6 sheet) | Tasks 2–7 |
| Machine-checkable rules (TDD) | Task 3 |
| Content as data (packs, edition + expansion) | Tasks 1, 8 |
| Content **ingestion** from the card PDFs | **Deferred** — a later pipeline plan (text-extractable sets first; image cards via OCR/vision) |
| Steps 1–11 UI + wiring | **Deferred** — one slice per step |
| Persistence of the new state (`:data` schema v4) | **Deferred** — after the model settles |

**2. Step scan:** each step is RED → implement → GREEN → commit; the type shapes are exact.

**3. Type consistency:** `ContentPack`/`ContentCatalog` (Task 1) are used by Task 8; `MilestoneState` (Task 3) is used by Task 7; `Timeline`/`Innovation`/`LocationSet`/`StorageLine` (Tasks 2,4,5,6) are composed by Task 7.

**4. Review Focus:** each of the five lines maps to a task with tests (1, 3, 3/8, 5, 8).

**5. Proportion:** task-level; strict-TDD steps; no code transcripts beyond the type shapes.
