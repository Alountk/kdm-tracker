# App Shell — the design's chrome and 4-tab navigation (Slice 5)

**Status:** Draft (awaiting review)
**Slice:** Replace the app shell with the Stitch design's chrome — a fixed top bar with the settlement's stat cluster, a fixed 4-tab bottom navigation, and the 11-step phase carousel. Every screen then lives inside that frame.
**Depends on:** the Atramentum Lantern theme (PRs #23–#27, merged). Part 1 of the "apply the design to the screens" effort: after the shell, each screen is rebuilt 1:1 in its own slice (Asentamiento → Supervivientes → Caza → Enfrentamiento).

## Goal

The app currently navigates `Home → Settlement | Hunt | Showdown | Account` with our own top bar (a title, a sync chip, Home/Account buttons) and a numbered step bar. None of that is the design.

The design ships one shell shared by all four of its screens:

1. a **fixed top bar** (80dp, blurred) carrying the lantern year, the phase name, the settlement name, a three-stat cluster and a round avatar;
2. a **fixed bottom navigation** (64dp, blurred) with four tabs — Asentamiento, Supervivientes, Caza, Enfrentamiento;
3. a **phase carousel** of 11 step chips inside the Asentamiento view.

This slice builds that shell and routes the existing screens into it. It does **not** rebuild the screens' content — that is the following slices.

## Source of truth

Taken from the Stitch screen markup (not from the brand prose):

**Top bar** — `fixed top-0 z-50 bg-surface/90 backdrop-blur-xl shadow-[0_4px_24px_rgba(0,0,0,0.5)]`, inner row `h-20 px-margin`:
- left column: `light_mode` (16sp, `secondary`) + `AÑO LINTERNA N` in `labelCaps` uppercase `secondary` with wide tracking; the phase name in `headlineSmall` `primary`; the settlement name in `bodySmall` `onSurfaceVariant`;
- right: a stat cluster `bg-surfaceContainerLow px-2 py-1`, radius 4, three stats separated by 1px `surfaceContainerHighest` dividers — `group`+population (`onSurface`), `skull`+deaths (`error`), `flare`+lantern reserve (`secondary`) — then a 32dp round avatar (`primary` fill, `person` 18sp `onPrimary`).

**Bottom nav** — `fixed bottom-0 z-50 bg-surface/95 backdrop-blur-xl shadow-[0_-4px_24px_rgba(0,0,0,0.6)]`, inner row `h-16 px-gutter`, tabs `justify-around` and each `min-w-[56px] min-h-[44px]`: icon (20sp) + `labelCaps` uppercase label. Active `secondary`; inactive `onSurfaceVariant`.
Icons, in order: `fort` Asentamiento · `shield_person` Supervivientes · `explore` Caza · `swords` Enfrentamiento.

**Phase carousel** — a `surfaceContainerLow px-margin py-space-sm` section: the title `Secuencia de Fase Oficial` in `labelCaps` `onSurfaceVariant`, and `Paso N de 11` in `labelCaps` `secondary`; then a `flex gap-2 overflow-x-auto` strip of 11 chips, each `w-16 p-2` column: icon (18sp), the step number (`labelCaps`), and a 9sp uppercase truncating short label. States: active `bg-primary text-onPrimary` bold; **step 9** `bg-[#8E1214] text-primary` bold; otherwise `bg-surfaceContainer text-onSurfaceVariant`. The active chip auto-scrolls to centre.

| # | icon | short label | | # | icon | short label |
|---|---|---|---|---|---|---|
| 1 | `group_add` | Prep. | | 7 | `handyman` | Desarrollo |
| 2 | `account_balance` | Regreso | | 8 | `shield` | Partida |
| 3 | `emergency` | Esfuerzo | | 9 | `swords` | Especial |
| 4 | `hourglass_empty` | Tiempo | | 10 | `edit_note` | Almacén |
| 5 | `skull` | Muertes | | 11 | `flare` | Fin |
| 6 | `check_box_outline_blank` | Hitos | | | | |

Content sits under both bars with `pt-20 pb-24`.

## Design (validated with the user)

### Topology

- **Before a campaign is chosen**: the existing Home (campaign list) and Account screens, unchanged.
- **Inside a campaign**: the shell — fixed top bar, fixed bottom nav, and the content area. The four tabs are the four views of that settlement.
- **Free tabs (chosen):** all four tabs navigate at any time, as the prototype does. A tab whose phase has not started shows its own empty/not-started state; nothing is gated.
- **The bar's title follows the selected tab, not the record's phase** — each design screen names itself in the bar ("Asentamiento", "Caza", …), so the bar always describes the view you are looking at. The record's phase only decides which tab is selected when the shell opens. The other two bar fields stay record-derived (lantern year, settlement name).

### The two smaller decisions

1. **The sync chip leaves the top bar.** The design's bar has no sync affordance; the chip ("Not signed in" / "Sync error") moves into the Account screen, which the avatar opens. This keeps the bar pixel-faithful and still surfaces sync failures, one tap away.
2. **Home stays as the campaign list** and is the pre-shell entry point.

### What the tabs show, and in what order it gets rebuilt

The shell routes each tab to a destination. This slice points them at the screens that already exist (already themed by `KdmTheme`); each is then rebuilt 1:1 in its own slice:

| Tab | Now (this slice) | Later |
| --- | --- | --- |
| Asentamiento | the existing settlement step flow, with the numbered step bar **replaced** by the phase carousel | rebuilt to the design's settlement view |
| Supervivientes | the existing roster | rebuilt as the design's **paged sheet** (prev/next survivor; the design has no roster list) |
| Caza | the existing hunt screen | rebuilt |
| Enfrentamiento | the existing showdown screen | rebuilt |

## Scope

### In

- `:app-android`: the shell (`AppShell`, `ShellTopBar`, `ShellBottomNav`, `PhaseCarousel`), the `ShellTab` state, the routing change, retiring our top bar from the in-campaign screens, and the carousel replacing the step bar.
- Moving the sync status out of the top bar and into the Account screen.

### Out (explicitly deferred)

- Rebuilding any screen's content (the four slices that follow).
- The design's "lantern glow" and crimson-border elevation treatments, and any gradient.
- The `designMd` prose palette (already settled: the render wins).

## Architecture

New, under `app-android/src/main/kotlin/io/kdmtracker/app/ui/shell/`:

- `ShellTab.kt` — `enum class ShellTab { SETTLEMENT, SURVIVORS, HUNT, SHOWDOWN }` with its icon name and Spanish label.
- `AppShell.kt` — the frame: `ShellTopBar` + content slot + `ShellBottomNav`.
- `ShellTopBar.kt` — the bar above, taking the settlement context and the three stats.
- `ShellBottomNav.kt` — the 4 tabs with the active state.
- `PhaseCarousel.kt` — the 11-chip strip, taking the active step and a `onSelectStep` callback.

Modified:

- `KdmTrackerApp.kt` — inside a campaign, render `AppShell` with the selected tab instead of switching on `Screen`; keep Home and Account on their own.
- `AppChrome.kt` / `AppTopBar.kt` — keep only for Home/Account; the in-campaign top bar is `ShellTopBar`.
- The selected tab is app-layer UI state (like the current `Screen`), so `:core` is untouched — the domain stays the single source of truth for the data each view renders.
- `SettlementScreen` — its numbered step bar is removed in favour of `PhaseCarousel`.

## Data

The three stats read from `UiState`:

| Chip | Source |
| --- | --- |
| `group` population | `UiState.population` ✓ |
| `skull` deaths | `UiState.deaths` ✓ |
| `flare` "Reserva de Linternas" | **not modelled** |

The lantern reserve has no backing in the domain (there is no lantern-hoard concept; only a doc-comment mention). Under the agreed rule — render the design exactly, disable what is not implemented — the chip renders in place with the design's structure and an inert `—` value, and is recorded as a gap rather than faked or hidden.

## Testing

- **`:app-android` JVM**: `ShellTab` carries the right icon and Spanish label per tab; the initial tab follows the settlement's phase; `PhaseCarousel`'s model yields 11 steps with the design's icons, short labels and the step-9 crimson variant, and selects the active step from the record.
- **Manual visual acceptance (required)**: render the shell on the emulator and compare against the design's markup for each of the four tabs — bar heights, blur, the stat cluster's dividers, the active/inactive tab colours, and the carousel's three states (active, step 9, rest).
- **Gate**: the full suite plus `spotlessCheck`, then PR and squash-merge.

## Risks

- **Hiding sync failures.** Moving the chip to Account makes a broken sync less visible. Mitigation: the avatar is the only account affordance, and the Account screen keeps the existing, explicit status line.
- **Free tabs over an empty phase.** Tapping Caza during the settlement phase must show a legible not-started state, not a broken screen. Each tab's empty state is checked in the visual pass.
- **The step-bar swap.** The carousel is taller than the numbered bar and scrolls horizontally; the settlement content's layout must be re-checked on a narrow viewport.
- **Shell size.** The shell plus the routing change is a moderate diff; if the forecast crosses the 400-line budget the carousel splits into its own PR.

## References

- Stitch screens: `310b1d1ff8994d619e67fda164f35f36` (Ficha), `59fdd89d6100424eb9b5bbf1a6e74ffa` (Asentamiento), `4632e56b39c5470188dbe0b087122767` (Caza), `cd089a6938264afea5bd710ff04e3716` (Enfrentamiento).
- `app-android/.../KdmTrackerApp.kt`, `ui/AppChrome.kt`, `ui/AppTopBar.kt`, `ui/SettlementScreen.kt`.
- `app-android/.../UiState.kt` (`population`, `deaths`), `ui/theme/` (`KdmTheme`, `KdmSymbols`, `KdmType`).
- `core/.../rules/SettlementStepCatalog.kt` (the 11 steps whose numbers/labels the carousel mirrors).
