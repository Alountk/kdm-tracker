# Settlement Steps 1 & 7

How the Settlement Phase steps are implemented in the Compose app and in `:core`. Back to the [README](../README.md).

Two Settlement Phase steps are real in the Compose app; the rest keep their `Pendiente` body. Both
are backed by a content-driven rules layer in `:core` — the screens render state and report intent,
and every cost and limit comes from the composed `ContentCatalog`, never a literal in the UI.

- **Step 1 — Set Up the Settlement** (`SetUpStepScreen`): read-only. Shows the principles in play,
  the derived **Innovation Deck** (the consequences the unlocked innovations add), the locations
  already built, and this lantern year's timeline/event, then a single *Confirm Setup* that advances.
- **Step 7 — Develop** (`DevelopStepScreen`): rendered inside the cockpit's chrome, so it is never
  a navigation dead-end. Three tabs, all drawn from `:core`-derived views:
  - **Recursos** — resources grouped by **source** (Básicos / León Blanco / Fénix / …), each with a
    `+1` (the manual funding bridge until hunt rewards are modeled).
  - **Innovar** — the **unlock tree** by access state: filter chips *Todas / Disponibles /
    Desbloqueadas / Sin acceso*, coloured **green** (unlocked) / **blue** (available — its parent is
    unlocked, i.e. in the `InnovationDeck`) / **grey** (no access; names the parent it still needs).
  - **Fabricar** — the locations you can still build, plus gear grouped by **localización** or by
    **tipo**; a recipe is **green with an enabled *Crear*** iff `Craft` would succeed, and otherwise
    dimmed with what is missing.
  Rules text (effects) is shown verbatim; the group resolves it.

`SettlementScreen` renders a **thin top step bar** (the 11 steps as numbered chips, the current one
highlighted and forward-skips disabled) with the body spanning the full width — the old
half-screen left rail is gone. Step 1 renders Set Up, step 7 supplies the Develop body, and every
other step shows the "Pendiente" placeholder — all inside the same chrome, so no step is
unreachable. Each action calls a pure `:core` use-case through the
controller and persists the mutated `SettlementRecord` via `SettlementRepository.update` (local
store + outbox), so it syncs like any other change. Rules are **total**: an invalid action returns
`StepResult.Blocked(reason)` instead of throwing, and the screen shows the reason inline with the
record unchanged.

| Rule | Enforcement |
| --- | --- |
| **Endeavor income** | Entering Settlement Step 3 runs `GainEndeavors`, adding the Step 3 breakdown (base **+1 per returning survivor**, funded from the population in this slice) to the pool once per phase. |
| **Innovate** | `Innovate`: only at a **built** location granting it (the Lantern Hoard is built in the seed) — spends the action's endeavors **and** resources (1 Bone + 1 Organ + 1 Hide in 1.5), is **once per Settlement Phase** (a phase marker reset at the year boundary), and **draws 2, keeps 1**. |
| **Build** | `BuildLocation`: the location's `requires` must already be built, storage must cover its resource cost, and the pool must cover its `buildEndeavors` (prereq tree). |
| **Craft** | `Craft`: only at a **built** location, using one of its `GearRecipe`s — the recipe's innovation `requires` must be unlocked and storage must cover its resource cost; the gear is then added to the settlement. |
| **Generic endeavor** | `SpendEndeavor`: a rule carrying the `[endeavor]` symbol costs −1; the player resolves the effect. |
| **Storage** | `AddResourceEntry` appends a gain, but blocks a spend that would take a resource below 0. |
| **Step 11** | Unspent endeavors are discarded at the year boundary (`AdvanceStep`), not carried over. |

**Craft (Step 7).** `Gear` carries a `keywords`/`rules`/`modifiers` model, and `GearRecipe` a
resource cost plus innovation prerequisites; recipes travel on the built `SettlementLocation`.
`Craft` enforces built location + recipe + prerequisites + storage, then adds the gear to the
settlement. The **gear `Modifier` model** (`target`/`value`/`scope`/`condition`) is ready for
curated data: settlement-scope modifiers feed derived values, survivor-scope ones are stored and
applied once a `Survivor` model exists. The gear definitions and recipes are **auto-extracted** from
the 1.5 cards by `tools/content-ingest/ingest_gear.py` (see *Content ingestion pipeline*):
`pdftotext -raw`/`-layout` were not enough (they strip spaces or flatten columns), so it uses
`pdftotext -bbox-layout` (word boxes) to recover spaced text and separate columns. A prerequisite
that is not an innovation name (e.g. `Requiere Calor`) is kept as display-only `requiresText` and
does not gate crafting. The card bonuses remain `[gap]` (they are conditional rule text), never
guessed.

**Known limitation (Ruling CB).** The extracted `buildCost`/`resourceCost` are keyed by a **specific
resource card name** that approximates the card's printed material *type* (`3 x hueso` →
`Hueso de Monstruo`), because the model has no resource-type key yet. A settlement holding a
different resource of the same type would be wrongly `Blocked`; resolving costs by type is a
follow-up work unit.

**Known limitation (upgrade path).** The shipped seed enables `core-1.6` and `core-1.5`, but a
campaign stored before that change keeps its original `enabledPacks` (often empty) and therefore
composes an empty catalog with no in-app path to fix it. The app is pre-release with no persisted
production campaigns, so this is deferred: an existing local campaign must be **re-created** (or the
store re-seeded) to pick up the packs.

**Deferred:** gear `modifiers` content (the card bonuses are conditional rule text, recorded as
`[gap]`), applying survivor-scope modifiers (needs the `Survivor` model), executable effects (shown
as text, not resolved), a full per-survivor `Survivor` model (returnees and deaths are
settlement-level counters), and 1.6-specific rules (the local rulebook is 1.5 and the 1.6 sheet is
image-only).
