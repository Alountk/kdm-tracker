package io.kdmtracker.core.ruleset

import io.kdmtracker.core.domain.Core16Milestones
import io.kdmtracker.core.domain.MilestoneId
import io.kdmtracker.core.domain.PrincipleCategory
import io.kdmtracker.core.rules.MilestoneConditionSpec
import io.kdmtracker.core.rules.MilestoneMetric
import io.kdmtracker.core.rules.SettlementStepCatalog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RulesetTest {
    @Test
    fun core16BaselineValues() {
        assertEquals("core-1.6", Core16Ruleset.id)
        assertEquals("1.6", Core16Ruleset.version)
        assertEquals(1, Core16Ruleset.survivalLimit)
        assertEquals(SettlementStepCatalog.ALL, Core16Ruleset.settlementSteps)
    }

    @Test
    fun stepCatalogParityWithCore16Ruleset() {
        assertEquals(SettlementStepCatalog.ALL, Core16Ruleset.settlementSteps)
        assertEquals(SettlementStepCatalog.TOTAL, Core16Ruleset.settlementSteps.size)
    }

    @Test
    fun core16UsesOfficialStepAnchorsAndEndeavorRate() {
        assertEquals(9, Core16Ruleset.specialShowdownStep)
        assertEquals(5, Core16Ruleset.deathCountStep)
        assertEquals(1, Core16Ruleset.endeavorsPerReturnee)
    }

    @Test
    fun ruleAnchorsAndEndeavorRateAreOverridableWithoutMutatingBase() {
        val overridden =
            Core16Ruleset.applyOverrides(
                RulesetOverrides(
                    specialShowdownStep = 10,
                    deathCountStep = 6,
                    endeavorsPerReturnee = 2,
                ),
            )
        assertEquals(10, overridden.specialShowdownStep)
        assertEquals(6, overridden.deathCountStep)
        assertEquals(2, overridden.endeavorsPerReturnee)
        assertEquals(9, Core16Ruleset.specialShowdownStep)
        assertEquals(5, Core16Ruleset.deathCountStep)
        assertEquals(1, Core16Ruleset.endeavorsPerReturnee)
    }

    @Test
    fun partialRuleOverrideKeepsOtherAnchorsFromBase() {
        val overridden = Core16Ruleset.applyOverrides(RulesetOverrides(endeavorsPerReturnee = 3))
        assertEquals(3, overridden.endeavorsPerReturnee)
        assertEquals(9, overridden.specialShowdownStep)
        assertEquals(5, overridden.deathCountStep)
    }

    @Test
    fun core16HasFiveOfficialMilestonesWithTypedConditions() {
        assertEquals(5, Core16Ruleset.milestones.size)
        assertEquals(
            Core16Milestones.ALL.toSet(),
            Core16Ruleset.milestones.map { it.id }.toSet(),
        )

        val byId = Core16Ruleset.milestones.associateBy { it.id }
        val death = byId.getValue(Core16Milestones.PRINCIPLE_DEATH)
        assertEquals(MilestoneConditionSpec.AtLeast(MilestoneMetric.DEATHS, 1), death.condition)
        assertEquals("1 death", death.description)

        val newLife = byId.getValue(Core16Milestones.PRINCIPLE_NEW_LIFE)
        assertEquals(MilestoneConditionSpec.AtLeast(MilestoneMetric.BIRTHS, 1), newLife.condition)
        assertEquals("first child is born", newLife.description)

        val society = byId.getValue(Core16Milestones.PRINCIPLE_SOCIETY)
        assertEquals(MilestoneConditionSpec.AtLeast(MilestoneMetric.POPULATION, 15), society.condition)
        assertEquals("population reaches 15", society.description)

        val hoodedKnight = byId.getValue(Core16Milestones.HOODED_KNIGHT)
        assertEquals(MilestoneConditionSpec.AtLeast(MilestoneMetric.INNOVATIONS, 5), hoodedKnight.condition)
        assertEquals("settlement has 5 innovations", hoodedKnight.description)

        val gameOver = byId.getValue(Core16Milestones.GAME_OVER)
        assertEquals(MilestoneConditionSpec.AtMost(MilestoneMetric.POPULATION, 0), gameOver.condition)
        assertEquals("population reaches 0", gameOver.description)
    }

    @Test
    fun core16HasTwoChoicesPerPrincipleCategory() {
        assertEquals(
            setOf(PrincipleCategory.DEATH, PrincipleCategory.NEW_LIFE, PrincipleCategory.SOCIETY),
            Core16Ruleset.principles.keys,
        )
        Core16Ruleset.principles.forEach { (category, choices) ->
            assertEquals(2, choices.size, "expected two choices for $category")
            assertTrue(choices.all { it.category == category })
        }
    }

    @Test
    fun survivalLimitOverrideDoesNotMutateBase() {
        val overridden = Core16Ruleset.applyOverrides(RulesetOverrides(survivalLimit = 12))
        assertEquals(12, overridden.survivalLimit)
        assertEquals(1, Core16Ruleset.survivalLimit)
    }

    @Test
    fun emptyOverridesReturnSameInstance() {
        assertSame(Core16Ruleset, Core16Ruleset.applyOverrides(RulesetOverrides()))
    }

    @Test
    fun principleOverrideReplacesOnlyThatCategoryAndKeepsBase() {
        val replacement =
            listOf(
                PrincipleChoice(PrincipleCategory.DEATH, "House Rule", "a custom death rule"),
            )
        val overridden =
            Core16Ruleset.applyOverrides(
                RulesetOverrides(principleOverrides = mapOf(PrincipleCategory.DEATH to replacement)),
            )
        assertEquals(replacement, overridden.principles[PrincipleCategory.DEATH])
        assertEquals(
            Core16Ruleset.principles[PrincipleCategory.NEW_LIFE],
            overridden.principles[PrincipleCategory.NEW_LIFE],
        )
        assertEquals(
            Core16Ruleset.principles[PrincipleCategory.SOCIETY],
            overridden.principles[PrincipleCategory.SOCIETY],
        )
        assertEquals(2, Core16Ruleset.principles.getValue(PrincipleCategory.DEATH).size)
    }

    @Test
    fun extraMilestoneWithANewIdIsAppendedWithoutDroppingBase() {
        val extra =
            MilestoneDefinition(
                MilestoneId("house-rule-game-over"),
                MilestoneConditionSpec.AtMost(MilestoneMetric.POPULATION, 0),
                "house rule game over",
            )
        val overridden =
            Core16Ruleset.applyOverrides(
                RulesetOverrides(extraMilestones = listOf(extra)),
            )
        assertEquals(6, overridden.milestones.size)
        assertTrue(overridden.milestones.containsAll(Core16Ruleset.milestones))
        assertEquals(Core16Ruleset.milestones + extra, overridden.milestones)
    }

    @Test
    fun extraMilestoneWithTheSameIdReplacesOnlyThatThreshold() {
        val extra =
            MilestoneDefinition(
                Core16Milestones.PRINCIPLE_SOCIETY,
                MilestoneConditionSpec.AtLeast(MilestoneMetric.POPULATION, 20),
                "population reaches 20",
            )
        val overridden =
            Core16Ruleset.applyOverrides(
                RulesetOverrides(extraMilestones = listOf(extra)),
            )

        assertEquals(5, overridden.milestones.size)
        assertEquals(extra, overridden.milestones.first { it.id == Core16Milestones.PRINCIPLE_SOCIETY })
        assertEquals(
            MilestoneConditionSpec.AtLeast(MilestoneMetric.POPULATION, 15),
            Core16Ruleset.milestones.first { it.id == Core16Milestones.PRINCIPLE_SOCIETY }.condition,
        )
    }
}
