# Survivor Roster, Steps 2 & 8 — Design

**Status:** Draft (awaiting review)
**Slice:** Add the **Survivor** domain aggregate (the full record sheet as data), a **Survivor Editor**, and the Settlement Phase bodies for **Step 2 (Survivors Return)** and **Step 8 (Prepare Departing Survivors)**. Population becomes a projection of the roster.
**Depends on:** PR #14 (cockpit/step bar), #15 (bookkeeping steps 4/5/6/10/11), #17 (top bar + account).

## Goal

Steps 2 and 8 still render `Pendiente`, and the settlement has no survivor roster — only a `population` scalar. This slice adds the roster and turns the two survivor-driven steps into usable bodies, with the record sheet's fields as the model of record.

Success is:
1. A settlement holds a **roster of survivors** with the full sheet's data (metadata, vitals, stats, per-location armor/wounds, progression, limitations, notes, hunt state).
2. **Population is derived** from the roster (living survivors), not a free scalar.
3. **Step 2** heals the returning survivors (erase light/heavy injuries, clear tokens) and lets the user record what they brought back (stat losses from permanent injuries, new disorders, improvements, XP) through a **Survivor Editor**.
4. **Step 8** picks up to 4 departing survivors and assigns settlement gear to each.
5. Rules stay in `:core`; screens are stateless.

## Design (validated with the user)

### Survivor model (data-complete)

The reference is the official survivor sheet (`external-assets/KDM Ficha superviviente (Eli mini).md`). The model mirrors it as **data**; the Steps 2 & 8 UI uses the subset it needs, and the rest is editable later without a migration.

- `SurvivorStatus { ALIVE, DEPARTING, DEAD }`.
- `HitLocation { HEAD, TORSO, ARMS, WAIST, LEGS }`; `WoundState(armor: Int = 0, light: Boolean = false, heavy: Boolean = false)`
  (`light` = firme, `heavy` = grave).
- `Attributes(movement, accuracy, strength, evasion, luck, speed: Int = 0)`.
- `WeaponProficiency(type: String = "", xp: Int = 0, specialist: Boolean = false, master: Boolean = false)`.
- `Survivor`:
  - **metadata**: `id: Ulid`, `name: String = ""`, `sex: Sex = Sex.OTHER`, `nameBonusApplied: Boolean = false`, `retired: Boolean = false`
  - **vitals**: `survival: Int = 0`, `insanity: Int = 0`, `brainTrauma: Boolean = false`
  - **stats**: `attributes: Attributes = Attributes()`
  - **armor & wounds**: `wounds: Map<HitLocation, WoundState> = emptyMap()` (a missing key reads as zero)
  - **progression**: `huntXp: Int = 0`, `weapon: WeaponProficiency`, `courage: Int = 0`, `understanding: Int = 0`
  - **limitations**: `disorders: List<String> = emptyList()` (≤ 3), `fightingArts: List<String> = emptyList()` (≤ 3), `notes: String = ""`
  - **hunt state**: `tokens: Set<String> = emptySet()`, `gear: List<String> = emptyList()`, `status: SurvivorStatus = ALIVE`

The **maximum survival** is the settlement's survival limit (derived, not stored). The Valor/Understanding **milestone abilities** (Audaz, Firme, …) are derived from `courage`/`understanding` when the ruleset models them (later); they are not stored.

### Derived rules

- `SettlementState.survivors: List<Survivor> = emptyList()`.
- **Population** = `survivors.count { it.status != DEAD }`, exposed as `SettlementRecord.population`. Every survivor mutation keeps the legacy `Settlement.population` mirror equal to it (for export back-compat), and all readers use the derived value.
- **Effective attribute**: `effectiveAttribute(survivor, target, catalog)` = the base attribute for `target` + the **survivor-scope** gear modifiers of the survivor's assigned gear (settlement-scope modifiers feed settlement derived values elsewhere). Displayed only; never stored.

### Step 2 — Survivors Return

- A **`Heal & clear tokens`** bulk action (`ReturnSurvivors`): every living survivor's `light`/`heavy` wounds reset to `false` and their `tokens` clear. Armor is left untouched and survival/insanity are **not** refilled.
- A roster list, and a per-survivor **Editor** to record the return-time changes: attribute edits (permanent injuries / improvements), new disorders, new fighting arts, courage/understanding, hunt XP, weapon XP, brain trauma, retirement.

### Step 8 — Prepare Departing Survivors

- Pick up to **4** departing survivors (fewer when the living population < 4): `MarkDeparting(ids)` sets `DEPARTING` on the chosen, `ALIVE` on the rest.
- Assign settlement gear to each departing survivor (`AssignGear` / `UnassignGear`) from the settlement's crafted gear. Per-slot gear grids are out of scope; gear is a name list per survivor.

### Survivor Editor

One stateless screen/sheet editing the whole sheet: name, sex, `nameBonusApplied`, `retired`, survival, insanity, brainTrauma, the six attributes, per-location armor + light/heavy, tokens, disorders (≤ 3), fighting arts (≤ 3), hunt XP, weapon type/XP/specialist/master, courage, understanding, notes. Reachable from the Step 2 roster, the Step 8 roster, and (later) the roster anywhere.

