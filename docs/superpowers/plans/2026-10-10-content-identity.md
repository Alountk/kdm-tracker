# Content Identity (S1 — resources end to end) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give resources a stable language-neutral id, a localized-name layer and an explicit material, so the settlement summary aggregates monster resources into the right bucket instead of double-counting them per language.

**Architecture:** Five task-sized PR slices. Tasks 1–2 add inert vocabulary (types only, no behaviour change). Task 3 re-keys `ResourceDefinition` and its two producers (the hand-written 1.6 seed and the PDF-generated 1.5 pack) so both packs carry ids and materials. Task 4 re-keys the resource ledger and, in the same commit, teaches the entry serializer to keep decoding the old shape and bumps the transfer document — the re-key and its tolerant decode cannot be separated without a window in which persisted settlements reset. Task 5 is the only user-visible change. Recipe costs stay name-keyed throughout and are bridged by `ContentIdentity` — a deliberate transitional bridge that S2 removes when gear and locations carry ids themselves.

**Tech Stack:** Kotlin Multiplatform (`:core` common), SQLDelight (`:data`), Compose Multiplatform Android (`:app-android`), kotlinx.serialization, kotlin.test (`:core:jvmTest`), Python 3 + poppler (`tools/content-ingest`), Gradle.

**Spec:** `docs/superpowers/specs/2026-10-10-content-identity-design.md`

## Global Constraints

- **Spanish is the default language**; `LocalizedText.inLanguage` falls back `EN → ES` and never renders an id or an empty string.
- **No new dependencies.** The language preference uses plain `SharedPreferences`, following `AndroidKeystoreTokenStore` — the version catalog is not touched.
- **Artifacts in English**: code, identifiers, comments, commit messages, docs. Conventional commits, no AI attribution.
- **Run `./gradlew spotlessApply` before every commit.** Spotless strips imports that become unused, so a re-added usage needs its import re-added.
- **The generated pack stays deterministic**: after any `ingest_resources.py` change, `git diff --exit-code core/src/commonMain/kotlin/io/kdmtracker/core/content/generated/` must be clean on a re-run.
- **400-line review budget per PR.** The five tasks are the five PR slices. Task 3 is the one expected to exceed it — the regenerated pack is mechanical; if the forecast crosses the budget, raise it at the review gate rather than splitting a shape change that must land atomically.
- **Visual gate (task 5 only):** no commit, push, PR or merge until the user approves the rendered result on the emulator. Install, leave the app signed in on the settlement screen, measure, present, stop.
- **The ledger never stores a display name.** Names are resolved from `resourceDefinitions` at render time.

## Review Focus

Five inputs the spec implies but whose behaviour no task's ordinary tests would catch. Each gets its test in the owning task, named in that task's steps.

1. **A persisting settlement whose ledger names a resource the identity table does not cover** (a hand-edited or future pack). Expected: the entry **survives under a synthesized id and stays visible** — it is never silently dropped, and the settlement's total is unchanged. (Task 4/5.)
2. **Two variants of one material in the same ledger** — `Pelaje Blanco` and `Piel de Monstruo`, both `HIDE`. Expected: the projection returns **one** `HIDE` line whose quantity is the sum, not two lines. (Task 4.)
3. **`LocalizedText` with a null `en` while the language is `EN`.** Expected: the Spanish string is shown — never the id, never an empty label. (Task 1, re-pinned at the UI in task 5.)
4. **A transfer document at `schemaVersion` 10** importing into the new build. Expected: it still decodes, with the ledger mapped to ids. (Task 5.)
5. **A `GearRecipe` whose `resourceCost` names a resource absent from the catalog.** Expected: `Craft` returns `Blocked` with a reason; it must not throw and must not partially spend. (Task 4.)

---

### Task 1: The vocabulary — `Language`, `LocalizedText`, `ResourceMaterial`

**Files:**
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/domain/Language.kt`
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/domain/LocalizedText.kt`
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/domain/ResourceMaterial.kt`
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/domain/LocalizedTextTest.kt`

**Interfaces:**
- Consumes: nothing.
- Produces: `enum class Language { ES, EN }`; `@Serializable data class LocalizedText(val es: String, val en: String? = null)` with `fun inLanguage(language: Language): String`; `enum class ResourceMaterial(val names: LocalizedText) { BONE, ORGAN, HIDE, SCRAP }` whose four members carry `Hueso/Bone`, `Órgano/Organ`, `Pellejo/Hide`, `Chatarra/Scrap`.

