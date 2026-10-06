package io.kdmtracker.core.domain

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class DomainEnumsTest {
    @Test
    fun settlementPhaseEntries() {
        assertContentEquals(
            listOf("SETTLEMENT", "HUNT", "SHOWDOWN"),
            SettlementPhase.entries.map { it.name },
        )
    }

    @Test
    fun stepTypeEntries() {
        assertContentEquals(
            listOf("BOOKKEEPING", "DECISION", "BRANCHING"),
            StepType.entries.map { it.name },
        )
    }

    @Test
    fun huntStatusEntries() {
        assertContentEquals(
            listOf("ACTIVE", "QUARRY_FOUND", "AMBUSH", "FAILED", "RUN_AWAY"),
            HuntStatus.entries.map { it.name },
        )
    }

    @Test
    fun sexEntries() {
        assertContentEquals(
            listOf("MALE", "FEMALE", "OTHER"),
            Sex.entries.map { it.name },
        )
    }

    @Test
    fun resourceCategoryEntries() {
        assertContentEquals(
            listOf("BASIC", "SPECIAL", "MONSTER"),
            ResourceCategory.entries.map { it.name },
        )
    }

    @Test
    fun principleCategoryEntriesIncludeConviction() {
        assertContentEquals(
            listOf("DEATH", "NEW_LIFE", "SOCIETY", "CONVICTION"),
            PrincipleCategory.entries.map { it.name },
        )
    }

    @Test
    fun core16MilestoneIdsAreStableAndOpenToNewOnes() {
        assertContentEquals(
            listOf(
                "principle-death",
                "principle-new-life",
                "principle-society",
                "hooded-knight",
                "game-over",
            ),
            Core16Milestones.ALL.map { it.value },
        )
        assertEquals(5, Core16Milestones.ALL.size)
        assertNotEquals(Core16Milestones.GAME_OVER, MilestoneId("house-rule-game-over"))
    }
}
