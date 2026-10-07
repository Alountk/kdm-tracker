# Settlement Steps 1 & 7 — Design

**Status:** Draft (awaiting review)
**Slice:** First interactive Settlement Steps in the Compose app — Step 1 (Set Up the Settlement) and Step 7 (Develop) — backed by a faithful, content-driven rules foundation in `:core`.
**Depends on:** PR #7 (settlement domain + content packs), PR #8 (1.5 content ingestion), PR #9 (settlement record persistence).

## Goal

Replace two of the app's `Pendiente` step placeholders with real screens, and build the missing `:core` rules layer those screens need — **following the actual KDM rules** (costs and limits come from the cards/rulebook, not from invented numbers).

Success is:
1. Step 1 renders the settlement's setup truthfully (derived Innovation Deck, built locations, principles, timeline).
2. Step 7 lets the player **Innovate**, **Build**, and take **generic endeavor actions** with the **real costs and per-phase limits**, mutating the persisted `SettlementRecord`.
3. The costs are **extracted from the source material** and pinned by tests — not hardcoded.
4. All rules live in `:core` as pure, tested use-cases; the UI decides nothing.

## Scope

### In
- `:core` content model + rules use-cases for the Step 3 endeavor income and the Step 7 actions.
- A content-extraction step that reads the base **settlement location cards** and produces typed content (build costs + location actions), pinned by fixture tests (same philosophy as the PR #8 pipeline).
- Two Compose screens: `SetUpStepScreen` (Step 1) and `DevelopStepScreen` (Step 7).
- App wiring: compose the `ContentCatalog` from `Campaign.enabledPacks`; expose the record + catalog to the UI; controller/ViewModel actions.

### Out (deferred, explicitly)
- **Craft / gear** — needs a gear + recipe model we do not have.
- **Executable effects** — endeavor/innovation/location effects are shown as **text** and resolved by the player; we do not implement a rules interpreter.
- **Full `Survivor` model** — no per-survivor stats/injuries/gear grids. Returnees and deaths are **settlement-level counters**.
- **1.6-specific rules** — the local rulebook is 1.5 and the 1.6 sheet is image-only; the 1.6 diff is a known gap.
- **Image-only card content** — only text-extractable sources this slice.
- Steps 2–6 and 8–11 keep their `Pendiente` placeholder.

## Rules model (grounded)

Every rule below is sourced; numbers are never invented.

| Rule | Value | Source |
| --- | --- | --- |
| Endeavor income (Step 3) | **+1 per returning survivor** | 1.5 rulebook (ES) pp.82/233; `lantern-year-steps.md` §2 |
| Income bonuses (named) | Cocinar +1/phase; Esfuerzo Colectivo +1 per 10 population; Sepulturas +1 per settlement-phase death | Innovation cards `AF Innovaciones.pdf` |
| Innovate | Only at **Lantern Hoard**; **1 endeavor + 1 Bone + 1 Organ + 1 Hide**; **once per Settlement Phase**; **draw 2, keep 1** | Location card `AF Asentamientos Loc.pdf`; rulebook pp.34–35, 84–85 |
| Innovate — deck | When an innovation is gained, shuffle its **consequence** cards into the Innovation Deck; update Survival Limit | rulebook p.84; research md §2 |
| Build | Cost = **resources + endeavor(s)**, printed **on the location card** (e.g. Armería = 3 Bone + 1 Hide; Círculo de Piedra = 3 Organ + 1 Hide) | Location cards; rulebook pp.83, 85 |
| Build prerequisite | Location must be enabled first (tech tree) | 1.6 sheet (visual); `Core16Content` tree |
| Generic endeavor action | A rule carrying the `[endeavor]` symbol costs **1 endeavor** besides its listed cost; nominate one survivor; **−1 endeavor**; effect applies to that survivor unless stated; effect text is on the card | rulebook p.83 |
| Storage | Resources may be spent at any time in the phase, but **must exist** at spend time | rulebook p.83 |
| Step 11 | Unspent endeavors are **discarded**, not carried over | rulebook p.83; research md §2 |

**Fidelity boundary:** we enforce **costs, prerequisites, storage, and per-phase limits**. We **display** effects as text; the group resolves them. Craft/gear is out.

## Architecture

### 1. Content model extension (`:core`, pure)

Current: `SettlementLocation(name, requires: List<String>)` — no costs, no actions.

Extend (all additive, defaulted):
- `SettlementLocation.buildEndeavors: Int = 0`
- `SettlementLocation.buildCost: Map<String, Int> = emptyMap()` — resource name → count.
- `SettlementLocation.actions: List<SettlementAction> = emptyList()`

New types:
- `SettlementAction(name, kind, endeavorCost, resourceCost: Map<String,Int>, oncePerPhase, effect: String)` with `kind: ActionKind` (`ENDEAVOR`, `INNOVATE`). `INNOVATE` also carries `draw = 2`, `keep = 1` and is `oncePerPhase = true`.
- Resource quantities are by **name**; the category is resolved from the `ContentCatalog` when an entry is written to the ledger.

**Content extraction.** A tool (Python, in the spirit of `tools/content-ingest`) reads `AF Asentamientos Loc.pdf` and emits a generated Kotlin pack with each location's `buildEndeavors`/`buildCost`/`actions`, plus a fixture test pinning representative costs (Armería, Círculo de Piedra, Lantern Hoard Innovate). Deterministic output, no timestamps/paths.

### 2. `:core` use-cases (pure, TDD)

A typed result so rules never throw:
```kotlin
sealed interface StepResult<out T> {
    data class Ok<T>(val value: T) : StepResult<T>
    data class Blocked(val reason: String) : StepResult<Nothing>
}
```
Use-cases over `SettlementRecord`:
- `GainEndeavors(record, returnedSurvivors, ruleset)` — adds the Step 3 base (+ named bonuses when their source is modeled) to the pool.
- `Innovate(record, locationName, innovationName, catalog)` — validates the location grants Innovate, the once-per-phase limit, storage has the resources, and endeavors ≥ 1; spends (ledger + endeavor), marks the phase limit, adds the chosen innovation.
- `BuildLocation(record, locationName, catalog)` — validates not already built, `requires` satisfied, storage has the cost, endeavors ≥ `buildEndeavors`; spends and adds to `LocationSet`.
- `SpendEndeavor(record)` — generic: validates ≥ 1, decrements by 1 (effect resolved by the player).
- `AddResourceEntry(record, entry)` — appends to the ledger; a spend (`delta < 0`) is blocked when it would take the resource below 0.

Per-phase state: `SettlementState` gains a small per-phase marker (e.g. used once-per-phase actions) that resets at the year boundary, so **Innovate once per phase** is enforceable. Exact shape is the plan's call.

### 3. App wiring (`:app-android`)

- A controller/repository composes the `ContentCatalog` from `Campaign.enabledPacks` (via `ContentCatalogComposer`), once per load.
- `UiState` gains the `SettlementRecord` and the catalog data the screens need (built locations, available innovations/locations, storage lines, endeavor breakdown).
- `SettlementScreen` dispatches by step number: `1 → SetUpStepScreen`, `7 → DevelopStepScreen`, else `PendientePlaceholder`.
- Controller methods (`innovate`, `build`, `spendEndeavor`, `addResource`, `gainEndeavors`) call the `:core` use-cases and republish; the UI renders `Blocked(reason)` inline.

### UI

- **Set Up (Step 1)** — read-only: derived **Innovation Deck** (consequences of unlocked innovations), built **locations**, **principles**, the year's **timeline/event**; a single "confirm setup → advance" action. Consumes content; mutates nothing.
- **Develop (Step 7)** — three affordances with **real costs**: Innovate (Lantern Hoard, cost + once-per-phase), Build (list of buildable locations with cost + prerequisites), generic Endeavor (spend 1, pick a survivor target conceptually, effect text shown). Storage/endeavor display with the current totals.

## Data flow

`Campaign.enabledPacks` → `ContentCatalogComposer.compose` → `ContentCatalog`.
`SettlementRecord` (persisted, sync-backed) ↔ `:core` use-cases ↔ controller ↔ `UiState` → screens.
Every Step 7 action persists through `SettlementRepository.update(record)` (local store + outbox), same as existing mutations.

## Error handling

Rules are **total**: invalid actions return `Blocked(reason)` (insufficient storage, prerequisite missing, once-per-phase already used, endeavor exhausted) — never an exception. The UI shows the reason inline and leaves the record unchanged.

## Testing

- **`:core`** — strict TDD per use-case: innovate cost/limit/deck, build prerequisite + cost, storage non-negativity, endeavor income; a `Blocked` case per rule.
- **Extraction** — fixture tests pin the generated costs against the source cards (deterministic emit).
- **`:app-android`** — controller/ViewModel JVM tests over `InMemoryCampaignStore` + a composed catalog. No Compose UI tests this slice.
- Full suite stays green: `./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck`.

## Risks

- **Extraction accuracy** — card icon costs may not extract cleanly; mitigated by fixture tests and honest `[gap]` markers.
- **1.5 vs 1.6** — local rulebook is 1.5; the 1.6 sheet is image-only, so the 1.6 branch of some rules is unverifiable now.
- **Per-phase reset semantics** — advancing a year must clear the once-per-phase markers; needs an explicit rule + test.
- **Scope** — this is a large first slice (content + rules + 2 screens); the implementation plan splits it into work units.

## References

- `external-assets/KDM 1.5 EdESP Reglamento - Digital.pdf` (pp.34–35, 82–85)
- `external-assets/Hoja Asentamiento KDM 1.5 A4.pdf` (1.5 sheet, text)
- `external-assets/asentamiento_v1.6.pdf` (1.6 sheet, image-only)
- `external-assets/Juego Completo/Formato XXL 102x204 mm/AF Asentamientos Loc.pdf`, `.../AF Innovaciones.pdf`, `.../AF Eventos.pdf`, `.../AF Equipo/*.pdf`
- `server/src/main/resources/static/prototypes/research/lantern-year-steps.md`
- Existing slices: `docs/superpowers/plans/2026-10-06-settlement-domain.md`, `2026-10-06-content-ingestion.md`, `2026-10-06-settlement-record-persistence.md`
