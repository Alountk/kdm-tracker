package io.kdmtracker.core.domain

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

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
    fun principleCategoryEntries() {
        assertContentEquals(
            listOf("DEATH", "NEW_LIFE", "SOCIETY"),
            PrincipleCategory.entries.map { it.name },
        )
    }

    @Test
    fun milestoneKeyEntries() {
        assertContentEquals(
            listOf(
                "PRINCIPLE_DEATH",
                "PRINCIPLE_NEW_LIFE",
                "PRINCIPLE_SOCIETY",
                "HOODED_KNIGHT",
                "GAME_OVER",
            ),
            MilestoneKey.entries.map { it.name },
        )
        assertEquals(5, MilestoneKey.entries.size)
    }
}
