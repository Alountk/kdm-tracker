# Content Identity, Naming and Variants — Design

**Status:** Draft (awaiting review)
**Depends on:** nothing merged. Branches from `main` (`1828237`). Its UI step (S1's label resolution) touches files the unmerged Settlement slice (Slice 6) also changes — `SettlementSummaryModel.kt`, `SettlementScreen.kt`, `RecursosTab.kt` — so the two are sequenced at apply time.
**Shape:** a program of four implementation slices, not one slice. Part B of the "apply the design to the screens" effort; part A (the Develop step's three flows) depends on it.

## Goal

Every content entity in the repo is identified by its **display-name string**. The same entity therefore exists once per language as a distinct entry, and renaming a string silently orphans persisted data. This program gives every content entity a **stable, language-neutral id**, a **localized-name layer** resolved at render time by the user's language, and an explicit **material** dimension so monster resources aggregate into the settlement summary's four buckets.

The design's regenerated "Fase de Asentamiento" screen (`59fdd89d`) is the occasion: it renders every content name in **two languages at once** (`Daga de Hueso (Bone Dagger)`, `Taller de Huesos (Bonesmith)`, `Pintura (Paint)`) and puts a **base material** in parentheses next to monster resources (`Melena Brillante (Pellejo)`, `Garras de León (Hueso/Arma)`). Neither concept exists in the repo.

## What the repo does today (evidence)

- **Identity is the name string, everywhere.** `Storage.project` groups by `(name, category)` (`core/.../domain/Storage.kt:63`); `ContentCatalog.mergeDefinition` merges with `distinctBy { it.name }` (`core/.../content/ContentCatalog.kt:73`); locations merge with `groupBy { it.name }` (`:112`) and their actions/recipes dedupe by `name`/`gearName` (`:125–126`); `LocationSet.built` is `List<String>` of names (`core/.../domain/SettlementLocation.kt:34–51`); `InnovationCollection` dedupes and resolves by name (`core/.../domain/Innovation.kt:37,54`); `AddResourceEntry` compares `it.name == entry.name` (`core/.../usecase/AddResourceEntry.kt:27`); `Craft` builds `ResourceEntry(name, category, -count)` from a name→category map (`core/.../usecase/Craft.kt:40–44`).
- **No content entity has an id.** `Gear`, `GearRecipe`, `SettlementLocation`, `SettlementAction`, `Innovation`, `ContentEntry`, `HuntEvent`, `SettlementEvent`, `MonsterCard` are all keyed by a name string. Only `Settlement`, `Campaign` and `Survivor` carry a real id.
- **Three vocabularies coexist.** `Core16Content` declares 4 English resources (`Bone`, `Hide`, `Organ`, `Scrap`) and the Innovate cost `mapOf("Bone" to 1, "Organ" to 1, "Hide" to 1)` (`content/Core16Content.kt:48,75–81`); `Core15Resources` declares **56 Spanish** ones, generated from PDFs (`content/generated/Core15Resources.kt`); the server declares a **third** English set (`Bone`, `Hide`, `Sinew`, `Amber`, `Cloth`, `Iron`, `Pigment`, `Scrap Iron` — `server/.../seed/DatabaseSeeder.kt:28–35`). Both client packs are loaded together (`app-android/.../SettlementRepository.kt:94,205,318`) and both `"Language"` and `"Lenguaje"` are seeded at once (`:355–361`), so the same entity appears twice with split counts.
- **`Core15Resources` is generated and emits no id.** `tools/content-ingest/ingest_resources.py` parses the **untracked Spanish** PDFs under `external-assets/**` with `pdftotext` and writes the committed Kotlin; its resource emitter writes **only `name` + `category`** (`:194–203`). Re-running is byte-identical (`git diff --exit-code`), verified with poppler 26.04.0. `Core16Content` has **no generator** — it is hand-maintained.
- **There is no i18n layer at all.** No `composeResources`, no `Res.string`, no `strings.xml`, no `values-es/`. The only `Locale` use is date formatting (`AccountScreen.kt:25,116`). All user-facing copy is a hardcoded Kotlin literal.
- **The UI shows raw domain strings.** The summary's resource grid keys off a **hardcoded 4-entry map of domain names** and falls back to `definition.name` (`app-android/.../ui/settlement/SettlementSummaryModel.kt:40,43–49,70,73`); the Recursos tab renders `definition.name` verbatim (`RecursosTab.kt:64–65`); the Locations panel renders `location.name`, `requires.joinToString()` and `actionLabel ?: "Construir"` (`LocationsModel.kt:31–37`, `DevelopPanels.kt:182,193,206`).
- **Persistence is one JSON blob, versioned twice.** `settlement.stateJson TEXT NOT NULL DEFAULT '{}'` holds the whole `SettlementState` (SQLDelight `Settlement.sq:16`; migrations `1.sqm`–`4.sqm` cover other tables only). The mapper encodes/decodes it with `Json { ignoreUnknownKeys = true }` and a `runCatching { … }.getOrElse { SettlementState() }` fallback (`SqlDelightCampaignStore.kt:128,148–153,345`). `TransferDocument.schemaVersion` is **10** with `SUPPORTED_VERSIONS = 1..10` (`core/.../transfer/TransferDocument.kt:30,36`).
- **There is no app-preference store**, but there is a precedent: `AndroidKeystoreTokenStore` uses plain `SharedPreferences` in a private file (`app-android/.../sync/TokenStore.kt:39–40,87`), with no extra dependency in the version catalog.

## Decisions (validated with the user)

1. **The language is a runtime selector**, not a permanent two-language annotation. Spanish is the default; English is selectable. The design's `(Bone Dagger)` is the mock's styling, not the shipped layout — the UI shows one language at a time.
2. **Spanish ships complete; English grows incrementally.** Authorship of the English names starts with the entities the screens actually show (roughly 40–60 official names across resources, gear and locations) and the catalog grows afterwards. When the selected language has no entry, `LocalizedText` **falls back to Spanish**.
3. **The material dimension is explicit and separate from the rules category** (see §3).

## Design

### 1 · Identity

A `value class` per entity, wrapping a **human-chosen slug** — never derived from a display name:

```kotlin
@JvmInline value class ResourceId(val value: String)    // "bone.monster", "hide.white_lion"
@JvmInline value class LocationId(val value: String)    // "bone_smith"
@JvmInline value class GearId(val value: String)        // "bone_dagger"
@JvmInline value class InnovationId(val value: String)  // "language"
```

The **type** supplies the namespace, so the slug carries none. Passing a `LocationId` where a `GearId` is expected does not compile. Slugs are chosen once and are stable across renames and translations.

### 2 · Names

```kotlin
enum class Language { ES, EN }   // ES default

@Serializable
data class LocalizedText(val es: String, val en: String? = null) {
    fun inLanguage(language: Language): String = when (language) {
        Language.ES -> es
        Language.EN -> en ?: es          // falls back rather than rendering an id
    }
}
```

Two fields, not a `Map`: the project has two languages, and the `en → es` fallback is explicit and testable. A third language would introduce the map then.

### 3 · The two resource dimensions

The design renders a monster resource as `Melena Brillante (Pellejo)`. `Pellejo` is not `ResourceCategory.MONSTER` — it is the **bucket the settlement summary aggregates into**. The existing enum is a rules dimension; the new one is an aggregate dimension. Both are needed:

```kotlin
enum class ResourceMaterial { BONE, ORGAN, HIDE, SCRAP }   // NEW — the summary's four buckets
enum class ResourceCategory { BASIC, SPECIAL, MONSTER }    // EXISTING — the rules dimension

@Serializable
data class ResourceDefinition(
    val id: ResourceId,
    val names: LocalizedText,
    val category: ResourceCategory,
    val materials: Set<ResourceMaterial> = emptySet(),   // ← new
    val source: String = "",
)
```

- The **summary aggregates by material** (four cells, four fixed buckets), counting a resource only when it commits to **exactly one**.
- **Recipes reference the exact `id`** (`hide.white_lion`, not `hide`).
- **`materials` is a set, and that is not a flourish — it is the card.** The `???` card in the Básicos deck is a **wildcard**: it can pay a bone, a hide *or* an organ, so it carries all three. A set with one element is the ordinary resource; an empty set is a herb, a bar of iron or a skull, which feeds none of the four cells; a set with several is a wildcard, and while it sits in storage it is **not committed to any bucket**, so the grid does not count it. Counting it in three cells at once would make the four totals add up to more stock than the settlement holds.
- The CSV's material cell therefore reads: empty = not decided yet, `none` = deliberately no bucket, `BONE|HIDE|ORGAN` = that set. Only a card whose tags leave it ambiguous must be decided by hand.

- **Variants need no extra field.** `hide.white_lion` and `hide.antelope` are two ids that share `material = HIDE`; that is enough to aggregate them and to reference them exactly. An explicit `variantOf` is deferred (§Out) until substitution between variants is a real requirement — YAGNI now.

### 4 · Where the identity flows

The ledger keys resources by id and **stores nothing but the id and the delta**:

| Today | New |
| --- | --- |
| `ResourceEntry(name, category, delta)` | `ResourceEntry(id, delta)` |
| `StorageLine(name, category, quantity)` | `StorageLine(id, quantity)` |
| `Storage.project` groups by `(name, category)` | groups by `id` |
| `AddResourceEntry` compares `it.name == entry.name` | compares `id` |
| `Craft` resolves `category` by name | resolves the definition (and so the id) by name, for now |

**Names and material are both content and both leave the ledger.** The UI resolves `id → names` from `resourceDefinitions`, and the summary resolves `id → material` through `Storage.byMaterial(entries, definitions)`. The projection itself stays catalog-free, so `SettlementState.storage()` keeps working without one; only the aggregation by bucket needs the catalog, and only the UI and the model that renders the grid ever call it.

This is deliberately **not** the denormalization the ledger used to do with `category`, and the reason is the migration: an entry persisted before ids existed carries only a display name, and while [ContentIdentity] can map that name to an id, *nothing* can recover which of the four buckets the resource belonged to. Denormalizing the material would have made every legacy entry's bucket permanently `null` — silently dropping it from the grid. Storing only the id keeps the legacy decode lossless and lets the catalog answer the material question on every read.

`Storage.forCategory` had no production consumer and is gone; `Storage.forMaterial` is replaced by the catalog-aware `byMaterial`.

**S1 changes `resourceEntries` only.** `LocationSet.built`, `SettlementState.gear` and `InnovationCollection` keep their current shape until S2, so the diff — and the review — stay small.

### 5 · Migration and compatibility

- The state lives in **one JSON column**, so **no SQLDelight migration is required** — only tolerance for the old JSON shape.
- `ResourceEntry` gains a **custom serializer accepting both shapes**: an element carrying `id` is new; one carrying only `name` is legacy and is mapped `name → id` through the identity table. Both shapes are covered by tests.
- `TransferDocument.schemaVersion` rises to **11**, and `SUPPORTED_VERSIONS` gains it. Older documents keep decoding by defaulted fields.
- **Hard rule:** the identity table must cover **every** name the **client's two packs** can produce — the 56 generated Spanish resources and the 4 English seeds. A test asserts *every catalog resource name resolves to an id*, so nothing is orphaned silently. A name with no mapping **fails the test**, not the user. The server's separate vocabulary never reaches the client's catalog (it owns its own `Resource` entity and the client only ever ships it a `SettlementState` blob), so it is not part of this table.

### 6 · The language at runtime

- `Language` is persisted in its own `SharedPreferences` file following the `TokenStore` pattern — **no new dependency**.
- It is provided to the UI through a `CompositionLocal`; models resolve `names.inLanguage(current)`.
- The hardcoded 4-entry `DESIGN` map and `DESIGN_ORDER` in `SettlementSummaryModel` are **deleted**: the grid becomes `ResourceMaterial.entries` in design order, with the material's own `LocalizedText` (`Hueso`/`Bone`, …) as the label and its colour.
- Changing the language touches **no domain data and no persistence** — the identity is neutral. That is precisely what this design buys.

### 7 · Data: who owns what

The PDFs are Spanish and carry no ids. Responsibilities split:

- **The PDFs remain the content authority** — which cards exist, their category, and **their material tags**. The parser already extracts each card's type line as a validated tuple of tags (`ingest_resources.py:93,147`; vocabulary in `KNOWN_TAGS:69`) and its own docstring records that `hueso`/`órgano`/`piel`/`restos` appear on both BASIC and MONSTER cards — which is exactly why the *deck* had to decide the category while the *tags* can decide the material.
- **A versioned CSV becomes the identity-and-naming authority** — `tools/content-ingest/identity/resources.csv`:

```csv
id,spanishName,englishName,material
bone.monster,Hueso de Monstruo,,BONE
hide.white_lion,Pelaje Blanco,White Lion Fur,HIDE
```

- `material` is **derived from the card's tags**, not hand-authored: it is the card's tag among the four primaries (`hueso`→`BONE`, `órgano`→`ORGAN`, `piel`→`HIDE`, `restos`→`SCRAP`). The rules are exact and all three are needed: **exactly one** primary tag yields that material; **none** yields `null` (a legitimate "feeds no bucket", and no CSV entry is required); **several** is ambiguous, so the CSV's `material` column must decide, and the generator **fails loudly when it is empty**.
- The CSV deliberately **does not carry `category`**. The deck a card ships in decides its category (`ingest_resources.py:57–64`), so duplicating it in the CSV would create two authorities for one field.
- `ingest_resources.py` reads the PDF, looks each card up in the CSV and **fails loudly on an unmapped card**, so no id can drift into existence by accident and the generator stays deterministic (the same `git diff --exit-code` gate).
- The `englishName` column may be **empty** and grows incrementally — decision 2.
- Sibling CSVs cover `locations.csv`, `gear.csv` and `innovations.csv` when S2/S3 reach them.

## Scope

### In

- `:core`: `ContentId` value classes, `LocalizedText`, `Language`, `ResourceMaterial`; `ResourceDefinition`, `ResourceEntry` and `StorageLine` re-keyed; the two-shape serializer; the identity table and its completeness test; `TransferDocument` v11.
- `:data`: the mapper's decode path for the legacy JSON shape.
- `tools/content-ingest`: the CSV, the lookup, the loud failure, and the resource emitter's new fields.
- `:app-android`: the language preference and its `CompositionLocal`; the summary grid and the Recursos tab resolving localized names; deletion of the hardcoded `DESIGN` map.

### Out

- **The server** and its third vocabulary. Its own `Resource` entity is untouched, and the sync boundary carries the client's `SettlementState` unchanged (it now holds ids, which the server stores opaquely). Aligning the server is its own change.
- An explicit `variantOf` field (§3).
- `principles` (`Map<PrincipleCategory, String>`) — a different model that shares none of this identity.
- The identity of locations, gear and innovations beyond what S2 carries.
- Any UI restyling — part A owns the Develop step's flows.

## Architecture

- `core/.../domain/` — new `ContentId.kt` (the four value classes), `LocalizedText.kt`, `Language.kt`, `ResourceMaterial.kt`; `Storage.kt`, `ResourceCategory.kt` touched. `ResourceMaterial` is an enum whose four members each carry their own `LocalizedText` (`Hueso`/`Bone`, `Órgano`/`Organ`, `Pellejo`/`Hide`, `Chatarra`/`Scrap`) — the summary's labels are the enum's, not a UI map.
- `core/.../content/` — `ContentCatalog.kt` merges by id; `Core16Content.kt` and `generated/Core15Resources.kt` carry ids; new `ContentIdentity.kt` (the name→id table).
- `core/.../transfer/TransferDocument.kt` — v11 and `SUPPORTED_VERSIONS`; the `ResourceEntry` two-shape serializer lives beside `ResourceEntry` in `Storage.kt`.
- `data/.../SqlDelightCampaignStore.kt` — the legacy decode path.
- `tools/content-ingest/identity/*.csv`, `ingest_resources.py`.
- `app-android/.../LanguagePreference.kt` (new), `ui/settlement/SettlementSummaryModel.kt`, `ui/RecursosTab.kt`.

## Data

| Design element | Source |
| --- | --- |
| the summary's four cells | `ResourceMaterial.entries` in design order, each with its own `LocalizedText` and colour |
| a cell's count | the ledger projected by `material` — the aggregate, which is what stops monster resources splitting |
| a resource's row name | `ResourceDefinition.names.inLanguage(current)` |
| a recipe's cost | `Map<ResourceId, Int>`, resolved to names at render time |
| the selected language | the `SharedPreferences` preference, defaulting to `ES` |

## Slices

The plan cuts the program into four; each is reviewable on its own.

| Slice | What lands | What becomes visible |
| --- | --- | --- |
| **S1** | The mechanism plus **resources end to end**: the id and name types, `ResourceMaterial`, the ledger by id, the migration, transfer v11, the summary and Recursos tab resolving real names | The grid aggregates correctly and stops double-counting |
| **S2** | The same mechanism for **locations, gear and innovations** | The panels lose their duplicate names |
| **S3** | The **real resource catalog**: all 56 with ids and materials through the CSV, plus variants | Data, not structure |
| **S4** | The **language selector** and the growth of the English catalog | The selector working |

## Testing

- **`:core`**: `LocalizedText` falls back `EN → ES`; the identity table covers every catalog name (the completeness assertion); `Storage.project` aggregates two variants of the same material into one line; `AddResourceEntry` refuses by id; the two-shape `ResourceEntry` serializer round-trips both a legacy `{name,category,delta}` and a new `{id,material,delta}`; `TransferDocument` v11 is accepted and v10 still decodes.
- **`:data`**: a settlement persisted with the legacy JSON shape decodes into id-keyed entries with no data loss; unknown keys are still ignored.
- **`tools/content-ingest`**: a card's material is derived from its tags; a card whose tags resolve to none and that the CSV does not cover **fails the ingest**; an unmapped card fails it too; and re-running over unchanged inputs is byte-identical.
- **`:app-android`**: the summary grid renders four cells keyed by material with the localized label; the Recursos tab shows the localized name; switching the language changes the label without touching the record.
- **Gate**: the full suite plus `spotlessCheck`.

## Risks

- **Re-keying is a data-shape change.** Old rows decode through a mapping that must be complete; an unmapped name is data loss. The completeness test is the mitigation and is not optional.
- **`ContentCatalog` identity changes ripple** into every merge and lookup (`resources`, `gearModifiersFor`, `resourceGroups`, `huntEventsFor`). S1 touches resources only, which keeps the blast radius bounded.
- **The generated file is the source of truth.** Adding a column to the emitter without regenerating would leave the checked-in Kotlin and the generator disagreeing, breaking the `git diff --exit-code` guarantee. The regeneration belongs in the same task as the emitter change.
- **The English catalog is authored, not sourced.** A wrong English name is a content defect, not a bug that a test catches. It is limited to what the screens show, and each authored name is reviewable in the diff.
- **Sequencing with the unmerged Slice 6.** Both change `SettlementSummaryModel.kt` and `RecursosTab.kt`; whichever lands second rebases. S1's `:core` work is independent of it and can proceed first.

## References

- Stitch screen `59fdd89d6100424eb9b5bbf1a6e74ffa` ("Fase de Asentamiento", regenerated 2026-10-10) and the reference images `AF Asentamientos Loc-*`.
- `core/.../domain/{Storage,ResourceCategory,SettlementState,SettlementLocation,Gear,GearRecipe,Innovation,SettlementAction}.kt`
- `core/.../content/{ContentCatalog,Core16Content,Legacy15Content}.kt`, `content/generated/Core15Resources.kt`
- `core/.../transfer/TransferDocument.kt`; `data/.../SqlDelightCampaignStore.kt`, `data/.../db/Settlement.sq`
- `tools/content-ingest/README.md` and `ingest_resources.py`
- `app-android/.../ui/settlement/SettlementSummaryModel.kt`, `ui/RecursosTab.kt`, `sync/TokenStore.kt`
- `external-assets/Juego Completo/Formato XXL 102x204 mm/AF Asentamientos Loc.pdf` (the locations source the reference images came from)
