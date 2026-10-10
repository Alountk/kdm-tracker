# Content Identity S2 — locations, gear and innovations Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extend the identity, naming and re-keying that S1 gave resources to the three remaining content families — settlement locations, gear and innovations — so the Develop step's panels stop showing the same entity twice, once per language.

**Architecture:** The mechanism is S1's, replayed: a `value class` id, a `LocalizedText` name and a versioned identity CSV per family, with the PDF-deck emitters looking each card up and failing loudly on an unmapped one. Recipe and build costs — name-keyed and bridged by `ContentIdentity` after S1 — are re-keyed to ids here, which lets both transitional bridges (`ContentIdentity`'s name lookup and `ContentCatalog.resourceDefinitionFor`) shrink to the decode path they were always meant for.

**Tech Stack:** as S1 — KMP `:core`, SQLDelight `:data`, Compose Android `:app-android`, kotlinx.serialization, kotlin.test, Python 3 + poppler, Gradle.

**Spec:** `docs/superpowers/specs/2026-10-10-content-identity-design.md` (S2 in its "Slices" table)

## Global Constraints

- **Spanish is the default language**; `LocalizedText.inLanguage` falls back `EN → ES`. No new dependencies.
- **Artifacts in English**: code, identifiers, comments, commits, docs. Conventional commits, no AI attribution.
- **Run `./gradlew spotlessApply` before every commit.**
- **The generated packs stay deterministic**: after any emitter change, re-running it must leave `git diff --exit-code` clean, and spotless must not reformat its output (S1 established that the emitter emits ktlint-clean code — keep it that way).
- **No content entity may carry a name as its identity** when this plan is done.
- **The ledger and the state never store a display name**: ids only, names resolved from the catalog where they are rendered.
- **Legacy decode must be lossless.** A settlement persisted before this change carries names; it must decode into ids without dropping a location, a gear or an innovation. An unmapped name survives under a synthesized id rather than being discarded — S1's rule, reused.
- **400-line review budget per PR**; `size:exception` must be authorised at the gate, as in S1.
- **Visual gate (T4 only):** no commit, push, PR or merge until the user approves the rendered result. Install, leave the app signed in on the settlement screen, measure, present, stop.

## Review Focus

Five inputs the spec implies but whose behaviour no task's ordinary tests would catch. Each gets its test in the owning task.

1. **A name in a legacy settlement that maps to no id.** Expected: the location/gear/innovation survives under a synthesized id, stays visible, and the settlement's derived state is unchanged. (T2.)
2. **Two locations that were the same entity under two languages**, e.g. `Lantern Hoard` and `Cúmulo de Linternas`. Expected: **one** definition after the merge, and the built set holds one id — the panel's `32 Opciones` collapses. (T1, re-pinned at the T4 panel.)
3. **A recipe whose cost names a resource the catalog does not carry.** Expected: `Craft` returns `Blocked` with a reason; it must not throw and must not partially spend. (T1 — the bridge is deleted here, so the failure mode changes.)
4. **A `LocalizedText` with a null `en` while the language is `EN`.** Expected: the Spanish name, never the id. (T4.)
5. **A v11 transfer document** importing into the new build. Expected: still decodes, with locations/gear/innovations mapped to ids. (T3.)

---

### Task 1: Re-key gear, locations and innovations, and produce their ids from the source

One commit on purpose: `SettlementLocation.recipes` holds `GearRecipe`s and `GearRecipe.gearName` names the gear, so the definition types, both packs and the catalog cannot be moved separately and still compile.

**Files:**
- Modify: `core/.../domain/Gear.kt`, `SettlementLocation.kt`, `SettlementAction.kt`, `GearRecipe.kt`, `Innovation.kt`
- Create: `tools/content-ingest/identity/{locations,gear,innovations}.csv`
- Modify: `tools/content-ingest/{ingest_locations,ingest_gear,ingest_innovations}.py`
- Modify: `core/.../content/generated/{Core15Locations,Core15Gear,Core15Innovations}.kt` (regenerated)
- Modify: `core/.../content/Core16Content.kt`, `Legacy15Content.kt`, `ContentCatalog.kt`, `ContentIdentity.kt`
- Modify: `core/.../usecase/{Craft,BuildLocation,Innovate}.kt`
- Test: `core/src/commonTest/.../content/ContentIdentityTest.kt` (extend), plus every test the shape change breaks

