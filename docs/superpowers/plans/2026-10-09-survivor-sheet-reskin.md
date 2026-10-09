# Survivor Sheet Reskin + Atramentum Lantern Theme — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Introduce the "Atramentum Lantern" design system as a Material 3 theme mounted app-wide, then rebuild the survivor sheet to match the Stitch design.

**Architecture:** A `KdmTheme` maps the design's rendered tokens (colour, type, shape) onto Material 3 roles so every existing screen inherits the new look with no per-screen edit. The sheet is then rebuilt as a column of tonal panels from Material 3 primitives, with its display rules living in `:core` and its controls writing through the existing `UpdateSurvivor` path.

**Tech Stack:** Kotlin 2.3.21, Compose Multiplatform 1.12.1, Material 3, Kotlin Multiplatform (`:core` pure, `:app-android` Compose), `kotlin.test` JVM suites.

**Spec:** `docs/superpowers/specs/2026-10-09-survivor-sheet-reskin-design.md`

## Global Constraints

- **Copy language:** the sheet's labels are **Spanish**, copied from the design ("Supervivencia", "Locura (Cerebro)", "Atributos Primarios", "Localizaciones de Impacto"). The rest of the app stays English in this slice.
- **Token source of truth:** the design's rendered Tailwind config, **not** the `designMd` prose. Gold is `#e9c349` (not `#d4af37`), the canvas is `#131315` (not `#0c0c0e`), and the radius is 4px/8px/pill (not 0px).
- **Rules live in `:core`, never in the UI.** Thresholds and caps come from `Ruleset`.
- **No persisted-data change.** No new `Survivor` fields, therefore **no transfer/schema bump**.
- `minSdk` is **24**; variable-font weight selection (`FontVariation`) applies on **API 26+** and falls back to the font's default instance below that. This is accepted and must be noted in the apply-progress.
- Run `./gradlew spotlessApply` before every commit (it enforces 120 columns and no unused imports). Conventional Commits, no AI attribution.
- **Deferred (out of scope):** the four region gaps (Edad 1–4/Retiro, Ruleta de Muerte, Armaduras rotas, Acto del Superviviente) and five small ones (header narrative subtitle, attribute `+0` modifier column, "Derribado", gear "Afinidades Activas", and the hunt-XP milestone glyphs inside the track).

## Review Focus

The input classes most likely to bite a real user, each pinned by a test in its owning task:

1. A survivor whose `wounds` map is **empty** — all five hit-location rows must still render (armour 0), not disappear.
2. `insanity` exactly at the boundary (2 vs 3) — the mental-state label must flip at 3, not before.
3. `survival` already at `survivalLimit` — "No puede gastar" shows and increments clamp; they never exceed the limit.
4. `huntXp` already at `huntXpTrackMax` — the track must not overflow and the increment is a no-op.
5. A gear list whose size differs from the 3×3 grid — the sheet must always offer 9 slots regardless of how many are filled.

---

## PR boundary 1 — Tasks 1–2 (rules + theme foundation)

### Task 1: `:core` display rules and the mental-state predicate

**Files:**
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/ruleset/Ruleset.kt`
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/ruleset/Core16Ruleset.kt`
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/domain/Survivor.kt`
- Create: `core/src/commonTest/kotlin/io/kdmtracker/core/ruleset/DisplayRulesTest.kt`

**Interfaces:**
- Produces: `Ruleset.huntXpTrackMax: Int`, `Ruleset.insanityThreshold: Int`, `Survivor.isInsane(threshold: Int): Boolean`

- [ ] **Step 1: Write the failing test**

```kotlin
class DisplayRulesTest {
    @Test fun coreRulesetPinsTheDisplayRules() {
        assertEquals(16, Core16Ruleset.huntXpTrackMax)
        assertEquals(3, Core16Ruleset.insanityThreshold)
    }

    @Test fun isInsaneFlipsExactlyAtTheThreshold() {
        val survivor = Survivor(id = Ulid.generate(1L) { 0 })
        assertFalse(survivor.copy(insanity = 2).isInsane(3))
        assertTrue(survivor.copy(insanity = 3).isInsane(3))
        assertTrue(survivor.copy(insanity = 4).isInsane(3))
    }

