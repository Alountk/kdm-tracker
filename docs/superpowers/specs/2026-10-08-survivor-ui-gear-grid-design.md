# Survivor UI Redesign + Gear Grid — Design

**Status:** Draft (awaiting review)
**Slice:** Redesign the survivor UI into a reusable component set — a compact **card** that expands in place, **filter chips**, the full-screen **sheet editor**, and a **3×3 gear grid** for the Step 8 loadout — matching the cockpit's visual language.
**Depends on:** PR #18 (survivor roster + Steps 2 & 8), #17 (top bar).

## Goal

The survivor UI shipped in #18 is functional but plain: a flat scroll of `Surface` cards and a full-screen `Dialog`, with no filters and no gear grid. This slice gives it the cockpit's language and a shared component set that later steps (Hunt party, Showdown) reuse.

Success is:
1. A **`SurvivorCard`** that collapses to one scannable line and **expands in place** to the sheet (read) — no navigation to look at a survivor.
2. **Filter chips** over the roster (Todos / Heridos / Salientes / Sin nombre / Muertos) with a **status dot** per card.
3. The **full-screen sheet editor** (the existing `SurvivorEditorDialog`) reached from the expanded card.
4. A **3×3 gear grid** on the expanded card of a departing survivor in Step 8: the 9 slots, one item each, assigned from the settlement's crafted gear.
5. Rules stay in `:core`; screens stay stateless.

## Design (validated with the user, browser mockups)

### Component set (all in `:app-android`, stateless)
- **`StatusDot`** — a small coloured dot; one colour per survivor with this precedence:
  `DEAD` → grey, `DEPARTING` → blue, injured (any light/heavy wound) → amber, unnamed (blank `name`)
  → grey, otherwise `ALIVE` → green.
- **`SurvivorCard`** — collapsed: `● Name · sex · STATUS` + a one-line summary (key stats, wound/token markers). Tapping toggles the expanded state. A `trailing` slot carries the step's contextual control (Depart, …).
- **`SurvivorSheet`** — the expanded read view: survival/limit, insanity, the six attributes, per-location armor + light/heavy, tokens, gear, disorders/fighting arts, progression, notes — plus an `Editar ficha` button.
- **`SurvivorEditorDialog`** — the existing full-screen sheet editor (choice **2b**), unchanged except for the error display already added.
- **`RosterFilterChips`** — a `ChoiceChip` row (the cockpit's chip vocabulary): **Todos** (default), Heridos, Salientes, Sin nombre, Muertos.
- **`GearGrid`** — the 3×3 config grid (below).

### Step 2 — Survivors Return
Filter chips + the roster of `SurvivorCard`s + `Heal & clear tokens` as the step's contextual action. Expanding a card shows the sheet; `Editar ficha` opens the editor.

### Step 8 — Prepare Departing Survivors
Filter chips (**default Todos**, choice **3a**) + the roster; each card's trailing control is a **Depart** toggle (capped at `min(4, living)` by `:core`). Expanding a **departing** card shows its **GearGrid**; non-departing cards expand to the sheet only.

### Dead survivors
**Shown** (choice **4a**): a `Muertos` filter chip and DEAD cards rendered greyed. They are not selectable/departable.

### Gear grid (3×3, configuration view)
- **9 uniform slots** (all the same height, sized to the tallest content so long names wrap without distorting). One item per slot; "two-handed" is a keyword (usage restriction), **not** a footprint.
- Assigning adds a name from the settlement's crafted gear (`UiState.gear`); removing takes it out. Enforced **≤ 9**.
- **No armor derivation** and **no content/ingestion change** here — this is the *setup/config* grid. Armor-per-location and a playable, card-faithful grid belong to the **Showdown** slice (temporary combat calculations).

## Scope

### In
- `:app-android`: the component set (`SurvivorCard`, `SurvivorSheet`, `StatusDot`, `RosterFilterChips`, `GearGrid`), rewritten `SurvivorsReturnStepScreen` and `PrepareDepartingStepScreen`, a pure `filterSurvivors(survivors, filter)` seam, and the Step 8 gear-grid wiring.
- `:core`: cap `AssignGear` at 9 items (`MAX_GEAR_SLOTS`).

### Out (explicitly deferred)
- Armor-per-location derivation and the gear-card-faithful, playable grid — the **Showdown** slice.
- Extending `ingest_gear.py` for armor icons / locations.
- Per-slot gear positions (the grid holds up to 9 items; it does not model cell indices as game state).
- Wiring the shared component into a `Survivors` screen, the Hunt party, or the Showdown.
- Any change to `Survivor.gear`'s type (`List<String>`) — the grid is a layout over it.

## Architecture

### `:core`
- `AssignGear`: refuse assigning beyond `MAX_GEAR_SLOTS = 9` (a new constant; the refusal reason is surfaced like the others).

### `:app-android`
- New stateless composables (above) + a `SurvivorFilter { ALL, INJURED, DEPARTING, UNNAMED, DEAD }` enum and a pure `filterSurvivors(...)` (testable, no Compose). The category filters are derived from `survivorTone`, so a chip never shows a survivor whose dot contradicts it.
- `SurvivorsReturnStepScreen` / `PrepareDepartingStepScreen` rewritten on the component set (keep the existing `onX` callbacks; the editor wiring stays).
- `SettlementScreen` dispatch unchanged (steps 2/8 already routed).

## Data flow

`UiState.survivors` → `filterSurvivors` → the card list; expansion is local UI state; the editor reports `onSaveSurvivor`; the Step 8 grid reports `onAssignGear`/`onUnassignGear`; `Depart` reports `onDepartingChange`. All mutations go through the existing controller methods.

## Testing

- **`:core`** — `AssignGear` refuses the 10th item with the record untouched.
- **`:app-android`** — a pure `filterSurvivors` table test (each filter, including DEAD/unnamed/injured) and a controller test that the cap surfaces `actionError`.
- Full suite green: `./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck`.
- Manual emulator pass: card expand, filters, editor, Step 8 depart + the 3×3 grid.

## Risks

- **Card expansion state** — one local `expandedId` per roster; opening one collapses the others. Pin the behaviour in the composable (no test).
- **Grid on small screens** — 3 columns must stay readable; uniform cell height sized to the tallest. Verify on the emulator.
- **Filter correctness** — the DEAD/UNNAMED/INJURED predicates must match the dots; the pure `filterSurvivors` test is the guard.
- **Cap** — the 9-item cap must be enforced in `:core`, not just hidden in the UI.

## References

- Mockups (persisted): `.superpowers/brainstorm/57426-1791498472/content/{survivor-ui-options,survivor-b-full,survivor-b-loadout,survivor-decisions,gear-grid-3x3-v2,gear-grid-3x3-v3}.html`
- Existing: `app-android/.../ui/{SurvivorsReturnStepScreen,PrepareDepartingStepScreen,SurvivorEditorScreen,DevelopStepScreen}.kt`, `core/.../usecase/AssignGear.kt`
