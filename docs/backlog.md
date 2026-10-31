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

## Menores visuales del flujo Innovar (diferidos)

Two visual minors from the Innovar review stayed deferred — they touch the Innovaciones surface,
not this phase, and each needs its own emulator gate:

- **`costChip` mezcla esfuerzo con la inicial** — the minified price row mixes the effort count
  with the resource initials (`1H 1Ó 1P`) in a way the review flagged as misleading.
- **La fila de robo se desvía del tablero** — the draw row's alignment drifts from the board.

Everything else from that review's nine minors is now closed: the two fragile `first{}` calls, the
dead code (`CostLine`, hairlines, nested `if (drawn)`, `onSpendEndeavor`, `acquired`), the stale
comments, the `Innovate.run` availability guard (M1 on `feat/innovar-flow`), `ResourceEdit` (it
became the foundation of the add-resources flow, not debt), the "mantén pulsada" hint and the
save-without-change celebration (both closed by Part A Phase 3 — `feat/anadir-recursos`).

- Raised by: native review of the Innovar flow, 2026-10-14.
- Scope: `app-android/src/main/kotlin/io/kdmtracker/app/ui/settlement/DevelopPanels.kt`,
  `InnovationCardView.kt`.

## Añadir recursos (Part A, Fase 3) — hecho

Implemented 2026-10-16 on `feat/anadir-recursos` per
`docs/superpowers/specs/2026-10-15-anadir-recursos-design.md` and its plan: `ResourceAdjustFlow`
owns the four design states, `SettlementSummary` renders them (tap to edit, dimmed neighbours,
red minimum with the save button kept, green confirmed cell with its strip), icons follow the
newest board (`nutrition` / `psychology`), and `delta == 0` saves nothing. Visual gate approved on
the emulator before the commits.

- Raised by: Part A roadmap, 2026-10-15.

## Ficha de superviviente: subtítulo de rol y estado vacío en emulador (V1, diferidos)

Two gaps from the V1 paged survivor sheet (`feat/supervivientes-paged`) were deferred, each out of
that slice's scope:

- **Subtítulo de rol no modelado** — the design carries `Prólogo • Superviviente Fundador` under
  the survivor's name in the pager bar; the `Survivor` domain has no role field, so the pager shows
  only name + gender chip. Adding a modelled role is its own feature, not a render gap.
- **Estado vacío sin captura en emulador** — `Aún no hay supervivientes.` renders only on a
  0-survivor roster, but `population = 4` is the fixed default (`Settlement.kt:60`), there is no UI
  to remove a survivor or create with population 0, and the sync does not import new campaigns
  (`rebind` only), so an empty roster is unreachable on-device without a domain change. The branch
  is pinned by JVM tests (`emptyRosterPredicate`, `frameIsEmptyOnAnEmptyRoster`,
  `advanceOnAnEmptyRosterStaysAtZero`); capture it once a remove/kill flow exists.

- Raised by: V1 visual gate review, 2026-10-16.
- Scope: `app-android/src/main/kotlin/io/kdmtracker/app/ui/SurvivorsScreen.kt`,
  `SurvivorPagerModel.kt`, `core/.../domain/Survivor.kt`.

## Tabla de Eventos de Caza (d100) — pendiente de modelar (V2 Caza, diferido)

Decisión del mantenedor (2026-10-18) al planificar V2: la **Tabla de Eventos de Caza** — los
eventos 01–100 que se tiran con **d100** en las casillas básicas — **queda fuera de V2** y se
modela como slice aparte. V2 solo reconstruye la pantalla 1:1 y modela `MoveMonster`.

- Los 100 eventos ya están transcritos en `external-assets/09..11_tabla_de_eventos_de_caza_*.md`
  (págs. 90–105) pero **no están en el modelo de dominio**. `Core15HuntEvents.kt` solo tiene 37
  cartas (3 BASIC, 32 MONSTER del León, 2 SPECIAL) — los decks, no la tabla d100.
- El diseño de Stitch de Caza dibuja un evento con "Tirada **d10**" y 3 rangos: es un **ejemplo
  ilustrativo**; el juego real usa **d100** sobre los 100 eventos (los rangos 1-3 / 4-7 / 8-10 del
  diseño son de ese ejemplo concreto).
- Al modelarla: parsear `external-assets/09..11`, crear el catálogo, y añadir un usecase de tirada
  d100 que resuelva el evento elegido. Requiere gate visual aparte.

