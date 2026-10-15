# Backlog

Work that is known, wanted, and deliberately not being done yet.

## Ampliar el subset de Material Symbols — resuelto (C1)

**Resuelto (2026-10-13), commit `f0a0688` + `b78af0d`.** `KdmSymbolsFontTest` opens the bundled
TTF and asserts every codepoint in `KdmSymbols.CODEPOINTS` is in its cmap (dependency-free TTF
cmap reader, formats 4 and 12) — the class of bug where sixteen glyphs shipped missing while the
count still read right now fails the suite. The subset is 88 codepoints (`expand_less` and
`swap_vert` were added for the catalogue) and the regeneration recipe is checked in as
`tools/fonts/subset_material_symbols.py` (`--check` / `--build`) with seven tests in
`tools/fonts/tests/`. `KdmSymbolsTest` pins the 88-name map.

- Raised by: implementing Task 3 of the Innovar flow, 2026-10-12.
- Resolved: 2026-10-13, with C1 of the catalogue work.

## Revisar: colores propios del flujo Innovar

The `Innovaciones: Flujo de Innovar` board renders gold `#E5B842`, green `#10B981` and red
`#EF4444`. The app theme has gold `#E9C349` (`KdmPalette.gold`), pink-red `#FFB4AB`
(`KdmPalette.blood`) and **no green at all**.

We adopted the board's values literally (`InnovarPalette`), so the Innovaciones section currently
paints a slightly different gold from the rest of the app and introduces two colours the theme has
no role for. Decide whether to keep that fidelity or fold them into `KdmPalette` (`gold` →
`secondary`, `red` → `error`, green promoted to a real theme role).

Pinned by `KdmThemeTest.theInnovarFlowTokensMatchTheBoard`, so a change is deliberate.

- Raised by: maintainer, 2026-10-12, while reviewing the Innovar flow design.
- Scope: `app-android/src/main/kotlin/io/kdmtracker/app/ui/theme/KdmColors.kt`.

## Tests de estilo / regresión visual (Innovaciones)

Today the Innovaciones styles are only validated by hand at each visual gate: a refactor can
break colours, spacing or typography without any test failing.

Add tests that pin the section's styling under `app-android/src/test/`, so the Innovar flow's
look cannot regress silently.

- Raised by: maintainer, 2026-10-12, while approving the Innovar flow v2 plan.
- Scope: the Innovaciones panel and its innovation cards (`DevelopPanels.kt`,
  `InnovationCardView.kt`).

## Sin salida visible desde dentro de una campaña

Inside a campaign the app skips its own top bar on purpose (`KdmTrackerApp.kt`:
`if (AppShell.inShell(current.screen)) return@let`), so `AppTopBar` — the only place the `Home`
button lives — never renders where it is needed. `AppChrome.showHome` computes `true` for exactly
those screens, which makes it a dead flag: **the only way back to the "Settlements" list is the
system back gesture**, and the maintainer reports it failing.

Proposals were requested from Stitch and landed as
`.superpowers/design/innovar-flow/salida_4_propuestas.html` (chevron in the screen header, a global
header action, a `SALIR` item in the bottom navigation, a floating edge pill). Pick one, wire it,
and delete or repurpose the dead `showHome`.

- Raised by: maintainer, 2026-10-12, while reviewing the Innovar flow states.
- Scope: `app-android/src/main/kotlin/io/kdmtracker/app/KdmTrackerApp.kt`,
  `ui/AppTopBar.kt`, `ui/AppChrome.kt`, `ui/AppShell.kt`.

**Resuelto (2026-10-12)** — propuesta **C** elegida: un `SALIR` como quinta acción al extremo
izquierdo del dock inferior, antes de `ASENTAMIENTO`, con un `home` (el `logout` del tablero no está
en el subset de fuentes). Cableado como `onExit -> viewModel.goHome` y verificado en el emulador.
Las cuatro propuestas siguen en `.superpowers/design/innovar-flow/salida_4_propuestas.html` como
contexto del dictamen.

**Segunda mitad de esta misma entrada:** the subset was regenerated ad hoc from a throwaway venv —
there is no checked-in script, so nobody can reproduce it after the glyph map changes. The recipe
is: `fontTools` reads `KdmSymbols.CODEPOINTS` plus any new names, resolves them through the full
Material Symbols TTF's cmap, and runs `pyftsubset --unicodes=… --layout-features=* --no-hinting
--desubroutinize` over `google/material-design-icons`' variable font. Worth committing as
`tools/fonts/subset_material_symbols.py` next time the font is touched.

- Raised by: 2026-10-12, after regenerating the subset for the Innovar flow.
- **Resuelto (2026-10-13):** el script está en el repo como
  `tools/fonts/subset_material_symbols.py` con su suite (`b78af0d`).

## Hallazgos informativos de la revisión del catálogo (R3)

The native review that closed `approved` over the catalogue commits (`review-f848edb3b6e74309`,
risk + reliability lenses) left two informational, non-blocking findings worth a small follow-up:

- **`R3-FilterChipLabelMatch`** (WARNING) — `InnovationCatalogue.kt` maps an acquisition chip back
  to its `Acquisition` entry by comparing the *display label*
  (`Acquisition.entries.firstOrNull { it.label == chip.label }`). A renamed or translated label
  would make the chip silently inert. Match on the enum name or carry the entry in `CatalogChip`.
- **`R3-FilterStateNotTestedInUI`** (SUGGESTION) — the catalogue filter state
  (`acquisition` / `category` in `InnovationsPanel`, `DevelopPanels.kt`) is exercised on the
  emulator but has no UI-level test asserting the filtered grid.

- Raised by: native review of `902726a..ec497b2`, closed 2026-10-13.
- Scope: `app-android/src/main/kotlin/io/kdmtracker/app/ui/settlement/InnovationCatalogue.kt`,
  `DevelopPanels.kt`, `app-android/src/test/`.
