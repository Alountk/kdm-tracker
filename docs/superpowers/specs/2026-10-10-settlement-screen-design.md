# Settlement Screen + house-rule knobs — Design (Slice 6)

**Status:** Draft (awaiting review)
**Slice:** Rebuild the Settlement (Asentamiento) view to the design — the settlement summary, the per-step frame and the Develop step's panels — and move the two rule values that screen shows into the ruleset so a house rule can change them.
**Depends on:** the app shell (#28–#30, merged). Part 2 of the "apply the design to the screens" effort; Caza and Enfrentamiento follow.

## Goal

The shell from the last slice frames the screens; this slice rebuilds the first of them. The design's "Fase de Asentamiento" screen has four blocks, two of which the shell already provides:

1. the **settlement summary** (the design calls it the *monolito superior*) — phase, survival limit, the year's story, available endeavors and the four basic resources;
2. the **phase carousel** — merged with the shell;
3. the **step frame** — the step's icon, number and name, its text, and the footer's advance/restore actions;
4. the **Develop step's panels** — Innovations and Settlement Locations.

This slice builds 1, 3 and 4, and leaves the other ten steps' bodies as they are.

## Source of truth

From the screen's markup. Summary block (`bg-surface-container-lowest px-margin py-space-md shadow-md`):

- row 1 — `local_fire_department` (14sp, `secondary`) + `FASE ACTIVA` in `labelCaps` `secondary` with wide tracking; right, a `bg-surface-container px-2 py-0.5` pill holding `LÍMITE SUPERVIVENCIA` (`labelCaps`, `onSurfaceVariant`) and its value (`labelCaps`, `secondary`, bold);
- row 2 — the year title in `headline-lg-mobile` (22/28) `primary` with a `bodySmall` narrative line under it; right, `ESFUERZOS DISPONIBLES` (`labelCaps`, `onSurfaceVariant`) over `emergency` (18sp, `secondary`) + the value in `statDisplay` `secondary` + `/ max` in `bodySmall`;
- row 3 — a four-column grid (`grid-cols-4 gap-1.5`), each cell `bg-surface-container p-2` with the resource name (`labelCaps`, `onSurfaceVariant`), its icon (13sp) and its count (`labelCaps`, bold). Each resource carries its own colour: Hueso → `primary`, Órgano → `tertiary-fixed-dim` (`#ffb4ac`), Pellejo → `secondary`, Chatarra → `outline`.

Step frame: the step's icon beside `Paso N. <Nombre>`; the step's text below; a footer with `Avanzar al Paso N+1: <Nombre>` plus `arrow_forward`, and `restart_alt` `Restaurar Esfuerzos (N)`.

Develop panels — **Innovaciones** (`auto_stories`, `Mazo: N cartas`): the `Principio` slot showing what was chosen, two empty slots (`Por Descubrir` / `Futuro`), and the action `Robar 2, elegir 1` → `add` `Innovar`. **Localizaciones** (`domain`, `N Opciones`): built locations (`Fundacional`, `Construido`, their description, `Costo: 1 Esfuerzo` and their action) and buildable ones (`Por Erigir`, `No construido`, their description, `Requiere: 1 Esfuerzo`, `Construir`).

The design's canonical **Spanish step names**, which this slice adopts for the catalog:

`1 Preparación · 2 Supervivientes Regresan · 3 Ganar Esfuerzos · 4 Actualizar Línea de Tiempo · 5 Actualizar Contador de Muertes · 6 Comprobar Hitos · 7 Desarrollo · 8 Preparar Supervivientes · 9 Enfrentamiento Especial · 10 Anotar y Almacenar · 11 El Fin del Principio`

## Design (validated with the user)

### Scope of the rebuild

The design covers the summary and **one step of eleven** (7 · Desarrollo). Therefore:

- the **summary** and the **step frame** are rebuilt and applied to all eleven steps, so the phase reads consistently;
- the **Develop panels** (Innovations, Locations) are rebuilt 1:1;
- the **other ten steps' bodies keep their current look** until each gets its own design — they are not restyled by inference, and no design is invented for them.

### Copy

The eleven **step titles move to the design's Spanish names** (listed above) — which also makes the carousel and the step header agree, since the carousel already shows the design's Spanish short labels while the catalog is still English. The ten step summaries that no design covers are a **translation of our own existing text**, not new content.

### House rules

The screen shows a rule as text (`Robar 2, elegir 1`). Written literally it would be a rule the user cannot change, and the project's own principle is the opposite: *"the concrete values … live here, so a campaign can swap in house rules without touching the mechanisms"* (`Ruleset`'s KDoc). `Campaign` already carries `RulesetOverrides`, `applyOverrides` composes them, and they persist and travel in the export — but only six knobs exist, and two of the values this screen needs are not among them.

So this slice adds the knobs the screen requires, following the established pattern:

| Place | What goes there |
| --- | --- |
| `Ruleset` | the value, with its official default |
| `Core16Ruleset` | the official value |
| `RulesetOverrides` | the nullable per-campaign knob |
| `applyOverrides` + `OverrideRuleset` | the pass-through |
| the mechanism | reads `ruleset.x` — **never a literal in the UI** |

Two values are new (`innovationDraw` 2, `innovationKeep` 1 — the Innovation deck draw), and two exist but are **not overridable**, an inconsistency introduced when they were added:

| Value | Today | This slice |
| --- | --- | --- |
| `innovationDraw` / `innovationKeep` | does not exist; the UI text is literal | new, overridable |
| `huntXpTrackMax` (16) | on `Ruleset`, no override | **made overridable** |
| `insanityThreshold` (3) | on `Ruleset`, no override | **made overridable** |

