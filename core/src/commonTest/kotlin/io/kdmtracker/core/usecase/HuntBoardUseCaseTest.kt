package io.kdmtracker.core.usecase

import io.kdmtracker.core.domain.HuntSpaceKind
import io.kdmtracker.core.domain.HuntStatus
import io.kdmtracker.core.domain.HybridLogicalClock
import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.domain.SyncMeta
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.rules.StepResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

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
    fun setUpSeedsThirteenSpacesWithStartAndMonster() {
        val result = SetUpHunt.run(record(), "White Lion", level = 1, players = 4)

        val ok = assertIs<StepResult.Ok<SettlementRecord>>(result)
        val spaces = ok.value.state.hunt.spaces
        assertEquals(13, spaces.size)
        assertEquals(HuntSpaceKind.START, spaces.first().kind)
        assertEquals(HuntSpaceKind.MONSTER, spaces.last().kind)
        assertEquals(HuntSpaceKind.NORMAL, spaces[1].kind)
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

    @Test
    fun advanceMovesOneSpaceAndCapsAtTheEnd() {
        val started = assertIs<StepResult.Ok<SettlementRecord>>(SetUpHunt.run(record(), "White Lion", 1, 1)).value

        val one = assertIs<StepResult.Ok<SettlementRecord>>(AdvanceHunt.run(started)).value
        assertEquals(1, one.state.hunt.position)

        var last = one
        repeat(20) { last = assertIs<StepResult.Ok<SettlementRecord>>(AdvanceHunt.run(last)).value }
        assertEquals(12, last.state.hunt.position)
    }
}
