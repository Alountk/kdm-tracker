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
import kotlin.test.assertTrue

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
        // S6 (md section 2, 0-based): the White Lion's first monster-event (`E`) space is index 1,
        // not an ordinary event space
        assertEquals(HuntSpaceKind.MONSTER_E, spaces[1].kind)
        // S6: the beast's home is its level space (md 4 for a White Lion level 1),
        // not the final Starvation space
        assertEquals(4, ok.value.state.hunt.monsterPosition)
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

    /** S5: a second advance replaces the previously drawn card with a new one (once the first is resolved). */
    @Test
    fun advancingAgainRedrawsTheEvent() {
        val deck =
            listOf(
                HuntEvent("Card A", "a", HuntEventKind.BASIC),
                HuntEvent("Card B", "b", HuntEventKind.BASIC),
            )
        val started = assertIs<StepResult.Ok<SettlementRecord>>(SetUpHunt.run(record(), "White Lion", 1, 4)).value

        val one = assertIs<StepResult.Ok<SettlementRecord>>(AdvanceHunt.run(started, deck = deck, Random(1))).value
        // S7: resolve the first drawn card on the party's current space (index 1) before advancing
        val first = assertIs<HuntEvent>(one.state.hunt.drawnEvent)
        val assigned = assertIs<StepResult.Ok<SettlementRecord>>(AssignHuntEvent.run(one, 1, first.name)).value
        val resolved =
            assertIs<StepResult.Ok<SettlementRecord>>(ResolveHuntEvent.run(assigned, 1, first.name)).value

        val two = assertIs<StepResult.Ok<SettlementRecord>>(AdvanceHunt.run(resolved, deck = deck, Random(2))).value

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

    /**
     * S6: the per-quarry board is seeded from `25_tableros_de_caza_de_presa` (md section 2,
     * 0-based indices 0-12): the fixed spaces, the quarry's `E` spaces, and the beast's starting
     * space per level.
     */
    @Test
    fun setUpSeedsFixedSpacesAndTheQuarrysMonsterEventSpaces() {
        // fixed spaces are the same for every quarry (md 0 / 6 / 12)
        listOf("White Lion" to 1, "Phoenix" to 1, "Screaming Antelope" to 1).forEach { (quarry, lvl) ->
            val spaces =
                assertIs<StepResult.Ok<SettlementRecord>>(SetUpHunt.run(record(), quarry, lvl, 4))
                    .value.state.hunt.spaces
            assertEquals(HuntSpaceKind.START, spaces[0].kind, "$quarry start")
            assertEquals(HuntSpaceKind.OVERWHELMING_DARKNESS, spaces[6].kind, "$quarry OD")
            assertEquals(HuntSpaceKind.STARVATION, spaces[12].kind, "$quarry starvation")
        }

        // monster-event (E) spaces per quarry (md section 2, 0-based indices)
        val eSpaces =
            mapOf(
                "White Lion" to listOf(1, 2, 5, 8, 9),
                "Phoenix" to listOf(1, 4, 7, 9),
                "Screaming Antelope" to listOf(1, 2, 7, 9),
            )
        eSpaces.forEach { (quarry, expectedE) ->
            val spaces =
                assertIs<StepResult.Ok<SettlementRecord>>(SetUpHunt.run(record(), quarry, 1, 4))
                    .value.state.hunt.spaces
            expectedE.forEach {
                assertEquals(HuntSpaceKind.MONSTER_E, spaces[it].kind, "$quarry E at $it")
            }
            // every index that is NOT a fixed space or an E space is an ordinary event space
            val fixed = setOf(0, 6, 12)
            spaces.indices
                .filter { it !in fixed && it !in expectedE }
                .forEach { assertEquals(HuntSpaceKind.NORMAL, spaces[it].kind, "$quarry normal at $it") }
        }
    }

    /** S6: the beast's starting space depends on the quarry AND its level (md level table). */
    @Test
    fun setUpSeedsTheBeastHomePerQuarryAndLevel() {
        val homes =
            mapOf(
                "White Lion" to mapOf(1 to 4, 2 to 7, 3 to 11),
                "Phoenix" to mapOf(1 to 5, 2 to 8, 3 to 11),
                "Screaming Antelope" to mapOf(1 to 4, 2 to 8, 3 to 10),
            )
        homes.forEach { (quarry, byLevel) ->
            byLevel.forEach { (level, expectedHome) ->
                val hunt =
                    assertIs<StepResult.Ok<SettlementRecord>>(SetUpHunt.run(record(), quarry, level, 4))
                        .value.state.hunt
                assertEquals(expectedHome, hunt.monsterPosition, "$quarry L$level home")
            }
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

    /**
     * S7: the hunt cannot advance over an unresolved event — a drawn card must be marked resolved
     * on the party's current space before the party can step forward. Once resolved, advance works.
     */
    @Test
    fun advanceIsBlockedUntilTheDrawnEventIsResolved() {
        val deck = listOf(HuntEvent("Card A", "a", HuntEventKind.BASIC))
        val started = assertIs<StepResult.Ok<SettlementRecord>>(SetUpHunt.run(record(), "White Lion", 1, 4)).value

        // first advance draws the card
        val one = assertIs<StepResult.Ok<SettlementRecord>>(AdvanceHunt.run(started, deck = deck, Random(1))).value
        val drawn = assertIs<HuntEvent>(one.state.hunt.drawnEvent)

        // second advance is blocked: the card is not resolved yet
        assertIs<StepResult.Blocked>(AdvanceHunt.run(one, deck = deck, Random(2)))

        // resolve it on the party's current space (index 1)
        val assigned = assertIs<StepResult.Ok<SettlementRecord>>(AssignHuntEvent.run(one, 1, drawn.name)).value
        val resolved = assertIs<StepResult.Ok<SettlementRecord>>(ResolveHuntEvent.run(assigned, 1, drawn.name)).value

        // now advance is allowed
        val two = assertIs<StepResult.Ok<SettlementRecord>>(AdvanceHunt.run(resolved, deck = deck, Random(3))).value
        assertEquals(2, two.state.hunt.position)
    }

    /** S7: with no drawn card there is no gate — the party can always advance. */
    @Test
    fun advanceIsNotBlockedWhenNoEventHasBeenDrawn() {
        val started = assertIs<StepResult.Ok<SettlementRecord>>(SetUpHunt.run(record(), "White Lion", 1, 4)).value
        assertIs<StepResult.Ok<SettlementRecord>>(AdvanceHunt.run(started))
    }

    /** S6: landing on a step rolls 2d10 (sum 2..20) and stores it on that step. */
    @Test
    fun advanceRolls2d10OnTheLandedStep() {
        val started = assertIs<StepResult.Ok<SettlementRecord>>(SetUpHunt.run(record(), "White Lion", 1, 4)).value
        val after = assertIs<StepResult.Ok<SettlementRecord>>(AdvanceHunt.run(started, random = Random(7))).value

        val landed = after.state.hunt.spaces[after.state.hunt.position]
        val roll = assertIs<Int>(landed.roll)
        assertTrue(roll in 2..20, "2d10 sum must be 2..20, was $roll")
    }

    /** S6: a step keeps its own roll — a later visit never re-rolls a step that already has one. */
    @Test
    fun aLandedStepKeepsItsRollOnALaterVisit() {
        val deck = listOf(HuntEvent("Card A", "a", HuntEventKind.BASIC))
        val started = assertIs<StepResult.Ok<SettlementRecord>>(SetUpHunt.run(record(), "White Lion", 1, 4)).value
        val first = assertIs<StepResult.Ok<SettlementRecord>>(AdvanceHunt.run(started, deck = deck, Random(1))).value
        // step 1 now has a roll
        val firstRoll =
            assertIs<Int>(
                first.state.hunt.spaces[1]
                    .roll,
            )
        // resolve the card so we can advance again
        val drawn = assertIs<HuntEvent>(first.state.hunt.drawnEvent)
        val assigned = assertIs<StepResult.Ok<SettlementRecord>>(AssignHuntEvent.run(first, 1, drawn.name)).value
        val resolved =
            assertIs<StepResult.Ok<SettlementRecord>>(ResolveHuntEvent.run(assigned, 1, drawn.name)).value
        val second = assertIs<StepResult.Ok<SettlementRecord>>(AdvanceHunt.run(resolved, deck = deck, Random(2))).value
        // step 1 still holds its original roll (per-step, persisted)
        assertEquals(
            firstRoll,
            second.state.hunt.spaces[1]
                .roll,
        )
        // step 2 got its own fresh roll
        assertIs<Int>(
            second.state.hunt.spaces[2]
                .roll,
        )
    }

    /** S6: the player can edit a stored roll by hand; the edit persists. */
    @Test
    fun thePlayerCanEditAStoredRoll() {
        val started = assertIs<StepResult.Ok<SettlementRecord>>(SetUpHunt.run(record(), "White Lion", 1, 4)).value
        val after = assertIs<StepResult.Ok<SettlementRecord>>(AdvanceHunt.run(started, random = Random(3))).value
        val edited =
            assertIs<StepResult.Ok<SettlementRecord>>(
                EditHuntRoll.run(after, after.state.hunt.position, 15),
            ).value

        assertEquals(
            15,
            edited.state.hunt.spaces[after.state.hunt.position]
                .roll,
        )
    }

    /** S6: an out-of-range edited roll is rejected. */
    @Test
    fun anOutOfRangeEditedRollIsRejected() {
        val started = assertIs<StepResult.Ok<SettlementRecord>>(SetUpHunt.run(record(), "White Lion", 1, 4)).value
        assertIs<StepResult.Blocked>(EditHuntRoll.run(started, 1, 1))
        assertIs<StepResult.Blocked>(EditHuntRoll.run(started, 1, 21))
    }
}
