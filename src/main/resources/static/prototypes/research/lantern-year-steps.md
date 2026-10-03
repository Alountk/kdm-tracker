# Kingdom Death: Monster — Lantern Year structure (research artifact)

Method: content cross-checked across the Fandom wiki, the Miraheze `kingdomdeath.wiki` (which cites the **1.6 Core Rulebook by page**), and the **official Settlement Record Sheet PDF**. Every factual block is tagged with its source. Lines without a fetched source are marked `[sin fuente]`. BGG threads returned HTTP 403 to the fetcher; where only a search-result snippet was visible, it is labeled as such.

---

## 1. The Lantern Year loop

A campaign session = one cycle of phases: **Hunt → Showdown → Settlement**, repeated until the campaign finale (source: kingdomdeath.wiki `Campaign`, citing *KDM 1.6 Core Rulebook pg. 36*). Expressed as a loop starting from a settled state:

| # | Phase | One-line purpose |
|---|-------|------------------|
| 1 | **Settlement Phase** | Resolve the year: heal returnees, advance the timeline, draw the settlement event, check milestones, spend endeavors/resources (craft, innovate, build), pick the next hunt party. |
| 2 | **Hunt Phase** | Choose quarry + level, lay out the hunt board, resolve hunt events space-by-space until the survivors reach (or are reached by) the monster — or the hunt fails. |
| 3 | **Showdown Phase** | Battle the monster to victory or defeat; resolve the **Aftermath** (rewards, hunt XP, proficiency, deaths). |
| → | back to **Settlement Phase** | The settlement "typically advances by one lantern year as part of each Settlement Phase" (Fandom `Lantern Year`). |

Edge exits that still loop back:
- **Hunt fails / all survivors perish on the hunt** → "The lantern year is over, and a new Settlement Phase begins" (Fandom `Hunt Phase`; 1.6 rulebook pg. 63 per kingdomdeath.wiki `Hunt Phase`).
- **Nemesis Encounter year** → "Complete the settlement phase normally. Departing survivors skip the Hunt Phase" and go straight to the showdown (Fandom `Nemesis Monster`).
- **Campaign end** → milestones `Game Over` (population reaches 0) and the campaign finale monster (official Settlement Record Sheet PDF; Fandom `Settlement Phase` intro).

---

## 2. SETTLEMENT PHASE — step by step (exhaustive, in official order)

Source unless noted: **kingdomdeath.fandom.com/wiki/Settlement_Phase** (11 numbered steps). Step numbering is corroborated by Fandom `Nemesis Monster` (links to "#5. Update the Death Count") and by a BGG-thread snippet quoting "Step 2 … Heal returning survivors, remove tokens" (snippet only, page not directly fetchable).

### Step 1 — Set Up the Settlement
- **Mechanics:** Place Settlement Board; place gear grids + record sheets of returning survivors; place Settlement Record Sheet (pencil!); shuffle and place the Settlement Event deck face down; lay out built Settlement Locations; lay out current Innovations and Principles; shuffle the Innovation Deck (must contain exactly the consequence-innovations your settlement has unlocked) and place it on the board.
- **Player decisions:** None — pure setup. (Composition of the Innovation Deck is a *derived state* that must be correct.)
- **Auto vs decision:** 100% administrative.
- **Tracker state:** settlement name; current innovations list (+ weapon masteries); principles by category (Death / New Life / Society / Conviction); built settlement locations; survival limit; death count; timeline (years + story/special events); quarry list; nemesis list; Innovation Deck contents (which consequence cards are in the deck); settlement storage; unused-gear list. *(Sections enumerated from the official Settlement Record Sheet PDF.)*

