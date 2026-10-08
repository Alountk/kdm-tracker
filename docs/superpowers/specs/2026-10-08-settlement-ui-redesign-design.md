# Settlement UI Redesign — Design

**Status:** Draft (awaiting review)
**Slice:** Redesign the Settlement cockpit so the step navigation stops consuming half the screen, and make the **Develop** step usable: grouped, filterable, with availability shown by colour.
**Depends on:** PR #7–#13 (domain, content packs, persistence, Craft, gear extraction).

## Goal

The current cockpit renders the 11-step rail as a wide left column (~half the screen), and Develop lists everything as long flat stacks (57 resources, 44 innovations, all recipes). Finding anything means scrolling forever. This slice replaces the chrome and the Develop body.

Success is:
1. The step navigation is a thin, always-visible strip — the content gets the full width.
2. **Recursos** are grouped by source, **Innovar** by access state, **Fabricar** by location and by type.
3. Availability is visible at a glance: green = you have it / unlocked; blue = available now; grey = not yet (with what's missing).
4. Rules stay in `:core`; the screens render state and report intent.

## Design (validated with the user, see the mockups)

### Chrome
- A **thin top step bar**: the 11 steps as small numbered chips; the current step highlighted; tap to jump (respecting the existing reachability gate).
- Below it, the three **Develop tabs**: **Recursos / Innovar / Fabricar**. Content is full-width.
- Home / Prev / Next stay.

### Recursos tab
- Grouped by **source** (Básicos, León Blanco, Fénix, Antílope Bramador, Carnicero, …).
- Filter chips: **Básicos / <fuente> / Todos**.
- Each resource row has its `+1` (the existing manual bridge).

### Innovar tab
- Innovations are a **unlock tree** — not all are reachable at the start, and the research cost is the **same for all**. So the axis is **access state**, not theme.
- Filter chips: **Todas / Disponibles / Desbloqueadas / Sin acceso**.
- Colour: **green** = unlocked, **blue** = available (its parent is unlocked → it is in the deck), **grey** = no access (shows the missing parent, e.g. *"requiere Pintura"*).
- This derives entirely from `Innovation.consequences` (the deck = the consequences of the unlocked innovations); validated against `external-assets/arbol_de_innovaciones_kdm_1_5.md`. **No new model.**

### Fabricar tab
- Grouping toggle: **Por localización** (Peletería, Felinarium, Herrería, Círculo de Piedra, …) and **Por tipo** (Armas / Armaduras / Objetos / …). Under "por localización" each location is sectioned by type.
- Availability: a recipe is **craftable** (green, "Crear") when `Craft` would return `Ok`; otherwise **grey**, showing what is missing (resources / the location unbuilt / the innovation prerequisite).
- Type comes from the gear's `keywords` (`arma` / `armadura` / `objeto` / …) — **no new model**.

## Scope

### In
- The redesigned cockpit chrome + the three Develop tabs in `:app-android`.
- `:core` pure helpers for the derived views (source grouping, innovation access state, craft affordability), TDD'd.
- A small **content** addition: a **source** on each resource (`ResourceDefinition.source`), mapped from the 1.5 resource decks.

### Out (deferred, explicitly)
- Other steps' bodies (still `Pendiente`), the Home, and any new **rules**.
- Re-designing Innovar's "theme" (rejected: the state axis is the right one).
- Survivor model, gear `modifiers`, Ruling CB.

## Architecture

### `:core` (pure, additive)
- `ResourceDefinition.source: String = ""` (the monster/deck a resource comes from) — additive, defaulted.
- `ContentCatalog.resourceGroups(): Map<String, List<ResourceDefinition>>` — resources by source, deterministic.
- `InnovationAccess` — given a `SettlementState`, classify each catalog innovation as `UNLOCKED` / `AVAILABLE` (in `InnovationDeck.of(state.innovations)` — i.e. a consequence of something unlocked) / `NO_ACCESS` (else, with the parent that would unlock it). Pure + tested.
- `Craftability` — given a `SettlementRecord` + a `GearRecipe` + the catalog, whether `Craft` would succeed, and if not, why (a display reason). Reuses `Craft`/`AddResourceEntry`; no duplicated rules.
- An innovation's **type** is not modelled; the type grouping for gear reads `Gear.keywords`.

### `:app-android`
- Replace `SettlementScreen`'s left rail with the **top step bar**; the body spans full width.
- `DevelopStepScreen` becomes a **tabbed** screen: `RecursosTab`, `InnovarTab`, `FabricarTab` (new composables), each rendering the derived, grouped, colour-coded lists from `UiState`.
- `UiState` gains the derived views (resource groups, innovation access states, craftability per recipe).
- Controller/ViewModel unchanged in behaviour: the tabs report the same `addResource` / `innovate` / `build` / `craft` intents.

## Data flow

`ContentCatalog` + `SettlementRecord` → `:core` derived helpers → `UiState` → the tabs. Every action still goes through the existing controller methods and persists via `SettlementRepository.update`.

## Testing

- **`:core`** — strict TDD for `resourceGroups`, `InnovationAccess` (unlocked / available / no-access, incl. the missing parent), and `Craftability` (affordable vs each blocking reason).
- **`:app-android`** — controller/ViewModel tests asserting the derived views (grouping counts, access states, craftable flags) over a seeded store; no Compose UI tests.
- Content: a fixture pinning the resource→source mapping.
- Full suite green: `./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck`.

## Risks

- **Resource → source mapping** — the biggest unknown: it is a content pass over the 1.5 resource decks (Básicos vs per-monster). Unmapped resources fall back to a "Otros" group, and the mapping is pinned by a fixture.
- **Compose restructuring** — moving from rail to top bar and tabbing Develop is a real UI rewrite; keep it stateless and drive every list from `UiState`.
- **Innovation access correctness** — the `consequences` graph must be read the right way (deck = consequences of unlocked). Pin with tests against the tree.

## References

- Mockups (persisted): `.superpowers/brainstorm/54671-1791454118/content/develop-A-final.html` (+ v2/v3/v4)
- Innovation tree: `external-assets/arbol_de_innovaciones_kdm_1_5.md`
- Existing screens: `app-android/.../ui/{SettlementScreen,DevelopStepScreen}.kt`
