package io.kdmtracker.core.usecase

import io.kdmtracker.core.domain.HybridLogicalClock
import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.domain.ShowdownStatus
import io.kdmtracker.core.domain.SyncMeta
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.rules.StepResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ShowdownDeckUseCaseTest {
    private fun record(): SettlementRecord =
        SettlementRecord(
            settlement =
                Settlement(
                    id = Ulid.generate(90L) { 0 },
                    campaignId = Ulid.generate(91L) { 0 },
                    name = "Kingsport",
                    lanternYear = 3,
                    phase = SettlementPhase.SHOWDOWN,
                    currentStep = 1,
                    endeavors = 0,
                    population = 4,
                    settlementEvent = null,
                    sync = SyncMeta(HybridLogicalClock(0, 0)),
                ),
        )

    private fun started(deck: List<String>): SettlementRecord =
        assertIs<StepResult.Ok<SettlementRecord>>(SetUpShowdown.run(record(), "White Lion", 1, deck)).value

    @Test
    fun setUpStoresTheEncounterAndDeck() {
        val hunt =
            assertIs<StepResult.Ok<SettlementRecord>>(SetUpShowdown.run(record(), "Phoenix", 2, listOf("A"))).value
        assertEquals("Phoenix", hunt.state.showdown.monster)
        assertEquals(2, hunt.state.showdown.level)
        assertEquals(listOf("A"), hunt.state.showdown.aiDeck)

        assertIs<StepResult.Blocked>(SetUpShowdown.run(record(), "Phoenix", 4, listOf("A")))
        assertIs<StepResult.Blocked>(SetUpShowdown.run(record(), "Phoenix", 1, emptyList()))
    }

    @Test
    fun drawMovesTheTopCardToTheDiscard() {
        val ok = assertIs<StepResult.Ok<SettlementRecord>>(DrawShowdownCard.run(started(listOf("A", "B"))))

        assertEquals(listOf("B"), ok.value.state.showdown.aiDeck)
        assertEquals(listOf("A"), ok.value.state.showdown.discard)
        assertEquals("A", ok.value.state.showdown.lastDrawn)
    }

    @Test
    fun drawReshufflesWhenTheDeckEmpties() {
        val once = assertIs<StepResult.Ok<SettlementRecord>>(DrawShowdownCard.run(started(listOf("A")))).value

        val twice = assertIs<StepResult.Ok<SettlementRecord>>(DrawShowdownCard.run(once)).value

        assertEquals(emptyList(), twice.state.showdown.aiDeck)
        assertEquals(listOf("A"), twice.state.showdown.discard)
    }

    @Test
    fun woundMovesTheTopCardToTheWounds() {
        val ok = assertIs<StepResult.Ok<SettlementRecord>>(WoundMonster.run(started(listOf("A", "B"))))

        assertEquals(listOf("B"), ok.value.state.showdown.aiDeck)
        assertEquals(listOf("A"), ok.value.state.showdown.wounds)
    }

    @Test
    fun woundingTheLastCardLeavesTheDeckAndDiscardEmpty() {
        val ok = assertIs<StepResult.Ok<SettlementRecord>>(WoundMonster.run(started(listOf("A"))))

        assertTrue(
            ok.value.state.showdown.aiDeck
                .isEmpty(),
        )
        assertTrue(
            ok.value.state.showdown.discard
                .isEmpty(),
        )
        assertEquals(listOf("A"), ok.value.state.showdown.wounds)
    }

    @Test
    fun healReturnsTheLastWoundToTheTopOfTheDeck() {
        val wounded =
            assertIs<StepResult.Ok<SettlementRecord>>(
                WoundMonster.run(
                    assertIs<StepResult.Ok<SettlementRecord>>(WoundMonster.run(started(listOf("A", "B", "C")))).value,
                ),
            ).value

        val healed = assertIs<StepResult.Ok<SettlementRecord>>(HealMonster.run(wounded)).value

        assertEquals(listOf("B", "C"), healed.state.showdown.aiDeck)
        assertEquals(listOf("A"), healed.state.showdown.wounds)
    }

    @Test
    fun healWithNoWoundsIsBlocked() {
        assertIs<StepResult.Blocked>(HealMonster.run(started(listOf("A", "B"))))
    }

    @Test
    fun healAfterTheShowdownEndedIsBlocked() {
        val wounded = assertIs<StepResult.Ok<SettlementRecord>>(WoundMonster.run(started(listOf("A", "B")))).value
        val over =
            assertIs<StepResult.Ok<SettlementRecord>>(
                SetShowdownOutcome.run(wounded, ShowdownStatus.VICTORY),
            ).value

        assertIs<StepResult.Blocked>(HealMonster.run(over))
    }

    @Test
    fun woundWithNoCardsIsBlocked() {
        val empty =
            assertIs<StepResult.Ok<SettlementRecord>>(
                SetUpShowdown.run(record(), "White Lion", 1, listOf("A")),
            ).value

        val wounded = assertIs<StepResult.Ok<SettlementRecord>>(WoundMonster.run(empty)).value

        assertIs<StepResult.Blocked>(WoundMonster.run(wounded))
    }
}
