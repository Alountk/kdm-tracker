package io.kdmtracker.core.usecase

import io.kdmtracker.core.domain.HuntEvent
import io.kdmtracker.core.domain.HuntEventKind
import io.kdmtracker.core.domain.HuntSpaceKind
import io.kdmtracker.core.domain.HuntStatus
import io.kdmtracker.core.domain.HybridLogicalClock
import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.domain.SyncMeta
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.rules.StepResult
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class HuntBoardUseCaseTest {
    private fun record(): SettlementRecord =
        SettlementRecord(
            settlement =
                Settlement(
                    id = Ulid.generate(90L) { 0 },
                    campaignId = Ulid.generate(91L) { 0 },
                    name = "Kingsport",
                    lanternYear = 3,
                    phase = SettlementPhase.HUNT,
                    currentStep = 1,
                    endeavors = 0,
                    population = 4,
                    settlementEvent = null,
                    sync = SyncMeta(HybridLogicalClock(0, 0)),
                ),
        )

    @Test
    fun setUpSeedsThirteenSpacesWithStartAndStarvation() {
        val result = SetUpHunt.run(record(), "White Lion", level = 1, players = 4)

        val ok = assertIs<StepResult.Ok<SettlementRecord>>(result)
        val spaces = ok.value.state.hunt.spaces
        assertEquals(13, spaces.size)
        assertEquals(HuntSpaceKind.START, spaces.first().kind)
        // the last space is Starvation (manual 9.1), not a fixed "monster" space;
        // the beast's location is hunt.monsterPosition, which starts at the end
        assertEquals(HuntSpaceKind.STARVATION, spaces.last().kind)
        assertEquals(HuntSpaceKind.NORMAL, spaces[1].kind)
        assertEquals(12, ok.value.state.hunt.monsterPosition)
    }

    @Test
    fun setUpStoresQuarryLevelPlayersAndResetsStatus() {
        val result = SetUpHunt.run(record(), "Phoenix", level = 3, players = 2)

        val hunt = assertIs<StepResult.Ok<SettlementRecord>>(result).value.state.hunt
        assertEquals("Phoenix", hunt.quarry)
        assertEquals(3, hunt.level)
        assertEquals(2, hunt.players)
        assertEquals(0, hunt.position)
        assertEquals(HuntStatus.ACTIVE, hunt.status)
    }

    @Test
    fun setUpRejectsABlankQuarry() {
        val result = SetUpHunt.run(record(), "  ", level = 1, players = 1)

        assertIs<StepResult.Blocked>(result)
    }

    @Test
    fun setUpRejectsAnOutOfRangeLevelOrPlayers() {
        assertIs<StepResult.Blocked>(SetUpHunt.run(record(), "White Lion", level = 4, players = 1))
        assertIs<StepResult.Blocked>(SetUpHunt.run(record(), "White Lion", level = 1, players = 5))
    }

    @Test
    fun markSpaceKindEditsAKindWithoutChangingTheCount() {
        val hunt = assertIs<StepResult.Ok<SettlementRecord>>(SetUpHunt.run(record(), "White Lion", 1, 1)).value

        val result = MarkSpaceKind.run(hunt, index = 5, kind = HuntSpaceKind.OVERWHELMING_DARKNESS)

        val spaces =
            assertIs<StepResult.Ok<SettlementRecord>>(result)
                .value.state.hunt.spaces
        assertEquals(13, spaces.size)
        assertEquals(HuntSpaceKind.OVERWHELMING_DARKNESS, spaces[5].kind)
    }

    /** S5: advancing draws a random card from the real deck into the persistent hunt state. */
    @Test
    fun advanceDrawsARandomHuntEventFromTheDeck() {
        val deck =
            listOf(
                HuntEvent("Card A", "a", HuntEventKind.BASIC),
                HuntEvent("Card B", "b", HuntEventKind.BASIC),
                HuntEvent("Card C", "c", HuntEventKind.BASIC),
            )
        val started = assertIs<StepResult.Ok<SettlementRecord>>(SetUpHunt.run(record(), "White Lion", 1, 4)).value

        val result = AdvanceHunt.run(started, deck = deck, random = Random(42))

        val hunt = assertIs<StepResult.Ok<SettlementRecord>>(result).value.state.hunt
        assertEquals(1, hunt.position)
        val drawn = assertIs<HuntEvent>(hunt.drawnEvent)
        // the drawn card comes from the deck
        assertContains(deck.map { it.name }, drawn.name)
    }

    /** S5: a second advance replaces the previously drawn card with a new one. */
    @Test
    fun advancingAgainRedrawsTheEvent() {
        val deck =
            listOf(
                HuntEvent("Card A", "a", HuntEventKind.BASIC),
                HuntEvent("Card B", "b", HuntEventKind.BASIC),
            )
        val started = assertIs<StepResult.Ok<SettlementRecord>>(SetUpHunt.run(record(), "White Lion", 1, 4)).value

        val one = assertIs<StepResult.Ok<SettlementRecord>>(AdvanceHunt.run(started, deck = deck, Random(1))).value
        val two = assertIs<StepResult.Ok<SettlementRecord>>(AdvanceHunt.run(one, deck = deck, Random(2))).value

        assertNotEquals(one.state.hunt.drawnEvent, two.state.hunt.drawnEvent)
    }

    /** S5: advancing with no deck draws nothing (backwards compatible with the bare signature). */
    @Test
    fun advanceWithoutADrawnDeckLeavesTheEventNull() {
        val started = assertIs<StepResult.Ok<SettlementRecord>>(SetUpHunt.run(record(), "White Lion", 1, 4)).value

        val result = assertIs<StepResult.Ok<SettlementRecord>>(AdvanceHunt.run(started)).value

        assertNull(result.state.hunt.drawnEvent)
    }

    @Test
    fun advanceMovesOneSpaceAndCapsAtTheEnd() {
        val started = assertIs<StepResult.Ok<SettlementRecord>>(SetUpHunt.run(record(), "White Lion", 1, 1)).value

        val one = assertIs<StepResult.Ok<SettlementRecord>>(AdvanceHunt.run(started)).value
        assertEquals(1, one.state.hunt.position)

        var last = one
        repeat(20) { last = assertIs<StepResult.Ok<SettlementRecord>>(AdvanceHunt.run(last)).value }
        assertEquals(12, last.state.hunt.position)
    }

    /** S1: the base 13-space layout (manual 9.1): OD is central, Starvation is last. */
    @Test
    fun setUpSeedsTheBaseLayoutWithOverwhelmingDarknessAndStarvation() {
        val spaces =
            assertIs<StepResult.Ok<SettlementRecord>>(SetUpHunt.run(record(), "White Lion", 1, 4))
                .value.state.hunt.spaces

        assertEquals(13, spaces.size)
        assertEquals(HuntSpaceKind.START, spaces[0].kind)
        assertEquals(HuntSpaceKind.OVERWHELMING_DARKNESS, spaces[6].kind)
        assertEquals(HuntSpaceKind.STARVATION, spaces[12].kind)
        // every other space is an ordinary event space until the player marks a special E
        listOf(1, 2, 3, 4, 5, 7, 8, 9, 10, 11).forEach { assertEquals(HuntSpaceKind.NORMAL, spaces[it].kind) }
    }

    /** S1: the same verified base layout applies to all three base quarries. */
    @Test
    fun setUpSeedsTheBaseLayoutForEveryBaseQuarry() {
        listOf("White Lion", "Screaming Antelope", "Phoenix").forEach { quarry ->
            val spaces =
                assertIs<StepResult.Ok<SettlementRecord>>(SetUpHunt.run(record(), quarry, 1, 4))
                    .value.state.hunt.spaces

            assertEquals(HuntSpaceKind.START, spaces[0].kind, "$quarry start")
            assertEquals(HuntSpaceKind.OVERWHELMING_DARKNESS, spaces[6].kind, "$quarry OD")
            assertEquals(HuntSpaceKind.STARVATION, spaces[12].kind, "$quarry starvation")
            // the per-quarry special-E positions live only in the rulebook's diagrams (images),
            // so none are invented here — the player still marks them with MarkSpaceKind
            assertEquals(HuntSpaceKind.NORMAL, spaces[3].kind, "$quarry must not invent an E at 3")
            assertEquals(HuntSpaceKind.NORMAL, spaces[4].kind, "$quarry must not invent an E at 4")
        }
    }

    /**
     * S5 history isolation: an event resolved on one space must stay on that space — advancing to
     * the next space must not make it appear in the next space's history.
     */
    @Test
    fun resolvedEventStaysOnItsOwnSpaceAcrossAdvance() {
        val deck = listOf(HuntEvent(name = "Evento Aleatorio", text = "x", kind = HuntEventKind.BASIC))
        val setup = assertIs<StepResult.Ok<SettlementRecord>>(SetUpHunt.run(record(), "White Lion", 1, 1)).value
        // party advances to space 1 (draw 1)
        val adv1 = assertIs<StepResult.Ok<SettlementRecord>>(AdvanceHunt.run(setup, deck)).value
        // resolve the drawn card on space 1
        val name =
            adv1.state.hunt.drawnEvent!!
                .name
        val assigned = assertIs<StepResult.Ok<SettlementRecord>>(AssignHuntEvent.run(adv1, 1, name)).value
        val resolved = assertIs<StepResult.Ok<SettlementRecord>>(ResolveHuntEvent.run(assigned, 1, name)).value

        assertContains(
            resolved.state.hunt.spaces[1]
                .events,
            name,
        )
        assertContains(
            resolved.state.hunt.spaces[1]
                .resolved,
            name,
        )
        // space 2 must not have picked it up
        assertEquals(
            emptyList(),
            resolved.state.hunt.spaces[2]
                .events,
        )
        assertEquals(
            emptyList(),
            resolved.state.hunt.spaces[2]
                .resolved,
        )

        // advancing to space 2 keeps history isolated
        val adv2 = assertIs<StepResult.Ok<SettlementRecord>>(AdvanceHunt.run(resolved, deck)).value
        assertEquals(
            emptyList(),
            adv2.state.hunt.spaces[2]
                .events,
        )
        assertContains(
            adv2.state.hunt.spaces[1]
                .resolved,
            name,
        )
    }
}
