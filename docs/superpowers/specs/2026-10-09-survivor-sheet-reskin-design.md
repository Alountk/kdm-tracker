# Survivor Sheet Reskin + Atramentum Lantern Theme — Design (Slice 4)

**Status:** Draft (awaiting review)
**Slice:** Reskin the survivor sheet to match the Stitch design, and introduce the app-wide design-system theme layer it sits on.
**Depends on:** the survivor roster/editor/gear-grid slices and the Hunt + Showdown effort (PRs #20–#22). Part 1 of the "apply the Stitch design system" effort; the Settlement, Hunt and Showdown screens follow in later slices.

## Goal

Today the app mounts the **default Material 3 theme** (`KdmTrackerApp.kt:53` — `MaterialTheme {}`), and `SurvivorSheet` is a text dump: a `Column` of `Text` rows. The Stitch project ships a real design system ("Atramentum Lantern") and a generated mobile survivor sheet.

This slice:

1. Builds `KdmTheme`, mapping the design system's tokens onto Material 3 roles (colour, type, shape), and mounts it at the app root — so **every screen inherits it without being edited**.
2. Rebuilds the survivor sheet as the dense, carved-stone sheet the design shows, reusing the existing gear grid.
3. Wires the sheet's controls to the **existing** `UpdateSurvivor` path — no new plumbing.

## Source of truth (validated with the user)

The Stitch design system contains **two contradictory palettes**: the `designMd` brand prose (canvas `#0c0c0e`, gold `#d4af37`, crimson `#991b1b`, "strict 0px radius") and the `namedColors` role set that the generated screens actually render.

Evidence from the downloaded screen (`survivor-sheet.html`, its embedded `tailwind.config`):

```
d4af37 -> 0 occurrences    991b1b -> 0    0c0c0e -> 0
e9c349 -> present          131315 -> present
radius in use: 44x rounded (4px), 27x rounded-none, 9x rounded-lg (8px), 1x rounded-full
```

**Decision: the rendered Tailwind config is authoritative; the `designMd` prose is not.** The sheet is reskinned to what the screen renders, not to the brand narrative.

## Design (validated with the user)

### Fidelity target

Byte-identical reproduction of an HTML/Tailwind design inside Compose is **not achievable** (different text and layout engines: font metrics, sub-pixel rounding, `line-height` as a box in CSS vs. a multiplier in Compose). The agreed success criterion is therefore:

- **Exact token fidelity** — every hex, every size/weight/letter-spacing/leading, every spacing value traced to the design.
- **Equivalent structure and hierarchy**, verified by a side-by-side screenshot comparison.

Not "identical to the binary". This is stated so it is not re-litigated mid-implementation.

### Approach (chosen)

**Token theme on Material 3.** Build the design system as a `KdmTheme` mapping tokens to Material 3 roles; compose the sheet from Material 3 primitives plus a few small custom composables for what Material 3 does not express (1px dividers, tabular numerals, square pips). Rejected alternative: literal CSS reproduction (fights Compose, unreusable, and would contradict the global-theme decision below).

### Theme scope (chosen)

**Global, now.** The theme is mounted at the root and every screen changes appearance in this PR. This works because existing screens style themselves through semantic roles (`MaterialTheme.colorScheme.surface`, `typography.bodySmall`), so they inherit the new tokens **without any per-screen code change** — the code diff stays small while the visual change is app-wide.

### Data scope (chosen)

**Only what the domain backs.** The design's regions that have no domain backing are **out of this slice**:

| Region | Why it is out |
| --- | --- |
| Edad 1–4 / Retiro chips | the domain models only `retired: Boolean`, not four ages |
| Ruleta de Muerte de Ibrahim Igne | not modelled |
| Armaduras rotas | not modelled |
| Acto del Superviviente panel | static rules reference |

Four smaller unbacked elements are also out, under the same ruling:

| Element | Why it is out |
| --- | --- |
| Header narrative subtitle ("Prólogo • Superviviente Fundador") | not modelled |
| Attribute `+0` modifier column ("Base + Modificadores") | only base values are modelled, no modifiers |
| "Derribado" on a wound | `WoundState` has `armor`/`light`/`heavy`, no knocked-down |
| "Afinidades Activas" on the gear grid | no affinity model |

These become follow-up slices. This slice still validates the whole design system, because the colour, type, shape and spacing tokens all appear in the backed regions.

## Scope

### In

- `:core`: two display-rule constants on `Ruleset` + `Core16Ruleset` (`huntXpTrackMax`, `insanityThreshold`) and the derived "insane" predicate.
- `:app-android`: the `KdmTheme` layer (colour, type, shape), bundled fonts, icon mapping; the rebuilt survivor sheet; mounting the theme at the root.

### Out (explicitly deferred)

- The four region gaps and four element gaps above.
- Reskinning the Settlement, Hunt and Showdown screens (they inherit the theme, but their layouts are unchanged).
- Compose screenshot/UI test infrastructure (there is none today; adding Paparazzi/Robolectric is a separate decision).
- Any change to the survivor **editor** screen beyond the "Arma Melé" entry point.
- Any persisted-data change: **no new fields, so no transfer/schema bump.**

## Architecture

### `:core` (additive, no data-model change)

- `Ruleset` gains `huntXpTrackMax: Int` and `insanityThreshold: Int`; `Core16Ruleset` sets `16` and `3`. These are display rules, and the project's standing principle is that rules live in `:core`, never in the UI (as `UpdateSurvivor`'s KDoc states).
- A pure predicate for the mental state (insane once `insanity >= insanityThreshold`).
- **No** new `Survivor` fields, so no transfer version change.

