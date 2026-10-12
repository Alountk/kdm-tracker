# Develop Step — the Innovar Flow (Part A, Phase 2)

**Status:** Draft (awaiting review)
**Depends on:** PR #34 (Phase 1, the fabricar flow) merged — this work edits the step-7 Develop body it ships.
**Shape:** Phase 2 of 3 of "Part A" (the Develop step's three flows from the Stitch design). Phase 1 = Fabricar (done), Phase 3 = Añadir recursos.

## Goal

Implement the Innovar flow — **draw N innovation cards, keep one** — as the design specifies: the Innovaciones section always shows the derived offer, `SELECCIONAR` is the only action, and the section renders its five states (offer, after choosing, already used this phase, no cards available, blocked by cost) without adding a single field to the persisted campaign.

## Source of truth

- **Design:** Stitch screen `projects/4909212909494507782/screens/96edc5b43d954fd5b6a1741e313433b4` — *"Innovaciones: Guía de 5 Estados de Flujo"*. It is the one **higher on the canvas** (`y=5066` vs `y=8678`) and the only one that renders the component in a phone-width column (x 898–1662 ≈ 382 CSS px), which is why it is the reference. Its desktop twin `8f70c90905d44fbead1a857ad462da72` is **discarded** — same prompt, wrong shape. The panel chrome (header, slots, deck row) is cross-checked against the step-7 screen `59fdd89d6100424eb9b5bbf1a6e74ffa`.
- **Card copy:** the 42 innovation card images attached to the Stitch project.
- **Content authority:** the identity CSVs and generated packs (spec `2026-10-10-content-identity-design.md`), as in Phase 1.

## Decisions (validated with the user)

1. **The hand is derived, never stored.** The drawn cards are computed from campaign state, so there is no new field in `SettlementState` and **no transfer-document version bump** (v12 stays). Chosen over persisting the hand (needs a v13) and over drawing transiently in the UI (two devices could see different offers).
2. **There is no `Innovar` button.** The reference shows the offer directly under `Innovar: Elige 1 de 2 Cartas` in the active state; the blocked states show status badges (`Acción de Innovar Agotada`, `INNOVAR NO DISPONIBLE`), not an action. `SELECCIONAR` is the only thing that fires.
3. **The whole copy ships.** The closing reason (quoting the once-per-phase rule), the per-resource `FALTA`/`OK` checklist, and the three "how to unlock more cards" bullets are in. All three are derivable from state we already hold; none of them adds model.
4. **Acquisition state filters first, category second.** The catalogue browser carries two chip groups, in this order: **`Todas` / `Adquiridas` / `No adquiridas`** (the maintainer's — they describe what the deck actually does), and *then* the per-category chips. Both groups filter the **catalogue browser only** — never the hand, which by construction can only hold not-yet-acquired cards. The mock's category order (`Supervivencia / Creencia / Arte / Sociedad`) is dropped as the leading group; the maintainer kept categories behind his own.
5. **The card's category is content.** `identity/innovations.csv` gains a `category` column, threaded through `ingest_innovations.py` into `Innovation.category` — the same move Phase 1 made with the location prose. The values are **authored** (the cards carry them; the CSV does not), so they are flagged for review. The column is what both the second filter group and each card's category badge read; neither is a literal.
6. **The card's subtitle is derived, never authored:** `consecuencia de <madre>` when the unlock tree has a parent for it, otherwise `innovación inicial`. Its footnote follows `consequences` (`Añade consecuencias de <nombre> al mazo`), and its bonus chip follows `survivalLimitBonus`.
7. **One language per render.** `names.es` today; no parenthetical English (content-identity Decision 1).
8. **No `:core` rule change.** `Innovate.run` already enforces the endeavor + resource cost, the once-per-phase limit and the unlock; this phase only decides *which* cards are offered and how they are drawn.

## Design

### Model

A pure draw next to the access rules it depends on:

```kotlin
object InnovationDraw {
    /** The [ruleset.innovationDraw] cards [record] would be offered right now. */
    fun of(record: SettlementRecord, catalog: ContentCatalog, ruleset: Ruleset): List<Innovation>
}
```

- Candidates = the innovations `InnovationAccess` classifies over `record.state` as **AVAILABLE** (in the deck: a parent is unlocked) — never `UNLOCKED`, never `NO_ACCESS`.
- Order = ascending `hash(record.settlement.campaignId, innovationId)`. Stable across devices, stable while the user decides, and varied per campaign. Taking the first N lets a newly unlocked card appear without reordering the ones already shown.
- `min(candidates.size, ruleset.innovationDraw)` are returned; `innovationKeep` bounds how many `SELECCIONAR` affordances are enabled (1 under 1.6, N under a house rule).
- Empty `AVAILABLE` ⇒ empty hand ⇒ `NO_CARDS`, which is the design's own empty state.

`InnovationsModel` grows to carry the section's five states:

```kotlin
enum class PhaseState { OFFER, USED_THIS_PHASE, NO_CARDS, UNAFFORDABLE }

data class InnovationCard(
    val id: InnovationId,
    val name: String,          // names.es
    val category: String,      // from identity/innovations.csv
    val subtitle: String,      // derived: "consecuencia de X" | "innovación inicial"
    val effect: String,        // Innovation.description
    val survivalBonus: Int,    // survivalLimitBonus
    val consequenceNote: String?, // "Añade consecuencias de X al mazo" when consequences is not empty
)

data class ResourceNeed(val name: String, val have: Int, val need: Int) { val ok get() = have >= need }

data class CostView(
    val endeavors: ResourceNeed,     // the action's endeavorCost vs the settlement pool
    val resources: List<ResourceNeed>, // the action's resourceCost vs storage
    val ok: Boolean,
    val reason: String,
)
```

`PhaseState` resolves in a fixed order — `USED_THIS_PHASE` (the location's action already ran) > `NO_CARDS` (hand empty) > `UNAFFORDABLE` (`!cost.ok`) > `OFFER` — so a section can only ever be in one of them, and the panel never has to guess.

### Components

1. **Slots** — not milestones: one slot per **principle already chosen at a milestone** (`state.chosenPrinciples`, filled by `ChoosePrinciple` in steps 5/6, one per `PrincipleCategory`), plus empty slots for the categories not chosen yet. The design's fourth, `MAZO ACTIVO` (`Cúmulo Linternas / Robando 2 cartas… / DESPLEGADO`), is not a principle — it is the built location whose `INNOVATE` action feeds the draw, derived from the catalog.
2. **Deck row** — `Mazo de Innovación (N Descubribles)` with its catalogue toggle, then the **acquisition chips first**: `Todas (N)`, `Adquiridas (N)`, `No adquiridas (N)` (counts over what is unlocked), and only **after them** one chip per distinct `Innovation.category`, each with its count. Labels come from the content; only `Todas`/`Adquiridas`/`No adquiridas` are literals, and they are the maintainer's wording. Both groups filter the browser, not the hand.
3. **Offer header** — `Innovar: Elige 1 de 2 Cartas`, the cost line (`COSTE: N ✳ + <chips>`), and the official rule line.
4. **The hand** — a **2-column grid** of `InnovationCardView`: category badge, subtitle, name, bonus chip, effect, footnote, and the action — `✓ SELECCIONAR` (gold) when selectable, `INERTE` in `USED_THIS_PHASE`, `REQUIERE COSTE` with a lock in `UNAFFORDABLE`.
5. **State banners** — `Acción de Innovar Agotada` + `1 DE 1 USADO` + `YA UTILIZADO (AÑO 1)`/`BLOQUEO TEMPORAL` (3); `MAZO: 0 CARTAS` + `No Quedan Innovaciones en el Mazo` + the three help bullets (4); `INNOVAR NO DISPONIBLE` + `Cartas a robar en la oferta: Vista previa no interactiva` (5).
6. **Cost block** — the `FALTA`/`OK` checklist and the `Motivo:` sentence, both built from `CostView`.

### Data flow and behaviour

| UI action | Callback (existing) | Path |
|---|---|---|
| `SELECCIONAR` | `onInnovate(innovationName)` | controller resolves the name → `Innovate.run(record, locationId, id, catalog)` — same transaction, same blockers as today |

On success the deck **recomputes from the state we just wrote**: the chosen card leaves `AVAILABLE` (it is `UNLOCKED` now) and its `consequences` enter `InnovationDeck`, so the next offer is a different hand built on the card that was taken. Nothing is stored to make that happen — it falls out of the derived hand.

Everything else (deck size, slots, `drawLabel`, the phase limit) keeps its current wiring.

### Retired in this phase

The disabled `Innovar` `KdmActionButton` in `InnovationsPanel` — the reference has no such button. Nothing else is removed.

## Out of scope

- Phase 3 (Añadir recursos), the bodies of steps 2–6 and 8–11, S3 (English catalog), S4 (language selector).
- Persisting the hand, or any change to the transfer document.
- Seeding a fresh settlement's innovations: `SettlementState.innovations` starts empty, so a campaign that reaches step 7 with nothing unlocked renders `NO_CARDS`. The normal setup flow picks a principle before then; if the gate shows otherwise it is a content/seed question, not a rules one.

## Testing

- **`InnovationDrawTest` (JVM, `:core`):** returns exactly `ruleset.innovationDraw` when more are available; returns fewer when they are not; never returns an `UNLOCKED` or `NO_ACCESS` card; the same state yields the same hand twice (determinism); the hand is identical for two campaigns with different ids only in its *choice*, never in its *order* relative to a given id (stability); an empty deck yields an empty hand.
- **`InnovationsModel` tests (JVM, app — the assertions today in `DevelopPanelsTest`, split out if they grow):** the `PhaseState` priority table, one state per input; `CostView.ok` and the `FALTA`/`OK` split for a short ledger; the subtitle and footnote derivation; the acquisition chips and the category chips both as counts, and that the acquisition group is the one rendered first.
- Port every existing test that touches `InnovationsModel` (the `drawLabel`/slots assertions stay).
- Gate: `./gradlew spotlessApply && ./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck`, plus `python3 -m unittest discover -s tools/content-ingest/tests`.

## Visual gate (hard rule)

Install on the emulator (AVD `kdm`, 1080×2400), open step 7, and measure the section against the reference in all five states — 2-column hand with `✓ SELECCIONAR`, the cost line, the state banners, the checklist and the empty state. Capture, present, **stop** — no commit, push or PR until the maintainer approves the measured render.

## Risks

- **Authoring 42 categories** is invented content with no card to transcribe (same exposure as Phase 1's six seed descriptions); they are flagged for review in the spec and in the PR.
- **A hand that reorders while the user reads it** would feel broken; the id-hash ordering is what prevents it, so it is pinned by a test rather than left to implementation.
- **Two devices seeing different offers** was the failure the persisted hand would have prevented; the derived hand only stays safe while the ordering is a pure function of campaign state — any future RNG would silently reintroduce the problem.