    @Test fun overridesCarryTheDisplayRulesThrough() {
        val overridden = Core16Ruleset.applyOverrides(RulesetOverrides(survivalLimit = 99))
        assertEquals(16, overridden.huntXpTrackMax)
        assertEquals(3, overridden.insanityThreshold)
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :core:jvmTest --tests "io.kdmtracker.core.ruleset.DisplayRulesTest"`
Expected: FAIL — unresolved reference `huntXpTrackMax`.

- [ ] **Step 3: Implement.**

Add `val huntXpTrackMax: Int` and `val insanityThreshold: Int` to the `Ruleset` interface (documented like the neighbouring values, quoting the rulebook). Override both in `Core16Ruleset` with `16` and `3`. **`OverrideRuleset` (a private data class in `Ruleset.kt`) also implements `Ruleset`, so add both as constructor properties there and pass them through from `applyOverrides` as `huntXpTrackMax = huntXpTrackMax, insanityThreshold = insanityThreshold`** — omitting this step breaks the build. Add `isInsane` to `Survivor.kt` as a pure one-liner (`insanity >= threshold`).

- [ ] **Step 4: Run it to verify it passes**

Run: `./gradlew :core:jvmTest --tests "io.kdmtracker.core.ruleset.DisplayRulesTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add core/src/commonMain/kotlin/io/kdmtracker/core/ruleset core/src/commonMain/kotlin/io/kdmtracker/core/domain/Survivor.kt core/src/commonTest/kotlin/io/kdmtracker/core/ruleset/DisplayRulesTest.kt
git commit -m "feat(core): add the hunt-xp cap and insanity threshold rules"
```

### Task 2: The Atramentum theme layer, mounted app-wide

**Files:**
- Create: `app-android/src/main/kotlin/io/kdmtracker/app/ui/theme/KdmColors.kt`
- Create: `app-android/src/main/kotlin/io/kdmtracker/app/ui/theme/KdmTypography.kt`
- Create: `app-android/src/main/kotlin/io/kdmtracker/app/ui/theme/KdmShapes.kt`
- Create: `app-android/src/main/kotlin/io/kdmtracker/app/ui/theme/KdmTheme.kt`
- Create: `app-android/src/main/res/font/playfair_display.ttf`, `playfair_display_italic.ttf`, `inter.ttf`, `inter_italic.ttf`
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/KdmTrackerApp.kt:53` (`MaterialTheme {` → `KdmTheme {`)
- Create: `app-android/src/test/kotlin/io/kdmtracker/app/KdmThemeTest.kt`

**Interfaces:**
- Produces: `KdmTheme(content: @Composable () -> Unit)`; `KdmScheme: ColorScheme`; `KdmTypography: Typography`; `KdmType.statDisplay` / `.labelCaps` / `.ruleQuote: TextStyle`; `KdmShapes: Shapes`; `KdmFontFamilies` (`playfair`, `inter`).

- [ ] **Step 1: Fetch the fonts into `res/font/`.**

Download the four **variable** TTFs (verified reachable) and rename to valid Android resource names:
`https://raw.githubusercontent.com/google/fonts/main/ofl/playfairdisplay/PlayfairDisplay%5Bwght%5D.ttf` → `playfair_display.ttf`;
`.../PlayfairDisplay-Italic%5Bwght%5D.ttf` → `playfair_display_italic.ttf`;
`https://raw.githubusercontent.com/google/fonts/main/ofl/inter/Inter%5Bopsz%2Cwght%5D.ttf` → `inter.ttf`;
`.../Inter-Italic%5Bopsz%2Cwght%5D.ttf` → `inter_italic.ttf`.
Both families are OFL; keep the licence text in the apply-progress.

- [ ] **Step 2: Write the failing test**

```kotlin
class KdmThemeTest {
    @Test fun colourRolesCarryTheRenderedTokens() {
        assertEquals(Color(0xFF131315), KdmScheme.background)
        assertEquals(Color(0xFFE5E1E4), KdmScheme.onSurface)
        assertEquals(Color(0xFF1B1B1D), KdmScheme.surfaceContainerLow)
        assertEquals(Color(0xFF2A2A2C), KdmScheme.surfaceContainerHigh)
        assertEquals(Color(0xFFE9C349), KdmScheme.secondary)
        assertEquals(Color(0xFF919189), KdmScheme.outline)
        assertEquals(Color(0xFF474741), KdmScheme.outlineVariant)
    }

    @Test fun typeScaleCarriesTheRenderedSteps() {
        val headline = KdmTypography.headlineLarge
        assertEquals(26.sp, headline.fontSize); assertEquals(32.sp, headline.lineHeight)
        assertEquals(FontWeight.SemiBold, headline.fontWeight)
        assertEquals(10.sp, KdmType.labelCaps.fontSize)
        assertEquals(0.08.em, KdmType.labelCaps.letterSpacing)
    }

    @Test fun shapesMatchTheRenderedRadii() {
        assertEquals(4.dp, KdmShapes.small)
        assertEquals(8.dp, KdmShapes.large)
    }
}
```

- [ ] **Step 3: Run it to verify it fails**

Run: `./gradlew :app-android:testDebugUnitTest --tests "io.kdmtracker.app.KdmThemeTest"`
Expected: FAIL — unresolved reference `KdmScheme`.

- [ ] **Step 4: Implement the theme.**

`KdmColors.kt`: the ~30 `Color(0xFF…)` constants and `KdmScheme = darkColorScheme(...)` mapping every role exactly as the spec's table lists, including the `surfaceContainerLowest/Low/Container/High/Highest`. `KdmTypography.kt`: the `KdmFontFamilies` built by registering the variable TTFs once per weight via `Font(R.font.x, weight = …, variationSettings = FontVariation.Settings(FontVariation.weight(n)))`; the `Typography` mapped as the spec lists (`displaySmall` 38/44/700, `headlineLarge` 26/32/600, `headlineMedium` 22/28/600, `headlineSmall` 19/24/600, `titleLarge` 16/22/600/`0.02em`, `bodyLarge` 15/22/400, `bodyMedium` 13/19/400, `bodySmall` 11/16/400) plus `KdmType.statDisplay` (24/28/700/`-0.02em`), `KdmType.labelCaps` (10/14/700/`0.08em`), `KdmType.ruleQuote` (13/18/400/Playfair). `KdmShapes.kt`: `extraSmall`/`small` 4.dp, `medium`/`large`/`extraLarge` 8.dp. `KdmTheme.kt`: `MaterialTheme(colorScheme = KdmScheme, typography = KdmTypography, shapes = KdmShapes, content = content)`.

- [ ] **Step 5: Mount it**

`KdmTrackerApp.kt:53`: replace `MaterialTheme {` with `KdmTheme {` (same closing brace).

- [ ] **Step 6: Run the suite and verify it passes**

Run: `./gradlew :app-android:testDebugUnitTest`
Expected: PASS — including the pre-existing suites, which now read the new scheme.

- [ ] **Step 7: Check no screen broke, then commit**

Run: `./gradlew spotlessApply :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck`

```bash
git add app-android/src/main app-android/src/test
git commit -m "feat(app): add the Atramentum Lantern theme and mount it app-wide"
```

---

## PR boundary 2 — Tasks 3–5 (the survivor sheet)

### Task 3: The sheet's pure model and edits

**Files:**
- Create: `app-android/src/main/kotlin/io/kdmtracker/app/ui/SurvivorSheetModel.kt`
- Create: `app-android/src/main/kotlin/io/kdmtracker/app/ui/SurvivorSheetEdits.kt`
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/UiState.kt` (add `huntXpTrackMax: Int = 0`, `insanityThreshold: Int = 0`)
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/LanternYearController.kt` (populate both from `ruleset` in the `UiState` build)
- Create: `app-android/src/test/kotlin/io/kdmtracker/app/SurvivorSheetViewsTest.kt`
- Create: `app-android/src/test/kotlin/io/kdmtracker/app/SurvivorSheetEditsTest.kt`

**Interfaces:**
- Consumes: `Ruleset.huntXpTrackMax`, `Ruleset.insanityThreshold`, `Survivor.isInsane` (Task 1).
- Produces: `SurvivorSheetModel.from(survivor, survivalLimit, huntXpTrackMax, insanityThreshold): SurvivorSheetModel` with `name`, `initial`, `huntXp`, `huntXpMax`, `survival`, `survivalLimit`, `canSpendSurvival`, `insanity`, `mentalState: MentalState` (`SANE`/`INSANE`), `attributes: List<AttributeRow>`, `hitLocations: List<HitLocationRowModel>` (all 5, armour defaulted), `tokenChips: List<TokenChip>`; and `SurvivorSheetEdits` pure functions `survival(survivor, limit, delta)`, `insanity(survivor, delta)`, `huntXp(survivor, max, delta)`, `armor(survivor, location, delta)`, `toggleLight(survivor, location)`, `toggleHeavy(survivor, location)`, `toggleToken(survivor, token)`; and `SurvivorSheetModel.gearSlots: Int` sourced from `AssignGear.MAX_GEAR_SLOTS`.

- [ ] **Step 1: Write the failing tests**

`SurvivorSheetViewsTest` — pin the Review Focus derivations: an empty `wounds` map still yields all five `hitLocations`; `copy(insanity = 2)` → `SANE` and `copy(insanity = 3)` → `INSANE`; `survival == limit` → `canSpendSurvival == false`; `gearSlots == 9` for a survivor carrying 0 and for one carrying 9.
`SurvivorSheetEditsTest` — pin the clamping: `survival(s, limit = 2, +1)` from `2` stays `2` and from `0` with `-1` stays `0`; `huntXp(s, max = 16, +1)` from `16` stays `16`; `armor(s, HEAD, -1)` from `0` stays `0`; each toggle is an involution.

- [ ] **Step 2: Run to verify they fail**

Run: `./gradlew :app-android:testDebugUnitTest --tests "*SurvivorSheet*"`
Expected: FAIL — unresolved references.

- [ ] **Step 3: Implement the model and the edits, and wire `UiState`.**

The model is a pure function of its arguments (no `Ruleset` parameter, so tests construct values directly). The edits return a modified `Survivor`; each clamps per the tests. `LanternYearController` fills `huntXpTrackMax`/`insanityThreshold` from its `ruleset` alongside the existing `survivalLimit`.

- [ ] **Step 4: Run to verify they pass**

Run: `./gradlew :app-android:testDebugUnitTest --tests "*SurvivorSheet*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app-android/src/main/kotlin/io/kdmtracker/app/ui/SurvivorSheetModel.kt app-android/src/main/kotlin/io/kdmtracker/app/ui/SurvivorSheetEdits.kt app-android/src/main/kotlin/io/kdmtracker/app/UiState.kt app-android/src/main/kotlin/io/kdmtracker/app/LanternYearController.kt app-android/src/test/kotlin/io/kdmtracker/app
git commit -m "feat(app): add the survivor sheet model and edit operations"
```

### Task 4: The Material Symbols font and codepoint map

**Files:**
- Create: `app-android/src/main/res/font/material_symbols_outlined.ttf`
- Create: `app-android/src/main/kotlin/io/kdmtracker/app/ui/theme/KdmSymbols.kt`
- Create: `app-android/src/test/kotlin/io/kdmtracker/app/KdmSymbolsTest.kt`

**Interfaces:**
- Produces: `KdmSymbols.family: FontFamily`; `KdmSymbols.glyph(name: String): String` returning the single-codepoint string; the 29 names are fixed (below).

- [ ] **Step 1: Fetch the font.**

Download the Material Symbols Outlined variable TTF (Apache-2.0) into `res/font/material_symbols_outlined.ttf`. Do **not** subset it (deliberately out of scope).

- [ ] **Step 2: Write the failing test**

Assert `KdmSymbols.glyph("<name>") == "\uXXXX"` for a representative sample (`flare` `0xe3e4`, `skull` `0xf89a`, `psychology` `0xea4a`, `sports_mma` `0xea2c`, `explore` `0xe87a`) and that `glyph` throws/returns a fallback for an unmapped name.

- [ ] **Step 3: Run to verify it fails**, then implement the map with the verified codepoints:

`flare 0xe3e4 · swords 0xf889 · light_mode 0xe518 · group 0xea21 · skull 0xf89a · person 0xf0d3 · chevron_left 0xe5cb · grid_view 0xe9b0 · psychology 0xea4a · directions_walk 0xe536 · target 0xe719 · fitness_center 0xeb43 · airline_stops 0xe7d0 · casino 0xeb40 · bolt 0xea0b · security 0xe32a · face 0xf008 · front_hand 0xe769 · accessibility_new 0xe92c · roller_shades 0xec12 · airline_seat_legroom_extra 0xe633 · shield 0xe9e0 · handyman 0xf10b · sports_mma 0xea2c · directions_run 0xe566 · emergency_heat 0xf15d · fort 0xeaad · shield_person 0xf650 · explore 0xe87a`

- [ ] **Step 4: Run to verify it passes, then commit**

```bash
git add app-android/src/main/res/font/material_symbols_outlined.ttf app-android/src/main/kotlin/io/kdmtracker/app/ui/theme/KdmSymbols.kt app-android/src/test/kotlin/io/kdmtracker/app/KdmSymbolsTest.kt
git commit -m "feat(app): bundle the Material Symbols glyphs for the sheet"
```

### Task 5: The rebuilt survivor sheet

**Files:**
- Create: `app-android/src/main/kotlin/io/kdmtracker/app/ui/SurvivorSheet.kt`
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/ui/SurvivorComponents.kt` (delete the text-dump `SurvivorSheet`; leave `SurvivorCard`/`StatusDot`/`RosterFilterChips` untouched)
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/KdmTrackerApp.kt` (pass the sheet's callbacks through `SettlementScreen`)

**Interfaces:**
- Consumes: `SurvivorSheetModel`, `SurvivorSheetEdits` (Task 3), `KdmSymbols` (Task 4), `KdmTheme` roles (Task 2), `GearGrid` (existing), `onSaveSurvivor: (Survivor) -> Unit` (existing).
- Produces: `SurvivorSheet(survivor, survivalLimit, huntXpTrackMax, insanityThreshold, settlementGear, onSave, onEditSheet)`.

- [ ] **Step 1: Build the pieces.**

`SheetPanel(title) { }` (a `surfaceContainerLow` container, 1px `surfaceContainerHigh`-ish `#2a2a30` border, 4.dp radius, `labelCaps` title), `CounterRow` (−, value, +), `PipTrack(filled, total)` (**16 cells of 16.dp, zero radius, `gap-1`, horizontally scrollable**; filled cells `primary`, the rest `surfaceContainerHigh`), `AttributeStat`, `HitLocationRow`, `ChipRow`. Then the screen: a scrollable column of panels.

- [ ] **Step 2: Use the design's exact Spanish copy.**

Section titles and labels: `"PE de Caza (Experiencia)"` / `"$huntXp / $max PE"`; `"Supervivencia"` / `"Límite: $limit"` / `"No puede gastar"` / chips `"Esquivar"`, `"Sprint"`, `"Forzar"`; `"Locura (Cerebro)"` / `"Estado Mental"` / `"Cuerdo"`·`"Loco"` / `"Si locura es 3+ estás loco."`; `"Atributos Primarios"` / `"Base + Modificadores"`; `"Localizaciones de Impacto"` / `"Armadura / Daño / Lesión"`; `"Cuadrícula de Equipo (3x3)"`; `"Arma Melé"`; `"Equipo"`.
Attribute labels use the design's Spanish abbreviations: `MOV`, `PREC`, `FZA`, `EVA`, `SUE`, `VEL` (mapped from `movement`/`accuracy`/`strength`/`evasion`/`luck`/`speed`).
Hit-location labels in the design's order: `Cabeza` (HEAD), `Brazos` (ARMS), `Torso` (TORSO), `Cintura` (WAIST), `Piernas` (LEGS).

- [ ] **Step 3: Wire every control through the existing write path.**

Each control computes `SurvivorSheetEdits.x(...)` and calls `onSave`. The "Arma Melé" row calls `onEditSheet`. No new use case and no controller change.

- [ ] **Step 4: Run the suite**

Run: `./gradlew spotlessApply :app-android:testDebugUnitTest`
Expected: PASS.

- [ ] **Step 5: Visual acceptance on the emulator (manual, required).**

Install with `./gradlew :app-android:installDebug`, open a survivor's sheet, screenshot it, and compare side by side against the Stitch render for screen `310b1d1ff8994d619e67fda164f35f36`. Record the result and any accepted deviation in the apply-progress. Also walk one screen of each phase to confirm the app-wide theme did not break contrast.

- [ ] **Step 6: Commit**

```bash
git add app-android/src/main
git commit -m "feat(app): rebuild the survivor sheet on the Atramentum theme"
```

---

## Final steps

- [ ] Whole-branch review over the base, then PRs per the boundaries above, squash-merged. If the task-phase forecast crosses 400 changed lines, PR 1 and PR 2 stay separate.
- [ ] Record the accepted deviations (API-24 font fallback, deferred gaps, any glyph substitution) in the apply-progress and the PR body.
