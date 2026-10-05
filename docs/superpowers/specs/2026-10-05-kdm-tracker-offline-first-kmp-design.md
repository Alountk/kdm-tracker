# kdm-tracker — Offline-First Multiplatform Architecture

- **Status:** Draft for review
- **Date:** 2026-10-05
- **Target baseline:** `v0.1.0`
- **Roadmap horizon:** `v0.6.0`+

---

## 1. Context

`kdm-tracker` is a campaign/settlement tracker for Kingdom Death: Monster. It must eventually
support the **complete lantern-year flow** (Settlement → Hunt → Showdown → Aftermath) and run on
**Android as an APK**, with **multi-device sync** so several players can edit the same campaign.

Current state:

- Kotlin 2.3.21 / Spring Boot 4.1.1 / JPA + H2 file DB / Thymeleaf server-rendered.
- All ten domain entities carry `jakarta.persistence` annotations — the domain is framework-bound.
- `DashboardController` is 587 lines and owns the whole settlement cockpit plus every hunt endpoint.
- Only `/api/export` and `/api/import` expose JSON; every other interaction is server-rendered HTML.
- No `Campaign` root; `Settlement` + `lanternYear` is the top-level state.
- Hunt data is wiped on import but never exported (schema stays v2).
- Showdown exists only as a phase flag; Aftermath and a combat model are absent.
- A rich, source-cited step inventory already exists at
  `src/main/resources/static/prototypes/research/lantern-year-steps.md`.

The core problem: the domain **is** the persistence layer and the controllers **are** the business
rules, so the logic cannot move to a device. Without extracting a pure core, a real APK is not
possible.

---

## 2. Goals and non-goals

### Goals

1. Extract a **pure, framework-free domain core** shared by server, Android, and (optionally) desktop.
2. Make the app **offline-first**: the device works without network; the server is a sync hub.
3. Support **multiple players editing** the same campaign concurrently without data loss or confusion.
4. Model the **complete lantern-year flow**, including branching loops and revisitable steps.
5. Keep the **rulebook as data** (rulesets) so future editions and house rules do not require a core rewrite.
6. Ship an **Android APK** from a Compose Multiplatform UI with no heavy design investment now.

### Non-goals (v1)

- Real-time collaborative editing (no WebSocket, no CRDT, no presence).
- A full monster AI/combat **simulator**. The Showdown is a **tracker** (state, wounds, turns, rewards), not an autonomous rules engine.
- iOS target (seams allow it later; not built now).
- Automatic ruleset authoring UI (the seam exists from M1; the UI is backlog).
- Preserving the Thymeleaf web UI (it is retired as the UI; the app is the UI).

---

## 3. Key decisions

| # | Decision | Rationale |
| --- | --- | --- |
| D1 | **Offline-first; server is a sync hub** | KDM is played at a table without guaranteed network. |
| D2 | **Multi-player editing** with `User`, `Campaign`, `CampaignMember` roles (`OWNER`/`EDITOR`/`VIEWER`), invite by campaign code, JWT sessions | Several players edit the same campaign from their own devices. |
| D3 | **Two data classes:** append-only ledgers vs. mutable aggregates | Ledgers (resources, deaths, log, hunt events) commute and never conflict; only a small set of scalars needs conflict handling. This is the key simplification. |
| D4 | **Per-record LWW with Hybrid Logical Clock + tombstones**, client outbox + server cursor | Simple, deterministic, and sufficient for this domain. No CRDT. |
| D5 | **Module architecture:** `:core` (pure Kotlin Multiplatform) + adapters | Breaks the framework coupling and enables device reuse. Dependency rule: `app → data → core`. |
| D6 | **Rules engine vs. ruleset data** | `rules/` holds stable mechanisms; `ruleset/` holds versioned content (numbers, tables, conditions). KDM 1.6 is the bundled default; house rules are per-campaign overrides. |
| D7 | **SQLDelight typed local mirror** + sync columns | Compile-time-safe queries; a `pending_ops` outbox and a `sync_cursor`. Swappable behind ports. |
| D8 | **Compose Multiplatform + Material 3, MVVM, stateless composables** | Functional and themable now; no heavy prototyping, but the layout is replaceable without touching logic. |
| D9 | **Walking skeleton + step catalog first** | Validate the full architecture end-to-end with stubs before investing in features. |
| D10 | **SemVer 2.0.0**, tags `vX.Y.Z`, baseline `v0.1.0` | The app already has a working MVP; `0.1.0` = first usable cut. |
| D11 | **Server storage becomes a change log** (`change_records`), not the old relational schema | Sync is record-based; the server's job is relay + durable backup, not rich reporting. |

