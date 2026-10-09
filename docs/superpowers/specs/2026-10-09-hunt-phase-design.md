# Hunt Phase — Design (Slice 1)

**Status:** Draft (awaiting review)
**Slice:** The Hunting Phase: choose a quarry and level, set up the 13-space board, resolve its hunt events, track the position and Overwhelming Darkness, and record the outcome — with the party (from Step 8) and optional per-player assignment. Faithful, tracker-level (the app records; the table plays).
**Depends on:** PR #18 (survivor roster), #19 (survivor UI). Part of the "Hunt + Showdown + Special Showdown" effort; this is slice 1 of 3.

## Goal

The Hunt Phase is a stub. This slice makes it a real, faithful tracker:

1. **Choose** the quarry (from the catalog) and the monster **level** (1–3), and set the **player count** (1–4).
2. **Set up** the 13-space hunt board: mark Start, Overwhelming Darkness, Starvation and the monster's space; place the hunt events on the other spaces (from the ingested decks).
3. **Resolve** events space by space (draw → show the text → mark resolved → advance), following the revealer/board rules the table plays.
4. **Track** the position, the first-entry Overwhelming Darkness, and event damage applied to the party.
5. **Record** the outcome: quarry found, ambush, starvation (hunt fails), run away, or hunt fails (TPK).
6. Support the **table-play fast path**: mark the outcome directly, without touching the board.

## Design (validated with the user, browser mockups)

### Hunt body (board option A)
- **Header**: quarry + level (editable), players (1–4), and the **party** (the departing survivors from Step 8) with their optional player assignment (`P1: Eli, Marcus · P2: …`).
- **13-space track** (horizontal, scrollable): each space shows its kind (Start / normal / E / Overwhelming Darkness / monster) and its progress (done / current); the monster sits on the last space. Tapping a space opens its **events panel**.
- **Events panel** (current or tapped space): the assigned events, each expandable to its text, with a `mark resolved` control, plus `assign event` from the deck. A resolved space is "clear".
- **Outcome actions** (always available): `Find quarry`, `Ambush`, `Starvation`, `Run away`, `Hunt fails` — the fast path for table play.
- **Event damage**: an event that wounds a survivor opens the shared **Survivor editor** on that party member.

### Content
- Ingest the `AF Caza` decks: **basic** and **special** events from `AF Caza Básicos.pdf`, and each monster's events from `AF Caza León/Antílope/Fénix.pdf`.
  - `HuntEvent(name: String, text: String, kind: HuntEventKind, monster: String? = null)` with `HuntEventKind { BASIC, SPECIAL, MONSTER }`.
  - The 12 identical "Evento Aleatorio" instruction cards dedupe to one entry; the d100 hunt-event table itself is **not** in the decks (it is on the board), so "random event" is a manual record.