## Scope

### In
- `:core`: the `Survivor` aggregate + `SettlementState.survivors`; derived population + effective attribute; use cases `RecordDeaths`/`RecordBirths` (roster-aware), `ReturnSurvivors`, `MarkDeparting`, `AssignGear`/`UnassignGear`, `UpdateSurvivor`; migration/transfer **v7** (seed the roster from the legacy population).
- `:app`: a survivor roster in `UiState`, the Step 2 and Step 8 bodies, the Survivor Editor, the controller methods, and the cockpit dispatch.

### Out (explicitly deferred)
- Per-slot **gear grids** and armor recompute from gear.
- The Valor/Understanding **milestone abilities** and age milestones as rules.
- **Strain/Pattern** fighting arts, weapon mastery perks, saga/retirement rules.
- Editing surface beyond the editor (e.g. a full survivor list on Home).
- Hunt/Showdown bodies.

## Architecture

### `:core` (pure, additive)
- `domain/Survivor.kt` (+ `SurvivorStatus`, `HitLocation`, `WoundState`, `Attributes`, `WeaponProficiency`).
- `SettlementState.survivors: List<Survivor>`; serialization + transfer v7.
- `SettlementRecord.population` (derived) + `SettlementRecord.effectiveAttribute(...)`.
- Use cases (each returns a `SettlementRecord` or `StepResult<SettlementRecord>` as today):
  - `RecordDeaths.run(record, count)` — marks the first `count` living survivors `DEAD`, `deaths += count`, population projects down.
  - `RecordBirths.run(record, count, newId: () -> Ulid)` — appends `count` new survivors (unnamed, `Sex.OTHER`), `births += count`.
  - `ReturnSurvivors.run(record)` — heals `light`/`heavy` and clears tokens for living survivors.
  - `MarkDeparting.run(record, ids, max)` — sets `DEPARTING` on `ids` (refuses more than `max` = min(4, living)); others `ALIVE`.
  - `AssignGear.run(record, survivorId, gearName)` / `UnassignGear.run(record, survivorId, gearName)`.
  - `UpdateSurvivor.run(record, survivor)` — replaces by id after validating (`disorders ≤ 3`, `fightingArts ≤ 3`, ints ≥ 0).
- Migration: `SettlementRecord.migrated()` seeds `population` unnamed survivors when the roster is empty and a legacy population is present (used on load and in `TransferMapper.toDomain`).

### `:app-android`
- `UiState`: `survivors: List<Survivor>`, `departingIds: List<Ulid>` (or read from status).
- Screens: `SurvivorsReturnStepScreen` (Step 2), `PrepareDepartingStepScreen` (Step 8), `SurvivorEditorScreen`.
- `SettlementScreen` dispatch for steps 2 and 8; controller methods `healSurvivors`, `recordDeaths`/`recordBirths` (roster-aware), `markDeparting`, `assignGear`/`unassignGear`, `updateSurvivor`.

## Data flow

`SettlementRecord.state.survivors` → derived population + `UiState.survivors` → the Step 2 / Step 8 bodies and the editor. Every mutation goes through the controller → `SettlementRepository.update` (stamped sync) → store; a sync pass re-reads via `reload()` as today.

## Testing

- **`:core`** — strict TDD: the model round-trips through serialization/transfer v7; population projects from the roster; `effectiveAttribute` sums survivor-scope gear modifiers; each use case (kill/add survivor, heal+clear, mark departing with the ≤ max guard, assign/unassign, update validation); migration seeds the roster from a legacy population.
- **`:app-android`** — controller tests: the Step 2 / Step 8 derived views and each intent persisting over a seeded store; no Compose UI tests.
- Full suite green: `./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck`.
- Manual emulator pass: add/edit a survivor, Step 2 heal, Step 8 depart + gear.

## Risks

- **Population migration** — existing records have a scalar population and no roster. The legacy scalar must be the seed source; pin a migration test (roster empty + population 4 → 4 survivors) and a round-trip.
- **Over-large model** — the data-complete sheet is big; keep every field defaulted and serialized with `ignoreUnknownKeys`, and keep the editor's validation in `:core` so the UI can't corrupt limits.
- **Id generation in `:core`** — a survivor id needs a `Ulid`; the births use case takes an id supplier so it stays pure and deterministic in tests.
- **Departing cap** — the "≤ 4 (or living)" rule must live in `:core` (`MarkDeparting`), not the UI.

## References

- Survivor sheet: `external-assets/KDM Ficha superviviente (Eli mini).md`
- Research: `server/src/main/resources/static/prototypes/research/lantern-year-steps.md` (§2 steps 2, 8)
- Existing domain: `core/.../domain/{Settlement,SettlementState,SettlementRecord,Modifier,Sex,Ulid}.kt`, `core/.../rules/DerivedValues.kt`
- Existing patterns: `core/.../usecase/*`, `app-android/.../ui/{SettlementScreen,DevelopStepScreen}.kt`, `app-android/.../LanternYearController.kt`
