# Game flow tests (rulebook scenarios)
Executable scenarios derived from the KDM 1.5 ES rulebook (local `external-assets/*.md`, not versioned): play a flow through the real controller and assert the effects the rule states. Branch: `test/settlement-phase-flow`. Runner: `./gradlew :app-android:testDebugUnitTest` (+ full CI suite).

## Specs
- S1. One test plays the first lantern year across the eleven settlement steps, then the hunt, the showdown and the return to step 1, asserting at each step what the settlement-phase table states (rulebook "Fase de Asentamiento", pp. 34-35 and 81-85).
- S2. Rules the code does not follow are not fixed silently: each becomes a test asserting the rule, marked `@Ignore` with the reason, and an entry in `docs/backlog.md` quoting the rule's source. The maintainer is told before any code is changed.
- S3. Hunt-phase rules (rulebook "Fase de Caza", pp. 61-63) get the same treatment after the settlement phase.
- S4. Tests cite the rulebook by section and page, never copy its text.

## Tasks
| ID | Specs | Route | Status | Commit |
|----|-------|-------|--------|--------|
| T1 | S1, S4 | inline | done (SettlementYearRulesFlowTest passes, 1 test executed) | this PR |
| T2 | S2 | inline | done (RED observed: both rules fail on the current code; then `@Ignore` + backlog) | this PR |
| T4 | S2 | inline | done (RED observed for the three guard gaps; then `@Ignore` + backlog; one passing pin) | this PR |
| T3 | S3 | inline | done (HuntPhaseRulesFlowTest: 4 tests pass on the current code, 1 RED then `@Ignore` + backlog) | this PR |

## Log
- L1 (2026-10-30): "creo que podemos dejar el B4 en el backlog y podriamos mirar de hacer tests de flows del juego, ya que tenemos las instrucciones tanto en md como en pdf." Then "dale" to starting with the settlement phase (the 11 steps).
- L2: audit — per-rule unit tests exist for most steps; what is missing is the whole-year scenario and the hunt rules. Starvation (lose d5 storage resources) is not implemented: only the board space exists.
- L3: two divergences found and left unfixed (S2): step 3 funds the whole population after a party wipe; starvation takes no resources.
- L4 (2026-10-31): "dale alguna vuelta más a algun edge case que pudieramos tener sin controlar en la fase del asentamiento" — probed the controller; three guard gaps found (advance without phase guard, selectStep without phase guard, commit with no departing survivor), one pinned (back-and-forth never funds or advances twice).
- L5 (2026-10-31): "si arreglalos ahora" (advance and selectStep phase guards, merged) and "si, por lo menos deberia de haber un minimo de supervivientes que van a la caza." (the last settlement step is refused with no departing survivor).
- L6 (2026-10-31): "dale" to the hunt-phase flow tests. Board, one-space advance, unresolved-card gate, contact vs ambush, damage kept, run away: already followed. Divergences: retreat moves the beast closer (test ignored); the two ambush kinds and their effect are not modelled (backlog only, no observable yet).
- L7 (2026-10-31): "si, corrigelo ahora" (retreat direction): `RetreatMonster` now moves the beast away from the party and clamps it to the board; `RetreatMonsterTest` updated; the ignored flow test is enabled.
- L8 (2026-10-31): "dale" to fixing the step-3 endeavors after a wiped party: a failed hunt now records the departing survivors as dead when it ends (like the showdown does), so step 3 funds only the living; `RulebookDivergencesTest` is gone (no divergence left to hold).
