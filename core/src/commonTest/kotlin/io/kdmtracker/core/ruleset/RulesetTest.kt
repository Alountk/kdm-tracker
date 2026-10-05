package io.kdmtracker.core.ruleset

import io.kdmtracker.core.domain.MilestoneKey
import io.kdmtracker.core.domain.PrincipleCategory
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
    fun core16HasFiveOfficialMilestones() {
        assertEquals(5, Core16Ruleset.milestones.size)
        assertEquals(
            MilestoneKey.entries.toSet(),
            Core16Ruleset.milestones.map { it.key }.toSet(),
        )
        assertEquals(
            "1 death",
            Core16Ruleset.milestones.first { it.key == MilestoneKey.PRINCIPLE_DEATH }.condition,
        )
        assertEquals(
            "first child is born",
            Core16Ruleset.milestones.first { it.key == MilestoneKey.PRINCIPLE_NEW_LIFE }.condition,
        )
        assertEquals(
            "population reaches 15",
            Core16Ruleset.milestones.first { it.key == MilestoneKey.PRINCIPLE_SOCIETY }.condition,
        )
        assertEquals(
            "settlement has 5 innovations",
            Core16Ruleset.milestones.first { it.key == MilestoneKey.HOODED_KNIGHT }.condition,
        )
        assertEquals(
            "population reaches 0",
            Core16Ruleset.milestones.first { it.key == MilestoneKey.GAME_OVER }.condition,
        )
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
    fun extraMilestonesAppendWithoutDroppingBase() {
        val extra = MilestoneDefinition(MilestoneKey.GAME_OVER, "house rule game over")
        val overridden =
            Core16Ruleset.applyOverrides(
                RulesetOverrides(extraMilestones = listOf(extra)),
            )
        assertEquals(6, overridden.milestones.size)
        assertTrue(overridden.milestones.containsAll(Core16Ruleset.milestones))
        assertEquals(Core16Ruleset.milestones + extra, overridden.milestones)
    }
}