### `:app-android`

- `ui/theme/KdmColors.kt` — the palette as `Color` constants and the role mapping.
- `ui/theme/KdmTypography.kt` — the type scale mapped to Material 3 `Typography`, plus three named extras Material 3 has no role for.
- `ui/theme/KdmShapes.kt` — corner shapes.
- `ui/theme/KdmTheme.kt` — the `KdmTheme { }` composable.
- `ui/theme/KdmIcons.kt` — the ~12 Material Symbols glyphs the sheet uses, as `ImageVector`s.
- `res/font/` — Playfair Display (600, 700, italic 400) and Inter (400, 600, 700), both OFL.
- `ui/SurvivorSheet.kt` — the rebuilt sheet and its pieces: `SheetPanel`, `CounterRow`, `PipTrack`, `AttributeStat`, `HitLocationRow`, `ChipRow`.
- `ui/SurvivorComponents.kt` — the current text-dump `SurvivorSheet` is replaced; `SurvivorCard`/`StatusDot` (the roster) are untouched.
- `KdmTrackerApp.kt` — `MaterialTheme {}` → `KdmTheme {}`.

### Token mapping

Colour (rendered tokens → Material 3 roles):

| Role | Hex | | Role | Hex |
| --- | --- | --- | --- | --- |
| background / surface | `#131315` | | primary | `#ffffff` |
| onBackground / onSurface | `#e5e1e4` | | onPrimary | `#30312c` |
| surfaceContainerLowest | `#0e0e10` | | primaryContainer | `#e4e2db` |
| surfaceContainerLow | `#1b1b1d` | | onPrimaryContainer | `#65655f` |
| surfaceContainer | `#201f21` | | secondary | `#e9c349` |
| surfaceContainerHigh | `#2a2a2c` | | onSecondary | `#3c2f00` |
| surfaceContainerHighest / surfaceVariant | `#353437` | | secondaryContainer | `#af8d11` |
| onSurfaceVariant | `#c8c7be` | | onSecondaryContainer | `#342800` |
| outline | `#919189` | | tertiary | `#ffffff` |
| outlineVariant | `#474741` | | onTertiary | `#690007` |
| inverseSurface | `#e5e1e4` | | tertiaryContainer | `#ffdad6` |
| inverseOnSurface | `#313032` | | onTertiaryContainer | `#b9332e` |
| inversePrimary | `#5f5f59` | | error | `#ffb4ab` |
| surfaceTint | `#c8c6c0` | | onError | `#690005` |
| | | | errorContainer | `#93000a` |
| | | | onErrorContainer | `#ffdad6` |

Typography: the design scale has 11 steps; Material 3 has 15 fixed roles. The closest roles are filled (`headlineLarge` → 26/32, `headlineMedium` → 22/28, `headlineSmall` → 19/24, `titleLarge` → 16/22, `bodyLarge` → 15/22, `bodyMedium` → 13/19, `bodySmall` → 11/16, `displaySmall` → 38/44), and the three with no role (`statDisplay` 24/28 `-0.02em`, `labelCaps` 10/14 `0.08em`/700, `ruleQuote` 13/18) are exposed as named extras rather than forced into a wrong role.

Shapes: `rounded` (4px) as the default, `rounded-lg` (8px) as large, pills via `RoundedCornerShape(percent = 50)` — matching the render, **not** the prose's "strict 0px".

Icons: the design renders its icons with the **Material Symbols Outlined** web font. Compose's `material-icons-extended` is a *different, older* icon set — the glyphs do not match — and the project currently has **no icon dependency at all**. Rather than approximate (wrong glyphs) or bundle a multi-megabyte variable font for ~12 icons, the dozen glyphs the sheet needs are added as local `ImageVector`s derived from the official Material Symbols set (Apache-2.0). This keeps the glyphs faithful with no heavy dependency and no R8 dependency. Any glyph that has to be substituted is recorded in the apply-progress.

## Data flow

**Read** — `UiState` already exposes everything the sheet needs (`survivors`, `survivalLimit`, `settlementName`, `lanternYear`, `population`, `deaths`). No changes.

**Write** — the path already exists end to end:

```
SettlementScreen.onSaveSurvivor
  → LanternYearViewModel.updateSurvivor
    → LanternYearController.updateSurvivor
      → UpdateSurvivor.run(record, survivor)   // validates non-negatives in :core
```

