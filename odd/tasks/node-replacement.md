# Node replacement: an expansion monster takes the place of the monster on its node
Objective: packs declare which monster node each monster sits on; a campaign stores which monster holds each contested node; enabling an expansion asks for that choice and swaps the timeline. Branch: `feat/node-replacement`. Delivery: one PR per task (T1, T2, T3 are each reviewable alone). Runner: full CI command (`spotlessCheck :core:jvmTest :data:jvmTest :server:test :app-android:testDebugUnitTest :app-android:assembleDebug`).

## Specs
- S1. Official rule (Kingdom Death "Monster Nodes"): "As a rule of thumb, when you add an expansion, remove any monster of the same node from the settlement's timeline and quarry list." Same-node monsters replace each other; it is not a conflict.
- S2. User decision: "si" to "la elección por nodo se guarde en la campaña y se haga en la hoja de confirmación", with: each monster of a pack declares its node; the campaign stores which monster occupies each node (a new synced field, like `enabledPacks`); the choice is made in the confirmation sheet shown when enabling an expansion, together with the combination warnings; the default is the new expansion's monster; replacing removes the outgoing monster's entries from the timeline.
- S3. Nodes are the official ones: `NQ1`-`NQ4` quarries, `NN1`-`NN3` nemeses, `Co` core foe, `Fi` finale. Quarry nodes feed the quarry list; the others feed the nemesis list.
- S4. A campaign saved before this change decodes unchanged (no stored choice) and keeps listing every monster it listed.
- S5. Assumption (not user-confirmed): an event of a newly enabled pack whose year is not after the current Lantern Year goes to the next Lantern Year (Gorm 1.2 and the official page say so for the story events they name).
- S6. Not modelled (no pack ships such data, nothing to check against): People of the Stars keeping NN1, and Slenderman / Manhunter / Lion Knight sitting outside one-per-node.

## Tasks
| ID | Specs | Route | Status | Commit |
|----|-------|-------|--------|--------|
| T1 | S1, S3 | inline | done (NodeOccupancyTest, 8 tests; RED observed only as a compile failure) | ecd3e04 (PR #79) |
| T2 | S2, S4 | inline | done (CampaignNodeChoicesTransferTest 4 + CampaignNodeChoicesStoreTest 4; RED observed only as a compile failure; migration 5.sqm moves the schema to version 6 and three older migration tests pinned the version) | this PR |
| T3 | S2, S5 | inline | pending: enabling packs takes the choices, stores them, swaps the timeline; contested nodes exposed for the sheet | |

## Log
- L1 (2026-11-02): "dale" to "¿Sigo con el modelo del reemplazo por nodo y del paso de activación mientras revisas las tablas?", then "si" to "¿Te parece bien que la elección por nodo se guarde en la campaña y se haga en la hoja de confirmación?"
- L2: Route: inline (one design, sequential tasks sharing the same types). Risk: T2 item 1 (new persisted column + migration + synced document field; default keeps old data valid).