**Interfaces:**
- Consumes: `ResourceId`, `LocalizedText` (S1), `ContentIdentity`.
- Produces: `Gear(id: GearId, names: LocalizedText, …)`; `SettlementLocation(id: LocationId, names: LocalizedText, requires: List<LocationId>, buildCost: Map<ResourceId, Int>, actions: List<SettlementAction>, recipes: List<GearRecipe>)`; `SettlementAction(names: LocalizedText, …)`; `GearRecipe(gearId: GearId, resourceCost: Map<ResourceId, Int>, requires: List<InnovationId>, requiresText: String?)`; `Innovation(id: InnovationId, names: LocalizedText, …)`; `ContentIdentity.locationIdFor/gearIdFor/innovationIdFor(name)`.

- [ ] **Step 1: Author the three identity CSVs**

Same shape as `resources.csv` (`id,spanishName,englishName`), one row per name each pack produces. The ids follow S1's rule — a human-chosen permanent slug, `<family>.<variant>`, never derived from a display name. The English column is filled only for what the settlement screen shows: the built locations and their actions.

- [ ] **Step 2: Re-key the five domain types**

`name: String` becomes `id` + `names` on `Gear`, `SettlementLocation` and `Innovation`; `SettlementAction` keeps its `kind` as its identity and gains `names`. Every name-keyed reference becomes an id: `LocationSet.built`, `SettlementLocation.requires`, `SettlementLocation.buildCost`, `GearRecipe.gearName`, `GearRecipe.resourceCost`, `GearRecipe.requires`.

- [ ] **Step 3: Teach the three emitters to emit ids and names**

Each emitter looks every card up in its CSV and **`sys.exit` non-zero, naming the card, when it is absent** — the loud-failure rule S1 established. Emit `id = …Id("…"), names = LocalizedText(es = …, en = …)` and the re-keyed cost maps, emitting ktlint-clean code at ≤120 columns.

- [ ] **Step 4: Regenerate the three packs and verify determinism**

```bash
for s in locations gear innovations; do python3 tools/content-ingest/ingest_$s.py; done
git diff --exit-code core/src/commonMain/kotlin/io/kdmtracker/core/content/generated/
./gradlew :core:spotlessApply && git diff --exit-code core/src/commonMain/kotlin/io/kdmtracker/core/content/generated/
```
Expected: each emitter rewrites its file once; a second run and spotless both leave the diff clean.

- [ ] **Step 5: Update the packs, the catalog, the bridge and the three use cases**

`Core16Content` gains ids and both-language names (its location tree and its `Innovate` action are English today — that is what shows `Innovate` on a Spanish screen); `Legacy15Content` maps through its real names. `ContentCatalog` merges locations, gear and innovations by id, and `resources`/`locations`/`innovations` (the derived name lists) read `names.es`. `Craft`, `BuildLocation` and `Innovate` resolve definitions by id.

```kotlin
class ContentIdentityTest {
    @Test fun everyCatalogLocationGearAndInnovationResolvesToAnId() { /* one loop per family */ }

    @Test fun theSameEntityUnderTwoLanguagesMergesIntoOne() {
        // "Lantern Hoard" (seed) and "Cúmulo de Linternas" (1.5) are one location, so the merged
        // catalog carries it once and the panel's option count collapses.
        assertEquals(1, catalog.locationDefinitions.count { it.id == LocationId("lantern_hoard") })
    }
}
```

- [ ] **Step 6: Move the existing assertions and run the suite**

Every content test that pins a location, gear or innovation name moves to `names.es` / `id`: `ContentCatalogTest`, `ContentCatalogMergeTest`, `Core16ContentTest`, `Legacy15ContentTest`, `Core15GearTest`, `Core15GearContentTest`, `Core15LocationsTest`, `GeneratedInnovationsTest`, `GearCatalogTest`, `BuildLocationUseCaseTest`, `CraftUseCaseTest`, `InnovateUseCaseTest`, `CraftabilityTest`.