Each control builds `survivor.copy(...)` and calls `onSaveSurvivor`. **No new use cases and no controller changes.**

| Control | Mutation |
| --- | --- |
| Survival −/+ | `survival ± 1`, clamped to `0..survivalLimit` |
| Insanity −/+ | `insanity ± 1`, clamped at `>= 0` |
| Hunt-XP pips | `huntXp + 1`, capped at `huntXpTrackMax` |
| Armour −/+ per location | `wounds[loc].copy(armor ± 1)`, clamped at `>= 0` |
| Light / heavy wound | toggle `light` / `heavy` on the location |
| Token chip | toggle the token in `tokens` |
| Gear grid 3×3 | delegated to the existing `GearGrid` |
| Arma Melé | opens `SurvivorEditorScreen` |

**Derived (never persisted, computed at render):** mental state (Cuerdo/Loco), "no puede gastar" (`survival == survivalLimit`), the survival limit, and the hunt-XP cap — all from the `ruleset`, not hard-coded.

**Local state:** none. The screen is stateless; `GearGrid` keeps its own selection state.

**Errors:** `UpdateSurvivor` can return `Blocked`; the controller already surfaces it in `actionError`, which the sheet displays like the other screens.

## Composition

A scrollable column of **panels**. Each panel is a `surfaceContainerLow` (`#1b1b1d`) container with a 1px `#2a2a30` border, a 4px radius and its title in `labelCaps`. No shadows — hierarchy is tonal, as the design specifies.

Sections built: settlement header · survivor header (name, initial, gear entry) · Hunt XP track · Survival · Insanity + mental state · Primary attributes (6) · Hit locations (armour / light / heavy, ×5) · Gear grid 3×3 · Melee weapon.

## Testing

The project has **no Compose UI test infrastructure** (`app-android/src/test/` holds JVM `kotlin.test` suites that pin derived views on `UiState`, as `DevelopViewsTest` does). Adding screenshot testing is out of scope.

1. **`:core` (commonTest, TDD)** — the two new rule constants and their `Core16Ruleset` values; the derived "insane" predicate at the threshold boundary (2, 3, 4).
2. **`:app-android` (JVM, TDD)**:
   - `KdmThemeTest` — pins every `ColorScheme` role's hex, every typography step's size / weight / letter-spacing / leading, and the shapes. **This is what makes token fidelity enforceable rather than aspirational**: a token edit that drifts from the design fails the suite.
   - `SurvivorSheetViewsTest` — pins the sheet's derived values (filled Hunt-XP pips, "no puede gastar", mental-state label, per-location armour, token chips), following the `DevelopViewsTest` pattern.
   - `SurvivorSheetEditsTest` — pins each control's mutation as a pure function (armour ±1 floored at 0, wound toggles, token toggle, survival clamped to the limit).
3. **Manual visual acceptance** — render the sheet on the emulator, screenshot it and compare side by side against the Stitch render. This is the only step that validates "looks the same", and it is manual.

**Gate:** the full suite (`:core:jvmTest`, `:data:jvmTest`, `:app-android:testDebugUnitTest`, `:server:test`) plus `spotlessCheck` green; PR against `main`; squash-merge — the same flow as the previous slices.

## Risks

- **Every screen changes appearance in this PR.** Existing screens inherit the dark scheme; a screen whose contrast suffers must be reported, not silently tolerated. Rollback is a single revert of the theme mount.
- **Text metrics differ from the browser.** Sizes, weights and spacing are exact; line boxes may differ by a pixel or two. Accepted and stated up front.
- **Token drift.** Mitigated by `KdmThemeTest`.
- **Font bundling increases the APK** (two families, five faces). Accepted; both are OFL.
- **Icon coverage.** Glyphs are hand-added vectors from the official Material Symbols set; any glyph that has to be substituted is recorded rather than silently approximated.
- **Slice size against the 400-line review budget.** This slice carries two deliverables (the theme layer plus ruleset constants, and the dense sheet rebuild) and will likely exceed the project's 400-changed-line PR policy. If the task-phase forecast crosses it, split into two PRs — theme + constants first, then the sheet — rather than seeking a `size:exception`.

## References

- Stitch project `projects/4909212909494507782`, design system "Atramentum Lantern", screen `310b1d1ff8994d619e67fda164f35f36` ("Ficha de Superviviente").
- `app-android/src/main/kotlin/io/kdmtracker/app/KdmTrackerApp.kt` (theme mount), `ui/SurvivorComponents.kt` (current sheet), `ui/GearGrid.kt` (reused).
- `core/src/commonMain/kotlin/io/kdmtracker/core/domain/Survivor.kt` (`WoundState`, `Attributes`, `HitLocation`), `ruleset/Ruleset.kt` + `Core16Ruleset.kt`.
- `core/src/commonMain/kotlin/io/kdmtracker/core/usecase/UpdateSurvivor.kt` (the write path).