---

## 4. Target architecture

```text
kdm-tracker/
├── core/            # Kotlin Multiplatform · PURE (no Spring, no JPA, no Android)
│   ├── domain/      #   Campaign, Settlement, Survivor, Hunt, Showdown, Aftermath, Timeline + enums
│   ├── rules/       #   stable mechanisms: phase/step state machine, calculations, invariants
│   ├── ruleset/     #   versioned content: 1.6 default + per-campaign overrides (house rules)
│   ├── transfer/    #   export/sync DTOs + mappers (schema versioning)
│   ├── usecase/     #   StartHunt, AdvanceStep, ResolveShowdown, AdjustResource, ...
│   └── sync/        #   protocol types: ChangeSet, HLC, tombstones, conflict policy
├── data/            # persistence adapters: SQLDelight schema + repositories (ports impl)
├── app-android/     # Compose Multiplatform UI -> APK
├── app-desktop/     # (optional, later) CMP desktop
└── server/          # Spring Boot: auth + sync hub (+ compatible export/import)
```

**Dependency rule:** `app-* → data → core`. The server depends on `core` and implements the sync
hub; it never leaks Spring/JPA types into `core`.

**Dependencies inside `:core`:** `kotlinx-serialization`, `kotlinx-datetime`, `kotlinx-coroutines`.
Nothing else.

---

## 5. Domain model

| Aggregate | Notes |
| --- | --- |
| `Campaign` (new root) | Owns settlements, members, timeline, and the ruleset reference. |
| `Settlement` | Lantern year, phase, current step, endeavors, population, principles, milestones — minus JPA. |
| `Survivor` | Full model: stats, `survival`, `luck`, weapon proficiency, fighting arts, disorders, gear grid, armor by hit location (head/body/arms/legs/waist), `alive`/retired. |
| `Resource` | **Append-only ledger** entries (gained/spent) + a **derived** inventory projection. No mutable quantity. |
| `Hunt` | Quarry, level, 13-space board position, party (≤4), hunt events, terrain, outcome. |
| `Showdown` (new) | Encounter state: monster, level, AI/wound tracking, round/turn, participant state. |
| `Aftermath` (new) | Rewards, hunt XP, proficiency gains, injuries, deaths — modeled as a distinct step of the Showdown. |
| `Timeline` / `Nemesis` (new) | Lantern-year events, nemesis encounters, quarry list. |

Cross-cutting on every mutable record: `id` (**ULID**, client-generated), `updatedAt` (**HLC**),
`deletedAt` (tombstone), `rev` (server-assigned monotonic revision).

---

## 6. Sync model

### Two data classes

| Class | Examples | Merge | Conflicts |
| --- | --- | --- | --- |
| **Append-only ledger** | resources gained/spent, deaths, log entries, hunt events | union by client `id`, idempotent | **None** — operations commute |
| **Mutable aggregate** | survivor stats, settlement scalars, board position | LWW per record by `updatedAt` (HLC) | Last writer wins; previous value retained in a conflict history for recovery |

### Protocol

- **Pull:** `GET /api/v1/campaigns/{id}/changes?since={rev}` → records with `rev > since`.
- **Push:** `POST /api/v1/campaigns/{id}/changes` → batch of `{entityType, entityId, op, baseRev, payload, updatedAt}`.
- **Deletes:** soft, via `deletedAt`; never hard-delete.
- **Cursor:** the client stores the last applied `rev`.
- **Cadence:** sync on app open, after each mutation, and periodically in the background (WorkManager).

### Auth

- `POST /api/v1/auth/*` for register/login; JWT bearer tokens.
- JWT stored encrypted on device (Android Keystore + DataStore).
- Campaign membership checked server-side on every sync call.

### Server storage

- `change_records(campaign_id, entity_type, entity_id, rev, updated_at, deleted_at, payload JSON)`.
- `users`, `campaigns`, `campaign_members`.
- The old normalized tables are retired as the source of truth after migration.

---

## 7. Phase/step state machine

The rules layer is **not a flat checklist**; the research artifact (`lantern-year-steps.md` §5) makes
these requirements explicit:

- **Revisitable steps with recompute:** derived values (armor points, attribute modifiers, endeavor totals, storage, survival limit) are recomputed from primitives when an earlier step is edited — never hand-entered.
- **Real loops / branches:**
  - Special Showdown (step 9) → loop back to step 5.
  - Nemesis year → skip the Hunt Phase, go straight to the Showdown.
  - Milestones can trigger at any point in the phase, not only at step 6.
  - Hunt failure / TPK → Settlement Phase starts with no returnees (step 3 yields 0 endeavors).
