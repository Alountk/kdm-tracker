# Showdown Phase — Design (Slice 2)

**Status:** Draft (awaiting review)
**Slice:** The Showdown Phase: set up an encounter (monster + level), track the monster's AI deck as its health, reference its hit locations, and record the outcome and the aftermath (rewards, hunt XP, deaths). Reused by the Special Showdown (slice 3).
**Depends on:** PR #20 (Hunt phase, slice 1), #18/#19 (survivors). Part 2 of 3 of the Hunt + Showdown effort.

## Goal

The Showdown is a stub. This slice makes it a faithful tracker for the table-driven combat:

1. **Set up** the encounter: the monster (from the catalog's quarries + nemeses) and its level (1–3).
2. **Track the monster's health**: the AI deck is the monster; **drawing** cycles a card (the monster's turn) and **wounding** removes one to the wound stack. Victory when the AI cards are all wounded; the Basic Action once the deck is empty.
3. **Reference the hit locations** deck for the monster.
4. **Record the outcome** (victory / defeat) and the **aftermath**: rewards to storage, hunt XP, and deaths.
5. Reuse the same encounter for the **Special Showdown** (step 9) — slice 3.

## Design (validated with the user)

### Content
- `MonsterCard(name, text, kind, monster, rank = null)` with `MonsterCardKind { AI, BASIC_ACTION, HIT_LOCATION }` and, for AI cards, a `rank` (`BASIC` / `ADVANCED` / `LEGENDARY`, from the card's corner symbol when readable).
- Ingest `AF Enfrentamientos/<monster>/`: `AF AI <monster>.pdf` (AI deck), `AF AI <monster> - Acción Básica.pdf` (basic action), `AF HL <monster>.pdf` (hit locations). A curated deck→monster map (the folder name → the catalog monster, e.g. `León Blanco → White Lion`, `Carnicero → The Butcher`).
- `ContentBundle.monsterCards` + `ContentCatalog.monsterCardsFor(monster)` (by `MonsterCardKind`).

### Model (`:core`)
- `ShowdownStatus { ACTIVE, VICTORY, DEFEAT }`.
- `ShowdownState(monster: String, level: Int, aiDeck: List<String> = emptyList(), discard: List<String> = emptyList(), wounds: List<String> = emptyList(), lastDrawn: String? = null, status: ShowdownStatus = ACTIVE)`.
  - The AI deck is the monster's health: **draw** moves the top card to `discard` (the monster's turn); **wound** moves the top card to `wounds`. When `aiDeck` is empty, the next action reshuffles `discard` back into it; victory when `aiDeck` and `discard` are both empty and `wounds` holds the whole deck.
- `SettlementState.showdown: ShowdownState = ShowdownState()`.

### Use cases (`:core`)
- `SetUpShowdown.run(record, monster, level, deck: List<String>)` — sets the encounter; the deck is the assembled AI deck (player-provided or the content's full list).
- `DrawShowdownCard.run(record)` — the monster's turn (reshuffles when empty).
- `WoundMonster.run(record)` — a wound (reshuffles when empty); when the last card is wounded the outcome becomes victory-eligible.
- `SetShowdownOutcome.run(record, status)` — records victory/defeat (only from `ACTIVE`).
- The aftermath reuses `RecordDeaths` (defeat) and `AddResourceEntry` (rewards); `RecordHuntXp` grants +1 hunt XP to each surviving participant.

### `:app-android`
- `UiState.showdown: ShowdownState?`, `merge` of the monster card decks; the `ShowdownScreen` body: the monster header (name + level), the AI deck tracker (`Draw`, `Wound`, the remaining/wounded counts, the last drawn card's text), the hit-locations reference, the outcome actions, and an aftermath panel (add rewards / record deaths / grant hunt XP).

## Scope

### In
- `:core`: `MonsterCard` content type + catalog; `ShowdownState` + `SettlementState.showdown`; the showdown use cases; `RecordHuntXp`; transfer **v9**.
- Content: the `AF Enfrentamientos` ingest pipeline + generated Kotlin.
- `:app-android`: the Showdown body + the aftermath panel; the controller methods.

### Out (explicitly deferred)
- The **Special Showdown** reuse and the **nemesis-year skip** — slice 3.
- The **playable, card-faithful gear grid** (the deferred big grid): rendering gear cards with all their content and per-location armor. A later slice.
- Extracting the **per-level AI deck composition tables** from the rulebook (the player assembles the deck).
- Monster *attributes* (movement/toughness/speed/damage) typed — the app shows the Basic Action text; it does not model the monster's stats.
- Automatic reward content (the aftermath's rewards are entered by hand).

## Architecture

### `:core`
- `domain/MonsterCard.kt` (+ `MonsterCardKind`); `domain/ShowdownState.kt` (+ `ShowdownStatus`); `SettlementState.showdown`.
- `content/{ContentPack,ContentCatalog}.kt` (monster cards + `monsterCardsFor`).
- `usecase/{SetUpShowdown,DrawShowdownCard,WoundMonster,SetShowdownOutcome,RecordHuntXp}.kt`.
- `transfer/TransferDocument.kt` → v9.

### `:app-android`
- `ui/ShowdownScreen.kt` rewritten (header + AI tracker + hit locations + outcome + aftermath).
- `UiState.showdown`; `LanternYearController` methods; `KdmTrackerApp` wiring.

## Data flow

`SettlementState.showdown` → `UiState.showdown` → the Showdown body. Mutations go through the controller → `SettlementRepository.update`. The outcome drives the existing flow (Showdown → Settlement step 1 via `LanternYearFlow`), with the aftermath recorded before that.

## Testing

- **`:core`** — strict TDD: the model round-trips (serialization/transfer v9); `SetUpShowdown`; `DrawShowdownCard`/`WoundMonster` (including the reshuffle when the deck empties and the victory condition); `SetShowdownOutcome`; `RecordHuntXp`; `monsterCardsFor`.
- **Python** — `ingest_monsters` fixture tests (AI/HL/basic-action recognition, the deck→monster map, integrity).
- **`:app-android`** — controller tests for the showdown methods and the outcome routing; no Compose UI tests.
- Full suite green; the manual emulator pass (set up an encounter, draw/wound, the aftermath).

## Risks

- **The AI deck reshuffle/victory rule** is subtle — pin the deck/discard/wounds transitions and the victory condition with tests.
- **Content shape** — the AI cards mix targeting columns and attack tables; the parser extracts name + text (+ rank when readable). The Basic Action card is two-sided; treat it as a `BASIC_ACTION` card. Pin with fixtures.
- **Level assembly is content-uncertain** — the deck is player-provided; `SetUpShowdown` accepts the deck list so the app never invents the composition.
- **Scope** — the Showdown body is large; keep the model to deck/discard/wounds/outcome, not a combat engine.

## References

- Research: `server/src/main/resources/static/prototypes/research/lantern-year-steps.md` (§4 Showdown).
- Rulebook: `KDM 1.5 EdESP Reglamento - Digital.pdf` "Preparación del Enfrentamiento".
- Content: `external-assets/Juego Completo/Formato Americano 57x88 mm/AF Enfrentamientos/<monster>/*.pdf`.
- Existing: `app-android/.../ui/ShowdownScreen.kt`, `core/.../usecase/{RecordDeaths,AddResourceEntry}.kt`, `tools/content-ingest/ingest_hunt_events.py`.