### Step 2 — Survivors Return
- **Mechanics:** All survivors who endured the previous showdown are **returning survivors**. Heal: erase filled light/heavy injury levels; remove **all** tokens. Survival and Insanity **do not** refill. Permanent injuries do **not** heal (some events/innovations may). Then apply return effects from principles, settlement events, and innovations (bonuses *and* penalties). Possible to have zero returnees.
- **Player decisions:** Nominate survivors when an effect says so (e.g., principle/settlement-event text); track which effects apply to whom.
- **Auto vs decision:** Healing/token removal = automatic. Principle/event applications = conditional, player-resolved.
- **Tracker state:** per-survivor injury level reset; token clearing (list of tokens removed); survival/insanity kept as-is; permanent injuries kept; flag "returning" vs "not returning" per survivor; any return-phase buffs/penalties applied.

### Step 3 — Gain Endeavors
- **Mechanics:** The settlement gains **+1 endeavor per returning survivor**. Innovations/principles may grant additional endeavors when criteria are met. Tracked with tokens.
- **Player decisions:** None (counting), but bonuses require checking innovation/principle conditions.
- **Auto vs decision:** Automatic + conditional bonuses.
- **Tracker state:** total settlement endeavors for this phase (base + bonuses, ideally with a breakdown of sources).

### Step 4 — Update Timeline  *(three sub-events, in this order)*
1. **Advance the lantern year** — fill the next unfilled lantern-year box on the timeline; this is the new current year. *(Automatic.)*
2. **Draw + resolve the settlement event** — draw the top Settlement Event card, perform it, leave it face up as a reminder of persistent effects (new endeavor sinks, crafting offers, lasting bonuses/restrictions). Timeline-preset settlement events are drawn **in addition**; the same event card cannot be played twice in one phase. *(Semi-automatic: draw is random, resolution usually contains player choices.)*
3. **Trigger story events listed for this year** — resolve one at a time in listed order. If the year shows a **Nemesis Encounter**, follow nemesis rules (choose monster + level per the four timeline formats: `Nemesis Encounter`, `- Lvl X`, `- Monster Name`, `- Monster Name Lvl X`; fill the chosen box on the record sheet). *(Automatic trigger; the event text drives decisions.)*
- **Player decisions:** whatever the drawn event/story event asks (votes, nominations, die rolls, spending endeavors); nemesis monster + level choice when the timeline entry is open.
- **Tracker state:** current lantern year; settlement event card ID + persistent effects; story events resolved this year (IDs + outcomes); nemesis selection (monster, level) recorded; new timeline entries added by events.

### Step 5 — Update the Death Count
- **Mechanics:** Record every survivor who perished during the previous hunt/showdown **or during the preceding settlement steps**; if someone dies later in the phase, update **immediately**. **First time the death count is ever updated → trigger the `Principle: Death` story event.**
- **Player decisions:** `Principle: Death` is a forced binary: **The First Harvest → Cannibalize** or **The First Grave → Graves**, then roll 1d10 for the follow-on effect (Fandom `Principle: Death`). Corpus handling = "what do we do with their first survivor corpse".
- **Auto vs decision:** Counting = administrative; the principle choice = hard player decision (one-time, permanent).
- **Tracker state:** death count total; list of dead survivor IDs (when/where they died); flag `death_count_first_update_done`; chosen Death principle + its roll outcome; population decrease (when population drops, players nominate which survivors die — Fandom `Population`).

### Step 6 — Check Milestones
- **Mechanics:** Conditional story events printed below the timeline. Check the record sheet; if more than one is reached, resolve **in any order**; tick each box; trigger the event. If a milestone is reached **later** in the phase, resolve it immediately. With **Strain Milestones**: check them first, apply Permanent Effect, unlock strain fighting arts into the deck, then **remove the reached strain card permanently**. With **Patterns**: on the Insight milestone, draw a random pattern card and record it.
- **Official milestone conditions** (official Settlement Record Sheet PDF, "Milestone Story Events"): `Principle: Death` = 1 death · `Principle: New Life` = first child is born · `Principle: Society` = population reaches 15 · `Hooded Knight` = settlement has 5 innovations · `Game Over` = population reaches 0 (listed twice).
- **Player decisions:** `Principle: New Life` → **Without Mercy → Survival of the Fittest** or **With Affection → Protect the Young** (+1d10 effect) (Fandom `Principle: New Life`). `Principle: Society` → **Collective Toil** (−1 population, adds `Bone Witch` to timeline in 3 years) or **Accept Darkness** (+1d10 effect) (Fandom `Principle: Society`). Multiple milestone choices are permanent branching.
- **Auto vs decision:** Detection/ticking = automatic; each principle story event = major decision.
- **Tracker state:** milestone checkboxes (per lantern year); principles chosen by category; pattern cards gained; strain milestones consumed; side effects of each principle event (population delta, timeline additions, survivor buffs).

