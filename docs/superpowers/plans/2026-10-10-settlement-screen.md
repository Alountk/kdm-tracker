# Settlement Screen + house-rule knobs Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the Settlement (Asentamiento) view to the design — the summary block, the per-step frame and the Develop step's panels — and move the rule values that screen shows into the ruleset so a house rule can change them.

**Architecture:** The ruleset gains the knobs the screen needs (and two that were missed), the step catalog moves to the design's Spanish names, the app gains a `ui/settlement/` package of small composables + pure models, and `SettlementScreen` composes them around the existing per-step bodies. No rule value is ever a literal in the UI.

**Tech Stack:** Kotlin 2.3.21, Compose Multiplatform 1.12.1, Material 3, `kotlin.test` JVM suites, `fontTools` (`pyftsubset`) for the icon subset.

**Spec:** `docs/superpowers/specs/2026-10-10-settlement-screen-design.md`

## Global Constraints

- **The render is the source of truth** for visuals: step 9's chip is `#8E1214`; the resource colours are Hueso `primary`, Órgano `#ffb4ac` (`tertiary-fixed-dim`), Pellejo `secondary`, Chatarra `outline`.
- **Copy is the design's Spanish**: the eleven step titles are exactly `1 Preparación · 2 Supervivientes Regresan · 3 Ganar Esfuerzos · 4 Actualizar Línea de Tiempo · 5 Actualizar Contador de Muertes · 6 Comprobar Hitos · 7 Desarrollo · 8 Preparar Supervivientes · 9 Enfrentamiento Especial · 10 Anotar y Almacenar · 11 El Fin del Principio`.
- **No rule value is a UI literal.** Every value the screen shows that is a rule comes from the `Ruleset` (through overrides); the screen never hardcodes `2`, `1`, `16` or `3`.
- **Render exactly, leave unbacked controls inert.** The year title/narrative, the year-1 rule and `Restaurar Esfuerzos` are drawn and disabled, never faked or hidden.
- **The ten steps no design covers keep their current bodies.** Do not restyle them by inference.
- **Visual changes carry an approval gate.** For any visual change in this slice: install, leave the app signed in on the exact screen, measure objectively, **present it, and stop. No commit, push, PR or merge until the user approves the rendered result.**
- Run `./gradlew spotlessApply` before every commit (120 columns, no unused imports). Conventional Commits, no AI attribution.

## Review Focus

The failure modes the spec implies but its prose does not exercise, each pinned by a test in its owning task:

1. **A resource the settlement has none of** — its cell must still render, showing `0`, not an empty cell or a missing column.
2. **A house-rule override on the new knobs** — the Innovate text must follow it (`Robar 4, elegir 1`), proving the value is data and not a literal.
3. **Step 11, the last one** — the advance label must not read `Avanzar al Paso 12`; it wraps or commits.
4. **Every step 1..11** — the frame renders for all of them, including the ten whose bodies are unchanged.
5. **The two newly bundled glyphs** — `format_h4` and `grid_4x4` resolve to their codepoints, so the summary draws a bone and a grid instead of tofu.

---

### Task 1: Regenerate the icon subset with the glyphs the extraction dropped

**Files:**
- Modify: `app-android/src/main/res/font/material_symbols_outlined.ttf`
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/ui/theme/KdmSymbols.kt`
- Modify: `app-android/src/test/kotlin/io/kdmtracker/app/KdmSymbolsTest.kt`

**Interfaces:**
- Produces: `KdmSymbols.glyph("format_h4")`, `KdmSymbols.glyph("grid_4x4")` (and the 66 existing names, unchanged).

- [ ] **Step 1: Write the failing test**

```kotlin
    @Test
    fun theGlyphsTheExtractionDroppedAreBundled() {
        assertEquals("\uF860", KdmSymbols.glyph("format_h4"))
        assertEquals("\uF016", KdmSymbols.glyph("grid_4x4"))
    }

    @Test
    fun everyDesignedGlyphIsMapped() {
        assertEquals(68, KdmSymbols.glyphCount)
    }
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :app-android:testDebugUnitTest --tests "io.kdmtracker.app.KdmSymbolsTest"`
Expected: FAIL — `KdmSymbols.glyph` throws `IllegalArgumentException: unmapped Material Symbol: format_h4`, and `glyphCount` is unresolved.

- [ ] **Step 3: Re-extract with a digit-tolerant pattern, then rebuild the font and the map.**

The previous extraction used `[a-z_]+`, which cannot match a name containing a digit. Re-extract the union across the four screens with `[a-z0-9_]+` (69 names), keep the 68 that exist in Google's codepoints file, and subset:

```bash
python3 -m venv /tmp/fontvenv && /tmp/fontvenv/bin/pip install fonttools
/tmp/fontvenv/bin/pyftsubset <full MaterialSymbolsOutlined.ttf> \
  --unicodes=<the 68 codepoints as U+XXXX,...> \
  --output-file=app-android/src/main/res/font/material_symbols_outlined.ttf --drop-tables+=DSIG
