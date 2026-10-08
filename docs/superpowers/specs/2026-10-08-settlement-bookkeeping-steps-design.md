# Settlement Bookkeeping Steps (4, 5, 6, 10, 11) — Design

**Status:** Draft (awaiting review)
**Slice:** Turn the Settlement Phase steps that still render `Pendiente` — **4 Update Timeline, 5 Update the Death Count, 6 Check Milestones, 10 Record & Archive Resources, 11 End Settlement Phase** — into real, usable bodies wired to the `SettlementRecord` / `Ruleset` / `ContentCatalog` and the redesigned cockpit. Also fix the missing **lantern-year increment**.
**Depends on:** PR #7–#14 (domain backbone, content packs, persistence v4/v5, Steps 1 & 7, UI redesign).

## Goal

Steps 2–6 and 8–11 currently show the shared `Pendiente` placeholder, so a Settlement Phase cannot actually be played through. This slice implements the five **bookkeeping / branching** steps (the ones whose data the domain already owns or can own with a small, additive model), leaving the survivor-driven steps (2, 8) and the Special Showdown loop (9) to later slices.

Success is:
1. A Settlement Phase can be walked 1 → 11 recording: the drawn **settlement event**, the **timeline** of the year, **deaths/births**, the chosen **principles**, the **milestones** resolved, the **storage** review, and the final **endeavor discard**.
2. Advancing past Step 4 **increments the lantern year** exactly once per phase.
3. Every rule stays in `:core`; the screens render state and report intent through the controller.
4. The record survives a reload (persisted in `SettlementState`) and round-trips through the transfer document.

## Design (validated with the user)

The app is a **tracker, not a rules engine** (per the prior-art research in `lantern-year-steps.md` §5): auto-derive what is safe (year advance, milestone detection, endeavor discard), take **explicit inputs** for decisions (principles), and **record** what content does not model.