- New `ContentBundle.huntEvents` + `ContentCatalog.huntEventDefinitions` (+ a `huntEventsFor(quarry)` helper: the basic/special deck plus that monster's events). The bundle lives in `Legacy15Content` (the ES 1.5 decks).

### Model (`:core`)
- `HuntSpaceKind { START, NORMAL, MONSTER_E, OVERWHELMING_DARKNESS, STARVATION, MONSTER }`.
- `HuntSpace(kind: HuntSpaceKind = NORMAL, events: List<String> = emptyList(), resolved: List<String> = emptyList())` — `events`/`resolved` are event **names**.
- `HuntState(quarry: String? = null, level: Int = 1, players: Int = 1, assignments: Map<Ulid, Int> = emptyMap(), spaces: List<HuntSpace> = emptyList(), position: Int = 0, status: HuntStatus = ACTIVE)`.
- `SettlementState.hunt: HuntState = HuntState()`.
- **The board layout is player-set**: the app seeds a default (Start at index 0, the monster at index 12, the rest normal) and the player marks Overwhelming Darkness / Starvation / the E spaces — the exact diagram is a per-monster image (not extractable), so the app cannot auto-place it.

### Use cases (`:core`)
- `SetUpHunt.run(record, quarry, level, players)` — seeds the 13 spaces + quarry/level/players.
- `AssignPlayer.run(record, survivorId, player)` — the player↔survivor assignment.
- `AssignHuntEvent.run(record, spaceIndex, eventName)` / `ResolveHuntEvent.run(record, spaceIndex, eventName)`.
- `MarkSpaceKind.run(record, spaceIndex, kind)` — the editable board (OD / Starvation / E / monster).
- `AdvanceHunt.run(record)` — move to the next space.
- `SetHuntOutcome.run(record, status)` — `QUARRY_FOUND` / `AMBUSH` / `FAILED` / `RUN_AWAY` (`STARVATION` maps to `FAILED`).

### Flow
- Hunt → Showdown when the outcome is `QUARRY_FOUND`/`AMBUSH`; Hunt → Settlement restart when `FAILED`. This reuses `LanternYearFlow` (`huntFailed`) and the existing controller `advance`/`failHunt`.
- Nemesis years skip the Hunt — **slice 3** (not here).

## Scope

### In
- `:core`: the hunt model + `SettlementState.hunt`; the use cases; the hunt-event content type + catalog.
- Content: the `AF Caza` ingest pipeline + the generated Kotlin.
- `:app-android`: the Hunt body (board A) + the party/players header + the events panel + the outcome actions; the controller methods; `UiState.hunt`.
- Transfer v8 (the new `hunt` field).

### Out (explicitly deferred)
- The **Showdown** body and monster content — slice 2.
- The **nemesis-year skip** and the **Special Showdown reuse** — slice 3.
- Any **random draft** of hunt events (the app records what the table drew).
- The d100 hunt-event **table** content (on the board, not extractable).
- **Spectators / read-only members** — a separate platform slice.
- Hunt-event *effects* (the app shows text; survivors are edited through the shared editor).

## Architecture

### `:core`
- `domain/HuntEvent.kt` (+ `HuntEventKind`), `domain/HuntState.kt` (+ `HuntSpace`, `HuntSpaceKind`); `SettlementState.hunt`.
- `content/ContentPack.kt` + `ContentCatalog.kt` (hunt events, incl. `huntEventsFor(quarry)`).
- `usecase/{SetUpHunt,AssignPlayer,AssignHuntEvent,ResolveHuntEvent,MarkSpaceKind,AdvanceHunt,SetHuntOutcome}.kt`.
- `transfer/TransferDocument.kt` → v8.

### `:app-android`
- `ui/HuntScreen.kt` rewritten on the board; new `ui/HuntBoard.kt` + `ui/HuntEventPanel.kt`.
- `UiState.hunt: HuntState?`; `LanternYearController` methods (`setUpHunt`, `assignPlayer`, `assignHuntEvent`, `resolveHuntEvent`, `markSpaceKind`, `advanceHunt`, `setHuntOutcome`).
- The outcome buttons route through the controller; `QUARRY_FOUND`/`AMBUSH` advance to the Showdown, `FAILED` restarts the settlement.

## Data flow

`SettlementState.hunt` → `UiState.hunt` → the Hunt body. Mutations go through the controller → `SettlementRepository.update` → store. The outcome drives `LanternYearFlow` via the existing `advance`/`failHunt`.

## Testing

- **`:core`** — strict TDD: the model round-trips (serialization/transfer v8); `SetUpHunt` seeds 13 spaces; `AssignHuntEvent`/`ResolveHuntEvent`/`MarkSpaceKind`/`AdvanceHunt`; `SetHuntOutcome`; `huntEventsFor(quarry)` includes the basic + that monster's events.
- **Python** — `ingest_hunt_events` fixture tests (card recognition, dedupe of the 12 random-event cards, no empty cards).
- **`:app-android`** — controller tests for the hunt methods + the outcome→screen routing; no Compose UI tests.
- Full suite green: `./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck`.
- Manual emulator pass: set up the board, resolve an event, advance, record the outcome.

## Risks

- **Board layout is content-uncertain** (the diagram is an image) — the board is player-set with a default; pin the default and the edit path.
- **Content ingest shape** — the hunt decks mix instruction cards, specials and basics; the parser must dedupe and reject empty cards. Pin with fixtures.
- **Outcome → flow** — the mapping to `LanternYearFlow` must not break the existing nemesis/TBD transitions; pin with tests.
- **Scope creep** — the hunt is a big body; keep the board model small (spaces + events), not a rules engine.

## References

- Mockup (persisted): `.superpowers/brainstorm/18605-1791547123/content/hunt-board-options.html` (option A).
- Research: `server/src/main/resources/static/prototypes/research/lantern-year-steps.md` (§3 Hunt); the rulebook `KDM 1.5 EdESP Reglamento - Digital.pdf` "Preparación del Tablero de Caza" (13 spaces: Start / Overwhelming Darkness / Starvation).
- Content: `external-assets/Juego Completo/Formato Americano 57x88 mm/AF Caza/*.pdf`.
- Existing: `core/.../domain/HuntStatus.kt`, `core/.../rules/LanternYearFlow.kt`, `app-android/.../ui/HuntScreen.kt`, `tools/content-ingest/ingest_settlement_events.py`.