```

Add the two entries (`"format_h4" to 0x{F860}`, `"grid_4x4" to 0x{F016}`) and expose `KdmSymbols.glyphCount` (the map's `size`). `drive_file_stream`, which the design names for its Construct buttons, **does not exist in the font** — do not add it; those call sites use `handyman`.

- [ ] **Step 4: Run it to verify it passes**

Run: `./gradlew :app-android:testDebugUnitTest --tests "io.kdmtracker.app.KdmSymbolsTest"`
Expected: PASS — all 68 names resolve to their codepoints.

- [ ] **Step 5: Commit**

```bash
git add app-android/src/main/res/font/material_symbols_outlined.ttf app-android/src/main/kotlin/io/kdmtracker/app/ui/theme/KdmSymbols.kt app-android/src/test/kotlin/io/kdmtracker/app/KdmSymbolsTest.kt
git commit -m "fix(app): bundle the icon glyphs the extraction dropped"
```

### Task 2: The house-rule knobs

**Files:**
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/ruleset/Ruleset.kt`
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/ruleset/Core16Ruleset.kt`
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/ruleset/RulesetOverrides.kt`
- Modify: `core/src/commonTest/kotlin/io/kdmtracker/core/ruleset/DisplayRulesTest.kt`

**Interfaces:**
- Produces: `Ruleset.innovationDraw: Int` (2), `Ruleset.innovationKeep: Int` (1); `RulesetOverrides.innovationDraw`/`.innovationKeep`/`.huntXpTrackMax`/`.insanityThreshold` (all `Int? = null`).

- [ ] **Step 1: Write the failing test**

```kotlin
    @Test
    fun coreRulesetPinsTheInnovationDraw() {
        assertEquals(2, Core16Ruleset.innovationDraw)
        assertEquals(1, Core16Ruleset.innovationKeep)
    }

    @Test
    fun everyHouseRuleKnobSurvivesAnOverride() {
        val overridden =
            Core16Ruleset.applyOverrides(
                RulesetOverrides(
                    innovationDraw = 4,
                    innovationKeep = 1,
                    huntXpTrackMax = 20,
                    insanityThreshold = 5,
                ),
            )

        assertEquals(4, overridden.innovationDraw)
        assertEquals(1, overridden.innovationKeep)
        assertEquals(20, overridden.huntXpTrackMax)
        assertEquals(5, overridden.insanityThreshold)
    }

    @Test
    fun anUntouchedKnobKeepsTheOfficialValue() {
        val overridden = Core16Ruleset.applyOverrides(RulesetOverrides(survivalLimit = 9))

        assertEquals(2, overridden.innovationDraw)
        assertEquals(16, overridden.huntXpTrackMax)
    }
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :core:jvmTest --tests "io.kdmtracker.core.ruleset.DisplayRulesTest"`
Expected: FAIL — unresolved `innovationDraw` / `RulesetOverrides(innovationDraw = …)`.

- [ ] **Step 3: Implement the five places.**