### Step 7 — Develop  ← *the core decision step*
"During this step players will decide, as a group, how to spend the settlement's resources. Players can craft gear, endeavor, innovate, and build." (Fandom `Settlement Phase`). Four sub-activities, **free-form and repeatable** within the step:

- **Crafting Gear** — spend resources (archived) at built settlement locations to gain gear cards; some gear instead has a printed **gear recipe** on the card (not location-bound) and may require innovations; Pattern cards can be crafted here with their own requirements/limits. Players decide pool-vs-individual resources.
- **Endeavor** — any `[endeavor]`-costed text on an Innovation, Settlement Location, or Settlement Event: nominate a survivor, −1 settlement endeavor, apply effect (usually to that survivor only).
- **Innovate** — at the campaign's starting settlement location (Lantern Hoard / The Sun / The Throne…), spend an endeavor + required resources. Canonical Lantern Hoard cost: **1 endeavor + Bone + Organ + Hide, once per Settlement Phase, draw 2 innovation cards, keep 1, return the other** (Fandom `Lantern Hoard`). Gaining an innovation: place it, update the record sheet, adjust survival limit if applicable, **shuffle its consequence innovations into the Innovation Deck** (Fandom `Innovations`; 1.6 rulebook pg. 84 per kingdomdeath.wiki).
- **Build** — spend resources (+ endeavors) to add a new permanent Settlement Location; record it. Locations can also be gained from story events or the **first** time a monster is defeated (Fandom `Settlement Locations`).

Notes for ordering: "Resources in the settlement storage may be spent **at any time** during the Settlement Phase" (Fandom `Settlement Phase`, step 10 text) — i.e., storage reads/writes are legal across the whole phase, not only in step 7.
- **Player decisions:** Everything — what/where/for whom to craft, which innovation to keep, which location to build, which endeavor actions to take, who nominates.
- **Auto vs decision:** Resource archiving bookkeeping is automatic once chosen; deck reshuffling/consequence injection is automatic.
- **Tracker state:** endeavor ledger (spent → target → effect); resources consumed (storage delta); gear gained + assigned to survivor vs settlement storage; innovations gained (with deck consequences added); survival-limit changes; locations built; craft/innovate/build eligibility flags (prerequisites/locks); any population changes triggered by events (e.g., Intimacy 1d10 → +1/+2 population, or deaths on roll 1–3 — Fandom `Intimacy (Story Event)`).

### Step 8 — Prepare Departing Survivors
- **Mechanics:** Decide to continue to the next hunt or end the session. If continuing: choose **4 eligible departing survivors** (if population < 4, take as many as possible; with < 4 players, players control multiple survivors). Remove returning survivors' gear; load departing survivors' gear grids; record armor points and attribute modifiers from gear; apply innovation/endeavor/event bonuses to departing survivors.
- **Player decisions:** Session end vs continue; **which 4** depart; gear allocation between survivors.
- **Auto vs decision:** Decision-heavy, with derived recompute (armor points, attribute modifiers).
- **Tracker state:** hunt party (4 IDs); per-slot gear grid; armor points per hit location; attribute modifiers; departing-survivor bonuses; "session continues" flag.
- **Note:** choosing the quarry is NOT here — that is the first substantive Hunt Phase step (see §3), except nemesis years where the timeline already dictates the monster.

