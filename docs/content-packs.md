# Content packs

How game content is modelled as data and ingested from the card PDFs. Back to the [README](../README.md).

Game content is **data, not code**. `:core` ships a modular content-catalog seam
(`io.kdmtracker.core.content`) so the rules engine stays generic while the cards,
locations, quarries, nemeses and resources a campaign can use live in selectable
**content packs**. Nothing about the settlement record is hardcoded in `rules/`.

A `ContentPack` is identified by a stable string id and carries the typed `ContentBundle`
the domain reads:

```kotlin
@Serializable
data class ContentPack(
    val id: String,                 // e.g. "core-1.6", "gorm"
    val version: String,
    val name: String,
    val requires: List<String> = emptyList(),   // pack ids that must also be enabled
    val bundle: ContentBundle = ContentBundle(),
)

@Serializable
data class ContentBundle(
    val innovations: List<Innovation> = emptyList(),        // names + consequences
    val locations: List<SettlementLocation> = emptyList(),  // names + prerequisites
    val resources: List<ResourceDefinition> = emptyList(),  // names + typed category
    val fightingArts: List<ContentEntry> = emptyList(),     // names + effect text
    val disorders: List<ContentEntry> = emptyList(),        // names + effect text
    val timeline: List<TimelineEntry> = emptyList(),
    val quarries: List<String> = emptyList(),
    val nemeses: List<String> = emptyList(),
)
```

Two pack kinds share one model:

- **Edition packs** — a base game edition (`core-1.6`), with no `requires`.
- **Expansion packs** — additions (e.g. `gorm`, `dragon-king`), declaring the packs
  they build on via `requires`.

`ContentCatalogComposer.compose(packs)` validates that every `requires` id is present
(returning `CatalogResult.MissingRequirement` otherwise) and, on success, merges the
packs into a `ContentCatalog`. The catalog exposes the **typed definitions**
(`innovationDefinitions`/`locationDefinitions`/`resourceDefinitions`/`timeline`) and
derives the display-name lists (`innovations`/`locations`/`resources`/`quarries`/
`nemeses`) as the **deduped, sorted union** across packs — so composition is
order-independent and deterministic.

```kotlin
val result = ContentCatalogComposer.compose(listOf(Core16Content.pack, gorm.pack))
// CatalogResult.Ok(catalog) — or MissingRequirement(packId, missing)
```

A campaign **selects** the packs it plays with through its `enabledPacks` list
(alongside `rulesetId` and `rulesetOverrides`), so the same settlement record model
serves any edition + expansion combination. `Core16Content.pack` is the shipped
`core-1.6` **seed** — the location tree, the initial innovation `Language`, the base
resources, and the core quarries and nemeses. It is deliberately not exhaustive: the
full 1.5/1.6 card sets arrive later through the content-ingestion pipeline, split
across edition and expansion packs.

### Content ingestion pipeline

The `core-1.5` edition pack (`Legacy15Content.pack`, id `core-1.5`) is built from the
Spanish 1.5 card PDFs by the scripts in `tools/content-ingest/`. The source PDFs live under
`external-assets/`, which is **git-ignored and never committed**; each `ingest_*.py` script
parses its PDFs with `pdftotext` and writes **committed** Kotlin sources under
`core/src/commonMain/kotlin/io/kdmtracker/core/content/generated/`. The generator is the
source of truth — the generated files are never edited by hand — and **the app never reads
a PDF at runtime**; it only reads the committed Kotlin.

| Generated file | Script | Content |
| --- | --- | --- |
| `Core15Innovations.kt` | `ingest_innovations.py` | 42 innovations + consequence graph |
| `Core15Resources.kt` | `ingest_resources.py` | 56 resources + typed category |
| `Core15FightingArts.kt` | `ingest_fighting_arts.py` | 24 fighting arts + 23 disorders |
| `Core15Locations.kt` | `ingest_locations.py` | settlement locations: build cost + actions |
| `Core15Gear.kt` | `ingest_gear.py` | gear definitions + crafting recipes (via `pdftotext -bbox-layout`) |

Output is **deterministic**: entries and every nested list are sorted, and no timestamps or
paths are embedded, so re-running a script over unchanged inputs is **byte-identical**.
See [`tools/content-ingest/README.md`](../tools/content-ingest/README.md) for the exact
re-run commands, the parsing model, and the known gaps (image-only card decks, the
secret-arts deck, dropped cost-icon glyphs).