Add `innovationDraw`/`innovationKeep` to the `Ruleset` interface (documented with the rulebook's 2/1) and to `Core16Ruleset` as `2`/`1`; add all four knobs to `RulesetOverrides`; pass them through in `applyOverrides`'s `OverrideRuleset(...)` construction **and** declare them on `OverrideRuleset` — omitting either half is the mistake this project already made once, and it fails to compile.

- [ ] **Step 4: Run it to verify it passes**

Run: `./gradlew :core:jvmTest --tests "io.kdmtracker.core.ruleset.DisplayRulesTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add core/src/commonMain/kotlin/io/kdmtracker/core/ruleset core/src/commonTest/kotlin/io/kdmtracker/core/ruleset/DisplayRulesTest.kt
git commit -m "feat(core): make the innovation draw and the display rules house-rulable"
```

### Task 3: The step catalog in the design's Spanish

**Files:**
- Modify: `core/src/commonMain/kotlin/io/kdmtracker/core/rules/SettlementStepCatalog.kt`
- Create/Modify: `core/src/commonTest/kotlin/io/kdmtracker/core/rules/SettlementStepCatalogTest.kt`

**Interfaces:**
- Consumes: `SettlementStep(number, title, summary, type)`.
- Produces: the eleven titles are the design's Spanish names; the summaries are the Spanish translation of the existing English text.

- [ ] **Step 1: Write the failing test**

```kotlin
    @Test
    fun theStepsCarryTheDesignsSpanishNames() {
        assertEquals(
            listOf(
                "Preparación", "Supervivientes Regresan", "Ganar Esfuerzos", "Actualizar Línea de Tiempo",
                "Actualizar Contador de Muertes", "Comprobar Hitos", "Desarrollo", "Preparar Supervivientes",
                "Enfrentamiento Especial", "Anotar y Almacenar", "El Fin del Principio",
            ),
            SettlementStepCatalog.ALL.map { it.title },
        )
    }

    @Test
    fun everyStepStillCarriesASummary() {
        assertEquals(11, SettlementStepCatalog.ALL.size)
        assertTrue(SettlementStepCatalog.ALL.all { it.summary.isNotBlank() })
    }
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :core:jvmTest --tests "*SettlementStepCatalogTest"`
Expected: FAIL — the titles are still the English ones (`Set Up the Settlement`, …).

- [ ] **Step 3: Replace the eleven titles with the design's names and translate the summaries.**

The titles are exactly the list in the test. Each summary is a faithful Spanish rendering of its current English text — no new rules, no invented content. Update any other `:core` test that asserts an English step title.

- [ ] **Step 4: Run the module**

Run: `./gradlew :core:jvmTest`
Expected: PASS (the catalog's consumers move with it).

- [ ] **Step 5: Commit**

```bash
git add core/src/commonMain/kotlin/io/kdmtracker/core/rules/SettlementStepCatalog.kt core/src/commonTest/kotlin/io/kdmtracker/core/rules
git commit -m "feat(core): name the settlement steps as the design does"
```

### Task 4: The settlement summary

**Files:**
- Create: `app-android/src/main/kotlin/io/kdmtracker/app/ui/settlement/SettlementSummaryModel.kt`
- Create: `app-android/src/main/kotlin/io/kdmtracker/app/ui/settlement/SettlementSummary.kt`
- Create: `app-android/src/test/kotlin/io/kdmtracker/app/SettlementSummaryTest.kt`

**Interfaces:**
- Consumes: `UiState.survivalLimit`, `.endeavors`, `.population`, `.resourceDefinitions`, `.storageLines`, `.phase`; `DerivedValues.endeavorsFor(population, ruleset)`.
- Produces: `data class SummaryResource(val name: String, val label: String, val quantity: Int, val colour: ResourceColour)`; `enum class ResourceColour { BONE, CORAL, GOLD, DIM }`; `SettlementSummaryModel.of(survivalLimit, endeavors, maxEndeavors, resources: List<SummaryResource>): SettlementSummaryModel`; `@Composable fun SettlementSummary(model: SettlementSummaryModel, year: Int, phaseLabel: String)`.

- [ ] **Step 1: Write the failing test**

```kotlin
class SettlementSummaryTest {
    @Test
    fun theFourBasicResourcesKeepTheDesignsOrderLabelsAndColours() {
        val model =
            SettlementSummaryModel.of(
                survivalLimit = 1, endeavors = 4, maxEndeavors = 4,
                resources =
                    listOf(
                        SummaryResource("Hueso de Monstruo", "Hueso", 1, ResourceColour.BONE),
                        SummaryResource("Órgano de Monstruo", "Órgano", 1, ResourceColour.CORAL),
                        SummaryResource("Piel de Monstruo", "Pellejo", 1, ResourceColour.GOLD),
                        SummaryResource("Scrap", "Chatarra", 0, ResourceColour.DIM),
                    ),
            )

        assertEquals(listOf("Hueso", "Órgano", "Pellejo", "Chatarra"), model.resources.map { it.label })
        assertEquals(listOf(ResourceColour.BONE, ResourceColour.CORAL, ResourceColour.GOLD, ResourceColour.DIM), model.resources.map { it.colour })
        assertEquals(4, model.resources.size, "a resource with none still renders its cell")
        assertEquals(0, model.resources.last().quantity)
    }

    @Test
    fun theEndeavorsPairCarriesTheDerivedMaximum() {
        val model = SettlementSummaryModel.of(1, endeavors = 3, maxEndeavors = 4, resources = emptyList())

        assertEquals(3, model.endeavors)
        assertEquals(4, model.maxEndeavors)
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :app-android:testDebugUnitTest --tests "io.kdmtracker.app.SettlementSummaryTest"`
Expected: FAIL — unresolved `SettlementSummaryModel`.

- [ ] **Step 3: Implement the model and the composable.**

The model is a pure carrier (no `Ruleset`, so tests build it directly). `SettlementSummary` renders the design's block: `surfaceContainerLowest` panel, `px 16 / py 8`; row 1 `local_fire_department` (14sp `secondary`) + `FASE ACTIVA` (`KdmType.labelCaps`, `secondary`) and a `surfaceContainer px 8 py 2` pill with `LÍMITE SUPERVIVENCIA` + the value in `secondary` bold; row 2 the year title in `headlineMedium` with its narrative line (`bodySmall`, `onSurfaceVariant`, **inert**) on the left and `ESFUERZOS DISPONIBLES` + `emergency` (18sp `secondary`) + the value in `KdmType.statDisplay` + `/ max` on the right; row 3 a four-column grid of `surfaceContainer p-2` cells with the label (`labelCaps`, `onSurfaceVariant`), the icon (13sp, the resource's colour) and the count (`labelCaps`, bold, same colour).

- [ ] **Step 4: Run it to verify it passes**

Run: `./gradlew :app-android:testDebugUnitTest --tests "io.kdmtracker.app.SettlementSummaryTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app-android/src/main/kotlin/io/kdmtracker/app/ui/settlement app-android/src/test/kotlin/io/kdmtracker/app/SettlementSummaryTest.kt
git commit -m "feat(app): add the settlement summary block"
```

### Task 5: The step frame

**Files:**
- Create: `app-android/src/main/kotlin/io/kdmtracker/app/ui/settlement/StepFrameModel.kt`
- Create: `app-android/src/main/kotlin/io/kdmtracker/app/ui/settlement/StepFrame.kt`
- Create: `app-android/src/test/kotlin/io/kdmtracker/app/StepFrameTest.kt`

**Interfaces:**
- Consumes: `UiState.steps: List<SettlementStep>`; `SettlementStepCatalog`.
- Produces: `StepFrameModel.of(step: SettlementStep, steps: List<SettlementStep>): StepFrameModel` with `title: String` (`"Paso 7. Desarrollo"`), `summary: String`, `advanceLabel: String`; `@Composable fun StepFrame(model: StepFrameModel, onAdvance: () -> Unit, onRestore: () -> Unit)`.

- [ ] **Step 1: Write the failing test**

```kotlin
class StepFrameTest {
    @Test
    fun theTitleCarriesTheStepNumberAndItsSpanishName() {
        val model = StepFrameModel.of(SettlementStepCatalog.ALL[6], SettlementStepCatalog.ALL)

        assertEquals("Paso 7. Desarrollo", model.title)
    }

    @Test
    fun theLastStepAdvancesWithoutNamingAStepTwelve() {
        val last = StepFrameModel.of(SettlementStepCatalog.ALL[10], SettlementStepCatalog.ALL)

        assertFalse(last.advanceLabel.contains("12"), "there is no step 12")
    }

    @Test
    fun aMiddleStepNamesTheNext() {
        val model = StepFrameModel.of(SettlementStepCatalog.ALL[6], SettlementStepCatalog.ALL)

        assertEquals("Avanzar al Paso 8: Preparar Supervivientes", model.advanceLabel)
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :app-android:testDebugUnitTest --tests "io.kdmtracker.app.StepFrameTest"`
Expected: FAIL — unresolved `StepFrameModel`.

- [ ] **Step 3: Implement.**

`of` derives the title from the step, and `advanceLabel` from the next step when there is one (`Avanzar al Paso N+1: <título>`); on the last step it names the phase commit instead — `Avanzar: cerrar la fase` — so no nonexistent step is named. `StepFrame` renders the step's icon (from the carousel's icon table) + the title, the summary, and the footer: the advance button (`arrow_forward`) and `restart_alt` `Restaurar Esfuerzos (N)` **disabled**.