### Step 9 — Special Showdown
- **Mechanics:** If the timeline lists a **Special Showdown** for this year, departing survivors immediately fight it (multiple ones resolve in listed order). Afterward — win or lose — **return to Step 5 (Update the Death Count)**; heal and remove tokens, but those survivors are **not** "returning survivors"; do **not** repeat earlier steps (no new endeavors, no new settlement event, no timeline advance) (Fandom `Settlement Phase`; Fandom `Nemesis Monster` corroborates return-to-step-5).
- **Player decisions:** Fight resolution itself (play), then death-count handling.
- **Auto vs decision:** Loop control is automatic; combat is play.
- **Tracker state:** special showdown record (monster, level, outcome, deaths) + a **loop pointer back to step 5** in the UI flow.

### Step 10 — Record & Archive Resources
- **Mechanics:** Write unspent resources into **Settlement Storage** on the record sheet and archive the physical cards (so decks stay complete for future draws). Storage entries are erased when spent.
- **Player decisions:** Which resources are kept vs already spent (reconciliation).
- **Auto vs decision:** Mostly bookkeeping with reconciliation decisions.
- **Tracker state:** settlement storage (resource → quantity); archive log for resources; this is also where showdown **Rewards** (basic + monster resources drawn in the Aftermath) become durable inventory.

### Step 11 — End Settlement Phase  ← *hard commit point*
- **Mechanics:** **Discard any unspent endeavors** (they do not carry over); store settlement innovations/principles/masteries; store unused gear (a written gear list is allowed); clean up; **proceed to the Hunt Phase** (Fandom `Settlement Phase`).
- **Player decisions:** Implicit "are we done?" confirmation; any last-minute spending should happen *before* this.
- **Auto vs decision:** Automatic, but destructive (endeavors zeroed).
- **Tracker state:** endeavors → 0 (with explicit confirm); phase status = completed; persisted innovation/principle/mastery sets + gear list + storage snapshot; next phase = Hunt (or campaign end).

### Corrections to common misconceptions
1. **No dedicated population/procreation step** exists in the settlement phase. Population changes come from story events (e.g., `Intimacy`), innovations, settlement events, principles, and milestone events; new survivors are added to the record sheet when population rises, and players nominate who dies when it falls (Fandom `Population`; `Intimacy (Story Event)`).
2. **Corpse handling is not a step** — it is the one-time `Principle: Death` milestone fired from **step 5** (first death count update), plus death bookkeeping at step 5.
3. **Quarry selection & hunt declaration are NOT settlement steps** — they open the Hunt Phase (Fandom `Hunt Phase`, 1.6 rulebook pg. 61). The settlement-side equivalent is step 8 (choose the party) + the nemesis-encounter selection inside step 4.
4. **Rewards/loot are earned in the Showdown Aftermath**, then formalized into storage at **step 10** — there is no separate "arrival rewards" step beyond steps 2/3 (heal, endeavors) and 10 (storage).
5. Special Showdown (step 9) is an **interruption loop**, not the terminal step; the terminal step is 11.

---

## 3. HUNT PHASE — ordered steps (brief)

Sources: Fandom `Hunt Phase`; kingdomdeath.wiki `Hunt Phase` (1.6 rulebook pg. 61–63).

