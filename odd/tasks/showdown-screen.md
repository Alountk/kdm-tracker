# Showdown screen 1:1 (V3)
Rebuild `ShowdownScreen` against Stitch `cd089a6938264afea5bd710ff04e3716` (HTML, never screenshots).
Branch: `feat/showdown-screen`. Delivery: sliced PRs of ~400 lines, visual gate before commit. Runner: `./gradlew spotlessCheck :core:jvmTest :app-android:testDebugUnitTest`.

## Specs
- S1. The active showdown shows the design's header: round/turn chip, monster card (name, level badge), stat strip, and the `Vida / Mazo IA` track with `Heridas` and the `−` / `+` buttons.
- S2. `+` wounds the monster (existing `WoundMonster`); `−` returns the last wound to the AI deck (new, minimal `HealMonster`). The deck count and wound count come from `ShowdownState`.
- S3. `Carta de IA en Curso` shows the last drawn AI card (name, rank badge, text) with `Robar Siguiente IA`; `Resolver Ataque` is not drawn (no round state in the domain).
- S4. `Ataque de Supervivientes & Localizaciones` lists the monster's hit locations; each location resolves out of the list (`Fallo`, `Herida`, `Crítico`); `Herida` and `Crítico` also wound the monster; reactions are marked from the card text.
- S5. The outcome (`Victoria` / `Derrota`), the hunt XP grant and the deaths counter stay available below the design's sections.
- S6. Controls the domain cannot back (controller, blind spot, stats, terrain, round state) are drawn as in the design and recorded in `docs/backlog.md`.
- S7. Spanish UI copy, Atramentum theme, no process labels in comments.

## Tasks
| ID | Specs | Route | Status | Commit |
|----|-------|-------|--------|--------|
| T1 | S2 | inline | done (RED observed: `HealMonster` unresolved; GREEN: ShowdownDeckUseCaseTest, ShowdownControllerTest) | a04c5b9 (PR #61) |
| T2 | S1, S3, S4, S5 | inline | done (ShowdownBoardModelTest; emulator: draw, wound, heal, hit location verified) | second slice (board PR) |
| T3 | S6, S7 | inline | done (backlog section added; full suite green) | second slice (board PR) |

## Log
- L1 (2026-10-30): "hagamos limpieza de worktree settlement-steps y etc, y luego sigamos con lo siguiente de roadmap" — cleanup done; next roadmap item is V3 Enfrentamiento 1:1.
- L2: assumption — scope is UI plus the data the domain already holds; round/turn, controller, hit-location deck state and monster stats are not modelled and stay inert (S6).
- L3: the model test was written together with the model, so no separate RED run for it. Deaths counter now resets after `Registrar` (it kept its value before). Visual gate pending: maintainer approval of the emulator captures before commit.
