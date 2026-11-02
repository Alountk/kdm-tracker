# Node replacement: an expansion monster takes the place of the monster on its node
Objective: packs declare which monster node each monster sits on; a campaign stores which monster holds each contested node; enabling an expansion asks for that choice and swaps the timeline. Branch: `feat/node-replacement`. Delivery: one PR per task (T1, T2, T3 are each reviewable alone). Runner: full CI command (`spotlessCheck :core:jvmTest :data:jvmTest :server:test :app-android:testDebugUnitTest :app-android:assembleDebug`).

## Specs
- S1. Official rule (Kingdom Death "Monster Nodes"): "As a rule of thumb, when you add an expansion, remove any monster of the same node from the settlement's timeline and quarry list." Same-node monsters replace each other; it is not a conflict.
- S2. User decision: "si" to "la elección por nodo se guarde en la campaña y se haga en la hoja de confirmación", with: each monster of a pack declares its node; the campaign stores which monster occupies each node (a new synced field, like `enabledPacks`); the choice is made in the confirmation sheet shown when enabling an expansion, together with the combination warnings; the default is the new expansion's monster; replacing removes the outgoing monster's entries from the timeline.
- S3. Nodes are the official ones: `NQ1`-`NQ4` quarries, `NN1`-`NN3` nemeses, `Co` core foe, `Fi` finale. Quarry nodes feed the quarry list; the others feed the nemesis list.
- S4. A campaign saved before this change decodes unchanged (no stored choice) and keeps listing every monster it listed.
- S5. Deferred, see T4. Assumption (not user-confirmed): an event of a newly enabled pack whose year is not after the current Lantern Year goes to the next Lantern Year (Gorm 1.2 and the official page say so for the story events they name).
- S6. Not modelled (no pack ships such data, nothing to check against): People of the Stars keeping NN1, and Slenderman / Manhunter / Lion Knight sitting outside one-per-node.

## Tasks
| ID | Specs | Route | Status | Commit |
|----|-------|-------|--------|--------|
| T1 | S1, S3 | inline | done (NodeOccupancyTest, 8 tests; RED observed only as a compile failure) | ecd3e04 (PR #79) |
| T2 | S2, S4 | inline | done (CampaignNodeChoicesTransferTest 4 + CampaignNodeChoicesStoreTest 4; RED observed only as a compile failure; migration 5.sqm moves the schema to version 6 and three older migration tests pinned the version; reviewed risk + reliability, approved, two non-blocking warnings) | 391d54c (PR #80) |
| T3 | S2 | inline | done (NodeContestTest 5, 2 timeline cases in NodeOccupancyTest, 3 in SettlementRepositoryPacksTest; RED observed only as a compile failure). The timeline swap happens in the catalog: the year events shown come from the enabled packs, the settlement's own timeline starts empty | this PR |
| T4 | S5 | not done | the shown year events are derived from the enabled packs, so a story event of a pack added after its year has nothing to attach to; needs a stored list of events moved to the next year and the screen to show it | |

## Log
- L1 (2026-11-02): "dale" to "¿Sigo con el modelo del reemplazo por nodo y del paso de activación mientras revisas las tablas?", then "si" to "¿Te parece bien que la elección por nodo se guarde en la campaña y se haga en la hoja de confirmación?"
- L2: Route: inline (one design, sequential tasks sharing the same types). Risk: T2 item 1 (new persisted column + migration + synced document field; default keeps old data valid).
- L3: Finding while doing T3: `UiStateMapper` builds the year events from `content.timeline`; the settlement's `state.timeline` stays empty unless an event is recorded (so `nemesisOf` finds nothing today, pre-existing). The swap therefore lives in `ContentCatalog.timeline`, and S5 needs new stored state: left as T4.
- L4: T2 review (risk + reliability) approved with two WARNINGs on `NodeChoices.decode`: a node name this build does not know is dropped, so an older device that rewrites the campaign loses that choice. Not fixed here; no node beyond the nine exists yet.