- **Phase-scoped effects inbox:** persistent settlement/story event effects stay "face up" for the whole phase; not one-shot toasts.
- **Gates:**
  - Step 8 (Prepare Departure) only after Develop (step 7) is closed or explicitly abandoned.
  - Step 11 discards unspent endeavors with an explicit confirmation.
  - Crafting validates against storage at the moment of craft; Innovate enforces per-phase counters.
- **One-time campaign flags:** first death, first birth, population-15, campaign end.

---

## 8. App architecture

- **Targets:** `androidApp` (APK) now; `desktopApp` optional later, sharing `:ui`.
- **Pattern:** MVVM — one `ViewModel` per screen consuming `:core` use cases, exposing an immutable
  `UiState`. Composables are **stateless** (state + lambdas).
- **Navigation:** `navigation-compose`.
- **Design:** Material 3 themable base; design tokens isolated in a minimal `:designsystem`
  (reusing `tokens.css` as reference). **No new prototypes.**
- **Persistence:** SQLDelight typed local mirror; every domain table carries `id`, `updatedAt`,
  `rev`, `deletedAt`; plus `pending_ops` (outbox) and `sync_cursor`.
- **Sync client:** background pull → merge → push of the outbox; visible sync state (last sync,
  pending count).
- **Security:** JWT in Android Keystore + DataStore.

### Anti-stopper guarantees

1. Rules and state live in `:core`; the UI never contains business logic.
2. The `ViewModel` seam means screens can be redesigned without touching the domain.
3. Persistence sits behind ports; swapping SQLDelight for Room/Realm does not touch the domain.
4. Design tokens are isolated; changing the look does not touch logic.

---

## 9. Migration

- One-shot script: export the current H2 data with the existing **schema v2** → transform to
  `change_records` → import into the new model. No permanent dual-write logic.
- Keep `/api/export` and `/api/import` compatible during the transition.
- Tag the current commit **`v0.1.0`** as the baseline.
- Retire the Thymeleaf UI as the primary interface; the server keeps auth + sync + compatible transfer.

---

## 10. Roadmap

| Version | Milestone | Contents |
| --- | --- | --- |
| `v0.1.0` | Baseline | Tag of the current working app. |
| `v0.2.0` | Foundations | `:core` pure domain + rules engine + ruleset seam + phase/step state machine + step catalog + walking skeleton (navigable loop with stubs). |
| `v0.3.0` | Offline + sync | SQLDelight + ports + server change-log + auth + sync client. |
| `v0.4.0` | Settlement | Steps 1–11 end-to-end. |
| `v0.5.0` | Hunt + Showdown + Aftermath | Full remaining phases. |
| `v0.6.0+` | Extensibility | House-rules/ruleset UI, timeline/nemesis, monster catalog. |

Implementation starts with the **walking skeleton**, then **Settlement Step 1 (Set Up)** — it is
100% administrative (no decisions), making it the ideal first slice to exercise the entire stack.

---

## 11. Testing strategy

- `:core` tests are **pure unit tests** (no Spring, no Android) — fast and exhaustive for rules,
  state machine, and recompute.
- Sync tested with the ledger/LWW policy: idempotency, tombstones, cursor advancement, conflict
  retention.
- App: ViewModel-level tests for state derivation; navigation smoke tests for the skeleton.
- Server: API tests for auth, membership, pull/push, and export/import compatibility.

---

## 12. Risks and open questions

- **Showdown scope creep:** keep it a tracker, not a simulator. Revisit only after v0.5.0.
- **HLC correctness on device clock skew:** validate `updatedAt` ordering server-side on push.
- **Ruleset boundary:** where a "house rule" stops being data and becomes code. Start strict
  (parameters/lists/tables only) and revisit.
- **Concurrent edits to the same scalar record:** LWW may lose an edit silently. Mitigation: retain
  the previous value and surface recent conflicts in the UI (backlog).
- **Migration fidelity:** verify the v2 export round-trips all current data before retiring H2.

---

## 13. Backlog (explicitly deferred)

- Ruleset/house-rules authoring UI.
- Conflict-resolution UI beyond "last write wins + history".
- Timeline/nemesis full modeling and monster catalog.
- Real-time sync / presence.
- Desktop and iOS targets.
- Campaign finale and `Game Over` end states.
