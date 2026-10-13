# Backlog

Work that is known, wanted, and deliberately not being done yet.

## Ampliar el subset de Material Symbols al vocabulario del flujo Innovar

`app-android/src/main/res/font/material_symbols_outlined.ttf` bundles only the glyphs the original
four screens used. The `Innovaciones: Flujo de Innovar` board draws several the subset does not
have, and `KdmSymbols.glyph` **throws** on an unmapped name (it crashed the app once already).

Missing and needed: `error`, `lock_open`, `sync_alt`, `undo`, `brush`, `unfold_more`,
`manage_search`, `check`, `palette`.

Until the font is re-subset, those draw as the nearest mapped glyph (`error` → `warning`, and so
on) with a comment at each site. Re-subsetting needs `fonttools` and the full Material Symbols
TTF — an environment decision, not a code one.

- Raised by: implementing Task 3 of the Innovar flow, 2026-10-12.
- Scope: `app-android/src/main/res/font/material_symbols_outlined.ttf`,
  `app-android/src/main/kotlin/io/kdmtracker/app/ui/theme/KdmSymbols.kt`, `KdmSymbolsTest`.

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