The **house-rules editor** — a screen to change them — is explicitly **not** in this slice.

### Unbacked, rendered and left inert

Per the agreed rule — render the design exactly, disable what is not implemented:

| Element | Why it is inert |
| --- | --- |
| the year's story title and narrative (`Primer Día` / `Luz tenue tras el León Blanco`) | no year-narrative model |
| `Regla de Año Linterna 1 · Sáltate el Paso 9` | no year-rule model |
| `Restaurar Esfuerzos (N)` | no restore/re-run model |

## A prerequisite this slice must fix: the icon subset is missing glyphs

Reviewing the design's markup for this screen exposed a defect in the **merged** subset font. The glyph extraction used the pattern `[a-z_]+`, which **cannot match a name containing a digit**, so two icons the designs use were silently dropped:

- `format_h4` (U+**F860**) — the Hueso icon in the summary's resource grid;
- `grid_4x4` (U+**F016**).

Both exist in Google's official codepoints file and are absent from the bundled subset, so this screen would render **tofu**. The subset and `KdmSymbols` must both be regenerated with a digit-tolerant extraction (69 names found; 68 exist in the font).

`drive_file_stream`, which the design uses for the Construct buttons, **does not exist in the font at all** — the closest glyph names are `drive_file_rename` / `drive_file_move`. It is **substituted with `handyman`** (already bundled, semantically the build icon) and recorded.

## Scope

### In

- `:core`: the three ruleset knobs (two new, two made overridable) with their overrides and pass-through; the eleven step titles in Spanish and the ten summaries translated.
- `:app-android`: the icon subset + map fix; the summary composable; the step frame; the Develop panels.
- The screen reads every rule value from the ruleset — no rule literal in the UI.

### Out

- The ten steps whose bodies no design covers (kept as they are).
- The house-rules editor.
- Caza and Enfrentamiento.

## Architecture

- `core/.../ruleset/Ruleset.kt`, `Core16Ruleset.kt`, `RulesetOverrides.kt` — the knobs and the pass-through.
- `core/.../rules/SettlementStepCatalog.kt` — the Spanish titles and summaries.
- `core/.../usecase/Innovate.kt` — unchanged in behaviour; the draw/keep values are consumed where the deck is offered.
- `app-android/.../ui/settlement/` — new: `SettlementSummary.kt`, `StepFrame.kt`, `InnovationsPanel.kt`, `LocationsPanel.kt`.
- `app-android/.../ui/SettlementScreen.kt` — composes them around the existing per-step bodies.
- `app-android/.../ui/theme/KdmSymbols.kt` + `res/font/material_symbols_outlined.ttf` — the regenerated subset.

## Data

| Design element | Source |
| --- | --- |
| Fase Activa | `record.phase` |
| Límite Supervivencia | `survivalLimit` |
| Esfuerzos `N / M` | `endeavors` (spent) over `DerivedValues.endeavorsFor(population, ruleset)` — the same derived total the Gain Endeavors step awards, so `M` is never a literal |
| the four resources | the catalog's four basic resources, in the catalog's order. The design's grid labels them with its short forms (`HUESO`, `ÓRGANO`, `PELLEJO`, `CHATARRA`), which are shorter than the catalog's names (`Hueso de Monstruo`, …), so each cell is keyed by the resource's identity and takes the design's short label and colour; an unmapped resource falls back to the catalog name and `onSurfaceVariant`. Counts come from the settlement's resource ledger |
| `Mazo: N cartas` | the innovation deck |
| the chosen principle slot | the settlement's principles |
| locations built / buildable | the settlement's locations + the catalog |
| `Robar N, elegir M` | the new ruleset knobs |
| the year story, the year-1 rule, Restaurar Esfuerzos | **unbacked → inert** |

## Testing

- **`:core`**: the two new knobs default to 2/1 and are overridable through `applyOverrides`; `huntXpTrackMax`/`insanityThreshold` are now overridable too; the step catalog's eleven titles are the design's Spanish names.
- **`:app-android`**: the summary's derived values (a zero resource still renders its cell; the endeavors pair uses the ruleset values); the step frame's `Paso N` and its advance label come from the catalog; the panels' models.
- **Manual visual acceptance — mandatory, and this slice carries a stricter rule.** The prior slices' lesson: two visual fixes were merged on the agent's own judgement and both were wrong to the user. So for any visual change here, the app is installed, **left signed in on the exact screen**, measured objectively *and* presented; **no commit, push, PR or merge happens until the user has approved the rendered result.**
- **Gate**: the full suite plus `spotlessCheck`.

## Risks

- **Touching the ruleset**: adding knobs changes `Ruleset`, `RulesetOverrides` and `OverrideRuleset` — mirroring what the Override pass-through cost the last time it was missed. The pass-through is part of the same task.
- **Translating the step catalog** touches `:core` tests that assert the English titles; they move with it.
- **The icon subset**: regenerating the font is a binary swap; the glyphs in use are pinned by `KdmSymbolsTest`, which must be extended to the new names so a bad codepoint fails loudly rather than drawing a different icon.
- **Scope**: the summary + the frame + two panels is a large slice; if the forecast crosses the review budget it splits into (a) the prerequisite + the knobs and (b) the screen.

## References

- Stitch screen `59fdd89d6100424eb9b5bbf1a6e74ffa` ("Fase de Asentamiento").
- `core/.../ruleset/{Ruleset,Core16Ruleset,RulesetOverrides}.kt`, `core/.../rules/SettlementStepCatalog.kt`.
- `app-android/.../ui/SettlementScreen.kt`, `app-android/.../ui/shell/PhaseCarousel.kt`, `app-android/.../ui/theme/{KdmSymbols,KdmType}.kt`.