- [ ] **Step 4: Run it to verify it passes**

Run: `./gradlew :app-android:testDebugUnitTest --tests "io.kdmtracker.app.StepFrameTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app-android/src/main/kotlin/io/kdmtracker/app/ui/settlement/StepFrame*.kt app-android/src/test/kotlin/io/kdmtracker/app/StepFrameTest.kt
git commit -m "feat(app): add the settlement step frame"
```

### Task 6: The Develop step's panels

**Files:**
- Create: `app-android/src/main/kotlin/io/kdmtracker/app/ui/settlement/InnovationsModel.kt`, `LocationsModel.kt`
- Create: `app-android/src/main/kotlin/io/kdmtracker/app/ui/settlement/InnovationsPanel.kt`, `LocationsPanel.kt`
- Create: `app-android/src/test/kotlin/io/kdmtracker/app/DevelopPanelsTest.kt`

**Interfaces:**
- Consumes: `UiState.innovationDeck`, `.innovationViews`, `.principles`, `.builtLocations`, `.catalogLocations`; `Ruleset.innovationDraw`/`.innovationKeep`; `InnovationView(innovation, status, missingParent, endeavorCost, resourceCost, used)`; `SettlementLocation(name, requires, buildEndeavors, buildCost, actions, recipes)`.
- Produces: `InnovationsModel.of(deck, principles, draw, keep)` with `deckSize`, `principleSlots`, `emptySlots`, `drawLabel` (`"Robar 2, elegir 1"`); `LocationsModel.of(built, catalog)` with `built: List<LocationEntry>` and `buildable: List<LocationEntry>`, where `LocationEntry(name, description, endeavors, actionLabel)`; and `@Composable fun InnovationsPanel(model, onInnovate)`, `@Composable fun LocationsPanel(model, onBuild, onAction)`.