### Step 4 — Update Timeline
- Shows the current lantern year and this year's timeline entries.
- **Settlement event:** a picker over the ingested **Settlement Event deck**; the chosen event is recorded on `Settlement.settlementEvent` and its full text is shown. A **Draw** affordance picks a random event as a convenience (the physical draw remains authoritative; the recorded name is what persists).
- Story events for the year (`ContentCatalog.timeline` + the settlement's own event) are listed read-only.
- Leaving Step 4 for Step 5 applies **`AdvanceTimeline`**: `lanternYear += 1`, guarded by a phase marker so revisiting Step 4 cannot double-increment.

### Step 5 — Update the Death Count
- An input records **deaths** → `RecordDeaths`: `state.deaths += n`, `settlement.population -= n` (floored at 0).
- When `deaths` first becomes `>= 1`, the **`Principle: Death`** choices (from `Core16Ruleset.principles[DEATH]`) are surfaced; the choice is persisted.
- **Explicit deviation:** this screen also records **births** (`RecordBirths`: `state.births += n`, `population += n`). Births have no other home, and the `Principle: New Life` milestone reads the `births` counter.

### Step 6 — Check Milestones
- Lists the ruleset's milestones with their **reached** state (`record.reachedMilestones(ruleset)`) and **resolved** state.
- Principle milestones (`Principle: Death`, `Principle: New Life`, `Principle: Society`) expose their choices; choosing one persists it via `ChoosePrinciple` and marks the milestone resolved via `ResolveMilestone`.
- `Hooded Knight` and `Game Over` are informational (reached/not); resolving them just ticks them.

### Step 10 — Record & Archive Resources
- Read-only projection of the settlement's storage (`record.storage()`); the ledger is already persisted by Step 7's `AddResourceEntry`. A short note frames it as the archive/reconciliation checkpoint.

### Step 11 — End Settlement Phase
- A **gated commit**: shows "N unspent endeavors will be discarded" plus a summary. The existing `FlowTransition.Commit` (Step 11 → Hunt/Showdown) already zeroes `endeavors`, so the body only warns; no new rule.

## Scope

### In
- **Content:** ingest `AF Eventos.pdf` (23 Settlement Event cards) into a committed `Core15SettlementEvents.kt`, exposed through the existing pack/catalog pipeline.
- **`:core`:** additive `SettlementState` fields (`principles`, `resolvedMilestones`) and use cases (`AdvanceTimeline`, `RecordDeaths`, `RecordBirths`, `ChoosePrinciple`, `ResolveMilestone`, `RecordSettlementEvent`); transfer/serialization round-trip.
- **`:app-android`:** five new step-body composables + `UiState` derived views + controller methods; wiring into `SettlementScreen`.

### Out (deferred, explicitly)
- Steps **2 (Survivors Return)** and **8 (Prepare Departing Survivors)** — blocked on the missing `Survivor` model (next slice).
- Step **9 (Special Showdown)** loop back to Step 5 — its own slice.
- **Hunt** / **Showdown** phase bodies.
- Campaign-wide exclusion of already-drawn settlement events (the rule forbids a repeat only **within one phase**); the picker shows the full deck.
- Any rule automation of event/story-event *effects* (text is displayed, not evaluated).

## Architecture

### Content (`tools/content-ingest/` + `:core` generated)
- New domain type `SettlementEvent(name: String, text: String)` (`:core`, `@Serializable`).
- `ContentBundle.settlementEvents: List<SettlementEvent>` (defaulted, additive).
- `ContentCatalog.settlementEventDefinitions: List<SettlementEvent>` (merged by name) and the derived `settlementEvents: List<String>` names.
- `Legacy15Content` includes the generated bundle.
- `ingest_settlement_events.py`: one card per PDF page (p1 is the cover); title = first non-blank line, body = the remaining lines; deterministic Kotlin emit via the shared `kotlin_emit`/`pdf_text` helpers. Fixture tests pin the page count and a sample card; the existing integrity tests (non-empty, unique names) are extended.

### `:core` (pure, additive)
- `SettlementState.principles: Map<PrincipleCategory, String> = emptyMap()` — the chosen principle label per category.
- `SettlementState.resolvedMilestones: Set<MilestoneId> = emptySet()`.
- Use cases (each returns a new `SettlementRecord`; refusals surfaced as today):
  - `AdvanceTimeline.run(record)` — increments the year once per phase (marker in `phaseActionsUsed`, reset at the year boundary like `GainEndeavors`).
  - `RecordDeaths.run(record, count)` / `RecordBirths.run(record, count)` — counters + population, non-negative.
  - `ChoosePrinciple.run(record, category, label, ruleset)` — persists the choice (rejects an unknown category/label against the ruleset).
  - `ResolveMilestone.run(record, id, ruleset)` — ticks a reached milestone (rejects an unreached id).
  - `RecordSettlementEvent.run(record, name)` — sets `Settlement.settlementEvent` (validates the name against the catalog when one is supplied).
- `TransferDocument.schemaVersion` → `6` (fields carry defaults, so v5 documents still decode; `SUPPORTED_VERSIONS` gains 6).

### `:app-android`
- New composables, each stateless, driven from `UiState` and reporting intents: `TimelineStepScreen`, `DeathCountStepScreen`, `MilestonesStepScreen`, `ArchiveStepScreen`, `EndPhaseStepScreen`.
- `SettlementScreen`'s `when (state.currentStep)` dispatches to them (7 → `DevelopStepScreen`, 1 → `SetUpStepScreen`, the rest still `Pendiente` for now).
- `UiState` gains: `settlementEventDeck: List<SettlementEvent>`, `settlementEvent: String?`, `deaths: Int`, `births: Int`, `milestoneViews: List<MilestoneView>`, `chosenPrinciples: Map<PrincipleCategory, String>`. The Step 11 warning reuses the existing `endeavors` field.
- `MilestoneView(id, description, reached, resolved, choices: List<PrincipleChoice>)` is an app-side derived view.
- Controller methods: `recordDeaths`, `recordBirths`, `choosePrinciple`, `resolveMilestone`, `recordSettlementEvent`; plus the year increment folded into the existing `applyStep` when the transition leaves Step 4.

## Data flow

`ContentCatalog` (settlement events) + `Ruleset` (milestones/principles) + `SettlementRecord` → `:core` use cases / derived helpers → `UiState` → the step bodies. Every mutation goes through the controller → `SettlementRepository.update` (stamped sync) → store; a sync pass re-reads via `reload()` as today.

## Testing

- **`:core`** — strict TDD for each use case (year increments once, revisits are idempotent, deaths/births floor at 0, unknown principle/milestone refused, event recorded), plus `SettlementState` serialization and a v5→v6 transfer round-trip.
- **Python** — `ingest_settlement_events` fixture tests: page count = cards, title/body split, no duplicates, non-empty text.
- **`:app-android`** — controller tests asserting the derived views and each intent persisting over a seeded store; no Compose UI tests.
- Full suite green: `./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck`.

## Risks

- **PDF text quality** — the event deck is Illustrator output; the extraction is line/paragraph based. Pin a fixture per card and fail loudly on an empty card rather than emitting a blank event.
- **Year-increment placement** — the increment must key off *leaving Step 4*, and the phase marker must reset at the year boundary (mirrors `fundGainEndeavors`). Pin with a revisit test.
- **Step 5 scope creep** — births on the death-count screen is a deliberate deviation to keep `NEW_LIFE` reachable; keep the UI clearly two counters, not a full population editor.
- **Transfer compatibility** — new `SettlementState` fields are defaulted, but the version bump must keep v5 decoding; pin a v5 document decode test.

## References

- Research: `server/src/main/resources/static/prototypes/research/lantern-year-steps.md` (§2 steps 4–6/10–11, §5 UX)
- Content source: `external-assets/Juego Completo/Formato XL 86x146 mm/AF Eventos.pdf` (23 cards, git-ignored)
- Ingest pattern: `tools/content-ingest/ingest_innovations.py`, `pdf_text.py`, `kotlin_emit.py`
- Existing surfaces: `core/.../domain/{SettlementState,Settlement,Timeline}.kt`, `core/.../ruleset/Core16Ruleset.kt`, `app-android/.../ui/SettlementScreen.kt`, `app-android/.../LanternYearController.kt`
