package io.kdmtracker.core.rules

import io.kdmtracker.core.domain.Core16Milestones
import io.kdmtracker.core.ruleset.Core16Ruleset
import io.kdmtracker.core.ruleset.MilestoneDefinition
import io.kdmtracker.core.ruleset.RulesetOverrides
import io.kdmtracker.core.ruleset.applyOverrides
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
        assertFalse(
            MilestoneConditions.evaluate(state(deaths = 0), MilestoneConditionSpec.AtLeast(MilestoneMetric.DEATHS, 1)),
        )
        assertTrue(
            MilestoneConditions.evaluate(state(deaths = 1), MilestoneConditionSpec.AtLeast(MilestoneMetric.DEATHS, 1)),
        )
    }

    @Test
    fun newLifeReachedAtFirstBirth() {
        assertFalse(
            MilestoneConditions.evaluate(state(births = 0), MilestoneConditionSpec.AtLeast(MilestoneMetric.BIRTHS, 1)),
        )
        assertTrue(
            MilestoneConditions.evaluate(state(births = 1), MilestoneConditionSpec.AtLeast(MilestoneMetric.BIRTHS, 1)),
        )
    }

    @Test
    fun societyReachedAtFifteenPopulation() {
        assertFalse(
            MilestoneConditions.evaluate(
                state(population = 14),
                MilestoneConditionSpec.AtLeast(MilestoneMetric.POPULATION, 15),
            ),
        )
        assertTrue(
            MilestoneConditions.evaluate(
                state(population = 15),
                MilestoneConditionSpec.AtLeast(MilestoneMetric.POPULATION, 15),
            ),
        )
    }

    @Test
    fun hoodedKnightReachedAtFiveInnovations() {
        assertFalse(
            MilestoneConditions.evaluate(
                state(innovations = 4),
                MilestoneConditionSpec.AtLeast(MilestoneMetric.INNOVATIONS, 5),
            ),
        )
        assertTrue(
            MilestoneConditions.evaluate(
                state(innovations = 5),
                MilestoneConditionSpec.AtLeast(MilestoneMetric.INNOVATIONS, 5),
            ),
        )
    }

    @Test
    fun gameOverReachedAtZeroPopulationAndBelow() {
        val spec = MilestoneConditionSpec.AtMost(MilestoneMetric.POPULATION, 0)
        assertFalse(MilestoneConditions.evaluate(state(population = 1), spec))
        assertTrue(MilestoneConditions.evaluate(state(population = 0), spec))
        assertTrue(MilestoneConditions.evaluate(state(population = -1), spec))
    }

    @Test
    fun reachedMilestonesReadsTheRulesetDefinitions() {
        val reached = reachedMilestones(state(deaths = 1, population = 15), Core16Ruleset.milestones)

        assertEquals(
            setOf(Core16Milestones.PRINCIPLE_DEATH, Core16Milestones.PRINCIPLE_SOCIETY),
            reached,
        )
    }

    @Test
    fun reachedMilestonesHonoursTheSuppliedDefinitionSubset() {
        val subset = Core16Ruleset.milestones.filter { it.id == Core16Milestones.PRINCIPLE_DEATH }
        val reached = reachedMilestones(state(deaths = 1, population = 15), subset)

        assertEquals(setOf(Core16Milestones.PRINCIPLE_DEATH), reached)
    }

    @Test
    fun reachedMilestonesIsEmptyWhenNothingIsReached() {
        assertTrue(reachedMilestones(state(population = 1), Core16Ruleset.milestones).isEmpty())
    }

    @Test
    fun anOverrideChangesTheSocietyThreshold() {
        val overridden =
            Core16Ruleset.applyOverrides(
                RulesetOverrides(
                    extraMilestones =
                        listOf(
                            MilestoneDefinition(
                                Core16Milestones.PRINCIPLE_SOCIETY,
                                MilestoneConditionSpec.AtLeast(MilestoneMetric.POPULATION, 20),
                                "population reaches 20",
                            ),
                        ),
                ),
            )

        assertFalse(
            Core16Milestones.PRINCIPLE_SOCIETY in reachedMilestones(state(population = 15), overridden.milestones),
        )
        assertTrue(
            Core16Milestones.PRINCIPLE_SOCIETY in reachedMilestones(state(population = 20), overridden.milestones),
        )
    }
}
