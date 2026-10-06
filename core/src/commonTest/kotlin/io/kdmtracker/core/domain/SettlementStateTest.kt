package io.kdmtracker.core.domain

import io.kdmtracker.core.rules.MilestoneState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettlementStateTest {
    private fun state(
        population: Int = 4,
        deaths: Int = 0,
        births: Int = 0,
        innovations: List<Innovation> = emptyList(),
    ): SettlementState =
        SettlementState(
            survivalLimit = 10,
            population = population,
            deaths = deaths,
            births = births,
            innovations = InnovationCollection(innovations),
        )

    @Test
    fun milestoneStateAdaptsTheCountersAndTheInnovationCount() {
        val state =
            state(
                population = 7,
                deaths = 2,
                births = 3,
                innovations =
                    listOf(
                        Innovation(name = "Language", description = ""),
                        Innovation(name = "Paint", description = ""),
                    ),
            )

        assertEquals(
            MilestoneState(deaths = 2, births = 3, population = 7, innovations = 2),
            state.milestoneState(),
        )
    }

    @Test
    fun aFreshSettlementReachesNoMilestones() {
        assertTrue(SettlementState(survivalLimit = 10).reachedMilestones().isEmpty())
    }

    @Test
    fun aDefaultSettlementStateStartsWithFourSurvivorsAndReachesNoMilestones() {
        val state = SettlementState(survivalLimit = 10)

        assertEquals(4, state.population)
        assertTrue(state.reachedMilestones().isEmpty())
    }

    @Test
    fun fifteenPopulationReachesSociety() {
        assertTrue(MilestoneKey.PRINCIPLE_SOCIETY in state(population = 15).reachedMilestones())
    }

    @Test
    fun zeroPopulationReachesGameOver() {
        assertTrue(MilestoneKey.GAME_OVER in state(population = 0).reachedMilestones())
    }

    @Test
    fun oneDeathReachesTheDeathMilestone() {
        assertTrue(MilestoneKey.PRINCIPLE_DEATH in state(deaths = 1).reachedMilestones())
    }

    @Test
    fun fiveInnovationsReachTheHoodedKnight() {
        val innovations = List(5) { Innovation(name = "Innovation $it", description = "") }

        assertTrue(MilestoneKey.HOODED_KNIGHT in state(innovations = innovations).reachedMilestones())
    }

    @Test
    fun reachedMilestonesReturnsExactlyTheReachedSet() {
        val reached =
            state(
                population = 15,
                deaths = 1,
                births = 1,
                innovations = List(5) { Innovation(name = "Innovation $it", description = "") },
            ).reachedMilestones()

        assertEquals(
            setOf(
                MilestoneKey.PRINCIPLE_DEATH,
                MilestoneKey.PRINCIPLE_NEW_LIFE,
                MilestoneKey.PRINCIPLE_SOCIETY,
                MilestoneKey.HOODED_KNIGHT,
            ),
            reached,
        )
    }
}