- [ ] **Step 1: Write the failing test**

```kotlin
class LocalizedTextTest {
    @Test fun spanishLanguageReturnsTheSpanishName() {
        val text = LocalizedText(es = "Daga de Hueso", en = "Bone Dagger")
        assertEquals("Daga de Hueso", text.inLanguage(Language.ES))
    }

    @Test fun englishLanguageReturnsTheEnglishName() {
        val text = LocalizedText(es = "Daga de Hueso", en = "Bone Dagger")
        assertEquals("Bone Dagger", text.inLanguage(Language.EN))
    }

    @Test fun englishLanguageFallsBackToSpanishWhenEnglishIsMissing() {
        val text = LocalizedText(es = "Hueso de Monstruo")
        assertEquals("Hueso de Monstruo", text.inLanguage(Language.EN))
    }

    @Test fun everyResourceMaterialCarriesBothNames() {
        ResourceMaterial.entries.forEach { material ->
            assertTrue(material.names.es.isNotBlank(), "$material has no Spanish name")
            assertNotEquals(material.names.es, material.names.inLanguage(Language.EN))
        }
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :core:jvmTest --tests "io.kdmtracker.core.domain.LocalizedTextTest"`
Expected: FAIL — unresolved references to `LocalizedText`, `Language`, `ResourceMaterial`.

- [ ] **Step 3: Implement the three files**

`LocalizedText` is a data class holding the two names; `inLanguage` uses `en ?: es` for `EN`. `ResourceMaterial` is an enum with a constructor property `val names: LocalizedText` — a four-entry table, no UI map. Add a `ResourceMaterialTest` case pinning the four members and their two names as exact strings.

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :core:jvmTest --tests "io.kdmtracker.core.domain.LocalizedTextTest" --tests "io.kdmtracker.core.domain.ResourceMaterialTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply
git add core/src/commonMain/kotlin/io/kdmtracker/core/domain/Language.kt \
        core/src/commonMain/kotlin/io/kdmtracker/core/domain/LocalizedText.kt \
        core/src/commonMain/kotlin/io/kdmtracker/core/domain/ResourceMaterial.kt \
        core/src/commonTest/kotlin/io/kdmtracker/core/domain/LocalizedTextTest.kt \
        core/src/commonTest/kotlin/io/kdmtracker/core/domain/ResourceMaterialTest.kt
git commit -m "feat(core): add the language, localized text and resource material vocabulary"
```

---

### Task 2: The id types

**Files:**
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/domain/ContentId.kt`
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/domain/ContentIdTest.kt`

**Interfaces:**
- Consumes: nothing.
- Produces: `@JvmInline value class ResourceId(val value: String)`, and the sibling `LocationId`, `GearId`, `InnovationId`. All four are `@Serializable`.

- [ ] **Step 1: Write the failing test**

```kotlin
class ContentIdTest {
    @Test fun resourceIdComparesByItsSlug() {
        assertEquals(ResourceId("bone.monster"), ResourceId("bone.monster"))
        assertNotEquals(ResourceId("bone.monster"), ResourceId("hide.white_lion"))
    }

