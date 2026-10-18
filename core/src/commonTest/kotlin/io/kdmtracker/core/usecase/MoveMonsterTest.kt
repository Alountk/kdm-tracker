package io.kdmtracker.core.usecase

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

class MoveMonsterTest {
    private fun started(): SettlementRecord {
        val record =
            SettlementRecord(
                settlement =
                    Settlement(
                        id = Ulid.generate(92L) { 0 },
                        campaignId = Ulid.generate(93L) { 0 },
                        name = "Kingsport",
                        lanternYear = 1,
                        phase = SettlementPhase.HUNT,
                        currentStep = 1,
                        endeavors = 0,
                        population = 4,
                        settlementEvent = null,
                        sync = SyncMeta(HybridLogicalClock(0, 0)),
                    ),
            )
        val setup = SetUpHunt.run(record, "White Lion", level = 1, players = 1)
        return assertIs<StepResult.Ok<SettlementRecord>>(setup).value
    }

    @Test
    fun monsterStartsOnTheLastSpaceAfterSetUp() {
        val hunt = started().state.hunt
        assertEquals(hunt.spaces.lastIndex, hunt.monsterPosition)
        assertEquals(HuntStatus.ACTIVE, hunt.status)
    }

    @Test
    fun moveMonsterStepsOneSpaceTowardTheParty() {
        // The party advances to space 1; the monster starts at 12.
        val party = assertIs<StepResult.Ok<SettlementRecord>>(AdvanceHunt.run(started())).value
        val before = party.state.hunt.monsterPosition
        val moved = assertIs<StepResult.Ok<SettlementRecord>>(MoveMonster.run(party)).value
        assertEquals(before - 1, moved.state.hunt.monsterPosition)
        assertEquals(HuntStatus.ACTIVE, moved.state.hunt.status)
    }

    @Test
    fun moveMonsterAmbushesWhenItReachesTheParty() {
        // put the party and the monster one apart, then step the monster onto the party's space
        var record = started()
        val partyIndex = 10
        while (record.state.hunt.position < partyIndex) {
            record = assertIs<StepResult.Ok<SettlementRecord>>(AdvanceHunt.run(record)).value
        }
        // walk the monster down to party+1
        val target = partyIndex + 1
        while (record.state.hunt.monsterPosition > target) {
            record = assertIs<StepResult.Ok<SettlementRecord>>(MoveMonster.run(record)).value
        }
        assertEquals(partyIndex + 1, record.state.hunt.monsterPosition)

        val ambush = assertIs<StepResult.Ok<SettlementRecord>>(MoveMonster.run(record)).value
        assertEquals(partyIndex, ambush.state.hunt.monsterPosition)
        assertEquals(HuntStatus.AMBUSH, ambush.state.hunt.status)
    }

    @Test
    fun moveMonsterBlocksBeforeSetup() {
        val record =
            SettlementRecord(
                settlement =
                    Settlement(
                        id = Ulid.generate(94L) { 0 },
                        campaignId = Ulid.generate(95L) { 0 },
                        name = "Kingsport",
                        lanternYear = 1,
                        phase = SettlementPhase.HUNT,
                        currentStep = 1,
                        endeavors = 0,
                        population = 4,
                        settlementEvent = null,
                        sync = SyncMeta(HybridLogicalClock(0, 0)),
                    ),
            )
        assertIs<StepResult.Blocked>(MoveMonster.run(record))
    }

    @Test
    fun moveMonsterBlocksWhenTheHuntIsOver() {
        val outcome = SetHuntOutcome.run(started(), HuntStatus.QUARRY_FOUND)
        val ended = assertIs<StepResult.Ok<SettlementRecord>>(outcome).value
        assertIs<StepResult.Blocked>(MoveMonster.run(ended))
    }
}
