package io.kdmtracker.app

import io.kdmtracker.core.domain.HitLocation
import io.kdmtracker.core.domain.HuntSpaceKind
import io.kdmtracker.core.domain.HuntStatus
import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.domain.WoundState
import io.kdmtracker.core.persistence.InMemoryCampaignStore
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Plays the hunt phase through the real controller and checks what the rulebook states (KDM 1.5 ES,
 * "Fase de Caza", pp. 61-63): the board layout, the party advancing one space at a time toward the
 * beast, the two ways a hunt ends in a showdown, damage carried over, and the run away exit.
 */
class HuntPhaseRulesFlowTest {
    private val repository = SettlementRepository(InMemoryCampaignStore())
    private val controller = LanternYearController(repository)

    private val state get() = controller.state.value
    private val hunt get() = repository.record.state.hunt

    private fun huntAWhiteLion() {
        while (state.currentStep < 11) controller.flow.advance()
        controller.chooseTheDepartingParty()
        controller.flow.advance()
        assertEquals(SettlementPhase.HUNT, state.phase)
        controller.hunt.setUpHunt("White Lion", 1, 4)
    }

    @Test
    fun theBoardHasThirteenSpacesWithTheFixedOnesInPlace() {
        huntAWhiteLion()

        assertEquals(13, hunt.spaces.size)
        assertEquals(HuntSpaceKind.START, hunt.spaces[0].kind)
        assertEquals(HuntSpaceKind.OVERWHELMING_DARKNESS, hunt.spaces[6].kind)
        assertEquals(HuntSpaceKind.STARVATION, hunt.spaces[12].kind)
        assertEquals(
            listOf(1, 2, 5, 8, 9),
            hunt.spaces.indices.filter { hunt.spaces[it].kind == HuntSpaceKind.MONSTER_E },
            "monster events sit on the quarry's own spaces",
        )
        assertEquals(0, hunt.position)
        assertEquals(4, hunt.monsterPosition, "a level 1 white lion waits on its starting space")
    }

    @Test
    fun theSurvivorsWalkOneSpaceAtATimeAndFindTheBeastWithoutAnAmbush() {
        huntAWhiteLion()
        val hurt = state.survivors.first()
        var steps = 0

        while (state.phase == SettlementPhase.HUNT && steps++ < 20) {
            val before = hunt.position
            controller.hunt.advanceHunt()
            if (state.phase != SettlementPhase.HUNT) break
            assertEquals(before + 1, hunt.position, "the party moves exactly one space")
            assertNotNull(hunt.drawnEvent, "each space reveals a card")

            // The revealed card must be resolved before the party moves on.
            controller.hunt.advanceHunt()
            assertEquals(before + 1, hunt.position, "an unresolved card holds the party in place")
            if (hunt.position == 2) {
                controller.survivors.updateSurvivor(
                    hurt.copy(wounds = mapOf(HitLocation.ARMS to WoundState(light = true, heavy = false))),
                )
            }
            controller.hunt.resolveDrawnEvent()
        }

        assertEquals(HuntStatus.QUARRY_FOUND, hunt.status, "reaching the beast is not an ambush")
        assertEquals(hunt.monsterPosition, hunt.position, "the party stops on the beast's space")
        assertEquals(SettlementPhase.SHOWDOWN, state.phase)
        val carried = state.survivors.single { it.id == hurt.id }.wounds[HitLocation.ARMS]
        assertEquals(true, carried?.light, "event damage is kept for the showdown")
    }

    @Test
    fun theBeastReachingTheSurvivorsAmbushesThem() {
        huntAWhiteLion()

        var steps = 0
        while (state.phase == SettlementPhase.HUNT && steps++ < 20) controller.hunt.moveMonster()

        assertEquals(HuntStatus.AMBUSH, hunt.status)
        assertEquals(SettlementPhase.SHOWDOWN, state.phase)
        assertEquals(hunt.position, hunt.monsterPosition, "the beast stops on the survivors' space")
    }

    @Test
    fun runningAwayBringsEverySurvivorBackToAFreshSettlementPhase() {
        huntAWhiteLion()
        val population = state.population

        controller.flow.finishHunt(HuntStatus.RUN_AWAY)

        assertEquals(SettlementPhase.SETTLEMENT, state.phase)
        assertEquals(1, state.currentStep)
        assertEquals(population, state.population, "nobody is lost by running away")
        while (state.currentStep < 3) controller.flow.advance()
        assertEquals(population, state.endeavors, "every survivor returned, so every one earns an endeavor")
    }

    @Test
    fun aRetreatingBeastMovesAwayFromTheSurvivors() {
        // Hunt events such as Asustadizo: the beast "se mueve 1 espacio alejándose de los supervivientes".
        huntAWhiteLion()
        val before = abs(hunt.monsterPosition - hunt.position)

        controller.hunt.retreatMonster()

        assertTrue(abs(hunt.monsterPosition - hunt.position) > before, "the gap to the survivors grows")
    }
}