- Raised by: V2 Caza scope decision, 2026-10-18.
- Scope: `core/.../content/`, `core/.../usecase/`, `app-android/.../ui/HuntScreen.kt`.

## Pasos del asentamiento 1:1: controles inertes y funciones retiradas (diferidos)

Al reconstruir los cuerpos de los pasos 1–6 y 8–11 desde el diseño de Stitch, cada control que el
diseño dibuja **deshabilitado** y cada función que el diseño ya no muestra quedan aquí.

**Dibujado pero inerte** (no hay modelo detrás):

- `+1 Supervivencia` a los que regresan (paso 2): la regla del núcleo no está modelada.
- Alternar el consumo de esfuerzos por superviviente y su `−`/`+` (paso 3).
- Las listas de comprobación del paso 11 y los chips de armadura / equipo del paso 8.
- `Añadir Ítem` en `Equipo Almacenado` (paso 10): no hay un modelo para añadir equipo al almacén.
- Cambiar la presa (`Canteras`) en el paso 8.
- `Vida / IA` y `Dureza` del monstruo (paso 8): no hay modelo de estadísticas de monstruo.
- La etiqueta de herida (corazón) del paso 2.

**Retirado de la UI** (la lógica sigue en el controlador y sus tests):

- Robar y registrar el evento de asentamiento (paso 4).
- El contador de nacimientos y el hito Nueva Vida (paso 5); deshacer una muerte registrada.
- La declaración del enfrentamiento especial y su UI de encuentro (paso 9).
- El resumen y el aviso de esfuerzos sin gastar del paso 11 (el contador sigue en el pie).

**Sin superficie diseñada:**

- Las líneas de almacén que no son los cuatro recursos básicos no se muestran en el paso 10; se
  resuelve junto con B3 (catálogo real de recursos).
- Las fotos de fondo de los héroes quedan fuera de alcance.
- Los hitos que el diseño no nombra conservan el texto en inglés del núcleo hasta B4 (selector de
  idioma).

- Raised by: reconstrucción 1:1 de los pasos del asentamiento, 2026-10-29.
- Scope: `app-android/.../ui/*StepScreen.kt`, `core/.../usecase/`.

## Enfrentamiento 1:1: lo que el diseño dibuja y el modelo no tiene (V3, diferidos)

Al reconstruir `ShowdownScreen` desde el diseño de Stitch (`León Blanco`) solo se conecta lo que el
encuentro guardado ya sabe: mazo de IA, heridas, última carta robada y las localizaciones de
impacto del monstruo. El resto queda aquí.

**Dibujado sin datos detrás:**

- `Mov`, `Res`, `Vel` y `Daño` del monstruo: se dibuja el guion; no hay estadísticas por monstruo y nivel.
- El punto ciego es siempre la base 2x2 mirando a la derecha; no depende del monstruo.

**Retirado de la UI** (no hay estado que lo respalde):

- La ronda y el turno (`Ronda 1 · Turno Monstruo`), el `Paso 7: Robar Ataque` y `Resolver Ataque`.
- El controlador del monstruo y su `+1 Locura`.
- La carta de IA con la selección de objetivo por niveles: se muestra el texto impreso de la carta; solo
  se extraen Velocidad, Precisión y Daño.
- El tope del mazo de localizaciones (`Mano Extraña`, trampa) y las dos cartas «reveladas»: el
  encuentro no guarda un mazo de localizaciones, así que la lista muestra todas y resolver una (fallo,
  herida o crítico) la retira solo de la lista hasta barajar; no se persiste.
- `Instinto pasivo` y `Terreno`: ni el instinto ni el terreno están modelados.

**Aproximaciones:**

- `Reacción` se marca cuando el texto de la localización empieza por `Fallo` o `Reflejo`.
- El contador de impactos es un bloc de notas local; no se guarda.

**Añadido para que el diseño funcione:** `HealMonster` (el `−` de heridas devuelve la última herida al
mazo de IA).

- Raised by: reconstrucción 1:1 del enfrentamiento, 2026-10-30.
- Scope: `app-android/.../ui/showdown/`, `core/.../usecase/HealMonster.kt`.

## Contar las celdas de recursos por material (B3, diferido)