Run: `./gradlew :core:jvmTest`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
./gradlew spotlessApply
git add -A
git commit -m "feat(core): key locations, gear and innovations by a stable id with localized names"
```

---

### Task 2: Re-key the settlement state, and keep the old shape decoding

One commit, for S1's reason: a state type that requires an id with default serialization makes every persisted `stateJson` fail to decode, and the mapper's `runCatching { … }.getOrElse { SettlementState() }` would silently reset the settlement.

**Files:**
- Modify: `core/.../domain/{SettlementLocation,Innovation,SettlementState}.kt` (the serializers)
- Test: `core/src/commonTest/.../domain/SettlementStateTest.kt`, `.../transfer/SettlementStateTransferTest.kt`, plus a new serializer test per family

**Interfaces:**
- Produces: `LocationSet.built: List<LocationId>`; `SettlementState.gear: List<GearId>`; `InnovationCollection` keyed by `InnovationId`; a two-shape `KSerializer` for each of the three, mirroring `ResourceEntrySerializer`.

- [ ] **Step 1: Write the failing tests**

```kotlin
@Test fun aLegacyBuiltLocationDecodesThroughTheIdentityTable() {
    val legacy = """{"built":["Lantern Hoard"]}"""
    assertEquals(listOf(LocationId("lantern_hoard")), json.decodeFromString(LocationSet.serializer(), legacy).built)
}

@Test fun anUnmappedLegacyLocationSurvivesUnderASynthesizedId() {
    val legacy = """{"built":["Sitio Que No Existe"]}"""
    val set = json.decodeFromString(LocationSet.serializer(), legacy)
    assertTrue(set.built.single().value.startsWith("unknown."))
}

@Test fun aLegacyGearListDecodes() { /* SettlementState.gear */ }

@Test fun aLegacyInnovationDecodes() { /* InnovationCollection by id */ }
```

- [ ] **Step 2: Run the tests to verify they fail** — `./gradlew :core:jvmTest`; expect unresolved-reference failures.

- [ ] **Step 3: Implement the re-keying and the serializers**

Each family gets the two-shape decode S1 gave `ResourceEntry`: `id` present means new, otherwise the legacy name maps through `ContentIdentity`, and an unmapped one becomes `unknown.<slug>` so nothing is lost.

- [ ] **Step 4: Run the tests to verify they pass** — `./gradlew :core:jvmTest :app-android:testDebugUnitTest :data:jvmTest`.

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply
git add -A
git commit -m "refactor(core): key the settlement state by id and carry the old shape"
```

---

### Task 3: Bump the transfer document to 12

**Files:** `core/.../transfer/TransferDocument.kt`; its tests.

- [ ] **Step 1: Add the v11-still-decodes test, then bump** — `schemaVersion = 12`, `SUPPORTED_VERSIONS` gains 12, and the three version-pinned tests move with it (S1's precedent).
- [ ] **Step 2: Run** — `./gradlew :core:jvmTest :data:jvmTest`.
- [ ] **Step 3: Commit** — `git commit -m "feat(core): bump the transfer document to 12"`.

---

### Task 4: The panels read localized names — and the two cheap defects

**VISUALLY GATED.** No commit, push, PR or merge until the user approves the rendered result.

**Files:** `app-android/.../ui/settlement/{LocationsModel,InnovationsModel,DevelopPanels}.kt`, `app-android/.../LanternYearController.kt`, `app-android/.../ui/RecursosTab.kt` and the other storage-line readers.

**Interfaces:**
- Consumes: everything above.
- Produces: panels rendering `names.inLanguage(language)`; a built location showing its own action rather than `Construir`; the principe slots rendering the principle's label rather than a concatenated string.

- [ ] **Step 1: Write the failing model tests**

```kotlin
@Test fun aBuiltLocationShowsItsActionNotConstruir() { /* built → its action, buildable → "Construir" */ }

@Test fun aPrincipleSlotShowsTheChoiceLabelNotTheCategoryPrefix() {
    // The controller built "DEATH: The First Harvest → Cannibalize" by hand; the slot shows the
    // label the player chose.
    assertEquals("The First Harvest → Cannibalize", slot.labelFor(PrincipleCategory.DEATH))
}
```

- [ ] **Step 2: Run them to verify they fail.**
- [ ] **Step 3: Implement** — the models resolve names; the built-location label distinguishes built from buildable; `LanternYearController` stops concatenating `"${category.name}: ${label}"` and passes the bare label.
- [ ] **Step 4: Run the tests and install for the visual gate** — `./gradlew :app-android:testDebugUnitTest :app-android:installDebug`. Sign in, open the settlement screen, leave it on step 7. Capture, measure the location list and the option count against the design, and present. **Stop for approval.**
- [ ] **Step 5: After approval, commit** — `git commit -m "feat(app): render the develop panels from localized content"`.

---

## Final steps

- Whole-branch review after T4 is approved, then PR per task (or the chained set) under the agreed delivery strategy.
- **S3** (the full resource catalog and English names) and **S4** (the language selector) follow. After S2 the two transitional bridges shrink to the decode path, and the panel's `32 Opciones` is gone.