- [ ] **Step 1: Write the failing test**

```kotlin
class DevelopPanelsTest {
    @Test
    fun theDrawLabelFollowsTheRulesetNotALiteral() {
        assertEquals("Robar 2, elegir 1", InnovationsModel.of(emptyList(), emptyList(), draw = 2, keep = 1).drawLabel)
        assertEquals("Robar 4, elegir 1", InnovationsModel.of(emptyList(), emptyList(), draw = 4, keep = 1).drawLabel)
    }

    @Test
    fun theDeckSizeCountsTheCards() {
        assertEquals(14, InnovationsModel.of(List(14) { "i$it" }, emptyList(), 2, 1).deckSize)
    }

    @Test
    fun aLocationWithNoBuildEndeavorsIsAlreadyBuilt() {
        val model =
            LocationsModel.of(
                built = listOf("El Cúmulo de Linternas"),
                catalog =
                    listOf(
                        SettlementLocation("El Cúmulo de Linternas"),
                        SettlementLocation("Taller de Huesos", buildEndeavors = 1),
                    ),
            )

        assertEquals(listOf("El Cúmulo de Linternas"), model.built.map { it.name })
        assertEquals(listOf("Taller de Huesos"), model.buildable.map { it.name })
        assertEquals(1, model.buildable.single().endeavors)
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :app-android:testDebugUnitTest --tests "io.kdmtracker.app.DevelopPanelsTest"`
Expected: FAIL — unresolved `InnovationsModel`.