Las celdas del resumen y del paso 10 muestran la cantidad del recurso básico de su material
(`ResourceMaterial.basicId`), no la suma del material: una Garra de León (hueso) no suma a la celda
Hueso. `Storage.byMaterial` ya existe y está probada, pero usarla tal cual deja el `−` de la celda
incoherente (la celda marcaría 3 y gastar del básico se rechazaría con 0).

Antes de contar por material hace falta una regla de **gastar por material**: primero el recurso
básico y después las variantes. Fabricar ya gasta por id y no cambia.

- Raised by: saldar la deuda del grid de B3, 2026-10-30.
- Scope: `app-android/.../ui/settlement/SettlementSummaryModel.kt`, `ui/StepBodies9To11Model.kt`,
  `core/.../usecase/AddResourceEntry.kt`.

## Selector de idioma (B4, diferido)

Selector de runtime `Language{ES,EN}`, con español por defecto e inglés incremental, según la decisión
de i18n de la spec de identidad de contenido. Hoy no existe ningún selector en la UI: los nombres de
contenido ya llevan `LocalizedText` (con respaldo a español), pero la interfaz usa literales en español.
Se retira del roadmap por decisión del mantenedor y se retoma cuando se quiera jugar en inglés.

- Raised by: priorización del mantenedor, 2026-10-30.
- Scope: `core/.../domain/Language.kt`, `LocalizedText`, `app-android/.../ui/`.

## Reglas del manual que la app no cumple todavía (tests de flujo, 2026-10-30)

Los tests de `RulebookDivergencesTest` enuncian cada regla y están marcados `@Ignore` hasta que
exista el comportamiento. Se descubrieron al escribir los tests de flujo del año linterna contra el
manual (KDM 1.5 ES); ninguna se ha corregido sin avisar al mantenedor.

- **Paso 3, Ganar Esfuerzos** (fase de asentamiento, págs. 34-35 y 81-85): «cada superviviente que
  regresa genera +1 esfuerzo». La app fondea con la población entera (`YearFlowActions.fundGainEndeavors`),
  así que tras una caza fallida (partida muerta, nadie regresa) el paso 3 reparte igualmente 4 esfuerzos.
  Las muertes se anotan en el paso 5, después del 3, por lo que la población aún las cuenta.
- **Hambruna** (fase de caza, págs. 61-63): «si la caza llega o se resuelve en la casilla de hambruna,
  se pierden d5 recursos del almacén; si la partida muere, la caza fracasa y se activa hambruna».
  Hoy la casilla `STARVATION` existe en el tablero, pero llegar a ella o fracasar no quita recursos.
  Hace falta decidir qué recursos se pierden (el manual no lo fija en el texto disponible).

- **Retroceso del monstruo** (resuelto): `RetreatMonster` aleja la bestia del grupo (sube el índice) y
  se queda dentro del tablero. Queda abierto que el evento del Antílope pida alejarla 3 espacios y el
  botón mueva uno por pulsación.
- **Emboscada** (fase de caza, págs. 61-63): hay dos emboscadas, la del monstruo («los supervivientes
  pierden su primer turno») y la de los supervivientes por un evento de sigilo («eligen quién actúa
  primero y colocan el terreno»). La app solo distingue `AMBUSH` (la bestia llega al grupo), que sirve
  para pasar al enfrentamiento, pero el estado del enfrentamiento no guarda quién emboscó ni su
  efecto. No hay test porque aún no existe dónde observarlo; hace falta decidir el modelo.

- Raised by: tests de flujo del año linterna, 2026-10-30.
- Scope: `app-android/.../YearFlowActions.kt`, `core/.../usecase/SetHuntOutcome.kt`,
  `core/.../usecase/RetreatMonster.kt`, `app-android/src/test/.../RulebookDivergencesTest.kt`,
  `app-android/src/test/.../HuntPhaseRulesFlowTest.kt`.

## Guardas de fase del controlador (casos límite, 2026-10-31)

Resuelto: `advance()` y `selectStep()` ignoran la fase de Caza (y `selectStep()` también la de
Enfrentamiento), y el último paso del asentamiento se rechaza con «no departing survivors» mientras
nadie esté elegido para partir en el paso 8 (decisión del mantenedor: la caza necesita al menos un
superviviente). Tests en `PhaseGuardsTest`.

- Raised by: sondeo de casos límite del asentamiento, 2026-10-31.
- Scope: `app-android/.../YearFlowActions.kt`, `app-android/src/test/.../PhaseGuardsTest.kt`.
