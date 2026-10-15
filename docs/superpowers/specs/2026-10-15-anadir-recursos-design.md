# Develop Step — the Add-Resources Flow (Part A, Phase 3)

**Status:** Draft (awaiting review)
**Depends on:** PR #35 (Phase 2, the innovar flow) merged — squash commit `955d7b6` is on `main`.
**Shape:** Phase 3 of 3 of "Part A" (the Develop step's three flows from the Stitch design). Phase 1 = Fabricar (#34), Phase 2 = Innovar (#35). This is the last one.

## Goal

Implement the resource-adjust flow — **tap a cell, step it inline, save** — as the design specifies: the summary's four-cell grid renders its four states (rest, editing, minimum, confirmed) from one pure flow model, the ledger can never go negative, and nothing is added to the persisted campaign.

## Source of truth

- **Design:** cached `.superpowers/design/innovar-flow/recursos_flujo.html` — *"Recursos: añadir y quitar — flujo corto"* (four labelled `ESTADO N` sections plus a UX verdict). It is a guide sheet, not a 390px screen: "1:1" means same structure, tokens and copy at the app's density, as in Phase 2.
- **Existing surface:** `SettlementSummary.kt` already renders a first cut of this flow (cells, inline stepper, `previo:`, save button, success strip) from local `remember` state. Phase 3 moves that state into a pure model and lands the design states the first cut never had (dimmed non-selected cells, the red minimum state, the green confirmed cell).
- **Content authority:** the S2 name→id resolution already carries this flow — `SettlementScreen` passes `(ResourceId, delta)` through `onAdjust` → `LanternYearController.addResource` → `:core`'s `AddResourceEntry`. Unchanged.

## Decisions (validated with the maintainer 2026-10-15)

1. **Tap enters editing; the hint follows the design's own UX verdict.** The sheet's state-1 hint says `Mantén pulsada una celda para ajustarla.`, but its *Dictamen UX* warns that a long-press-only affordance is undiscoverable and endorses the simple tap. The maintainer chose the tap: behaviour stays as today and the hint copy becomes **`Toca una celda para ajustarla.`** This closes the review's deferred "mantén pulsada" visual minor, in its own component.
2. **Icons follow the newest HTML.** Hueso = `nutrition`, Órgano = `psychology`, Pellejo = `layers`, Chatarra = `hardware`. The settlement-screen grid's `format_h4` / `vital_signs` retire for these two cells (maintainer's choice: the newer design wins). All four glyphs are already pinned in `KdmSymbolsTest`'s 88-name map and present in the subset — **no font work**.
3. **The flow's state is a pure model, and `ResourceEdit` becomes the foundation it was kept for.** The composable's `editing` / `pending` / `saved` locals retire in favour of one `ResourceAdjustFlow` built on the already-tested `ResourceEdit` and `AdjustResult`. The composable only renders it. (This answers the review's "`ResourceEdit` is dead code" minor: it was the skeleton for this phase, not debt.)
4. **Saving with no change does nothing.** `delta == 0` ⇒ no `onAdjust` call and no success strip. Today's code skips the ledger call but still celebrates (`Hueso: 12 → 12`) — the review's deferred "guardar sin cambio grita éxito" minor, closed here because this component owns it.
5. **No `:core` rule change and no schema change.** `AddResourceEntry` already refuses an overdraft as `Blocked`, and the controller already publishes that reason to `state.actionError`. `TransferDocument.schemaVersion` stays **12**: the flow is derived UI state, nothing is persisted.

### Deliberate deltas against the sheet (called out at the visual gate)

- **The save button stays visible at the minimum.** The sheet's state 3 drops it, which would make `12 → 0` unconfirmable — a legal spend the ledger accepts. State 3 instead renders the cell's `agotado` caption **and** the red error caption below the row, with `Guardar ajuste` still under them.
- **The hint copy** (decision 1) replaces state 1's literal long-press text.

## Design

### Model — new, pure, JVM-tested

```kotlin
/** The summary's adjust flow: which cell is open, what it holds, what was just saved. */
data class ResourceAdjustFlow(
    val edit: ResourceEdit? = null,              // null ⇒ no cell is open
    val confirmed: AdjustedCell? = null,         // the last save: identity + result
) {
    enum class CellState { REST, EDITING, MINIMUM, CONFIRMED }

    fun select(resource: SummaryResource): ResourceAdjustFlow  // opens the cell, clears any confirmed
    fun bump(by: Int): ResourceAdjustFlow                      // floors at 0 via ResourceEdit.bump
    fun save(): ResourceAdjustFlow                             // clears edit; sets confirmed iff delta != 0
    val delta: Int                                             // edit?.delta ?: 0 — the caller's onAdjust gate
    fun cellState(resource: SummaryResource): CellState        // CONFIRMED > EDITING/MINIMUM(pending == 0) > REST
    val isEditing: Boolean                                     // dims the non-selected cells
    val isIdle: Boolean                                        // edit == null && confirmed == null ⇒ hint row
}
```

Matching is by `ResourceId` (never by display label), so a confirmed cell re-greens after every model rebuild. `select` clears `confirmed`; `save` with `delta == 0` clears the edit and leaves `confirmed` null.

### UI — render only

| flow | grid | footer |
|---|---|---|
| idle | cells rest: icon → label → value, 74dp tall (the sheet's stack order) | hint row `touch_app` + `Toca una celda para ajustarla.` |
| editing | open cell: gold `border-2`, icon+label gold, stepper (`remove` inert at 0 / value / `add`), `previo: N` **inside** the cell; other cells `opacity-60` | full-width `Guardar ajuste` (`check_circle`, gold) |
| minimum | open cell red: border/icon/label/value, minus disabled, `agotado` caption inside | red caption box (`error` + `Mínimo alcanzado — el almacén no admite negativos.`) **above** the save button (delta) |
| confirmed | changed cell: green border, green dot top-right, green icon; others rest | green strip (`check_circle` + `Hueso: 12 → 14` + `Ajustado ahora`); no hint |

Footer precedence: editing/minimum ⇒ caption + save; confirmed ⇒ strip; otherwise ⇒ hint. `ResourceColour.icon` carries decision 2; the cell colour roles (`tint()`) stay as they are for the rest state.

### Where things change

- `ResourceAdjustModel.kt` — add `ResourceAdjustFlow` (+ `AdjustedCell`) next to `ResourceEdit` / `AdjustResult`.
- `SettlementSummary.kt` — hold one `ResourceAdjustFlow` in `remember`; render the four states; `onAdjust` fires only through `delta != 0`.
- `SettlementSummaryModel.kt` — `ResourceColour.BONE("nutrition")`, `ResourceColour.CORAL("psychology")`.
- Tests: `ResourceAdjustModelTest` (flow semantics), `SettlementSummaryTest` (icon pins).
- Unchanged: `SettlementScreen` wiring, `:core`, `data`, `server`, `KdmSymbols`, the TTF.

## Testing

JVM only — the app module has no Compose test harness (established): every rule above is model-level and checked through `:app-android:testDebugUnitTest`.

- **Flow (RED first):** `select` opens and clears any confirmed result; `bump` floors at 0 and flips to `MINIMUM` at `pending == 0`; `cellState` by `ResourceId`; `save` with `delta == 0` clears the edit and emits no confirmed result; `save` with `delta != 0` records `AdjustedCell(id, "Hueso: 12 → 14")` and clears the edit; `isIdle` only when there is nothing open and nothing confirmed.
- **Icons:** the four `ResourceColour.icon` values are `nutrition` / `psychology` / `layers` / `hardware`.
- **Gate:** pixels, copy, dimming and the minimum/confirmed accents are judged on the emulator before any commit (global constraint).

## Out of scope

- **Batch add + loot-origin selector** (the fabricar spec's older sketch of "Phase 3"): no design exists for it; it needs its own Stitch fetch before anyone implements it.
- Resources outside the design's four cells — the grid stays `DESIGN_ORDER`.
- The other two deferred review visuals (`costChip` copy, steal-row alignment): they belong to the Innovar/catalogue surface, not this flow.
- Any `:core`, controller, transfer-schema or font change.