- [ ] **Step 3: Implement the models, then the panels.**

`InnovationsPanel` renders `auto_stories` + `Innovaciones` + `Mazo: N cartas`, the `Principio` slot with the chosen principles, the two empty slots (`Por Descubrir` / `Futuro`), and the action row showing `model.drawLabel` and an `Innovar` button wired to `onInnovate`. `LocationsPanel` renders `domain` + `Localizaciones de Asentamiento` + `N Opciones`, each built location under `Fundacional` / `Construido` with its cost and its action (e.g. `Augurio`), and each buildable one under `Por Erigir` / `No construido` with `Requiere: N Esfuerzo` and a `Construir` button — using `handyman` where the design names the glyph that does not exist in the font. Below the list it renders the design's year-1 rule (`warning` + `Regla de Año Linterna 1` + `Sáltate el Paso 9` + its text) **inert** — the rule is not modelled (spec's unbacked table).

- [ ] **Step 4: Run it to verify it passes**

Run: `./gradlew :app-android:testDebugUnitTest --tests "io.kdmtracker.app.DevelopPanelsTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app-android/src/main/kotlin/io/kdmtracker/app/ui/settlement app-android/src/test/kotlin/io/kdmtracker/app/DevelopPanelsTest.kt
git commit -m "feat(app): add the develop step's innovation and location panels"
```

### Task 7: Compose the screen, then the visual gate

**Files:**
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/ui/SettlementScreen.kt`
- Test: `app-android/src/test/kotlin/io/kdmtracker/app/SettlementStepFrameRoutingTest.kt`

**Interfaces:**
- Consumes: `SettlementSummary`, `StepFrame`, `InnovationsPanel`, `LocationsPanel` (Tasks 4–6); the existing per-step bodies.

- [ ] **Step 1: Write the failing test**

```kotlin
class SettlementStepFrameRoutingTest {
    @Test
    fun everyStepRendersTheFrame() {
        SettlementStepCatalog.ALL.forEach { step ->
            assertTrue(StepFrameModel.of(step, SettlementStepCatalog.ALL).title.startsWith("Paso ${step.number}."))
        }
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :app-android:testDebugUnitTest --tests "io.kdmtracker.app.SettlementStepFrameRoutingTest"`
Expected: FAIL — unresolved `StepFrameModel`.

- [ ] **Step 3: Wire the screen.**

In `SettlementScreen`, render `SettlementSummary` above the carousel and `StepFrame` around the existing `when (currentStep)` body, adding `InnovationsPanel`/`LocationsPanel` inside step 7's branch. The ten unchanged bodies must keep rendering.

- [ ] **Step 4: Run the full suite**

Run: `./gradlew spotlessApply :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck`
Expected: PASS.

- [ ] **Step 5: The visual gate — measure, present, and STOP.**

Install with `./gradlew :app-android:installDebug`, ensure the app is **signed in** (the shell only renders inside a campaign; the dev user is `ficha` / `ficha1234` against a server started with `SPRING_PROFILES_ACTIVE=dev`), open the settlement and walk to step 7. Then:

- sample the rendered frame for the panel tones and the resource colours;
- compare each block against the design's markup (positions, sizes, copy);
- **present the result to the user and stop.**

**Do not commit, push, open a PR or merge until the user approves the rendered result.** This is the rule earned by the last two visual fixes, both merged on the agent's own judgement and both wrong to the user.

- [ ] **Step 6: Commit, only after approval**

```bash
git add app-android/src/main/kotlin/io/kdmtracker/app/ui/SettlementScreen.kt app-android/src/test/kotlin/io/kdmtracker/app/SettlementStepFrameRoutingTest.kt
git commit -m "feat(app): mount the settlement summary and step frame"
```

---

## Final steps

- [ ] Whole-branch review, then PR against `main` and squash-merge **after the visual result is approved**.
- [ ] If the forecast crosses the review budget, split into (a) the icon fix + the knobs + the catalog and (b) the screen.
- [ ] Record the accepted deviations (the `drive_file_stream` substitution; the inert narrative, year-1 rule and restore action) in the apply-progress and the PR body.