    @Test fun resourceIdSurvivesAJsonRoundTrip() {
        val json = Json.encodeToString(ResourceId.serializer(), ResourceId("hide.white_lion"))
        assertEquals("\"hide.white_lion\"", json)
        assertEquals(ResourceId("hide.white_lion"), Json.decodeFromString(ResourceId.serializer(), json))
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :core:jvmTest --tests "io.kdmtracker.core.domain.ContentIdTest"`
Expected: FAIL — unresolved reference to `ResourceId`.

- [ ] **Step 3: Implement the four value classes**

Each is a `@JvmInline value class` over `String`, annotated `@Serializable`. They are distinct types on purpose: passing a `LocationId` where a `GearId` is expected must not compile. The slug carries no entity prefix — the type is the namespace.

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :core:jvmTest --tests "io.kdmtracker.core.domain.ContentIdTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply
git add core/src/commonMain/kotlin/io/kdmtracker/core/domain/ContentId.kt \
        core/src/commonTest/kotlin/io/kdmtracker/core/domain/ContentIdTest.kt
git commit -m "feat(core): add the content id value classes"
```

---

### Task 3: Re-key `ResourceDefinition` and produce ids from the source

This is the atomic shape change: the definition's identity, both packs that produce it, and the catalog that merges it must move in one commit. Recipe and build costs stay name-keyed and are bridged here by `ContentIdentity`; S2 removes the bridge.

**Files:**
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/domain/Storage.kt` (`ResourceDefinition` only)
- Create: `core/src/commonMain/kotlin/io/kdmtracker/core/content/ContentIdentity.kt`
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/content/ContentCatalog.kt`
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/content/Core16Content.kt`
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/content/ResourceSources.kt` (call-site only)
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/content/generated/Core15Resources.kt` (regenerated — never hand-edited)
- Create: `tools/content-ingest/identity/resources.csv`
- Modify: `tools/content-ingest/ingest_resources.py`
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/content/ContentIdentityTest.kt`, plus the existing content tests named in Step 6

**Interfaces:**
- Consumes: `ResourceId`, `LocalizedText`, `ResourceMaterial` (tasks 1–2).
- Produces: `data class ResourceDefinition(val id: ResourceId, val names: LocalizedText, val category: ResourceCategory, val material: ResourceMaterial, val source: String = "")`; `object ContentIdentity { fun resourceIdFor(name: String): ResourceId?; fun requireResourceId(name: String): ResourceId }`.

- [ ] **Step 1: Author the identity CSV**

Create `tools/content-ingest/identity/resources.csv` with one row per resource name the catalog produces: the 56 names in `Core15Resources.kt` and the 4 in `Core16Content.kt`. Header and the rule:

```csv
id,spanishName,englishName,material
bone.monster,Hueso de Monstruo,,BONE
hide.white_lion,Pelaje Blanco,White Lion Fur,HIDE
```

- `id` is a **human-chosen, permanent slug** — `lower_snake_case`, dotted into `<family>.<variant>` where a variant exists. It is never derived from a display name and never changes afterwards, not even if the Spanish card name is re-translated.
- `englishName` may be empty; fill it only for the entities the settlement screens actually show.
- `material` is **optional**: leave it empty when the card's own tags resolve it (the generator derives it), and fill it only where the tags are ambiguous or outside the four primaries.

- [ ] **Step 2: Change `ResourceDefinition` and the catalog, and add the bridge**

`ResourceDefinition` loses `name` and gains `id`, `names` and `material`. `ContentCatalog`:
- `resourceDefinitions` merges with `distinctBy { it.id }` instead of `{ it.name }`;
- `resources` (the derived name list) becomes `resourceDefinitions.map { it.names.es }.sorted()`;
- `resourceGroups()`'s `ResourceSources.sourceOf(it.name)` becomes `sourceOf(it.names.es)`.

`ContentIdentity` holds the legacy bridge: `resourceIdFor(name)` returns the id whose row's `spanishName` (or, failing that, `englishName`) equals `name`, or `null`; `requireResourceId(name)` returns it or throws `IllegalStateException("no content id for '$name'")`. The table is populated from the CSV rows plus the four `Core16Content` English names.

- [ ] **Step 3: Teach the generator to emit ids and derive materials**

In `ingest_resources.py`, after parsing each card's `name`, `category` and `tags`:
- look the card up in `resources.csv` by `spanishName` and **`sys.exit` with a non-zero status naming the card when it is absent** — an unmapped card must never reach the catalog;
- derive `material` as the card's single tag among `hueso`→`BONE`, `órgano`→`ORGAN`, `piel`→`HIDE`, `restos`→`SCRAP`; when the tags yield none or several, take the CSV's `material` column; when neither resolves, **exit non-zero naming the card**;
- emit the new `ResourceDefinition(id = ResourceId("…"), names = LocalizedText(es = "…", en = …), category = …, material = ResourceMaterial.…)` shape, with the `import` lines the new types need.

Keep the existing `???`-card comment and dedup behaviour intact.

- [ ] **Step 4: Regenerate the pack and verify determinism**

```bash
python3 tools/content-ingest/ingest_resources.py \
  --pdf "external-assets/Juego Completo/Formato Americano 57x88 mm/AF Recursos"
git diff --exit-code core/src/commonMain/kotlin/io/kdmtracker/core/content/generated/
```
Expected: the first run rewrites the file; running it a second time leaves the diff clean.

- [ ] **Step 5: Update `Core16Content` and write the completeness test**

`Core16Content`'s four resources take their CSV ids, names and materials; its Innovate action's `resourceCost` stays name-keyed (the bridge resolves it).

```kotlin
class ContentIdentityTest {
    @Test fun everyCatalogResourceNameResolvesToAnId() {
        val catalog = ContentCatalog(listOf(Core16Content.pack, Legacy15Content.pack))
        catalog.resourceDefinitions.forEach { definition ->
            assertNotNull(
                ContentIdentity.resourceIdFor(definition.names.es),
                "no id for '${definition.names.es}'",
            )
        }
    }

    @Test fun theCatalogCarriesEachResourceExactlyOnce() {
        val catalog = ContentCatalog(listOf(Core16Content.pack, Legacy15Content.pack))
        assertEquals(catalog.resourceDefinitions.size, catalog.resourceDefinitions.distinctBy { it.id }.size)
    }
}
```

- [ ] **Step 6: Move the existing assertions to the new shape and run the suite**

The content tests that pin resource names move to `names.es` / `id`: `ContentCatalogTest`, `Core16ContentTest`, `Legacy15ContentTest` (its `assertEquals(56, resources.size)` now counts `id`s), `ResourceGroupsTest`, and the gear tests that resolve recipes against the pack (`Core15GearContentTest`) — those still resolve by name through `ContentIdentity`.

Run: `./gradlew :core:jvmTest`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
./gradlew spotlessApply
git add -A core/src tools/content-ingest tests 2>/dev/null
git commit -m "feat(core): key resources by a stable id with localized names and a material"
```

---

### Task 4: Re-key the resource ledger, and keep the old JSON shape decoding

The re-key and the tolerant decode are one commit on purpose: a `ResourceEntry` that requires `id` with default serialization makes every already-persisted `stateJson` fail to decode, and the mapper's `runCatching { … }.getOrElse { SettlementState() }` would silently reset the whole settlement. Shipping the two-shape serializer in the same commit closes that window; there is never a build in which old settlements lose their ledger.

**Files:**
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/domain/Storage.kt` (`ResourceEntry` + its serializer, `StorageLine`, `Storage`)
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/usecase/AddResourceEntry.kt`
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/usecase/Craft.kt`
- Test: `core/src/commonTest/kotlin/io/kdmtracker/core/domain/StorageTest.kt`, `core/src/commonTest/kotlin/io/kdmtracker/core/usecase/CraftUseCaseTest.kt`, `core/src/commonTest/kotlin/io/kdmtracker/core/usecase/AddResourceEntryTest.kt`

**Interfaces:**
- Consumes: `ResourceId`, `ResourceMaterial` (tasks 1–2), `ContentIdentity.resourceIdFor` / `requireResourceId` (task 3).
- Produces: `@Serializable data class ResourceEntry(val id: ResourceId, val material: ResourceMaterial, val delta: Int)` with a hand-written `KSerializer<ResourceEntry>` accepting both shapes; `data class StorageLine(val id: ResourceId, val material: ResourceMaterial, val quantity: Int)`; `Storage.project(entries)`, `Storage.total(entries)`, `Storage.forMaterial(entries, material)`.

- [ ] **Step 1: Write the failing tests**

```kotlin
@Test fun twoVariantsOfOneMaterialProjectIntoASingleLine() {
    val entries = listOf(
        ResourceEntry(ResourceId("hide.white_lion"), ResourceMaterial.HIDE, 1),
        ResourceEntry(ResourceId("hide.monster"), ResourceMaterial.HIDE, 2),
    )
    val lines = Storage.project(entries)
    assertEquals(1, lines.size)
    assertEquals(3, lines.single().quantity)
    assertEquals(ResourceMaterial.HIDE, lines.single().material)
}

@Test fun forMaterialReturnsOnlyThatBucketsLines() {
    val entries = listOf(
        ResourceEntry(ResourceId("bone.monster"), ResourceMaterial.BONE, 1),
        ResourceEntry(ResourceId("hide.monster"), ResourceMaterial.HIDE, 1),
    )
    assertEquals(listOf(ResourceMaterial.BONE), Storage.forMaterial(entries, ResourceMaterial.BONE).map { it.material })
}

@Test fun addingAnUnknownResourceLeavesTheTotalUnchanged() {
    val record = emptyRecord()
    val result = AddResourceEntry.run(record, ResourceEntry(ResourceId("bone.monster"), ResourceMaterial.BONE, 2))
    assertTrue(result is StepResult.Ok)
    assertEquals(2, Storage.total((result as StepResult.Ok).value.state.resourceEntries))
}

@Test fun craftingIsBlockedWhenARecipeNamesAResourceTheCatalogLacks() {
    // Reuses the record and catalog builders the file already has; `catalogMissingBone()` is a
    // catalog whose resourceDefinitions omits the recipe's "Hueso de Monstruo".
    val result = Craft.run(recordWithBuiltLocation(), "Bonesmith", "Bone Dagger", catalogMissingBone())
    assertTrue(result is StepResult.Blocked)
}

@Test fun aLegacyResourceEntryDecodesIntoAnIdKeyedOne() {
    val legacy = """{"name":"Hueso de Monstruo","category":"BASIC","delta":1}"""
    val entry = Json.decodeFromString(ResourceEntry.serializer(), legacy)
    assertEquals(ResourceId("bone.monster"), entry.id)
    assertEquals(ResourceMaterial.BONE, entry.material)
}

@Test fun anUnmappedLegacyNameSurvivesUnderASynthesizedId() {
    val legacy = """{"name":"Recurso Que No Existe","category":"BASIC","delta":3}"""
    val entry = Json.decodeFromString(ResourceEntry.serializer(), legacy)
    assertEquals(3, entry.delta)
    assertTrue(entry.id.value.startsWith("unknown."))
}

@Test fun aNewResourceEntryRoundTripsUnchanged() {
    val entry = ResourceEntry(ResourceId("bone.monster"), ResourceMaterial.BONE, 2)
    val json = Json.encodeToString(ResourceEntry.serializer(), entry)
    assertEquals(entry, Json.decodeFromString(ResourceEntry.serializer(), json))
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :core:jvmTest --tests "io.kdmtracker.core.domain.StorageTest"`
Expected: FAIL — `ResourceEntry` has no `id`/`material` parameters.

- [ ] **Step 3: Implement the re-keyed ledger**

`Storage.project` groups by `id` and carries `material` into the line; the sort becomes `compareBy({ it.material }, { it.id.value })`. `forCategory(entries, category)` is replaced by `forMaterial(entries, material)` — the category is no longer on the ledger, so no consumer can project by it. `AddResourceEntry` compares `it.id == entry.id`.

`Craft`/`Craftability` keep reading `resourceCost: Map<String, Int>` unchanged and bridge each name with `ContentIdentity.requireResourceId(name)` when building the spending `ResourceEntry`; a name with no id returns `StepResult.Blocked("unknown resource …")` rather than throwing. The material comes from the entry's definition resolved through the catalog.

- [ ] **Step 4: Implement the two-shape `ResourceEntry` serializer**

The serializer reads a JSON object: `id` present means the new shape; otherwise `name` is mapped through `ContentIdentity.resourceIdFor`, and when that returns `null` the id is synthesized as `ResourceId("unknown." + name.lowercase().replace(' ', '_'))` so the entry and its delta survive — the count is never lost. A legacy entry's material comes from the same lookup, defaulting to `ResourceMaterial.SCRAP` only on the synthesized path. Encoding always writes the new shape.

- [ ] **Step 5: Bump the transfer document to 11**

`TransferDocument.schemaVersion` becomes `11` and `SUPPORTED_VERSIONS` gains `11`. No field is removed, so older documents keep decoding by defaulted fields, and the `:data` mapper needs no change beyond recompiling — `ignoreUnknownKeys = true` with the existing `runCatching { … }.getOrElse { SettlementState() }` already tolerates both shapes. Add to `TransferDocumentTest`:

```kotlin
@Test fun aVersionTenDocumentStillDecodes() {
    val document = """{"schemaVersion":10,"exportedAt":"x","campaigns":[]}"""
    assertNotNull(Json.decodeFromString(TransferDocument.serializer(), document))
}
```

- [ ] **Step 6: Run the tests to verify they pass**

Run: `./gradlew :core:jvmTest --tests "io.kdmtracker.core.domain.StorageTest" --tests "io.kdmtracker.core.usecase.*" --tests "io.kdmtracker.core.transfer.*" && ./gradlew :data:jvmTest`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
./gradlew spotlessApply
git add core/src/commonMain/kotlin/io/kdmtracker/core/domain/Storage.kt \
        core/src/commonMain/kotlin/io/kdmtracker/core/transfer/TransferDocument.kt \
        core/src/commonMain/kotlin/io/kdmtracker/core/usecase/AddResourceEntry.kt \
        core/src/commonMain/kotlin/io/kdmtracker/core/usecase/Craft.kt \
        core/src/commonTest data/src
git commit -m "refactor(core): key the resource ledger by id and carry the old shape"
```

---

### Task 5: The settlement summary reads materials and localized names

**VISUALLY GATED.** No commit, push, PR or merge until the user approves the rendered result.

**Files:**
- Create: `app-android/src/main/kotlin/io/kdmtracker/app/LanguagePreference.kt`
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/ui/settlement/SettlementSummaryModel.kt`
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/ui/RecursosTab.kt`
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/ui/SettlementScreen.kt` (the summary call site)
- Test: `app-android/src/test/kotlin/io/kdmtracker/app/SettlementSummaryModelTest.kt`

**Interfaces:**
- Consumes: `Language`, `LocalizedText`, `ResourceMaterial`, `ResourceId` (tasks 1–2), `StorageLine` (task 4).
- Produces: `class LanguagePreference(context)` with `fun current(): Language` and `fun set(language: Language)`; `val LocalLanguage: ProvidableCompositionLocal<Language>`; `SettlementSummaryModel.cells(materials, storage, language): List<SummaryResource>`, where `SummaryResource(label: String, quantity: Int, material: ResourceMaterial, colour: ResourceColour)` replaces the current name-keyed `SummaryResource`.

- [ ] **Step 1: Write the failing test**

```kotlin
@Test fun theGridHasOneCellPerMaterialInDesignOrder() {
    val cells = SettlementSummaryModel.cells(ResourceMaterial.entries.toList(), emptyList(), Language.ES)
    assertEquals(listOf("HUESO", "ÓRGANO", "PELLEJO", "CHATARRA"), cells.map { it.label.uppercase() })
}

@Test fun aCellSumsEveryVariantOfItsMaterial() {
    val storage = listOf(
        StorageLine(ResourceId("hide.white_lion"), ResourceMaterial.HIDE, 1),
        StorageLine(ResourceId("hide.monster"), ResourceMaterial.HIDE, 2),
    )
    val cells = SettlementSummaryModel.cells(ResourceMaterial.entries.toList(), storage, Language.ES)
    assertEquals(3, cells.single { it.material == ResourceMaterial.HIDE }.quantity)
}

@Test fun aMaterialWithoutEnglishStillRendersItsSpanishLabel() {
    val cells = SettlementSummaryModel.cells(ResourceMaterial.entries.toList(), emptyList(), Language.EN)
    assertEquals("Hueso", cells.first().label)
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :app-android:testDebugUnitTest --tests "io.kdmtracker.app.SettlementSummaryModelTest"`
Expected: FAIL — `SettlementSummaryModel.cells` does not exist.

- [ ] **Step 3: Implement the preference, the local and the grid**

`LanguagePreference` mirrors `AndroidKeystoreTokenStore`: its own `SharedPreferences` file, a `KEY_LANGUAGE` string holding `Language.name`, defaulting to `ES` when unset or unparseable. `LocalLanguage` provides it to the composition.

`SettlementSummaryModel` **deletes the hardcoded `DESIGN` map and `DESIGN_ORDER`** — the grid becomes `ResourceMaterial.entries` in that order, each cell's label the material's `names.inLanguage(language)`, its quantity the sum of `storage.filter { it.material == material }`, and its colour the existing `ResourceColour` mapping keyed by material instead of by name.

- [ ] **Step 4: Update the two UI call sites**

`SettlementScreen`'s summary call passes the materials, the storage lines and the current language. `RecursosTab` adds a resource by `definition.id` and `definition.material`, and renders `definition.names.inLanguage(language)`.

- [ ] **Step 5: Run the tests and install for the visual gate**

Run: `./gradlew :app-android:testDebugUnitTest && ./gradlew :app-android:installDebug`
Expected: PASS, then the app installed.

- [ ] **Step 6: Visual acceptance — measure, present, stop**

Sign in at `ficha` / `ficha1234` against a dev-profile server (`SPRING_PROFILES_ACTIVE=dev`), open the settlement screen and leave it there. Capture a screenshot, measure the four cells' bounds and colours against the design, and present the result. **Do not commit, push, open a PR or merge — wait for the user's approval.** After approval, commit and open the PR:

```bash
./gradlew spotlessApply
git add app-android/src
git commit -m "feat(app): key the settlement summary grid by resource material"
```

---

## Final steps

- After task 5 is approved and committed, run the whole-branch review, then a single PR per task (or the chained set) per the agreed delivery strategy, squash-merging each.
- S2 (locations, gear, innovations), S3 (the full resource catalog and variants) and S4 (the language selector UI) get their own plans; the spec is their input.
