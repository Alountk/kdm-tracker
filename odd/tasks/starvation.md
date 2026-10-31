# Starvation: the hunt costs d5 stored resources
Objective: a failed hunt (or one that ends on the starvation space) takes d5 resources from the storage, and the player picks which. Branch: `feat/starvation`. Delivery: single PR. Runner: full CI command (`spotlessCheck :core:jvmTest :data:jvmTest :server:test :app-android:testDebugUnitTest :app-android:assembleDebug`).

## Specs
- S1. Rulebook "Fase de Caza", starvation (pp. 61-63): a failed hunt (the whole party dies) triggers starvation, which costs d5 resources from the settlement storage. A d5 is a roll of 1 to 5; when the storage holds fewer resources than the roll, only what exists is owed.
- S2. The same loss applies when the hunt reaches or resolves on the starvation space (board space 12): contact there (quarry found or ambush) triggers it too.
- S3. User decision: "si, es lo que se hace en una partida." — the app rolls the d5 and the player chooses which stored resources to discard until the owed amount is paid; the app never discards on its own.
- S4. The owed amount survives closing the app (stored with the settlement state) and is visible on any screen of the shell until it is paid, with a way to discard one unit of a stored resource at a time.
- S5. `RulebookDivergencesTest.aFailedHuntLosesBetweenOneAndFiveStoredResourcesToStarvation` is enabled and passes; the backlog entry is closed.

## Tasks
| ID | Specs | Route | Status | Commit |
|----|-------|-------|--------|--------|
| T1 | S1, S2, S3, S4 | inline | done (StarvationTest, 10 tests; written with the code, no observed RED) | this PR |
| T2 | S1, S2 | inline | done (StarvationFlowTest; RED observed by disabling each trigger) | this PR |
| T3 | S4 | inline | done (StarvationBanner + choices model test; not seen on the emulator) | this PR |
| T4 | S5 | inline | done (the divergence test moved to StarvationFlowTest as passing; backlog closed) | this PR |

## Log
- L1 (2026-10-31): "dale" to starvation, then, asked whether the player chooses which resources are lost or the app discards at random: "si, es lo que se hace en una partida."
- L2: Route: inline (one understood behavior across core, controller and one UI banner; no parallel units). Risk: item 1 only in the weak sense of a new state field with a default (old saves decode unchanged).
