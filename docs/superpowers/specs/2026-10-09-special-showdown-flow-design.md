# Special Showdown + Nemesis Flow — Design (Slice 3)

**Status:** Draft (awaiting review)
**Slice:** Wire the two settlement-flow branches the domain already models but the app never triggers: a **nemesis year** skips the Hunt straight to the Showdown, and a **Special Showdown** (step 9) runs an encounter during the Settlement Phase and loops back to the death-count step.
**Depends on:** PR #20 (Hunt) and #21 (Showdown). Part 3 of 3 of the Hunt + Showdown effort.

## Goal

`LanternYearFlow` already models both branches (`FlowContext.isNemesisYear` → Showdown; `FlowContext.hasSpecialShowdown` → LoopBack to the death-count step), but the controller never sets either flag. This slice makes them real:

1. **Nemesis year**: when the current year's timeline lists a nemesis encounter, the Settlement Phase's final commit goes **straight to the Showdown**, skipping the Hunt; the Showdown setup pre-selects that nemesis.
2. **Special Showdown (step 9)**: the player declares one for the year; the step body runs the **reused encounter tracker**, and concluding **loops back to Step 5** (Update the Death Count).
3. Reuse the encounter tracker between the Showdown phase and the Step 9 body.

## Design (validated with the user)

### Nemesis year (automatic)
- A year is a **nemesis year** when its timeline entries include a nemesis label (the core-1.6 timeline ships `"Nemesis: The Butcher Lvl 2"`, `"Nemesis: King's Man Lvl 2"`, …).
- The controller derives `isNemesisYear` from `record.state.timeline.entriesFor(settlement.lanternYear)` and passes it in the `FlowContext` of the normal advance, so `LanternYearFlow` routes the Step 11 commit to the **Showdown** instead of the Hunt.
- A `nemesisFor(record)` helper extracts the nemesis name (the label after `"Nemesis:"`, level stripped) so the Showdown setup can pre-select it.

### Special Showdown (player-declared at step 9)
- `SettlementState.specialShowdown: Boolean = false`, set by a Step 9 control ("This year has a special showdown").
- When set, the Step 9 body shows the **shared encounter tracker** (the monster + its AI deck + the outcome + the aftermath) and a single `Conclude · back to Step 5` action.
- Concluding advances from Step 9 with `hasSpecialShowdown = true`, so `LanternYearFlow` emits the **LoopBack to the death-count step (5)**; the settlement then continues normally.
- When not set, Step 9 advances normally to Step 10.

### Shared encounter tracker
- Extract the Showdown body's AI-deck tracker + hit-locations reference + outcome + aftermath from `ShowdownScreen` into a reusable `EncounterPanel` composable, used by both the Showdown phase body and the Step 9 special-showdown body.
- The encounter state is `SettlementState.showdown` (one encounter at a time: the special is resolved before the year's phase Showdown is set up).

## Scope

### In
- `:core`: `SettlementState.specialShowdown`; `SetSpecialShowdown`; a `timeline`/nemesis helper; transfer **v10**.
- `:app-android`: the controller passes `isNemesisYear`/`hasSpecialShowdown` in the `FlowContext's`; `UiState.specialShowdown` + `nemesisFor`; the Step 9 body; the extracted `EncounterPanel`; the Showdown setup pre-fills the nemesis.

### Out (explicitly deferred)
- Ingesting/mostrating **Special Showdown timeline content** (the player declares it) and an editable timeline.
- The **playable gear grid** (still a later slice).
- Automatic nemesis level selection from the timeline label (the label carries `Lvl 2`; the setup uses level 1 by default and the player adjusts).
- Any change to the Hunt or Showdown bodies beyond extracting the shared panel.

## Architecture

### `:core` (pure, additive)
- `SettlementState.specialShowdown: Boolean = false`; transfer v10.
- `usecase/SetSpecialShowdown.kt` (`run(record, enabled)`).
- A helper on `SettlementRecord`/`Timeline`: `nemesisOf(year): String?` — the nemesis name from the year's labels (after `"Nemesis:"`, level suffix trimmed).

### `:app-android`
- `LanternYearController`: `declareSpecialShowdown(enabled)`; `advance`/`applyStep` build `FlowContext(ruleset, isNemesisYear = nemesisFor(record) != null, hasSpecialShowdown = record.state.specialShowdown, unspentEndeavors = …)`.
- `UiState.specialShowdown` + `nemesis: String?`.
- `ui/SpecialShowdownStepScreen.kt` (the toggle + the `EncounterPanel` + conclude).
- `ui/EncounterPanel.kt` extracted from `ShowdownScreen`; the Showdown setup pre-selects `state.nemesis`.
- `SettlementScreen` dispatches Step 9.

## Data flow

`record.state.timeline` → `nemesisFor` → the Step 11 commit routing; `record.state.specialShowdown` → `hasSpecialShowdown` → the Step 9 LoopBack. Mutations via the controller → the repository.

## Testing

- **`:core`** — `SetSpecialShowdown` (set/clear); `nemesisOf` (a nemesis year vs a plain one); serialization/transfer v10; the `LanternYearFlow` branches are already tested.
- **`:app-android`** — the controller: a nemesis year's Step 11 `advance` reaches the **Showdown** (not the Hunt); a non-nemesis year reaches the Hunt; the Step 9 advance with the flag **loops to Step 5**; `declareSpecialShowdown` persists.
- Full suite green; the manual emulator pass.

## Risks

- **The flags must reach `FlowContext` on the right transition** — `isNemesisYear` only matters at the Step 11 commit; `hasSpecialShowdown` only at Step 9. Pin with controller tests.
- **Reusing `SettlementState.showdown`** for both the special and the phase encounter — they do not overlap in time, but a new `SetUpShowdown` must overwrite the special's; pin.
- **The loop must not double-advance** — the Step 9 conclude must be a single transition; pin.

## References

- `core/.../rules/LanternYearFlow.kt` (the branches), `FlowContext`, `SettlementStepCatalog` (step 9).
- `app-android/.../ui/ShowdownScreen.kt` (to extract), `LanternYearController.kt`.
- Research: `server/src/main/resources/static/prototypes/research/lantern-year-steps.md` (§2 step 9).