1. **Table Setup** — hunt board (reverse of settlement board), gear grids + record sheets of the hunt team. *Auto.*
2. **Choose Your Quarry** — pick from the Quarry list on the settlement record sheet. Story/nemesis-only monsters (Butcher, Slenderman, The Hand…) are not selectable. **Nemesis year → skip hunt entirely, go to showdown.** *Decision.*
3. **Choose the Monster Level** — 1/2/3 (higher = better loot, harder). *Decision.*
4. **Set Up the Hunt** — 13-space board (Start, Overwhelming Darkness, Starvation). Hunt-board diagram from the monster's showdown story event: place monster hunt-event cards on "E" spaces, fill remaining open spaces with basic hunt events, place special hunt events triggered by gear, Forest Gate over Overwhelming Darkness where required, place monster miniature by level, place departing survivors on Start. *Auto/derived.*
5. **Hunt Your Quarry** — survivors take turns as **event revealer**: draw top card of the current space → read → resolve → **archive card** → next revealer. No cards left in the space → move 1 space toward the monster and draw there. Must clear a space before moving unless an event compels movement. Empty space with no cards → **roll 1d100 on the hunt event table** (100 entries) before moving on. *Loop, mostly resolution; events contain decisions.*
6. **Overwhelming Darkness** — first entry each hunt triggers that story event (cannot be skipped; re-entry later = blank/roll table). *Auto trigger + event decisions.*
7. **Find the Quarry** — move into the monster's space → **hunt ends, showdown begins immediately** (only Starvation resolves there). *Auto.*
8. **Ambush (reverse)** — monster moves into the survivors' space → end current events, start showdown; survivors **skip their first turn** (monster usually acts twice). Some events let survivors ambush the monster (survivors may choose to go first, terrain placed as players decide). *Conditional, decision on who goes first.*
9. **In-hunt housekeeping (interleaved):** gear gained → add to grid or archive/replace (no grid rearrangement, no handing to another survivor); **event damage** is non-lethal but persists into the showdown (armor, insanity, light/heavy injuries; no severe injury/brain trauma except where an event says so); tokens persist through the showdown aftermath or next settlement start; special severe injuries/brain trauma per event; retire-on-hunt (fights the showdown, then retires forever); death-on-hunt → gear (except irreplaceable) returns to settlement storage.
10. **Starvation / The Hunt Fails / Run Away** — fighting on the Starvation space: remove 1d5 resources from storage. All survivors perish → hunt fails, apply Starvation, **lantern year over → new Settlement Phase**. If `Pictographs` innovated → nominated survivors may trigger `Run Away` after an event resolves. *Conditional decisions.*

**Tracker sketch for hunt (later phase):** quarry + level; party (4 IDs); board position; per-space event card resolved (or table roll result); revealers; accumulated damage/injuries/tokens; gear gained/lost; hunger state (Starvation reached?); outcome = showdown | ambush | fail | nemesis-skip.

---

## 4. SHOWDOWN PHASE — ordered steps (brief)

Sources: Fandom `Showdown Phase`, `Turn`, `Act`, `Attack`, `Wound`, `AI_Deck`, `Rewards`, `Victory`, `Defeat`, `Aftermath`; example `Showdown: White Lion`.

