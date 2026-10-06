package io.kdmtracker.core.rules

import io.kdmtracker.core.domain.MilestoneKey
import io.kdmtracker.core.ruleset.Core16Ruleset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MilestoneConditionsTest {
    private fun state(
        deaths: Int = 0,
        births: Int = 0,
        population: Int = 0,
        innovations: Int = 0,
    ): MilestoneState =
        MilestoneState(deaths = deaths, births = births, population = population, innovations = innovations)

    @Test
    fun deathReachedAtFirstDeath() {
        assertFalse(MilestoneConditions.death.reached(state(deaths = 0)))
        assertTrue(MilestoneConditions.death.reached(state(deaths = 1)))
    }

    @Test
    fun newLifeReachedAtFirstBirth() {
        assertFalse(MilestoneConditions.newLife.reached(state(births = 0)))
        assertTrue(MilestoneConditions.newLife.reached(state(births = 1)))
    }

    @Test
    fun societyReachedAtFifteenPopulation() {
        assertFalse(MilestoneConditions.society.reached(state(population = 14)))
        assertTrue(MilestoneConditions.society.reached(state(population = 15)))
    }

    @Test
    fun hoodedKnightReachedAtFiveInnovations() {
        assertFalse(MilestoneConditions.hoodedKnight.reached(state(innovations = 4)))
        assertTrue(MilestoneConditions.hoodedKnight.reached(state(innovations = 5)))
    }

    @Test
    fun gameOverReachedAtZeroPopulationAndBelow() {
        assertFalse(MilestoneConditions.gameOver.reached(state(population = 1)))
        assertTrue(MilestoneConditions.gameOver.reached(state(population = 0)))
        assertTrue(MilestoneConditions.gameOver.reached(state(population = -1)))
    }

    @Test
    fun forKeyReturnsTheMatchingCondition() {
        assertTrue(MilestoneConditions.forKey(MilestoneKey.PRINCIPLE_DEATH).reached(state(deaths = 1)))
        assertTrue(MilestoneConditions.forKey(MilestoneKey.PRINCIPLE_NEW_LIFE).reached(state(births = 1)))
        assertTrue(MilestoneConditions.forKey(MilestoneKey.PRINCIPLE_SOCIETY).reached(state(population = 15)))
        assertTrue(MilestoneConditions.forKey(MilestoneKey.HOODED_KNIGHT).reached(state(innovations = 5)))
        assertTrue(MilestoneConditions.forKey(MilestoneKey.GAME_OVER).reached(state(population = 0)))
    }

    @Test
    fun reachedMilestonesReturnsExactlyTheReachedSet() {
        val reached = reachedMilestones(state(deaths = 1, population = 15), MilestoneKey.entries)
        assertEquals(setOf(MilestoneKey.PRINCIPLE_DEATH, MilestoneKey.PRINCIPLE_SOCIETY), reached)
    }

    @Test
    fun reachedMilestonesHonoursTheSuppliedKeySubset() {
        val reached = reachedMilestones(state(deaths = 1, population = 15), listOf(MilestoneKey.PRINCIPLE_DEATH))
        assertEquals(setOf(MilestoneKey.PRINCIPLE_DEATH), reached)
    }

    @Test
    fun reachedMilestonesIsEmptyWhenNothingIsReached() {
        assertTrue(reachedMilestones(state(population = 1), MilestoneKey.entries).isEmpty())
    }

    @Test
    fun thresholdsMatchCore16RulesetDeclaredMilestoneConditions() {
        val declared = Core16Ruleset.milestones.associate { it.key to it.condition }

        assertEquals(MilestoneConditions.DEATHS, declared.numberFor(MilestoneKey.PRINCIPLE_DEATH))
        assertEquals(MilestoneConditions.SOCIETY_POPULATION, declared.numberFor(MilestoneKey.PRINCIPLE_SOCIETY))
        assertEquals(MilestoneConditions.HOODED_KNIGHT_INNOVATIONS, declared.numberFor(MilestoneKey.HOODED_KNIGHT))
        assertEquals(MilestoneConditions.GAME_OVER_POPULATION, declared.numberFor(MilestoneKey.GAME_OVER))

        assertEquals(1, MilestoneConditions.BIRTHS)
        assertTrue(
            declared.getValue(MilestoneKey.PRINCIPLE_NEW_LIFE).startsWith("first"),
            "1.6 declares the first birth as the New Life milestone",
        )
    }

    private fun Map<MilestoneKey, String>.numberFor(key: MilestoneKey): Int {
        val declared = getValue(key)
        return requireNotNull(Regex("\\d+").find(declared)?.value?.toInt()) {
            "Core16Ruleset declares no numeric threshold for $key: '$declared'"
        }
    }
}