1. **Table Setup** — showdown board, monster control panel, participant gear grids + record sheets.
2. **Create the Monster** — assemble **AI deck** per monster + level (shuffle Basic/Advanced/Legendary stacks separately, draw the indicated counts blind, combine, shuffle); place **special AI / in-play cards** + attribute tokens (not in deck); Basic Action/Reference card on the panel; shuffle **basic resource deck** and **monster resource deck**.
3. **Showdown Board Setup (Terrain & Deployment)** — specific terrain cards per diagram, then random terrain from the deck (follow each card's placement rule), then place monster and survivors per diagram. *(Ambush variants alter this.)* **Showdown begins.**
4. **Round structure** — each round: **Monster Turn first, then Survivors' Turn** ("Unless otherwise stated, monsters always go first"). Monster controller tile rotates clockwise each round; controller's survivor targeted = +1 insanity bonus to that survivor.
   - **Monster turn:** Start-Turn effects → **draw top AI card and perform all actions** (target pick → move & attack → etc.) → discard → End-Turn effects. Empty deck → reshuffle discard; no cards at all → Basic Action each turn. Persistent injuries can replace card actions; moods/traits stay in play.
   - **Survivors' turn:** survivors act **one at a time, in any order**; each survivor completes an **act** (movement + activation) before the next; unused movement/activations are lost.
   - **Survivor attack:** activate weapon → roll `speed` d10s vs weapon accuracy (± survivor accuracy / monster evasion) → each hit: draw a **hit location card**, then **wound attempt** = 1d10 + survivor strength + weapon strength vs monster toughness → success = **wound** (remove top AI deck card to the Wound Stack; AI deck exhaustion is how the monster dies).
   - **Monster attack:** AI/Basic Action supplies targeting + attack profile (speed/accuracy/damage) → roll speed d10s vs accuracy ± evasion → per hit roll **hit location dice**, apply damage to that location (armor points absorb first), plus triggers.
5. **End condition** — monster dead (**Victory**) or all survivors dead (**Defeat**); only then does the showdown end.
6. **Aftermath / Victory** — per the monster's story event: e.g., White Lion L1: +1 Hunt XP, +1 Weapon Proficiency level, +Rewards (L1: 4 basic + 4 White Lion resources; L2: +6; L3: +8 + 1 strange resource); **first** White Lion defeat unlocks the **Catarium** location; Saga-triggered bonuses. Rewards are gained **as a group, once per victorious showdown** (Fandom `Rewards`). Record defeated monster (and nemesis entry) in the Defeated Monsters section (Fandom `Victory`).
7. **Aftermath / Defeat** — follow the story event's defeat text (White Lion: roll 1d10, on 6+ archive a jewelry gear of choice); deaths feed the settlement's **step 5 death count** (Fandom `Defeat`; `Death Count`).
8. **Handoff** — survivors who endured → **returning survivors** for settlement step 2; tokens last through this aftermath or until the next settlement phase begins, whichever is first.

---

## 5. Implications for a step-tracking UX

**(a) Pure bookkeeping (auto-calc / one-tap confirm):**
step 1 setup loadout · step 2 injury/token healing · step 3 endeavor count · step 4a year increment · step 5 counting deaths · step 6 milestone box-ticking (condition detection) · step 7 archiving once a craft is chosen · step 10 storage write · step 11 cleanup.

**(b) Player decisions (must be explicit, auditable inputs):**
step 2 nominations · step 4b/4c event resolutions, story-event votes, nemesis monster+level · step 5 `Principle: Death` choice · step 6 `Principle: New Life` / `Principle: Society` / `Hooded Knight` choices · step 7 everything (craft what/for whom, innovate = draw-2-keep-1, build which location, which endeavor actions, whom to nominate) · step 8 party of 4 + gear allocation · hunt: quarry, level, run-away/ambush choices · showdown: no real tracker decisions (table-side), but rewards allocation to storage.

**(c) Branching / conditional (needs explicit state machines, not a flat checklist):**
- Special Showdown → **loop back to step 5** (must be re-enterable mid-flow).
- Nemesis year → settlement runs normally, **hunt step is skipped**.
- Milestones can trigger **any time** during the phase, not only at step 6.
- Hunt failure/TPK → settlement phase starts with no returnees (step 3 yields 0 endeavors).
- First-death / first-birth / population-15 gates (one-time, campaign-scoped flags).
- Innovation deck composition changes as consequences are added (step 7 side effect).
- `Game Over` at population 0; campaign finale ends the loop.

**Revisitable/editable steps:** every step's outputs are corrections-prone (miscounted storage, wrong armor points, forgot a token, wrong gear grid) — allow **back/forward navigation with recompute**: derived values (armor points, attribute modifiers, endeavor totals, storage, survival limit) should be recomputed from primitives whenever an earlier step is edited, not hand-entered. Physical-game reality (pencil erasures on the record sheet) means players **will** fix mistakes mid-phase.

**Hard dependencies / gating:**
1. Step order is strictly sequential except the step-9 loop and mid-phase milestone interrupts.
2. **You cannot depart (step 8) before Develop (step 7)** — the rulebook order enforces it; the UI should present "Prepare Departure" only after Develop is closed (or explicitly abandoned for session end).
3. **Step 11 discards unspent endeavors** → make it a gated commit ("you are about to lose N endeavors").
4. Storage can be read/written any time in the phase, but crafting requires the resources to exist → validate against storage at the moment of craft.
5. Innovate is limited (Lantern Hoard: once per settlement phase) → enforce per-phase counters.
6. Quarry choice belongs to the Hunt Phase screen, but the app should surface the handoff at the end of step 11 (and pre-fill nemesis years).
7. Settlement-event/story-event persistent effects live "face up" for the whole phase → need a phase-scoped effects inbox, not one-shot toasts.
8. Prior art to learn from/avoid: `vibrantlantern.com/cm` (tabbed record sheets, milestone highlighting, survivor status flags: Departing/Hunting/Killed/Retired; explicitly "does not implement rules"), `kdm-manager.com`, `thewatcher.io`, `pgsgames.com/kdm` (all linked from Vibrant Lantern's overview).

---

## 6. Source list

**Fetched directly (content used above):**
- https://kingdomdeath.fandom.com/wiki/Lantern_Year
- https://kingdomdeath.fandom.com/wiki/Settlement_Phase *(primary, 11 steps)*
- https://kingdomdeath.fandom.com/wiki/Hunt_Phase
- https://kingdomdeath.fandom.com/wiki/Showdown_Phase
- https://kingdomdeath.fandom.com/wiki/Innovations
- https://kingdomdeath.fandom.com/wiki/Principle
- https://kingdomdeath.fandom.com/wiki/Principle:_Death
- https://kingdomdeath.fandom.com/wiki/Principle:_New_Life
- https://kingdomdeath.fandom.com/wiki/Principle:_Society
- https://kingdomdeath.fandom.com/wiki/Milestone
- https://kingdomdeath.fandom.com/wiki/Death_Count
- https://kingdomdeath.fandom.com/wiki/Intimacy_(Story_Event)
- https://kingdomdeath.fandom.com/wiki/Population
- https://kingdomdeath.fandom.com/wiki/Nemesis_Monster
- https://kingdomdeath.fandom.com/wiki/Quarry
- https://kingdomdeath.fandom.com/wiki/Departing_Survivors
- https://kingdomdeath.fandom.com/wiki/Settlement_Locations
- https://kingdomdeath.fandom.com/wiki/Lantern_Hoard
- https://kingdomdeath.fandom.com/wiki/Settlement_Event
- https://kingdomdeath.fandom.com/wiki/Settlement_Record_Sheet
- https://kingdomdeath.fandom.com/wiki/Timeline
- https://kingdomdeath.fandom.com/wiki/Endeavors
- https://kingdomdeath.fandom.com/wiki/Aftermath · /Rewards · /Victory · /Defeat
- https://kingdomdeath.fandom.com/wiki/Turn · /Act · /Attack · /Wound · /AI_Deck
- https://kingdomdeath.fandom.com/wiki/Showdown:_White_Lion
- https://kingdomdeath.wiki/wiki/Campaign *(1.6 rulebook pg. 36, 235)*
- https://kingdomdeath.wiki/wiki/Hunt_Phase *(1.6 rulebook pg. 61–63)*
- https://kingdomdeath.wiki/wiki/Innovations *(1.6 rulebook pg. 38, 84)*
- https://kingdomdeath.wiki/wiki/Settlement_Events *(1.6 rulebook pg. 82, 234)*
- https://www.datocms-assets.com/28070/1594237495-settlement-record-sheet-v6.pdf *(official sheet)*
- http://vibrantlantern.com/cm/ *(prior-art campaign managers list)*

**Discovered but not directly fetchable (HTTP 403 / JS wall — snippet-only, treat as secondary):**
- https://boardgamegeek.com/thread/1442781/outlines-of-phases-hunt-showdown-settlement
- https://boardgamegeek.com/thread/2333020/do-survivors-loose-bleeding-tokens-in-the-settlement
- https://boardgamegeek.com/thread/1683147/new-to-kdm-everything-you-need-to-know-before-back
- https://www.scribd.com/document/827055859/KDM-Guided-Settlement-Booklet-v1-2
